package com.mipyme.security;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.mipyme.access.service.AccessService;
import com.mipyme.company.Company;
import com.mipyme.company.CompanyService;
import com.mipyme.tenant.TenantContext;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

	private final JwtService jwtService;
	private final UserDetailsService userDetailsService;
	private final CompanyService companyService;
	private final AccessService accessService;

	public JwtAuthenticationFilter(JwtService jwtService, UserDetailsService userDetailsService,
			CompanyService companyService, AccessService accessService) {
		this.jwtService = jwtService;
		this.userDetailsService = userDetailsService;
		this.companyService = companyService;
		this.accessService = accessService;
	}

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {
		TenantContext.clear();

		String authHeader = request.getHeader("Authorization");

		if (authHeader == null || !authHeader.startsWith("Bearer ")) {
			filterChain.doFilter(request, response);
			return;
		}

		logger.info("Incoming request: {} {}", request.getMethod(), request.getRequestURI());

		String token = authHeader.substring("Bearer ".length());

		try {
			Claims claims = jwtService.validateAndGetClaims(token);
			String username = claims.getSubject();
			String companyId = claims.get("companyId", String.class);

			if (username == null || username.isBlank() || companyId == null || companyId.isBlank()) {
				logger.warn("JWT missing username or companyId. Username: {}, CompanyId: {}", username, companyId);
				filterChain.doFilter(request, response);
				return;
			}



			Company company = companyService.findById(companyId).orElse(null);
			if (company == null) {
				logger.warn("Company not found for id: {}", companyId);
				filterChain.doFilter(request, response);
				return;
			}

			TenantContext.setCurrentTenant(company.getTenantSchema());

			String sid = claims.get("sid", String.class);
			if (!accessService.isSessionValid(sid)) {
				logger.warn("Session invalid or revoked for sid: {}", sid);


				filterChain.doFilter(request, response);
				return;
			}

			if (SecurityContextHolder.getContext().getAuthentication() == null) {
				UserDetails userDetails = userDetailsService.loadUserByUsername(username);
				UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
						userDetails,
						null,
						userDetails.getAuthorities());
				authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
				SecurityContextHolder.getContext().setAuthentication(authentication);
			}

			filterChain.doFilter(request, response);
		} catch (org.springframework.security.core.userdetails.UsernameNotFoundException ex) {
			logger.warn("User not found during JWT authentication: {}", ex.getMessage());

			filterChain.doFilter(request, response);
		} catch (JwtException ex) {
			logger.error("JWT Validation failed: {}", ex.getMessage());
			filterChain.doFilter(request, response);
		} finally {
			TenantContext.clear();
		}
	}
}
