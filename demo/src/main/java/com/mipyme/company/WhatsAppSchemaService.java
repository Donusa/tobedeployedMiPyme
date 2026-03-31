package com.mipyme.company;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Service
public class WhatsAppSchemaService {

    private static final Logger logger = LoggerFactory.getLogger(WhatsAppSchemaService.class);

    @PersistenceContext
    private EntityManager entityManager;


    @Transactional
    public void performWhatsAppMigrations() {
        try {
            logger.info("Starting WhatsApp schema migrations...");


            addTenantIdToWppConnection();



            logger.info("WhatsApp schema migrations completed successfully.");
        } catch (Exception e) {
            logger.error("Error during WhatsApp schema migrations", e);
            throw new RuntimeException("WhatsApp migration failed: " + e.getMessage(), e);
        }
    }


    private void addTenantIdToWppConnection() {
        try {

            String checkColumn = "SELECT COUNT(*) as cnt FROM INFORMATION_SCHEMA.COLUMNS " +
                    "WHERE TABLE_NAME='wpp_connection' AND COLUMN_NAME='tenant_id' " +
                    "AND TABLE_SCHEMA=DATABASE()";

            Number columnExists = (Number) entityManager.createNativeQuery(checkColumn)
                    .getSingleResult();

            if (columnExists.intValue() == 0) {

                entityManager.createNativeQuery(
                    "ALTER TABLE wpp_connection " +
                    "ADD COLUMN tenant_id VARCHAR(255) NOT NULL DEFAULT 'default'"
                ).executeUpdate();

                logger.info("Added tenant_id column to wpp_connection table");


                try {
                    entityManager.createNativeQuery(
                        "ALTER TABLE wpp_connection ADD INDEX idx_tenant_id (tenant_id)"
                    ).executeUpdate();
                    logger.info("Added index idx_tenant_id to wpp_connection table");
                } catch (Exception e) {

                    logger.debug("Index idx_tenant_id already exists or could not be created");
                }

                try {
                    entityManager.createNativeQuery(
                        "ALTER TABLE wpp_connection ADD INDEX idx_tenant_status (tenant_id, status)"
                    ).executeUpdate();
                    logger.info("Added index idx_tenant_status to wpp_connection table");
                } catch (Exception e) {

                    logger.debug("Index idx_tenant_status already exists or could not be created");
                }
            } else {
                logger.debug("Column tenant_id already exists in wpp_connection table, skipping migration");
            }
        } catch (Exception e) {
            logger.error("Error adding tenant_id column to wpp_connection", e);
            throw new RuntimeException("Failed to add tenant_id to wpp_connection: " + e.getMessage(), e);
        }
    }
}
