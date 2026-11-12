package com.shopwise.app.integration;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

/** US 7 et 8 : endpoint de recommandation de bout en bout (ventes reelles -> modele -> API). */
class RecommendationApiIT extends AbstractApiIT {

	@Test
	void withBasket_recommendsProductsBoughtTogether() throws Exception {
		long beans = createProduct("Espresso beans", "Roasted arabica", "12.00");
		long filter = createProduct("Espresso filter", "Paper", "3.00");
		long mug = createProduct("Marble mug", "Stone", "6.00");
		for (int i = 0; i < 5; i++) {
			createSale(new long[] { beans, 1 }, new long[] { filter, 1 });
		}
		createSale(new long[] { beans, 1 }, new long[] { mug, 1 });

		mvc.perform(get("/api/recommendations?productIds=" + beans + "&limit=2")
				.header("Authorization", bearer(merchantToken()))).andExpect(status().isOk())
				.andExpect(jsonPath("$.strategy").value("HYBRID_ITEM_KNN"))
				.andExpect(jsonPath("$.basis[0]").value(beans))
				.andExpect(jsonPath("$.recommendations.length()").value(2))
				.andExpect(jsonPath("$.recommendations[0].productId").value(filter))
				.andExpect(jsonPath("$.recommendations[0].name").value("Espresso filter"))
				.andExpect(jsonPath("$.recommendations[0].reason").value("SIMILAR_TO_BASKET"))
				.andExpect(jsonPath("$.recommendations[*].productId").value(not(hasItem((int) beans))))
				.andExpect(jsonPath("$.model.algorithm").isNotEmpty());
	}

	@Test
	void withoutBasket_returnsBestSellers() throws Exception {
		long a = createProduct("Best seller item", "x", "1.00");
		long b = createProduct("Slow seller item", "y", "1.00");
		createSale(new long[] { a, 10 });
		createSale(new long[] { b, 1 });

		mvc.perform(get("/api/recommendations?limit=1").header("Authorization", bearer(merchantToken())))
				.andExpect(status().isOk()).andExpect(jsonPath("$.strategy").value("BEST_SELLERS"))
				.andExpect(jsonPath("$.recommendations[0].productId").value(a))
				.andExpect(jsonPath("$.recommendations[0].reason").value("BEST_SELLER"))
				.andExpect(jsonPath("$.recommendations[0].score").value(1.0));
	}

	@Test
	void newSales_automaticallyRefreshTheModel() throws Exception {
		long a = createProduct("Refresh alpha", "x", "2.00");
		long b = createProduct("Refresh bravo", "y", "2.00");
		mvc.perform(get("/api/recommendations?limit=1").header("Authorization", bearer(merchantToken())))
				.andExpect(jsonPath("$.model.basketCount").value(0));

		createSale(new long[] { a, 1 }, new long[] { b, 1 });

		mvc.perform(get("/api/recommendations?productIds=" + a + "&limit=1")
				.header("Authorization", bearer(merchantToken()))).andExpect(status().isOk())
				.andExpect(jsonPath("$.model.basketCount").value(1))
				.andExpect(jsonPath("$.recommendations[0].productId").value(b));
	}

	@Test
	void unknownProduct_returns404() throws Exception {
		mvc.perform(get("/api/recommendations?productIds=777777").header("Authorization", bearer(merchantToken())))
				.andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value(404));
	}

	@Test
	void invalidLimit_returns400() throws Exception {
		mvc.perform(get("/api/recommendations?limit=0").header("Authorization", bearer(merchantToken())))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("BAD_REQUEST"));
		mvc.perform(get("/api/recommendations?limit=1000").header("Authorization", bearer(merchantToken())))
				.andExpect(status().isBadRequest());
	}

	@Test
	void nonNumericProductId_returns400() throws Exception {
		mvc.perform(get("/api/recommendations?productIds=abc").header("Authorization", bearer(merchantToken())))
				.andExpect(status().isBadRequest());
	}
}
