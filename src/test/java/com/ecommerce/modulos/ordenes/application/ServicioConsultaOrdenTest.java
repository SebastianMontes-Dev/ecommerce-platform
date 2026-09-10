package com.ecommerce.modulos.ordenes.application;

import com.ecommerce.modulos.compartido.domain.Dinero;
import com.ecommerce.modulos.compartido.domain.ExcepcionEntidadNoEncontrada;
import com.ecommerce.modulos.compartido.domain.ExcepcionOperacionInvalida;
import com.ecommerce.modulos.ordenes.application.dto.FacturaOrden;
import com.ecommerce.modulos.ordenes.domain.ArticuloOrden;
import com.ecommerce.modulos.ordenes.domain.EstadoOrden;
import com.ecommerce.modulos.ordenes.domain.Orden;
import com.ecommerce.modulos.ordenes.domain.RepositorioOrden;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServicioConsultaOrdenTest {

    @Mock private RepositorioOrden repositorioOrden;

    @InjectMocks private ServicioConsultaOrden servicioConsultaOrden;

    private UUID idOrden;
    private Orden orden;

    @BeforeEach
    void setUp() {
        idOrden = UUID.randomUUID();
        orden = new Orden();
        orden.setId(idOrden);
        orden.setNumeroOrden("ORD-9");
        orden.setCorreoCliente("c@test.com");
        orden.setNombreCliente("Ana");
        orden.setSubtotal(Dinero.of(new BigDecimal("100.00"), "USD"));
        orden.setMontoImpuesto(Dinero.of(new BigDecimal("10.00"), "USD"));
        orden.setMontoEnvio(Dinero.of(new BigDecimal("5.00"), "USD"));
        orden.setTotal(Dinero.of(new BigDecimal("115.00"), "USD"));
    }

    @Test
    @DisplayName("obtenerResumen: mapea número, destinatario y total formateado")
    void obtenerResumen() {
        when(repositorioOrden.findById(idOrden)).thenReturn(Optional.of(orden));

        var resumen = servicioConsultaOrden.obtenerResumen(idOrden);

        assertEquals("ORD-9", resumen.numeroOrden());
        assertEquals("c@test.com", resumen.correoCliente());
        assertEquals("Ana", resumen.nombreCliente());
        assertEquals("115.00 USD", resumen.totalFormateado());
    }

    @Test
    @DisplayName("obtenerResumen: nombre nulo -> 'Cliente'")
    void obtenerResumenNombreNulo() {
        orden.setNombreCliente(null);
        when(repositorioOrden.findById(idOrden)).thenReturn(Optional.of(orden));

        assertEquals("Cliente", servicioConsultaOrden.obtenerResumen(idOrden).nombreCliente());
    }

    @Test
    @DisplayName("obtenerFactura: incluye las líneas con sus precios")
    void obtenerFactura() {
        ArticuloOrden articulo = new ArticuloOrden();
        articulo.setNombreProducto("Camiseta");
        articulo.setCantidad(2);
        articulo.setPrecioUnitario(Dinero.of(new BigDecimal("50.00"), "USD"));
        articulo.setSubtotal(Dinero.of(new BigDecimal("100.00"), "USD"));
        orden.getArticulos().add(articulo);
        when(repositorioOrden.findByIdConArticulos(idOrden)).thenReturn(Optional.of(orden));

        FacturaOrden factura = servicioConsultaOrden.obtenerFactura(idOrden);

        assertEquals("ORD-9", factura.getNumeroOrden());
        assertEquals(1, factura.getArticulos().size());
        FacturaOrden.Linea linea = factura.getArticulos().get(0);
        assertEquals("Camiseta", linea.getNombreProducto());
        assertEquals(2, linea.getCantidad());
        assertEquals(new BigDecimal("50.00"), linea.getPrecioUnitario().getMonto());
    }

    @Test
    @DisplayName("orden inexistente -> ExcepcionEntidadNoEncontrada")
    void ordenInexistente() {
        when(repositorioOrden.findById(idOrden)).thenReturn(Optional.empty());
        assertThrows(ExcepcionEntidadNoEncontrada.class, () -> servicioConsultaOrden.obtenerResumen(idOrden));
    }

    // --- verificarElegibilidadResena ---

    private Orden ordenEntregadaCon(UUID idProducto, UUID idCliente) {
        Orden o = new Orden();
        o.setId(idOrden);
        o.setIdCliente(idCliente);
        o.setEstado(EstadoOrden.DELIVERED);
        ArticuloOrden a = new ArticuloOrden();
        a.setIdProducto(idProducto);
        o.getArticulos().add(a);
        return o;
    }

    @Test
    @DisplayName("verificarElegibilidadResena: compra verificada -> no lanza")
    void elegibilidadOk() {
        UUID idProducto = UUID.randomUUID();
        UUID idCliente = UUID.randomUUID();
        when(repositorioOrden.findByIdConArticulos(idOrden))
                .thenReturn(Optional.of(ordenEntregadaCon(idProducto, idCliente)));

        assertDoesNotThrow(() -> servicioConsultaOrden.verificarElegibilidadResena(idOrden, idCliente, idProducto));
    }

    @Test
    @DisplayName("verificarElegibilidadResena: orden de otro cliente / no entregada / sin el producto -> ExcepcionOperacionInvalida")
    void elegibilidadRechazada() {
        UUID idProducto = UUID.randomUUID();
        UUID idCliente = UUID.randomUUID();

        Orden deOtro = ordenEntregadaCon(idProducto, UUID.randomUUID());
        when(repositorioOrden.findByIdConArticulos(idOrden)).thenReturn(Optional.of(deOtro));
        assertThrows(ExcepcionOperacionInvalida.class,
                () -> servicioConsultaOrden.verificarElegibilidadResena(idOrden, idCliente, idProducto));

        Orden noEntregada = ordenEntregadaCon(idProducto, idCliente);
        noEntregada.setEstado(EstadoOrden.SHIPPED);
        when(repositorioOrden.findByIdConArticulos(idOrden)).thenReturn(Optional.of(noEntregada));
        assertThrows(ExcepcionOperacionInvalida.class,
                () -> servicioConsultaOrden.verificarElegibilidadResena(idOrden, idCliente, idProducto));

        Orden sinProducto = ordenEntregadaCon(UUID.randomUUID(), idCliente);
        when(repositorioOrden.findByIdConArticulos(idOrden)).thenReturn(Optional.of(sinProducto));
        assertThrows(ExcepcionOperacionInvalida.class,
                () -> servicioConsultaOrden.verificarElegibilidadResena(idOrden, idCliente, idProducto));
    }
}
