package com.shopwise.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.shopwise.app.dto.request.CreateSaleRequest;
import com.shopwise.app.dto.request.SaleItemRequest;
import com.shopwise.app.dto.response.SaleResponse;
import com.shopwise.app.dto.response.SaleSummaryResponse;
import com.shopwise.app.entity.Product;
import com.shopwise.app.entity.Sale;
import com.shopwise.app.entity.SaleItem;
import com.shopwise.app.exception.BadRequestException;
import com.shopwise.app.exception.NotFoundException;
import com.shopwise.app.mapper.SaleMapper;
import com.shopwise.app.repository.ProductRepository;
import com.shopwise.app.repository.SaleRepository;

@ExtendWith(MockitoExtension.class)
class SaleServiceImplTest {

	private static final Instant NOW = Instant.parse("2026-10-22T10:00:00Z");

	@Mock
	SaleRepository saleRepository;
	@Mock
	ProductRepository productRepository;

	SaleServiceImpl service;

	@BeforeEach
	void setUp() {
		service = new SaleServiceImpl(saleRepository, productRepository, new SaleMapper(),
				Clock.fixed(NOW, ZoneOffset.UTC));
	}

	private static Product product(long id, String name, String price) {
		Product p = new Product();
		p.setId(id);
		p.setName(name);
		p.setPrice(new BigDecimal(price));
		return p;
	}

	@Test
	void create_computesLineTotalsAndGrandTotal() {
		Product a = product(1, "Mug", "5.50");
		Product b = product(2, "Tea", "3.20");
		when(productRepository.findAllById(any())).thenReturn(List.of(a, b));
		when(saleRepository.save(any(Sale.class))).thenAnswer(inv -> inv.getArgument(0));

		SaleResponse response = service
				.create(new CreateSaleRequest(List.of(new SaleItemRequest(1L, 2), new SaleItemRequest(2L, 3))));

		assertThat(response.items()).hasSize(2);
		assertThat(response.items().get(0).lineTotal()).isEqualByComparingTo("11.00");
		assertThat(response.items().get(1).lineTotal()).isEqualByComparingTo("9.60");
		assertThat(response.totalAmount()).isEqualByComparingTo("20.60");
		assertThat(response.saleDate()).isEqualTo(LocalDateTime.of(2026, 10, 22, 10, 0));
	}

	@Test
	void create_mergesDuplicateProductLines() {
		Product a = product(1, "Mug", "5.00");
		when(productRepository.findAllById(any())).thenReturn(List.of(a));
		when(saleRepository.save(any(Sale.class))).thenAnswer(inv -> inv.getArgument(0));

		SaleResponse response = service
				.create(new CreateSaleRequest(List.of(new SaleItemRequest(1L, 2), new SaleItemRequest(1L, 3))));

		assertThat(response.items()).hasSize(1);
		assertThat(response.items().get(0).quantity()).isEqualTo(5);
		assertThat(response.totalAmount()).isEqualByComparingTo("25.00");
	}

	@Test
	void create_unknownProduct_throwsBadRequestAndDoesNotSave() {
		when(productRepository.findAllById(any())).thenReturn(List.of(product(1, "Mug", "5.00")));

		assertThatThrownBy(() -> service
				.create(new CreateSaleRequest(List.of(new SaleItemRequest(1L, 1), new SaleItemRequest(42L, 1)))))
				.isInstanceOf(BadRequestException.class).hasMessageContaining("42");
		verify(saleRepository, never()).save(any());
	}

	@Test
	void getById_unknown_throwsNotFoundWithClearMessage() {
		when(saleRepository.findDetailById(9L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.getById(9L)).isInstanceOf(NotFoundException.class)
				.hasMessage("Sale not found with id 9");
	}

	@Test
	void getById_returnsLinesWithUnitPriceQuantityAndTotal() {
		Sale sale = new Sale();
		sale.setId(1L);
		sale.setSaleDate(LocalDateTime.of(2026, 1, 1, 8, 0));
		sale.setTotalAmount(new BigDecimal("10.00"));
		SaleItem line = new SaleItem();
		line.setProduct(product(1, "Mug", "5.00"));
		line.setQuantity(2);
		line.setUnitPrice(new BigDecimal("5.00"));
		line.setLineTotal(new BigDecimal("10.00"));
		sale.addItem(line);
		when(saleRepository.findDetailById(1L)).thenReturn(Optional.of(sale));

		SaleResponse response = service.getById(1L);

		assertThat(response.items()).singleElement().satisfies(i -> {
			assertThat(i.productName()).isEqualTo("Mug");
			assertThat(i.quantity()).isEqualTo(2);
			assertThat(i.unitPrice()).isEqualByComparingTo("5.00");
			assertThat(i.lineTotal()).isEqualByComparingTo("10.00");
		});
	}

	@Test
	void getAll_returnsSummariesInRepositoryOrder() {
		Sale recent = new Sale();
		recent.setId(2L);
		recent.setSaleDate(LocalDateTime.of(2026, 2, 1, 9, 0));
		recent.setTotalAmount(new BigDecimal("4.00"));
		Sale old = new Sale();
		old.setId(1L);
		old.setSaleDate(LocalDateTime.of(2026, 1, 1, 9, 0));
		old.setTotalAmount(new BigDecimal("8.00"));
		when(saleRepository.findAllByOrderBySaleDateDescIdDesc()).thenReturn(List.of(recent, old));

		List<SaleSummaryResponse> result = service.getAll();

		assertThat(result).extracting(SaleSummaryResponse::id).containsExactly(2L, 1L);
	}
}
