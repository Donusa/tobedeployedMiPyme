package com.mipyme.access.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "access_logs")
public class AccessLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "ip_address")
    private String ipAddress;

    private String device;

    private String location;

    private String medium;

    public AccessLog() {
    }

    public AccessLog(String username, LocalDateTime timestamp, String ipAddress, String device, String location, String medium) {
        this.username = username;
        this.timestamp = timestamp;
        this.ipAddress = ipAddress;
        this.device = device;
        this.location = location;
        this.medium = medium;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
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
}
