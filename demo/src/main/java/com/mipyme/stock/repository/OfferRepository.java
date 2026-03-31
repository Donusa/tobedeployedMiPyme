package com.mipyme.stock.repository;

import com.mipyme.stock.model.Offer;
import com.mipyme.stock.model.OfferStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OfferRepository extends JpaRepository<Offer, Long> {
    List<Offer> findByStatus(OfferStatus status);

    List<Offer> findByStatusIn(List<OfferStatus> statuses);
}
