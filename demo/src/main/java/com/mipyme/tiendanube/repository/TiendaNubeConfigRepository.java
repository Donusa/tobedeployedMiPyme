package com.mipyme.tiendanube.repository;

import com.mipyme.tiendanube.model.TiendaNubeConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TiendaNubeConfigRepository extends JpaRepository<TiendaNubeConfig, Long> {

    Optional<TiendaNubeConfig> findTopByOrderByIdDesc();
}
