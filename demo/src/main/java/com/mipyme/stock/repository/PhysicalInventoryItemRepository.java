package com.mipyme.stock.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mipyme.stock.model.PhysicalInventoryItem;

public interface PhysicalInventoryItemRepository extends JpaRepository<PhysicalInventoryItem, Long> {
}
