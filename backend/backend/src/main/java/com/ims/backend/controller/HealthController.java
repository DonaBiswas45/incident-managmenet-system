package com.ims.backend.controller;

import com.ims.backend.service.SignalIngestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class HealthController {

    private final SignalIngestionService signalIngestionService;
    private final DataSource dataSource;
    private final MongoTemplate mongoTemplate;
    private final RedisTemplate<String, Object> redisTemplate;

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new LinkedHashMap<>();
        health.put("status", "ok");
        health.put("bufferSize", signalIngestionService.getBufferSize());
        health.put("postgres", checkPostgres());
        health.put("mongodb", checkMongo());
        health.put("redis", checkRedis());
        return ResponseEntity.ok(health);
    }

    private String checkPostgres() {
        try {
            dataSource.getConnection().close();
            return "UP";
        } catch (Exception e) {
            return "DOWN";
        }
    }

    private String checkMongo() {
        try {
            mongoTemplate.getDb().runCommand(
                new org.bson.Document("ping", 1));
            return "UP";
        } catch (Exception e) {
            return "DOWN";
        }
    }

    private String checkRedis() {
        try {
            redisTemplate.getConnectionFactory()
                .getConnection().ping();
            return "UP";
        } catch (Exception e) {
            return "DOWN";
        }
    }
}
