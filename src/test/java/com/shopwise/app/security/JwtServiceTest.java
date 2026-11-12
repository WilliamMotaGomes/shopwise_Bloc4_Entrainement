package com.shopwise.app.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

class JwtServiceTest {

	private static final String SECRET = "unit-test-secret-0123456789-0123456789";

	private final JwtConfig config = new JwtConfig();
	private final SecretKey key = config.jwtSecretKey(SECRET);
	private final JwtDecoder decoder = config.jwtDecoder(key);

	@Test
	void issuedToken_carriesSubjectRolesAndExpiry() {
		Instant now = Instant.now();
		JwtService service = new JwtService(config.jwtEncoder(key), Clock.fixed(now, ZoneOffset.UTC), 600);

		Jwt jwt = decoder.decode(service.issue("marie", List.of("MERCHANT")));

		assertThat(jwt.getSubject()).isEqualTo("marie");
		assertThat(jwt.getClaimAsStringList("roles")).containsExactly("MERCHANT");
		assertThat(jwt.getExpiresAt()).isAfter(now.plusSeconds(500));
	}

	@Test
	void expiredToken_isRejected() {
		JwtService service = new JwtService(config.jwtEncoder(key),
				Clock.fixed(Instant.now().minusSeconds(7200), ZoneOffset.UTC), 60);

		String token = service.issue("marie", List.of("MERCHANT"));

		assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtException.class);
	}

	@Test
	void tokenSignedWithAnotherKey_isRejected() {
		SecretKey other = config.jwtSecretKey("another-secret-key-0123456789-abcdef");
		JwtService forger = new JwtService(config.jwtEncoder(other), Clock.systemUTC(), 600);

		String forged = forger.issue("admin", List.of("ADMIN"));

		assertThatThrownBy(() -> decoder.decode(forged)).isInstanceOf(JwtException.class);
	}

	@Test
	void shortSecret_isRefusedAtStartup() {
		assertThatThrownBy(() -> config.jwtSecretKey("too-short")).isInstanceOf(IllegalStateException.class);
	}
}
