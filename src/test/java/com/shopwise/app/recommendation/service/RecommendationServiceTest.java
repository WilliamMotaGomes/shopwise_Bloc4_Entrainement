package com.shopwise.app.recommendation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.shopwise.app.exception.BadRequestException;
import com.shopwise.app.exception.NotFoundException;
import com.shopwise.app.recommendation.domain.Basket;
import com.shopwise.app.recommendation.domain.CatalogItem;
import com.shopwise.app.recommendation.domain.CatalogProvider;
import com.shopwise.app.recommendation.domain.PurchaseHistoryProvider;
import com.shopwise.app.recommendation.domain.RecommendationModelTrainer;
import com.shopwise.app.recommendation.engine.HybridConfig;
import com.shopwise.app.recommendation.engine.HybridModelTrainer;

/** Teste l'orchestration (cache, invalidation, validation) avec des fournisseurs en memoire : zero Spring, zero JPA. */
class RecommendationServiceTest {

	private final List<CatalogItem> catalog = List.of(
			new CatalogItem(1, "Coffee beans", "Arabica", new BigDecimal("12.00")),
			new CatalogItem(2, "Coffee filters", "Paper", new BigDecimal("3.00")),
			new CatalogItem(3, "Tea", "Green tea", new BigDecimal("8.00")));
	private final List<Basket> baskets = new ArrayList<>();
	private final AtomicInteger trainings = new AtomicInteger();

	private RecommendationService service;

	@BeforeEach
	void setUp() {
		CatalogProvider catalogProvider = new CatalogProvider() {
			public List<CatalogItem> loadCatalog() {
				return catalog;
			}

			public String version() {
				return "c" + catalog.size();
			}
		};
		PurchaseHistoryProvider history = new PurchaseHistoryProvider() {
			public List<Basket> loadBaskets() {
				return List.copyOf(baskets);
			}

			public String version() {
				return "h" + baskets.size();
			}
		};
		HybridModelTrainer delegate = new HybridModelTrainer(HybridConfig.defaults());
		RecommendationModelTrainer counting = (cat, b, now) -> {
			trainings.incrementAndGet();
			return delegate.train(cat, b, now);
		};
		service = new RecommendationService(catalogProvider, history, counting,
				Clock.fixed(Instant.parse("2026-10-22T10:00:00Z"), ZoneOffset.UTC));
	}

	@Test
	void modelIsCached_untilSalesChange() {
		service.recommend(List.of(1L), 3);
		service.recommend(List.of(2L), 3);
		assertThat(trainings.get()).isEqualTo(1);

		baskets.add(new Basket(1, Map.of(1L, 1, 2L, 1)));
		service.recommend(List.of(1L), 3);

		assertThat(trainings.get()).isEqualTo(2);
	}

	@Test
	void retrain_forcesNewTraining() {
		service.recommend(List.of(), 3);
		service.retrain();

		assertThat(trainings.get()).isEqualTo(2);
	}

	@Test
	void emptyBasis_usesBestSellersStrategy() {
		RecommendationResult result = service.recommend(null, null);

		assertThat(result.strategy()).isEqualTo("BEST_SELLERS");
		assertThat(result.items()).hasSize(3);
	}

	@Test
	void basis_usesHybridStrategy_andEnrichesWithCatalogData() {
		baskets.add(new Basket(1, Map.of(1L, 1, 2L, 1)));

		RecommendationResult result = service.recommend(List.of(1L, 1L), 1);

		assertThat(result.strategy()).isEqualTo("HYBRID_ITEM_KNN");
		assertThat(result.basis()).containsExactly(1L); // doublons supprimes
		assertThat(result.items()).singleElement().satisfies(i -> {
			assertThat(i.productId()).isEqualTo(2L);
			assertThat(i.name()).isEqualTo("Coffee filters");
			assertThat(i.price()).isEqualByComparingTo("3.00");
		});
	}

	@Test
	void unknownProduct_isNotFound() {
		assertThatThrownBy(() -> service.recommend(List.of(77L), 3)).isInstanceOf(NotFoundException.class);
	}

	@Test
	void invalidLimit_isBadRequest() {
		assertThatThrownBy(() -> service.recommend(List.of(), 0)).isInstanceOf(BadRequestException.class);
		assertThatThrownBy(() -> service.recommend(List.of(), RecommendationService.MAX_LIMIT + 1))
				.isInstanceOf(BadRequestException.class);
	}
}
