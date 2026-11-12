package com.shopwise.app.recommendation.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.shopwise.app.recommendation.domain.Basket;
import com.shopwise.app.recommendation.domain.CatalogItem;
import com.shopwise.app.recommendation.domain.RecommendationModel;
import com.shopwise.app.recommendation.domain.RecommendationReason;
import com.shopwise.app.recommendation.domain.ScoredProduct;

class HybridModelTrainerTest {

	private HybridModelTrainer trainer;
	private List<CatalogItem> catalog;

	@BeforeEach
	void setUp() {
		trainer = new HybridModelTrainer(HybridConfig.defaults());
		catalog = List.of(item(1, "Coffee beans 1kg", "Arabica roasted", "12.00"),
				item(2, "Coffee filters", "Paper filters", "3.00"), item(3, "Ceramic mug", "White mug", "6.00"),
				item(4, "Green tea", "Japanese sencha", "8.00"), item(5, "Sugar", "White sugar 1kg", "1.50"));
	}

	private static CatalogItem item(long id, String name, String description, String price) {
		return new CatalogItem(id, name, description, new BigDecimal(price));
	}

	private static Basket basket(long id, long... products) {
		Map<Long, Integer> quantities = new LinkedHashMap<>();
		for (long p : products) {
			quantities.put(p, 1);
		}
		return new Basket(id, quantities);
	}

	private List<Basket> coffeeHistory() {
		List<Basket> baskets = new ArrayList<>();
		long id = 1;
		for (int i = 0; i < 5; i++) {
			baskets.add(basket(id++, 1, 2));
		}
		for (int i = 0; i < 2; i++) {
			baskets.add(basket(id++, 1, 3));
		}
		for (int i = 0; i < 4; i++) {
			baskets.add(basket(id++, 4, 5));
		}
		baskets.add(basket(id, 1, 2, 3));
		return baskets;
	}

	@Test
	void frequentlyBoughtTogether_ranksFirst() {
		RecommendationModel model = trainer.train(catalog, coffeeHistory(), Instant.now());

		List<ScoredProduct> result = model.recommend(List.of(1L), 3);

		assertThat(result.get(0).productId()).isEqualTo(2L); // co-achete 6 fois
		assertThat(result.get(1).productId()).isEqualTo(3L); // co-achete 3 fois
		assertThat(result).allMatch(p -> p.reason() == RecommendationReason.SIMILAR_TO_BASKET);
		assertThat(result).extracting(ScoredProduct::productId).doesNotContain(1L);
	}

	@Test
	void independentBasketCluster_isRecommendedForItsOwnProducts() {
		RecommendationModel model = trainer.train(catalog, coffeeHistory(), Instant.now());

		assertThat(model.recommend(List.of(4L), 1).get(0).productId()).isEqualTo(5L);
	}

	@Test
	void scoresAreSortedDescending_andLimitIsRespected() {
		RecommendationModel model = trainer.train(catalog, coffeeHistory(), Instant.now());

		List<ScoredProduct> result = model.recommend(List.of(1L), 2);

		assertThat(result).hasSize(2);
		assertThat(result.get(0).score()).isGreaterThanOrEqualTo(result.get(1).score());
		assertThat(model.recommend(List.of(1L), 0)).isEmpty();
	}

	@Test
	void multiProductBasket_averagesSimilarities_andExcludesBasketItems() {
		RecommendationModel model = trainer.train(catalog, coffeeHistory(), Instant.now());

		List<ScoredProduct> result = model.recommend(List.of(1L, 3L), 5);

		assertThat(result).extracting(ScoredProduct::productId).doesNotContain(1L, 3L);
		assertThat(result.get(0).productId()).isEqualTo(2L);
	}

	@Test
	void noSeed_returnsBestSellersNormalizedToOne() {
		RecommendationModel model = trainer.train(catalog, coffeeHistory(), Instant.now());

		List<ScoredProduct> result = model.recommend(List.of(), 2);

		assertThat(result).allMatch(p -> p.reason() == RecommendationReason.BEST_SELLER);
		assertThat(result.get(0).productId()).isEqualTo(1L); // vendu dans 8 paniers
		assertThat(result.get(0).score()).isCloseTo(1.0, within(1e-9));
	}

	@Test
	void coldStart_noSales_fallsBackToContentSimilarity() {
		RecommendationModel model = trainer.train(catalog, List.of(), Instant.now());

		List<ScoredProduct> result = model.recommend(List.of(1L), 1);

		assertThat(result.get(0).productId()).isEqualTo(2L); // partage le terme "coffee"
		assertThat(model.info().basketCount()).isZero();
		assertThat(model.info().tuned()).isFalse();
	}

	@Test
	void coldStart_noSalesAndNoSeed_bestSellersHaveZeroScore() {
		RecommendationModel model = trainer.train(catalog, List.of(), Instant.now());

		assertThat(model.recommend(List.of(), 2)).allMatch(p -> p.score() == 0.0);
	}

	@Test
	void unknownProductsInHistory_areIgnored() {
		Map<Long, Integer> quantities = new LinkedHashMap<>();
		quantities.put(1L, 1);
		quantities.put(999L, 2);

		RecommendationModel model = trainer.train(catalog, List.of(new Basket(1, quantities)), Instant.now());

		assertThat(model.knows(999L)).isFalse();
		assertThat(model.info().basketCount()).isEqualTo(1);
	}

	@Test
	void weightIsTuned_whenEnoughValidationBaskets() {
		List<CatalogItem> big = new ArrayList<>();
		String[] names = { "Alpha red", "Bravo blue", "Charlie green", "Delta black", "Echo white", "Foxtrot gray",
				"Golf pink", "Hotel brown", "India cyan", "Juliet teal" };
		for (int i = 0; i < names.length; i++) {
			big.add(item(i + 1, names[i], "item " + names[i], String.valueOf(10 + i)));
		}
		long[][] pairs = { { 1, 2 }, { 3, 4 }, { 5, 6 }, { 7, 8 } };
		List<Basket> baskets = new ArrayList<>();
		for (int i = 0; i < 40; i++) {
			baskets.add(basket(i + 1, pairs[i % 4]));
		}

		RecommendationModel model = trainer.train(big, baskets, Instant.now());

		assertThat(model.info().tuned()).isTrue();
		assertThat(model.info().validationHitRate()).isEqualTo(1.0);
		assertThat(model.info().collaborativeWeight()).isBetween(0.0, 1.0);
		assertThat(model.recommend(List.of(1L), 1).get(0).productId()).isEqualTo(2L);
	}

	@Test
	void priceProximity_isBoundedAndSymmetric() {
		assertThat(HybridModelTrainer.priceProximity(new BigDecimal("10"), new BigDecimal("10"))).isEqualTo(1.0);
		assertThat(HybridModelTrainer.priceProximity(new BigDecimal("5"), new BigDecimal("10")))
				.isCloseTo(0.5, within(1e-9));
		assertThat(HybridModelTrainer.priceProximity(null, new BigDecimal("10"))).isZero();
		assertThat(HybridModelTrainer.priceProximity(new BigDecimal("3"), new BigDecimal("9")))
				.isEqualTo(HybridModelTrainer.priceProximity(new BigDecimal("9"), new BigDecimal("3")));
	}
}
