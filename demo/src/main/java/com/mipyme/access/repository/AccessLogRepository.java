package com.mipyme.access.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.mipyme.access.model.AccessLog;

@Repository
public interface AccessLogRepository extends JpaRepository<AccessLog, Long> {


    List<AccessLog> findByUsernameOrderByTimestampDesc(String username);


    List<AccessLog> findTop5ByUsernameOrderByTimestampDesc(String username);


    void deleteByTimestampBefore(LocalDateTime cutoff);


    List<AccessLog> findByTimestampBetweenOrderByTimestampDesc(
            LocalDateTime from, LocalDateTime to);
}
