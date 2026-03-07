package com.example.crm.controller;

import com.example.crm.service.InstantlyMemoryService;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GetLeadsController {

    private final InstantlyMemoryService memoryService;
    private final ObjectMapper mapper = new ObjectMapper();

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
    public ResponseEntity<String> getLeads() throws Exception {
        String json = mapper.writeValueAsString(memoryService.getAllLeads());
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(json);
    }

}
