package com.example.crm.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.crm.model.LeadStage;

public interface LeadStageRepository extends JpaRepository<LeadStage, Long> {
    List<LeadStage> findByEmailLead_IdOrderByStageIndex(String emailLeadId);
    LeadStage findByEmailLead_IdAndStageIndex(String emailLeadId, Integer stageIndex);
}
