package com.ims.backend.repository.mongo;

import com.ims.backend.model.Signal;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface SignalRepository extends MongoRepository<Signal, String> {
    List<Signal> findByWorkItemId(String workItemId);
    long countByWorkItemId(String workItemId);
    long countByReceivedAtAfter(Instant time);
}
