package com.ims.backend.service;

import com.ims.backend.model.Rca;
import com.ims.backend.model.WorkItem;
import com.ims.backend.repository.jpa.RcaRepository;
import com.ims.backend.repository.jpa.WorkItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RcaService {

    private final RcaRepository rcaRepository;
    private final WorkItemRepository workItemRepository;

    @Transactional
    public Rca submitRca(UUID workItemId, Rca rcaRequest) {
        // Check work item exists
        WorkItem workItem = workItemRepository.findById(workItemId)
            .orElseThrow(() -> new RuntimeException("WorkItem not found: " + workItemId));

        // Check RCA doesn't already exist
        if (rcaRepository.findByWorkItemId(workItemId).isPresent()) {
            throw new RuntimeException("RCA already exists for WorkItem: " + workItemId);
        }

        // Validate all required fields
        validateRca(rcaRequest);

        // Calculate MTTR
        Instant start = workItem.getFirstSignalAt();
        Instant end = rcaRequest.getIncidentEndTime();
        long mttrMinutes = ChronoUnit.MINUTES.between(start, end);

        // Build and save RCA
        Rca rca = Rca.builder()
            .workItemId(workItemId)
            .rootCauseCategory(rcaRequest.getRootCauseCategory())
            .rootCauseDescription(rcaRequest.getRootCauseDescription())
            .fixApplied(rcaRequest.getFixApplied())
            .preventionSteps(rcaRequest.getPreventionSteps())
            .incidentStartTime(rcaRequest.getIncidentStartTime())
            .incidentEndTime(rcaRequest.getIncidentEndTime())
            .mttrMinutes((int) mttrMinutes)
            .submittedBy(rcaRequest.getSubmittedBy())
            .submittedAt(Instant.now())
            .isComplete(true)
            .build();

        Rca saved = rcaRepository.save(rca);
        log.info("RCA submitted for WorkItem: {}. MTTR: {} minutes", workItemId, mttrMinutes);
        return saved;
    }

    public Rca getRcaByWorkItemId(UUID workItemId) {
        return rcaRepository.findByWorkItemId(workItemId)
            .orElseThrow(() -> new RuntimeException("RCA not found for WorkItem: " + workItemId));
    }

    // This is the method unit tests will test
    public void validateRca(Rca rca) {
        if (rca.getRootCauseCategory() == null) {
            throw new IllegalArgumentException("Root cause category is required.");
        }
        if (rca.getRootCauseDescription() == null || rca.getRootCauseDescription().isBlank()) {
            throw new IllegalArgumentException("Root cause description is required.");
        }
        if (rca.getFixApplied() == null || rca.getFixApplied().isBlank()) {
            throw new IllegalArgumentException("Fix applied is required.");
        }
        if (rca.getPreventionSteps() == null || rca.getPreventionSteps().isBlank()) {
            throw new IllegalArgumentException("Prevention steps are required.");
        }
        if (rca.getIncidentStartTime() == null) {
            throw new IllegalArgumentException("Incident start time is required.");
        }
        if (rca.getIncidentEndTime() == null) {
            throw new IllegalArgumentException("Incident end time is required.");
        }
        if (rca.getSubmittedBy() == null || rca.getSubmittedBy().isBlank()) {
            throw new IllegalArgumentException("Submitted by is required.");
        }
    }
}
