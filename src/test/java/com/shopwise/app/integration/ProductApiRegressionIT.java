package com.shopwise.app.integration;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/**
 * NON-REGRESSION : le catalogue historique (creation, mise a jour, liste, detail, suppression)
 * doit se comporter comme avant les evolutions (contrat JSON et codes HTTP inchanges).
 */
class ProductApiRegressionIT extends AbstractApiIT {

	@Test
	void seededCatalog_isStillListed() throws Exception {
		mvc.perform(get("/api/products").header("Authorization", bearer(clerkToken()))).andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(4)))
				.andExpect(jsonPath("$[*].name").value(hasItem("iPhone 15")))
				.andExpect(jsonPath("$[*].name").value(hasItem("Dell XPS 13")));
	}

	@Test
	void getById_returnsHistoricalContract() throws Exception {
		mvc.perform(get("/api/products/1").header("Authorization", bearer(clerkToken()))).andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(1)).andExpect(jsonPath("$.name").value("iPhone 15"))
				.andExpect(jsonPath("$.description").isNotEmpty()).andExpect(jsonPath("$.price").value(999.99))
				.andExpect(jsonPath("$.createdAt").exists()).andExpect(jsonPath("$.updatedAt").exists());
	}

	@Test
	void create_returns201WithGeneratedId() throws Exception {
		String body = mvc.perform(post("/api/products").header("Authorization", bearer(merchantToken()))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Regression mug\",\"description\":\"d\",\"price\":7.50}"))
				.andExpect(status().isCreated()).andExpect(jsonPath("$.id").isNumber())
				.andExpect(jsonPath("$.name").value("Regression mug")).andReturn().getResponse().getContentAsString();
		Number id = com.jayway.jsonpath.JsonPath.read(body, "$.id");
		productRepository.deleteById(id.longValue());
	}

	@Test
	void update_thenGet_reflectsChanges() throws Exception {
		long id = createProduct("Before", "d", "10.00");

		mvc.perform(put("/api/products/" + id).header("Authorization", bearer(merchantToken()))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"After\",\"description\":\"new\",\"price\":12.00}")).andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("After")).andExpect(jsonPath("$.price").value(12.00));
		mvc.perform(get("/api/products/" + id).header("Authorization", bearer(clerkToken())))
				.andExpect(jsonPath("$.description").value("new"));
	}

	@Test
	void delete_returns204_thenGetReturns404() throws Exception {
		long id = createProduct("To delete", "d", "1.00");

		mvc.perform(delete("/api/products/" + id).header("Authorization", bearer(adminToken())))
				.andExpect(status().isNoContent());
		mvc.perform(get("/api/products/" + id).header("Authorization", bearer(adminToken())))
				.andExpect(status().isNotFound());
	}

	@Test
	void get_unknownProduct_returns404WithClearMessage() throws Exception {
		mvc.perform(get("/api/products/999999").header("Authorization", bearer(adminToken())))
				.andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value(404))
				.andExpect(jsonPath("$.message").value("Product not found"));
	}

	@Test
	void create_invalidPayload_returns400WithViolatedFields() throws Exception {
		mvc.perform(post("/api/products").header("Authorization", bearer(merchantToken()))
				.contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\",\"price\":-1}"))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.details[*].field").value(hasItem("name")))
				.andExpect(jsonPath("$.details[*].field").value(hasItem("price")));
	}

	@Test
	void delete_productAlreadySold_returns409_andProductIsKept() throws Exception {
		long id = createProduct("Sold product", "d", "3.00");
		createSale(new long[] { id, 1 });

		mvc.perform(delete("/api/products/" + id).header("Authorization", bearer(adminToken())))
				.andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("CONFLICT"));
		mvc.perform(get("/api/products/" + id).header("Authorization", bearer(adminToken())))
				.andExpect(status().isOk());
	}

	@Test
	void unsupportedMethod_returns405InStandardFormat() throws Exception {
		mvc.perform(put("/api/products").header("Authorization", bearer(adminToken()))
				.contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isMethodNotAllowed())
				.andExpect(jsonPath("$.code").value(405));
	}
}
