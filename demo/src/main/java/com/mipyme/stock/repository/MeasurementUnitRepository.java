package com.mipyme.stock.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mipyme.stock.model.MeasurementUnit;

public interface MeasurementUnitRepository extends JpaRepository<MeasurementUnit, String> {
}
