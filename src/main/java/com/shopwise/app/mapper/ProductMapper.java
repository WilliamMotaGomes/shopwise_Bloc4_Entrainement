package com.shopwise.app.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import com.shopwise.app.dto.request.CreateProductRequest;
import com.shopwise.app.dto.request.UpdateProductRequest;
import com.shopwise.app.dto.response.ProductResponse;
import com.shopwise.app.entity.Product;

@Mapper(componentModel = "spring")
public interface ProductMapper {
    Product toEntity(CreateProductRequest dto);

    void updateEntity(UpdateProductRequest dto, @MappingTarget Product entity);

    ProductResponse toResponse(Product product);

    List<ProductResponse> toResponseList(List<Product> products);
}
