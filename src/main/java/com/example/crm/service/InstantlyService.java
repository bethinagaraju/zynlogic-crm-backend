package com.example.crm.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.crm.model.Lead;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Service responsible for calling the Instantly.ai API to fetch leads and Unibox data.
 */
@Service
@Slf4j
public class InstantlyService {

    private final RestTemplate restTemplate;
    private final String apiKey;
    private final String baseUrl;
    private final String leadsPath;
    private final com.example.crm.repository.LeadRepository leadRepository;

    @Autowired
    public InstantlyService(RestTemplate restTemplate,
                            @Value("${instantly.api.key}") String apiKey,
                            @Value("${instantly.api.base-url:https://api.instantly.ai}") String baseUrl,
                            @Value("${instantly.api.leads-path:/api/v2/leads}") String leadsPath,
                            com.example.crm.repository.LeadRepository leadRepository) {
        this.restTemplate = restTemplate;
        this.apiKey = apiKey;
        this.baseUrl = baseUrl != null ? baseUrl.replaceAll("/$", "") : "";
        this.leadsPath = leadsPath != null ? leadsPath : "/api/v2/leads";
        this.leadRepository = leadRepository;
    }

    private String leadsUrl() {
        String path = leadsPath.startsWith("/") ? leadsPath : "/" + leadsPath;
        return baseUrl + path;
    }

    public ResponseEntity<String> fetchAllLeads() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>("{}", headers);

        String url = leadsUrl();
        log.info("Calling Instantly API (leads): {}", url);
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

            // Attempt to persist each lead item into local DB as raw JSON
            try {
                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    ObjectMapper mapper = new ObjectMapper();
                    JsonNode root = mapper.readTree(response.getBody());
                    JsonNode items = root.get("items");
                    if (items != null && items.isArray()) {
                        for (JsonNode item : items) {
                            JsonNode idNode = item.get("id");
                            if (idNode == null || idNode.isNull() || idNode.asText().isBlank()) continue;
                            String id = idNode.asText();

                            // Build values from payload
                            Lead incoming = new Lead();
                            incoming.setId(id);
                            incoming.setRawJson(item.toString());
                            incoming.setOrganization(getText(item, "organization"));
                            incoming.setCampaign(getText(item, "campaign"));
                            incoming.setEmail(getText(item, "email"));
                            incoming.setFirstName(getText(item, "first_name"));
                            incoming.setCompanyDomain(getText(item, "company_domain"));
                            incoming.setStatus(getInt(item, "status"));
                            incoming.setEmailOpenCount(getInt(item, "email_open_count"));
                            incoming.setEmailReplyCount(getInt(item, "email_reply_count"));
                            incoming.setEmailClickCount(getInt(item, "email_click_count"));
                            incoming.setEspCode(getInt(item, "esp_code"));
                            incoming.setUploadMethod(getText(item, "upload_method"));

                            JsonNode statusSummary = item.get("status_summary");
                            if (statusSummary != null && !statusSummary.isNull()) {
                                JsonNode lastStep = statusSummary.get("lastStep");
                                if (lastStep != null && !lastStep.isNull()) {
                                    incoming.setLastStepId(getText(lastStep, "stepID"));
                                    incoming.setLastStepFrom(getText(lastStep, "from"));
                                    incoming.setLastStepExecutedAt(parseTimestamp(getText(lastStep, "timestamp_executed")));
                                }
                            }

                            JsonNode payload = item.get("payload");
                            if (payload != null && !payload.isNull()) {
                                incoming.setLocation(getText(payload, "location"));
                                incoming.setRoleOffer(getText(payload, "ROLE_OFFER"));
                                incoming.setTimezoneRegion(getText(payload, "TIMEZONE_REGION"));
                                incoming.setUniversityName(getText(payload, "UNIVERSITY_NAME"));
                                incoming.setPublicationTitle(getText(payload, "PUBLICATION_TITLE"));
                                incoming.setPublicationTitleShort(getText(payload, "PUBLICATION_TITLE_SHORT"));
                            }

                            incoming.setTimestampCreated(parseTimestamp(getText(item, "timestamp_created")));
                            incoming.setTimestampUpdated(parseTimestamp(getText(item, "timestamp_updated")));
                            incoming.setTimestampLastContact(parseTimestamp(getText(item, "timestamp_last_contact")));
                            incoming.setTimestampLastTouch(parseTimestamp(getText(item, "timestamp_last_touch")));
                            incoming.setSyncedAt(LocalDateTime.now());

                            try {
                                // Upsert: if exists, update fields; otherwise insert
                                leadRepository.findById(id).map(existing -> {
                                    // update fields on existing to preserve any local-only data
                                    existing.setRawJson(incoming.getRawJson());
                                    existing.setOrganization(incoming.getOrganization());
                                    existing.setCampaign(incoming.getCampaign());
                                    existing.setEmail(incoming.getEmail());
                                    existing.setFirstName(incoming.getFirstName());
                                    existing.setCompanyDomain(incoming.getCompanyDomain());
                                    existing.setStatus(incoming.getStatus());
                                    existing.setEmailOpenCount(incoming.getEmailOpenCount());
                                    existing.setEmailReplyCount(incoming.getEmailReplyCount());
                                    existing.setEmailClickCount(incoming.getEmailClickCount());
                                    existing.setEspCode(incoming.getEspCode());
                                    existing.setUploadMethod(incoming.getUploadMethod());
                                    existing.setLastStepId(incoming.getLastStepId());
                                    existing.setLastStepFrom(incoming.getLastStepFrom());
                                    existing.setLastStepExecutedAt(incoming.getLastStepExecutedAt());
                                    existing.setLocation(incoming.getLocation());
                                    existing.setRoleOffer(incoming.getRoleOffer());
                                    existing.setTimezoneRegion(incoming.getTimezoneRegion());
                                    existing.setUniversityName(incoming.getUniversityName());
                                    existing.setPublicationTitle(incoming.getPublicationTitle());
                                    existing.setPublicationTitleShort(incoming.getPublicationTitleShort());
                                    existing.setTimestampCreated(incoming.getTimestampCreated());
                                    existing.setTimestampUpdated(incoming.getTimestampUpdated());
                                    existing.setTimestampLastContact(incoming.getTimestampLastContact());
                                    existing.setTimestampLastTouch(incoming.getTimestampLastTouch());
                                    existing.setSyncedAt(incoming.getSyncedAt());
                                    return leadRepository.save(existing);
                                }).orElseGet(() -> leadRepository.save(incoming));
                            } catch (Exception e) {
                                log.warn("Failed to persist lead {}: {}", id, e.getMessage());
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to parse/persist Instantly leads response: {}", e.getMessage());
            }

            return response;
        } catch (HttpClientErrorException e) {
            log.warn("Instantly API returned error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Unexpected error calling Instantly API (leads)", e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("{\"error\":\"upstream_error\"}");
        }
    }

    /**
     * Paginated sync: repeatedly call Instantly leads API with limit=100 and
     * starting_after cursor until no more pages, upserting leads into local DB.
     */
    public ResponseEntity<String> syncAllLeads() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>("{}", headers);

        ObjectMapper mapper = new ObjectMapper();
        String startingAfter = null;
        int page = 1;
        int totalProcessed = 0;

        try {
            while (true) {
                String url = leadsUrl() + "?limit=100";
                if (startingAfter != null) url += "&starting_after=" + URLEncoder.encode(startingAfter, StandardCharsets.UTF_8);

                log.info("Syncing Instantly leads page {}: {}", page, url);
                ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

                if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) break;

                JsonNode root = mapper.readTree(response.getBody());
                JsonNode items = root.get("items");
                if (items == null || !items.isArray() || items.size() == 0) break;

                for (JsonNode item : items) {
                    JsonNode idNode = item.get("id");
                    if (idNode == null || idNode.isNull() || idNode.asText().isBlank()) continue;
                    Lead incoming = mapJsonToLead(item);
                    try {
                        upsertLead(incoming);
                        totalProcessed++;
                    } catch (Exception e) {
                        log.warn("Failed to upsert lead {}: {}", incoming.getId(), e.getMessage());
                    }
                }

                JsonNode nextCursor = root.get("next_starting_after");
                if (nextCursor == null || nextCursor.isNull() || nextCursor.asText().isBlank()) break;
                startingAfter = nextCursor.asText();
                page++;
            }

            String result = mapper.writeValueAsString(Map.of("total_processed", totalProcessed));
            return ResponseEntity.ok(result);
        } catch (HttpClientErrorException e) {
            log.warn("Instantly API returned error during sync: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Unexpected error during Instantly leads sync", e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("{\"error\":\"upstream_error\"}");
        }
    }

    private Lead mapJsonToLead(JsonNode item) {
        Lead incoming = new Lead();
        String id = getText(item, "id");
        incoming.setId(id);
        incoming.setRawJson(item.toString());
        incoming.setOrganization(getText(item, "organization"));
        incoming.setCampaign(getText(item, "campaign"));
        incoming.setEmail(getText(item, "email"));
        incoming.setFirstName(getText(item, "first_name"));
        incoming.setCompanyDomain(getText(item, "company_domain"));
        incoming.setStatus(getInt(item, "status"));
        incoming.setEmailOpenCount(getInt(item, "email_open_count"));
        incoming.setEmailReplyCount(getInt(item, "email_reply_count"));
        incoming.setEmailClickCount(getInt(item, "email_click_count"));
        incoming.setEspCode(getInt(item, "esp_code"));
        incoming.setUploadMethod(getText(item, "upload_method"));

        JsonNode statusSummary = item.get("status_summary");
        if (statusSummary != null && !statusSummary.isNull()) {
            JsonNode lastStep = statusSummary.get("lastStep");
            if (lastStep != null && !lastStep.isNull()) {
                incoming.setLastStepId(getText(lastStep, "stepID"));
                incoming.setLastStepFrom(getText(lastStep, "from"));
                incoming.setLastStepExecutedAt(parseTimestamp(getText(lastStep, "timestamp_executed")));
            }
        }

        JsonNode payload = item.get("payload");
        if (payload != null && !payload.isNull()) {
            incoming.setLocation(getText(payload, "location"));
            incoming.setRoleOffer(getText(payload, "ROLE_OFFER"));
            incoming.setTimezoneRegion(getText(payload, "TIMEZONE_REGION"));
            incoming.setUniversityName(getText(payload, "UNIVERSITY_NAME"));
            incoming.setPublicationTitle(getText(payload, "PUBLICATION_TITLE"));
            incoming.setPublicationTitleShort(getText(payload, "PUBLICATION_TITLE_SHORT"));
        }

        incoming.setTimestampCreated(parseTimestamp(getText(item, "timestamp_created")));
        incoming.setTimestampUpdated(parseTimestamp(getText(item, "timestamp_updated")));
        incoming.setTimestampLastContact(parseTimestamp(getText(item, "timestamp_last_contact")));
        incoming.setTimestampLastTouch(parseTimestamp(getText(item, "timestamp_last_touch")));
        incoming.setSyncedAt(LocalDateTime.now());
        return incoming;
    }

    private void upsertLead(Lead incoming) {
        if (incoming == null || incoming.getId() == null) return;
        leadRepository.findById(incoming.getId()).map(existing -> {
            existing.setRawJson(incoming.getRawJson());
            existing.setOrganization(incoming.getOrganization());
            existing.setCampaign(incoming.getCampaign());
            existing.setEmail(incoming.getEmail());
            existing.setFirstName(incoming.getFirstName());
            existing.setCompanyDomain(incoming.getCompanyDomain());
            existing.setStatus(incoming.getStatus());
            existing.setEmailOpenCount(incoming.getEmailOpenCount());
            existing.setEmailReplyCount(incoming.getEmailReplyCount());
            existing.setEmailClickCount(incoming.getEmailClickCount());
            existing.setEspCode(incoming.getEspCode());
            existing.setUploadMethod(incoming.getUploadMethod());
            existing.setLastStepId(incoming.getLastStepId());
            existing.setLastStepFrom(incoming.getLastStepFrom());
            existing.setLastStepExecutedAt(incoming.getLastStepExecutedAt());
            existing.setLocation(incoming.getLocation());
            existing.setRoleOffer(incoming.getRoleOffer());
            existing.setTimezoneRegion(incoming.getTimezoneRegion());
            existing.setUniversityName(incoming.getUniversityName());
            existing.setPublicationTitle(incoming.getPublicationTitle());
            existing.setPublicationTitleShort(incoming.getPublicationTitleShort());
            existing.setTimestampCreated(incoming.getTimestampCreated());
            existing.setTimestampUpdated(incoming.getTimestampUpdated());
            existing.setTimestampLastContact(incoming.getTimestampLastContact());
            existing.setTimestampLastTouch(incoming.getTimestampLastTouch());
            existing.setSyncedAt(incoming.getSyncedAt());
            return leadRepository.save(existing);
        }).orElseGet(() -> leadRepository.save(incoming));
    }

    private static String getText(JsonNode node, String field) {
        if (node == null || node.isNull()) return null;
        JsonNode v = node.get(field);
        return v == null || v.isNull() ? null : v.asText();
    }

    private static Integer getInt(JsonNode node, String field) {
        String t = getText(node, field);
        if (t == null || t.isBlank()) return null;
        try { return Integer.valueOf(t); } catch (NumberFormatException e) { return null; }
    }

    private static LocalDateTime parseTimestamp(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return OffsetDateTime.parse(s).toLocalDateTime();
        } catch (DateTimeParseException e) {
            try {
                return LocalDateTime.parse(s);
            } catch (Exception ex) {
                return null;
            }
        }
    }

    /**
     * Fetch Unibox replies (paginated) using GET /api/v2/emails?ue_type=1
     */
    public ResponseEntity<String> fetchUniboxEmails(int limit) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        List<Object> allEmails = new ArrayList<>();
        String startingAfter = null;
        int page = 1;
        ObjectMapper mapper = new ObjectMapper();

        try {
            while (true) {
                String url = baseUrl + "/api/v2/emails?limit=" + Math.min(limit, 100) + "&ue_type=1";
                if (startingAfter != null) url += "&starting_after=" + URLEncoder.encode(startingAfter, StandardCharsets.UTF_8);

                log.info("Fetching Unibox page {}: {}", page, url);
                ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

                JsonNode root = mapper.readTree(response.getBody());
                JsonNode items = root.get("items");
                if (items == null || !items.isArray() || items.size() == 0) break;

                for (JsonNode item : items) allEmails.add(item);

                JsonNode nextCursor = root.get("next_starting_after");
                if (nextCursor == null || nextCursor.isNull() || nextCursor.asText().isBlank()) break;
                startingAfter = nextCursor.asText();
                page++;
                if (allEmails.size() >= limit) break;
            }

            String result = mapper.writeValueAsString(Map.of("total", allEmails.size(), "items", allEmails));
            return ResponseEntity.ok(result);
        } catch (HttpClientErrorException e) {
            log.warn("Instantly API error (Unibox): {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Unexpected error fetching Unibox emails", e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("{\"error\":\"upstream_error\"}");
        }
    }

    /**
     * Fetch a single email thread by thread_id.
     */
    public ResponseEntity<String> fetchEmailThread(String threadId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            String encoded = URLEncoder.encode(threadId == null ? "" : threadId, StandardCharsets.UTF_8);
            String url = baseUrl + "/api/v2/emails?thread_id=" + encoded;
            log.info("Fetching Instantly email thread: {}", url);
            return restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
        } catch (HttpClientErrorException e) {
            log.warn("Instantly API returned error fetching thread: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Unexpected error fetching Instantly email thread", e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("{\"error\":\"upstream_error\"}");
        }
    }

    /**
     * Fetch the thread_id for a lead by email using Instantly.ai: /api/v2/emails?email={email}&limit=1
     */
    public ResponseEntity<String> fetchThreadIdByEmail(String email) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            String encoded = URLEncoder.encode(email == null ? "" : email, StandardCharsets.UTF_8);
            String url = baseUrl + "/api/v2/emails?email=" + encoded + "&limit=1";
            log.info("Fetching thread id by email from Instantly: {}", url);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response.getBody());
            JsonNode items = root.get("items");
            if (items != null && items.isArray() && items.size() > 0) {
                JsonNode first = items.get(0);
                JsonNode threadNode = first.get("thread_id");
                String threadId = threadNode != null && !threadNode.isNull() ? threadNode.asText() : null;
                if (threadId != null && !threadId.isBlank()) {
                    String result = mapper.writeValueAsString(Map.of("thread_id", threadId));
                    return ResponseEntity.ok(result);
                }
            }

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("{\"message\":\"thread_not_found\"}");
        } catch (HttpClientErrorException e) {
            log.warn("Instantly API returned error fetching by email: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Unexpected error fetching thread id by email", e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("{\"error\":\"upstream_error\"}");
        }
    }

}
