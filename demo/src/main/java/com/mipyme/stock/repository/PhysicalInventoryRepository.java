package com.mipyme.stock.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mipyme.stock.model.PhysicalInventory;

public interface PhysicalInventoryRepository extends JpaRepository<PhysicalInventory, Long> {
}
