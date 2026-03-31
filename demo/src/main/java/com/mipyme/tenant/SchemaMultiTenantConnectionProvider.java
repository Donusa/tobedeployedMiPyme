package com.mipyme.tenant;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;

public class SchemaMultiTenantConnectionProvider implements MultiTenantConnectionProvider<String> {

	private static final long serialVersionUID = 8223078019080103950L;
	private final DataSource dataSource;
	private final String defaultTenant;

	public SchemaMultiTenantConnectionProvider(DataSource dataSource, String defaultTenant) {
		this.dataSource = dataSource;
		this.defaultTenant = defaultTenant;
	}

	@Override
	public Connection getAnyConnection() throws SQLException {
		return dataSource.getConnection();
	}

	@Override
	public void releaseAnyConnection(Connection connection) throws SQLException {
		connection.close();
	}

	@Override
	public Connection getConnection(String tenantIdentifier) throws SQLException {
		Connection connection = getAnyConnection();
		String tenant = (tenantIdentifier == null || tenantIdentifier.isBlank()) ? defaultTenant : tenantIdentifier;

		if (tenant != null && !tenant.isBlank()) {
			try {
				connection.setCatalog(tenant);
			} catch (SQLException ex) {
				connection.setSchema(tenant);
			}
		} else {

			System.err.println("CRITICAL: Tenant identifier and defaultTenant are both null/empty. Defaulting to 'mipyme'");
			try {
				connection.setCatalog("mipyme");
			} catch (SQLException ex) {
				connection.setSchema("mipyme");
			}
		}
		return connection;
	}

	@Override
	public void releaseConnection(String tenantIdentifier, Connection connection) throws SQLException {

		String resetTo = (defaultTenant != null && !defaultTenant.isBlank()) ? defaultTenant : "mipyme";
		try {
			connection.setCatalog(resetTo);
		} catch (SQLException ex) {
			connection.setSchema(resetTo);
		}
		releaseAnyConnection(connection);
	}

	@Override
	public boolean supportsAggressiveRelease() {
		return false;
	}

	@Override
	public boolean isUnwrappableAs(Class<?> unwrapType) {
		return unwrapType.isAssignableFrom(getClass());
	}

	@Override
	public <T> T unwrap(Class<T> unwrapType) {
		if (isUnwrappableAs(unwrapType)) {
			return unwrapType.cast(this);
		}
		throw new IllegalArgumentException("Unknown unwrap type: " + unwrapType);
	}
}
