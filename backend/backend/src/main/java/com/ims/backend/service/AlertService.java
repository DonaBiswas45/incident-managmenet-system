package com.ims.backend.service;

import com.ims.backend.enums.Priority;
import com.ims.backend.model.WorkItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AlertService {

    // Strategy Pattern — selects alert logic based on priority
    public void sendAlert(WorkItem workItem) {
        switch (workItem.getPriority()) {
            case P0 -> sendP0Alert(workItem);
            case P1 -> sendP1Alert(workItem);
            case P2 -> sendP2Alert(workItem);
            case P3 -> sendP3Alert(workItem);
        }
    }

    private void sendP0Alert(WorkItem workItem) {
        log.error("🚨 P0 CRITICAL ALERT 🚨 Component: {} | WorkItem: {}",
            workItem.getComponentId(), workItem.getId());
        log.error("→ PagerDuty: Oncall engineer being paged");
        log.error("→ Slack #incidents: P0 alert posted");
    }

    private void sendP1Alert(WorkItem workItem) {
        log.warn("⚠️ P1 HIGH ALERT | Component: {} | WorkItem: {}",
            workItem.getComponentId(), workItem.getId());
        log.warn("→ Slack #incidents: P1 alert posted");
        log.warn("→ Email: Team lead notified");
    }

    private void sendP2Alert(WorkItem workItem) {
        log.warn("📢 P2 MEDIUM ALERT | Component: {} | WorkItem: {}",
            workItem.getComponentId(), workItem.getId());
        log.warn("→ Slack #alerts: P2 notification posted");
    }

    private void sendP3Alert(WorkItem workItem) {
        log.info("📋 P3 LOW ALERT | Component: {} | WorkItem: {}",
            workItem.getComponentId(), workItem.getId());
        log.info("→ Slack #monitoring: P3 logged");
    }
}