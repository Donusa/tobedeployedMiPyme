package com.mipyme.user;

import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mipyme.access.model.AccessLog;
import com.mipyme.access.model.ActiveSession;
import com.mipyme.access.service.AccessService;
import com.mipyme.auth.AuthResponse;
import com.mipyme.company.Company;
import com.mipyme.company.CompanyRepository;
import com.mipyme.mercadopago.service.MpSubscriptionService;
import com.mipyme.security.JwtService;
import com.mipyme.tenant.TenantContext;

import io.jsonwebtoken.Claims;

@RestController
@RequestMapping("/api")
public class MeController {

	private static final Logger logger = LoggerFactory.getLogger(MeController.class);

	private final AppUserRepository userRepository;
	private final CompanyRepository companyRepository;
	private final PasswordEncoder passwordEncoder;
	private final AccessService accessService;
	private final JwtService jwtService;
	private final MpSubscriptionService mpSubscriptionService;
	private final JdbcTemplate jdbcTemplate;

	public MeController(AppUserRepository userRepository, CompanyRepository companyRepository,
			PasswordEncoder passwordEncoder, AccessService accessService, JwtService jwtService,
			MpSubscriptionService mpSubscriptionService, DataSource dataSource) {
		this.userRepository = userRepository;
		this.companyRepository = companyRepository;
		this.passwordEncoder = passwordEncoder;
		this.accessService = accessService;
		this.jwtService = jwtService;
		this.mpSubscriptionService = mpSubscriptionService;
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}

	@GetMapping("/me")
	public ResponseEntity<MeResponse> me(Authentication authentication) {
		String username = authentication.getName();

		AppUser user = userRepository.findByUsername(username).orElse(null);
		if (user == null) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
		}

		String currentTenant = TenantContext.getCurrentTenant();
		Company company = null;
		if (currentTenant != null) {

			TenantContext.clear();
			try {
				company = companyRepository.findByTenantSchema(currentTenant).orElse(null);
			} finally {
				TenantContext.setCurrentTenant(currentTenant);
			}
		}

		MeResponse.CompanySummary companySummary = null;
		if (company != null) {
			companySummary = new MeResponse.CompanySummary(
					company.getSsoCode(),
					company.getBusinessName(),
					company.getName(),
					company.getCuit()
			);
		}

		return ResponseEntity.ok(new MeResponse(
				String.valueOf(user.getId()),
				user.getUsername(),
				user.getName(),
				user.getEmail(),
				user.getRole(),
				companySummary
		));
	}

	@PutMapping("/me")
	public ResponseEntity<MeResponse> updateProfile(Authentication authentication, @RequestBody UpdateProfileRequest request) {
		String username = authentication.getName();

		AppUser user = userRepository.findByUsername(username).orElse(null);
		if (user == null) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
		}

		if (request.name() != null && !request.name().isBlank()) {
			user.setName(request.name().trim());
		}

		if (request.email() != null && !request.email().isBlank()) {
			user.setEmail(request.email().trim());
		}

		userRepository.save(user);








		String currentTenant = TenantContext.getCurrentTenant();
		Company company = null;
		if (currentTenant != null) {
			TenantContext.clear();
			try {
				company = companyRepository.findByTenantSchema(currentTenant).orElse(null);
			} finally {
				TenantContext.setCurrentTenant(currentTenant);
			}
		}

		MeResponse.CompanySummary companySummary = null;
		if (company != null) {
			companySummary = new MeResponse.CompanySummary(
					company.getSsoCode(),
					company.getBusinessName(),
					company.getName(),
					company.getCuit()
			);
		}

		return ResponseEntity.ok(new MeResponse(
				String.valueOf(user.getId()),
				user.getUsername(),
				user.getName(),
				user.getEmail(),
				user.getRole(),
				companySummary
		));
	}

	@PostMapping(value = "/me/email", produces = "application/json")
	public ResponseEntity<?> changeEmail(org.springframework.security.core.Authentication authentication, HttpServletRequest httpRequest, @RequestBody ChangeEmailRequest request) {
		String username = authentication.getName();

		AppUser user = userRepository.findByUsername(username).orElse(null);
		if (user == null) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(java.util.Map.of("message", "Usuario no encontrado"));
		}

		if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(java.util.Map.of("message", "Contraseña incorrecta"));
		}

		if (!user.getEmail().equalsIgnoreCase(request.currentEmail())) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(java.util.Map.of("message", "El email actual no coincide"));
		}

		String oldUsername = user.getUsername();
		boolean usernameChanged = false;

		user.setEmail(request.newEmail());

		if (user.getUsername().equalsIgnoreCase(request.currentEmail())) {
			user.setUsername(request.newEmail());
			usernameChanged = true;
		}

		userRepository.save(user);

		if (usernameChanged) {

			accessService.updateUsernameInSessions(oldUsername, user.getUsername());


			String authHeader = httpRequest.getHeader("Authorization");
			String sid = null;
			if (authHeader != null && authHeader.startsWith("Bearer ")) {
				String oldToken = authHeader.substring(7);
				try {
					Claims claims = jwtService.validateAndGetClaims(oldToken);
					sid = claims.get("sid", String.class);
				} catch (Exception e) {

				}
			}

			String currentTenant = TenantContext.getCurrentTenant();
			Company company = null;
			if (currentTenant != null) {
				TenantContext.clear();
				try {
					company = companyRepository.findByTenantSchema(currentTenant).orElse(null);
				} finally {
					TenantContext.setCurrentTenant(currentTenant);
				}
			}

			String companyId = company != null ? company.getCompanyId() : "";
			String companyName = company != null ? company.getName() : "";
			String planTier = company != null ? company.getPlanTier() : null;
			String planStatus = company != null && company.getPlanStatus() != null ? company.getPlanStatus().name().toLowerCase() : null;

			String newToken = jwtService.generateToken(user.getUsername(), companyId, user.getRole(), user.getPermissions(), sid, planTier, planStatus);
			String newRefreshToken = jwtService.generateRefreshToken(user.getUsername(), companyId, user.getRole(), user.getPermissions(), sid, planTier, planStatus);

























			if (sid != null) {





				accessService.updateSessionTokenBySid(sid, newRefreshToken);
			}

			return ResponseEntity.ok(new AuthResponse(newToken, newRefreshToken, companyId, user.getRole(), companyName, user.getName(), user.getPermissions(), false, user.getTheme(), company != null ? company.getPlanTier() : null, company != null && company.getPlanStatus() != null ? company.getPlanStatus().name().toLowerCase() : null));
		}

		return ResponseEntity.ok().build();
	}

	@PostMapping("/me/password")
	public ResponseEntity<Void> changePassword(org.springframework.security.core.Authentication authentication, @RequestBody ChangePasswordRequest request) {
		String username = authentication.getName();

		AppUser user = userRepository.findByUsername(username).orElse(null);
		if (user == null) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
		}

		if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
		}

		user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
		userRepository.save(user);

		return ResponseEntity.ok().build();
	}

	@GetMapping("/me/access-logs")
	public ResponseEntity<List<AccessLog>> getAccessLogs(Authentication authentication) {
		return ResponseEntity.ok(accessService.getRecentAccessLogs(authentication.getName()));
	}

	@GetMapping("/me/sessions")
	public ResponseEntity<List<ActiveSession>> getActiveSessions(Authentication authentication) {
		return ResponseEntity.ok(accessService.getActiveSessions(authentication.getName()));
	}

	@PostMapping("/me/sessions/revoke-others")
	public ResponseEntity<Void> revokeOtherSessions(Authentication authentication, HttpServletRequest request) {
		String username = authentication.getName();
		String authHeader = request.getHeader("Authorization");
		if (authHeader != null && authHeader.startsWith("Bearer ")) {
			String token = authHeader.substring(7);
			try {
				Claims claims = jwtService.validateAndGetClaims(token);
				String sid = claims.get("sid", String.class);
				if (sid != null) {
					accessService.revokeAllOtherSessionsBySid(username, sid);
					return ResponseEntity.ok().build();
				}
			} catch (Exception e) {

			}
		}
		return ResponseEntity.badRequest().build();
	}

	@DeleteMapping("/me/sessions/{id}")
	public ResponseEntity<Void> revokeSession(@PathVariable Long id) {
		accessService.revokeSession(id);
		return ResponseEntity.noContent().build();
	}


	@PostMapping("/me/backup")
	public ResponseEntity<Void> saveBackup(@RequestBody(required = false) Object body) {

		return ResponseEntity.ok().build();
	}


	@DeleteMapping("/me")
	public ResponseEntity<?> deleteAccount(
			Authentication authentication,
			@RequestBody(required = false) DeleteAccountRequest request) {

		if (request == null || request.password() == null || request.password().isBlank()) {
			return ResponseEntity.badRequest().body(Map.of("error", "La contraseña es requerida"));
		}

		String username = authentication.getName();
		AppUser user = userRepository.findByUsername(username).orElse(null);
		if (user == null) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
		}

		if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
					.body(Map.of("error", "Contraseña incorrecta"));
		}

		String tenantSchema = TenantContext.getCurrentTenant();


		if (tenantSchema != null) {
			try {
				mpSubscriptionService.cancelSubscriptionIfActive(tenantSchema);
				logger.info("MP subscription cancelled for tenant {} on account deletion", tenantSchema);
			} catch (Exception e) {
				logger.warn("Could not cancel MP subscription for tenant {} during account deletion: {}",
						tenantSchema, e.getMessage());
			}
		}


		accessService.revokeAllSessions(username);


		TenantContext.clear();
		try {
			if (tenantSchema != null && tenantSchema.matches("[a-z0-9_]+")) {
				jdbcTemplate.execute("DROP SCHEMA IF EXISTS `" + tenantSchema + "`");
				logger.info("Dropped tenant schema: {}", tenantSchema);
			}
			companyRepository.findByTenantSchema(tenantSchema).ifPresent(company -> {
				companyRepository.delete(company);
				logger.info("Deleted Company record for tenant: {}", tenantSchema);
			});
		} catch (Exception e) {
			logger.error("Error dropping tenant schema/company for {}: {}", tenantSchema, e.getMessage(), e);
		}


		userRepository.delete(user);
		logger.info("Deleted AppUser: {}", username);

		return ResponseEntity.noContent().build();
	}

	public record DeleteAccountRequest(String password) {}

	@PostMapping("/me/2fa")
	public ResponseEntity<Boolean> toggleTwoFactor(org.springframework.security.core.Authentication authentication, @RequestBody java.util.Map<String, Boolean> body) {
		String username = authentication.getName();
		AppUser user = userRepository.findByUsername(username).orElse(null);
		if (user == null) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
		}

		Boolean enabled = body.get("enabled");
		if (enabled == null) enabled = false;

		user.setTwoFactorEnabled(enabled);
		userRepository.save(user);
		return ResponseEntity.ok(enabled);
	}

	@GetMapping("/me/2fa")
	public ResponseEntity<Boolean> getTwoFactorStatus(org.springframework.security.core.Authentication authentication) {
		String username = authentication.getName();
		AppUser user = userRepository.findByUsername(username).orElse(null);
		if (user == null) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
		}
		return ResponseEntity.ok(user.isTwoFactorEnabled());
	}

	@PutMapping("/me/preferences")
	public ResponseEntity<Void> updatePreferences(org.springframework.security.core.Authentication authentication, @RequestBody UpdatePreferencesRequest request) {
		String username = authentication.getName();
		AppUser user = userRepository.findByUsername(username).orElse(null);
		if (user == null) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
		}
		if (request.theme() != null) {
			String theme = request.theme();

			if (theme.equals("light") || theme.equals("dark")) {
				user.setTheme(theme);
			} else {
				user.setTheme("light");
			}
		}
		userRepository.save(user);
		return ResponseEntity.ok().build();
	}
}
