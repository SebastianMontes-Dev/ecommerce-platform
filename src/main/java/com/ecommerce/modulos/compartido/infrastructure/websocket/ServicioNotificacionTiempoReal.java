package com.ecommerce.modulos.compartido.infrastructure.websocket;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class ServicioNotificacionTiempoReal {

    private final SimpMessagingTemplate messagingTemplate;

    public ServicioNotificacionTiempoReal(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void notificarNuevaOrden(java.util.UUID idTienda, Object orden) {
        messagingTemplate.convertAndSend("/topic/tienda/" + idTienda + "/ordenes", orden);
    }
}
