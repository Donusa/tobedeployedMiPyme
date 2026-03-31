package com.mipyme.company;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyRepository extends JpaRepository<Company, Long> {
	Optional<Company> findByCompanyId(String companyId);

	Optional<Company> findByWppPhoneNumberId(String wppPhoneNumberId);

	boolean existsByCompanyId(String companyId);

	Optional<Company> findBySsoCode(String ssoCode);

	boolean existsBySsoCode(String ssoCode);

	boolean existsByCuit(String cuit);

	Optional<Company> findByTenantSchema(String tenantSchema);

	List<Company> findByPendingPlanKeyIsNotNull();




	List<Company> findByPlanStatusIn(List<Company.PlanStatus> statuses);


	List<Company> findByPlanStatusInAndTrialEndBefore(List<Company.PlanStatus> statuses, Instant trialEnd);


	List<Company> findByCancelAtPeriodEndTrueAndPlanStatusIn(List<Company.PlanStatus> statuses);


	List<Company> findByProrationStatus(String prorationStatus);
}

