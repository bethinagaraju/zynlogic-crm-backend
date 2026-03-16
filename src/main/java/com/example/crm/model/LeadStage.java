package com.example.crm.model;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.ZoneId;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "lead_stages")
public class LeadStage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "email_lead_id", referencedColumnName = "id")
    private EmailLead emailLead;

    @Column(name = "stage_index")
    private Integer stageIndex;

    @Column(name = "stage_name")
    private String stageName;

    @Column(name = "default_days")
    private Integer defaultDays;

    @Column(name = "days")
    private Integer days; // editable per lead

    @Column(name = "completed")
    private Boolean completed = false;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "due_at")
    private Instant dueAt; // computed timestamp (stored as UTC instant) based on defaultDays/days in IST

    public LeadStage() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public EmailLead getEmailLead() { return emailLead; }
    public void setEmailLead(EmailLead emailLead) { this.emailLead = emailLead; }

    public Integer getStageIndex() { return stageIndex; }
    public void setStageIndex(Integer stageIndex) { this.stageIndex = stageIndex; }

    public String getStageName() { return stageName; }
    public void setStageName(String stageName) { this.stageName = stageName; }

    public Integer getDefaultDays() { return defaultDays; }
    public void setDefaultDays(Integer defaultDays) { this.defaultDays = defaultDays; }

    public Integer getDays() { return days; }
    public void setDays(Integer days) { this.days = days; }

    public Boolean getCompleted() { return completed; }
    public void setCompleted(Boolean completed) { this.completed = completed; }

    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }

    public Instant getDueAt() { return dueAt; }
    public void setDueAt(Instant dueAt) { this.dueAt = dueAt; }
}
