package com.ims.backend.service;

import com.ims.backend.enums.WorkItemStatus;
import com.ims.backend.model.StatusHistory;
import com.ims.backend.model.WorkItem;
import com.ims.backend.repository.jpa.RcaRepository;
import com.ims.backend.repository.jpa.StatusHistoryRepository;
import com.ims.backend.repository.jpa.WorkItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkItemService {

    private final WorkItemRepository workItemRepository;
    private final RcaRepository rcaRepository;
    private final StatusHistoryRepository statusHistoryRepository;
    private final SignalIngestionService signalIngestionService;
    private final DashboardCacheService dashboardCacheService;
    private final WorkItemStateService workItemStateService;

    public List<WorkItem> getAllWorkItems() {
        List<WorkItem> cached = dashboardCacheService.getActiveIncidents();
        if (cached != null) return cached;
        List<WorkItem> fromDb = workItemRepository.findAllByOrderByCreatedAtDesc();
        dashboardCacheService.updateActiveIncidents(fromDb);
        return fromDb;
    }

    public WorkItem getWorkItemById(UUID id) {
        return workItemRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("WorkItem not found: " + id));
    }

    public List<StatusHistory> getStatusHistory(UUID workItemId) {
        return statusHistoryRepository.findByWorkItemIdOrderByChangedAtAsc(workItemId);
    }

    @Transactional
    public WorkItem updateStatus(UUID workItemId, WorkItemStatus newStatus, String changedBy) {
        WorkItem workItem = getWorkItemById(workItemId);
        WorkItemStatus oldStatus = workItem.getStatus();

        // RCA check before CLOSED
        if (newStatus == WorkItemStatus.CLOSED) {
            boolean rcaComplete = rcaRepository.existsByWorkItemIdAndIsCompleteTrue(workItemId);
            if (!rcaComplete) {
                throw new RuntimeException("Cannot close WorkItem — RCA is missing or incomplete.");
            }
        }

        // State Pattern validates transition
        workItemStateService.transition(workItem, newStatus);

        workItem.setStatus(newStatus);
        workItem.setUpdatedAt(Instant.now());
        WorkItem saved = workItemRepository.save(workItem);

        StatusHistory history = StatusHistory.builder()
            .workItemId(workItemId)
            .fromStatus(oldStatus)
            .toStatus(newStatus)
            .changedBy(changedBy)
            .changedAt(Instant.now())
            .build();
        statusHistoryRepository.save(history);

        dashboardCacheService.invalidate();

        if (newStatus == WorkItemStatus.CLOSED) {
            signalIngestionService.clearActiveWorkItem(workItem.getComponentId());
            log.info("WorkItem {} closed. Component {} freed.", workItemId, workItem.getComponentId());
        }

        log.info("WorkItem {} transitioned {} → {}", workItemId, oldStatus, newStatus);
        return saved;
    }
}
