package com.ims.backend.service;

import com.ims.backend.enums.Priority;
import com.ims.backend.model.WorkItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
public class AlertService {

    // Each strategy is a separate implementation — true Strategy Pattern
    private final Map<Priority, AlertStrategy> strategies;

    public AlertService() {
        this.strategies = Map.of(
            Priority.P0, workItem -> {
                log.error("🚨 P0 CRITICAL ALERT 🚨 Component: {} | WorkItem: {}",
                    workItem.getComponentId(), workItem.getId());
                log.error("→ PagerDuty: Oncall engineer being paged");
                log.error("→ Slack #incidents: P0 alert posted");
            },
            Priority.P1, workItem -> {
                log.warn("⚠️ P1 HIGH ALERT | Component: {} | WorkItem: {}",
                    workItem.getComponentId(), workItem.getId());
                log.warn("→ Slack #incidents: P1 alert posted");
                log.warn("→ Email: Team lead notified");
            },
            Priority.P2, workItem -> {
                log.warn("📢 P2 MEDIUM ALERT | Component: {} | WorkItem: {}",
                    workItem.getComponentId(), workItem.getId());
                log.warn("→ Slack #alerts: P2 notification posted");
            },
            Priority.P3, workItem -> {
                log.info("📋 P3 LOW ALERT | Component: {} | WorkItem: {}",
                    workItem.getComponentId(), workItem.getId());
                log.info("→ Slack #monitoring: P3 logged");
            }
        );
    }

    // Swaps strategy at runtime based on priority
    public void sendAlert(WorkItem workItem) {
        AlertStrategy strategy = strategies.getOrDefault(
            workItem.getPriority(),
            wi -> log.warn("No alert strategy for priority: {}", wi.getPriority())
        );
        strategy.sendAlert(workItem);
    }
}
