package com.mipyme.security;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration(proxyBeanMethods = false)
public class SecurityConfig {

	private final JwtAuthenticationFilter jwtAuthenticationFilter;
	@Value("${mipyme.cors.allowed-origins:}")
	private final String allowedOrigins;
	private final CorsConfigurationSource corsConfigurationSourceBean;

	public SecurityConfig(
			JwtAuthenticationFilter jwtAuthenticationFilter,
			@Value("${mipyme.cors.allowed-origins:}") String allowedOrigins,
			CorsConfigurationSource corsConfigurationSourceBean) {
		this.jwtAuthenticationFilter = jwtAuthenticationFilter;
		this.allowedOrigins = allowedOrigins;
		this.corsConfigurationSourceBean = corsConfigurationSourceBean;
	}

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		return http
				.csrf(csrf -> csrf.disable())
				.cors(cors -> cors.configurationSource(corsConfigurationSourceBean))
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.formLogin(form -> form.disable())
				.httpBasic(basic -> basic.disable())
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/api/auth/**").permitAll()
						.requestMatchers("/api/companies/sso/**").permitAll()
						.requestMatchers("/api/companies/register").permitAll()
						.requestMatchers("/api/mercadolibre/webhooks").permitAll()
						.requestMatchers("/api/mercadopago/webhooks").permitAll()
						.requestMatchers("/api/mercadopago/pricing").permitAll()
						.requestMatchers("/api/mercadopago/admin/**").permitAll()
						.requestMatchers("/api/whatsapp/webhook").permitAll()
					.requestMatchers("/api/whatsapp/debug").permitAll()
					.requestMatchers("/api/arca/p12").permitAll()
						.requestMatchers("/ws/**").permitAll()
						.requestMatchers("/ws").permitAll()
						.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
						.anyRequest().authenticated())
				.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
				.build();
	}

	@Bean
	CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();
		List<String> origins;
		if (allowedOrigins == null || allowedOrigins.isBlank()) {
			origins = List.of();
		} else {
			origins = Arrays.stream(allowedOrigins.split(","))
					.map(String::trim)
					.filter(s -> !s.isEmpty())
					.toList();
		}
		configuration.setAllowedOrigins(origins);
		configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
		configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With", "Origin",
				"Accept", "X-TiendaNube-Access-Token", "X-Caller-Id"));
		configuration.setAllowCredentials(true);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}

	@Bean
	CorsFilter corsFilter() {
		return new CorsFilter(corsConfigurationSourceBean);
	}
}