package com.shopwise.app.dto.response;

import java.time.Instant;
import java.util.List;

public record ErrorResponse(String timestamp, int code, String error, String message, String path,
		List<FieldViolation> details) {
	public record FieldViolation(String field, String message) {
	}

	public static ErrorResponse of(int code, String error, String message, String path) {
		return new ErrorResponse(Instant.now().toString(), code, error, message, path, List.of());
	}

	public static ErrorResponse of(int code, String error, String message, String path,
			List<FieldViolation> details) {
		return new ErrorResponse(Instant.now().toString(), code, error, message, path, details);
	}
}
