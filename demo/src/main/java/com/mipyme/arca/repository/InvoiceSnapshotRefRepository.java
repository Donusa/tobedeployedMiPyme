package com.mipyme.arca.repository;

import com.mipyme.arca.model.InvoiceSnapshotRef;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceSnapshotRefRepository extends JpaRepository<InvoiceSnapshotRef, Long> {

    Optional<InvoiceSnapshotRef> findByInvoiceIndexId(Long invoiceIndexId);

    List<InvoiceSnapshotRef> findByExpiresAtBeforeAndStoragePathIsNotNull(LocalDate cutoff);
}
