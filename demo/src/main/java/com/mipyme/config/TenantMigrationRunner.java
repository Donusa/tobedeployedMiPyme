package com.mipyme.config;

import com.mipyme.company.Company;
import com.mipyme.company.CompanyRepository;
import com.mipyme.company.CompanySchemaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TenantMigrationRunner implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(TenantMigrationRunner.class);

    private final CompanyRepository companyRepository;
    private final CompanySchemaService companySchemaService;

    public TenantMigrationRunner(CompanyRepository companyRepository, CompanySchemaService companySchemaService) {
        this.companyRepository = companyRepository;
        this.companySchemaService = companySchemaService;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        logger.info("=================================================");
        logger.info("STARTING TENANT SCHEMA MIGRATION CHECK");
        logger.info("=================================================");

        try {
            List<Company> companies = companyRepository.findAll();
            logger.info("Found {} companies/tenants to check.", companies.size());

            for (Company company : companies) {
                try {
                    logger.info("Ensuring schema and tables for tenant: {} (Company ID: {})",
                            company.getTenantSchema(), company.getCompanyId());
                    companySchemaService.ensureTenantSchemaAndTables(company.getTenantSchema());
                    logger.info("Successfully verified/migrated schema for tenant: {}",
                            company.getTenantSchema());
                } catch (Exception e) {
                    logger.error("CRITICAL ERROR: Failed to migrate schema for tenant: " +
                            company.getTenantSchema(), e);

                }
            }
        } catch (Exception ex) {
            logger.error("Failed to fetch companies for migration", ex);
        }

        logger.info("=================================================");
        logger.info("TENANT SCHEMA MIGRATION CHECK COMPLETED");
        logger.info("=================================================");
    }
}
