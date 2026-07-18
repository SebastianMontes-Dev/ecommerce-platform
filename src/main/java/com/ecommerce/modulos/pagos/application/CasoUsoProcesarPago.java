package com.ecommerce.modulos.pagos.application;

import com.ecommerce.modulos.compartido.domain.ExcepcionEntidadNoEncontrada;
import com.ecommerce.modulos.compartido.domain.ExcepcionOperacionInvalida;
import com.ecommerce.modulos.compartido.infrastructure.ContextoInquilino;
import com.ecommerce.modulos.ordenes.domain.EstadoOrden;
import com.ecommerce.modulos.ordenes.domain.Orden;
import com.ecommerce.modulos.ordenes.domain.RepositorioOrden;
import com.ecommerce.modulos.pagos.domain.EstadoPago;
import com.ecommerce.modulos.pagos.domain.Pago;
import com.ecommerce.modulos.pagos.domain.RepositorioPago;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CasoUsoProcesarPago {

    private final RepositorioPago repositorioPago;
    private final RepositorioOrden repositorioOrden;

    @Value("${app.stripe.api-key}")
    private String stripeApiKey;

    @Transactional
    public Map<String, Object> ejecutar(UUID idOrden, UUID idUsuario) {
        Orden orden = repositorioOrden.findById(idOrden)
                .orElseThrow(() -> new ExcepcionEntidadNoEncontrada("Orden", idOrden));

        if (!orden.getIdCliente().equals(idUsuario)) {
            throw new ExcepcionOperacionInvalida("La orden no pertenece al usuario autenticado.");
        }

        if (orden.getEstado() != EstadoOrden.PENDING) {
            throw new ExcepcionOperacionInvalida("Solo se pueden pagar órdenes en estado pendiente.");
        }

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

            Pago pago = new Pago();
            pago.setIdTienda(ContextoInquilino.getIdTienda());
            pago.setIdOrden(idOrden);
            pago.setMonto(orden.getTotal().getMonto());
            pago.setMoneda(orden.getTotal().getMoneda());
            pago.setMetodoPago("STRIPE");
            pago.setEstado(EstadoPago.PENDING);
            pago.setIdExterno(idReferencia);
            repositorioPago.save(pago);

            return Map.of(
                    "paymentId", pago.getId(),
                    "checkoutUrl", session.getUrl()
            );

        } catch (StripeException e) {
            log.error("Error al crear sesión de pago en Stripe", e);
            throw new ExcepcionOperacionInvalida("No se pudo iniciar el proceso de pago.");
        }
    }
}
