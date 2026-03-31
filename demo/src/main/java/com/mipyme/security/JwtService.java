package com.mipyme.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

	private final SecretKey key;
	private final long expirationSeconds;
	private final long refreshExpirationSeconds;

	public JwtService(
			@Value("${jwt.secret:mySecretKeyForDevelopmentOnlyChange12345}") String secret,
			@Value("${jwt.expiration-seconds:3600}") long expirationSeconds,
			@Value("${jwt.refresh-expiration-seconds:86400}") long refreshExpirationSeconds
	) {
		this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
		this.expirationSeconds = expirationSeconds;
		this.refreshExpirationSeconds = refreshExpirationSeconds;
	}

	public String generateToken(String subject, String companyId, String role, String permissions, String sid, String planTier, String planStatus) {
		return generateToken(subject, companyId, role, permissions, sid, planTier, planStatus, expirationSeconds);
	}

	public String generateRefreshToken(String subject, String companyId, String role, String permissions, String sid, String planTier, String planStatus) {
		return generateToken(subject, companyId, role, permissions, sid, planTier, planStatus, refreshExpirationSeconds);
	}

	private String generateToken(String subject, String companyId, String role, String permissions, String sid, String planTier, String planStatus, long expiration) {
		Instant now = Instant.now();
		Instant exp = now.plusSeconds(expiration);

		return Jwts.builder()
				.subject(subject)
				.claim("companyId", companyId)
				.claim("role", role)
				.claim("permissions", permissions)
				.claim("sid", sid)
				.claim("planTier", planTier != null ? planTier.toLowerCase() : "base")
				.claim("planStatus", planStatus != null ? planStatus.toLowerCase() : "pending_payment")
				.issuedAt(Date.from(now))
				.expiration(Date.from(exp))
				.signWith(key)
				.compact();
	}

	public Claims validateAndGetClaims(String token) {
		return Jwts.parser()
				.verifyWith(key)
				.build()
				.parseSignedClaims(token)
				.getPayload();
	}

	public String validateAndGetSubject(String token) {
		return validateAndGetClaims(token).getSubject();
	}
}
