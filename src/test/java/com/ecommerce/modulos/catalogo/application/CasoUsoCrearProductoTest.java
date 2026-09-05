package com.ecommerce.modulos.catalogo.application;

import com.ecommerce.modulos.catalogo.application.dto.RespuestaProducto;
import com.ecommerce.modulos.catalogo.application.dto.SolicitudCrearProducto;
import com.ecommerce.modulos.catalogo.application.dto.SolicitudVariante;
import com.ecommerce.modulos.catalogo.domain.EstadoProducto;
import com.ecommerce.modulos.catalogo.domain.Producto;
import com.ecommerce.modulos.catalogo.domain.RepositorioProducto;
import com.ecommerce.modulos.catalogo.domain.VarianteProducto;
import com.ecommerce.modulos.catalogo.domain.eventos.EventoProductoCreado;
import com.ecommerce.modulos.compartido.domain.EventoDominio;
import com.ecommerce.modulos.compartido.domain.ExcepcionViolacionReglaNegocio;
import com.ecommerce.modulos.compartido.domain.PublicadorEventoDominio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CasoUsoCrearProductoTest {

    @Mock
    private RepositorioProducto repositorioProducto;
    @Mock
    private PublicadorEventoDominio eventPublisher;

    @InjectMocks
    private CasoUsoCrearProducto casoUsoCrearProducto;

    private UUID idTienda;
    private SolicitudCrearProducto request;

    @BeforeEach
    void setUp() {
        idTienda = UUID.randomUUID();
        request = SolicitudCrearProducto.builder()
                .nombre("Remera básica")
                .enlaceCorto("remera-basica")
                .descripcion("Una remera de algodón")
                .precio(new BigDecimal("100.00"))
                .moneda("USD")
                .sku("SKU-1")
                .inventario(10)
                .rastreoInventarioHabilitado(true)
                .build();
    }

    @Test
    void debeLanzarExcepcionSiSeAlcanzaElLimiteMaximoDeProductosPorTienda() {
        when(repositorioProducto.countByIdTienda(idTienda)).thenReturn(999999L);

        assertThrows(ExcepcionViolacionReglaNegocio.class, () -> {
            casoUsoCrearProducto.execute(request, idTienda);
        });

        verify(repositorioProducto, never()).save(any());
        verify(eventPublisher, never()).publish(anyList());
    }

    @Test
    void debePermitirCrearProductoCuandoElConteoEstaJustoDebajoDelLimite() {
        when(repositorioProducto.countByIdTienda(idTienda)).thenReturn(999998L);
        when(repositorioProducto.save(any(Producto.class))).thenAnswer(i -> i.getArguments()[0]);

        RespuestaProducto respuesta = casoUsoCrearProducto.execute(request, idTienda);

        assertNotNull(respuesta);
        verify(repositorioProducto).save(any(Producto.class));
    }

    @Test
    void debeCrearProductoSinVariantesCuandoNoSeProveenVariantes() {
        when(repositorioProducto.countByIdTienda(idTienda)).thenReturn(0L);
        when(repositorioProducto.save(any(Producto.class))).thenAnswer(i -> i.getArguments()[0]);

        RespuestaProducto respuesta = casoUsoCrearProducto.execute(request, idTienda);

        assertNotNull(respuesta);
        assertTrue(respuesta.getVariants().isEmpty());
    }

    @Test
    void debeCrearProductoConVariantesAsignandoIdTiendaYReferenciaBidireccional() {
        SolicitudVariante variante1 = SolicitudVariante.builder()
                .nombre("Rojo - M")
                .sku("SKU-1-ROJO-M")
                .monto(new BigDecimal("105.00"))
                .moneda("USD")
                .inventario(5)
                .build();
        SolicitudVariante variante2 = SolicitudVariante.builder()
                .nombre("Azul - L")
                .sku("SKU-1-AZUL-L")
                .monto(new BigDecimal("110.00"))
                .moneda("USD")
                .inventario(3)
                .build();
        request.setVariants(List.of(variante1, variante2));

        when(repositorioProducto.countByIdTienda(idTienda)).thenReturn(0L);
        when(repositorioProducto.save(any(Producto.class))).thenAnswer(i -> i.getArguments()[0]);

        casoUsoCrearProducto.execute(request, idTienda);

        verify(repositorioProducto).save(argThat(producto -> {
            List<VarianteProducto> variants = producto.getVariants();
            if (variants.size() != 2) {
                return false;
            }
            return variants.stream().allMatch(v ->
                    v.getIdTienda().equals(idTienda) && v.getProducto() == producto);
        }));
    }

    @Test
    void debeMapearPrecioComparacionYPrecioCostoCuandoSonProvistos() {
        request.setPrecioComparacion(new BigDecimal("150.00"));
        request.setPrecioCosto(new BigDecimal("60.00"));

        when(repositorioProducto.countByIdTienda(idTienda)).thenReturn(0L);
        when(repositorioProducto.save(any(Producto.class))).thenAnswer(i -> i.getArguments()[0]);

        RespuestaProducto respuesta = casoUsoCrearProducto.execute(request, idTienda);

        assertEquals(0, new BigDecimal("150.00").compareTo(respuesta.getPrecioComparacion()));
        assertEquals(0, new BigDecimal("60.00").compareTo(respuesta.getPrecioCosto()));
    }

    @Test
    void debeDejarPrecioComparacionYPrecioCostoEnNuloCuandoNoSonProvistos() {
        when(repositorioProducto.countByIdTienda(idTienda)).thenReturn(0L);
        when(repositorioProducto.save(any(Producto.class))).thenAnswer(i -> i.getArguments()[0]);

        RespuestaProducto respuesta = casoUsoCrearProducto.execute(request, idTienda);

        assertNull(respuesta.getPrecioComparacion());
        assertNull(respuesta.getPrecioCosto());
    }

    @Test
    void debeCrearProductoConEstadoInicialDraft() {
        when(repositorioProducto.countByIdTienda(idTienda)).thenReturn(0L);
        when(repositorioProducto.save(any(Producto.class))).thenAnswer(i -> i.getArguments()[0]);

        RespuestaProducto respuesta = casoUsoCrearProducto.execute(request, idTienda);

        assertEquals(EstadoProducto.DRAFT.name(), respuesta.getEstado());
    }

    @Test
    void debePublicarEventoProductoCreadoYLimpiarLosEventosDeDominioLuegoDeCrear() {
        when(repositorioProducto.countByIdTienda(idTienda)).thenReturn(0L);
        when(repositorioProducto.save(any(Producto.class))).thenAnswer(i -> i.getArguments()[0]);

        // producto.getDomainEvents() devuelve una vista viva sobre la lista interna:
        // hay que copiar su contenido en el momento del publish, antes de que
        // clearDomainEvents() la vacíe, o la captura llegaría vacía al verificarla.
        List<EventoDominio> eventosCapturados = new java.util.ArrayList<>();
        doAnswer(invocation -> {
            List<EventoDominio> eventos = invocation.getArgument(0);
            eventosCapturados.addAll(eventos);
            return null;
        }).when(eventPublisher).publish(anyList());

        casoUsoCrearProducto.execute(request, idTienda);

        verify(eventPublisher).publish(anyList());
        assertEquals(1, eventosCapturados.size());
        assertInstanceOf(EventoProductoCreado.class, eventosCapturados.get(0));
    }
}
