package com.example.crm.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "email_lead")
public class EmailLead {

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

    public EmailLead() {
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
}
