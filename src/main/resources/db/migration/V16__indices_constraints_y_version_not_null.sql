-- ============================================================
-- Índices faltantes (confirmados contra las queries reales de
-- cada RepositorioX; ver el plan de la Fase 10 para el detalle).
-- ============================================================
CREATE INDEX idx_outbox_agregado_tipo ON outbox_eventos (agregado_id, tipo);
CREATE INDEX idx_orders_tenant_created_at ON ordenes (tenant_id, created_at DESC);
CREATE INDEX idx_categories_tenant_parent ON categorias (tenant_id, id_padre);
CREATE INDEX idx_products_tenant_category ON productos (tenant_id, category_id);

-- ============================================================
-- Índices duplicados/sin uso — se eliminan.
-- ============================================================
-- idx_users_email es redundante: usuarios.correo ya es UNIQUE (V2), y Postgres
-- crea automáticamente un índice único para toda restricción UNIQUE.
DROP INDEX IF EXISTS idx_users_email;
-- idx_tenants_slug es redundante: inquilinos.enlace_corto ya es UNIQUE (V3).
DROP INDEX IF EXISTS idx_tenants_slug;
-- idx_cupones_tenant es redundante: ya es el prefijo izquierdo del índice único
-- de UNIQUE(tenant_id, codigo) (V9) — cualquier query que filtre solo por
-- tenant_id ya puede usar ese índice compuesto.
DROP INDEX IF EXISTS idx_cupones_tenant;
-- idx_cupones_codigo no tiene ningún caller que filtre solo por "codigo" sin
-- tenant_id (RepositorioCupon siempre filtra por idTienda + codigo juntos).
DROP INDEX IF EXISTS idx_cupones_codigo;

-- ============================================================
-- CHECK constraints — defensa en profundidad contra un UPDATE directo que
-- se salte Producto.decreaseInventory / Dinero (la app ya lo garantiza; esto
-- cubre el caso de una migración de datos o un acceso fuera de JPA).
--
-- NOTA sobre nombres de columna: el SQL original del brief fue escrito contra
-- los nombres de V5/V6/V4 tal como se crearon originalmente (subtotal_amount,
-- total_amount, unit_price_amount, amount, currency). Pero V12/V13/V14
-- renombraron esas mismas columnas en ordenes/order_items/pagos/productos/
-- product_variants a sus nombres en español (monto_subtotal, monto_total,
-- monto_precio_unitario, monto, moneda) para alinearlas con las entidades JPA
-- (confirmado leyendo Orden.java, ArticuloOrden.java, Pago.java, Producto.java
-- y VarianteProducto.java, que mapean @Column(name = "monto"/"monto_subtotal"/
-- etc.) — los nombres de abajo ya están corregidos a los nombres reales
-- post-V14, no a los del brief original.
-- ============================================================
ALTER TABLE ordenes
    ADD CONSTRAINT chk_ordenes_montos_no_negativos CHECK (
        monto_subtotal >= 0 AND monto_impuesto >= 0 AND
        monto_envio >= 0 AND monto_total >= 0
    );

ALTER TABLE order_items
    ADD CONSTRAINT chk_order_items_montos_no_negativos CHECK (
        monto_precio_unitario >= 0 AND monto_subtotal >= 0
    );

ALTER TABLE pagos
    ADD CONSTRAINT chk_pagos_monto_no_negativo CHECK (monto >= 0);

ALTER TABLE productos
    ADD CONSTRAINT chk_productos_monto_no_negativo CHECK (monto >= 0),
    ADD CONSTRAINT chk_productos_inventario_no_negativo CHECK (inventario >= 0);

ALTER TABLE product_variants
    ADD CONSTRAINT chk_product_variants_monto_no_negativo CHECK (monto IS NULL OR monto >= 0),
    ADD CONSTRAINT chk_product_variants_inventario_no_negativo CHECK (inventario >= 0);

ALTER TABLE cupones
    ADD CONSTRAINT chk_cupones_valor_no_negativo CHECK (valor >= 0);

-- ============================================================
-- version NOT NULL DEFAULT 0 — coherente con @Version, que Hibernate siempre
-- puebla en el insert; esto solo cierra la ventana de un insert fuera de JPA.
-- Se hace en dos pasos (backfill + NOT NULL) por si alguna fila existente ya
-- tuviera NULL en un entorno con datos reales.
-- ============================================================
UPDATE usuarios SET version = 0 WHERE version IS NULL;
UPDATE refresh_tokens SET version = 0 WHERE version IS NULL;
UPDATE inquilinos SET version = 0 WHERE version IS NULL;
UPDATE subscription_plans SET version = 0 WHERE version IS NULL;
UPDATE subscriptions SET version = 0 WHERE version IS NULL;
UPDATE direcciones SET version = 0 WHERE version IS NULL;
UPDATE ordenes SET version = 0 WHERE version IS NULL;
UPDATE order_items SET version = 0 WHERE version IS NULL;
UPDATE order_status_history SET version = 0 WHERE version IS NULL;
UPDATE categorias SET version = 0 WHERE version IS NULL;
UPDATE productos SET version = 0 WHERE version IS NULL;
UPDATE product_variants SET version = 0 WHERE version IS NULL;
UPDATE product_images SET version = 0 WHERE version IS NULL;
UPDATE pagos SET version = 0 WHERE version IS NULL;
UPDATE refunds SET version = 0 WHERE version IS NULL;
UPDATE resenas SET version = 0 WHERE version IS NULL;
UPDATE notifications SET version = 0 WHERE version IS NULL;
UPDATE cupones SET version = 0 WHERE version IS NULL;
UPDATE envios SET version = 0 WHERE version IS NULL;
UPDATE evento_tracking SET version = 0 WHERE version IS NULL;
UPDATE outbox_eventos SET version = 0 WHERE version IS NULL;

ALTER TABLE usuarios ALTER COLUMN version SET NOT NULL, ALTER COLUMN version SET DEFAULT 0;
ALTER TABLE refresh_tokens ALTER COLUMN version SET NOT NULL, ALTER COLUMN version SET DEFAULT 0;
ALTER TABLE inquilinos ALTER COLUMN version SET NOT NULL, ALTER COLUMN version SET DEFAULT 0;
ALTER TABLE subscription_plans ALTER COLUMN version SET NOT NULL, ALTER COLUMN version SET DEFAULT 0;
ALTER TABLE subscriptions ALTER COLUMN version SET NOT NULL, ALTER COLUMN version SET DEFAULT 0;
ALTER TABLE direcciones ALTER COLUMN version SET NOT NULL, ALTER COLUMN version SET DEFAULT 0;
ALTER TABLE ordenes ALTER COLUMN version SET NOT NULL, ALTER COLUMN version SET DEFAULT 0;
ALTER TABLE order_items ALTER COLUMN version SET NOT NULL, ALTER COLUMN version SET DEFAULT 0;
ALTER TABLE order_status_history ALTER COLUMN version SET NOT NULL, ALTER COLUMN version SET DEFAULT 0;
ALTER TABLE categorias ALTER COLUMN version SET NOT NULL, ALTER COLUMN version SET DEFAULT 0;
ALTER TABLE productos ALTER COLUMN version SET NOT NULL, ALTER COLUMN version SET DEFAULT 0;
ALTER TABLE product_variants ALTER COLUMN version SET NOT NULL, ALTER COLUMN version SET DEFAULT 0;
ALTER TABLE product_images ALTER COLUMN version SET NOT NULL, ALTER COLUMN version SET DEFAULT 0;
ALTER TABLE pagos ALTER COLUMN version SET NOT NULL, ALTER COLUMN version SET DEFAULT 0;
ALTER TABLE refunds ALTER COLUMN version SET NOT NULL, ALTER COLUMN version SET DEFAULT 0;
ALTER TABLE resenas ALTER COLUMN version SET NOT NULL, ALTER COLUMN version SET DEFAULT 0;
ALTER TABLE notifications ALTER COLUMN version SET NOT NULL, ALTER COLUMN version SET DEFAULT 0;
ALTER TABLE cupones ALTER COLUMN version SET NOT NULL, ALTER COLUMN version SET DEFAULT 0;
ALTER TABLE envios ALTER COLUMN version SET NOT NULL, ALTER COLUMN version SET DEFAULT 0;
ALTER TABLE evento_tracking ALTER COLUMN version SET NOT NULL, ALTER COLUMN version SET DEFAULT 0;
ALTER TABLE outbox_eventos ALTER COLUMN version SET NOT NULL, ALTER COLUMN version SET DEFAULT 0;
