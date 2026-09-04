-- La entidad Orden (y sus @Entity relacionados) nunca coincidió con estas columnas
-- desde que se agregó soporte de cupones/descuento; enmascarado hasta ahora porque
-- ddl-auto: validate nunca había corrido contra un Postgres real en CI.

ALTER TABLE ordenes RENAME COLUMN subtotal_amount TO monto_subtotal;
ALTER TABLE ordenes RENAME COLUMN total_amount TO monto_total;
ALTER TABLE ordenes RENAME COLUMN notes TO notas;
ALTER TABLE ordenes ADD COLUMN codigo_cupon VARCHAR(50);
ALTER TABLE ordenes ADD COLUMN monto_descuento DECIMAL(10,2);
ALTER TABLE ordenes ADD COLUMN descuento_currency VARCHAR(3) DEFAULT 'USD';

ALTER TABLE order_items RENAME COLUMN unit_price_amount TO monto_precio_unitario;
ALTER TABLE order_items RENAME COLUMN subtotal_amount TO monto_subtotal;

ALTER TABLE order_status_history RENAME COLUMN notes TO notas;
