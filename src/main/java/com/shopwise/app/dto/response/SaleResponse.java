package com.shopwise.app.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record SaleResponse(Long id, LocalDateTime saleDate, BigDecimal totalAmount, List<SaleItemResponse> items) {
}
