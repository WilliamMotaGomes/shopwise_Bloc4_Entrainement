package com.shopwise.app.recommendation.web;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.shopwise.app.recommendation.service.RecommendationService;

@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {
	private final RecommendationService service;

	public RecommendationController(RecommendationService service) {
		this.service = service;
	}

	@GetMapping
	public ResponseEntity<RecommendationResponse> recommend(
			@RequestParam(name = "productIds", required = false) List<Long> productIds,
			@RequestParam(name = "limit", required = false) Integer limit) {
		return ResponseEntity.ok(RecommendationResponse.from(service.recommend(productIds, limit)));
	}

	@PostMapping("/retrain")
	public ResponseEntity<RecommendationResponse.ModelSummary> retrain() {
		return ResponseEntity.ok(RecommendationResponse.ModelSummary.from(service.retrain()));
	}
}
