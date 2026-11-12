package com.shopwise.app.recommendation.domain;

import java.time.Instant;
import java.util.List;

public interface RecommendationModelTrainer {
	RecommendationModel train(List<CatalogItem> catalog, List<Basket> baskets, Instant now);
}
