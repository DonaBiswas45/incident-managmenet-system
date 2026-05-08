package com.ims.backend.controller;

import com.ims.backend.repository.jpa.WorkItemRepository;
import com.ims.backend.repository.mongo.SignalRepository;
import com.ims.backend.service.SignalIngestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/metrics")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class MetricsController {

    private final WorkItemRepository workItemRepository;
    private final SignalRepository signalRepository;
    private final SignalIngestionService signalIngestionService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getMetrics() {
        Map<String, Object> metrics = new LinkedHashMap<>();

        // Work item counts by status
        Map<String, Long> byStatus = new LinkedHashMap<>();
        byStatus.put("OPEN", workItemRepository.countByStatus(
            com.ims.backend.enums.WorkItemStatus.OPEN));
        byStatus.put("INVESTIGATING", workItemRepository.countByStatus(
            com.ims.backend.enums.WorkItemStatus.INVESTIGATING));
        byStatus.put("RESOLVED", workItemRepository.countByStatus(
            com.ims.backend.enums.WorkItemStatus.RESOLVED));
        byStatus.put("CLOSED", workItemRepository.countByStatus(
            com.ims.backend.enums.WorkItemStatus.CLOSED));

        // Work item counts by priority
        Map<String, Long> byPriority = new LinkedHashMap<>();
        byPriority.put("P0", workItemRepository.countByPriority(
            com.ims.backend.enums.Priority.P0));
        byPriority.put("P1", workItemRepository.countByPriority(
            com.ims.backend.enums.Priority.P1));
        byPriority.put("P2", workItemRepository.countByPriority(
            com.ims.backend.enums.Priority.P2));
        byPriority.put("P3", workItemRepository.countByPriority(
            com.ims.backend.enums.Priority.P3));

        // Total signals in MongoDB
        long totalSignals = signalRepository.count();

        // Signals in last hour
        long signalsLastHour = signalRepository.countByReceivedAtAfter(
            Instant.now().minus(1, ChronoUnit.HOURS));

        // Signals in last 24 hours
        long signalsLast24h = signalRepository.countByReceivedAtAfter(
            Instant.now().minus(24, ChronoUnit.HOURS));

        metrics.put("workItemsByStatus", byStatus);
        metrics.put("workItemsByPriority", byPriority);
        metrics.put("totalWorkItems", workItemRepository.count());
        metrics.put("totalSignals", totalSignals);
        metrics.put("signalsLastHour", signalsLastHour);
        metrics.put("signalsLast24h", signalsLast24h);
        metrics.put("bufferSize", signalIngestionService.getBufferSize());
        metrics.put("timestamp", Instant.now().toString());

        return ResponseEntity.ok(metrics);
    }
}
