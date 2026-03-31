package com.mipyme.access.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.mipyme.access.model.AccessLog;
import com.mipyme.access.model.ActiveSession;
import com.mipyme.access.repository.AccessLogRepository;
import com.mipyme.access.repository.ActiveSessionRepository;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class AccessService {

    private final AccessLogRepository accessLogRepository;
    private final ActiveSessionRepository activeSessionRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    public AccessService(AccessLogRepository accessLogRepository, ActiveSessionRepository activeSessionRepository) {
        this.accessLogRepository = accessLogRepository;
        this.activeSessionRepository = activeSessionRepository;
    }

    @Transactional
    public void logAccess(String username, String ipAddress, String userAgent, String medium) {
        String device = parseDevice(userAgent);
        String location = resolveLocation(ipAddress);

        AccessLog log = new AccessLog(username, LocalDateTime.now(), ipAddress, device, location, medium);
        accessLogRepository.save(log);
    }

    @Transactional
    public void createSession(String username, String token, String ipAddress, String userAgent, String medium, String sessionIdentifier) {


        String device = parseDevice(userAgent);
        activeSessionRepository.deleteByUsernameAndIpAddressAndDevice(username, ipAddress, device);

        String location = resolveLocation(ipAddress);

        ActiveSession session = new ActiveSession(username, token, LocalDateTime.now(), LocalDateTime.now(), ipAddress, device, location, medium, sessionIdentifier);
        activeSessionRepository.save(session);
    }

    @Transactional(readOnly = true)
    public boolean isSessionValid(String sessionIdentifier) {
        if (sessionIdentifier == null) return true;
        return activeSessionRepository.findBySessionIdentifier(sessionIdentifier).isPresent();
    }

    @Transactional
    public void updateSessionActivity(String token) {
        activeSessionRepository.findByToken(token).ifPresent(session -> {
            session.setLastActive(LocalDateTime.now());
            activeSessionRepository.save(session);
        });
    }

    @Transactional
    public void rotateSessionToken(String oldToken, String newToken) {
        activeSessionRepository.findByToken(oldToken).ifPresent(session -> {
            session.setToken(newToken);
            session.setLastActive(LocalDateTime.now());
            activeSessionRepository.save(session);
        });
    }

    @Transactional
    public void revokeSession(Long sessionId) {
        activeSessionRepository.deleteById(sessionId);
    }

    @Transactional
    public void revokeSessionByToken(String token) {
        activeSessionRepository.deleteByToken(token);
    }

    @Transactional
    public void revokeAllOtherSessions(String username, String currentToken) {
        activeSessionRepository.deleteByUsernameAndTokenNot(username, currentToken);
    }

    @Transactional
    public void revokeAllSessions(String username) {
        activeSessionRepository.deleteByUsername(username);
    }

    @Transactional
    public void revokeAllOtherSessionsBySid(String username, String sessionIdentifier) {
        activeSessionRepository.deleteByUsernameAndSessionIdentifierNot(username, sessionIdentifier);
    }

    @Transactional
    public void updateUsernameInSessions(String oldUsername, String newUsername) {
        activeSessionRepository.updateUsername(oldUsername, newUsername);
    }

    @Transactional
    public void updateSessionTokenBySid(String sid, String newToken) {
        activeSessionRepository.findBySessionIdentifier(sid).ifPresent(session -> {
            session.setToken(newToken);
            session.setLastActive(LocalDateTime.now());
            activeSessionRepository.save(session);
        });
    }


    public List<AccessLog> getRecentAccessLogs(String username) {
        return accessLogRepository.findByUsernameOrderByTimestampDesc(username);
    }









    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void pruneAccessLogs() {
        LocalDateTime now          = LocalDateTime.now();
        LocalDateTime cutoff365    = now.minusDays(365);
        LocalDateTime cutoff90     = now.minusDays(90);


        accessLogRepository.deleteByTimestampBefore(cutoff365);


        List<AccessLog> window = accessLogRepository
                .findByTimestampBetweenOrderByTimestampDesc(cutoff365, cutoff90);


        Map<String, Map<LocalDate, List<AccessLog>>> grouped = window.stream()
                .collect(Collectors.groupingBy(
                        AccessLog::getUsername,
                        Collectors.groupingBy(l -> l.getTimestamp().toLocalDate())));

        List<Long> toDelete = new ArrayList<>();
        grouped.values().forEach(byDate ->
                byDate.values().forEach(dailyLogs -> {

                    dailyLogs.stream()
                            .skip(1)
                            .map(AccessLog::getId)
                            .forEach(toDelete::add);
                }));

        if (!toDelete.isEmpty()) {
            accessLogRepository.deleteAllById(toDelete);
        }
    }

    public List<ActiveSession> getActiveSessions(String username) {
        return activeSessionRepository.findByUsername(username);
    }

    private String parseDevice(String userAgent) {
        if (userAgent == null) return "Unknown";
        if (userAgent.contains("Mobi")) return "Mobile";
        if (userAgent.contains("Tablet")) return "Tablet";
        if (userAgent.contains("Windows")) return "Windows PC";
        if (userAgent.contains("Mac")) return "Mac";
        if (userAgent.contains("Linux")) return "Linux PC";
        return "Desktop";
    }

    public String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }

    public String getUserAgent(HttpServletRequest request) {
        return request.getHeader("User-Agent");
    }

    private String resolveLocation(String ipAddress) {
        if (ipAddress == null || "127.0.0.1".equals(ipAddress) || "0:0:0:0:0:0:0:1".equals(ipAddress)) {
            return "Localhost";
        }
        try {


            String url = "http://ip-api.com/json/" + ipAddress;
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response != null && "success".equals(response.get("status"))) {
                String city = (String) response.get("city");
                String region = (String) response.get("regionName");
                String country = (String) response.get("country");

                StringBuilder loc = new StringBuilder();
                if (city != null && !city.isEmpty()) loc.append(city);

                if (region != null && !region.isEmpty()) {
                    if (loc.length() > 0) loc.append(", ");
                    loc.append(region);
                }

                if (country != null && !country.isEmpty()) {
                    if (loc.length() > 0) loc.append(", ");
                    loc.append(country);
                }

                return loc.length() > 0 ? loc.toString() : ipAddress;
            }
        } catch (Exception e) {


        }
        return ipAddress;
    }
}
