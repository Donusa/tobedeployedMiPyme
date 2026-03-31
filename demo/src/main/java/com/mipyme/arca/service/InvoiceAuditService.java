package com.mipyme.arca.service;

import com.mipyme.arca.model.InvoiceAuditLog;
import com.mipyme.arca.repository.InvoiceAuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.List;

@Service
public class InvoiceAuditService {

    private static final Logger logger = LoggerFactory.getLogger(InvoiceAuditService.class);
    private static final String GENESIS_HASH = sha256Static("GENESIS");
    private static final DateTimeFormatter ISO_FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final InvoiceAuditLogRepository auditLogRepository;

    public InvoiceAuditService(InvoiceAuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }


    @Transactional(propagation = Propagation.MANDATORY)
    public InvoiceAuditLog append(String eventType, Long invoiceIndexId, String fiscalKey,
                                   String actor, String detail,
                                   String requestHash, String responseHash) {

        String prevLogHash = auditLogRepository.findLastEntryForUpdate()
                .map(InvoiceAuditLog::getEntryHash)
                .orElse(GENESIS_HASH);

        InvoiceAuditLog entry = new InvoiceAuditLog();
        entry.setEventType(eventType);
        entry.setInvoiceIndexId(invoiceIndexId);
        entry.setFiscalKey(fiscalKey);
        entry.setActor(actor);
        entry.setDetail(truncate(detail, 500));
        entry.setRequestHash(requestHash);
        entry.setResponseHash(responseHash);
        entry.setPrevLogHash(prevLogHash);
        entry.setCreatedAt(LocalDateTime.now());


        String hashInput = safe(eventType) + "|" +
                safe(fiscalKey) + "|" +
                safe(actor) + "|" +
                safe(entry.getDetail()) + "|" +
                safe(requestHash) + "|" +
                safe(responseHash) + "|" +
                safe(prevLogHash) + "|" +
                entry.getCreatedAt().format(ISO_FMT);
        entry.setEntryHash(sha256(hashInput));

        return auditLogRepository.save(entry);
    }


    @Transactional(readOnly = true)
    public boolean verifyChain() {
        List<InvoiceAuditLog> all = auditLogRepository.findAllByOrderByIdAsc();
        if (all.isEmpty()) return true;

        String expectedPrev = GENESIS_HASH;
        for (InvoiceAuditLog entry : all) {

            if (!expectedPrev.equals(entry.getPrevLogHash())) {
                logger.error("Audit chain broken at id={}: expected prev_log_hash={} but found={}",
                        entry.getId(), expectedPrev, entry.getPrevLogHash());
                return false;
            }


            String hashInput = safe(entry.getEventType()) + "|" +
                    safe(entry.getFiscalKey()) + "|" +
                    safe(entry.getActor()) + "|" +
                    safe(entry.getDetail()) + "|" +
                    safe(entry.getRequestHash()) + "|" +
                    safe(entry.getResponseHash()) + "|" +
                    safe(entry.getPrevLogHash()) + "|" +
                    entry.getCreatedAt().format(ISO_FMT);
            String recomputed = sha256(hashInput);

            if (!recomputed.equals(entry.getEntryHash())) {
                logger.error("Audit entry tampered at id={}: stored hash={}, recomputed={}",
                        entry.getId(), entry.getEntryHash(), recomputed);
                return false;
            }

            expectedPrev = entry.getEntryHash();
        }

        logger.info("Audit chain verified OK: {} entries", all.size());
        return true;
    }


    @Transactional(readOnly = true)
    public List<InvoiceAuditLog> getHistory(Long invoiceIndexId) {
        return auditLogRepository.findByInvoiceIndexIdOrderByCreatedAtAsc(invoiceIndexId);
    }



    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 unavailable", e);
        }
    }

    private static String sha256Static(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 unavailable", e);
        }
    }

    private static String safe(String value) {
        return value != null ? value : "";
    }

    private static String truncate(String value, int maxLen) {
        if (value == null) return null;
        return value.length() <= maxLen ? value : value.substring(0, maxLen);
    }
}
