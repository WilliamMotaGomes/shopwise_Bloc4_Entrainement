package com.shopwise.app.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.shopwise.app.entity.Product;
import com.shopwise.app.entity.Sale;
import com.shopwise.app.entity.SaleItem;

/** Test d'integration couche persistance (H2) : mapping JPA, tri, requetes personnalisees. Rollback automatique. */
@SpringBootTest
@Transactional
class SaleRepositoryIT {

	@Autowired
	SaleRepository saleRepository;
	@Autowired
	SaleItemRepository saleItemRepository;
	@Autowired
	ProductRepository productRepository;

	private Product product(String name, String price) {
		Product p = new Product();
		p.setName(name);
		p.setPrice(new BigDecimal(price));
		return productRepository.save(p);
	}

	private Sale sale(LocalDateTime date, Product product, int quantity) {
		Sale sale = new Sale();
		sale.setSaleDate(date);
		SaleItem item = new SaleItem();
		item.setProduct(product);
		item.setQuantity(quantity);
		item.setUnitPrice(product.getPrice());
		item.setLineTotal(product.getPrice().multiply(BigDecimal.valueOf(quantity)));
		sale.addItem(item);
		sale.setTotalAmount(item.getLineTotal());
		return saleRepository.saveAndFlush(sale);
	}

	@Test
	void findAll_isOrderedByDateDescendingThenIdDescending() {
		Product p = product("Repo mug", "2.00");
		LocalDateTime day = LocalDateTime.of(2030, 1, 1, 10, 0);
		Sale oldest = sale(day.minusDays(1), p, 1);
		Sale sameTimeFirst = sale(day, p, 1);
		Sale sameTimeSecond = sale(day, p, 1);

		List<Sale> result = saleRepository.findAllByOrderBySaleDateDescIdDesc();

		assertThat(result.stream().map(Sale::getId).toList()).containsSubsequence(sameTimeSecond.getId(),
				sameTimeFirst.getId(), oldest.getId());
	}

	@Test
	void findDetailById_loadsItemsAndProducts() {
		Product p = product("Detail mug", "4.00");
		Sale saved = sale(LocalDateTime.now(), p, 3);

		Sale found = saleRepository.findDetailById(saved.getId()).orElseThrow();

		assertThat(found.getItems()).hasSize(1);
		assertThat(found.getItems().get(0).getProduct().getName()).isEqualTo("Detail mug");
		assertThat(found.getTotalAmount()).isEqualByComparingTo("12.00");
	}

	@Test
	void existsByProductId_andPurchaseTuples() {
		Product sold = product("Tuple mug", "1.00");
		Product unsold = product("Unsold mug", "1.00");
		Sale s = sale(LocalDateTime.now(), sold, 2);

		assertThat(saleItemRepository.existsByProductId(sold.getId())).isTrue();
		assertThat(saleItemRepository.existsByProductId(unsold.getId())).isFalse();
		assertThat(saleItemRepository.findPurchaseTuples()).anySatisfy(row -> {
			assertThat(((Number) row[0]).longValue()).isEqualTo(s.getId());
			assertThat(((Number) row[1]).longValue()).isEqualTo(sold.getId());
			assertThat(((Number) row[2]).intValue()).isEqualTo(2);
		});
	}

	@Test
	void deletingSale_cascadesToItems() {
		Product p = product("Cascade mug", "1.00");
		Sale s = sale(LocalDateTime.now(), p, 1);

		saleRepository.delete(s);
		saleRepository.flush();

		assertThat(saleItemRepository.existsByProductId(p.getId())).isFalse();
	}

	@Test
	void findMaxId_isZeroOrPositive() {
		assertThat(saleRepository.findMaxId()).isGreaterThanOrEqualTo(0L);
	}
}
