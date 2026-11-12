package com.shopwise.app.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.shopwise.app.entity.Sale;

@Repository
public interface SaleRepository extends JpaRepository<Sale, Long> {
	@EntityGraph(attributePaths = "items")
	List<Sale> findAllByOrderBySaleDateDescIdDesc();

	@EntityGraph(attributePaths = { "items", "items.product" })
	@Query("select s from Sale s where s.id = :id")
	Optional<Sale> findDetailById(@Param("id") Long id);

	@Query("select coalesce(max(s.id), 0L) from Sale s")
	long findMaxId();
}
