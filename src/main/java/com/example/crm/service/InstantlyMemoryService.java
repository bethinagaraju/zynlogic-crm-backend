package com.example.crm.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Iterator;
import java.util.List;
import java.net.URLEncoder;

import com.example.crm.model.EmailLead;
import com.example.crm.model.LeadDetails;
import com.example.crm.repository.EmailLeadRepository;
import com.example.crm.repository.LeadDetailsRepository;
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
    private final LeadDetailsRepository detailsRepository;
    private final java.time.ZoneId appZoneId;

    public InstantlyMemoryService(@Value("${instantly.api.key}") String apiKey,
            @Value("${instantly.api.base-url:https://api.instantly.ai}") String baseUrl,
            EmailLeadRepository repository,
            LeadDetailsRepository detailsRepository,
            @Value("${app.timezone:Asia/Kolkata}") String appTimezone) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        this.repository = repository;
        this.detailsRepository = detailsRepository;
        this.appZoneId = java.time.ZoneId.of(appTimezone == null ? "Asia/Kolkata" : appTimezone);
    }

    public List<EmailLead> getAllLeads() {
        java.util.List<EmailLead> leads = repository.findAllByOrderByTimestampCreatedDesc();
        // No LeadStage tracking; return leads as stored
        for (EmailLead lead : leads) {
            // nothing to compute here
        }
        return leads;
    }

    public org.springframework.data.domain.Page<EmailLead> getLeads(org.springframework.data.domain.Pageable pageable) {
        return repository.findAllByOrderByTimestampCreatedDesc(pageable);
    }

    public org.springframework.data.domain.Page<EmailLead> getLeads(org.springframework.data.domain.Pageable pageable, String leadType) {
        if (leadType == null || leadType.isBlank()) {
            return getLeads(pageable);
        }
        return repository.findByLeadTypeOrderByTimestampCreatedDesc(leadType, pageable);
    }

    public org.springframework.data.domain.Page<EmailLead> getLeads(org.springframework.data.domain.Pageable pageable, String leadType, Integer currentStage) {
        boolean hasLeadType = leadType != null && !leadType.isBlank();
        boolean hasStage = currentStage != null;

        if (!hasLeadType && !hasStage) {
            return getLeads(pageable);
        }
        if (hasLeadType && !hasStage) {
            return getLeads(pageable, leadType);
        }
        if (!hasLeadType && hasStage) {
            return repository.findByCurrentStageOrderByTimestampCreatedDesc(currentStage, pageable);
        }
        // both provided
        return repository.findByLeadTypeAndCurrentStageOrderByTimestampCreatedDesc(leadType, currentStage, pageable);
    }

    public org.springframework.data.domain.Page<EmailLead> getLeads(org.springframework.data.domain.Pageable pageable, String leadType, Integer currentStage, String dueFilter) {
        org.springframework.data.jpa.domain.Specification<EmailLead> spec = org.springframework.data.jpa.domain.Specification.where(null);

        if (leadType != null && !leadType.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("leadType"), leadType));
        }
        if (currentStage != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("currentStage"), currentStage));
        }

        if (dueFilter != null && !dueFilter.isBlank()) {
            java.time.ZonedDateTime nowZ = java.time.ZonedDateTime.now(this.appZoneId);
            java.time.ZonedDateTime startOfToday = nowZ.toLocalDate().atStartOfDay(this.appZoneId);
            java.time.Instant startToday = startOfToday.toInstant();
            java.time.Instant startTomorrow = startOfToday.plusDays(1).toInstant();
            java.time.Instant startDayAfterTomorrow = startOfToday.plusDays(2).toInstant();
            java.time.Instant startThreeDays = startOfToday.plusDays(3).toInstant();
            java.time.Instant startFourDays = startOfToday.plusDays(4).toInstant();

            switch (dueFilter) {
                case "overdue":
                    spec = spec.and((root, query, cb) -> cb.and(cb.isNotNull(root.get("dueDate")), cb.lessThan(root.get("dueDate"), startToday)));
                    break;
                case "replyToday":
                    spec = spec.and((root, query, cb) -> cb.and(cb.greaterThanOrEqualTo(root.get("dueDate"), startToday), cb.lessThan(root.get("dueDate"), startTomorrow)));
                    break;
                case "1dayLeft":
                    spec = spec.and((root, query, cb) -> cb.and(cb.greaterThanOrEqualTo(root.get("dueDate"), startTomorrow), cb.lessThan(root.get("dueDate"), startDayAfterTomorrow)));
                    break;
                case "2daysLeft":
                    spec = spec.and((root, query, cb) -> cb.and(cb.greaterThanOrEqualTo(root.get("dueDate"), startDayAfterTomorrow), cb.lessThan(root.get("dueDate"), startThreeDays)));
                    break;
                case "3daysLeft":
                    spec = spec.and((root, query, cb) -> cb.and(cb.greaterThanOrEqualTo(root.get("dueDate"), startThreeDays), cb.lessThan(root.get("dueDate"), startFourDays)));
                    break;
                default:
                    break;
            }
        }

        return repository.findAll(spec, pageable);
    }

    public org.springframework.data.domain.Page<EmailLead> getLeads(org.springframework.data.domain.Pageable pageable, String leadType, Integer currentStage, String dueFilter, String tagsCsv, String tagsMode) {
        org.springframework.data.jpa.domain.Specification<EmailLead> spec = org.springframework.data.jpa.domain.Specification.where(null);

        if (leadType != null && !leadType.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("leadType"), leadType));
        }
        if (currentStage != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("currentStage"), currentStage));
        }

        if (dueFilter != null && !dueFilter.isBlank()) {
            java.time.ZonedDateTime nowZ = java.time.ZonedDateTime.now(this.appZoneId);
            java.time.ZonedDateTime startOfToday = nowZ.toLocalDate().atStartOfDay(this.appZoneId);
            java.time.Instant startToday = startOfToday.toInstant();
            java.time.Instant startTomorrow = startOfToday.plusDays(1).toInstant();
            java.time.Instant startDayAfterTomorrow = startOfToday.plusDays(2).toInstant();
            java.time.Instant startThreeDays = startOfToday.plusDays(3).toInstant();
            java.time.Instant startFourDays = startOfToday.plusDays(4).toInstant();

            switch (dueFilter) {
                case "overdue":
                    spec = spec.and((root, query, cb) -> cb.and(cb.isNotNull(root.get("dueDate")), cb.lessThan(root.get("dueDate"), startToday)));
                    break;
                case "replyToday":
                    spec = spec.and((root, query, cb) -> cb.and(cb.greaterThanOrEqualTo(root.get("dueDate"), startToday), cb.lessThan(root.get("dueDate"), startTomorrow)));
                    break;
                case "1dayLeft":
                    spec = spec.and((root, query, cb) -> cb.and(cb.greaterThanOrEqualTo(root.get("dueDate"), startTomorrow), cb.lessThan(root.get("dueDate"), startDayAfterTomorrow)));
                    break;
                case "2daysLeft":
                    spec = spec.and((root, query, cb) -> cb.and(cb.greaterThanOrEqualTo(root.get("dueDate"), startDayAfterTomorrow), cb.lessThan(root.get("dueDate"), startThreeDays)));
                    break;
                case "3daysLeft":
                    spec = spec.and((root, query, cb) -> cb.and(cb.greaterThanOrEqualTo(root.get("dueDate"), startThreeDays), cb.lessThan(root.get("dueDate"), startFourDays)));
                    break;
                default:
                    break;
            }
        }

        // Tags filter: comma-separated tag keys mapping to LeadDetails boolean fields.
        if (tagsCsv != null && !tagsCsv.isBlank()) {
            String[] tags = java.util.Arrays.stream(tagsCsv.split(","))
                    .map(String::trim).filter(s->!s.isEmpty()).toArray(String[]::new);
            boolean requireAll = tagsMode == null || !tagsMode.equalsIgnoreCase("any");

            spec = spec.and((root, query, cb) -> {
                jakarta.persistence.criteria.Join<Object, Object> join = root.join("details", jakarta.persistence.criteria.JoinType.LEFT);
                java.util.List<jakarta.persistence.criteria.Predicate> predicates = new java.util.ArrayList<>();
                for (String t : tags) {
                    String field = mapTagToField(t);
                    if (field == null) continue;
                    predicates.add(cb.isTrue(join.get(field)));
                }
                if (predicates.isEmpty()) return cb.conjunction();
                if (requireAll) {
                    return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
                } else {
                    return cb.or(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
                }
            });
        }

        return repository.findAll(spec, pageable);
    }

    // Map friendly tag names to LeadDetails boolean field names
    private static String mapTagToField(String tag) {
        if (tag == null) return null;
        String t = tag.trim().toLowerCase();
        return switch (t) {
            case "invitation", "invitationletter", "invitationlettersent", "invitation_letter_sent" -> "invitationLetterSent";
            case "abstract", "abstractreceived", "abstract_received" -> "abstractReceived";
            case "bio", "bioreceived", "bio_received" -> "bioReceived";
            case "photo", "photoreceived", "photo_received" -> "photoReceived";
            case "passport" -> "passport";
            case "pricing", "askedpricing", "asked_pricing" -> "askedPricing";
            case "travel", "askedtravelsupport", "asked_travel_support" -> "askedTravelSupport";
            case "feewaiver", "fee_waiver" -> "feeWaiver";
            case "virtual", "wantsvirtual", "wants_virtual" -> "wantsVirtual";
            case "inperson", "wantsinperson", "wants_in_person" -> "wantsInPerson";
            case "schedule", "scheduleconflict", "schedule_conflict" -> "scheduleConflict";
            case "needsapproval", "needs_approval" -> "needsApproval";
            case "student", "studentjoining", "student_joining" -> "studentJoining";
            case "onwebsite", "on_website" -> "onWebsite";
            case "reinvite", "reinvite_next_year" -> "reinviteNextYear";
            case "title", "titlesubmission", "title_submission" -> "titleSubmission";
            case "acceptance", "acceptancelettersent", "acceptance_letter_sent" -> "acceptanceLetterSent";
            case "registration", "registrationcompleted", "registration_completed" -> "registrationCompleted";
            default -> null;
        };
    }

    // Normalize leadType strings to canonical categories (handles variants and simple typos)
    private static String normalizeLeadType(String s) {
        if (s == null) return "Unknown";
        String t = s.trim();
        if (t.isEmpty()) return "Unknown";
        String key = t.toLowerCase().replaceAll("[^a-z0-9]", "");
        if (key.contains("dead")) return "Dead";
        // common patterns for Not Interested (handle hyphens, spaces, minor misspellings)
        if (key.contains("notinterested") || (key.contains("not") && (key.contains("interest") || key.contains("intrest") || key.contains("interested"))) || key.contains("nnot") ) return "Not Interested";
        if (key.contains("interested")) return "Interested";
        // default: title-case the original trimmed string
        return Character.toUpperCase(t.charAt(0)) + (t.length() > 1 ? t.substring(1) : "");
    }

    public org.springframework.data.domain.Page<EmailLead> getLeadsDueSoon(org.springframework.data.domain.Pageable pageable) {
        return repository.findAllOrderByDueDateNullsLast(pageable);
    }

    public org.springframework.data.domain.Page<EmailLead> getLeadsDueSoon(org.springframework.data.domain.Pageable pageable, String leadType) {
        if (leadType == null || leadType.isBlank()) {
            return getLeadsDueSoon(pageable);
        }
        return repository.findByLeadTypeOrderByDueDateNullsLast(leadType, pageable);
    }

    public org.springframework.data.domain.Page<EmailLead> getLeadsByFromAddress(String fromAddressEmail, org.springframework.data.domain.Pageable pageable) {
        return repository.findByFromAddressEmailOrderByTimestampCreatedDesc(fromAddressEmail, pageable);
    }

    public java.util.Map<String, Long> getLeadsStatistics() {
        long total = repository.count();
        java.time.ZonedDateTime nowZ = java.time.ZonedDateTime.now(this.appZoneId);
        java.time.ZonedDateTime startOfToday = nowZ.toLocalDate().atStartOfDay(this.appZoneId);
        java.time.ZonedDateTime startOfTomorrow = startOfToday.plusDays(1);
        java.time.ZonedDateTime startOfDayAfterTomorrow = startOfToday.plusDays(2);

        java.time.Instant nowInstant = java.time.Instant.now();
        // overdue should be dates before the start of today (in configured app timezone)
        long overdue = repository.countByDueDateBefore(startOfToday.toInstant());

        long replyToday = repository.countByDueDateBetween(startOfToday.toInstant(), startOfTomorrow.toInstant());
        long oneDayLeft = repository.countByDueDateBetween(startOfTomorrow.toInstant(), startOfDayAfterTomorrow.toInstant());

        long conversions = repository.countByCurrentStage(9);

        // Aggregate counts by leadType (grouped) and normalize variants/typos to canonical keys
        java.util.List<java.lang.Object[]> grouped = repository.countGroupByLeadType();
        java.util.Map<String, Long> normalized = new java.util.HashMap<>();
        if (grouped != null) {
            for (java.lang.Object[] row : grouped) {
                String lt = row[0] == null ? null : row[0].toString();
                long cnt = row[1] == null ? 0L : ((Number) row[1]).longValue();
                String key = normalizeLeadType(lt);
                normalized.put(key, normalized.getOrDefault(key, 0L) + cnt);
            }
        }
        long interested = normalized.getOrDefault("Interested", 0L);
        long notInterested = normalized.getOrDefault("Not Interested", 0L);
        long dead = normalized.getOrDefault("Dead", 0L);

        // LeadDetails-based tag counts
        long invitationLetterSent = detailsRepository.countByInvitationLetterSentTrue();
        long abstractReceived = detailsRepository.countByAbstractReceivedTrue();
        long bioReceived = detailsRepository.countByBioReceivedTrue();
        long photoReceived = detailsRepository.countByPhotoReceivedTrue();
        long acceptanceLetterSent = detailsRepository.countByAcceptanceLetterSentTrue();
        long registrationCompleted = detailsRepository.countByRegistrationCompletedTrue();
        long passport = detailsRepository.countByPassportTrue();
        long askedPricing = detailsRepository.countByAskedPricingTrue();
        long askedTravelSupport = detailsRepository.countByAskedTravelSupportTrue();
        long feeWaiver = detailsRepository.countByFeeWaiverTrue();
        long wantsVirtual = detailsRepository.countByWantsVirtualTrue();
        long wantsInPerson = detailsRepository.countByWantsInPersonTrue();
        long scheduleConflict = detailsRepository.countByScheduleConflictTrue();
        long needsApproval = detailsRepository.countByNeedsApprovalTrue();
        long studentJoining = detailsRepository.countByStudentJoiningTrue();
        long onWebsite = detailsRepository.countByOnWebsiteTrue();
        long reinviteNextYear = detailsRepository.countByReinviteNextYearTrue();
        long titleSubmission = detailsRepository.countByTitleSubmissionTrue();

        java.util.Map<String, Long> map = new java.util.LinkedHashMap<>();
        map.put("totalleads", total);
        map.put("overdue", overdue);
        map.put("replytoday", replyToday);
        map.put("conversions", conversions);
        map.put("1dayLeft", oneDayLeft);

        map.put("interested", interested);
        map.put("notInterested", notInterested);
        map.put("dead", dead);

        // Also include breakdown per normalized leadType for visibility
        for (java.util.Map.Entry<String, Long> e : normalized.entrySet()) {
            String k = e.getKey().replaceAll("\\s+", "");
            // keep keys concise in JSON: e.g., "leadType_Interested"
            map.put("leadType_" + k, e.getValue());
        }

        map.put("invitationLetterSent", invitationLetterSent);
        map.put("abstractReceived", abstractReceived);
        map.put("bioReceived", bioReceived);
        map.put("photoReceived", photoReceived);
        map.put("acceptanceLetterSent", acceptanceLetterSent);
        map.put("registrationCompleted", registrationCompleted);
        map.put("passport", passport);
        map.put("askedPricing", askedPricing);
        map.put("askedTravelSupport", askedTravelSupport);
        map.put("feeWaiver", feeWaiver);
        map.put("wantsVirtual", wantsVirtual);
        map.put("wantsInPerson", wantsInPerson);
        map.put("scheduleConflict", scheduleConflict);
        map.put("needsApproval", needsApproval);
        map.put("studentJoining", studentJoining);
        map.put("onWebsite", onWebsite);
        map.put("reinviteNextYear", reinviteNextYear);
        map.put("titleSubmission", titleSubmission);

        return map;
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
                    String leadStr = item.path("lead").asText(null);
                    lead.setLead(leadStr);

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

                    // default values: dueDate = now + 24 hours, currentStage = 1
                    try {
                        lead.setDueDate(Instant.now().plus(1, ChronoUnit.DAYS));
                    } catch (Exception ex) {
                        lead.setDueDate(Instant.now());
                    }
                    lead.setCurrentStage(1);

                    boolean existsById = repository.existsById(id);
                    boolean existsByLead = (leadStr != null && !leadStr.isEmpty()) && repository.existsByLead(leadStr);

                    if (!existsById && !existsByLead) {
                        repository.save(lead);
                        // create an empty LeadDetails record linked to this EmailLead
                        try {
                            LeadDetails details = new LeadDetails();
                            details.setEmailLead(lead);
                            detailsRepository.save(details);
                        } catch (Exception ex) {
                            // don't fail the whole sync if details creation fails
                        }
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

    /**
     * Process an Instantly.ai webhook payload (JSON body). Returns number of leads created/updated.
     */
    public int processWebhookPayload(String body) {
        int processed = 0;
        try {
            com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(body);

            // Support common webhook shapes. First, handle single-object webhook with `lead` + `reply`.
            com.fasterxml.jackson.databind.JsonNode itemsNode = null;
            if (root.has("lead") && root.has("reply")) {
                com.fasterxml.jackson.databind.node.ObjectNode item = mapper.createObjectNode();
                com.fasterxml.jackson.databind.JsonNode leadNode = root.get("lead");
                com.fasterxml.jackson.databind.JsonNode replyNode = root.get("reply");

                item.put("id", leadNode.path("id").asText(null));
                // store lead email in the `lead` column (existing code expects this)
                if (leadNode.has("email")) item.put("lead", leadNode.path("email").asText(null));
                if (leadNode.has("first_name") || leadNode.has("last_name")) {
                    String fn = leadNode.path("first_name").asText("");
                    String ln = leadNode.path("last_name").asText("");
                    String fullname = (fn + " " + ln).trim();
                    if (!fullname.isEmpty()) item.put("lead_name", fullname);
                }
                if (root.has("campaign") && root.get("campaign").has("id")) item.put("campaign_id", root.get("campaign").path("id").asText(null));
                if (root.has("email_account") && root.get("email_account").has("email")) item.put("from_address_email", root.get("email_account").path("email").asText(null));

                if (replyNode.has("email_id")) item.put("thread_id", replyNode.path("email_id").asText(null));
                if (replyNode.has("received_at")) item.put("timestamp_email", replyNode.path("received_at").asText(null));
                if (replyNode.has("body")) item.put("body", replyNode.path("body").asText(null));

                // simple heuristic: if reply body mentions 'interest' mark as Interested
                String bodyText = replyNode.path("body").asText("").toLowerCase();
                if (bodyText.contains("interested") || bodyText.contains("interest")) {
                    item.put("lead_type", "Interested");
                }

                com.fasterxml.jackson.databind.node.ArrayNode arr = mapper.createArrayNode();
                arr.add(item);
                itemsNode = arr;
            } else if (root.isArray()) {
                itemsNode = root;
            } else {
                if (root.has("data")) {
                    itemsNode = root.get("data");
                } else if (root.has("emails")) {
                    itemsNode = root.get("emails");
                } else {
                    java.util.Iterator<com.fasterxml.jackson.databind.JsonNode> it = root.elements();
                    while (it.hasNext()) {
                        com.fasterxml.jackson.databind.JsonNode n = it.next();
                        if (n.isArray()) {
                            itemsNode = n;
                            break;
                        }
                    }
                }
            }

            if (itemsNode != null && itemsNode.isArray()) {
                for (com.fasterxml.jackson.databind.JsonNode item : itemsNode) {
                    String id = item.path("id").asText(null);
                    if (id == null) continue;

                    EmailLead lead = repository.findById(id).orElse(new EmailLead());
                    lead.setId(id);

                    String tsCreated = item.path("timestamp_created").asText(null);
                    if (tsCreated != null && !tsCreated.isEmpty()) {
                        try { lead.setTimestampCreated(java.time.Instant.parse(tsCreated)); } catch (Exception ex) { }
                    }
                    String tsEmail = item.path("timestamp_email").asText(null);
                    if (tsEmail != null && !tsEmail.isEmpty()) {
                        try { lead.setTimestampEmail(java.time.Instant.parse(tsEmail)); } catch (Exception ex) { }
                    }

                    // common fields
                    if (item.has("organization_id")) lead.setOrganizationId(item.path("organization_id").asText(null));
                    if (item.has("from_address_email")) lead.setFromAddressEmail(item.path("from_address_email").asText(null));
                    // fallback: sometimes nested from object
                    if ((lead.getFromAddressEmail() == null || lead.getFromAddressEmail().isBlank()) && item.has("from")) {
                        com.fasterxml.jackson.databind.JsonNode from = item.get("from");
                        if (from.has("email")) lead.setFromAddressEmail(from.path("email").asText(null));
                        else if (from.has("address")) lead.setFromAddressEmail(from.path("address").asText(null));
                    }

                    if (item.has("campaign_id")) lead.setCampaignId(item.path("campaign_id").asText(null));
                    if (item.has("lead")) lead.setLead(item.path("lead").asText(null));
                    if (item.has("thread_id")) lead.setThreadId(item.path("thread_id").asText(null));
                    if (item.has("lead_name")) lead.setLeadName(item.path("lead_name").asText(null));
                    if (item.has("lead_type")) lead.setLeadType(normalizeLeadType(item.path("lead_type").asText(null)));

                    repository.save(lead);
                    processed++;
                }
            }
        } catch (Exception e) {
            System.out.println("Failed to process webhook payload: " + e.getMessage());
        }
        return processed;
    }

    /**
     * Query Instantly API for emails for a specific lead email and campaign id
     * and process the returned items to enrich/save leads.
     */
    public int enrichAndSaveForLeadAndCampaign(String leadEmail, String campaignId) {
        if (leadEmail == null || leadEmail.isBlank()) return 0;
        try {
            String qs = "email_type=received&limit=100";
            qs += "&lead=" + java.net.URLEncoder.encode(leadEmail, java.nio.charset.StandardCharsets.UTF_8);
            if (campaignId != null && !campaignId.isBlank()) {
                qs += "&campaign_id=" + java.net.URLEncoder.encode(campaignId, java.nio.charset.StandardCharsets.UTF_8);
            }
            java.net.URI uri = java.net.URI.create(baseUrl + "/api/v2/emails?" + qs);

            java.net.http.HttpRequest req = java.net.http.HttpRequest.newBuilder(uri)
                    .GET()
                    .timeout(java.time.Duration.ofSeconds(20))
                    .header("Accept", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .build();

            java.net.http.HttpResponse<String> resp = httpClient.send(req, java.net.http.HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() >= 400) {
                throw new IllegalStateException("Instantly API returned status " + resp.statusCode());
            }

            String body = resp.body();
            return processWebhookPayload(body);
        } catch (Exception e) {
            System.out.println("Failed to enrich leads from Instantly API: " + e.getMessage());
            return 0;
        }
    }

    public String getConversationByThreadId(String threadId) throws Exception {
        if (threadId == null) throw new IllegalArgumentException("threadId is required");

        String nextToken = null;
        com.fasterxml.jackson.databind.node.ArrayNode combined = mapper.createArrayNode();

        do {
            String q = "thread:" + threadId + (nextToken != null ? "&starting_after=" + nextToken : "");
            String encoded = URLEncoder.encode(q, "UTF-8");
            URI uri = URI.create(baseUrl + "/api/v2/emails?search=" + encoded);

            HttpRequest req = HttpRequest.newBuilder(uri)
                    .GET()
                    .header("Accept", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .timeout(Duration.ofSeconds(30))
                    .build();

            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() >= 400) {
                throw new IllegalStateException("Instantly API returned status " + resp.statusCode() + ": " + resp.body());
            }

            com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(resp.body());

            // reuse logic from syncAllEmails to locate the array node
            com.fasterxml.jackson.databind.JsonNode itemsNode = null;
            if (root.isArray()) {
                itemsNode = root;
            } else {
                if (root.has("data")) {
                    itemsNode = root.get("data");
                } else if (root.has("emails")) {
                    itemsNode = root.get("emails");
                } else {
                    java.util.Iterator<com.fasterxml.jackson.databind.JsonNode> it = root.elements();
                    while (it.hasNext()) {
                        com.fasterxml.jackson.databind.JsonNode n = it.next();
                        if (n.isArray()) {
                            itemsNode = n;
                            break;
                        }
                    }
                }
            }

            if (itemsNode != null && itemsNode.isArray()) {
                for (com.fasterxml.jackson.databind.JsonNode item : itemsNode) {
                    combined.add(item);
                }
            }

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

        com.fasterxml.jackson.databind.node.ObjectNode out = mapper.createObjectNode();
        out.set("data", combined);
        return mapper.writeValueAsString(out);
    }
}
