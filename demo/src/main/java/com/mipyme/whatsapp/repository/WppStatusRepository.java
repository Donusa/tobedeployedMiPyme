package com.mipyme.whatsapp.repository;

import com.mipyme.whatsapp.model.WppStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WppStatusRepository extends JpaRepository<WppStatus, Long> {

    boolean existsByWamidAndStatusAndTimestamp(String wamid, String status, java.time.LocalDateTime timestamp);
}
