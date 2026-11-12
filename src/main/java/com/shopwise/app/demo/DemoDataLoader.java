package com.shopwise.app.demo;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.shopwise.app.dto.request.CreateProductRequest;
import com.shopwise.app.dto.request.CreateSaleRequest;
import com.shopwise.app.dto.request.SaleItemRequest;
import com.shopwise.app.dto.response.ProductResponse;
import com.shopwise.app.service.ProductService;
import com.shopwise.app.service.SaleService;

@Component
@Profile("demo")
public class DemoDataLoader implements CommandLineRunner {
	private final ProductService productService;
	private final SaleService saleService;

	public DemoDataLoader(ProductService productService, SaleService saleService) {
		this.productService = productService;
		this.saleService = saleService;
	}

	@Override
	public void run(String... args) {
		long airpods = create("AirPods Pro", "Apple wireless earbuds with noise cancelling", "279.00");
		long buds = create("Galaxy Buds", "Samsung wireless earbuds", "149.00");
		long caseIphone = create("iPhone 15 case", "Protective case for Apple iPhone 15", "29.00");
		long sleeve = create("Laptop sleeve 16", "Padded sleeve for 16 inch Apple laptop", "39.00");
		long mouse = create("Wireless mouse", "Bluetooth mouse for laptop", "25.00");

		List<long[]> baskets = new ArrayList<>();
		for (int i = 0; i < 6; i++) {
			baskets.add(new long[] { 1, airpods, caseIphone });
		}
		for (int i = 0; i < 4; i++) {
			baskets.add(new long[] { 2, buds });
		}
		for (int i = 0; i < 5; i++) {
			baskets.add(new long[] { 3, sleeve, mouse });
		}
		for (int i = 0; i < 3; i++) {
			baskets.add(new long[] { 4, mouse });
		}
		baskets.add(new long[] { 1, 3 });
		for (long[] basket : baskets) {
			List<SaleItemRequest> items = new ArrayList<>();
			for (long productId : basket) {
				items.add(new SaleItemRequest(productId, 1));
			}
			saleService.create(new CreateSaleRequest(items));
		}
	}

	private long create(String name, String description, String price) {
		CreateProductRequest request = new CreateProductRequest();
		request.setName(name);
		request.setDescription(description);
		request.setPrice(new BigDecimal(price));
		ProductResponse created = productService.create(request);
		return created.getId();
	}
}
