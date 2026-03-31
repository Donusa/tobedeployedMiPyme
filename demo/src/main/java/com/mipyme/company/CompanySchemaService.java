package com.mipyme.company;

import java.util.Locale;

import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class CompanySchemaService {

	private static final Logger logger = LoggerFactory.getLogger(CompanySchemaService.class);

	private final JdbcTemplate jdbcTemplate;

	public CompanySchemaService(DataSource dataSource) {
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}

	public void ensureTenantSchemaAndTables(String tenantSchema) {
		String schema = sanitizeSchemaName(tenantSchema);
		jdbcTemplate.execute("CREATE DATABASE IF NOT EXISTS `" + schema + "`");

		createAppUsersTable(schema);
		createSecurityTables(schema);
		createStockTables(schema);
		createSalesTables(schema);
		createNotificationsTable(schema);
		createOfferTables(schema);
		createCajaTables(schema);
		performMigrations(schema);
	}

	private void performMigrations(String schema) {

		dropColumnIfExists(schema, "product_variants", "attribute_values");
		addColumnIfNotExists(schema, "products", "measurement_value", "VARCHAR(255)");
		addColumnIfNotExists(schema, "products", "stock_quantity", "DECIMAL(19,2) DEFAULT 0");
		addColumnIfNotExists(schema, "products", "min_stock", "DECIMAL(19,2) DEFAULT 0");
		addColumnIfNotExists(schema, "products", "cost", "DECIMAL(19,2) DEFAULT 0");
		modifyColumn(schema, "products", "measurement_value", "VARCHAR(255)");
		modifyColumn(schema, "products", "stock_quantity", "DECIMAL(19,2) DEFAULT 0");
		modifyColumn(schema, "products", "min_stock", "DECIMAL(19,2) DEFAULT 0");

		addColumnIfNotExists(schema, "product_variants", "stock_quantity", "DECIMAL(19,2) DEFAULT 0");
		addColumnIfNotExists(schema, "product_variants", "min_stock", "DECIMAL(19,2) DEFAULT 0");
		addColumnIfNotExists(schema, "product_variants", "cost", "DECIMAL(19,2) DEFAULT 0");
		modifyColumn(schema, "product_variants", "stock_quantity", "DECIMAL(19,2) DEFAULT 0");
		modifyColumn(schema, "product_variants", "min_stock", "DECIMAL(19,2) DEFAULT 0");


		modifyColumn(schema, "sale_items", "quantity", "DECIMAL(19,4) NOT NULL");


		addColumnIfNotExists(schema, "sales", "facturado", "BOOLEAN DEFAULT FALSE");


		addColumnIfNotExists(schema, "app_users", "permissions", "TEXT");


		addColumnIfNotExists(schema, "active_sessions", "session_identifier", "VARCHAR(36)");


		addColumnIfNotExists(schema, "active_sessions", "is_current", "BOOLEAN DEFAULT FALSE");


		addColumnIfNotExists(schema, "products", "tienda_nube_id", "BIGINT");
		addColumnIfNotExists(schema, "product_variants", "tienda_nube_id", "BIGINT");


		addColumnIfNotExists(schema, "sales", "created_by", "VARCHAR(255)");


		addColumnIfNotExists(schema, "sales", "medio_pago", "VARCHAR(30)");


		createTiendaNubeConfigTable(schema);


		createMercadoLibreConfigTable(schema);
		createMercadoLibreMessagingTables(schema);
		modifyColumn(schema, "mercadolibre_config", "scope", "TEXT");

		addColumnIfNotExists(schema, "products", "mercadolibre_id", "VARCHAR(255)");
		addColumnIfNotExists(schema, "product_variants", "mercadolibre_id", "VARCHAR(255)");


		createArcaConfigTable(schema);


		addColumnIfNotExists(schema, "app_users", "is_two_factor_enabled", "BOOLEAN DEFAULT FALSE");
		addColumnIfNotExists(schema, "app_users", "two_factor_code", "VARCHAR(10)");
		addColumnIfNotExists(schema, "app_users", "two_factor_expires_at", "DATETIME");


		addColumnIfNotExists(schema, "app_users", "password_recovery_code", "VARCHAR(10)");
		addColumnIfNotExists(schema, "app_users", "password_recovery_expires_at", "DATETIME");


		addColumnIfNotExists(schema, "offers", "buy_quantity", "INT");
		addColumnIfNotExists(schema, "offers", "pay_quantity", "INT");


		createLocalOrderTrackingTable(schema);
		addColumnIfNotExists(schema, "local_order_tracking", "payment_status", "VARCHAR(255)");


		addColumnIfNotExists(schema, "offers", "status", "VARCHAR(20) DEFAULT 'DRAFT'");
		addColumnIfNotExists(schema, "offers", "ml_promotion_id", "VARCHAR(255)");
		addColumnIfNotExists(schema, "offers", "ml_offer_id", "VARCHAR(255)");
		addColumnIfNotExists(schema, "offers", "ml_promotion_type", "VARCHAR(255)");
		addColumnIfNotExists(schema, "offers", "tn_mode", "VARCHAR(20)");
		addColumnIfNotExists(schema, "offers", "tn_promotion_id", "VARCHAR(255)");
		addColumnIfNotExists(schema, "offers", "last_synced_at", "DATETIME");
		addColumnIfNotExists(schema, "offers", "error_message", "TEXT");


		createOfferAuditLogTable(schema);
		createEffectivePricesTable(schema);
		createCartPromoRulesTable(schema);


		addColumnIfNotExists(schema, "notifications", "type", "VARCHAR(30) NOT NULL DEFAULT 'ML_WEBHOOK'");
		addColumnIfNotExists(schema, "notifications", "reference_key", "VARCHAR(255)");
		addColumnIfNotExists(schema, "notifications", "is_read", "BOOLEAN DEFAULT FALSE");
		addColumnIfNotExists(schema, "notifications", "viewed_at", "DATETIME");


		createWhatsAppTables(schema);


		addColumnIfNotExists(schema, "wpp_connection", "tenant_id", "VARCHAR(255) NOT NULL DEFAULT 'default'");



		addColumnIfNotExists(schema, "sale_items", "product_name", "VARCHAR(255)");
		addColumnIfNotExists(schema, "sale_items", "variant_sku", "VARCHAR(255)");
		addColumnIfNotExists(schema, "sale_items", "internal_code", "VARCHAR(255)");
		dropForeignKeyIfExists(schema, "sale_items", "fk_sale_items_product");
		dropForeignKeyIfExists(schema, "sale_items", "fk_sale_items_variant");
		backfillSaleItemSnapshots(schema);


		createInvoiceIndexTables(schema);


		addColumnIfNotExists(schema, "app_users", "warehouse_ids", "TEXT");


		createCajaTables(schema);


		createMercadoPagoTables(schema);


		addColumnIfNotExists(schema, "mp_subscription", "billing_day", "INT");
		addColumnIfNotExists(schema, "mp_subscription", "next_payment_date", "DATETIME(6)");
		addColumnIfNotExists(schema, "mp_subscription", "last_charged_date", "DATETIME(6)");


		addColumnIfNotExists(schema, "app_users", "theme", "VARCHAR(20) NOT NULL DEFAULT 'light'");
	}

	private void modifyColumn(String schema, String tableName, String columnName, String columnDefinition) {
		try {
			String checkSql = "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? AND COLUMN_NAME = ?";
			Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, schema, tableName, columnName);

			if (count != null && count > 0) {
				jdbcTemplate.execute("ALTER TABLE `" + schema + "`.`" + tableName + "` MODIFY COLUMN `" + columnName
						+ "` " + columnDefinition);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void addColumnIfNotExists(String schema, String tableName, String columnName, String columnDefinition) {
		try {
			logger.info("Checking if column {}.{}.{} exists...", schema, tableName, columnName);
			String checkSql = "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? AND COLUMN_NAME = ?";
			Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, schema, tableName, columnName);

			logger.info("Column check result for {}.{}.{}: {}", schema, tableName, columnName, count);

			if (count != null && count == 0) {
				logger.info("Adding column {}.{}.{}...", schema, tableName, columnName);
				jdbcTemplate.execute("ALTER TABLE `" + schema + "`.`" + tableName + "` ADD COLUMN `" + columnName + "` "
						+ columnDefinition);
				logger.info("Column added successfully.");
			} else {
				logger.info("Column {}.{}.{} already exists (or check failed).", schema, tableName, columnName);
			}
		} catch (Exception e) {
			logger.error("Error adding column " + columnName + " to table " + schema + "." + tableName, e);
		}
	}

	private void dropColumnIfExists(String schema, String tableName, String columnName) {
		try {
			String checkSql = "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? AND COLUMN_NAME = ?";
			Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, schema, tableName, columnName);

			if (count != null && count > 0) {
				jdbcTemplate
						.execute("ALTER TABLE `" + schema + "`.`" + tableName + "` DROP COLUMN `" + columnName + "`");
			}
		} catch (Exception e) {

			e.printStackTrace();
		}
	}

	private void dropForeignKeyIfExists(String schema, String tableName, String constraintName) {
		try {
			String checkSql = "SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS " +
					"WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? AND CONSTRAINT_NAME = ? AND CONSTRAINT_TYPE = 'FOREIGN KEY'";
			Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, schema, tableName, constraintName);
			if (count != null && count > 0) {
				logger.info("Dropping FK {}.{}.{}...", schema, tableName, constraintName);
				jdbcTemplate.execute("ALTER TABLE `" + schema + "`.`" + tableName + "` DROP FOREIGN KEY `" + constraintName + "`");
			}
		} catch (Exception e) {
			logger.warn("Could not drop FK {} on {}.{}: {}", constraintName, schema, tableName, e.getMessage());
		}
	}

	private void backfillSaleItemSnapshots(String schema) {
		try {

			jdbcTemplate.execute("UPDATE `" + schema + "`.`sale_items` si " +
					"INNER JOIN `" + schema + "`.`products` p ON si.product_id = p.product_id " +
					"SET si.product_name = p.product_name, si.internal_code = p.internal_code " +
					"WHERE si.product_name IS NULL AND si.product_id IS NOT NULL");


			jdbcTemplate.execute("UPDATE `" + schema + "`.`sale_items` si " +
					"INNER JOIN `" + schema + "`.`product_variants` pv ON si.product_variant_id = pv.product_variant_id " +
					"SET si.variant_sku = pv.variant_sku " +
					"WHERE si.variant_sku IS NULL AND si.product_variant_id IS NOT NULL");


			jdbcTemplate.execute("UPDATE `" + schema + "`.`sale_items` " +
					"SET product_name = 'Producto eliminado' " +
					"WHERE product_name IS NULL AND product_id IS NOT NULL");
		} catch (Exception e) {
			logger.warn("Could not backfill sale item snapshots for {}: {}", schema, e.getMessage());
		}
	}

	public void createTiendaNubeConfigTable(String schema) {
		jdbcTemplate.execute(
				"CREATE TABLE IF NOT EXISTS `" + schema + "`.`tiendanube_config` (" +
						"`id` BIGINT NOT NULL AUTO_INCREMENT, " +
						"`access_token` TEXT NOT NULL, " +
						"`token_type` VARCHAR(255), " +
						"`scope` TEXT, " +
						"`user_id` BIGINT, " +
						"PRIMARY KEY (`id`))");
		modifyColumn(schema, "tiendanube_config", "scope", "TEXT");
	}

	public void createInvoiceIndexTables(String schema) {
		jdbcTemplate.execute(
				"CREATE TABLE IF NOT EXISTS `" + schema + "`.`invoice_index` (" +
						"`id` BIGINT NOT NULL AUTO_INCREMENT, " +
						"`idempotency_key` CHAR(64) NOT NULL, " +
						"`cuit_emisor` CHAR(11) NOT NULL, " +
						"`pto_vta` SMALLINT NOT NULL, " +
						"`cbte_tipo` SMALLINT NOT NULL, " +
						"`cbte_nro` BIGINT NOT NULL, " +
						"`cbte_fch` DATE NOT NULL, " +
						"`concepto` TINYINT NOT NULL DEFAULT 1, " +
						"`doc_tipo` TINYINT NOT NULL DEFAULT 99, " +
						"`doc_nro_token` CHAR(64), " +
						"`doc_last4` CHAR(4), " +
						"`imp_total` DECIMAL(15,2) NOT NULL, " +
						"`imp_neto` DECIMAL(15,2) NOT NULL DEFAULT 0, " +
						"`imp_iva` DECIMAL(15,2) NOT NULL DEFAULT 0, " +
						"`imp_trib` DECIMAL(15,2) NOT NULL DEFAULT 0, " +
						"`imp_tot_conc` DECIMAL(15,2) NOT NULL DEFAULT 0, " +
						"`imp_op_ex` DECIMAL(15,2) NOT NULL DEFAULT 0, " +
						"`mon_id` CHAR(3) NOT NULL DEFAULT 'PES', " +
						"`mon_cotiz` DECIMAL(10,6) NOT NULL DEFAULT 1, " +
						"`cae` CHAR(14), " +
						"`cae_fch_vto` DATE, " +
						"`resultado` CHAR(1), " +
						"`sale_id` BIGINT, " +
						"`request_hash` CHAR(64), " +
						"`response_hash` CHAR(64), " +
						"`obs_codes` VARCHAR(200), " +
						"`obs_msg` VARCHAR(500), " +
						"`created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), " +
						"`created_by` VARCHAR(50), " +
						"PRIMARY KEY (`id`), " +
						"UNIQUE KEY `uk_fiscal_key` (`cuit_emisor`, `pto_vta`, `cbte_tipo`, `cbte_nro`), " +
						"UNIQUE KEY `uk_idempotency` (`idempotency_key`), " +
						"INDEX `idx_cbte_fch` (`cbte_fch`), " +
						"INDEX `idx_cae` (`cae`), " +
						"INDEX `idx_sale_id` (`sale_id`))");


		addColumnIfNotExists(schema, "invoice_index", "concepto", "TINYINT NOT NULL DEFAULT 1");
		addColumnIfNotExists(schema, "invoice_index", "doc_tipo", "TINYINT NOT NULL DEFAULT 99");
		addColumnIfNotExists(schema, "invoice_index", "mon_id", "CHAR(3) NOT NULL DEFAULT 'PES'");
		addColumnIfNotExists(schema, "invoice_index", "mon_cotiz", "DECIMAL(10,6) NOT NULL DEFAULT 1");
		modifyColumn(schema, "invoice_index", "cbte_fch", "DATE NOT NULL");
		modifyColumn(schema, "invoice_index", "cae_fch_vto", "DATE");
		modifyColumn(schema, "invoice_index", "cuit_emisor", "CHAR(11) NOT NULL");
		modifyColumn(schema, "invoice_index", "created_at", "DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)");

		jdbcTemplate.execute(
				"CREATE TABLE IF NOT EXISTS `" + schema + "`.`invoice_audit_log` (" +
						"`id` BIGINT NOT NULL AUTO_INCREMENT, " +
						"`event_type` VARCHAR(30) NOT NULL, " +
						"`invoice_index_id` BIGINT, " +
						"`fiscal_key` VARCHAR(50), " +
						"`actor` VARCHAR(100), " +
						"`detail` TEXT, " +
						"`request_hash` CHAR(64), " +
						"`response_hash` CHAR(64), " +
						"`prev_log_hash` CHAR(64), " +
						"`entry_hash` CHAR(64) NOT NULL, " +
						"`created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
						"PRIMARY KEY (`id`), " +
						"INDEX `idx_audit_invoice_id` (`invoice_index_id`), " +
						"INDEX `idx_audit_created_at` (`created_at`))");

		jdbcTemplate.execute(
				"CREATE TABLE IF NOT EXISTS `" + schema + "`.`invoice_snapshot_ref` (" +
						"`id` BIGINT NOT NULL AUTO_INCREMENT, " +
						"`invoice_index_id` BIGINT NOT NULL, " +
						"`storage_type` VARCHAR(20) NOT NULL, " +
						"`storage_path` VARCHAR(500), " +
						"`content_hash` CHAR(64), " +
						"`format` VARCHAR(10), " +
						"`size_bytes` BIGINT, " +
						"`expires_at` DATETIME, " +
						"`created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
						"PRIMARY KEY (`id`), " +
						"UNIQUE KEY `uk_snapshot_invoice` (`invoice_index_id`))");
	}

	public void createArcaConfigTable(String schema) {
		jdbcTemplate.execute(
				"CREATE TABLE IF NOT EXISTS `" + schema + "`.`arca_config` (" +
						"`id` BIGINT NOT NULL AUTO_INCREMENT, " +
						"`company_name` VARCHAR(255), " +
						"`cuit` VARCHAR(255), " +
						"`ta_ciphertext` TEXT, " +
						"`ta_iv` VARCHAR(24), " +
						"`ta_key_version` INT, " +
						"`token_expiration` DATETIME, " +
						"`current_step` INT DEFAULT 1, " +
						"`wizard_completed` BOOLEAN DEFAULT FALSE, " +
						"`environment` VARCHAR(50) DEFAULT 'HOMOLOGATION', " +
						"PRIMARY KEY (`id`))");


		addColumnIfNotExists(schema, "arca_config", "current_step", "INT DEFAULT 1");
		addColumnIfNotExists(schema, "arca_config", "wizard_completed", "BOOLEAN DEFAULT FALSE");
		addColumnIfNotExists(schema, "arca_config", "environment", "VARCHAR(50) DEFAULT 'HOMOLOGATION'");






		addColumnIfNotExists(schema, "arca_config", "token_expiration", "DATETIME");


		addColumnIfNotExists(schema, "arca_config", "ta_ciphertext", "TEXT");
		addColumnIfNotExists(schema, "arca_config", "ta_iv", "VARCHAR(24)");
		addColumnIfNotExists(schema, "arca_config", "ta_key_version", "INT");


		dropColumnIfExists(schema, "arca_config", "private_key");
		dropColumnIfExists(schema, "arca_config", "csr");
		dropColumnIfExists(schema, "arca_config", "certificate");
		dropColumnIfExists(schema, "arca_config", "auth_token");
		dropColumnIfExists(schema, "arca_config", "auth_sign");
	}

	public void createMercadoLibreConfigTable(String schema) {
		jdbcTemplate.execute(
				"CREATE TABLE IF NOT EXISTS `" + schema + "`.`mercadolibre_config` (" +
						"`id` BIGINT NOT NULL AUTO_INCREMENT, " +
						"`access_token` TEXT NOT NULL, " +
						"`refresh_token` TEXT, " +
						"`expires_at` DATETIME, " +
						"`token_type` VARCHAR(255), " +
						"`scope` TEXT, " +
						"`user_id` BIGINT, " +
						"PRIMARY KEY (`id`))");
	}

	public void createMercadoLibreMessagingTables(String schema) {

		jdbcTemplate.execute(
				"CREATE TABLE IF NOT EXISTS `" + schema + "`.`meli_conversations` (" +
						"`id` BIGINT NOT NULL, " +
						"`seller_id` BIGINT, " +
						"`buyer_id` BIGINT, " +
						"`status` VARCHAR(255), " +
						"`substatus` VARCHAR(255), " +
						"`blocked` BOOLEAN, " +
						"`last_message_date` DATETIME, " +
						"`unread_count` INT, " +
						"PRIMARY KEY (`id`))");


		jdbcTemplate.execute(
				"CREATE TABLE IF NOT EXISTS `" + schema + "`.`meli_messages` (" +
						"`id` VARCHAR(255) NOT NULL, " +
						"`conversation_id` BIGINT NOT NULL, " +
						"`from_user_id` BIGINT, " +
						"`to_user_id` BIGINT, " +
						"`text` TEXT, " +
						"`status` VARCHAR(255), " +
						"`date_created` DATETIME, " +
						"`date_read` DATETIME, " +
						"`read_by_me` BOOLEAN, " +
						"PRIMARY KEY (`id`), " +
						"CONSTRAINT `fk_meli_msg_conv` FOREIGN KEY (`conversation_id`) REFERENCES `" + schema
						+ "`.`meli_conversations` (`id`))");


		jdbcTemplate.execute(
				"CREATE TABLE IF NOT EXISTS `" + schema + "`.`meli_notification_log` (" +
						"`id` BIGINT NOT NULL AUTO_INCREMENT, " +
						"`topic` VARCHAR(255), " +
						"`resource` VARCHAR(255), " +
						"`user_id` BIGINT, " +
						"`received_at` DATETIME, " +
						"`processed_at` DATETIME, " +
						"`status` VARCHAR(255), " +
						"`error_message` TEXT, " +
						"`payload` TEXT, " +
						"PRIMARY KEY (`id`))");
	}

	public void createWhatsAppTables(String schema) {

		jdbcTemplate.execute(
				"CREATE TABLE IF NOT EXISTS `" + schema + "`.`wpp_connection` (" +
						"`id` BIGINT NOT NULL AUTO_INCREMENT, " +
						"`tenant_id` VARCHAR(255) NOT NULL, " +
						"`waba_id` VARCHAR(255) NOT NULL, " +
						"`phone_number_id` VARCHAR(255) NOT NULL, " +
						"`display_phone_number` VARCHAR(255), " +
						"`access_token_ciphertext` TEXT, " +
						"`access_token_iv` VARCHAR(24), " +
						"`access_token_key_version` INT, " +
						"`token_created_at` DATETIME, " +
						"`status` VARCHAR(20) NOT NULL DEFAULT 'DISCONNECTED', " +
						"`created_at` DATETIME NOT NULL, " +
						"`updated_at` DATETIME, " +
						"PRIMARY KEY (`id`), " +
						"INDEX `idx_tenant_id` (`tenant_id`), " +
						"INDEX `idx_tenant_status` (`tenant_id`, `status`))");


		jdbcTemplate.execute(
				"CREATE TABLE IF NOT EXISTS `" + schema + "`.`wpp_connect_sessions` (" +
						"`id` BIGINT NOT NULL AUTO_INCREMENT, " +
						"`sid` VARCHAR(36) NOT NULL, " +
						"`tenant_id` VARCHAR(255) NOT NULL, " +
						"`status` VARCHAR(20) NOT NULL DEFAULT 'PENDING', " +
						"`expires_at` DATETIME NOT NULL, " +
						"`created_at` DATETIME NOT NULL, " +
						"PRIMARY KEY (`id`), " +
						"UNIQUE KEY `uk_wpp_session_sid` (`sid`))");


		jdbcTemplate.execute(
				"CREATE TABLE IF NOT EXISTS `" + schema + "`.`wpp_conversations` (" +
						"`id` BIGINT NOT NULL AUTO_INCREMENT, " +
						"`contact_wa_id` VARCHAR(255) NOT NULL, " +
						"`contact_name` VARCHAR(255), " +
						"`last_message_at` DATETIME, " +
						"`unread_count` INT NOT NULL DEFAULT 0, " +
						"`created_at` DATETIME NOT NULL, " +
						"PRIMARY KEY (`id`))");


		jdbcTemplate.execute(
				"CREATE TABLE IF NOT EXISTS `" + schema + "`.`wpp_messages` (" +
						"`id` BIGINT NOT NULL AUTO_INCREMENT, " +
						"`wamid` VARCHAR(255) NOT NULL, " +
						"`conversation_id` BIGINT NOT NULL, " +
						"`direction` VARCHAR(3) NOT NULL, " +
						"`type` VARCHAR(30) NOT NULL, " +
						"`text_body` TEXT, " +
						"`media_id` VARCHAR(255), " +
						"`media_url_local` VARCHAR(512), " +
						"`media_mime_type` VARCHAR(100), " +
						"`timestamp` DATETIME NOT NULL, " +
						"`raw_payload` TEXT, " +
						"PRIMARY KEY (`id`), " +
						"CONSTRAINT `fk_wpp_msg_conv` FOREIGN KEY (`conversation_id`) REFERENCES `" + schema
						+ "`.`wpp_conversations` (`id`))");


		jdbcTemplate.execute(
				"CREATE TABLE IF NOT EXISTS `" + schema + "`.`wpp_statuses` (" +
						"`id` BIGINT NOT NULL AUTO_INCREMENT, " +
						"`wamid` VARCHAR(255) NOT NULL, " +
						"`status` VARCHAR(20) NOT NULL, " +
						"`timestamp` DATETIME NOT NULL, " +
						"`error_code` VARCHAR(50), " +
						"`error_message` TEXT, " +
						"PRIMARY KEY (`id`))");


		jdbcTemplate.execute(
				"CREATE TABLE IF NOT EXISTS `" + schema + "`.`wpp_webhook_log` (" +
						"`id` BIGINT NOT NULL AUTO_INCREMENT, " +
						"`event_hash` VARCHAR(64) NOT NULL, " +
						"`payload` TEXT, " +
						"`received_at` DATETIME NOT NULL, " +
						"`processed` BOOLEAN NOT NULL DEFAULT FALSE, " +
						"PRIMARY KEY (`id`), " +
						"UNIQUE KEY `uk_wpp_webhook_hash` (`event_hash`))");
	}

	private void createAppUsersTable(String schema) {
		jdbcTemplate.execute(
				"CREATE TABLE IF NOT EXISTS `" + schema + "`.`app_users` ("
						+ "`id` BIGINT NOT NULL AUTO_INCREMENT,"
						+ "`username` VARCHAR(255) NOT NULL,"
						+ "`name` VARCHAR(255) NOT NULL,"
						+ "`email` VARCHAR(255) NOT NULL,"
						+ "`password_hash` VARCHAR(255) NOT NULL,"
						+ "`role` VARCHAR(32) NOT NULL,"
						+ "`permissions` TEXT,"
						+ "`is_two_factor_enabled` BOOLEAN DEFAULT FALSE,"
						+ "`two_factor_code` VARCHAR(10),"
						+ "`two_factor_expires_at` DATETIME,"
						+ "PRIMARY KEY (`id`),"
						+ "UNIQUE KEY `uk_app_users_username` (`username`)"
						+ ")");
	}

	private void createStockTables(String schema) {

		jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `" + schema + "`.`measurement_units` ("
				+ "`unit_code` VARCHAR(10) NOT NULL,"
				+ "`unit_name` VARCHAR(255) NOT NULL,"
				+ "PRIMARY KEY (`unit_code`)"
				+ ")");


		jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `" + schema + "`.`product_brands` ("
				+ "`brand_id` BIGINT NOT NULL AUTO_INCREMENT,"
				+ "`brand_name` VARCHAR(255) NOT NULL,"
				+ "PRIMARY KEY (`brand_id`)"
				+ ")");


		jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `" + schema + "`.`product_categories` ("
				+ "`category_id` BIGINT NOT NULL AUTO_INCREMENT,"
				+ "`category_name` VARCHAR(255) NOT NULL,"
				+ "`parent_category_id` BIGINT,"
				+ "PRIMARY KEY (`category_id`),"
				+ "CONSTRAINT `fk_categories_parent` FOREIGN KEY (`parent_category_id`) REFERENCES `" + schema
				+ "`.`product_categories` (`category_id`)"
				+ ")");


		jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `" + schema + "`.`warehouses` ("
				+ "`warehouse_id` BIGINT NOT NULL AUTO_INCREMENT,"
				+ "`warehouse_code` VARCHAR(255),"
				+ "`warehouse_name` VARCHAR(255) NOT NULL,"
				+ "`address` VARCHAR(255),"
				+ "PRIMARY KEY (`warehouse_id`)"
				+ ")");


		jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `" + schema + "`.`storage_locations` ("
				+ "`storage_location_id` BIGINT NOT NULL AUTO_INCREMENT,"
				+ "`warehouse_id` BIGINT NOT NULL,"
				+ "`location_code` VARCHAR(255),"
				+ "`location_description` VARCHAR(255),"
				+ "`location_type` VARCHAR(255),"
				+ "PRIMARY KEY (`storage_location_id`),"
				+ "CONSTRAINT `fk_locations_warehouse` FOREIGN KEY (`warehouse_id`) REFERENCES `" + schema
				+ "`.`warehouses` (`warehouse_id`)"
				+ ")");


		jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `" + schema + "`.`products` ("
				+ "`product_id` BIGINT NOT NULL AUTO_INCREMENT,"
				+ "`internal_code` VARCHAR(255),"
				+ "`product_name` VARCHAR(255) NOT NULL,"
				+ "`measurement_unit_id` VARCHAR(10),"
				+ "`measurement_value` VARCHAR(255),"
				+ "`product_category_id` BIGINT,"
				+ "`product_brand_id` BIGINT,"
				+ "`warehouse_id` BIGINT,"
				+ "`storage_location_id` BIGINT,"
				+ "`price` DECIMAL(19,2),"
				+ "`cost` DECIMAL(19,2) DEFAULT 0,"
				+ "`stock_quantity` DECIMAL(19,2) DEFAULT 0,"
				+ "`min_stock` DECIMAL(19,2) DEFAULT 0,"
				+ "`is_active` BOOLEAN DEFAULT TRUE,"
				+ "PRIMARY KEY (`product_id`),"
				+ "CONSTRAINT `fk_products_unit` FOREIGN KEY (`measurement_unit_id`) REFERENCES `" + schema
				+ "`.`measurement_units` (`unit_code`),"
				+ "CONSTRAINT `fk_products_category` FOREIGN KEY (`product_category_id`) REFERENCES `" + schema
				+ "`.`product_categories` (`category_id`),"
				+ "CONSTRAINT `fk_products_brand` FOREIGN KEY (`product_brand_id`) REFERENCES `" + schema
				+ "`.`product_brands` (`brand_id`),"
				+ "CONSTRAINT `fk_products_warehouse` FOREIGN KEY (`warehouse_id`) REFERENCES `" + schema
				+ "`.`warehouses` (`warehouse_id`),"
				+ "CONSTRAINT `fk_products_storage` FOREIGN KEY (`storage_location_id`) REFERENCES `" + schema
				+ "`.`storage_locations` (`storage_location_id`)"
				+ ")");


		jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `" + schema + "`.`product_variants` ("
				+ "`product_variant_id` BIGINT NOT NULL AUTO_INCREMENT,"
				+ "`product_id` BIGINT NOT NULL,"
				+ "`variant_sku` VARCHAR(255),"
				+ "`variant_gtin` VARCHAR(255),"
				+ "`net_weight_grams` DECIMAL(19,4),"
				+ "`size_dimensions_cm` VARCHAR(255),"
				+ "`price` DECIMAL(19,2),"
				+ "`cost` DECIMAL(19,2),"
				+ "`is_active` BOOLEAN DEFAULT TRUE,"
				+ "PRIMARY KEY (`product_variant_id`),"
				+ "CONSTRAINT `fk_variants_product` FOREIGN KEY (`product_id`) REFERENCES `" + schema
				+ "`.`products` (`product_id`)"
				+ ")");


		jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `" + schema + "`.`product_variant_attributes` ("
				+ "`attribute_id` BIGINT NOT NULL AUTO_INCREMENT,"
				+ "`product_variant_id` BIGINT NOT NULL,"
				+ "`attribute_key` VARCHAR(255) NOT NULL,"
				+ "`attribute_value` VARCHAR(255) NOT NULL,"
				+ "PRIMARY KEY (`attribute_id`),"
				+ "CONSTRAINT `fk_attributes_variant` FOREIGN KEY (`product_variant_id`) REFERENCES `" + schema
				+ "`.`product_variants` (`product_variant_id`)"
				+ ")");






		jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `" + schema + "`.`stock_balance` ("
				+ "`product_variant_id` BIGINT NOT NULL,"
				+ "`storage_location_id` BIGINT NOT NULL,"
				+ "`quantity_on_hand` DECIMAL(19,4),"
				+ "`safety_stock_quantity` DECIMAL(19,4),"
				+ "`reorder_point_quantity` DECIMAL(19,4),"
				+ "`last_movement_at` DATETIME,"
				+ "PRIMARY KEY (`product_variant_id`, `storage_location_id`),"
				+ "CONSTRAINT `fk_balance_variant` FOREIGN KEY (`product_variant_id`) REFERENCES `" + schema
				+ "`.`product_variants` (`product_variant_id`),"
				+ "CONSTRAINT `fk_balance_location` FOREIGN KEY (`storage_location_id`) REFERENCES `" + schema
				+ "`.`storage_locations` (`storage_location_id`)"
				+ ")");


		jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `" + schema + "`.`stock_movements` ("
				+ "`stock_movement_id` BIGINT NOT NULL AUTO_INCREMENT,"
				+ "`occurred_at` DATETIME NOT NULL,"
				+ "`movement_type` VARCHAR(50) NOT NULL,"
				+ "`product_variant_id` BIGINT NOT NULL,"
				+ "`quantity_units` DECIMAL(19,4) NOT NULL,"
				+ "`from_storage_location_id` BIGINT,"
				+ "`to_storage_location_id` BIGINT,"
				+ "`reference_source` VARCHAR(255),"
				+ "`reference_id` VARCHAR(255),"
				+ "`note` TEXT,"
				+ "PRIMARY KEY (`stock_movement_id`),"
				+ "CONSTRAINT `fk_movements_variant` FOREIGN KEY (`product_variant_id`) REFERENCES `" + schema
				+ "`.`product_variants` (`product_variant_id`),"
				+ "CONSTRAINT `fk_movements_from_loc` FOREIGN KEY (`from_storage_location_id`) REFERENCES `" + schema
				+ "`.`storage_locations` (`storage_location_id`),"
				+ "CONSTRAINT `fk_movements_to_loc` FOREIGN KEY (`to_storage_location_id`) REFERENCES `" + schema
				+ "`.`storage_locations` (`storage_location_id`)"
				+ ")");


		jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `" + schema + "`.`physical_inventories` ("
				+ "`physical_inventory_id` BIGINT NOT NULL AUTO_INCREMENT,"
				+ "`warehouse_id` BIGINT NOT NULL,"
				+ "`started_at` DATETIME,"
				+ "`posted_at` DATETIME,"
				+ "`status` VARCHAR(50),"
				+ "PRIMARY KEY (`physical_inventory_id`),"
				+ "CONSTRAINT `fk_inventory_warehouse` FOREIGN KEY (`warehouse_id`) REFERENCES `" + schema
				+ "`.`warehouses` (`warehouse_id`)"
				+ ")");


		jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `" + schema + "`.`physical_inventory_items` ("
				+ "`physical_inventory_item_id` BIGINT NOT NULL AUTO_INCREMENT,"
				+ "`physical_inventory_id` BIGINT NOT NULL,"
				+ "`product_variant_id` BIGINT NOT NULL,"
				+ "`storage_location_id` BIGINT,"
				+ "`counted_quantity` DECIMAL(19,4),"
				+ "PRIMARY KEY (`physical_inventory_item_id`),"
				+ "CONSTRAINT `fk_inv_items_inventory` FOREIGN KEY (`physical_inventory_id`) REFERENCES `" + schema
				+ "`.`physical_inventories` (`physical_inventory_id`),"
				+ "CONSTRAINT `fk_inv_items_variant` FOREIGN KEY (`product_variant_id`) REFERENCES `" + schema
				+ "`.`product_variants` (`product_variant_id`),"
				+ "CONSTRAINT `fk_inv_items_location` FOREIGN KEY (`storage_location_id`) REFERENCES `" + schema
				+ "`.`storage_locations` (`storage_location_id`)"
				+ ")");
	}

	private void createSalesTables(String schema) {

		jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `" + schema + "`.`sales` ("
				+ "`sale_id` BIGINT NOT NULL AUTO_INCREMENT,"
				+ "`sale_date` DATETIME NOT NULL,"
				+ "`total_amount` DECIMAL(19,2) NOT NULL,"
				+ "`facturado` BOOLEAN DEFAULT FALSE,"
				+ "`created_by` VARCHAR(255),"
				+ "PRIMARY KEY (`sale_id`)"
				+ ")");


		jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `" + schema + "`.`sale_items` ("
				+ "`sale_item_id` BIGINT NOT NULL AUTO_INCREMENT,"
				+ "`sale_id` BIGINT NOT NULL,"
				+ "`product_id` BIGINT,"
				+ "`product_variant_id` BIGINT,"
				+ "`product_name` VARCHAR(255),"
				+ "`variant_sku` VARCHAR(255),"
				+ "`internal_code` VARCHAR(255),"
				+ "`quantity` INT NOT NULL,"
				+ "`unit_price` DECIMAL(19,2) NOT NULL,"
				+ "`subtotal` DECIMAL(19,2) NOT NULL,"
				+ "PRIMARY KEY (`sale_item_id`),"
				+ "CONSTRAINT `fk_sale_items_sale` FOREIGN KEY (`sale_id`) REFERENCES `" + schema
				+ "`.`sales` (`sale_id`)"
				+ ")");
	}

	private void createSecurityTables(String schema) {

		jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `" + schema + "`.`access_logs` ("
				+ "`id` BIGINT NOT NULL AUTO_INCREMENT,"
				+ "`username` VARCHAR(255) NOT NULL,"
				+ "`timestamp` DATETIME NOT NULL,"
				+ "`ip_address` VARCHAR(255),"
				+ "`device` VARCHAR(255),"
				+ "`location` VARCHAR(255),"
				+ "`medium` VARCHAR(255),"
				+ "PRIMARY KEY (`id`)"
				+ ")");


		jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `" + schema + "`.`active_sessions` ("
				+ "`id` BIGINT NOT NULL AUTO_INCREMENT,"
				+ "`username` VARCHAR(255) NOT NULL,"
				+ "`token` TEXT NOT NULL,"
				+ "`created` DATETIME NOT NULL,"
				+ "`last_active` DATETIME,"
				+ "`ip_address` VARCHAR(255),"
				+ "`device` VARCHAR(255),"
				+ "`location` VARCHAR(255),"
				+ "`medium` VARCHAR(255),"
				+ "`session_identifier` VARCHAR(36),"
				+ "`is_current` BOOLEAN DEFAULT FALSE,"
				+ "PRIMARY KEY (`id`)"
				+ ")");
	}

	private void createNotificationsTable(String schema) {
		jdbcTemplate.execute(
				"CREATE TABLE IF NOT EXISTS `" + schema + "`.`notifications` (" +
						"`id` BIGINT NOT NULL AUTO_INCREMENT," +
						"`message` TEXT NOT NULL," +
						"`url` VARCHAR(512) NOT NULL," +
						"`created_at` DATETIME NOT NULL," +
						"PRIMARY KEY (`id`)" +
						")");
	}

	private void createOfferTables(String schema) {

		jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `" + schema + "`.`offers` ("
				+ "`offer_id` BIGINT NOT NULL AUTO_INCREMENT,"
				+ "`name` VARCHAR(255) NOT NULL,"
				+ "`start_date` DATE,"
				+ "`end_date` DATE,"
				+ "`indefinite` BOOLEAN,"
				+ "`target_type` VARCHAR(50),"
				+ "`discount_value` DECIMAL(19,2),"
				+ "`discount_type` VARCHAR(50),"
				+ "`published_tienda_nube` BOOLEAN DEFAULT FALSE,"
				+ "`published_mercado_libre` BOOLEAN DEFAULT FALSE,"
				+ "PRIMARY KEY (`offer_id`)"
				+ ")");


		jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `" + schema + "`.`offer_target_ids` ("
				+ "`offer_id` BIGINT NOT NULL,"
				+ "`target_id` BIGINT,"
				+ "KEY `idx_offer_target` (`offer_id`),"
				+ "CONSTRAINT `fk_target_ids_offer` FOREIGN KEY (`offer_id`) REFERENCES `" + schema
				+ "`.`offers` (`offer_id`)"
				+ ")");
	}

	public void createLocalOrderTrackingTable(String schema) {
		jdbcTemplate.execute(
				"CREATE TABLE IF NOT EXISTS `" + schema + "`.`local_order_tracking` (" +
						"`id` BIGINT NOT NULL AUTO_INCREMENT, " +
						"`external_id` VARCHAR(255) NOT NULL, " +
						"`source` VARCHAR(255) NOT NULL, " +
						"`local_status` VARCHAR(255) NOT NULL, " +
						"`updated_at` DATETIME, " +
						"PRIMARY KEY (`id`)" +
						")");
	}

	private void createMercadoPagoTables(String schema) {
		jdbcTemplate.execute(
				"CREATE TABLE IF NOT EXISTS `" + schema + "`.`mp_webhook_event` (" +
						"`id` BIGINT NOT NULL AUTO_INCREMENT, " +
						"`received_at` DATETIME NOT NULL, " +
						"`type` VARCHAR(100) NOT NULL, " +
						"`action` VARCHAR(100) NOT NULL, " +
						"`data_id` VARCHAR(100) NOT NULL, " +
						"`event_id` VARCHAR(255), " +
						"`x_request_id` VARCHAR(255), " +
						"`ts` VARCHAR(100) NOT NULL, " +
						"`v1` VARCHAR(255) NOT NULL, " +
						"`raw_body` TEXT NOT NULL, " +
						"`status` VARCHAR(20) NOT NULL DEFAULT 'RECEIVED', " +
						"`attempts` INT NOT NULL DEFAULT 0, " +
						"`last_error` TEXT, " +
						"`processed_at` DATETIME, " +
						"PRIMARY KEY (`id`), " +
						"UNIQUE KEY `uk_mp_wh_event` (`type`, `action`, `data_id`, `ts`))" );

		jdbcTemplate.execute(
				"CREATE TABLE IF NOT EXISTS `" + schema + "`.`mp_subscription` (" +
					"`id` BIGINT NOT NULL AUTO_INCREMENT, " +
					"`subscription_id` VARCHAR(255) NOT NULL, " +
					"`status` VARCHAR(255) NOT NULL, " +
					"`amount` DECIMAL(19,4), " +
					"`period` VARCHAR(255), " +
					"`payer_email` VARCHAR(255), " +
					"`tenant_id` VARCHAR(255) NOT NULL, " +
					"`plan_tier` VARCHAR(30), " +
					"`raw_json` TEXT, " +
					"`updated_at` DATETIME, " +
					"PRIMARY KEY (`id`), " +
					"UNIQUE KEY `uk_mp_sub_id` (`subscription_id`))" );
		addColumnIfNotExists(schema, "mp_subscription", "plan_tier", "VARCHAR(30)");

		jdbcTemplate.execute(
				"CREATE TABLE IF NOT EXISTS `" + schema + "`.`mp_payment` (" +
						"`id` BIGINT NOT NULL AUTO_INCREMENT, " +
						"`payment_id` VARCHAR(255) NOT NULL, " +
						"`status` VARCHAR(255) NOT NULL, " +
						"`status_detail` VARCHAR(255), " +
						"`transaction_amount` DECIMAL(19,4), " +
						"`currency_id` VARCHAR(10), " +
						"`approved_at` DATETIME, " +
						"`payer_email` VARCHAR(255), " +
						"`external_reference` VARCHAR(255), " +
						"`tenant_id` VARCHAR(255) NOT NULL, " +
						"`updated_at` DATETIME, " +
						"`raw_json` TEXT, " +
						"PRIMARY KEY (`id`), " +
						"UNIQUE KEY `uk_mp_pay_id` (`payment_id`))" );

		jdbcTemplate.execute(
				"CREATE TABLE IF NOT EXISTS `" + schema + "`.`mp_claim` (" +
						"`id` BIGINT NOT NULL AUTO_INCREMENT, " +
						"`dispute_id` VARCHAR(255) NOT NULL, " +
						"`payment_id` VARCHAR(255) NOT NULL, " +
						"`status` VARCHAR(255) NOT NULL, " +
						"`reason` VARCHAR(255), " +
						"`stage` VARCHAR(255), " +
						"`tenant_id` VARCHAR(255) NOT NULL, " +
						"`raw_json` TEXT, " +
						"`updated_at` DATETIME, " +
						"PRIMARY KEY (`id`), " +
						"UNIQUE KEY `uk_mp_claim_id` (`dispute_id`))" );

		jdbcTemplate.execute(
				"CREATE TABLE IF NOT EXISTS `" + schema + "`.`mp_chargeback` (" +
						"`id` BIGINT NOT NULL AUTO_INCREMENT, " +
						"`dispute_id` VARCHAR(255) NOT NULL, " +
						"`payment_id` VARCHAR(255) NOT NULL, " +
						"`status` VARCHAR(255) NOT NULL, " +
						"`reason` VARCHAR(255), " +
						"`tenant_id` VARCHAR(255) NOT NULL, " +
						"`raw_json` TEXT, " +
						"`updated_at` DATETIME, " +
						"PRIMARY KEY (`id`), " +
						"UNIQUE KEY `uk_mp_chargeback_id` (`dispute_id`))" );

		jdbcTemplate.execute(
				"CREATE TABLE IF NOT EXISTS `" + schema + "`.`mp_exchange_rate` (" +
						"`id` BIGINT NOT NULL AUTO_INCREMENT, " +
						"`rate` DECIMAL(19,4) NOT NULL, " +
						"`last_plan_update_rate` DECIMAL(19,4), " +
						"`fetched_at` DATETIME NOT NULL, " +
						"`source` VARCHAR(255) NOT NULL, " +
						"PRIMARY KEY (`id`))" );

		jdbcTemplate.execute(
				"CREATE TABLE IF NOT EXISTS `" + schema + "`.`mp_plan_config` (" +
						"`id` BIGINT NOT NULL AUTO_INCREMENT, " +
						"`plan_key` VARCHAR(50) NOT NULL, " +
						"`mp_plan_id` VARCHAR(255) NOT NULL, " +
						"`price_usd` DECIMAL(10,2) NOT NULL, " +
						"`price_ars` DECIMAL(19,2), " +
						"`frequency` INT NOT NULL, " +
						"`frequency_type` VARCHAR(20) NOT NULL, " +
						"`label` VARCHAR(100), " +
						"`created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
						"`updated_at` DATETIME, " +
						"PRIMARY KEY (`id`), " +
						"UNIQUE KEY `uk_mp_plan_key` (`plan_key`))" );
	}

	private String sanitizeSchemaName(String tenantSchema) {
		String normalized = tenantSchema == null ? "" : tenantSchema.trim().toLowerCase(Locale.ROOT);
		if (normalized.isEmpty()) {
			throw new IllegalArgumentException("tenantSchema is required");
		}
		if (!normalized.matches("[a-z0-9_]+")) {
			throw new IllegalArgumentException("tenantSchema contains invalid characters");
		}
		return normalized;
	}

	private void createCajaTables(String schema) {

		jdbcTemplate.execute(
				"CREATE TABLE IF NOT EXISTS `" + schema + "`.`caja_egreso_categoria` ("
						+ "`categoria_id` BIGINT NOT NULL AUTO_INCREMENT,"
						+ "`nombre` VARCHAR(100) NOT NULL,"
						+ "`descripcion` VARCHAR(255),"
						+ "`activa` BOOLEAN NOT NULL DEFAULT TRUE,"
						+ "PRIMARY KEY (`categoria_id`)"
						+ ")");


		String[] defaultCats = {
			"Proveedores", "Viáticos", "Limpieza", "Mantenimiento", "Caja chica",
			"Adelanto / Sueldo", "Retiro de dueño", "Impuestos", "Servicios", "Otros"
		};
		for (String cat : defaultCats) {
			try {
				jdbcTemplate.execute(
						"INSERT IGNORE INTO `" + schema + "`.`caja_egreso_categoria` (`nombre`) VALUES ('" + cat.replace("'", "''") + "')");
			} catch (Exception e) {
				logger.debug("Skipping duplicate caja category: {}", cat);
			}
		}


		jdbcTemplate.execute(
				"CREATE TABLE IF NOT EXISTS `" + schema + "`.`caja_diaria` ("
						+ "`caja_diaria_id` BIGINT NOT NULL AUTO_INCREMENT,"
						+ "`warehouse_id` BIGINT NOT NULL,"
						+ "`fecha_operativa` DATE NOT NULL,"
						+ "`estado` VARCHAR(30) NOT NULL DEFAULT 'ABIERTA',"
						+ "`usuario_apertura` VARCHAR(255) NOT NULL,"
						+ "`usuario_responsable` VARCHAR(255) NOT NULL,"
						+ "`usuario_cierre` VARCHAR(255),"
						+ "`fecha_hora_apertura` DATETIME(3) NOT NULL,"
						+ "`fecha_hora_cierre` DATETIME(3),"
						+ "`monto_apertura_efectivo` DECIMAL(19,2) NOT NULL,"
						+ "`monto_esperado_efectivo` DECIMAL(19,2),"
						+ "`monto_contado_efectivo` DECIMAL(19,2),"
						+ "`monto_diferencia` DECIMAL(19,2),"
						+ "`observacion_apertura` TEXT,"
						+ "`observacion_cierre` TEXT,"
						+ "`requiere_revision` BOOLEAN NOT NULL DEFAULT FALSE,"
						+ "`version` BIGINT NOT NULL DEFAULT 0,"
						+ "PRIMARY KEY (`caja_diaria_id`),"
						+ "INDEX `idx_caja_warehouse_fecha` (`warehouse_id`, `fecha_operativa`),"
						+ "INDEX `idx_caja_estado` (`estado`)"
						+ ")");


		jdbcTemplate.execute(
				"CREATE TABLE IF NOT EXISTS `" + schema + "`.`caja_movimiento` ("
						+ "`movimiento_id` BIGINT NOT NULL AUTO_INCREMENT,"
						+ "`caja_diaria_id` BIGINT NOT NULL,"
						+ "`tipo` VARCHAR(40) NOT NULL,"
						+ "`categoria_egreso_id` BIGINT,"
						+ "`medio_pago` VARCHAR(30) NOT NULL,"
						+ "`monto` DECIMAL(19,2) NOT NULL,"
						+ "`descripcion` TEXT,"
						+ "`justificacion` TEXT,"
						+ "`comprobante_texto` TEXT,"
						+ "`comprobante_numero` VARCHAR(100),"
						+ "`comprobante_archivo_url` VARCHAR(500),"
						+ "`referencia_tipo` VARCHAR(50),"
						+ "`referencia_id` VARCHAR(100),"
						+ "`usuario_creador` VARCHAR(255) NOT NULL,"
						+ "`usuario_anulador` VARCHAR(255),"
						+ "`fecha_hora_creacion` DATETIME(3) NOT NULL,"
						+ "`fecha_hora_anulacion` DATETIME(3),"
						+ "`motivo_anulacion` TEXT,"
						+ "`estado` VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',"
						+ "`movimiento_origen_id` BIGINT,"
						+ "`entidad_pago` VARCHAR(100),"
						+ "`referencia_externa_pago` VARCHAR(255),"
						+ "`observaciones` TEXT,"
						+ "`idempotency_key` VARCHAR(100),"
						+ "PRIMARY KEY (`movimiento_id`),"
						+ "CONSTRAINT `fk_caja_mov_caja` FOREIGN KEY (`caja_diaria_id`) REFERENCES `" + schema + "`.`caja_diaria` (`caja_diaria_id`),"
						+ "UNIQUE KEY `uk_caja_mov_idempotency` (`caja_diaria_id`, `idempotency_key`),"
						+ "INDEX `idx_caja_mov_tipo` (`tipo`),"
						+ "INDEX `idx_caja_mov_estado` (`estado`)"
						+ ")");


		jdbcTemplate.execute(
				"CREATE TABLE IF NOT EXISTS `" + schema + "`.`caja_relevo` ("
						+ "`relevo_id` BIGINT NOT NULL AUTO_INCREMENT,"
						+ "`caja_diaria_id` BIGINT NOT NULL,"
						+ "`usuario_saliente` VARCHAR(255) NOT NULL,"
						+ "`usuario_entrante` VARCHAR(255) NOT NULL,"
						+ "`fecha_hora_solicitud` DATETIME(3) NOT NULL,"
						+ "`fecha_hora_confirmacion` DATETIME(3),"
						+ "`estado` VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',"
						+ "`observacion` TEXT,"
						+ "`usuario_forzador` VARCHAR(255),"
						+ "PRIMARY KEY (`relevo_id`),"
						+ "CONSTRAINT `fk_caja_relevo_caja` FOREIGN KEY (`caja_diaria_id`) REFERENCES `" + schema + "`.`caja_diaria` (`caja_diaria_id`)"
						+ ")");


		jdbcTemplate.execute(
				"CREATE TABLE IF NOT EXISTS `" + schema + "`.`caja_audit_log` ("
						+ "`audit_id` BIGINT NOT NULL AUTO_INCREMENT,"
						+ "`caja_diaria_id` BIGINT NOT NULL,"
						+ "`entidad` VARCHAR(50) NOT NULL,"
						+ "`entidad_id` BIGINT,"
						+ "`accion` VARCHAR(50) NOT NULL,"
						+ "`usuario` VARCHAR(255) NOT NULL,"
						+ "`fecha_hora` DATETIME(3) NOT NULL,"
						+ "`payload_anterior_json` TEXT,"
						+ "`payload_nuevo_json` TEXT,"
						+ "`descripcion` TEXT,"
						+ "PRIMARY KEY (`audit_id`),"
						+ "CONSTRAINT `fk_caja_audit_caja` FOREIGN KEY (`caja_diaria_id`) REFERENCES `" + schema + "`.`caja_diaria` (`caja_diaria_id`),"
						+ "INDEX `idx_caja_audit_fecha` (`fecha_hora`)"
						+ ")");
	}

	private void createOfferAuditLogTable(String schema) {
		jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `" + schema + "`.`offer_audit_log` ("
				+ "`id` BIGINT NOT NULL AUTO_INCREMENT,"
				+ "`offer_id` BIGINT NOT NULL,"
				+ "`channel` VARCHAR(10),"
				+ "`action` VARCHAR(20) NOT NULL,"
				+ "`request_data` TEXT,"
				+ "`response_data` TEXT,"
				+ "`created_at` DATETIME NOT NULL,"
				+ "PRIMARY KEY (`id`),"
				+ "KEY `idx_audit_offer` (`offer_id`)"
				+ ")");
	}

	private void createEffectivePricesTable(String schema) {
		jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `" + schema + "`.`effective_prices` ("
				+ "`id` BIGINT NOT NULL AUTO_INCREMENT,"
				+ "`local_product_id` BIGINT NOT NULL,"
				+ "`channel` VARCHAR(10) NOT NULL,"
				+ "`effective_price` DECIMAL(19,2) NOT NULL,"
				+ "`regular_price` DECIMAL(19,2) NOT NULL,"
				+ "`computed_at` DATETIME NOT NULL,"
				+ "`source_offer_id` BIGINT,"
				+ "PRIMARY KEY (`id`),"
				+ "UNIQUE KEY `uk_effective_product_channel` (`local_product_id`, `channel`)"
				+ ")");
	}

	private void createCartPromoRulesTable(String schema) {
		jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `" + schema + "`.`cart_promo_rules` ("
				+ "`id` BIGINT NOT NULL AUTO_INCREMENT,"
				+ "`offer_id` BIGINT NOT NULL,"
				+ "`tn_store_id` BIGINT NOT NULL,"
				+ "`tn_promotion_id` VARCHAR(255),"
				+ "`rule_type` VARCHAR(20) NOT NULL,"
				+ "`buy_quantity` INT,"
				+ "`pay_quantity` INT,"
				+ "`target_variant_ids` TEXT,"
				+ "`target_category_ids` TEXT,"
				+ "`is_active` BOOLEAN DEFAULT TRUE,"
				+ "`created_at` DATETIME NOT NULL,"
				+ "`updated_at` DATETIME,"
				+ "PRIMARY KEY (`id`),"
				+ "KEY `idx_cart_promo_store` (`tn_store_id`)"
				+ ")");
	}
}
