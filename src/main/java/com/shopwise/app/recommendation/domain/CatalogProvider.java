package com.shopwise.app.recommendation.domain;

import java.util.List;

public interface CatalogProvider {
	List<CatalogItem> loadCatalog();

	String version();
}
