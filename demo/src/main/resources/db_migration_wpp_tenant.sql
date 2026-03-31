-- SQL Script para agregar soporte multi-tenant a WppConnection
-- Este script añade la columna tenant_id a la tabla wpp_connection
-- 
-- NOTA: Si usas ddl-auto=update en Hibernate, esta migración se ejecutará automáticamente
-- al iniciar la aplicación. Solo usa este script si prefieres hacer la migración manualmente.

-- 1. Agregar la columna tenant_id (si es necesario)
ALTER TABLE wpp_connection 
ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(255) NOT NULL DEFAULT 'default';

-- 2. Agregar índice para mejorar queries por tenant
ALTER TABLE wpp_connection 
ADD INDEX idx_tenant_id (tenant_id);

-- 3. Agregar índice compuesto para búsquedas comunes
ALTER TABLE wpp_connection 
ADD INDEX idx_tenant_status (tenant_id, status);

-- 4. (OPCIONAL) Si necesitas asociar conexiones existentes a un tenant específico:
-- UPDATE wpp_connection SET tenant_id = 'your_tenant_id' WHERE tenant_id = 'default';

-- 5. (OPCIONAL) Hacer la columna NOT NULL forzadamente después de la migración
-- ALTER TABLE wpp_connection MODIFY COLUMN tenant_id VARCHAR(255) NOT NULL;

-- Verificar la estructura de la tabla
-- DESCRIBE wpp_connection;
-- SELECT * FROM wpp_connection;
