package com.mipyme.tenant;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;

public class SchemaTenantIdentifierResolver implements CurrentTenantIdentifierResolver<String> {

	private final String defaultTenant;

	public SchemaTenantIdentifierResolver(String defaultTenant) {
		this.defaultTenant = defaultTenant;
	}

	@Override
	public String resolveCurrentTenantIdentifier() {
		String tenant = TenantContext.getCurrentTenant();
		return (tenant == null || tenant.isBlank()) ? defaultTenant : tenant;
	}

	@Override
	public boolean validateExistingCurrentSessions() {
		return true;
	}
}

