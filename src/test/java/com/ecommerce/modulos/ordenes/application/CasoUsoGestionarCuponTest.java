package com.ecommerce.modulos.ordenes.application;

import com.ecommerce.modulos.ordenes.application.dto.RespuestaCupon;
import com.ecommerce.modulos.ordenes.application.dto.SolicitudCrearCupon;
import com.ecommerce.modulos.ordenes.domain.Cupon;
import com.ecommerce.modulos.ordenes.domain.RepositorioCupon;
import com.ecommerce.modulos.ordenes.domain.TipoDescuento;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CasoUsoGestionarCuponTest {

    @Mock private RepositorioCupon repositorioCupon;
    @InjectMocks private CasoUsoGestionarCupon casoUsoGestionarCupon;

    private final UUID idTienda = UUID.randomUUID();

    private Cupon cupon(TipoDescuento tipo, String valor) {
        Cupon c = new Cupon();
        c.setCodigo("DESC");
        c.setTipo(tipo);
        c.setValor(new BigDecimal(valor));
        c.setActivo(true);
        return c;
    }

    @Test
    void calcularDescuentoPorcentaje() {
        when(repositorioCupon.findByIdTiendaAndCodigo(any(), anyString()))
                .thenReturn(Optional.of(cupon(TipoDescuento.PORCENTAJE, "10")));

        assertEquals(new BigDecimal("20.00"),
                casoUsoGestionarCupon.calcularDescuento(idTienda, "DESC", new BigDecimal("200.00")));
    }

    @Test
    void calcularDescuentoMontoFijo() {
        when(repositorioCupon.findByIdTiendaAndCodigo(any(), anyString()))
                .thenReturn(Optional.of(cupon(TipoDescuento.MONTO_FIJO, "15.00")));

        assertEquals(new BigDecimal("15.00"),
                casoUsoGestionarCupon.calcularDescuento(idTienda, "DESC", new BigDecimal("200.00")));
    }

    @Test
    void elDescuentoNuncaSuperaElSubtotal() {
        when(repositorioCupon.findByIdTiendaAndCodigo(any(), anyString()))
                .thenReturn(Optional.of(cupon(TipoDescuento.MONTO_FIJO, "50.00")));

        assertEquals(new BigDecimal("30.00"),
                casoUsoGestionarCupon.calcularDescuento(idTienda, "DESC", new BigDecimal("30.00")));
    }

    @Test
    void cuponInvalidoLanza() {
        Cupon inactivo = cupon(TipoDescuento.MONTO_FIJO, "10");
        inactivo.setActivo(false);
        when(repositorioCupon.findByIdTiendaAndCodigo(any(), anyString())).thenReturn(Optional.of(inactivo));

        assertThrows(IllegalArgumentException.class,
                () -> casoUsoGestionarCupon.calcularDescuento(idTienda, "DESC", new BigDecimal("100.00")));
    }

    @Test
    void crearCuponDevuelveUnDtoNoLaEntidad() {
        when(repositorioCupon.findByIdTiendaAndCodigo(any(), anyString())).thenReturn(Optional.empty());
        when(repositorioCupon.save(any(Cupon.class))).thenAnswer(inv -> inv.getArgument(0));

        SolicitudCrearCupon solicitud = SolicitudCrearCupon.builder()
                .codigo("promo10")
                .tipo(TipoDescuento.PORCENTAJE)
                .valor(new BigDecimal("10"))
                .build();

        RespuestaCupon respuesta = casoUsoGestionarCupon.crearCupon(idTienda, solicitud);

        assertEquals("PROMO10", respuesta.getCodigo());
        assertEquals(TipoDescuento.PORCENTAJE, respuesta.getTipo());
        assertEquals(new BigDecimal("10"), respuesta.getValor());
    }
}
