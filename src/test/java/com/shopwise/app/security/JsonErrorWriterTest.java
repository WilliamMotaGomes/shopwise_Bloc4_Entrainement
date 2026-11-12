package com.shopwise.app.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class JsonErrorWriterTest {

	@Test
	void escape_handlesQuotesBackslashesAndControlCharacters() {
		assertThat(JsonErrorWriter.escape("a\"b\\c\nd")).isEqualTo("a\\\"b\\\\c\\nd");
	}

	@Test
	void escape_nullIsEmpty() {
		assertThat(JsonErrorWriter.escape(null)).isEmpty();
	}
}
