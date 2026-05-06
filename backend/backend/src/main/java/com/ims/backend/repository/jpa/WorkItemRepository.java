package com.ims.backend.repository.jpa;

import com.ims.backend.enums.WorkItemStatus;
import com.ims.backend.model.WorkItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkItemRepository extends JpaRepository<WorkItem, UUID> {
     Optional<WorkItem> findByComponentIdAndStatusNot(String componentId, WorkItemStatus status);
    List<WorkItem> findByStatusOrderByCreatedAtDesc(WorkItemStatus status);
    List<WorkItem> findAllByOrderByCreatedAtDesc();
    Page<WorkItem> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
