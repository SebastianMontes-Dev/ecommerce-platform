-- Mismo patron de drift que V12 (ordenes) y V13 (pagos): las entidades Producto
-- y VarianteProducto siempre esperaron columnas en espanol (monto/moneda) para
-- el precio principal, pero la tabla se creo con amount/currency en ingles.
-- Los demas precios de productos (comparacion, costo) ya coincidian.

ALTER TABLE productos RENAME COLUMN amount TO monto;
ALTER TABLE productos RENAME COLUMN currency TO moneda;

ALTER TABLE product_variants RENAME COLUMN amount TO monto;
ALTER TABLE product_variants RENAME COLUMN currency TO moneda;
