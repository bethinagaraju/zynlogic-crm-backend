package com.example.crm.controller;

import com.example.crm.service.InstantlyMemoryService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;

@RestController
@RequestMapping("/api/webhook")
public class WebhookController {

    private final InstantlyMemoryService memoryService;

    @Value("${instantly.webhook.id:019d0057-7243-7c8a-8bf3-cf4c688630ed}")
    private String expectedWebhookId;

    public WebhookController(InstantlyMemoryService memoryService) {
        this.memoryService = memoryService;
    }

    @PostMapping(value = "/instantly", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> handleInstantlyWebhook(@RequestHeader(value = "X-Webhook-Id", required = false) String headerId,
                                                    @RequestBody String body) {
        // Basic validation: if a webhook id header is present, require it to match configured value
        if (headerId != null && !headerId.isBlank() && !headerId.equals(expectedWebhookId)) {
            return ResponseEntity.status(403).body(Map.of("error", "webhook id mismatch"));
        }

        int processed = memoryService.processWebhookPayload(body);

        // If webhook includes an Instantly email id, fetch the full email and save it
        int fetched = 0;
        try {
            com.fasterxml.jackson.databind.JsonNode root = new com.fasterxml.jackson.databind.ObjectMapper().readTree(body);
            String emailId = null;
            if (root.has("email_id")) emailId = root.path("email_id").asText(null);
            // also support nested reply.email_id shapes
            if ((emailId == null || emailId.isBlank()) && root.has("reply") && root.get("reply").has("email_id")) {
                emailId = root.get("reply").path("email_id").asText(null);
            }

            if (emailId != null && !emailId.isBlank()) {
                try {
                    memoryService.fetchEmailByIdAndSave(emailId);
                    fetched = 1;
                } catch (Exception ex) {
                    // ignore fetch errors but report fetched=0
                    fetched = 0;
                }
            }
            // Attempt enrichment by lead email as before
            String leadEmail = null;
            String campaignId = null;
            if (root.has("lead") && root.get("lead").has("email")) {
                leadEmail = root.get("lead").path("email").asText(null);
            }
            if (root.has("campaign") && root.get("campaign").has("id")) {
                campaignId = root.get("campaign").path("id").asText(null);
            }
            int enriched = 0;
            if (leadEmail != null && !leadEmail.isBlank()) {
                enriched = memoryService.enrichAndSaveForLeadAndCampaign(leadEmail, campaignId);
            }
            return ResponseEntity.ok().body(Map.of("processed", processed, "fetched", fetched, "enriched", enriched));
        } catch (Exception ex) {
            return ResponseEntity.ok().body(Map.of("processed", processed, "fetched", 0, "enriched", 0));
        }
    }
}
