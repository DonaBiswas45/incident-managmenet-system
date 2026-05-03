package com.ims.backend.controller;

import com.ims.backend.model.Rca;
import com.ims.backend.service.RcaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/rca")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class RcaController {

    private final RcaService rcaService;

    @PostMapping("/{workItemId}")
    public ResponseEntity<Rca> submitRca(
            @PathVariable UUID workItemId,
            @RequestBody Rca rca) {
        return ResponseEntity.ok(rcaService.submitRca(workItemId, rca));
    }

    @GetMapping("/{workItemId}")
    public ResponseEntity<Rca> getRca(@PathVariable UUID workItemId) {
        return ResponseEntity.ok(rcaService.getRcaByWorkItemId(workItemId));
    }

    @GetMapping("/{workItemId}/mttr")
    public ResponseEntity<Map<String, Object>> getMttr(@PathVariable UUID workItemId) {
        Rca rca = rcaService.getRcaByWorkItemId(workItemId);
        return ResponseEntity.ok(Map.of(
            "workItemId", workItemId,
            "mttrMinutes", rca.getMttrMinutes(),
            "incidentStartTime", rca.getIncidentStartTime(),
            "incidentEndTime", rca.getIncidentEndTime()
        ));
    }
}
