package com.shopwise.app.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.shopwise.app.dto.request.LoginRequest;
import com.shopwise.app.dto.response.LoginResponse;

class AuthServiceTest {

	private final UserStoreConfig users = new UserStoreConfig();
	private final PasswordEncoder encoder = users.passwordEncoder();
	private final JwtConfig jwt = new JwtConfig();
	private final AuthService service = new AuthService(users.userDetailsService(encoder, "a-pass", "m-pass", "c-pass"),
			encoder, new JwtService(jwt.jwtEncoder(jwt.jwtSecretKey("unit-test-secret-0123456789-0123456789")),
					Clock.systemUTC(), 3600));

	@Test
	void validCredentials_returnBearerTokenAndRoles() {
		LoginResponse response = service.login(new LoginRequest("marie", "m-pass"));

		assertThat(response.accessToken()).isNotBlank();
		assertThat(response.tokenType()).isEqualTo("Bearer");
		assertThat(response.roles()).containsExactly("MERCHANT");
		assertThat(response.expiresIn()).isEqualTo(3600);
	}

	@Test
	void wrongPassword_isRejected() {
		assertThatThrownBy(() -> service.login(new LoginRequest("marie", "nope")))
				.isInstanceOf(BadCredentialsException.class);
	}

	@Test
	void unknownUser_isRejectedWithSameMessage() {
		assertThatThrownBy(() -> service.login(new LoginRequest("ghost", "x")))
				.isInstanceOf(BadCredentialsException.class).hasMessage("Invalid credentials");
	}
}
