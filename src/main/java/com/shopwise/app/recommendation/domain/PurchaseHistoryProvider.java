package com.shopwise.app.recommendation.domain;

import java.util.List;

public interface PurchaseHistoryProvider {
	List<Basket> loadBaskets();

	String version();
}
