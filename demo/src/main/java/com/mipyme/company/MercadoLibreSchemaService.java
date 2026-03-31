package com.mipyme.company;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Service
public class MercadoLibreSchemaService {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public void createMessagingTables() {
        try {
            entityManager.createNativeQuery(
                "CREATE TABLE IF NOT EXISTS meli_notification_log (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                "payload VARCHAR(2000), " +
                "topic VARCHAR(255), " +
                "resource VARCHAR(255), " +
                "user_id BIGINT, " +
                "status VARCHAR(50), " +
                "received_at DATETIME, " +
                "processed_at DATETIME, " +
                "error_message VARCHAR(1000)" +
                ")"
            ).executeUpdate();

            entityManager.createNativeQuery(
                "CREATE TABLE IF NOT EXISTS meli_conversations (" +
                "id BIGINT PRIMARY KEY, " +
                "seller_id BIGINT, " +
                "buyer_id BIGINT, " +
                "status VARCHAR(50), " +
                "substatus VARCHAR(50), " +
                "blocked BOOLEAN, " +
                "last_message_date DATETIME, " +
                "unread_count INT" +
                ")"
            ).executeUpdate();

            entityManager.createNativeQuery(
                "CREATE TABLE IF NOT EXISTS meli_messages (" +
                "id VARCHAR(255) PRIMARY KEY, " +
                "conversation_id BIGINT, " +
                "from_user_id BIGINT, " +
                "to_user_id BIGINT, " +
                "text TEXT, " +
                "status VARCHAR(50), " +
                "date_created DATETIME, " +
                "date_read DATETIME, " +
                "read_by_me BOOLEAN, " +
                "FOREIGN KEY (conversation_id) REFERENCES meli_conversations(id)" +
                ")"
            ).executeUpdate();

            System.out.println("MercadoLibre messaging tables created successfully.");
        } catch (Exception e) {
            System.err.println("Error creating MercadoLibre messaging tables: " + e.getMessage());
        }
    }
}
