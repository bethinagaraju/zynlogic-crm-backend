package com.example.crm.controller;

import java.time.Instant;
import java.util.Map;

import com.example.crm.model.LeadStage;
import com.example.crm.repository.LeadStageRepository;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LeadStageController {

    private final LeadStageRepository leadStageRepository;

    public LeadStageController(LeadStageRepository leadStageRepository) {
        this.leadStageRepository = leadStageRepository;
    }

    public static class DueAtRequest {
        private String dueAt;
        public String getDueAt() { return dueAt; }
        public void setDueAt(String dueAt) { this.dueAt = dueAt; }
    }

    @PutMapping(value = "/api/leads/{emailLeadId}/stages/{stageIndex}/due", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> updateDueAt(@PathVariable String emailLeadId,
                                         @PathVariable Integer stageIndex,
                                         @RequestBody DueAtRequest req) {
        if (req == null || req.getDueAt() == null || req.getDueAt().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "dueAt is required"));
        }

        Instant due;
        try {
            due = Instant.parse(req.getDueAt());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid dueAt format. Use ISO-8601 instant, e.g. 2026-06-20T10:00:00Z"));
        }

        LeadStage stage = leadStageRepository.findByEmailLead_IdAndStageIndex(emailLeadId, stageIndex);
        if (stage == null) {
            return ResponseEntity.status(404).body(Map.of("error", "Stage not found"));
        }

        stage.setDueAt(due);
        leadStageRepository.save(stage);

        return ResponseEntity.ok(stage);
    }
}
