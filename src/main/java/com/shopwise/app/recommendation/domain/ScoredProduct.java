package com.shopwise.app.recommendation.domain;

public record ScoredProduct(long productId, double score, RecommendationReason reason) {
}
