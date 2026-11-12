package com.shopwise.app.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.shopwise.app.dto.response.SaleItemResponse;
import com.shopwise.app.dto.response.SaleResponse;
import com.shopwise.app.dto.response.SaleSummaryResponse;
import com.shopwise.app.entity.Sale;
import com.shopwise.app.entity.SaleItem;

@Component
public class SaleMapper {
	public SaleItemResponse toItemResponse(SaleItem item) {
		return new SaleItemResponse(item.getProduct().getId(), item.getProduct().getName(), item.getQuantity(),
				item.getUnitPrice(), item.getLineTotal());
	}

	public SaleResponse toResponse(Sale sale) {
		List<SaleItemResponse> items = sale.getItems().stream().map(this::toItemResponse).toList();
		return new SaleResponse(sale.getId(), sale.getSaleDate(), sale.getTotalAmount(), items);
	}

	public SaleSummaryResponse toSummary(Sale sale) {
		return new SaleSummaryResponse(sale.getId(), sale.getSaleDate(), sale.getTotalAmount(),
				sale.getItems().size());
	}
}
