package com.mipyme.mercadolibre.repository;

import com.mipyme.mercadolibre.model.MercadoLibreConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface MercadoLibreConfigRepository extends JpaRepository<MercadoLibreConfig, Long> {
    Optional<MercadoLibreConfig> findTopByOrderByIdDesc();
}
