package com.ims.backend.repository.jpa;

import com.ims.backend.model.Rca;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RcaRepository extends JpaRepository<Rca, UUID> {
    Optional<Rca> findByWorkItemId(UUID workItemId);
    boolean existsByWorkItemIdAndIsCompleteTrue(UUID workItemId);
}
