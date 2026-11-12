package com.shopwise.app.recommendation.domain;

import java.time.Instant;

public record ModelInfo(String algorithm, Instant trainedAt, int productCount, int basketCount,
		double collaborativeWeight, boolean tuned, Double validationHitRate) {
}
