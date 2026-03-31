package com.mipyme.mercadolibre.repository;

import com.mipyme.mercadolibre.model.messaging.MercadoLibreMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface MercadoLibreMessageRepository extends JpaRepository<MercadoLibreMessage, String> {
    List<MercadoLibreMessage> findByConversation_IdOrderByDateCreatedAsc(Long conversationId);
    List<MercadoLibreMessage> findByConversation_IdAndDateCreatedLessThan(Long conversationId, LocalDateTime before, Pageable pageable);
}
