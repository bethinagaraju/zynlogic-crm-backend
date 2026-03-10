package com.example.crm.controller;

import com.example.crm.service.InstantlyMemoryService;
import com.example.crm.model.EmailLead;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@RestController
public class GetLeadsController {

    private final InstantlyMemoryService memoryService;

    public GetLeadsController(InstantlyMemoryService memoryService) {
        this.memoryService = memoryService;
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

}
