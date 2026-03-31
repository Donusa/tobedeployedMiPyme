package com.mipyme.tenant;

import javax.sql.DataSource;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HibernateMultiTenancyConfig {

	@Bean
	public HibernatePropertiesCustomizer hibernatePropertiesCustomizer(
			MultiTenantConnectionProvider<String> multiTenantConnectionProvider,
			CurrentTenantIdentifierResolver<String> currentTenantIdentifierResolver
	) {
		return props -> {
			props.put("hibernate.multiTenancy", "DATABASE");
			props.put("hibernate.multi_tenant", "DATABASE");
			props.put(AvailableSettings.MULTI_TENANT_CONNECTION_PROVIDER, multiTenantConnectionProvider);
			props.put(AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER, currentTenantIdentifierResolver);
		};
	}

	@Bean
	public String defaultTenant(@Value("${mipyme.tenant.default-schema:mipyme}") String defaultSchema) {
		if (defaultSchema == null || defaultSchema.isBlank()) {
			return "mipyme";
		}
		return defaultSchema;
	}

	@Bean
	public MultiTenantConnectionProvider<String> multiTenantConnectionProvider(DataSource dataSource, String defaultTenant) {
		return new SchemaMultiTenantConnectionProvider(dataSource, defaultTenant);
	}

	@Bean
	public CurrentTenantIdentifierResolver<String> currentTenantIdentifierResolver(String defaultTenant) {
		return new SchemaTenantIdentifierResolver(defaultTenant);
	}
}
