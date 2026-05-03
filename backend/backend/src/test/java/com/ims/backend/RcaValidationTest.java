package com.ims.backend;

import com.ims.backend.enums.RootCauseCategory;
import com.ims.backend.model.Rca;
import com.ims.backend.service.RcaService;
import com.ims.backend.repository.jpa.RcaRepository;
import com.ims.backend.repository.jpa.WorkItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class RcaValidationTest {

    private RcaService rcaService;

    @BeforeEach
    void setUp() {
        RcaRepository rcaRepository = mock(RcaRepository.class);
        WorkItemRepository workItemRepository = mock(WorkItemRepository.class);
        rcaService = new RcaService(rcaRepository, workItemRepository);
    }

    @Test
    void shouldPassValidation_whenAllFieldsPresent() {
        Rca rca = buildValidRca();
        assertDoesNotThrow(() -> rcaService.validateRca(rca));
    }

    @Test
    void shouldFail_whenRootCauseCategoryMissing() {
        Rca rca = buildValidRca();
        rca.setRootCauseCategory(null);
        Exception ex = assertThrows(IllegalArgumentException.class,
            () -> rcaService.validateRca(rca));
        assertEquals("Root cause category is required.", ex.getMessage());
    }

    @Test
    void shouldFail_whenRootCauseDescriptionBlank() {
        Rca rca = buildValidRca();
        rca.setRootCauseDescription("");
        Exception ex = assertThrows(IllegalArgumentException.class,
            () -> rcaService.validateRca(rca));
        assertEquals("Root cause description is required.", ex.getMessage());
    }

    @Test
    void shouldFail_whenFixAppliedBlank() {
        Rca rca = buildValidRca();
        rca.setFixApplied("");
        Exception ex = assertThrows(IllegalArgumentException.class,
            () -> rcaService.validateRca(rca));
        assertEquals("Fix applied is required.", ex.getMessage());
    }

    @Test
    void shouldFail_whenPreventionStepsBlank() {
        Rca rca = buildValidRca();
        rca.setPreventionSteps("");
        Exception ex = assertThrows(IllegalArgumentException.class,
            () -> rcaService.validateRca(rca));
        assertEquals("Prevention steps are required.", ex.getMessage());
    }

    @Test
    void shouldFail_whenStartTimeMissing() {
        Rca rca = buildValidRca();
        rca.setIncidentStartTime(null);
        Exception ex = assertThrows(IllegalArgumentException.class,
            () -> rcaService.validateRca(rca));
        assertEquals("Incident start time is required.", ex.getMessage());
    }

    @Test
    void shouldFail_whenEndTimeMissing() {
        Rca rca = buildValidRca();
        rca.setIncidentEndTime(null);
        Exception ex = assertThrows(IllegalArgumentException.class,
            () -> rcaService.validateRca(rca));
        assertEquals("Incident end time is required.", ex.getMessage());
    }

    @Test
    void shouldFail_whenSubmittedByBlank() {
        Rca rca = buildValidRca();
        rca.setSubmittedBy("");
        Exception ex = assertThrows(IllegalArgumentException.class,
            () -> rcaService.validateRca(rca));
        assertEquals("Submitted by is required.", ex.getMessage());
    }

    private Rca buildValidRca() {
        return Rca.builder()
            .rootCauseCategory(RootCauseCategory.SOFTWARE_BUG)
            .rootCauseDescription("Connection pool exhausted")
            .fixApplied("Increased pool size")
            .preventionSteps("Add monitoring alerts")
            .incidentStartTime(Instant.now().minusSeconds(3600))
            .incidentEndTime(Instant.now())
            .submittedBy("dona")
            .build();
    }
}
