package com.example.crm.repository;

import com.example.crm.model.EmailLead;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmailLeadRepository extends JpaRepository<EmailLead, String> {

	boolean existsByLead(String lead);

	@Override
	@EntityGraph(attributePaths = {"details"})
	List<EmailLead> findAll();

}
