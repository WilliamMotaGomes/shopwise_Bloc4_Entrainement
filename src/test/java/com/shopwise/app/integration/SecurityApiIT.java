package com.shopwise.app.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtEncoder;

import com.shopwise.app.security.JwtConfig;
import com.shopwise.app.security.JwtService;

/** US 4 (403), US 5 (401) et US 6 (format d'erreur) de bout en bout. */
class SecurityApiIT extends AbstractApiIT {

	private static final String PRODUCT_JSON = "{\"name\":\"Mug\",\"description\":\"d\",\"price\":5.00}";
	private static final String SALE_JSON = "{\"items\":[{\"productId\":1,\"quantity\":1}]}";

	@Autowired
	JwtEncoder jwtEncoder;

	// ---------- 401 : pas de token / token invalide

	@Test
	void everyProtectedEndpoint_withoutToken_returns401() throws Exception {
		mvc.perform(get("/api/products")).andExpect(status().isUnauthorized());
		mvc.perform(get("/api/products/1")).andExpect(status().isUnauthorized());
		mvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content(PRODUCT_JSON))
				.andExpect(status().isUnauthorized());
		mvc.perform(put("/api/products/1").contentType(MediaType.APPLICATION_JSON).content(PRODUCT_JSON))
				.andExpect(status().isUnauthorized());
		mvc.perform(delete("/api/products/1")).andExpect(status().isUnauthorized());
		mvc.perform(get("/api/sales")).andExpect(status().isUnauthorized());
		mvc.perform(post("/api/sales").contentType(MediaType.APPLICATION_JSON).content(SALE_JSON))
				.andExpect(status().isUnauthorized());
		mvc.perform(get("/api/recommendations")).andExpect(status().isUnauthorized());
	}

	@Test
	void missingToken_returnsNormalizedJsonBody() throws Exception {
		mvc.perform(get("/api/sales")).andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value(401)).andExpect(jsonPath("$.error").value("UNAUTHORIZED"))
				.andExpect(jsonPath("$.message").isNotEmpty()).andExpect(jsonPath("$.path").value("/api/sales"))
				.andExpect(jsonPath("$.timestamp").isNotEmpty());
	}

	@Test
	void garbageToken_returns401() throws Exception {
		mvc.perform(get("/api/products").header("Authorization", "Bearer not.a.jwt"))
				.andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value(401));
	}

	@Test
	void tamperedToken_returns401() throws Exception {
		String token = adminToken();
		int i = token.length() - 10; // caractere au milieu de la signature (evite les bits de bourrage base64)
		char flipped = token.charAt(i) == 'A' ? 'B' : 'A';
		String tampered = token.substring(0, i) + flipped + token.substring(i + 1);

		mvc.perform(get("/api/products").header("Authorization", bearer(tampered)))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void expiredToken_returns401() throws Exception {
		JwtService expiredIssuer = new JwtService(jwtEncoder,
				Clock.fixed(Instant.now().minusSeconds(7200), ZoneOffset.UTC), 60);
		String expired = expiredIssuer.issue("admin", List.of("ADMIN"));

		mvc.perform(get("/api/products").header("Authorization", bearer(expired)))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void tokenSignedWithForeignKey_returns401() throws Exception {
		JwtConfig other = new JwtConfig();
		JwtService forger = new JwtService(other.jwtEncoder(other.jwtSecretKey("foreign-secret-key-0123456789-abcdef")),
				Clock.systemUTC(), 600);

		mvc.perform(get("/api/products").header("Authorization", bearer(forger.issue("admin", List.of("ADMIN")))))
				.andExpect(status().isUnauthorized());
	}

	// ---------- 403 : role insuffisant

	@Test
	void clerk_canRead_butCannotWriteProducts() throws Exception {
		String token = bearer(clerkToken());

		mvc.perform(get("/api/products").header("Authorization", token)).andExpect(status().isOk());
		mvc.perform(post("/api/products").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
				.content(PRODUCT_JSON)).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value(403))
				.andExpect(jsonPath("$.error").value("FORBIDDEN")).andExpect(jsonPath("$.message").isNotEmpty());
		mvc.perform(put("/api/products/1").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
				.content(PRODUCT_JSON)).andExpect(status().isForbidden());
		mvc.perform(delete("/api/products/1").header("Authorization", token)).andExpect(status().isForbidden());
	}

	@Test
	void clerk_cannotAccessSalesNorRecommendations() throws Exception {
		String token = bearer(clerkToken());

		mvc.perform(get("/api/sales").header("Authorization", token)).andExpect(status().isForbidden());
		mvc.perform(post("/api/sales").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
				.content(SALE_JSON)).andExpect(status().isForbidden());
		mvc.perform(get("/api/recommendations").header("Authorization", token)).andExpect(status().isForbidden());
	}

	@Test
	void merchant_cannotDeleteProducts_norRetrainModel() throws Exception {
		String token = bearer(merchantToken());

		mvc.perform(delete("/api/products/1").header("Authorization", token)).andExpect(status().isForbidden());
		mvc.perform(post("/api/recommendations/retrain").header("Authorization", token))
				.andExpect(status().isForbidden());
	}

	@Test
	void admin_canRetrainModel() throws Exception {
		mvc.perform(post("/api/recommendations/retrain").header("Authorization", bearer(adminToken())))
				.andExpect(status().isOk()).andExpect(jsonPath("$.algorithm").isNotEmpty());
	}

	// ---------- US 6 : autres erreurs normalisees

	@Test
	void unknownRoute_whenAuthenticated_returns404InStandardFormat() throws Exception {
		mvc.perform(get("/api/does-not-exist").header("Authorization", bearer(adminToken())))
				.andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value(404))
				.andExpect(jsonPath("$.error").value("NOT_FOUND"));
	}

	@Test
	void malformedJson_returns400InStandardFormat() throws Exception {
		mvc.perform(post("/api/products").header("Authorization", bearer(merchantToken()))
				.contentType(MediaType.APPLICATION_JSON).content("{not json")).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("MALFORMED_REQUEST"));
	}

	@Test
	void wrongPathVariableType_returns400() throws Exception {
		mvc.perform(get("/api/products/abc").header("Authorization", bearer(adminToken())))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(400));
	}
}
