package com.ecommerce.modulos.inquilino.application;

import com.ecommerce.modulos.ordenes.domain.EstadoOrden;
import com.ecommerce.modulos.ordenes.domain.events.EventoOrdenCreada;
import com.ecommerce.modulos.ordenes.domain.eventos.EventoEstadoOrdenCambiado;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;
import java.util.UUID;

/**
 * Conecta los eventos de dominio de ordenes al motor de Webhooks Outbound
 * (ServicioEmisorWebhook). Los nombres de evento expuestos a los clientes
 * de la API de webhooks siguen la convencion "recurso.accion" (orden.creada,
 * orden.pagada, etc.) - son un contrato publico, no deben cambiar sin avisar
 * a los tenants que tengan un WebhookTenant suscripto a ellos.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OyenteEventoOrdenWebhook {

    private final ServicioEmisorWebhook servicioEmisorWebhook;
    private final ObjectMapper objectMapper;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onOrderCreated(EventoOrdenCreada evento) {
        emitir(evento.getIdTienda(), "orden.creada", Map.of(
                "idOrden", evento.getIdOrden(),
                "idTienda", evento.getIdTienda(),
                "idCliente", evento.getIdCliente(),
                "ocurrioEn", evento.getOcurrioEn()
        ));
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onOrderStatusChanged(EventoEstadoOrdenCambiado evento) {
        String nombreEvento = nombreEventoParaEstado(evento.getNuevoEstado());
        if (nombreEvento == null) {
            log.debug("Sin evento de webhook configurado para el estado: {}", evento.getNuevoEstado());
            return;
        }

        emitir(evento.getIdTienda(), nombreEvento, Map.of(
                "idOrden", evento.getIdOrden(),
                "idTienda", evento.getIdTienda(),
                "estadoAnterior", evento.getEstadoAnterior(),
                "estadoNuevo", evento.getNuevoEstado(),
                "ocurrioEn", evento.getOcurrioEn()
        ));
    }

    private String nombreEventoParaEstado(EstadoOrden estado) {
        return switch (estado) {
            case CONFIRMED -> "orden.confirmada";
            case PAID -> "orden.pagada";
            case SHIPPED -> "orden.enviada";
            case DELIVERED -> "orden.entregada";
            case CANCELLED -> "orden.cancelada";
            case REFUNDED -> "orden.reembolsada";
            default -> null;
        };
    }

    private void emitir(UUID idTienda, String evento, Map<String, Object> payload) {
        try {
            String payloadJson = objectMapper.writeValueAsString(payload);
            servicioEmisorWebhook.emitirEvento(idTienda, evento, payloadJson);
        } catch (Exception e) {
            log.error("No se pudo armar el payload del webhook '{}' para la tienda {}: {}",
                    evento, idTienda, e.getMessage(), e);
        }
    }
}
