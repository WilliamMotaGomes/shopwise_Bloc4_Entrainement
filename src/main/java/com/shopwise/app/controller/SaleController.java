package com.shopwise.app.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shopwise.app.dto.request.CreateSaleRequest;
import com.shopwise.app.dto.response.SaleResponse;
import com.shopwise.app.dto.response.SaleSummaryResponse;
import com.shopwise.app.service.SaleService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/sales")
public class SaleController {
	private final SaleService saleService;

	public SaleController(SaleService saleService) {
		this.saleService = saleService;
	}

	@PostMapping
	public ResponseEntity<SaleResponse> create(@Valid @RequestBody CreateSaleRequest request) {
		SaleResponse created = saleService.create(request);
		return ResponseEntity.created(URI.create("/api/sales/" + created.id())).body(created);
	}

	@GetMapping
	public ResponseEntity<List<SaleSummaryResponse>> getAll() {
		return ResponseEntity.ok(saleService.getAll());
	}

	@GetMapping("/{id}")
	public ResponseEntity<SaleResponse> getById(@PathVariable Long id) {
		return ResponseEntity.ok(saleService.getById(id));
	}
}
