package com.ims.backend.model;

import com.ims.backend.enums.ComponentType;
import com.ims.backend.enums.Priority;
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
@Table(name = "work_items")
public class WorkItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String componentId;

    @Enumerated(EnumType.STRING)
    private ComponentType componentType;

    private String title;

    @Enumerated(EnumType.STRING)
    private Priority priority;

    @Enumerated(EnumType.STRING)
    private WorkItemStatus status;

    private Integer signalCount;
    private String assignedTo;
    private Instant firstSignalAt;
    private Instant lastSignalAt;
    private Instant createdAt;
    private Instant updatedAt;
}
