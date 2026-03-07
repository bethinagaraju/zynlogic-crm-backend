package com.example.crm.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.List;

import com.example.crm.model.EmailLead;
import com.example.crm.repository.EmailLeadRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class InstantlyMemoryService {

    private final String apiKey;
    private final String baseUrl;
    private final HttpClient httpClient;
    private final ObjectMapper mapper = new ObjectMapper();
    private final EmailLeadRepository repository;

    public InstantlyMemoryService(@Value("${instantly.api.key}") String apiKey,
            @Value("${instantly.api.base-url:https://api.instantly.ai}") String baseUrl,
            EmailLeadRepository repository) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        this.repository = repository;
    }

    public List<EmailLead> getAllLeads() {
        return repository.findAll();
    }

    public int syncAllEmails() throws Exception {
        String nextToken = null;
        int added = 0;
        do {
            URI uri = URI.create(baseUrl + "/api/v2/emails?limit=100&email_type=received"
                    + (nextToken != null ? "&starting_after=" + nextToken : ""));

            HttpRequest req = HttpRequest.newBuilder(uri)
                    .GET()
                    .header("Accept", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .timeout(Duration.ofSeconds(30))
                    .build();

            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() >= 400) {
                throw new IllegalStateException("Instantly API returned status " + resp.statusCode() + ": "
                        + resp.body());
            }

            JsonNode root = mapper.readTree(resp.body());

            // Find first array node containing items
            JsonNode itemsNode = null;
            if (root.isArray()) {
                itemsNode = root;
            } else {
                if (root.has("data")) {
                    itemsNode = root.get("data");
                } else if (root.has("emails")) {
                    itemsNode = root.get("emails");
                } else {
                    Iterator<JsonNode> it = root.elements();
                    while (it.hasNext()) {
                        JsonNode n = it.next();
                        if (n.isArray()) {
                            itemsNode = n;
                            break;
                        }
                    }
                }
            }

            if (itemsNode != null && itemsNode.isArray()) {
                for (JsonNode item : itemsNode) {
                    String id = item.path("id").asText(null);
                    if (id == null) continue;

                    EmailLead lead = new EmailLead();
                    lead.setId(id);

                    String tsCreated = item.path("timestamp_created").asText(null);
                    if (tsCreated != null && !tsCreated.isEmpty()) {
                        try { lead.setTimestampCreated(Instant.parse(tsCreated)); } catch (Exception ex) { }
                    }
                    String tsEmail = item.path("timestamp_email").asText(null);
                    if (tsEmail != null && !tsEmail.isEmpty()) {
                        try { lead.setTimestampEmail(Instant.parse(tsEmail)); } catch (Exception ex) { }
                    }

                    lead.setOrganizationId(item.path("organization_id").asText(null));
                    lead.setEaccount(item.path("eaccount").asText(null));
                    lead.setFromAddressEmail(item.path("from_address_email").asText(null));
                    lead.setCampaignId(item.path("campaign_id").asText(null));
                    lead.setLead(item.path("lead").asText(null));

                    if (item.has("ue_type") && item.get("ue_type").canConvertToInt()) {
                        lead.setUeType(item.path("ue_type").asInt());
                    }
                    lead.setStep(item.path("step").asText(null));
                    if (item.has("is_unread") && item.get("is_unread").canConvertToInt()) {
                        lead.setIsUnread(item.path("is_unread").asInt());
                    }
                    if (item.has("ai_interest_value") && item.get("ai_interest_value").canConvertToInt()) {
                        lead.setAiInterestValue(item.path("ai_interest_value").asInt());
                    }
                    if (item.has("is_focused") && item.get("is_focused").canConvertToInt()) {
                        lead.setIsFocused(item.path("is_focused").asInt());
                    }
                    if (item.has("i_status") && item.get("i_status").canConvertToInt()) {
                        lead.setIStatus(item.path("i_status").asInt());
                    }
                    lead.setThreadId(item.path("thread_id").asText(null));

                    if (!repository.existsById(id)) {
                        repository.save(lead);
                        added++;
                    }
                }
            }

            // Look for next token in common fields
            String token = null;
            if (root.has("next_starting_after")) {
                token = root.path("next_starting_after").asText(null);
            }
            if (token == null && root.has("next")) {
                token = root.path("next").path("starting_after").asText(null);
            }
            if (token == null && root.has("starting_after")) {
                token = root.path("starting_after").asText(null);
            }

            nextToken = (token == null || token.isEmpty()) ? null : token;

            if (nextToken == null) break;

        } while (true);

        return added;
    }
}
