package com.ims.backend.service;

import com.ims.backend.enums.WorkItemStatus;
import com.ims.backend.model.WorkItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class WorkItemStateService {

    public void transition(WorkItem workItem, WorkItemStatus newStatus) {
        switch (workItem.getStatus()) {
            case OPEN -> handleOpen(workItem, newStatus);
            case INVESTIGATING -> handleInvestigating(workItem, newStatus);
            case RESOLVED -> handleResolved(workItem, newStatus);
            case CLOSED -> throw new RuntimeException("Cannot transition from CLOSED state.");
        }
    }

    private void handleOpen(WorkItem workItem, WorkItemStatus newStatus) {
        if (newStatus != WorkItemStatus.INVESTIGATING) {
            throw new RuntimeException("OPEN can only go to INVESTIGATING. Got: " + newStatus);
        }
        log.info("WorkItem {} : OPEN → INVESTIGATING", workItem.getId());
    }

    private void handleInvestigating(WorkItem workItem, WorkItemStatus newStatus) {
        if (newStatus != WorkItemStatus.RESOLVED) {
            throw new RuntimeException("INVESTIGATING can only go to RESOLVED. Got: " + newStatus);
        }
        log.info("WorkItem {} : INVESTIGATING → RESOLVED", workItem.getId());
    }

    private void handleResolved(WorkItem workItem, WorkItemStatus newStatus) {
        if (newStatus != WorkItemStatus.CLOSED) {
            throw new RuntimeException("RESOLVED can only go to CLOSED. Got: " + newStatus);
        }
        log.info("WorkItem {} : RESOLVED → CLOSED", workItem.getId());
    }
}
