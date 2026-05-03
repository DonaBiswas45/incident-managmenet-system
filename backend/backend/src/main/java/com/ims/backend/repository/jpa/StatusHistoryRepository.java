package com.ims.backend.repository.jpa;

import com.ims.backend.model.StatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StatusHistoryRepository extends JpaRepository<StatusHistory, UUID> {
    List<StatusHistory> findByWorkItemIdOrderByChangedAtAsc(UUID workItemId);
}
