package com.shopwise.app.recommendation.engine;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

final class TextVectorizer {
	private static final Set<String> STOP_WORDS = Set.of("le", "la", "les", "un", "une", "des", "du", "de", "et", "ou",
			"pour", "avec", "sans", "sur", "dans", "the", "a", "an", "and", "or", "for", "with", "of", "to", "in", "on",
			"latest", "new");

	private TextVectorizer() {
	}

	static List<String> tokenize(String text) {
		if (text == null || text.isBlank()) {
			return List.of();
		}
		String normalized = Normalizer.normalize(text.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
				.replaceAll("\\p{M}+", "");
		List<String> tokens = new ArrayList<>();
		for (String token : normalized.split("[^a-z0-9]+")) {
			if (token.length() >= 2 && !STOP_WORDS.contains(token)) {
				tokens.add(token);
			}
		}
		return tokens;
	}

	static List<Map<String, Double>> tfIdf(List<List<String>> documents) {
		int n = documents.size();
		Map<String, Integer> documentFrequency = new HashMap<>();
		for (List<String> doc : documents) {
			for (String term : Set.copyOf(doc)) {
				documentFrequency.merge(term, 1, Integer::sum);
			}
		}
		List<Map<String, Double>> vectors = new ArrayList<>(n);
		for (List<String> doc : documents) {
			Map<String, Double> vector = new HashMap<>();
			Map<String, Integer> termFrequency = new HashMap<>();
			for (String term : doc) {
				termFrequency.merge(term, 1, Integer::sum);
			}
			double norm = 0;
			for (Map.Entry<String, Integer> e : termFrequency.entrySet()) {
				double idf = Math.log((1.0 + n) / (1.0 + documentFrequency.get(e.getKey()))) + 1.0;
				double weight = e.getValue() * idf;
				vector.put(e.getKey(), weight);
				norm += weight * weight;
			}
			norm = Math.sqrt(norm);
			if (norm > 0) {
				final double divisor = norm;
				vector.replaceAll((k, v) -> v / divisor);
			}
			vectors.add(vector);
		}
		return vectors;
	}

	static double dot(Map<String, Double> a, Map<String, Double> b) {
		Map<String, Double> small = a.size() <= b.size() ? a : b;
		Map<String, Double> large = small == a ? b : a;
		double sum = 0;
		for (Map.Entry<String, Double> e : small.entrySet()) {
			Double other = large.get(e.getKey());
			if (other != null) {
				sum += e.getValue() * other;
			}
		}
		return sum;
	}
}
