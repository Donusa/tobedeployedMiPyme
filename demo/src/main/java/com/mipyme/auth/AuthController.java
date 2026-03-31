package com.mipyme.auth;

import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mipyme.access.service.AccessService;
import com.mipyme.company.Company;
import com.mipyme.company.CompanyRepository;
import com.mipyme.security.JwtService;
import com.mipyme.tenant.TenantContext;
import com.mipyme.user.AppUser;
import com.mipyme.user.AppUserRepository;
import com.mipyme.user.UserRole;
import com.mipyme.email.EmailService;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final AppUserRepository userRepository;
	private final CompanyRepository companyRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final AccessService accessService;
	private final EmailService emailService;

	public AuthController(
			AppUserRepository userRepository,
			CompanyRepository companyRepository,
			PasswordEncoder passwordEncoder,
			JwtService jwtService,
			AccessService accessService,
			EmailService emailService
	) {
		this.userRepository = userRepository;
		this.companyRepository = companyRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
		this.accessService = accessService;
		this.emailService = emailService;
	}

	@PostMapping("/logout")
	public ResponseEntity<Void> logout(@RequestBody RefreshTokenRequest request) {
		String refreshToken = request.refreshToken();
		if (refreshToken != null && !refreshToken.isBlank()) {
			try {
				Claims claims = jwtService.validateAndGetClaims(refreshToken);
				String companyId = claims.get("companyId", String.class);
				Company company = companyRepository.findByCompanyId(companyId).orElse(null);

				if (company != null) {
					TenantContext.setCurrentTenant(company.getTenantSchema());
					try {
						accessService.revokeSessionByToken(refreshToken);
					} finally {
						TenantContext.clear();
					}
				}
			} catch (Exception e) {

			}
		}
		return ResponseEntity.ok().build();
	}

	@PostMapping("/refresh")
	public ResponseEntity<AuthResponse> refresh(@RequestBody RefreshTokenRequest request) {
		String refreshToken = request.refreshToken();
		if (refreshToken == null || refreshToken.isBlank()) {
			return ResponseEntity.badRequest().build();
		}

		try {
			Claims claims = jwtService.validateAndGetClaims(refreshToken);

			String companyId = claims.get("companyId", String.class);
			Company company = companyRepository.findByCompanyId(companyId).orElse(null);

			if (company == null) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
			}

			TenantContext.setCurrentTenant(company.getTenantSchema());
			try {



				String username = claims.getSubject();
				String role = claims.get("role", String.class);


				String permissions = "";
				AppUser user = userRepository.findByUsername(username).orElse(null);
				if (user != null) {
					permissions = user.getPermissions();
					if (UserRole.ADMIN.name().equals(user.getRole())) {
						permissions = "ALL";
					}
				} else {

					permissions = claims.get("permissions", String.class);
				}

				String sid = claims.get("sid", String.class);

				if (!accessService.isSessionValid(sid)) {
					return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
				}

				String planTierRefresh = company.getPlanTier();
				String planStatusRefresh = company.getPlanStatus() != null ? company.getPlanStatus().name().toLowerCase() : null;
				String newAccessToken = jwtService.generateToken(username, companyId, role, permissions, sid, planTierRefresh, planStatusRefresh);
				String newRefreshToken = jwtService.generateRefreshToken(username, companyId, role, permissions, sid, planTierRefresh, planStatusRefresh);


				accessService.rotateSessionToken(refreshToken, newRefreshToken);

				return ResponseEntity.ok(new AuthResponse(newAccessToken, newRefreshToken, companyId, role, null, null, permissions, false, null, planTierRefresh, planStatusRefresh));
			} finally {
				TenantContext.clear();
			}
		} catch (JwtException ex) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}
	}

	@PostMapping("/register")
	public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
		String ssoCode = request.ssoCode() == null ? null : request.ssoCode().trim();
		String email = request.email() == null ? null : request.email().trim();
		String name = request.name() == null ? null : request.name().trim();
		String password = request.password();
		UserRole role;
		try {
			role = UserRole.fromString(request.role());
		} catch (IllegalArgumentException ex) {
			role = null;
		}

		if (ssoCode == null || ssoCode.isBlank() || email == null || email.isBlank() || name == null || name.isBlank() || password == null || password.isBlank() || role == null) {
			return ResponseEntity.badRequest().build();
		}

		Company company = companyRepository.findBySsoCode(ssoCode).orElse(null);
		if (company == null) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
		}

		TenantContext.setCurrentTenant(company.getTenantSchema());
		try {
			if (userRepository.existsByUsername(email)) {
				return ResponseEntity.status(HttpStatus.CONFLICT).build();
			}


			String permissions = role == UserRole.ADMIN ? "ALL" : "";
			AppUser user = new AppUser(email, name, email, passwordEncoder.encode(password), role.name(), permissions);
			userRepository.save(user);

			String planTierReg = company.getPlanTier();
			String planStatusReg = company.getPlanStatus() != null ? company.getPlanStatus().name().toLowerCase() : null;
			String token = jwtService.generateToken(email, company.getCompanyId(), role.name(), permissions, null, planTierReg, planStatusReg);
			String refreshToken = jwtService.generateRefreshToken(email, company.getCompanyId(), role.name(), permissions, null, planTierReg, planStatusReg);
			return ResponseEntity.status(HttpStatus.CREATED).body(new AuthResponse(token, refreshToken, company.getCompanyId(), role.name(), company.getName(), user.getName(), user.getPermissions(), false, user.getTheme(), planTierReg, planStatusReg));
		} finally {
			TenantContext.clear();
		}
	}

	@PostMapping("/verify-2fa")
	public ResponseEntity<AuthResponse> verifyTwoFactor(@RequestBody VerifyTwoFactorRequest request, HttpServletRequest httpRequest) {
		String ssoCode = request.ssoCode() == null ? null : request.ssoCode().trim();
		String username = request.username() == null ? null : request.username().trim();
		String code = request.code() == null ? null : request.code().trim();

		if (ssoCode == null || ssoCode.isBlank() || username == null || username.isBlank() || code == null || code.isBlank()) {
			return ResponseEntity.badRequest().build();
		}

		Company company = companyRepository.findBySsoCode(ssoCode).orElse(null);
		if (company == null) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
		}

		TenantContext.setCurrentTenant(company.getTenantSchema());
		try {
			AppUser user = userRepository.findByUsername(username).orElse(null);
			if (user == null) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
			}

			if (user.getTwoFactorCode() == null || !user.getTwoFactorCode().equals(code) ||
				user.getTwoFactorCodeExpiresAt() == null || user.getTwoFactorCodeExpiresAt().isBefore(java.time.LocalDateTime.now())) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
			}


			user.setTwoFactorCode(null);
			user.setTwoFactorCodeExpiresAt(null);
			userRepository.save(user);


			String permissions = user.getPermissions();
			if (UserRole.ADMIN.name().equals(user.getRole())) {
				permissions = "ALL";
			}

			String sid = UUID.randomUUID().toString();
			String planTier2fa = company.getPlanTier();
			String planStatus2fa = company.getPlanStatus() != null ? company.getPlanStatus().name().toLowerCase() : null;
			String token = jwtService.generateToken(user.getUsername(), company.getCompanyId(), user.getRole(), permissions, sid, planTier2fa, planStatus2fa);
			String refreshToken = jwtService.generateRefreshToken(user.getUsername(), company.getCompanyId(), user.getRole(), permissions, sid, planTier2fa, planStatus2fa);

			String ip = accessService.getClientIp(httpRequest);
			String ua = accessService.getUserAgent(httpRequest);
			accessService.logAccess(user.getUsername(), ip, ua, "WEB");
			accessService.createSession(user.getUsername(), refreshToken, ip, ua, "WEB", sid);
			return ResponseEntity.ok(new AuthResponse(
					token,
					refreshToken,
					company.getCompanyId(),
					user.getRole(),
					company.getName(),
					user.getName(),
					permissions,
					false,
					user.getTheme(),
					planTier2fa,
					planStatus2fa
			));

		} finally {
			TenantContext.clear();
		}
	}

	@PostMapping("/login")
	public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {
		String ssoCode = request.ssoCode() == null ? null : request.ssoCode().trim();
		String username = request.username() == null ? null : request.username().trim();
		String password = request.password();

		if (ssoCode == null || ssoCode.isBlank() || username == null || username.isBlank() || password == null || password.isBlank()) {
			return ResponseEntity.badRequest().build();
		}

		Company company = companyRepository.findBySsoCode(ssoCode).orElse(null);
		if (company == null) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
		}

		TenantContext.setCurrentTenant(company.getTenantSchema());
		try {
			System.out.println("DEBUG: Login attempt for user: " + username + " in company: " + company.getName());
			var userOpt = userRepository.findByUsername(username);

			if (userOpt.isEmpty()) {
				System.out.println("DEBUG: User not found: " + username);
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
			}

			var user = userOpt.get();
			if (!passwordEncoder.matches(password, user.getPasswordHash())) {
				System.out.println("DEBUG: Password mismatch for user: " + username);
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
			}

			if (user.isTwoFactorEnabled()) {
				String code = String.format("%06d", new java.security.SecureRandom().nextInt(999999));
				user.setTwoFactorCode(code);
				user.setTwoFactorCodeExpiresAt(java.time.LocalDateTime.now().plusMinutes(10));
				userRepository.save(user);
				try {
					emailService.sendTwoFactorCode(user.getEmail(), code);
				} catch (Exception e) {
					System.out.println("DEBUG: Failed to send 2FA email: " + e.getMessage());
					e.printStackTrace();





				}
				return ResponseEntity.ok(new AuthResponse(null, null, company.getCompanyId(), user.getRole(), company.getName(), user.getName(), null, true, null, null, null));
			}


			String permissions = user.getPermissions();
			if (UserRole.ADMIN.name().equals(user.getRole())) {
				permissions = "ALL";
			}

			String sid = UUID.randomUUID().toString();
			String planTier = company.getPlanTier();
			String planStatus = company.getPlanStatus() != null ? company.getPlanStatus().name().toLowerCase() : null;
			String token = jwtService.generateToken(user.getUsername(), company.getCompanyId(), user.getRole(), permissions, sid, planTier, planStatus);
			String refreshToken = jwtService.generateRefreshToken(user.getUsername(), company.getCompanyId(), user.getRole(), permissions, sid, planTier, planStatus);

			String ip = accessService.getClientIp(httpRequest);
			String ua = accessService.getUserAgent(httpRequest);
			accessService.logAccess(user.getUsername(), ip, ua, "WEB");
			accessService.createSession(user.getUsername(), refreshToken, ip, ua, "WEB", sid);
			return ResponseEntity.ok(new AuthResponse(
					token,
					refreshToken,
					company.getCompanyId(),
					user.getRole(),
					company.getName(),
					user.getName(),
					permissions,
					false,
					user.getTheme(),
					planTier,
					planStatus
			));

		} finally {
			TenantContext.clear();
		}
	}

	@PostMapping("/forgot-password")
	public ResponseEntity<Void> forgotPassword(@RequestBody ForgotPasswordRequest request) {
		String ssoCode = request.ssoCode() == null ? null : request.ssoCode().trim();
		String email = request.email() == null ? null : request.email().trim();

		if (ssoCode == null || ssoCode.isBlank() || email == null || email.isBlank()) {
			return ResponseEntity.badRequest().build();
		}

		Company company = companyRepository.findBySsoCode(ssoCode).orElse(null);
		if (company == null) {

			return ResponseEntity.ok().build();
		}

		TenantContext.setCurrentTenant(company.getTenantSchema());
		try {
			AppUser user = userRepository.findByUsername(email).orElse(null);
			if (user != null) {
				String code = String.format("%06d", new java.security.SecureRandom().nextInt(999999));
				user.setPasswordRecoveryCode(code);
				user.setPasswordRecoveryExpiresAt(java.time.LocalDateTime.now().plusMinutes(15));
				userRepository.save(user);
				emailService.sendPasswordRecoveryCode(user.getEmail(), code);
			}
			return ResponseEntity.ok().build();
		} finally {
			TenantContext.clear();
		}
	}

	@PostMapping("/verify-recovery-code")
	public ResponseEntity<Void> verifyRecoveryCode(@RequestBody VerifyRecoveryCodeRequest request) {
		String ssoCode = request.ssoCode() == null ? null : request.ssoCode().trim();
		String email = request.email() == null ? null : request.email().trim();
		String code = request.code() == null ? null : request.code().trim();

		if (ssoCode == null || ssoCode.isBlank() || email == null || email.isBlank() || code == null || code.isBlank()) {
			return ResponseEntity.badRequest().build();
		}

		Company company = companyRepository.findBySsoCode(ssoCode).orElse(null);
		if (company == null) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
		}

		TenantContext.setCurrentTenant(company.getTenantSchema());
		try {
			AppUser user = userRepository.findByUsername(email).orElse(null);
			if (user == null) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
			}

			if (user.getPasswordRecoveryCode() == null || !user.getPasswordRecoveryCode().equals(code) ||
				user.getPasswordRecoveryExpiresAt() == null || user.getPasswordRecoveryExpiresAt().isBefore(java.time.LocalDateTime.now())) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
			}

			return ResponseEntity.ok().build();
		} finally {
			TenantContext.clear();
		}
	}

	@PostMapping("/reset-password")
	public ResponseEntity<Void> resetPassword(@RequestBody ResetPasswordRequest request) {
		String ssoCode = request.ssoCode() == null ? null : request.ssoCode().trim();
		String email = request.email() == null ? null : request.email().trim();
		String code = request.code() == null ? null : request.code().trim();
		String newPassword = request.newPassword();

		if (ssoCode == null || ssoCode.isBlank() || email == null || email.isBlank() ||
			code == null || code.isBlank() || newPassword == null || newPassword.isBlank()) {
			return ResponseEntity.badRequest().build();
		}

		Company company = companyRepository.findBySsoCode(ssoCode).orElse(null);
		if (company == null) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
		}

		TenantContext.setCurrentTenant(company.getTenantSchema());
		try {
			AppUser user = userRepository.findByUsername(email).orElse(null);
			if (user == null) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
			}

			if (user.getPasswordRecoveryCode() == null || !user.getPasswordRecoveryCode().equals(code) ||
				user.getPasswordRecoveryExpiresAt() == null || user.getPasswordRecoveryExpiresAt().isBefore(java.time.LocalDateTime.now())) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
			}

			user.setPasswordHash(passwordEncoder.encode(newPassword));
			user.setPasswordRecoveryCode(null);
			user.setPasswordRecoveryExpiresAt(null);
			userRepository.save(user);

			return ResponseEntity.ok().build();
		} finally {
			TenantContext.clear();
		}
	}
}
