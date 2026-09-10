package com.ecommerce.modulos.resenas.application;

import com.ecommerce.modulos.compartido.domain.ExcepcionOperacionInvalida;
import com.ecommerce.modulos.compartido.infrastructure.ContextoInquilino;
import com.ecommerce.modulos.ordenes.application.ServicioConsultaOrden;
import com.ecommerce.modulos.resenas.application.dto.RespuestaResena;
import com.ecommerce.modulos.resenas.application.dto.SolicitudCrearResena;
import com.ecommerce.modulos.resenas.domain.RepositorioResena;
import com.ecommerce.modulos.resenas.domain.Resena;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CasoUsoCrearResenaTest {

    @Mock private RepositorioResena repositorioResena;
    @Mock private ServicioConsultaOrden servicioConsultaOrden;

    @InjectMocks private CasoUsoCrearResena casoUsoCrearResena;

    private UUID idProducto;
    private UUID idCliente;
    private UUID idOrden;
    private SolicitudCrearResena solicitud;

    @BeforeEach
    void setUp() {
        idProducto = UUID.randomUUID();
        idCliente = UUID.randomUUID();
        idOrden = UUID.randomUUID();
        ContextoInquilino.setIdTienda(UUID.randomUUID());

        solicitud = new SolicitudCrearResena();
        solicitud.setIdOrden(idOrden);
        solicitud.setCalificacion(5);
        solicitud.setTitulo("Excelente");
        solicitud.setComentario("Muy buen producto");
    }

    @AfterEach
    void tearDown() {
        ContextoInquilino.clear();
    }

    @Test
    void debeCrearResenaCuandoLaCompraEsElegible() {
        when(repositorioResena.save(any(Resena.class))).thenAnswer(i -> i.getArguments()[0]);

        RespuestaResena resultado = casoUsoCrearResena.ejecutar(idProducto, idCliente, solicitud);

        assertEquals(idProducto, resultado.idProducto());
        assertEquals(5, resultado.calificacion().intValue());
        verify(servicioConsultaOrden).verificarElegibilidadResena(idOrden, idCliente, idProducto);
        verify(repositorioResena).save(any(Resena.class));
    }

    @Test
    void debeLanzarExcepcionSiUsuarioEsNulo() {
        assertThrows(ExcepcionOperacionInvalida.class,
                () -> casoUsoCrearResena.ejecutar(idProducto, null, solicitud));
        verifyNoInteractions(servicioConsultaOrden, repositorioResena);
    }

    @Test
    void debePropagarSiLaCompraNoEsElegible() {
        doThrow(new ExcepcionOperacionInvalida("Solo se pueden reseñar productos de órdenes entregadas."))
                .when(servicioConsultaOrden).verificarElegibilidadResena(idOrden, idCliente, idProducto);

        assertThrows(ExcepcionOperacionInvalida.class,
                () -> casoUsoCrearResena.ejecutar(idProducto, idCliente, solicitud));
        verify(repositorioResena, never()).save(any());
    }
}
