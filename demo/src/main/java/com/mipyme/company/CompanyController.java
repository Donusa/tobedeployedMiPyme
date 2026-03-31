package com.mipyme.company;

import java.time.Instant;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mipyme.tenant.TenantContext;
import com.mipyme.user.AppUser;
import com.mipyme.user.AppUserRepository;
import com.mipyme.user.UserRole;

@RestController
@RequestMapping("/api/companies")
public class CompanyController {

	private final CompanyRepository companyRepository;
	private final CompanySchemaService companySchemaService;
	private final AppUserRepository appUserRepository;
	private final PasswordEncoder passwordEncoder;
	private final CompanyService companyService;
	private final PlanLimitService planLimitService;

	public CompanyController(
			CompanyRepository companyRepository,
			CompanySchemaService companySchemaService,
			AppUserRepository appUserRepository,
			PasswordEncoder passwordEncoder,
			CompanyService companyService,
			PlanLimitService planLimitService
	) {
		this.companyRepository = companyRepository;
		this.companySchemaService = companySchemaService;
		this.appUserRepository = appUserRepository;
		this.passwordEncoder = passwordEncoder;
		this.companyService = companyService;
		this.planLimitService = planLimitService;
	}

	@GetMapping("/sso/next-available")
	public ResponseEntity<java.util.Map<String, String>> getNextAvailableSso(@RequestParam String base) {
		if (base == null || base.isBlank()) {
			return ResponseEntity.badRequest().build();
		}

		String cleanBase = base.trim().toUpperCase().replaceAll("[^A-Z0-9]", "");
		if (cleanBase.isEmpty()) {
			cleanBase = "COMP";
		}

		int startLen = Math.min(cleanBase.length(), 4);

		for (int len = startLen; len >= 1; len--) {
			String prefix = cleanBase.substring(0, len);
			int numberLen = 6 - len;
			int maxNum = (int) Math.pow(10, numberLen);

			for (int i = 1; i < maxNum; i++) {
				String format = "%s%0" + numberLen + "d";
				String candidate = String.format(format, prefix, i);
				if (!companyRepository.existsBySsoCode(candidate)) {
					return ResponseEntity.ok(java.util.Map.of("ssoCode", candidate));
				}
			}
		}

		return ResponseEntity.status(HttpStatus.CONFLICT).build();
	}

	@PostMapping("/register")
	public ResponseEntity<CompanyRegisterResponse> register(@RequestBody CompanyRegisterRequest request) {
		if (request.tradeName() == null || request.tradeName().isBlank()) {
			return ResponseEntity.badRequest().build();
		}
		if (!request.termsAccepted()) {
			return ResponseEntity.badRequest().build();
		}

		String ssoCode = request.ssoCode();
		if (ssoCode == null || ssoCode.isBlank() || ssoCode.length() > 6) {
			return ResponseEntity.badRequest().build();
		}

		if (companyRepository.existsBySsoCode(ssoCode)) {
			return ResponseEntity.status(HttpStatus.CONFLICT).build();
		}

		if (request.cuit() != null && !request.cuit().isBlank() && companyRepository.existsByCuit(request.cuit())) {
			return ResponseEntity.status(HttpStatus.CONFLICT).build();
		}

		String companyId = UUID.randomUUID().toString();
		String tenantSchema = "tenant_" + companyId.replace("-", "");

		if (companyRepository.existsByCompanyId(companyId)) {
			return ResponseEntity.status(HttpStatus.CONFLICT).build();
		}

		companySchemaService.ensureTenantSchemaAndTables(tenantSchema);

		Company company = new Company(
				companyId,
				request.tradeName(),
				tenantSchema,
				Instant.now(),
				ssoCode,
				emptyToNull(request.businessName()),
				emptyToNull(request.cuit()),
				emptyToNull(request.country()),
				emptyToNull(request.province()),
				emptyToNull(request.city()),
				emptyToNull(request.industry()),
				emptyToNull(request.companyEmail()),
				emptyToNull(request.phone()),
				emptyToNull(request.fiscalAddress()),
				request.termsAccepted()
		);
		String tier = request.planTier();
		if (tier != null && !tier.isBlank()) {
			String normalizedTier = tier.toLowerCase();
			if (normalizedTier.equals("base") || normalizedTier.equals("pro") || normalizedTier.equals("enterprise")) {
				company.setPlanTier(normalizedTier);
			}
			if (normalizedTier.equals("pro")) {
				company.setPlanStatus(Company.PlanStatus.TRIALING);
				company.setValidUntil(Instant.now().plus(java.time.Duration.ofDays(28)));
			} else {

				company.setPlanStatus(Company.PlanStatus.PENDING_ACTIVATION);
			}
		}
		companyRepository.save(company);

		TenantContext.setCurrentTenant(tenantSchema);
		try {
			if (request.adminEmail() != null && !request.adminEmail().isBlank() && request.adminPassword() != null) {
				AppUser admin = new AppUser(
						request.adminEmail(),
						request.adminName() != null ? request.adminName() : "Admin",
						request.adminEmail(),
						passwordEncoder.encode(request.adminPassword()),
						UserRole.ADMIN.name(),
						"ALL"
				);
				appUserRepository.save(admin);
			}
		} finally {
			TenantContext.clear();
		}

		return ResponseEntity.status(HttpStatus.CREATED).body(new CompanyRegisterResponse(companyId, tenantSchema, ssoCode));
	}

	@GetMapping("/me")
	public ResponseEntity<CompanyResponse> getMyCompany() {
		String currentTenant = TenantContext.getCurrentTenant();
		if (currentTenant == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}

		TenantContext.clear();
		try {
			Company company = companyService.getCompanyByTenant(currentTenant).orElse(null);
			if (company == null) {
				return ResponseEntity.notFound().build();
			}
			return ResponseEntity.ok(mapToResponse(company));
		} finally {
			TenantContext.setCurrentTenant(currentTenant);
		}
	}

	@org.springframework.web.bind.annotation.PutMapping("/me")
	public ResponseEntity<CompanyResponse> updateMyCompany(@RequestBody CompanyUpdateRequest request) {
		String currentTenant = TenantContext.getCurrentTenant();
		if (currentTenant == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}

		TenantContext.clear();
		try {
			Company saved = companyService.updateCompany(currentTenant, request);
			if (saved == null) {
				return ResponseEntity.notFound().build();
			}
			return ResponseEntity.ok(mapToResponse(saved));
		} catch (IllegalStateException e) {
			return ResponseEntity.status(HttpStatus.CONFLICT).build();
		} finally {
			TenantContext.setCurrentTenant(currentTenant);
		}
	}

	private CompanyResponse mapToResponse(Company company) {
		return new CompanyResponse(
				company.getCompanyId(),
				company.getSsoCode(),
				company.getBusinessName(),
				company.getName(),
				company.getCuit(),
				company.getCountry(),
				company.getProvince(),
				company.getCity(),
				company.getIndustry(),
				company.getEmail(),
				company.getPhone(),
				company.getFiscalAddress(),
				company.getTenantSchema(),
				company.getPlanStatus() != null ? company.getPlanStatus().name().toLowerCase() : "trial",
				company.getPlanTier(),
				company.getValidUntil() != null ? company.getValidUntil().toString() : null,
				company.getGraceUntil() != null ? company.getGraceUntil().toString() : null
		);
	}


	@GetMapping("/limits")
	public ResponseEntity<PlanLimitsResponse> getPlanLimits() {
		String currentTenant = TenantContext.getCurrentTenant();
		if (currentTenant == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}
		TenantContext.clear();
		try {
			PlanLimitsResponse response = planLimitService.getCurrentLimitsAndUsage(currentTenant);
			return ResponseEntity.ok(response);
		} catch (IllegalStateException e) {
			return ResponseEntity.notFound().build();
		} finally {
			TenantContext.setCurrentTenant(currentTenant);
		}
	}

	private String emptyToNull(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value;
	}
}
