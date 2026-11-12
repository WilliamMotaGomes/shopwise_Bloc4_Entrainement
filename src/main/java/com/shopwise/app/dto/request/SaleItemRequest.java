package com.shopwise.app.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SaleItemRequest(@NotNull(message = "productId is required") Long productId,
		@NotNull(message = "quantity is required") @Min(value = 1, message = "quantity must be at least 1") @Max(value = 10000, message = "quantity must be at most 10000") Integer quantity) {
}
