package com.mipyme.mercadopago.repository;

import com.mipyme.mercadopago.model.MpPlanConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MpPlanConfigRepository extends JpaRepository<MpPlanConfig, Long> {

    Optional<MpPlanConfig> findByPlanKey(String planKey);

    List<MpPlanConfig> findAllByOrderByPlanKeyAsc();
}
