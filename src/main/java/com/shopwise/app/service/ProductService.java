package com.shopwise.app.service;

import java.util.List;

import com.shopwise.app.dto.request.CreateProductRequest;
import com.shopwise.app.dto.request.UpdateProductRequest;
import com.shopwise.app.dto.response.ProductResponse;

public interface ProductService {
    public ProductResponse create(CreateProductRequest request);

    public ProductResponse getById(Long id);

	public List<ProductResponse> getAll();

    public ProductResponse update(Long id, UpdateProductRequest request);

    public void delete(Long id);
}
