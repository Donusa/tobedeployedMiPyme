package com.mipyme.mercadopago.repository;

import com.mipyme.mercadopago.model.MpExchangeRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MpExchangeRateRepository extends JpaRepository<MpExchangeRate, Long> {

    Optional<MpExchangeRate> findTopByOrderByFetchedAtDesc();
}
