package com.shopwise.app.recommendation;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.shopwise.app.recommendation.domain.RecommendationModelTrainer;
import com.shopwise.app.recommendation.engine.HybridConfig;
import com.shopwise.app.recommendation.engine.HybridModelTrainer;

@Configuration
public class RecommendationConfig {
	@Bean
	public RecommendationModelTrainer recommendationModelTrainer() {
		return new HybridModelTrainer(HybridConfig.defaults());
	}
}
