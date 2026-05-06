package com.ims.backend.service;

import com.ims.backend.model.WorkItem;

public interface AlertStrategy {
    void sendAlert(WorkItem workItem);
}
