package com.shopwise.app.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.shopwise.app.entity.SaleItem;

@Repository
public interface SaleItemRepository extends JpaRepository<SaleItem, Long> {
	boolean existsByProductId(Long productId);

	@Query("select i.sale.id, i.product.id, i.quantity from SaleItem i order by i.sale.id, i.id")
	List<Object[]> findPurchaseTuples();
}
