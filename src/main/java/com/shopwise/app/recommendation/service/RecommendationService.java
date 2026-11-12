package com.shopwise.app.recommendation.service;

import java.time.Clock;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.shopwise.app.exception.BadRequestException;
import com.shopwise.app.exception.NotFoundException;
import com.shopwise.app.recommendation.domain.CatalogItem;
import com.shopwise.app.recommendation.domain.CatalogProvider;
import com.shopwise.app.recommendation.domain.ModelInfo;
import com.shopwise.app.recommendation.domain.PurchaseHistoryProvider;
import com.shopwise.app.recommendation.domain.RecommendationModel;
import com.shopwise.app.recommendation.domain.RecommendationModelTrainer;
import com.shopwise.app.recommendation.domain.ScoredProduct;

@Service
public class RecommendationService {
	public static final int DEFAULT_LIMIT = 5;
	public static final int MAX_LIMIT = 20;

	private final CatalogProvider catalogProvider;
	private final PurchaseHistoryProvider historyProvider;
	private final RecommendationModelTrainer trainer;
	private final Clock clock;

	private volatile Snapshot snapshot;

	private record Snapshot(String version, RecommendationModel model, Map<Long, CatalogItem> catalog) {
	}

	public RecommendationService(CatalogProvider catalogProvider, PurchaseHistoryProvider historyProvider,
			RecommendationModelTrainer trainer, Clock clock) {
		this.catalogProvider = catalogProvider;
		this.historyProvider = historyProvider;
		this.trainer = trainer;
		this.clock = clock;
	}

	public RecommendationResult recommend(List<Long> productIds, Integer limit) {
		int effectiveLimit = limit == null ? DEFAULT_LIMIT : limit;
		if (effectiveLimit < 1 || effectiveLimit > MAX_LIMIT) {
			throw new BadRequestException("limit must be between 1 and " + MAX_LIMIT);
		}
		List<Long> basis = productIds == null ? List.of() : List.copyOf(new LinkedHashSet<>(productIds));

		Snapshot current = currentSnapshot();
		for (Long id : basis) {
			if (id == null || !current.model().knows(id)) {
				throw new NotFoundException("Product not found with id " + id);
			}
		}

		List<ScoredProduct> scored = current.model().recommend(basis, effectiveLimit);
		List<RecommendationResult.Item> items = scored.stream().map(s -> {
			CatalogItem c = current.catalog().get(s.productId());
			return new RecommendationResult.Item(s.productId(), c.name(), c.price(), s.score(), s.reason());
		}).toList();
		String strategy = basis.isEmpty() ? "BEST_SELLERS" : "HYBRID_ITEM_KNN";
		return new RecommendationResult(basis, strategy, current.model().info(), items);
	}

	public ModelInfo retrain() {
		synchronized (this) {
			return train().model().info();
		}
	}

	private Snapshot currentSnapshot() {
		String version = catalogProvider.version() + "|" + historyProvider.version();
		Snapshot s = snapshot;
		if (s == null || !s.version().equals(version)) {
			synchronized (this) {
				s = snapshot;
				if (s == null || !s.version().equals(version)) {
					s = train();
				}
			}
		}
		return s;
	}

	private Snapshot train() {
		String version = catalogProvider.version() + "|" + historyProvider.version();
		List<CatalogItem> catalog = catalogProvider.loadCatalog();
		RecommendationModel model = trainer.train(catalog, historyProvider.loadBaskets(), clock.instant());
		Snapshot fresh = new Snapshot(version, model,
				catalog.stream().collect(Collectors.toMap(CatalogItem::id, Function.identity())));
		this.snapshot = fresh;
		return fresh;
	}
}
