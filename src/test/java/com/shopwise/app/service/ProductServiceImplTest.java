package com.shopwise.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.shopwise.app.dto.request.CreateProductRequest;
import com.shopwise.app.dto.request.UpdateProductRequest;
import com.shopwise.app.dto.response.ProductResponse;
import com.shopwise.app.entity.Product;
import com.shopwise.app.exception.ConflictException;
import com.shopwise.app.exception.NotFoundException;
import com.shopwise.app.mapper.ProductMapper;
import com.shopwise.app.repository.ProductRepository;
import com.shopwise.app.repository.SaleItemRepository;

/** Tests unitaires du catalogue (non-regression de l'existant + nouvelle regle de suppression). */
@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

	@Mock
	ProductRepository productRepository;
	@Mock
	ProductMapper productMapper;
	@Mock
	SaleItemRepository saleItemRepository;
	@InjectMocks
	ProductServiceImpl service;

	@Test
	void getById_returnsMappedProduct() {
		Product product = new Product();
		ProductResponse response = new ProductResponse();
		response.setId(1L);
		when(productRepository.findById(1L)).thenReturn(Optional.of(product));
		when(productMapper.toResponse(product)).thenReturn(response);

		assertThat(service.getById(1L)).isSameAs(response);
	}

	@Test
	void getById_unknown_throwsNotFound() {
		when(productRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.getById(99L)).isInstanceOf(NotFoundException.class)
				.hasMessage("Product not found");
	}

	@Test
	void create_savesAndMaps() {
		CreateProductRequest request = new CreateProductRequest();
		request.setName("Mug");
		request.setPrice(new BigDecimal("5.00"));
		Product entity = new Product();
		ProductResponse response = new ProductResponse();
		when(productMapper.toEntity(request)).thenReturn(entity);
		when(productRepository.save(entity)).thenReturn(entity);
		when(productMapper.toResponse(entity)).thenReturn(response);

		assertThat(service.create(request)).isSameAs(response);
		verify(productRepository).save(entity);
	}

	@Test
	void getAll_mapsList() {
		List<Product> products = List.of(new Product());
		List<ProductResponse> responses = List.of(new ProductResponse());
		when(productRepository.findAll()).thenReturn(products);
		when(productMapper.toResponseList(products)).thenReturn(responses);

		assertThat(service.getAll()).isEqualTo(responses);
	}

	@Test
	void update_unknown_throwsNotFound() {
		when(productRepository.findById(5L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.update(5L, new UpdateProductRequest())).isInstanceOf(NotFoundException.class);
		verify(productRepository, never()).save(any());
	}

	@Test
	void delete_unknown_throwsNotFound() {
		when(productRepository.existsById(7L)).thenReturn(false);

		assertThatThrownBy(() -> service.delete(7L)).isInstanceOf(NotFoundException.class);
		verify(productRepository, never()).deleteById(any());
	}

	@Test
	void delete_productAlreadySold_throwsConflict() {
		when(productRepository.existsById(3L)).thenReturn(true);
		when(saleItemRepository.existsByProductId(3L)).thenReturn(true);

		assertThatThrownBy(() -> service.delete(3L)).isInstanceOf(ConflictException.class);
		verify(productRepository, never()).deleteById(any());
	}

	@Test
	void delete_neverSold_deletes() {
		when(productRepository.existsById(3L)).thenReturn(true);
		when(saleItemRepository.existsByProductId(3L)).thenReturn(false);

		service.delete(3L);

		verify(productRepository).deleteById(3L);
	}
}
