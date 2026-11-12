package com.shopwise.app.recommendation.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class TextVectorizerTest {

	@Test
	void tokenize_lowercasesRemovesAccentsStopWordsAndShortTokens() {
		assertThat(TextVectorizer.tokenize("Café Grand-Crème, pour LE matin & x"))
				.containsExactly("cafe", "grand", "creme", "matin");
	}

	@Test
	void tokenize_blankOrNull_isEmpty() {
		assertThat(TextVectorizer.tokenize(null)).isEmpty();
		assertThat(TextVectorizer.tokenize("   ")).isEmpty();
	}

	@Test
	void tfIdf_vectorsAreUnitLength_andRareTermsWeighMore() {
		List<Map<String, Double>> vectors = TextVectorizer.tfIdf(
				List.of(List.of("coffee", "beans"), List.of("coffee", "filter"), List.of("coffee", "mug")));

		for (Map<String, Double> v : vectors) {
			double norm = v.values().stream().mapToDouble(x -> x * x).sum();
			assertThat(norm).isCloseTo(1.0, within(1e-9));
		}
		// "coffee" est dans tous les documents (idf minimal) ; "beans" est rare => plus discriminant
		assertThat(vectors.get(0).get("beans")).isGreaterThan(vectors.get(0).get("coffee"));
	}

	@Test
	void dot_ofIdenticalNormalizedVectors_isOne_andDisjointIsZero() {
		List<Map<String, Double>> v = TextVectorizer.tfIdf(List.of(List.of("a1", "b2"), List.of("a1", "b2"), List.of("zz")));

		assertThat(TextVectorizer.dot(v.get(0), v.get(1))).isCloseTo(1.0, within(1e-9));
		assertThat(TextVectorizer.dot(v.get(0), v.get(2))).isZero();
	}

	@Test
	void emptyDocument_givesEmptyVector() {
		assertThat(TextVectorizer.tfIdf(List.of(List.<String>of())).get(0)).isEmpty();
	}
}
