package com.shopwise.app.security;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
	private final JwtEncoder encoder;
	private final Clock clock;
	private final long ttlSeconds;

	public JwtService(JwtEncoder encoder, Clock clock, @Value("${shopwise.security.jwt.ttl-seconds}") long ttlSeconds) {
		this.encoder = encoder;
		this.clock = clock;
		this.ttlSeconds = ttlSeconds;
	}

	public String issue(String username, List<String> roles) {
		Instant now = clock.instant();
		JwtClaimsSet claims = JwtClaimsSet.builder().issuer("shopwise").subject(username).issuedAt(now)
				.expiresAt(now.plusSeconds(ttlSeconds)).claim(JwtConfig.ROLES_CLAIM, roles).build();
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
	}

	public long getTtlSeconds() {
		return ttlSeconds;
	}
}
