package com.mipyme.stock.repository;

import com.mipyme.stock.model.OfferAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OfferAuditLogRepository extends JpaRepository<OfferAuditLog, Long> {
    List<OfferAuditLog> findByOfferIdOrderByCreatedAtDesc(Long offerId);

    List<OfferAuditLog> findByOfferIdAndChannelOrderByCreatedAtDesc(Long offerId,
            com.mipyme.stock.model.Channel channel);
}
