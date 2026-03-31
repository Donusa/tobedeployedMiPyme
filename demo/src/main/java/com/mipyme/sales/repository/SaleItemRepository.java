package com.mipyme.sales.repository;

import com.mipyme.sales.model.SaleItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SaleItemRepository extends JpaRepository<SaleItem, Long> {
    boolean existsByProductVariantId(Long productVariantId);
    boolean existsByProductId(Long productId);
}
