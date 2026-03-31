package com.mipyme.arca.repository;

import com.mipyme.arca.model.InvoiceIndex;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceIndexRepository extends JpaRepository<InvoiceIndex, Long> {

    Optional<InvoiceIndex> findByIdempotencyKey(String idempotencyKey);

    Optional<InvoiceIndex> findByCuitEmisorAndPtoVtaAndCbteTipoAndCbteNro(
            String cuitEmisor, Short ptoVta, Short cbteTipo, Long cbteNro);

    List<InvoiceIndex> findByCreatedAtBetweenOrderByCreatedAtDesc(
            LocalDateTime from, LocalDateTime to);

    List<InvoiceIndex> findByCbteFchBetweenOrderByCbteFchDesc(LocalDate from, LocalDate to);

    @Query("SELECT i FROM InvoiceIndex i WHERE i.cbteFch BETWEEN :from AND :to " +
           "AND (:cbteTipo IS NULL OR i.cbteTipo = :cbteTipo) " +
           "AND (:resultado IS NULL OR i.resultado = :resultado) " +
           "ORDER BY i.cbteFch DESC, i.cbteNro DESC")
    List<InvoiceIndex> findByFilters(LocalDate from, LocalDate to, Short cbteTipo, String resultado);

    Optional<InvoiceIndex> findByCae(String cae);

    Optional<InvoiceIndex> findBySaleId(Long saleId);

    long countByResultado(String resultado);
}
