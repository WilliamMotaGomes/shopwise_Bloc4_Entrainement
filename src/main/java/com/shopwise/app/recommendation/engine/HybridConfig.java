package com.shopwise.app.recommendation.engine;

public record HybridConfig(double defaultCollaborativeWeight, double contentTextWeight, int topK,
		int minValidationBaskets, double validationRatio) {
	public static HybridConfig defaults() {
		return new HybridConfig(0.6, 0.8, 5, 5, 0.2);
	}
}
