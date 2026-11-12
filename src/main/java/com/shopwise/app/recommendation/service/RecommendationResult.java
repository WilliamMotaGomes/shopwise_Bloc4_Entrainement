package com.shopwise.app.recommendation.service;

import java.math.BigDecimal;
import java.util.List;

import com.shopwise.app.recommendation.domain.ModelInfo;
import com.shopwise.app.recommendation.domain.RecommendationReason;

public record RecommendationResult(List<Long> basis, String strategy, ModelInfo model, List<Item> items) {
	public record Item(long productId, String name, BigDecimal price, double score, RecommendationReason reason) {
	}
}
