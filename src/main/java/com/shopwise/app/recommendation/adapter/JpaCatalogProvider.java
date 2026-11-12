package com.shopwise.app.recommendation.adapter;

import java.util.List;

import org.springframework.stereotype.Component;

import com.shopwise.app.recommendation.domain.CatalogItem;
import com.shopwise.app.recommendation.domain.CatalogProvider;
import com.shopwise.app.repository.ProductRepository;

@Component
public class JpaCatalogProvider implements CatalogProvider {
	private final ProductRepository productRepository;

	public JpaCatalogProvider(ProductRepository productRepository) {
		this.productRepository = productRepository;
	}

	@Override
	public List<CatalogItem> loadCatalog() {
		return productRepository.findAll().stream()
				.map(p -> new CatalogItem(p.getId(), p.getName(), p.getDescription(), p.getPrice())).toList();
	}

	@Override
	public String version() {
		return productRepository.count() + ":" + productRepository.findLastUpdate();
	}
}
