package com.ecommerce.modulos.pagos.domain;

public record ResultadoProcesamientoPago(
    String idExterno,
    String checkoutUrl
) {}
