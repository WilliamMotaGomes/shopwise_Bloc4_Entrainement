package com.shopwise.app.dto.response;

import java.math.BigDecimal;

public record SaleItemResponse(Long productId, String productName, int quantity, BigDecimal unitPrice,
		BigDecimal lineTotal) {
}
