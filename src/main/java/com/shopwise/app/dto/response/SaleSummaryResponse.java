package com.shopwise.app.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SaleSummaryResponse(Long id, LocalDateTime saleDate, BigDecimal totalAmount, int itemCount) {
}
