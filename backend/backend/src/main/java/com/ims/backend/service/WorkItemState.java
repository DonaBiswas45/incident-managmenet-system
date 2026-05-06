package com.ims.backend.service;

import com.ims.backend.enums.WorkItemStatus;
import com.ims.backend.model.WorkItem;

public interface WorkItemState {
    void transition(WorkItem workItem, WorkItemStatus newStatus);
}
