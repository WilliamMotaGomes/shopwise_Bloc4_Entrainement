package com.shopwise.app.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.jayway.jsonpath.JsonPath;
import com.shopwise.app.repository.ProductRepository;
import com.shopwise.app.repository.SaleRepository;

/**
 * Socle des tests d'integration/API : contexte Spring complet + base H2 + chaine de filtres Spring
 * Security reelle (les 401/403 sont donc testes de bout en bout, tokens JWT reels).
 */
@SpringBootTest
abstract class AbstractApiIT {

	private static final Map<String, String> TOKEN_CACHE = new ConcurrentHashMap<>();

	@Autowired
	protected WebApplicationContext context;
	@Autowired
	protected FilterChainProxy springSecurityFilterChain;
	@Autowired
	protected SaleRepository saleRepository;
	@Autowired
	protected ProductRepository productRepository;

	protected MockMvc mvc;
	private final List<Long> createdProductIds = new ArrayList<>();

	@BeforeEach
	void initMvc() {
		mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(springSecurityFilterChain).build();
		saleRepository.deleteAll();
	}

	@AfterEach
	void cleanUp() {
		saleRepository.deleteAll();
		productRepository.deleteAllById(createdProductIds);
		createdProductIds.clear();
	}

	protected String adminToken() throws Exception {
		return token("admin", "admin123");
	}

	protected String merchantToken() throws Exception {
		return token("marie", "marie123");
	}

	protected String clerkToken() throws Exception {
		return token("clerk", "clerk123");
	}

	private String token(String username, String password) throws Exception {
		String cached = TOKEN_CACHE.get(username);
		if (cached != null) {
			return cached;
		}
		MvcResult result = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}")).andReturn();
		String token = JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
		TOKEN_CACHE.put(username, token);
		return token;
	}

	protected static String bearer(String token) {
		return "Bearer " + token;
	}

	/** Cree un produit via l'API (role MERCHANT) et le supprime automatiquement en fin de test. */
	protected long createProduct(String name, String description, String price) throws Exception {
		MvcResult result = mvc.perform(post("/api/products").header("Authorization", bearer(merchantToken()))
				.contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"" + name + "\",\"description\":\""
						+ description + "\",\"price\":" + price + "}"))
				.andReturn();
		Number id = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
		createdProductIds.add(id.longValue());
		return id.longValue();
	}

	/** Enregistre une vente (role MERCHANT) ; items = paires {productId, quantity}. */
	protected long createSale(long[]... items) throws Exception {
		StringBuilder json = new StringBuilder("{\"items\":[");
		for (int i = 0; i < items.length; i++) {
			if (i > 0) {
				json.append(',');
			}
			json.append("{\"productId\":").append(items[i][0]).append(",\"quantity\":").append(items[i][1]).append('}');
		}
		json.append("]}");
		MvcResult result = mvc.perform(post("/api/sales").header("Authorization", bearer(merchantToken()))
				.contentType(MediaType.APPLICATION_JSON).content(json.toString())).andReturn();
		Number id = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
		return id.longValue();
	}
}
