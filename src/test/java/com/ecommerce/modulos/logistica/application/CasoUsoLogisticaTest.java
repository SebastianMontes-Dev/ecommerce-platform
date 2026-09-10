package com.ecommerce.modulos.logistica.application;

import com.ecommerce.modulos.compartido.domain.ExcepcionEntidadNoEncontrada;
import com.ecommerce.modulos.logistica.application.dto.RespuestaEnvio;
import com.ecommerce.modulos.logistica.domain.Envio;
import com.ecommerce.modulos.logistica.domain.EstadoEnvio;
import com.ecommerce.modulos.logistica.domain.RepositorioEnvio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CasoUsoLogisticaTest {

    @Mock
    private RepositorioEnvio repositorioEnvio;

    @InjectMocks
    private CasoUsoLogistica casoUsoLogistica;

    private UUID idTienda;
    private UUID idOrden;

    @BeforeEach
    void setUp() {
        idTienda = UUID.randomUUID();
        idOrden = UUID.randomUUID();
    }

    @Test
    void debePrepararEnvioConEstadoPreparandoYProveedorDhl() {
        when(repositorioEnvio.save(any(Envio.class))).thenAnswer(i -> i.getArguments()[0]);

        casoUsoLogistica.prepararEnvio(idTienda, idOrden);

        ArgumentCaptor<Envio> captor = ArgumentCaptor.forClass(Envio.class);
        verify(repositorioEnvio).save(captor.capture());
        Envio envio = captor.getValue();
        assertEquals(idTienda, envio.getIdTienda());
        assertEquals(idOrden, envio.getIdOrden());
        assertEquals(EstadoEnvio.PREPARANDO, envio.getEstado());
        assertEquals("DHL_EXPRESS", envio.getProveedor());
        assertTrue(envio.getNumeroGuia().startsWith("TRK-"));
        assertEquals(1, envio.getHistorial().size());
    }

    @Test
    void debeActualizarEstadoYDevolverElDto() {
        String numeroGuia = "TRK-ABC1234567";
        Envio envio = new Envio();
        envio.setIdTienda(idTienda);
        envio.setIdOrden(idOrden);
        envio.setNumeroGuia(numeroGuia);

        when(repositorioEnvio.findByIdTiendaAndNumeroGuia(idTienda, numeroGuia)).thenReturn(Optional.of(envio));
        when(repositorioEnvio.save(any(Envio.class))).thenAnswer(i -> i.getArguments()[0]);

        RespuestaEnvio resultado = casoUsoLogistica.actualizarEstado(
                idTienda, numeroGuia, EstadoEnvio.EN_TRANSITO, "Bogotá", "Salió del centro de distribución");

        assertEquals("EN_TRANSITO", resultado.estado());
        assertEquals(numeroGuia, resultado.numeroGuia());
        assertEquals(1, resultado.historial().size());
        assertEquals("Bogotá", resultado.historial().get(0).ubicacion());
        assertEquals("Salió del centro de distribución", resultado.historial().get(0).descripcion());
        verify(repositorioEnvio).save(envio);
    }

    @Test
    void debeLanzarExcepcionAlActualizarEstadoSiEnvioNoExiste() {
        String numeroGuia = "TRK-NOEXISTE01";
        when(repositorioEnvio.findByIdTiendaAndNumeroGuia(idTienda, numeroGuia)).thenReturn(Optional.empty());

        assertThrows(ExcepcionEntidadNoEncontrada.class, () ->
                casoUsoLogistica.actualizarEstado(idTienda, numeroGuia, EstadoEnvio.ENTREGADO, "Casa", "Entregado"));

        verify(repositorioEnvio, never()).save(any());
    }

    @Test
    void debeRastrearEnvioYDevolverElDto() {
        String numeroGuia = "TRK-ABC1234567";
        Envio envio = new Envio();
        envio.setIdTienda(idTienda);
        envio.setIdOrden(idOrden);
        envio.setNumeroGuia(numeroGuia);
        envio.setProveedor("DHL_EXPRESS");

        when(repositorioEnvio.findByIdTiendaAndNumeroGuia(idTienda, numeroGuia)).thenReturn(Optional.of(envio));

        RespuestaEnvio resultado = casoUsoLogistica.rastrearEnvio(idTienda, numeroGuia);

        assertEquals(numeroGuia, resultado.numeroGuia());
        assertEquals(idOrden, resultado.idOrden());
        assertEquals("DHL_EXPRESS", resultado.proveedor());
    }

    @Test
    void debeLanzarExcepcionAlRastrearSiEnvioNoExiste() {
        String numeroGuia = "TRK-NOEXISTE01";
        when(repositorioEnvio.findByIdTiendaAndNumeroGuia(idTienda, numeroGuia)).thenReturn(Optional.empty());

        assertThrows(ExcepcionEntidadNoEncontrada.class, () ->
                casoUsoLogistica.rastrearEnvio(idTienda, numeroGuia));

        verify(repositorioEnvio, never()).save(any());
    }
}
