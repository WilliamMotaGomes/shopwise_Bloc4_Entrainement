package com.shopwise.app.recommendation.engine;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.shopwise.app.recommendation.domain.Basket;
import com.shopwise.app.recommendation.domain.CatalogItem;
import com.shopwise.app.recommendation.domain.RecommendationModel;
import com.shopwise.app.recommendation.domain.RecommendationModelTrainer;

public class HybridModelTrainer implements RecommendationModelTrainer {
	private final HybridConfig config;

	public HybridModelTrainer(HybridConfig config) {
		this.config = config;
	}

	@Override
	public RecommendationModel train(List<CatalogItem> catalog, List<Basket> baskets, Instant now) {
		List<Basket> cleaned = keepKnownProducts(catalog, baskets);

		double weight = config.defaultCollaborativeWeight();
		boolean tuned = false;
		Double hitRate = null;
		int split = (int) Math.floor(cleaned.size() * (1.0 - config.validationRatio()));
		List<Basket> trainPart = cleaned.subList(0, split);
		List<Basket> validationPart = cleaned.subList(split, cleaned.size()).stream()
				.filter(b -> b.productIds().size() >= 2).toList();
		if (validationPart.size() >= config.minValidationBaskets() && !trainPart.isEmpty()) {
			HybridModel candidate = build(catalog, trainPart, config.defaultCollaborativeWeight(), now);
			double best = -1;
			double bestWeight = weight;
			for (int step = 0; step <= 10; step++) {
				double w = step / 10.0;
				double rate = hitRate(candidate.withWeight(w, false, null, trainPart.size(), now), validationPart);

				if (rate > best + 1e-12 || (Math.abs(rate - best) <= 1e-12
						&& Math.abs(w - config.defaultCollaborativeWeight()) < Math
								.abs(bestWeight - config.defaultCollaborativeWeight()))) {
					best = rate;
					bestWeight = w;
				}
			}
			weight = bestWeight;
			hitRate = best;
			tuned = true;
		}

		HybridModel full = build(catalog, cleaned, weight, now);
		return full.withWeight(weight, tuned, hitRate, cleaned.size(), now);
	}

	private List<Basket> keepKnownProducts(List<CatalogItem> catalog, List<Basket> baskets) {
		Set<Long> known = new HashSet<>();
		catalog.forEach(c -> known.add(c.id()));
		List<Basket> result = new ArrayList<>();
		for (Basket b : baskets) {
			Map<Long, Integer> q = new HashMap<>();
			b.quantities().forEach((id, qty) -> {
				if (known.contains(id) && qty != null && qty > 0) {
					q.put(id, qty);
				}
			});
			if (!q.isEmpty()) {
				result.add(new Basket(b.saleId(), q));
			}
		}
		return result;
	}

	double hitRate(HybridModel model, List<Basket> validation) {
		int hits = 0;
		int evaluated = 0;
		for (int b = 0; b < validation.size(); b++) {
			List<Long> items = new ArrayList<>(new TreeSet<>(validation.get(b).productIds()));
			if (items.size() < 2) {
				continue;
			}
			Long heldOut = items.get(b % items.size());
			List<Long> seeds = new ArrayList<>(items);
			seeds.remove(heldOut);
			boolean hit = model.recommend(seeds, config.topK()).stream().anyMatch(p -> p.productId() == heldOut);
			evaluated++;
			if (hit) {
				hits++;
			}
		}
		return evaluated == 0 ? 0.0 : (double) hits / evaluated;
	}

	private HybridModel build(List<CatalogItem> catalog, List<Basket> baskets, double weight, Instant now) {
		int n = catalog.size();
		long[] ids = new long[n];
		Map<Long, Integer> index = new HashMap<>();
		for (int i = 0; i < n; i++) {
			ids[i] = catalog.get(i).id();
			index.put(ids[i], i);
		}

		int[] purchaseCount = new int[n];
		double[] popularity = new double[n];
		int[][] co = new int[n][n];
		for (Basket basket : baskets) {
			List<Integer> idx = new ArrayList<>();
			for (Map.Entry<Long, Integer> e : basket.quantities().entrySet()) {
				int i = index.get(e.getKey());
				idx.add(i);
				purchaseCount[i]++;
				popularity[i] += e.getValue();
			}
			for (int a = 0; a < idx.size(); a++) {
				for (int b = a + 1; b < idx.size(); b++) {
					co[idx.get(a)][idx.get(b)]++;
					co[idx.get(b)][idx.get(a)]++;
				}
			}
		}
		double[][] collaborative = new double[n][n];
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < n; j++) {
				if (i != j && co[i][j] > 0) {
					collaborative[i][j] = co[i][j] / Math.sqrt((double) purchaseCount[i] * purchaseCount[j]);
				}
			}
		}

		List<List<String>> docs = new ArrayList<>();
		for (CatalogItem item : catalog) {
			docs.add(TextVectorizer.tokenize(item.name() + " " + (item.description() == null ? "" : item.description())));
		}
		List<Map<String, Double>> vectors = TextVectorizer.tfIdf(docs);
		double[][] content = new double[n][n];
		for (int i = 0; i < n; i++) {
			for (int j = i + 1; j < n; j++) {
				double text = TextVectorizer.dot(vectors.get(i), vectors.get(j));
				double price = priceProximity(catalog.get(i).price(), catalog.get(j).price());
				double sim = config.contentTextWeight() * text + (1.0 - config.contentTextWeight()) * price;
				content[i][j] = sim;
				content[j][i] = sim;
			}
		}
		return new HybridModel(ids, collaborative, content, purchaseCount, popularity, weight, baskets.size(), now,
				false, null);
	}

	static double priceProximity(BigDecimal p1, BigDecimal p2) {
		if (p1 == null || p2 == null) {
			return 0.0;
		}
		double a = p1.doubleValue();
		double b = p2.doubleValue();
		double max = Math.max(a, b);
		return max <= 0 ? 0.0 : 1.0 - Math.abs(a - b) / max;
	}
}
