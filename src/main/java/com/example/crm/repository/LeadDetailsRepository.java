package com.example.crm.repository;

import com.example.crm.model.LeadDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface LeadDetailsRepository extends JpaRepository<LeadDetails, Long> {

	Optional<LeadDetails> findByEmailLead_Id(String emailLeadId);

}
