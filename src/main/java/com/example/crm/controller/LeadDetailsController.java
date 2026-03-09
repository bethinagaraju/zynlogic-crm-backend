package com.example.crm.controller;

import com.example.crm.model.EmailLead;
import com.example.crm.model.LeadDetails;
import com.example.crm.repository.EmailLeadRepository;
import com.example.crm.repository.LeadDetailsRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/lead-details")
public class LeadDetailsController {

    private final LeadDetailsRepository detailsRepository;
    private final EmailLeadRepository emailLeadRepository;

    public LeadDetailsController(LeadDetailsRepository detailsRepository, EmailLeadRepository emailLeadRepository) {
        this.detailsRepository = detailsRepository;
        this.emailLeadRepository = emailLeadRepository;
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

}
