package com.mipyme.whatsapp.repository;

import com.mipyme.whatsapp.model.WppConnectSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

public interface WppConnectSessionRepository extends JpaRepository<WppConnectSession, Long> {

    Optional<WppConnectSession> findBySid(String sid);

    @Modifying
    @Transactional
    @Query("DELETE FROM WppConnectSession s WHERE s.expiresAt < ?1")
    void deleteExpired(LocalDateTime now);
}
