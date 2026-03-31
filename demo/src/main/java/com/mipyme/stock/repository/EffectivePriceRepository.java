package com.mipyme.stock.repository;

import com.mipyme.stock.model.Channel;
import com.mipyme.stock.model.EffectivePrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface EffectivePriceRepository extends JpaRepository<EffectivePrice, Long> {
    Optional<EffectivePrice> findByLocalProductIdAndChannel(Long localProductId, Channel channel);

    List<EffectivePrice> findByLocalProductId(Long localProductId);

    void deleteByLocalProductIdAndChannel(Long localProductId, Channel channel);

    void deleteBySourceOfferId(Long sourceOfferId);
}
