package com.ims.backend.model;

import com.ims.backend.enums.WorkItemStatus;
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
@Table(name = "status_history")
public class StatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID workItemId;

    @Enumerated(EnumType.STRING)
    private WorkItemStatus fromStatus;

    @Enumerated(EnumType.STRING)
    private WorkItemStatus toStatus;

    private String changedBy;
    private Instant changedAt;
    private String note;
}
