package com.ecommerce.modulos.catalogo.application;

import com.ecommerce.modulos.catalogo.application.dto.RespuestaCategoria;
import com.ecommerce.modulos.catalogo.application.dto.SolicitudCrearCategoria;
import com.ecommerce.modulos.catalogo.domain.Categoria;
import com.ecommerce.modulos.catalogo.domain.RepositorioCategoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CasoUsoCrearCategoriaTest {

    @Mock
    private RepositorioCategoria repositorioCategoria;

    @InjectMocks
    private CasoUsoCrearCategoria casoUsoCrearCategoria;

    private UUID idTienda;
    private SolicitudCrearCategoria request;

    @BeforeEach
    void setUp() {
        idTienda = UUID.randomUUID();
        request = SolicitudCrearCategoria.builder()
                .nombre("Electrónica")
                .enlaceCorto("electronica")
                .descripcion("Productos electrónicos")
                .urlImagen("http://img/electronica.png")
                .build();
    }

    @Test
    void debeCrearCategoriaExitosamenteConDatosValidos() {
        when(repositorioCategoria.save(any(Categoria.class))).thenAnswer(i -> i.getArguments()[0]);

        RespuestaCategoria respuesta = casoUsoCrearCategoria.execute(request, idTienda);

        assertNotNull(respuesta);
        assertEquals("Electrónica", respuesta.getNombre());
        assertEquals("electronica", respuesta.getEnlaceCorto());
        assertNull(respuesta.getIdPadre());
        verify(repositorioCategoria).save(argThat(c -> c.getIdTienda().equals(idTienda)));
    }

    @Test
    void debeAsignarIdPadreCuandoSeProveeEnLaSolicitud() {
        UUID idPadre = UUID.randomUUID();
        request.setIdPadre(idPadre);
        when(repositorioCategoria.save(any(Categoria.class))).thenAnswer(i -> i.getArguments()[0]);

        RespuestaCategoria respuesta = casoUsoCrearCategoria.execute(request, idTienda);

        assertEquals(idPadre, respuesta.getIdPadre());
    }

    @Test
    void debeListarCategoriasRaizPorTienda() {
        Categoria raiz = new Categoria();
        raiz.setNombre("Hogar");
        raiz.setEnlaceCorto("hogar");
        when(repositorioCategoria.findRootCategoriesWithChildren(idTienda)).thenReturn(List.of(raiz));

        List<RespuestaCategoria> respuesta = casoUsoCrearCategoria.getCategories(idTienda);

        assertEquals(1, respuesta.size());
        assertEquals("Hogar", respuesta.get(0).getNombre());
    }

    @Test
    void debeRetornarListaVaciaCuandoLaTiendaNoTieneCategorias() {
        when(repositorioCategoria.findRootCategoriesWithChildren(idTienda)).thenReturn(List.of());

        List<RespuestaCategoria> respuesta = casoUsoCrearCategoria.getCategories(idTienda);

        assertTrue(respuesta.isEmpty());
    }
}
