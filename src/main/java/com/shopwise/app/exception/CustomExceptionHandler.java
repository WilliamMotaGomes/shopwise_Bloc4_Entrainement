package com.shopwise.app.exception;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.shopwise.app.dto.response.ErrorResponse;
import com.shopwise.app.dto.response.ErrorResponse.FieldViolation;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class CustomExceptionHandler {
	private static final Logger log = LoggerFactory.getLogger(CustomExceptionHandler.class);

	@ExceptionHandler(NotFoundException.class)
	public ResponseEntity<ErrorResponse> handleNotFound(NotFoundException ex, HttpServletRequest req) {
		return build(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage(), req);
	}

	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<ErrorResponse> handleNoResource(NoResourceFoundException ex, HttpServletRequest req) {
		return build(HttpStatus.NOT_FOUND, "NOT_FOUND", "No endpoint " + req.getMethod() + " " + req.getRequestURI(),
				req);
	}

	@ExceptionHandler(BadRequestException.class)
	public ResponseEntity<ErrorResponse> handleBadRequest(BadRequestException ex, HttpServletRequest req) {
		return build(HttpStatus.BAD_REQUEST, "BAD_REQUEST", ex.getMessage(), req);
	}

	@ExceptionHandler(ConflictException.class)
	public ResponseEntity<ErrorResponse> handleConflict(ConflictException ex, HttpServletRequest req) {
		return build(HttpStatus.CONFLICT, "CONFLICT", ex.getMessage(), req);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex,
			HttpServletRequest req) {
		List<FieldViolation> details = ex.getBindingResult().getAllErrors().stream()
				.map(e -> new FieldViolation(e instanceof FieldError fe ? fe.getField() : e.getObjectName(),
						e.getDefaultMessage()))
				.toList();
		return ResponseEntity.badRequest().body(
				ErrorResponse.of(400, "VALIDATION_ERROR", "Request validation failed", req.getRequestURI(), details));
	}

	@ExceptionHandler(HandlerMethodValidationException.class)
	public ResponseEntity<ErrorResponse> handleMethodValidation(HandlerMethodValidationException ex,
			HttpServletRequest req) {
		return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Invalid request parameter", req);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException ex,
			HttpServletRequest req) {
		return build(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", "Request body is missing or malformed JSON", req);
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
			HttpServletRequest req) {
		return build(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "Invalid value for parameter '" + ex.getName() + "'",
				req);
	}

	@ExceptionHandler(MissingServletRequestParameterException.class)
	public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException ex,
			HttpServletRequest req) {
		return build(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "Missing parameter '" + ex.getParameterName() + "'", req);
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex,
			HttpServletRequest req) {
		return build(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED",
				"Method " + ex.getMethod() + " is not supported on this endpoint", req);
	}

	@ExceptionHandler(HttpMediaTypeNotSupportedException.class)
	public ResponseEntity<ErrorResponse> handleMediaType(HttpMediaTypeNotSupportedException ex,
			HttpServletRequest req) {
		return build(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_MEDIA_TYPE",
				"Content type is not supported, use application/json", req);
	}

	@ExceptionHandler(AuthenticationException.class)
	public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException ex, HttpServletRequest req) {
		return build(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Invalid credentials or missing authentication", req);
	}

	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest req) {
		return build(HttpStatus.FORBIDDEN, "FORBIDDEN", "You do not have the required role to perform this action",
				req);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleAll(Exception ex, HttpServletRequest req) {
		log.error("Unexpected error", ex);
		return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "An unexpected error occurred", req);
	}

	private ResponseEntity<ErrorResponse> build(HttpStatus status, String error, String message,
			HttpServletRequest req) {
		return ResponseEntity.status(status)
				.body(ErrorResponse.of(status.value(), error, message, req.getRequestURI()));
	}
}
