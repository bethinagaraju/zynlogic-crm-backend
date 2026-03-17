package com.example.crm.repository;

import com.example.crm.model.EmailLead;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface EmailLeadRepository extends JpaRepository<EmailLead, String>, JpaSpecificationExecutor<EmailLead> {

	boolean existsByLead(String lead);

	@Override
	@EntityGraph(attributePaths = {"details"})
	List<EmailLead> findAll();

	@EntityGraph(attributePaths = {"details"})
	List<EmailLead> findAllByOrderByTimestampCreatedDesc();

	@EntityGraph(attributePaths = {"details"})
	org.springframework.data.domain.Page<EmailLead> findAllByOrderByTimestampCreatedDesc(org.springframework.data.domain.Pageable pageable);

	@EntityGraph(attributePaths = {"details"})
	@Query("SELECT e FROM EmailLead e ORDER BY CASE WHEN e.dueDate IS NULL THEN 1 ELSE 0 END, e.dueDate ASC")
	org.springframework.data.domain.Page<EmailLead> findAllOrderByDueDateNullsLast(org.springframework.data.domain.Pageable pageable);

	@EntityGraph(attributePaths = {"details"})
	@Query("SELECT e FROM EmailLead e WHERE e.leadType = :leadType ORDER BY CASE WHEN e.dueDate IS NULL THEN 1 ELSE 0 END, e.dueDate ASC")
	org.springframework.data.domain.Page<EmailLead> findByLeadTypeOrderByDueDateNullsLast(String leadType, org.springframework.data.domain.Pageable pageable);

	@EntityGraph(attributePaths = {"details"})
	java.util.List<EmailLead> findByFromAddressEmailOrderByTimestampCreatedDesc(String fromAddressEmail);

	@EntityGraph(attributePaths = {"details"})
	org.springframework.data.domain.Page<EmailLead> findByFromAddressEmailOrderByTimestampCreatedDesc(String fromAddressEmail, org.springframework.data.domain.Pageable pageable);

	@EntityGraph(attributePaths = {"details"})
	org.springframework.data.domain.Page<EmailLead> findByLeadTypeOrderByTimestampCreatedDesc(String leadType, org.springframework.data.domain.Pageable pageable);

	@EntityGraph(attributePaths = {"details"})
	org.springframework.data.domain.Page<EmailLead> findByCurrentStageOrderByTimestampCreatedDesc(Integer currentStage, org.springframework.data.domain.Pageable pageable);

	@EntityGraph(attributePaths = {"details"})
	org.springframework.data.domain.Page<EmailLead> findByLeadTypeAndCurrentStageOrderByTimestampCreatedDesc(String leadType, Integer currentStage, org.springframework.data.domain.Pageable pageable);

	long countByDueDateBefore(java.time.Instant instant);

	long countByDueDateBetween(java.time.Instant startInclusive, java.time.Instant endExclusive);

	long countByCurrentStage(Integer currentStage);

	long countByLeadTypeIgnoreCase(String leadType);

	@org.springframework.data.jpa.repository.Query("SELECT e.leadType, COUNT(e) FROM EmailLead e GROUP BY e.leadType")
	java.util.List<java.lang.Object[]> countGroupByLeadType();

}
