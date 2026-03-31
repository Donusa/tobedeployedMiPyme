package com.mipyme.stock.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mipyme.stock.model.StockBalance;
import com.mipyme.stock.model.StockBalanceId;

public interface StockBalanceRepository extends JpaRepository<StockBalance, StockBalanceId> {
}
