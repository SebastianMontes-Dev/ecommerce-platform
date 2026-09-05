package com.ecommerce.modulos.catalogo.application;

import com.ecommerce.modulos.catalogo.application.dto.RespuestaProducto;
import com.ecommerce.modulos.catalogo.domain.Producto;
import com.ecommerce.modulos.catalogo.domain.RepositorioProducto;
import com.ecommerce.modulos.compartido.domain.ExcepcionEntidadNoEncontrada;
import com.ecommerce.modulos.compartido.infrastructure.RespuestaPaginada;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CasoUsoObtenerProductoTest {

    @Mock
    private RepositorioProducto repositorioProducto;

    @InjectMocks
    private CasoUsoObtenerProducto casoUsoObtenerProducto;

    private UUID idTienda;

    @BeforeEach
    void setUp() {
        idTienda = UUID.randomUUID();
    }

    @Test
    void debeListarProductosPaginadosParaLaTienda() {
        Producto producto = new Producto();
        producto.setNombre("Silla");
        producto.setEnlaceCorto("silla");
        Pageable pageable = PageRequest.of(0, 10);
        when(repositorioProducto.findAllByIdTienda(idTienda, pageable))
                .thenReturn(new PageImpl<>(List.of(producto), pageable, 1));

        RespuestaPaginada<RespuestaProducto> respuesta = casoUsoObtenerProducto.listProducts(idTienda, pageable);

        assertEquals(1, respuesta.getTotalElements());
        assertEquals("Silla", respuesta.getContent().get(0).getNombre());
    }

    @Test
    void debeRetornarPaginaVaciaCuandoLaTiendaNoTieneProductos() {
        Pageable pageable = PageRequest.of(0, 10);
        when(repositorioProducto.findAllByIdTienda(idTienda, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        RespuestaPaginada<RespuestaProducto> respuesta = casoUsoObtenerProducto.listProducts(idTienda, pageable);

        assertTrue(respuesta.getContent().isEmpty());
        assertEquals(0, respuesta.getTotalElements());
    }

    @Test
    void debeObtenerProductoPorSlugCuandoExiste() {
        Producto producto = new Producto();
        producto.setNombre("Mesa");
        producto.setEnlaceCorto("mesa");
        when(repositorioProducto.findByIdTiendaAndEnlaceCortoWithImages(idTienda, "mesa"))
                .thenReturn(Optional.of(producto));

        RespuestaProducto respuesta = casoUsoObtenerProducto.bySlug("mesa", idTienda);

        assertEquals("Mesa", respuesta.getNombre());
    }

    @Test
    void debeLanzarExcepcionSiElProductoPorSlugNoExiste() {
        when(repositorioProducto.findByIdTiendaAndEnlaceCortoWithImages(idTienda, "inexistente"))
                .thenReturn(Optional.empty());

        assertThrows(ExcepcionEntidadNoEncontrada.class, () ->
                casoUsoObtenerProducto.bySlug("inexistente", idTienda));
    }

    @Test
    void debeObtenerProductoPorIdCuandoPerteneceALaTienda() {
        UUID idProducto = UUID.randomUUID();
        Producto producto = new Producto();
        producto.setIdTienda(idTienda);
        producto.setNombre("Lámpara");
        when(repositorioProducto.findById(idProducto)).thenReturn(Optional.of(producto));

        RespuestaProducto respuesta = casoUsoObtenerProducto.byId(idProducto, idTienda);

        assertEquals("Lámpara", respuesta.getNombre());
    }

    @Test
    void debeLanzarExcepcionSiElProductoPorIdNoExiste() {
        UUID idProducto = UUID.randomUUID();
        when(repositorioProducto.findById(idProducto)).thenReturn(Optional.empty());

        assertThrows(ExcepcionEntidadNoEncontrada.class, () ->
                casoUsoObtenerProducto.byId(idProducto, idTienda));
    }

    @Test
    void debeLanzarExcepcionSiElProductoPerteneceAOtraTienda() {
        UUID idProducto = UUID.randomUUID();
        UUID idTiendaDueña = UUID.randomUUID();
        UUID idTiendaAtacante = UUID.randomUUID();
        Producto producto = new Producto();
        producto.setIdTienda(idTiendaDueña);
        when(repositorioProducto.findById(idProducto)).thenReturn(Optional.of(producto));

        assertThrows(ExcepcionEntidadNoEncontrada.class, () ->
                casoUsoObtenerProducto.byId(idProducto, idTiendaAtacante));
    }
}
