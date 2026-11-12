package com.shopwise.app.recommendation.domain;

import java.util.Collection;
import java.util.List;

public interface RecommendationModel {
	List<ScoredProduct> recommend(Collection<Long> seedProductIds, int limit);

	boolean knows(long productId);

	ModelInfo info();
}
