package com.example.crm.controller;

import com.example.crm.model.EmailLead;
import com.example.crm.model.LeadDetails;
import com.example.crm.repository.EmailLeadRepository;
import com.example.crm.repository.LeadDetailsRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/lead-details")
public class LeadDetailsController {

    private final LeadDetailsRepository detailsRepository;
    private final EmailLeadRepository emailLeadRepository;
    private final com.example.crm.service.FtpService ftpService;
    private final com.example.crm.repository.LeadStageRepository leadStageRepository;

    @org.springframework.beans.factory.annotation.Value("${hostinger.ftp.crm-upload-path:/uploads/crm}")
    private String crmUploadPath;

    public LeadDetailsController(LeadDetailsRepository detailsRepository, EmailLeadRepository emailLeadRepository,
                                 com.example.crm.service.FtpService ftpService,
                                 com.example.crm.repository.LeadStageRepository leadStageRepository) {
        this.detailsRepository = detailsRepository;
        this.emailLeadRepository = emailLeadRepository;
        this.ftpService = ftpService;
        this.leadStageRepository = leadStageRepository;
    }

    @GetMapping("/{emailLeadId}")
    public ResponseEntity<LeadDetails> getByEmailLeadId(@PathVariable String emailLeadId) {
        Optional<LeadDetails> opt = detailsRepository.findByEmailLead_Id(emailLeadId);
        if (opt.isPresent()) {
            return ResponseEntity.ok(opt.get());
        }

        // If no details exist but the EmailLead exists, create an empty details record
        Optional<EmailLead> leadOpt = emailLeadRepository.findById(emailLeadId);
        if (leadOpt.isPresent()) {
            LeadDetails details = new LeadDetails();
            details.setEmailLead(leadOpt.get());
            LeadDetails saved = detailsRepository.save(details);
            return ResponseEntity.ok(saved);
        }

        return ResponseEntity.notFound().build();
    }

    @PutMapping("/{emailLeadId}")
    public ResponseEntity<LeadDetails> updateByEmailLeadId(@PathVariable String emailLeadId,
                                                           @RequestBody LeadDetails incoming) {
        Optional<LeadDetails> opt = detailsRepository.findByEmailLead_Id(emailLeadId);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();

        LeadDetails existing = opt.get();

        existing.setInvitationLetterSent(incoming.getInvitationLetterSent());
        existing.setAbstractReceived(incoming.getAbstractReceived());
        existing.setBioReceived(incoming.getBioReceived());
        existing.setPhotoReceived(incoming.getPhotoReceived());
        existing.setAcceptanceLetterSent(incoming.getAcceptanceLetterSent());
        existing.setRegistrationCompleted(incoming.getRegistrationCompleted());
        existing.setPaymentStatus(incoming.getPaymentStatus());
        existing.setNotes(incoming.getNotes());

        LeadDetails saved = detailsRepository.save(existing);
        return ResponseEntity.ok(saved);
    }

    @PostMapping("/{emailLeadId}/upload")
    public ResponseEntity<LeadDetails> uploadFiles(@PathVariable String emailLeadId,
                                                   @RequestParam("files") MultipartFile[] files) {
        Optional<LeadDetails> opt = detailsRepository.findByEmailLead_Id(emailLeadId);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();

        LeadDetails existing = opt.get();

        try {
            String existingUrls = existing.getFileUrls();
            java.util.List<String> urls = existingUrls == null || existingUrls.isBlank() ? new java.util.ArrayList<>() : Arrays.stream(existingUrls.split(",")).map(String::trim).filter(s->!s.isEmpty()).collect(Collectors.toList());

            for (MultipartFile f : files) {
                if (f == null || f.isEmpty()) continue;
                String uploaded = ftpService.upload(f, crmUploadPath);
                urls.add(uploaded);
                // If this is an image, mark photo as received
                String ct = f.getContentType();
                if (ct != null && ct.startsWith("image/")) {
                    existing.setPhotoReceived(true);
                }
            }

            String joined = String.join(",", urls);
            existing.setFileUrls(joined);
            LeadDetails saved = detailsRepository.save(existing);
            return ResponseEntity.ok(saved);
        } catch (Exception ex) {
            return ResponseEntity.status(500).build();
        }
    }

    @DeleteMapping("/{emailLeadId}/file")
    public ResponseEntity<LeadDetails> deleteFile(@PathVariable String emailLeadId,
                                                  @RequestParam(value = "url", required = false) String publicUrl,
                                                  @RequestParam(value = "name", required = false) String name) {
        Optional<LeadDetails> opt = detailsRepository.findByEmailLead_Id(emailLeadId);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();

        LeadDetails existing = opt.get();

        try {
            boolean deleted = false;

            if (name != null && !name.isBlank()) {
                deleted = ftpService.deleteByFilename(crmUploadPath, name);
            } else if (publicUrl != null && !publicUrl.isBlank()) {
                deleted = ftpService.deleteByPublicUrl(publicUrl);
            } else {
                return ResponseEntity.badRequest().build();
            }

            // Remove from stored fileUrls regardless of FTP deletion success
            String existingUrls = existing.getFileUrls();
            java.util.List<String> urls = existingUrls == null || existingUrls.isBlank() ? new java.util.ArrayList<>() : java.util.Arrays.stream(existingUrls.split(",")).map(String::trim).filter(s->!s.isEmpty()).collect(java.util.stream.Collectors.toList());

            if (name != null && !name.isBlank()) {
                urls.removeIf(u -> {
                    String uName = u.contains("/") ? u.substring(u.lastIndexOf('/') + 1) : u;
                    return uName.equals(name);
                });
            } else if (publicUrl != null && !publicUrl.isBlank()) {
                urls.removeIf(u -> u.equals(publicUrl));
            }

            existing.setFileUrls(String.join(",", urls));

            // If no image URLs remain, clear photoReceived
            boolean hasImage = urls.stream().anyMatch(u -> u.matches("(?i).*\\.(jpg|jpeg|png|gif|webp)$"));
            existing.setPhotoReceived(hasImage);

            LeadDetails saved = detailsRepository.save(existing);
            return ResponseEntity.ok(saved);
        } catch (Exception ex) {
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/{emailLeadId}/stages")
    public ResponseEntity<java.util.List<com.example.crm.model.LeadStageDto>> getStages(@PathVariable String emailLeadId) {
        java.util.List<com.example.crm.model.LeadStage> stages = leadStageRepository.findByEmailLead_IdOrderByStageIndex(emailLeadId);

        if (stages == null || stages.isEmpty()) {
            // create default stages if the lead exists but has no stages
            java.util.Optional<com.example.crm.model.EmailLead> leadOpt = emailLeadRepository.findById(emailLeadId);
            if (leadOpt.isEmpty()) return ResponseEntity.notFound().build();
            com.example.crm.model.EmailLead lead = leadOpt.get();

            String[] names = new String[] {
                "Reply Received",
                "Warm Reply Sent",
                "Details Received",
                "Acceptance Email Sent",
                "Added to Website",
                "Share Website + Registration Link",
                "Handle Requests & Objections",
                "Payment Received"
            };
            int[] defaultDays = new int[] {7,1,14,2,0,1,5,14};

            java.util.List<com.example.crm.model.LeadStage> created = new java.util.ArrayList<>();
            for (int si = 0; si < names.length; si++) {
                com.example.crm.model.LeadStage stage = new com.example.crm.model.LeadStage();
                stage.setEmailLead(lead);
                stage.setStageIndex(si + 1);
                stage.setStageName(names[si]);
                stage.setDefaultDays(defaultDays[si]);
                stage.setDays(defaultDays[si]);
                stage.setCompleted(false);
                try {
                    java.time.ZonedDateTime nowIst = java.time.ZonedDateTime.now(java.time.ZoneId.of("Asia/Kolkata"));
                    stage.setDueAt(nowIst.plusDays(defaultDays[si]).toInstant());
                } catch (Exception ex) {
                    stage.setDueAt(java.time.Instant.now());
                }
                created.add(leadStageRepository.save(stage));
            }

            stages = leadStageRepository.findByEmailLead_IdOrderByStageIndex(emailLeadId);
        }

        java.util.List<com.example.crm.model.LeadStageDto> dtoList = new java.util.ArrayList<>();
        for (com.example.crm.model.LeadStage s : stages) {
            com.example.crm.model.LeadStageDto d = new com.example.crm.model.LeadStageDto();
            d.setStageIndex(s.getStageIndex());
            d.setStageName(s.getStageName());
            d.setDefaultDays(s.getDefaultDays());
            d.setDays(s.getDays());
            d.setCompleted(s.getCompleted());
            d.setCompletedAt(s.getCompletedAt());
            d.setDueAt(s.getDueAt());
            dtoList.add(d);
        }

        return ResponseEntity.ok(dtoList);
    }

    @PutMapping("/{emailLeadId}/stages/{stageIndex}")
    public ResponseEntity<com.example.crm.model.LeadStage> updateStage(@PathVariable String emailLeadId,
                                                                       @PathVariable Integer stageIndex,
                                                                       @RequestBody com.example.crm.model.LeadStage incoming) {
        com.example.crm.model.LeadStage existing = leadStageRepository.findByEmailLead_IdAndStageIndex(emailLeadId, stageIndex);
        if (existing == null) return ResponseEntity.notFound().build();

        Boolean prevCompleted = existing.getCompleted();

        java.time.ZonedDateTime nowIstForCompletion = null;
        if (incoming.getDays() != null) {
            existing.setDays(incoming.getDays());
            try {
                java.time.ZonedDateTime nowIst = java.time.ZonedDateTime.now(java.time.ZoneId.of("Asia/Kolkata"));
                existing.setDueAt(nowIst.plusDays(existing.getDays() == null ? 0 : existing.getDays()).toInstant());
                nowIstForCompletion = nowIst;
            } catch (Exception ex) {
                existing.setDueAt(java.time.Instant.now());
                nowIstForCompletion = java.time.ZonedDateTime.now(java.time.ZoneId.of("Asia/Kolkata"));
            }
        }
        // if caller provides an explicit dueAt, honor it (manually adjust time)
        if (incoming.getDueAt() != null) {
            existing.setDueAt(incoming.getDueAt());
        }
        if (incoming.getCompleted() != null) existing.setCompleted(incoming.getCompleted());

        // If completed is set true and completedAt not provided, set completedAt to current IST instant
        if (Boolean.TRUE.equals(incoming.getCompleted())) {
            if (incoming.getCompletedAt() != null) {
                existing.setCompletedAt(incoming.getCompletedAt());
            } else {
                try {
                    if (nowIstForCompletion == null) nowIstForCompletion = java.time.ZonedDateTime.now(java.time.ZoneId.of("Asia/Kolkata"));
                    existing.setCompletedAt(nowIstForCompletion.toInstant());
                } catch (Exception ex) {
                    existing.setCompletedAt(java.time.Instant.now());
                }
            }
        } else {
            if (incoming.getCompletedAt() != null) existing.setCompletedAt(incoming.getCompletedAt());
        }

        com.example.crm.model.LeadStage saved = leadStageRepository.save(existing);

        // If the stage was just completed (was not completed before but now is), set next stage's dueAt
        boolean justCompleted = (Boolean.TRUE.equals(incoming.getCompleted()) && (prevCompleted == null || !prevCompleted));
        if (justCompleted) {
            int nextIndex = stageIndex + 1;
            com.example.crm.model.LeadStage next = leadStageRepository.findByEmailLead_IdAndStageIndex(emailLeadId, nextIndex);
            if (next != null) {
                try {
                    java.time.ZonedDateTime nowIst = java.time.ZonedDateTime.now(java.time.ZoneId.of("Asia/Kolkata"));
                    int addDays = next.getDefaultDays() != null ? next.getDefaultDays() : (next.getDays() != null ? next.getDays() : 0);
                    next.setDueAt(nowIst.plusDays(addDays).toInstant());
                } catch (Exception ex) {
                    next.setDueAt(java.time.Instant.now());
                }
                if (next.getDays() == null) next.setDays(next.getDefaultDays());
                leadStageRepository.save(next);
            }
        }

        return ResponseEntity.ok(saved);
    }

}
