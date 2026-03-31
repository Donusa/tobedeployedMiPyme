package com.mipyme.stock.repository;

import com.mipyme.stock.model.CartPromoRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CartPromoRuleRepository extends JpaRepository<CartPromoRule, Long> {
    List<CartPromoRule> findByTnStoreIdAndIsActive(Long tnStoreId, Boolean isActive);

    List<CartPromoRule> findByOfferId(Long offerId);

    void deleteByOfferId(Long offerId);
}
