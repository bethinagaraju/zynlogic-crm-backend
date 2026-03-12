package com.example.crm.controller;

import com.example.crm.service.InstantlyMemoryService;
import com.example.crm.model.EmailLead;
import com.example.crm.repository.EmailLeadRepository;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Comparator;

@RestController
public class GetLeadsController {

    private final InstantlyMemoryService memoryService;
    private final EmailLeadRepository repository;

    public GetLeadsController(InstantlyMemoryService memoryService, EmailLeadRepository repository) {
        this.memoryService = memoryService;
        this.repository = repository;
    }
    @GetMapping("/sync")
    public ResponseEntity<String> sync() {
        try {
            int count = memoryService.syncAllEmails();
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON)
                    .body("{\"stored\":" + count + "}");
        } catch (Exception e) {
            return ResponseEntity.status(500).contentType(MediaType.APPLICATION_JSON)
                    .body("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    @GetMapping("/get-leads")
    public ResponseEntity<List<EmailLead>> getLeads() {
        List<EmailLead> leads = memoryService.getAllLeads();
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(leads);
    }

    @GetMapping("/get-leads/due-soon")
    public ResponseEntity<List<EmailLead>> getLeadsDueSoon() {
        List<EmailLead> leads = memoryService.getAllLeads();
        leads.sort(Comparator.comparing(EmailLead::getDueAt, Comparator.nullsLast(Comparator.naturalOrder())));
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(leads);
    }

    @GetMapping("/get-conversation-by-thread-id")
    public ResponseEntity<String> getConversationByThreadId(@RequestParam("threadId") String threadId) {
        try {
            String json = memoryService.getConversationByThreadId(threadId);
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(json);
        } catch (Exception e) {
            return ResponseEntity.status(500).contentType(MediaType.APPLICATION_JSON)
                    .body("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    public static class CurrentStageRequest {
        private Integer currentStage;
        public Integer getCurrentStage() { return currentStage; }
        public void setCurrentStage(Integer currentStage) { this.currentStage = currentStage; }
    }

    @PutMapping("/email-leads/{id}/current-stage")
    public ResponseEntity<?> updateCurrentStage(@PathVariable String id, @RequestBody CurrentStageRequest req) {
        if (req == null || req.getCurrentStage() == null) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", "currentStage is required"));
        }
        java.util.Optional<EmailLead> opt = repository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.status(404).body(java.util.Map.of("error", "EmailLead not found"));
        EmailLead lead = opt.get();
        lead.setCurrentStage(req.getCurrentStage());
        repository.save(lead);
        return ResponseEntity.ok().body(lead);
    }

    public static class DueDateRequest {
        private String dueDate;
        public String getDueDate() { return dueDate; }
        public void setDueDate(String dueDate) { this.dueDate = dueDate; }
    }

    @PutMapping("/email-leads/{id}/due-date")
    public ResponseEntity<?> updateDueDate(@PathVariable String id, @RequestBody DueDateRequest req) {
        if (req == null || req.getDueDate() == null || req.getDueDate().isBlank()) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", "dueDate is required"));
        }
        java.time.Instant due;
        try {
            due = java.time.Instant.parse(req.getDueDate());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", "Invalid dueDate format. Use ISO-8601 instant, e.g. 2026-06-20T10:00:00Z"));
        }
        java.util.Optional<EmailLead> opt = repository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.status(404).body(java.util.Map.of("error", "EmailLead not found"));
        EmailLead lead = opt.get();
        lead.setDueDate(due);
        repository.save(lead);
        return ResponseEntity.ok().body(lead);
    }

}
