package com.example.crm.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "leads")
public class Lead {

    // ── Core Identity ──────────────────────────────
    @Id
    private String id;                    // Instantly lead id

    private String organization;
    private String campaign;

    // ── Contact Info ───────────────────────────────
    private String email;
    private String firstName;
    private String companyDomain;

    // ── Instantly Status ───────────────────────────
    private Integer status;
    private Integer emailOpenCount;
    private Integer emailReplyCount;
    private Integer emailClickCount;
    private Integer espCode;
    private String uploadMethod;

    // ── Status Summary (lastStep) ──────────────────
    private String lastStepId;
    private String lastStepFrom;
    private LocalDateTime lastStepExecutedAt;

    // ── Payload Fields ─────────────────────────────
    private String location;
    private String roleOffer;
    private String timezoneRegion;
    private String universityName;

    @Column(columnDefinition = "TEXT")
    private String publicationTitle;

    @Column(columnDefinition = "TEXT")
    private String publicationTitleShort;

    // ── Timestamps (from Instantly) ────────────────
    private LocalDateTime timestampCreated;
    private LocalDateTime timestampUpdated;
    private LocalDateTime timestampLastContact;
    private LocalDateTime timestampLastTouch;

    // ── Your System Fields ─────────────────────────
    private LocalDateTime syncedAt;

    @Lob
    @Column(name = "raw_json", columnDefinition = "LONGTEXT")
    private String rawJson;               // full Instantly JSON stored as-is

}
