package com.example.crm.controller;

import com.example.crm.model.EmailLead;
import com.example.crm.service.InstantlyMemoryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/instantly")
public class InstantlyController {

    private final InstantlyMemoryService memoryService;

    public InstantlyController(InstantlyMemoryService memoryService) {
        this.memoryService = memoryService;
    }

    @PostMapping("/fetch/{emailId}")
    public ResponseEntity<?> fetchEmailById(@PathVariable String emailId) {
        try {
            EmailLead lead = memoryService.fetchEmailByIdAndSave(emailId);
            return ResponseEntity.ok(lead);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }
}
