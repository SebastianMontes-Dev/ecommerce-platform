package com.ecommerce.modulos.resenas.application;

import com.ecommerce.modulos.compartido.domain.ExcepcionOperacionInvalida;
import com.ecommerce.modulos.compartido.domain.ExcepcionEntidadNoEncontrada;
import com.ecommerce.modulos.compartido.infrastructure.ContextoInquilino;
import com.ecommerce.modulos.ordenes.domain.ArticuloOrden;
import com.ecommerce.modulos.ordenes.domain.EstadoOrden;
import com.ecommerce.modulos.ordenes.domain.Orden;
import com.ecommerce.modulos.ordenes.domain.RepositorioOrden;
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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CasoUsoCrearResenaTest {

    @Mock
    private RepositorioResena repositorioResena;

    @Mock
    private RepositorioOrden repositorioOrden;

    @InjectMocks
    private CasoUsoCrearResena casoUsoCrearResena;

    private UUID idProducto;
    private UUID idCliente;
    private UUID idOrden;
    private UUID idTienda;
    private SolicitudCrearResena solicitud;
    private Orden ordenMoc;

    @BeforeEach
    void setUp() {
        idProducto = UUID.randomUUID();
        idCliente = UUID.randomUUID();
        idOrden = UUID.randomUUID();
        idTienda = UUID.randomUUID();

        ContextoInquilino.setIdTienda(idTienda);

        solicitud = new SolicitudCrearResena();
        solicitud.setIdOrden(idOrden);
        solicitud.setCalificacion(5);
        solicitud.setTitulo("Excelente");
        solicitud.setComentario("Muy buen producto");

        ordenMoc = new Orden();
        ordenMoc.setIdCliente(idCliente);
        ordenMoc.setEstado(EstadoOrden.DELIVERED);
        
        ArticuloOrden articulo = new ArticuloOrden();
        articulo.setIdProducto(idProducto);
        ordenMoc.setArticulos(List.of(articulo));
    }

    @AfterEach
    void tearDown() {
        ContextoInquilino.clear();
    }

    @Test
    void debeCrearResenaExitosamente() {
        when(repositorioOrden.findById(idOrden)).thenReturn(Optional.of(ordenMoc));
        when(repositorioResena.save(any(Resena.class))).thenAnswer(i -> i.getArguments()[0]);

        Resena resultado = casoUsoCrearResena.ejecutar(idProducto, idCliente, solicitud);

        assertNotNull(resultado);
        assertEquals(5, resultado.getCalificacion().getValue().intValue());
        assertEquals(idProducto, resultado.getIdProducto());
        verify(repositorioResena, times(1)).save(any(Resena.class));
    }

    @Test
    void debeLanzarExcepcionSiUsuarioEsNulo() {
        assertThrows(ExcepcionOperacionInvalida.class, () -> {
            casoUsoCrearResena.ejecutar(idProducto, null, solicitud);
        });
    }

    @Test
    void debeLanzarExcepcionSiOrdenNoPerteneceAlCliente() {
        ordenMoc.setIdCliente(UUID.randomUUID()); // Otro cliente
        when(repositorioOrden.findById(idOrden)).thenReturn(Optional.of(ordenMoc));

        assertThrows(ExcepcionOperacionInvalida.class, () -> {
            casoUsoCrearResena.ejecutar(idProducto, idCliente, solicitud);
        });
    }

    @Test
    void debeLanzarExcepcionSiOrdenNoEstaEntregada() {
        ordenMoc.setEstado(EstadoOrden.SHIPPED);
        when(repositorioOrden.findById(idOrden)).thenReturn(Optional.of(ordenMoc));

        assertThrows(ExcepcionOperacionInvalida.class, () -> {
            casoUsoCrearResena.ejecutar(idProducto, idCliente, solicitud);
        });
    }

    @Test
    void debeLanzarExcepcionSiOrdenNoContieneElProducto() {
        ordenMoc.getArticulos().get(0).setIdProducto(UUID.randomUUID()); // Otro producto
        when(repositorioOrden.findById(idOrden)).thenReturn(Optional.of(ordenMoc));

        assertThrows(ExcepcionOperacionInvalida.class, () -> {
            casoUsoCrearResena.ejecutar(idProducto, idCliente, solicitud);
        });
    }
}
