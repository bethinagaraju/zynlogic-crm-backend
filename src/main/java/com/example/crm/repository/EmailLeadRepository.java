package com.example.crm.repository;

import com.example.crm.model.EmailLead;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailLeadRepository extends JpaRepository<EmailLead, String> {
	boolean existsByLead(String lead);

}
