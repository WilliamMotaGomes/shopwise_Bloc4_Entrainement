package com.shopwise.app.repository;

import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.shopwise.app.entity.Product;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
	@Query("select max(p.updatedAt) from Product p")
	LocalDateTime findLastUpdate();
}
