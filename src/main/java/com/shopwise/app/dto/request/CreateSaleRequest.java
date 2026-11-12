package com.shopwise.app.dto.request;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record CreateSaleRequest(
		@NotEmpty(message = "a sale must contain at least one item") List<@NotNull(message = "item must not be null") @Valid SaleItemRequest> items) {
}
