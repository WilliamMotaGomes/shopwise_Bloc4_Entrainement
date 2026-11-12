package com.shopwise.app.recommendation.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.shopwise.app.recommendation.domain.ModelInfo;
import com.shopwise.app.recommendation.service.RecommendationResult;

public record RecommendationResponse(List<Long> basis, String strategy, ModelSummary model,
		List<RecommendedProduct> recommendations) {
	public record RecommendedProduct(long productId, String name, BigDecimal price, double score, String reason) {
	}

	public record ModelSummary(String algorithm, Instant trainedAt, int productCount, int basketCount,
			double collaborativeWeight, boolean tuned, Double validationHitRate) {
		public static ModelSummary from(ModelInfo i) {
			return new ModelSummary(i.algorithm(), i.trainedAt(), i.productCount(), i.basketCount(),
					i.collaborativeWeight(), i.tuned(), i.validationHitRate());
		}
	}

	public static RecommendationResponse from(RecommendationResult r) {
		return new RecommendationResponse(r.basis(), r.strategy(), ModelSummary.from(r.model()),
				r.items().stream().map(i -> new RecommendedProduct(i.productId(), i.name(), i.price(),
						Math.round(i.score() * 10000.0) / 10000.0, i.reason().name())).toList());
	}
}
