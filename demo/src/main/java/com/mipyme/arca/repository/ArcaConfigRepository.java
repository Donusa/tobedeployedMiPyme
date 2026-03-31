package com.mipyme.arca.repository;

import com.mipyme.arca.model.ArcaConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

public interface ArcaConfigRepository extends JpaRepository<ArcaConfig, Long> {
    Optional<ArcaConfig> findTopByOrderByIdDesc();

    @Modifying
    @Transactional
    @Query("UPDATE ArcaConfig c SET c.taCiphertext = NULL, c.taIv = NULL, c.taKeyVersion = NULL, c.tokenExpiration = NULL WHERE c.tokenExpiration < :now")
    void purgeExpiredTokens(LocalDateTime now);
}
