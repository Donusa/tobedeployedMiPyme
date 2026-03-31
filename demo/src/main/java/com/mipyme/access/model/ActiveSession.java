package com.mipyme.access.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "active_sessions")
public class ActiveSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String username;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String token;

    @Column(nullable = false)
    private LocalDateTime created;

    @Column(name = "last_active")
    private LocalDateTime lastActive;

    @Column(name = "ip_address")
    private String ipAddress;

    private String device;

    private String location;

    @Column(nullable = false)
    private boolean isCurrent;

    @Column(nullable = false)
    private String medium;

    @Column(name = "session_identifier")
    private String sessionIdentifier;

    public ActiveSession() {
    }

    public ActiveSession(String username, String token, LocalDateTime created, LocalDateTime lastActive, String ipAddress, String device, String location, String medium, String sessionIdentifier) {
        this.username = username;
        this.token = token;
        this.created = created;
        this.lastActive = lastActive;
        this.ipAddress = ipAddress;
        this.device = device;
        this.location = location;
        this.medium = medium;
        this.sessionIdentifier = sessionIdentifier;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public LocalDateTime getLastActive() {
        return lastActive;
    }

    public void setLastActive(LocalDateTime lastActive) {
        this.lastActive = lastActive;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getDevice() {
        return device;
    }

    public String getLocation() {
        return location;
    }

    public String getMedium() {
        return medium;
    }

    public String getSessionIdentifier() {
        return sessionIdentifier;
    }

    public void setSessionIdentifier(String sessionIdentifier) {
        this.sessionIdentifier = sessionIdentifier;
    }
}
