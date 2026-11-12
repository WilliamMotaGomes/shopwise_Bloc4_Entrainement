package com.shopwise.app.recommendation.adapter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.shopwise.app.recommendation.domain.Basket;
import com.shopwise.app.recommendation.domain.PurchaseHistoryProvider;
import com.shopwise.app.repository.SaleItemRepository;
import com.shopwise.app.repository.SaleRepository;

@Component
public class JpaPurchaseHistoryProvider implements PurchaseHistoryProvider {
	private final SaleItemRepository saleItemRepository;
	private final SaleRepository saleRepository;

	public JpaPurchaseHistoryProvider(SaleItemRepository saleItemRepository, SaleRepository saleRepository) {
		this.saleItemRepository = saleItemRepository;
		this.saleRepository = saleRepository;
	}

	@Override
	public List<Basket> loadBaskets() {
		Map<Long, Map<Long, Integer>> bySale = new LinkedHashMap<>();
		for (Object[] row : saleItemRepository.findPurchaseTuples()) {
			long saleId = ((Number) row[0]).longValue();
			long productId = ((Number) row[1]).longValue();
			int quantity = ((Number) row[2]).intValue();
			bySale.computeIfAbsent(saleId, k -> new LinkedHashMap<>()).merge(productId, quantity, Integer::sum);
		}
		List<Basket> baskets = new ArrayList<>();
		bySale.forEach((saleId, quantities) -> baskets.add(new Basket(saleId, quantities)));
		return baskets;
	}

	@Override
	public String version() {
		return saleRepository.count() + ":" + saleRepository.findMaxId();
	}
}
