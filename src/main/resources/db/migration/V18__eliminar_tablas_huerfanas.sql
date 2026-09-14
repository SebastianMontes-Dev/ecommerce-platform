-- refunds y direcciones son tablas huerfanas: ninguna entidad JPA las mapea (verificado con
-- grep contra src/main/java antes de este DROP). CasoUsoGestionarReembolso opera sobre Pago/
-- Producto directamente, no sobre una entidad Reembolso; Direccion es un @Embeddable usado
-- inline en Orden (columnas shipping_*/billing_* planas), no mapea la tabla direcciones.
-- Decision tomada explicitamente: se eliminan, no se implementan como feature (Fase 11).
DROP TABLE IF EXISTS refunds;
DROP TABLE IF EXISTS direcciones;
