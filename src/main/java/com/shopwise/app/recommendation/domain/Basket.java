package com.shopwise.app.recommendation.domain;

import java.util.Map;
import java.util.Set;

public record Basket(long saleId, Map<Long, Integer> quantities) {
	public Set<Long> productIds() {
		return quantities.keySet();
	}
}
