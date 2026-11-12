package com.shopwise.app.recommendation.engine;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.shopwise.app.recommendation.domain.ModelInfo;
import com.shopwise.app.recommendation.domain.RecommendationModel;
import com.shopwise.app.recommendation.domain.RecommendationReason;
import com.shopwise.app.recommendation.domain.ScoredProduct;

public final class HybridModel implements RecommendationModel {
	static final String ALGORITHM = "hybrid-item-knn (cosine co-purchase + TF-IDF content)";

	private final long[] ids;
	private final Map<Long, Integer> indexById;
	private final double[][] collaborative;
	private final double[][] content;
	private final int[] purchaseCount;
	private final double[] popularity;
	private final double collaborativeWeight;
	private final int basketCount;
	private final Instant trainedAt;
	private final boolean tuned;
	private final Double validationHitRate;

	HybridModel(long[] ids, double[][] collaborative, double[][] content, int[] purchaseCount, double[] popularity,
			double collaborativeWeight, int basketCount, Instant trainedAt, boolean tuned, Double validationHitRate) {
		this.ids = ids;
		this.collaborative = collaborative;
		this.content = content;
		this.purchaseCount = purchaseCount;
		this.popularity = popularity;
		this.collaborativeWeight = collaborativeWeight;
		this.basketCount = basketCount;
		this.trainedAt = trainedAt;
		this.tuned = tuned;
		this.validationHitRate = validationHitRate;
		Map<Long, Integer> index = new HashMap<>();
		for (int i = 0; i < ids.length; i++) {
			index.put(ids[i], i);
		}
		this.indexById = Map.copyOf(index);
	}

	HybridModel withWeight(double weight, boolean tuned, Double hitRate, int basketCount, Instant trainedAt) {
		return new HybridModel(ids, collaborative, content, purchaseCount, popularity, weight, basketCount, trainedAt,
				tuned, hitRate);
	}

	@Override
	public boolean knows(long productId) {
		return indexById.containsKey(productId);
	}

	double similarity(int a, int b) {
		double w = (purchaseCount[a] > 0 && purchaseCount[b] > 0) ? collaborativeWeight : 0.0;
		return w * collaborative[a][b] + (1.0 - w) * content[a][b];
	}

	@Override
	public List<ScoredProduct> recommend(Collection<Long> seedProductIds, int limit) {
		Set<Integer> seeds = new HashSet<>();
		for (Long id : seedProductIds) {
			Integer idx = indexById.get(id);
			if (idx != null) {
				seeds.add(idx);
			}
		}
		if (limit <= 0) {
			return List.of();
		}
		if (seeds.isEmpty()) {
			return bestSellers(limit);
		}
		List<ScoredProduct> scored = new ArrayList<>();
		for (int j = 0; j < ids.length; j++) {
			if (seeds.contains(j)) {
				continue;
			}
			double sum = 0;
			for (int s : seeds) {
				sum += similarity(s, j);
			}
			scored.add(new ScoredProduct(ids[j], sum / seeds.size(), RecommendationReason.SIMILAR_TO_BASKET));
		}
		final double[] pop = popularity;
		final Map<Long, Integer> index = indexById;
		scored.sort(Comparator.comparingDouble(ScoredProduct::score).reversed()
				.thenComparing(Comparator.comparingDouble((ScoredProduct p) -> pop[index.get(p.productId())]).reversed())
				.thenComparingLong(ScoredProduct::productId));
		return scored.size() > limit ? List.copyOf(scored.subList(0, limit)) : List.copyOf(scored);
	}

	private List<ScoredProduct> bestSellers(int limit) {
		double max = 0;
		for (double p : popularity) {
			max = Math.max(max, p);
		}
		final double maxPopularity = max;
		List<ScoredProduct> all = new ArrayList<>();
		for (int i = 0; i < ids.length; i++) {
			all.add(new ScoredProduct(ids[i], maxPopularity > 0 ? popularity[i] / maxPopularity : 0.0,
					RecommendationReason.BEST_SELLER));
		}
		all.sort(Comparator.comparingDouble(ScoredProduct::score).reversed()
				.thenComparingLong(ScoredProduct::productId));
		return all.size() > limit ? List.copyOf(all.subList(0, limit)) : List.copyOf(all);
	}

	@Override
	public ModelInfo info() {
		return new ModelInfo(ALGORITHM, trainedAt, ids.length, basketCount, collaborativeWeight, tuned,
				validationHitRate);
	}

	long[] ids() {
		return ids;
	}
}
