package com.mipyme.sales.repository;

import com.mipyme.sales.model.Sale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SaleRepository extends JpaRepository<Sale, Long> {
    @Query("SELECT s FROM Sale s LEFT JOIN FETCH s.items ORDER BY s.saleDate DESC")
    List<Sale> findAllByOrderBySaleDateDesc();

    @Query("SELECT DISTINCT s FROM Sale s LEFT JOIN FETCH s.items WHERE s.saleDate BETWEEN :startDate AND :endDate ORDER BY s.saleDate ASC")
    List<Sale> findBySaleDateBetween(@Param("startDate") java.time.LocalDateTime startDate, @Param("endDate") java.time.LocalDateTime endDate);

    @Query("SELECT s FROM Sale s LEFT JOIN FETCH s.items WHERE s.saleId = :id")
    java.util.Optional<Sale> findByIdWithItems(@Param("id") Long id);
}
