package com.ims.backend.service;

import com.ims.backend.enums.*;
import com.ims.backend.model.Signal;
import com.ims.backend.model.WorkItem;
import com.ims.backend.repository.jpa.WorkItemRepository;
import com.ims.backend.repository.mongo.SignalRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
public class SignalIngestionService {

    private final SignalRepository signalRepository;
    private final WorkItemRepository workItemRepository;
    private final AlertService alertService;

    // In-memory buffer
    private final BlockingQueue<Signal> signalBuffer = new LinkedBlockingQueue<>(50000);

    // Debounce map — componentId -> workItemId
    private final ConcurrentHashMap<String, String> activeWorkItems = new ConcurrentHashMap<>();

    // Debounce timer map — componentId -> scheduled future
    private final ConcurrentHashMap<String, ScheduledFuture<?>> debounceTimers = new ConcurrentHashMap<>();
    private final ScheduledExecutorService debounceScheduler = Executors.newScheduledThreadPool(4);

    // Debounce window — 10 seconds
    private static final long DEBOUNCE_WINDOW_SECONDS = 10;

    // Rate limiter
    private final int MAX_SIGNALS_PER_SECOND = 10000;
    private final AtomicInteger tokenBucket = new AtomicInteger(MAX_SIGNALS_PER_SECOND);
    private final AtomicLong lastRefillTime = new AtomicLong(System.currentTimeMillis());

    // Throughput tracking
    private final AtomicInteger signalCounter = new AtomicInteger(0);
     // Retry config
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 500;

    // Multiple worker threads — handles MongoDB slowness
    private static final int WORKER_THREADS = 4;
    public SignalIngestionService(SignalRepository signalRepository,
                                   WorkItemRepository workItemRepository,
                                   AlertService alertService) {
        this.signalRepository = signalRepository;
        this.workItemRepository = workItemRepository;
        this.alertService = alertService;
        startAsyncWorkers();
        startMetricsPrinter();
        startTokenRefiller();
    }

    public boolean ingest(Signal signal) {
        if (!tryConsume()) {
            log.warn("Rate limit exceeded! Rejecting signal for: {}", signal.getComponentId());
            return false;
        }
        signal.setReceivedAt(Instant.now());
        boolean added = signalBuffer.offer(signal);
        if (!added) {
            log.warn("Buffer full! Dropping signal for: {}", signal.getComponentId());
            return false;
        }
        signalCounter.incrementAndGet();
        return true;
    }

    private boolean tryConsume() {
        refillTokens();
        int current = tokenBucket.get();
        if (current <= 0) return false;
        return tokenBucket.compareAndSet(current, current - 1);
    }

    private void refillTokens() {
        long now = System.currentTimeMillis();
        long last = lastRefillTime.get();
        if (now - last >= 1000) {
            if (lastRefillTime.compareAndSet(last, now)) {
                tokenBucket.set(MAX_SIGNALS_PER_SECOND);
            }
        }
    }

    private void startTokenRefiller() {
        Executors.newSingleThreadScheduledExecutor()
            .scheduleAtFixedRate(
                () -> tokenBucket.set(MAX_SIGNALS_PER_SECOND),
                1, 1, TimeUnit.SECONDS
            );
    }

    private void startMetricsPrinter() {
        Executors.newSingleThreadScheduledExecutor()
            .scheduleAtFixedRate(() -> {
                int count = signalCounter.getAndSet(0);
                log.info("Throughput: {} signals in last 5 seconds ({} signals/sec) | Buffer: {}/50000",
                    count, count / 5, signalBuffer.size());
            }, 5, 5, TimeUnit.SECONDS);
    }

private void startAsyncWorkers() {
        ExecutorService workerPool = Executors.newFixedThreadPool(WORKER_THREADS);
        for (int i = 0; i < WORKER_THREADS; i++) {
            final int workerId = i;
            workerPool.submit(() -> {
                log.info("Signal worker {} started", workerId);
                while (true) {
                    try {
                        Signal signal = signalBuffer.take();
                        processSignalWithRetry(signal);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    } catch (Exception e) {
                        log.error("Worker {} error: {}", workerId, e.getMessage());
                    }
                }
            });
        }
    }

     // Retry logic — retries up to 3 times if DB write fails
    private void processSignalWithRetry(Signal signal) {
        int attempts = 0;
        while (attempts < MAX_RETRIES) {
            try {
                processSignal(signal);
                return; // success
            } catch (Exception e) {
                attempts++;
                if (attempts >= MAX_RETRIES) {
                    log.error("Failed to process signal after {} retries. Component: {}. Error: {}",
                        MAX_RETRIES, signal.getComponentId(), e.getMessage());
                } else {
                    log.warn("Retry {}/{} for signal. Component: {}",
                        attempts, MAX_RETRIES, signal.getComponentId());
                    try {
                        Thread.sleep(RETRY_DELAY_MS * attempts); // exponential backoff
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
    }
    

    private void processSignal(Signal signal) {
        String componentId = signal.getComponentId();
        String existingWorkItemId = activeWorkItems.get(componentId);

        // Reset debounce timer — every new signal resets the 10-second window
        resetDebounceTimer(componentId);

        if (existingWorkItemId != null) {
            signal.setWorkItemId(existingWorkItemId);
            signalRepository.save(signal);

            workItemRepository.findById(UUID.fromString(existingWorkItemId))
                .ifPresent(wi -> {
                    wi.setSignalCount(wi.getSignalCount() + 1);
                    wi.setLastSignalAt(signal.getReceivedAt());
                    wi.setUpdatedAt(Instant.now());
                    workItemRepository.save(wi);
                });

            log.debug("Linked signal to existing WorkItem: {}", existingWorkItemId);
        } else {
            WorkItem workItem = WorkItem.builder()
                .componentId(componentId)
                .componentType(signal.getComponentType())
                .title(signal.getComponentType() + " failure on " + componentId)
                .priority(resolvePriority(signal.getComponentType()))
                .status(WorkItemStatus.OPEN)
                .signalCount(1)
                .firstSignalAt(signal.getReceivedAt())
                .lastSignalAt(signal.getReceivedAt())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

            WorkItem saved = workItemRepository.save(workItem);
            String workItemId = saved.getId().toString();

            activeWorkItems.put(componentId, workItemId);
            signal.setWorkItemId(workItemId);
            signalRepository.save(signal);
            alertService.sendAlert(saved);

            log.info("Created new WorkItem: {} for component: {}", workItemId, componentId);
        }
    }

    // Reset 10-second debounce timer for a component
    private void resetDebounceTimer(String componentId) {
        // Cancel existing timer if any
        ScheduledFuture<?> existing = debounceTimers.get(componentId);
        if (existing != null) {
            existing.cancel(false);
        }

        // Schedule new timer — after 10 seconds of silence, clear this component
        ScheduledFuture<?> future = debounceScheduler.schedule(() -> {
            activeWorkItems.remove(componentId);
            debounceTimers.remove(componentId);
            log.info("Debounce window expired for component: {}. Next signal will create new WorkItem.", componentId);
        }, DEBOUNCE_WINDOW_SECONDS, TimeUnit.SECONDS);

        debounceTimers.put(componentId, future);
    }

    public void clearActiveWorkItem(String componentId) {
        activeWorkItems.remove(componentId);
        ScheduledFuture<?> timer = debounceTimers.remove(componentId);
        if (timer != null) timer.cancel(false);
    }

    private Priority resolvePriority(ComponentType type) {
        return switch (type) {
            case RDBMS -> Priority.P0;
            case API -> Priority.P1;
            case CACHE -> Priority.P2;
            default -> Priority.P3;
        };
    }

    public int getBufferSize() {
        return signalBuffer.size();
    }
}
