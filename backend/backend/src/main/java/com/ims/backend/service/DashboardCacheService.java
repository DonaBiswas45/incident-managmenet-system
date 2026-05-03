package com.ims.backend.service;

import com.ims.backend.model.WorkItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardCacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String ACTIVE_INCIDENTS_KEY = "dashboard:active_incidents";
    private static final String STATS_KEY = "dashboard:stats";

    // Update active incidents in Redis
    public void updateActiveIncidents(List<WorkItem> workItems) {
        try {
            redisTemplate.opsForValue().set(
                ACTIVE_INCIDENTS_KEY,
                workItems,
                Duration.ofSeconds(30)
            );
            log.debug("Updated Redis dashboard cache with {} incidents", workItems.size());
        } catch (Exception e) {
            log.error("Failed to update Redis cache: {}", e.getMessage());
        }
    }

    // Get active incidents from Redis
    @SuppressWarnings("unchecked")
    public List<WorkItem> getActiveIncidents() {
        try {
            Object cached = redisTemplate.opsForValue().get(ACTIVE_INCIDENTS_KEY);
            if (cached != null) {
                log.debug("Cache HIT for active incidents");
                return (List<WorkItem>) cached;
            }
        } catch (Exception e) {
            log.error("Failed to read Redis cache: {}", e.getMessage());
        }
        log.debug("Cache MISS for active incidents");
        return null;
    }

    // Update stats
    public void updateStats(long totalOpen, long totalP0, int bufferSize) {
        try {
            java.util.Map<String, Object> stats = java.util.Map.of(
                "totalOpen", totalOpen,
                "totalP0", totalP0,
                "bufferSize", bufferSize,
                "updatedAt", java.time.Instant.now().toString()
            );
            redisTemplate.opsForValue().set(
                STATS_KEY,
                stats,
                Duration.ofSeconds(10)
            );
        } catch (Exception e) {
            log.error("Failed to update Redis stats: {}", e.getMessage());
        }
    }

    public void invalidate() {
        try {
            redisTemplate.delete(ACTIVE_INCIDENTS_KEY);
            redisTemplate.delete(STATS_KEY);
        } catch (Exception e) {
            log.error("Failed to invalidate Redis cache: {}", e.getMessage());
        }
    }
}
