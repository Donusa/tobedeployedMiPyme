package com.mipyme.access.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.mipyme.access.model.ActiveSession;

@Repository
public interface ActiveSessionRepository extends JpaRepository<ActiveSession, Long> {
    List<ActiveSession> findByUsername(String username);
    Optional<ActiveSession> findByToken(String token);
    Optional<ActiveSession> findBySessionIdentifier(String sessionIdentifier);
    void deleteByToken(String token);
    void deleteByUsernameAndIpAddress(String username, String ipAddress);
    void deleteByUsernameAndIpAddressAndDevice(String username, String ipAddress, String device);
    void deleteByUsernameAndTokenNot(String username, String token);
    void deleteByUsernameAndSessionIdentifierNot(String username, String sessionIdentifier);
    void deleteByUsername(String username);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE ActiveSession s SET s.username = :newUsername WHERE s.username = :oldUsername")
    void updateUsername(String oldUsername, String newUsername);
}
