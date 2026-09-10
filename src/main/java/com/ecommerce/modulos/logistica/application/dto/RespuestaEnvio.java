package com.ecommerce.modulos.logistica.application.dto;

import com.ecommerce.modulos.logistica.domain.Envio;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record RespuestaEnvio(
        UUID idOrden,
        String numeroGuia,
        String proveedor,
        String estado,
        List<EventoTracking> historial) {

    public record EventoTracking(String estado, String ubicacion, String descripcion, LocalDateTime fecha) {}

    public static RespuestaEnvio de(Envio envio) {
        return new RespuestaEnvio(
                envio.getIdOrden(),
                envio.getNumeroGuia(),
                envio.getProveedor(),
                envio.getEstado().name(),
                envio.getHistorial().stream()
                        .map(e -> new EventoTracking(
                                e.getEstado().name(), e.getUbicacion(), e.getDescripcion(), e.getCreadoEn()))
                        .toList());
    }
}
