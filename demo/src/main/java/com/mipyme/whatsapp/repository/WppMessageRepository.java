package com.mipyme.whatsapp.repository;

import com.mipyme.whatsapp.model.WppMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WppMessageRepository extends JpaRepository<WppMessage, Long> {

    boolean existsByWamid(String wamid);

    List<WppMessage> findByConversationIdOrderByTimestampAsc(Long conversationId);

    Page<WppMessage> findByConversationIdOrderByTimestampDesc(Long conversationId, Pageable pageable);
}
