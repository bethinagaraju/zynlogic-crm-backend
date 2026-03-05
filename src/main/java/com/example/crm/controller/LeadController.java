package com.example.crm.controller;

import com.example.crm.service.InstantlyService;
import com.example.crm.repository.LeadRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller that exposes an endpoint to retrieve leads from Instantly.ai.
 */
@RestController
@RequestMapping("/api")
public class LeadController {

    private final InstantlyService instantlyService;
    private final LeadRepository leadRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public LeadController(InstantlyService instantlyService, LeadRepository leadRepository) {
        this.instantlyService = instantlyService;
        this.leadRepository = leadRepository;
    }

    /**
     * GET /api/leads - proxies the Instantly.ai leads endpoint and returns raw JSON.
     */
    @GetMapping("/leads")
    public ResponseEntity<String> getLeads() {
        return instantlyService.fetchAllLeads();
    }

    /**
     * GET /api/db/leads - return leads stored in local DB in the same top-level shape.
     */
    @GetMapping("/db/leads")
    public ResponseEntity<String> getLocalLeads() {
        try {
            List<?> all = leadRepository.findAll();
            String body = objectMapper.writeValueAsString(Map.of("items", all));
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("{\"error\":\"could_not_read_db\"}");
        }
    }

    /** Trigger a full paginated sync from Instantly into local DB. */
    @GetMapping("/sync/leads")
    public ResponseEntity<String> syncLeads() {
        return instantlyService.syncAllLeads();
    }


//     @GetMapping("/unibox")
// public ResponseEntity<String> getUniboxEmails(
//         @RequestParam(required = false, defaultValue = "100") int limit) {
//     return instantlyService.fetchUniboxEmails(limit);
// }

}
