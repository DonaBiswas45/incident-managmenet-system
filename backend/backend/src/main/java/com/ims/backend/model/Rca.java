package com.ims.backend.model;

import com.ims.backend.enums.RootCauseCategory;
import jakarta.persistence.*;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "rca")
public class Rca {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true)
    private UUID workItemId;

    @Enumerated(EnumType.STRING)
    private RootCauseCategory rootCauseCategory;

    @Column(columnDefinition = "TEXT")
    private String rootCauseDescription;

    @Column(columnDefinition = "TEXT")
    private String fixApplied;

    @Column(columnDefinition = "TEXT")
    private String preventionSteps;

    private Instant incidentStartTime;
    private Instant incidentEndTime;
    private Integer mttrMinutes;
    private String submittedBy;
    private Instant submittedAt;
    private Boolean isComplete;
}
