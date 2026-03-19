package com.example.crm.model;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.CascadeType;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Transient;
import org.springframework.data.domain.Persistable;
import com.fasterxml.jackson.annotation.JsonManagedReference;

@Entity
@Table(name = "email_lead")
public class EmailLead implements Persistable<String> {

    @Id
    private String id;

    @Column(name = "timestamp_created")
    private Instant timestampCreated;

    @Column(name = "timestamp_email")
    private Instant timestampEmail;

    @Column(name = "organization_id")
    private String organizationId;

    private String eaccount;

    @Column(name = "from_address_email")
    private String fromAddressEmail;

    @Column(name = "campaign_id")
    private String campaignId;

    @Column(name = "lead", unique = true)
    private String lead;

    @Column(name = "ue_type")
    private Integer ueType;

    private String step;

    @Column(name = "is_unread")
    private Integer isUnread;

    @Column(name = "ai_interest_value")
    private Integer aiInterestValue;

    @Column(name = "is_focused")
    private Integer isFocused;

    @Column(name = "i_status")
    private Integer iStatus;

    @Column(name = "thread_id")
    private String threadId;

    @Column(name = "due_date")
    private Instant dueDate;

    @Column(name = "current_stage")
    private Integer currentStage;

    @OneToOne(mappedBy = "emailLead", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private LeadDetails details;

    @Transient
    private transient boolean _isNew = false;

    @Column(name = "lead_name")
    private String leadName;

    @Column(name = "profile_pic", nullable = true)
    private String profilePic;

    @Column(name = "lead_type", nullable = true)
    private String leadType;

    public EmailLead() {
        try {
            this.dueDate = Instant.now().plus(1, ChronoUnit.DAYS);
        } catch (Exception ex) {
            this.dueDate = Instant.now();
        }
        this.currentStage = 1;
        this.leadType = "None";
    }

    public void markNew() { this._isNew = true; }

    public void markNotNew() { this._isNew = false; }

    @Override
    @Transient
    public boolean isNew() {
        return this._isNew;
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Instant getTimestampCreated() { return timestampCreated; }
    public void setTimestampCreated(Instant timestampCreated) { this.timestampCreated = timestampCreated; }

    public Instant getTimestampEmail() { return timestampEmail; }
    public void setTimestampEmail(Instant timestampEmail) { this.timestampEmail = timestampEmail; }

    public String getOrganizationId() { return organizationId; }
    public void setOrganizationId(String organizationId) { this.organizationId = organizationId; }

    public String getEaccount() { return eaccount; }
    public void setEaccount(String eaccount) { this.eaccount = eaccount; }

    public String getFromAddressEmail() { return fromAddressEmail; }
    public void setFromAddressEmail(String fromAddressEmail) { this.fromAddressEmail = fromAddressEmail; }

    public String getCampaignId() { return campaignId; }
    public void setCampaignId(String campaignId) { this.campaignId = campaignId; }

    public String getLead() { return lead; }
    public void setLead(String lead) { this.lead = lead; }

    public Integer getUeType() { return ueType; }
    public void setUeType(Integer ueType) { this.ueType = ueType; }

    public String getStep() { return step; }
    public void setStep(String step) { this.step = step; }

    public Integer getIsUnread() { return isUnread; }
    public void setIsUnread(Integer isUnread) { this.isUnread = isUnread; }

    public Integer getAiInterestValue() { return aiInterestValue; }
    public void setAiInterestValue(Integer aiInterestValue) { this.aiInterestValue = aiInterestValue; }

    public Integer getIsFocused() { return isFocused; }
    public void setIsFocused(Integer isFocused) { this.isFocused = isFocused; }

    public Integer getIStatus() { return iStatus; }
    public void setIStatus(Integer iStatus) { this.iStatus = iStatus; }

    public String getThreadId() { return threadId; }
    public void setThreadId(String threadId) { this.threadId = threadId; }

    public Instant getDueDate() { return dueDate; }
    public void setDueDate(Instant dueDate) { this.dueDate = dueDate; }

    public Integer getCurrentStage() { return currentStage; }
    public void setCurrentStage(Integer currentStage) { this.currentStage = currentStage; }

    // compatibility with existing code that uses getDueAt()/setDueAt()
    public Instant getDueAt() { return this.dueDate; }
    public void setDueAt(Instant dueAt) { this.dueDate = dueAt; }

    public LeadDetails getDetails() { return details; }
    public void setDetails(LeadDetails details) { this.details = details; }

    public String getLeadName() { return leadName; }
    public void setLeadName(String leadName) { this.leadName = leadName; }

    public String getProfilePic() { return profilePic; }
    public void setProfilePic(String profilePic) { this.profilePic = profilePic; }

    public String getLeadType() { return leadType; }
    public void setLeadType(String leadType) { this.leadType = leadType; }
}
