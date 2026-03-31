package com.mipyme.stock.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mipyme.stock.model.ProductBrand;

public interface ProductBrandRepository extends JpaRepository<ProductBrand, Long> {
}
