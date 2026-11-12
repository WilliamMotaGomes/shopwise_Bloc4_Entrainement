package com.shopwise.app.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

@Service
public class SaleServiceImpl implements SaleService {
	private final SaleRepository saleRepository;
	private final ProductRepository productRepository;
	private final SaleMapper saleMapper;
	private final Clock clock;

	public SaleServiceImpl(SaleRepository saleRepository, ProductRepository productRepository, SaleMapper saleMapper,
			Clock clock) {
		this.saleRepository = saleRepository;
		this.productRepository = productRepository;
		this.saleMapper = saleMapper;
		this.clock = clock;
	}

	@Override
	@Transactional
	public SaleResponse create(CreateSaleRequest request) {
		Map<Long, Integer> quantityByProduct = new LinkedHashMap<>();
		for (SaleItemRequest item : request.items()) {
			quantityByProduct.merge(item.productId(), item.quantity(), Integer::sum);
		}

		Map<Long, Product> products = productRepository.findAllById(quantityByProduct.keySet()).stream()
				.collect(Collectors.toMap(Product::getId, Function.identity()));
		List<Long> unknown = quantityByProduct.keySet().stream().filter(id -> !products.containsKey(id)).toList();
		if (!unknown.isEmpty()) {
			throw new BadRequestException("Unknown product id(s): " + unknown);
		}

		Sale sale = new Sale();
		sale.setSaleDate(LocalDateTime.now(clock));
		BigDecimal total = BigDecimal.ZERO;
		for (Map.Entry<Long, Integer> entry : quantityByProduct.entrySet()) {
			Product product = products.get(entry.getKey());
			BigDecimal unitPrice = product.getPrice().setScale(2, RoundingMode.HALF_UP);
			BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(entry.getValue()));

			SaleItem line = new SaleItem();
			line.setProduct(product);
			line.setQuantity(entry.getValue());
			line.setUnitPrice(unitPrice);
			line.setLineTotal(lineTotal);
			sale.addItem(line);
			total = total.add(lineTotal);
		}
		sale.setTotalAmount(total);

		return saleMapper.toResponse(saleRepository.save(sale));
	}

	@Override
	@Transactional(readOnly = true)
	public List<SaleSummaryResponse> getAll() {
		return saleRepository.findAllByOrderBySaleDateDescIdDesc().stream().map(saleMapper::toSummary).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public SaleResponse getById(Long id) {
		Sale sale = saleRepository.findDetailById(id)
				.orElseThrow(() -> new NotFoundException("Sale not found with id " + id));
		return saleMapper.toResponse(sale);
	}
}
