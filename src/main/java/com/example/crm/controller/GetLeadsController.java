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

    public static class LeadDetailsRequest {
        private Boolean invitationLetterSent;
        private Boolean abstractReceived;
        private Boolean bioReceived;
        private Boolean photoReceived;
        private String fileUrls;
        private Boolean acceptanceLetterSent;
        private Boolean registrationCompleted;
        private String paymentStatus;
        private String notes;

        private Boolean passport;
        private Boolean askedPricing;
        private Boolean askedTravelSupport;
        private Boolean feeWaiver;
        private Boolean wantsVirtual;
        private Boolean wantsInPerson;
        private Boolean scheduleConflict;
        private Boolean needsApproval;
        private Boolean studentJoining;
        private Boolean onWebsite;
        private Boolean reinviteNextYear;
        private Boolean titleSubmission;

        public Boolean getInvitationLetterSent() { return invitationLetterSent; }
        public void setInvitationLetterSent(Boolean invitationLetterSent) { this.invitationLetterSent = invitationLetterSent; }
        public Boolean getAbstractReceived() { return abstractReceived; }
        public void setAbstractReceived(Boolean abstractReceived) { this.abstractReceived = abstractReceived; }
        public Boolean getBioReceived() { return bioReceived; }
        public void setBioReceived(Boolean bioReceived) { this.bioReceived = bioReceived; }
        public Boolean getPhotoReceived() { return photoReceived; }
        public void setPhotoReceived(Boolean photoReceived) { this.photoReceived = photoReceived; }
        public String getFileUrls() { return fileUrls; }
        public void setFileUrls(String fileUrls) { this.fileUrls = fileUrls; }
        public Boolean getAcceptanceLetterSent() { return acceptanceLetterSent; }
        public void setAcceptanceLetterSent(Boolean acceptanceLetterSent) { this.acceptanceLetterSent = acceptanceLetterSent; }
        public Boolean getRegistrationCompleted() { return registrationCompleted; }
        public void setRegistrationCompleted(Boolean registrationCompleted) { this.registrationCompleted = registrationCompleted; }
        public String getPaymentStatus() { return paymentStatus; }
        public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }
        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }

        public Boolean getPassport() { return passport; }
        public void setPassport(Boolean passport) { this.passport = passport; }
        public Boolean getAskedPricing() { return askedPricing; }
        public void setAskedPricing(Boolean askedPricing) { this.askedPricing = askedPricing; }
        public Boolean getAskedTravelSupport() { return askedTravelSupport; }
        public void setAskedTravelSupport(Boolean askedTravelSupport) { this.askedTravelSupport = askedTravelSupport; }
        public Boolean getFeeWaiver() { return feeWaiver; }
        public void setFeeWaiver(Boolean feeWaiver) { this.feeWaiver = feeWaiver; }
        public Boolean getWantsVirtual() { return wantsVirtual; }
        public void setWantsVirtual(Boolean wantsVirtual) { this.wantsVirtual = wantsVirtual; }
        public Boolean getWantsInPerson() { return wantsInPerson; }
        public void setWantsInPerson(Boolean wantsInPerson) { this.wantsInPerson = wantsInPerson; }
        public Boolean getScheduleConflict() { return scheduleConflict; }
        public void setScheduleConflict(Boolean scheduleConflict) { this.scheduleConflict = scheduleConflict; }
        public Boolean getNeedsApproval() { return needsApproval; }
        public void setNeedsApproval(Boolean needsApproval) { this.needsApproval = needsApproval; }
        public Boolean getStudentJoining() { return studentJoining; }
        public void setStudentJoining(Boolean studentJoining) { this.studentJoining = studentJoining; }
        public Boolean getOnWebsite() { return onWebsite; }
        public void setOnWebsite(Boolean onWebsite) { this.onWebsite = onWebsite; }
        public Boolean getReinviteNextYear() { return reinviteNextYear; }
        public void setReinviteNextYear(Boolean reinviteNextYear) { this.reinviteNextYear = reinviteNextYear; }
        public Boolean getTitleSubmission() { return titleSubmission; }
        public void setTitleSubmission(Boolean titleSubmission) { this.titleSubmission = titleSubmission; }
    }

    @PutMapping("/email-leads/{id}/details")
    public ResponseEntity<?> updateLeadDetails(@PathVariable String id, @RequestBody LeadDetailsRequest req) {
        java.util.Optional<EmailLead> opt = repository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.status(404).body(java.util.Map.of("error", "EmailLead not found"));
        EmailLead lead = opt.get();
        com.example.crm.model.LeadDetails details = lead.getDetails();
        if (details == null) {
            details = new com.example.crm.model.LeadDetails();
            details.setEmailLead(lead);
            lead.setDetails(details);
        }

        if (req.getInvitationLetterSent() != null) details.setInvitationLetterSent(req.getInvitationLetterSent());
        if (req.getAbstractReceived() != null) details.setAbstractReceived(req.getAbstractReceived());
        if (req.getBioReceived() != null) details.setBioReceived(req.getBioReceived());
        if (req.getPhotoReceived() != null) details.setPhotoReceived(req.getPhotoReceived());
        if (req.getFileUrls() != null) details.setFileUrls(req.getFileUrls());
        if (req.getAcceptanceLetterSent() != null) details.setAcceptanceLetterSent(req.getAcceptanceLetterSent());
        if (req.getRegistrationCompleted() != null) details.setRegistrationCompleted(req.getRegistrationCompleted());
        if (req.getPaymentStatus() != null) details.setPaymentStatus(req.getPaymentStatus());
        if (req.getNotes() != null) details.setNotes(req.getNotes());

        if (req.getPassport() != null) details.setPassport(req.getPassport());
        if (req.getAskedPricing() != null) details.setAskedPricing(req.getAskedPricing());
        if (req.getAskedTravelSupport() != null) details.setAskedTravelSupport(req.getAskedTravelSupport());
        if (req.getFeeWaiver() != null) details.setFeeWaiver(req.getFeeWaiver());
        if (req.getWantsVirtual() != null) details.setWantsVirtual(req.getWantsVirtual());
        if (req.getWantsInPerson() != null) details.setWantsInPerson(req.getWantsInPerson());
        if (req.getScheduleConflict() != null) details.setScheduleConflict(req.getScheduleConflict());
        if (req.getNeedsApproval() != null) details.setNeedsApproval(req.getNeedsApproval());
        if (req.getStudentJoining() != null) details.setStudentJoining(req.getStudentJoining());
        if (req.getOnWebsite() != null) details.setOnWebsite(req.getOnWebsite());
        if (req.getReinviteNextYear() != null) details.setReinviteNextYear(req.getReinviteNextYear());
        if (req.getTitleSubmission() != null) details.setTitleSubmission(req.getTitleSubmission());

        repository.save(lead);
        return ResponseEntity.ok().body(details);
    }

    public static class LeadNameRequest {
        private String leadName;
        public String getLeadName() { return leadName; }
        public void setLeadName(String leadName) { this.leadName = leadName; }
    }

    @PutMapping("/email-leads/{id}/lead-name")
    public ResponseEntity<?> updateLeadName(@PathVariable String id, @RequestBody LeadNameRequest req) {
        if (req == null || req.getLeadName() == null) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", "leadName is required"));
        }
        java.util.Optional<EmailLead> opt = repository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.status(404).body(java.util.Map.of("error", "EmailLead not found"));
        EmailLead lead = opt.get();
        lead.setLeadName(req.getLeadName());
        repository.save(lead);
        return ResponseEntity.ok().body(lead);
    }

}
