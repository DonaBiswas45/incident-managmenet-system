package com.ims.backend.service;

import com.ims.backend.enums.WorkItemStatus;
import com.ims.backend.model.WorkItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
public class WorkItemStateService {

    // Each state is its own implementation — true State Pattern
    private final Map<WorkItemStatus, WorkItemState> states;

    public WorkItemStateService() {
        this.states = Map.of(
            WorkItemStatus.OPEN, (workItem, newStatus) -> {
                if (newStatus != WorkItemStatus.INVESTIGATING) {
                    throw new RuntimeException(
                        "OPEN can only go to INVESTIGATING. Got: " + newStatus);
                }
                log.info("WorkItem {} : OPEN → INVESTIGATING", workItem.getId());
            },
            WorkItemStatus.INVESTIGATING, (workItem, newStatus) -> {
                if (newStatus != WorkItemStatus.RESOLVED) {
                    throw new RuntimeException(
                        "INVESTIGATING can only go to RESOLVED. Got: " + newStatus);
                }
                log.info("WorkItem {} : INVESTIGATING → RESOLVED", workItem.getId());
            },
            WorkItemStatus.RESOLVED, (workItem, newStatus) -> {
                if (newStatus != WorkItemStatus.CLOSED) {
                    throw new RuntimeException(
                        "RESOLVED can only go to CLOSED. Got: " + newStatus);
                }
                log.info("WorkItem {} : RESOLVED → CLOSED", workItem.getId());
            },
            WorkItemStatus.CLOSED, (workItem, newStatus) -> {
                throw new RuntimeException("Cannot transition from CLOSED state.");
            }
        );
    }

    public void transition(WorkItem workItem, WorkItemStatus newStatus) {
        WorkItemState state = states.get(workItem.getStatus());
        if (state == null) {
            throw new RuntimeException("Unknown state: " + workItem.getStatus());
        }
        state.transition(workItem, newStatus);
    }
}
