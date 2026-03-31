package com.mipyme.arca.service;

import com.mipyme.arca.model.InvoiceSnapshotRef;
import com.mipyme.arca.repository.InvoiceSnapshotRefRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class InvoiceRetentionService {

    private static final Logger logger = LoggerFactory.getLogger(InvoiceRetentionService.class);

    private final InvoiceSnapshotRefRepository snapshotRefRepository;
    private final InvoiceAuditService auditService;

    public InvoiceRetentionService(InvoiceSnapshotRefRepository snapshotRefRepository,
                                    InvoiceAuditService auditService) {
        this.snapshotRefRepository = snapshotRefRepository;
        this.auditService = auditService;
    }


    @Scheduled(cron = "0 30 3 * * *")
    @Transactional
    public void runNightlyMaintenance() {
        purgeExpiredSnapshots();
        verifyAuditChain();
    }


    @Transactional
    public void purgeExpiredSnapshots() {
        List<InvoiceSnapshotRef> expired = snapshotRefRepository
                .findByExpiresAtBeforeAndStoragePathIsNotNull(LocalDate.now());

        if (expired.isEmpty()) return;

        for (InvoiceSnapshotRef ref : expired) {
            logger.info("Clearing expired snapshot ref id={} invoice_index_id={} path={}",
                    ref.getId(), ref.getInvoiceIndexId(), ref.getStoragePath());
            ref.setStoragePath(null);
            snapshotRefRepository.save(ref);
        }

        logger.info("Cleared {} expired snapshot references", expired.size());
    }


    public boolean verifyAuditChain() {
        boolean valid = auditService.verifyChain();
        if (!valid) {
            logger.error("AUDIT CHAIN INTEGRITY VIOLATION DETECTED");
        }
        return valid;
    }
}
