package com.mipyme;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import org.springframework.security.crypto.password.PasswordEncoder;

import com.mipyme.security.JwtService;

@SpringBootTest
@TestPropertySource(properties = {
		"spring.datasource.url=jdbc:h2:mem:mipyme;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
		"spring.datasource.driverClassName=org.h2.Driver",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.jpa.hibernate.ddl-auto=create-drop"
})
class MiPymeApplicationTests {

	@Autowired
	JwtService jwtService;

	@Test
	void contextLoads() {
	}

	@Test
	void jwtRoundTrip() {
		String token = jwtService.generateToken("alice", "company-1", "ADMIN", "", null, null, null);
		assertThat(token).isNotBlank();
		assertThat(jwtService.validateAndGetSubject(token)).isEqualTo("alice");
	}

	@Test
	void passwordEncoderWorks(@Autowired PasswordEncoder passwordEncoder) {
		String hash = passwordEncoder.encode("password123");
		assertThat(passwordEncoder.matches("password123", hash)).isTrue();
	}

	@Test
	void refreshTokenHasLongerExpiration() {
		String token = jwtService.generateToken("test", "c1", "USER", "", null, null, null);
		String refreshToken = jwtService.generateRefreshToken("test", "c1", "USER", "", null, null, null);

		io.jsonwebtoken.Claims tokenClaims = jwtService.validateAndGetClaims(token);
		io.jsonwebtoken.Claims refreshClaims = jwtService.validateAndGetClaims(refreshToken);

		long tokenDuration = tokenClaims.getExpiration().getTime() - tokenClaims.getIssuedAt().getTime();
		long refreshDuration = refreshClaims.getExpiration().getTime() - refreshClaims.getIssuedAt().getTime();

		assertThat(refreshDuration).isGreaterThan(tokenDuration);
	}
}
