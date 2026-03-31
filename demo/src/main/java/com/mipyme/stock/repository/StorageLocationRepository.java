package com.mipyme.stock.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mipyme.stock.model.StorageLocation;

public interface StorageLocationRepository extends JpaRepository<StorageLocation, Long> {
}
