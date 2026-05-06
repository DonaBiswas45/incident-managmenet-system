package com.ims.backend.controller;

import com.ims.backend.model.Signal;
import com.ims.backend.repository.mongo.SignalRepository;
import com.ims.backend.service.SignalIngestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/signals")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SignalController {

    private final SignalIngestionService signalIngestionService;
    private final SignalRepository signalRepository;

    @PostMapping
    public ResponseEntity<String> ingestSignal(@Valid @RequestBody Signal signal) {
        boolean accepted = signalIngestionService.ingest(signal);
        if (!accepted) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body("Rate limit exceeded. Try again.");
        }
        return ResponseEntity.accepted().body("Signal queued");
    }

    @GetMapping("/workitem/{workItemId}")
    public ResponseEntity<List<Signal>> getSignalsByWorkItem(@PathVariable String workItemId) {
        return ResponseEntity.ok(signalRepository.findByWorkItemId(workItemId));
    }
}
