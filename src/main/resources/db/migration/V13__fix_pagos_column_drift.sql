-- La entidad Pago nunca coincidió con estas columnas desde V6 (mismo patrón
-- de drift que V12 corrigió para ordenes/order_items/order_status_history):
-- enmascarado hasta ahora porque ddl-auto: validate nunca había corrido
-- contra un Postgres real en CI.

ALTER TABLE pagos RENAME COLUMN amount TO monto;
ALTER TABLE pagos RENAME COLUMN currency TO moneda;
