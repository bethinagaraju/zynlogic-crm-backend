package com.example.crm.model;

import java.time.Instant;

public class LeadStageDto {

    private Integer stageIndex;
    private String stageName;
    private Integer defaultDays;
    private Integer days;
    private Boolean completed;
    private Instant completedAt;
    private Instant dueAt;

    public LeadStageDto() {}

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
