package com.shopwise.app.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.shopwise.app.dto.request.CreateProductRequest;
import com.shopwise.app.dto.request.UpdateProductRequest;
import com.shopwise.app.dto.response.ProductResponse;
import com.shopwise.app.entity.Product;
import com.shopwise.app.exception.ConflictException;
import com.shopwise.app.exception.NotFoundException;
import com.shopwise.app.mapper.ProductMapper;
import com.shopwise.app.repository.ProductRepository;
import com.shopwise.app.repository.SaleItemRepository;

import jakarta.transaction.Transactional;

@Service
@Transactional
public class ProductServiceImpl implements ProductService {
	private final ProductRepository productRepository;
	private final ProductMapper productMapper;
	private final SaleItemRepository saleItemRepository;

	public ProductServiceImpl(ProductRepository productRepository, ProductMapper productMapper,
			SaleItemRepository saleItemRepository) {
		super();
		this.productRepository = productRepository;
		this.productMapper = productMapper;
		this.saleItemRepository = saleItemRepository;
	}

    public ProductResponse create(CreateProductRequest request) {
        Product product = productMapper.toEntity(request);
        Product saved = productRepository.save(product);
        return productMapper.toResponse(saved);
    }

    public ProductResponse getById(Long id) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Product not found"));
        return productMapper.toResponse(product);
    }

	public List<ProductResponse> getAll() {
	    List<Product> products = productRepository.findAll();
	    return productMapper.toResponseList(products);
	}

    public ProductResponse update(Long id, UpdateProductRequest request) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Product not found"));

        productMapper.updateEntity(request, product);

        Product updated = productRepository.save(product);
        return productMapper.toResponse(updated);
    }

    public void delete(Long id) {
        if (!productRepository.existsById(id)) {
            throw new NotFoundException("Product not found");
        }

        if (saleItemRepository.existsByProductId(id)) {
            throw new ConflictException("Product is referenced by existing sales and cannot be deleted");
        }
        productRepository.deleteById(id);
    }
}