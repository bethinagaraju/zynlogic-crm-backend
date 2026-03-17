package com.example.crm.repository;

import com.example.crm.model.LeadDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface LeadDetailsRepository extends JpaRepository<LeadDetails, Long> {

	Optional<LeadDetails> findByEmailLead_Id(String emailLeadId);

	long countByInvitationLetterSentTrue();
	long countByAbstractReceivedTrue();
	long countByBioReceivedTrue();
	long countByPhotoReceivedTrue();
	long countByAcceptanceLetterSentTrue();
	long countByRegistrationCompletedTrue();
	long countByPassportTrue();
	long countByAskedPricingTrue();
	long countByAskedTravelSupportTrue();
	long countByFeeWaiverTrue();
	long countByWantsVirtualTrue();
	long countByWantsInPersonTrue();
	long countByScheduleConflictTrue();
	long countByNeedsApprovalTrue();
	long countByStudentJoiningTrue();
	long countByOnWebsiteTrue();
	long countByReinviteNextYearTrue();
	long countByTitleSubmissionTrue();

}
