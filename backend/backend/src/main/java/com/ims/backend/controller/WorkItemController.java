package com.ims.backend.controller;

import com.ims.backend.enums.WorkItemStatus;
import com.ims.backend.model.StatusHistory;
import com.ims.backend.model.WorkItem;
import com.ims.backend.service.WorkItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/workitems")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class WorkItemController {

    private final WorkItemService workItemService;

    @GetMapping
    public ResponseEntity<List<WorkItem>> getAllWorkItems(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        // Now the parameters are correctly placed in the method signature
        return ResponseEntity.ok(workItemService.getAllWorkItems(page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<WorkItem> getWorkItem(@PathVariable UUID id) {
        return ResponseEntity.ok(workItemService.getWorkItemById(id));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<WorkItem> updateStatus(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        WorkItemStatus newStatus = WorkItemStatus.valueOf(body.get("status"));
        String changedBy = body.getOrDefault("changedBy", "system");
        return ResponseEntity.ok(workItemService.updateStatus(id, newStatus, changedBy));
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<List<StatusHistory>> getHistory(@PathVariable UUID id) {
        return ResponseEntity.ok(workItemService.getStatusHistory(id));
    }
}
