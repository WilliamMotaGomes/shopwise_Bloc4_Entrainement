package com.shopwise.app.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class AuthApiIT extends AbstractApiIT {

	@Test
	void login_withValidCredentials_returnsBearerToken() throws Exception {
		mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"username\":\"marie\",\"password\":\"marie123\"}")).andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").isNotEmpty()).andExpect(jsonPath("$.tokenType").value("Bearer"))
				.andExpect(jsonPath("$.roles[0]").value("MERCHANT"));
	}

	@Test
	void login_withWrongPassword_returns401InStandardFormat() throws Exception {
		mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"username\":\"marie\",\"password\":\"wrong\"}")).andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value(401)).andExpect(jsonPath("$.error").value("UNAUTHORIZED"))
				.andExpect(jsonPath("$.message").isNotEmpty());
	}

	@Test
	void login_withMissingFields_returns400WithFieldDetails() throws Exception {
		mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.details.length()").value(2));
	}
}
