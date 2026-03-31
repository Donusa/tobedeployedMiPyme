package com.mipyme.stock.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mipyme.stock.model.StockMovement;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {
}
