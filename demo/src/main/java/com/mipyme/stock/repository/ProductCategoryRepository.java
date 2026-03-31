package com.mipyme.stock.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mipyme.stock.model.ProductCategory;

public interface ProductCategoryRepository extends JpaRepository<ProductCategory, Long> {
    boolean existsByCategoryNameAndParentCategory(String categoryName, ProductCategory parentCategory);
    boolean existsByCategoryNameAndParentCategoryIsNull(String categoryName);
}
