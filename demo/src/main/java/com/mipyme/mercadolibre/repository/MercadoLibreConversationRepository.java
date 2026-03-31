package com.mipyme.mercadolibre.repository;

import com.mipyme.mercadolibre.model.messaging.MercadoLibreConversation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MercadoLibreConversationRepository extends JpaRepository<MercadoLibreConversation, Long> {
}
