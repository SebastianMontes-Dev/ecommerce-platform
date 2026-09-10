package com.ecommerce.modulos.logistica.application;

import com.ecommerce.modulos.compartido.domain.ExcepcionEntidadNoEncontrada;
import com.ecommerce.modulos.logistica.application.dto.RespuestaEnvio;
import com.ecommerce.modulos.logistica.domain.Envio;
import com.ecommerce.modulos.logistica.domain.EstadoEnvio;
import com.ecommerce.modulos.logistica.domain.RepositorioEnvio;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CasoUsoLogistica {

    private final RepositorioEnvio repositorioEnvio;

    @Transactional
    public void prepararEnvio(UUID idTienda, UUID idOrden) {
        // En un caso real, se llama a la API de FedEx o DHL
        String numeroGuia = "TRK-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase();

        Envio envio = new Envio();
        envio.setIdTienda(idTienda);
        envio.setIdOrden(idOrden);
        envio.setNumeroGuia(numeroGuia);
        envio.setProveedor("DHL_EXPRESS");
        envio.actualizarEstado(EstadoEnvio.PREPARANDO, "Centro de Distribución", "Orden recibida para empaque");

        repositorioEnvio.save(envio);
    }

    @Transactional
    public RespuestaEnvio actualizarEstado(UUID idTienda, String numeroGuia, EstadoEnvio nuevoEstado, String ubicacion, String descripcion) {
        Envio envio = buscar(idTienda, numeroGuia);
        envio.actualizarEstado(nuevoEstado, ubicacion, descripcion);
        return RespuestaEnvio.de(repositorioEnvio.save(envio));
    }

    @Transactional(readOnly = true)
    public RespuestaEnvio rastrearEnvio(UUID idTienda, String numeroGuia) {
        return RespuestaEnvio.de(buscar(idTienda, numeroGuia));
    }

    private Envio buscar(UUID idTienda, String numeroGuia) {
        return repositorioEnvio.findByIdTiendaAndNumeroGuia(idTienda, numeroGuia)
                .orElseThrow(() -> new ExcepcionEntidadNoEncontrada("Envío", numeroGuia));
    }
}
