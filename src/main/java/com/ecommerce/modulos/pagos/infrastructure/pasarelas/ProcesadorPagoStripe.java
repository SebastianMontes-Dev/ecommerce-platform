package com.ecommerce.modulos.pagos.infrastructure.pasarelas;

import com.ecommerce.modulos.compartido.domain.ExcepcionOperacionInvalida;
import com.ecommerce.modulos.ordenes.domain.Orden;
import com.ecommerce.modulos.pagos.domain.ProcesadorPago;
import com.ecommerce.modulos.pagos.domain.ResultadoProcesamientoPago;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
@Slf4j
public class ProcesadorPagoStripe implements ProcesadorPago {

    @Value("${app.stripe.api-key}")
    private String stripeApiKey;

    @Override
    public String obtenerIdentificador() {
        return "STRIPE";
    }

    @Override
    public ResultadoProcesamientoPago procesar(Orden orden) {
        com.stripe.Stripe.apiKey = stripeApiKey;

        try {
            long totalCents = orden.getTotal().getMonto().multiply(new BigDecimal("100")).longValue();
            String idReferencia = UUID.randomUUID().toString();

            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl("http://localhost:3000/checkout/success?session_id={CHECKOUT_SESSION_ID}")
                    .setCancelUrl("http://localhost:3000/checkout/cancel")
                    .setClientReferenceId(idReferencia)
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setQuantity(1L)
                                    .setPriceData(
                                            SessionCreateParams.LineItem.PriceData.builder()
                                                    .setCurrency(orden.getTotal().getMoneda().toLowerCase())
                                                    .setUnitAmount(totalCents)
                                                    .setProductData(
                                                            SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                    .setName("Orden #" + orden.getNumeroOrden())
                                                                    .build()
                                                    )
                                                    .build()
                                    )
                                    .build()
                    )
                    .build();

            Session session = Session.create(params);

            return new ResultadoProcesamientoPago(idReferencia, session.getUrl());

        } catch (StripeException e) {
            log.error("Error al crear sesión de pago en Stripe", e);
            throw new ExcepcionOperacionInvalida("No se pudo iniciar el proceso de pago con Stripe.");
        }
    }
}
