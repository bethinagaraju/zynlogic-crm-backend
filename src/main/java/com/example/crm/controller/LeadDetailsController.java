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

    @org.springframework.beans.factory.annotation.Value("${hostinger.ftp.crm-upload-path:/uploads/crm}")
    private String crmUploadPath;

    public LeadDetailsController(LeadDetailsRepository detailsRepository, EmailLeadRepository emailLeadRepository,
                                 com.example.crm.service.FtpService ftpService) {
        this.detailsRepository = detailsRepository;
        this.emailLeadRepository = emailLeadRepository;
        this.ftpService = ftpService;
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

}
