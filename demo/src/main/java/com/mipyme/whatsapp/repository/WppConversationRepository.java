package com.mipyme.whatsapp.repository;

import com.mipyme.whatsapp.model.WppConversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WppConversationRepository extends JpaRepository<WppConversation, Long> {

    Optional<WppConversation> findByContactWaId(String contactWaId);

    List<WppConversation> findAllByOrderByLastMessageAtDesc();
}
