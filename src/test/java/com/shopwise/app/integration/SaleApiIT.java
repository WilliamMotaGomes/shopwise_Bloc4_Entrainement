package com.shopwise.app.integration;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/** US 1, 2, 3 : module Ventes. */
class SaleApiIT extends AbstractApiIT {

	@Test
	void createSale_withSeveralProducts_computesTotalAutomatically() throws Exception {
		long mug = createProduct("Mug", "d", "5.50");
		long tea = createProduct("Tea", "d", "3.20");

		mvc.perform(post("/api/sales").header("Authorization", bearer(merchantToken()))
				.contentType(MediaType.APPLICATION_JSON).content("{\"items\":[{\"productId\":" + mug
						+ ",\"quantity\":2},{\"productId\":" + tea + ",\"quantity\":3}]}"))
				.andExpect(status().isCreated()).andExpect(header().string("Location", containsString("/api/sales/")))
				.andExpect(jsonPath("$.id").isNumber()).andExpect(jsonPath("$.items.length()").value(2))
				.andExpect(jsonPath("$.items[0].lineTotal").value(11.00))
				.andExpect(jsonPath("$.items[1].lineTotal").value(9.60))
				.andExpect(jsonPath("$.totalAmount").value(20.60));
	}

	@Test
	void createdSale_isPersistedAndConsultable() throws Exception {
		long mug = createProduct("Mug", "d", "5.00");
		long saleId = createSale(new long[] { mug, 4 });

		mvc.perform(get("/api/sales/" + saleId).header("Authorization", bearer(merchantToken())))
				.andExpect(status().isOk()).andExpect(jsonPath("$.id").value(saleId))
				.andExpect(jsonPath("$.items[0].productId").value(mug))
				.andExpect(jsonPath("$.items[0].productName").value("Mug"))
				.andExpect(jsonPath("$.items[0].quantity").value(4))
				.andExpect(jsonPath("$.items[0].unitPrice").value(5.00))
				.andExpect(jsonPath("$.items[0].lineTotal").value(20.00))
				.andExpect(jsonPath("$.totalAmount").value(20.00));
	}

	@Test
	void duplicateProductLines_areMerged() throws Exception {
		long mug = createProduct("Mug", "d", "5.00");

		mvc.perform(post("/api/sales").header("Authorization", bearer(merchantToken()))
				.contentType(MediaType.APPLICATION_JSON).content("{\"items\":[{\"productId\":" + mug
						+ ",\"quantity\":1},{\"productId\":" + mug + ",\"quantity\":2}]}"))
				.andExpect(status().isCreated()).andExpect(jsonPath("$.items.length()").value(1))
				.andExpect(jsonPath("$.items[0].quantity").value(3)).andExpect(jsonPath("$.totalAmount").value(15.00));
	}

	@Test
	void unitPrice_isASnapshot_laterPriceChangeDoesNotRewriteHistory() throws Exception {
		long mug = createProduct("Mug", "d", "5.00");
		long saleId = createSale(new long[] { mug, 1 });

		mvc.perform(put("/api/products/" + mug).header("Authorization", bearer(merchantToken()))
				.contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Mug\",\"description\":\"d\",\"price\":99.00}"))
				.andExpect(status().isOk());

		mvc.perform(get("/api/sales/" + saleId).header("Authorization", bearer(merchantToken())))
				.andExpect(jsonPath("$.items[0].unitPrice").value(5.00)).andExpect(jsonPath("$.totalAmount").value(5.00));
	}

	@Test
	void listSales_isSortedMostRecentFirst_withMainInformation() throws Exception {
		long mug = createProduct("Mug", "d", "2.00");
		long first = createSale(new long[] { mug, 1 });
		long second = createSale(new long[] { mug, 2 }, new long[] { 1, 1 });
		long third = createSale(new long[] { mug, 3 });

		mvc.perform(get("/api/sales").header("Authorization", bearer(merchantToken()))).andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(3)).andExpect(jsonPath("$[0].id").value(third))
				.andExpect(jsonPath("$[1].id").value(second)).andExpect(jsonPath("$[2].id").value(first))
				.andExpect(jsonPath("$[1].itemCount").value(2)).andExpect(jsonPath("$[0].totalAmount").value(6.00))
				.andExpect(jsonPath("$[0].saleDate").isNotEmpty());
	}

	@Test
	void getSale_unknown_returns404WithClearMessage() throws Exception {
		mvc.perform(get("/api/sales/987654").header("Authorization", bearer(merchantToken())))
				.andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value(404))
				.andExpect(jsonPath("$.message").value("Sale not found with id 987654"));
	}

	@Test
	void createSale_withUnknownProduct_returns400() throws Exception {
		mvc.perform(post("/api/sales").header("Authorization", bearer(merchantToken()))
				.contentType(MediaType.APPLICATION_JSON).content("{\"items\":[{\"productId\":888888,\"quantity\":1}]}"))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("BAD_REQUEST"))
				.andExpect(jsonPath("$.message").value(containsString("888888")));
	}

	@Test
	void createSale_withInvalidPayload_returns400() throws Exception {
		mvc.perform(post("/api/sales").header("Authorization", bearer(merchantToken()))
				.contentType(MediaType.APPLICATION_JSON).content("{\"items\":[]}")).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
		mvc.perform(post("/api/sales").header("Authorization", bearer(merchantToken()))
				.contentType(MediaType.APPLICATION_JSON).content("{\"items\":[{\"productId\":1,\"quantity\":0}]}"))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.details[0].message").isNotEmpty());
		mvc.perform(post("/api/sales").header("Authorization", bearer(merchantToken()))
				.contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isBadRequest());
	}

	@Test
	void adminCanAlsoManageSales() throws Exception {
		mvc.perform(post("/api/sales").header("Authorization", bearer(adminToken()))
				.contentType(MediaType.APPLICATION_JSON).content("{\"items\":[{\"productId\":1,\"quantity\":1}]}"))
				.andExpect(status().isCreated()).andExpect(jsonPath("$.totalAmount").value(999.99));
	}
}
