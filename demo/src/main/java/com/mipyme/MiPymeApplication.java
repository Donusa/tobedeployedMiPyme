package com.mipyme;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.CommandLineRunner;
import com.mipyme.company.MercadoLibreSchemaService;
import com.mipyme.company.WhatsAppSchemaService;
import org.springframework.web.client.RestTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import javax.sql.DataSource;

@EnableAsync
@EnableScheduling
@SpringBootApplication
public class MiPymeApplication {

    public static void main(String[] args) {
        SpringApplication.run(MiPymeApplication.class, args);
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    CommandLineRunner initMessagingSchema(MercadoLibreSchemaService schemaService) {
        return args -> {
            schemaService.createMessagingTables();
        };
    }

    @Bean
    CommandLineRunner performWhatsAppMigrations(WhatsAppSchemaService whatsAppService) {
        return args -> {
            whatsAppService.performWhatsAppMigrations();
        };
    }

    @Bean
    CommandLineRunner fixNullVersions(DataSource dataSource) {
        return args -> {
            JdbcTemplate jdbc = new JdbcTemplate(dataSource);
            int updated = jdbc.update("UPDATE mipyme.companies SET version = 0 WHERE version IS NULL");
            if (updated > 0) {
                System.out.println("Fixed " + updated + " companies with NULL version.");
            }
        };
    }

    @Bean
    CommandLineRunner migratePlanStatusToVarchar(DataSource dataSource) {
        return args -> {
            JdbcTemplate jdbc = new JdbcTemplate(dataSource);


            String columnType = jdbc.queryForObject(
                "SELECT COLUMN_TYPE FROM INFORMATION_SCHEMA.COLUMNS " +
                "WHERE TABLE_SCHEMA = 'mipyme' AND TABLE_NAME = 'companies' AND COLUMN_NAME = 'plan_status'",
                String.class);
            if (columnType != null && columnType.toLowerCase().startsWith("enum")) {
                jdbc.execute("ALTER TABLE mipyme.companies MODIFY COLUMN plan_status VARCHAR(40) NOT NULL DEFAULT 'PENDING_PAYMENT'");
                System.out.println("Migrated plan_status column from ENUM to VARCHAR(40).");
            }
        };
    }

}
