package com.ecommerce.modulos.pagos.domain;

import com.ecommerce.modulos.ordenes.domain.Orden;

import java.util.Map;

/**
 * Contrato genérico para cualquier pasarela de pago (Stripe, PayPal, MercadoPago, etc.)
 */
public interface ProcesadorPago {
    
    /**
     * Devuelve el nombre identificador único de esta pasarela (ej. "STRIPE", "PAYPAL").
     */
    String obtenerIdentificador();

    /**
     * Inicia el proceso de pago con el proveedor externo y devuelve los datos necesarios 
     * (como la URL de redirección y el ID de referencia externo).
     */
    ResultadoProcesamientoPago procesar(Orden orden);
}
