package com.example.crm.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import com.fasterxml.jackson.annotation.JsonBackReference;

@Entity
@Table(name = "lead_details")
public class LeadDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "email_lead_id", referencedColumnName = "id", unique = true)
    @JsonBackReference
    private EmailLead emailLead;

    @Column(name = "invitation_letter_sent")
    private Boolean invitationLetterSent = false;

    @Column(name = "abstract_received")
    private Boolean abstractReceived = false;

    @Column(name = "bio_received")
    private Boolean bioReceived = false;

    @Column(name = "photo_received")
    private Boolean photoReceived = false;
    @Column(name = "file_urls", columnDefinition = "TEXT")
    private String fileUrls; // comma-separated public URLs

    @Column(name = "acceptance_letter_sent")
    private Boolean acceptanceLetterSent = false;

    @Column(name = "registration_completed")
    private Boolean registrationCompleted = false;

    @Column(name = "payment_status")
    private String paymentStatus;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    public LeadDetails() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public EmailLead getEmailLead() { return emailLead; }
    public void setEmailLead(EmailLead emailLead) { this.emailLead = emailLead; }

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
}
