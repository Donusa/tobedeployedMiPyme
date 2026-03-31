package com.mipyme.orders.repository;

import com.mipyme.orders.model.LocalOrderTracking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface LocalOrderTrackingRepository extends JpaRepository<LocalOrderTracking, Long> {
    Optional<LocalOrderTracking> findByExternalIdAndSource(String externalId, String source);
    List<LocalOrderTracking> findByExternalIdInAndSource(List<String> externalIds, String source);
    List<LocalOrderTracking> findBySource(String source);
}
