package com.ecommerce.modulos.pagos.application;

import com.ecommerce.modulos.compartido.domain.Dinero;
import com.ecommerce.modulos.compartido.domain.ExcepcionEntidadNoEncontrada;
import com.ecommerce.modulos.compartido.domain.ExcepcionOperacionInvalida;
import com.ecommerce.modulos.compartido.infrastructure.ContextoInquilino;
import com.ecommerce.modulos.ordenes.domain.EstadoOrden;
import com.ecommerce.modulos.ordenes.domain.Orden;
import com.ecommerce.modulos.ordenes.domain.RepositorioOrden;
import com.ecommerce.modulos.pagos.domain.EstadoPago;
import com.ecommerce.modulos.pagos.domain.FabricaProcesadorPago;
import com.ecommerce.modulos.pagos.domain.Pago;
import com.ecommerce.modulos.pagos.domain.ProcesadorPago;
import com.ecommerce.modulos.pagos.domain.RepositorioPago;
import com.ecommerce.modulos.pagos.domain.ResultadoProcesamientoPago;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CasoUsoProcesarPagoTest {

    @Mock
    private RepositorioPago repositorioPago;

    @Mock
    private RepositorioOrden repositorioOrden;

    @Mock
    private FabricaProcesadorPago fabricaProcesadorPago;

    @Mock
    private ProcesadorPago procesadorPago;

    @InjectMocks
    private CasoUsoProcesarPago casoUsoProcesarPago;

    private UUID idOrden;
    private UUID idUsuario;
    private UUID idTienda;
    private Orden ordenMoc;

    @BeforeEach
    void setUp() {
        idOrden = UUID.randomUUID();
        idUsuario = UUID.randomUUID();
        idTienda = UUID.randomUUID();

        ContextoInquilino.setIdTienda(idTienda);

        ordenMoc = new Orden();
        ordenMoc.setNumeroOrden("ORD-12345");
        ordenMoc.setIdCliente(idUsuario);
        ordenMoc.setEstado(EstadoOrden.PENDING);
        Dinero total = Dinero.of(new BigDecimal("150.50"), "USD");
        ordenMoc.setTotal(total);
    }

    @AfterEach
    void tearDown() {
        ContextoInquilino.clear();
    }

    @Test
    void debeLanzarExcepcionSiOrdenNoExiste() {
        when(repositorioOrden.findById(idOrden)).thenReturn(Optional.empty());

        assertThrows(ExcepcionEntidadNoEncontrada.class, () -> {
            casoUsoProcesarPago.ejecutar(idOrden, idUsuario);
        });
    }

    @Test
    void debeLanzarExcepcionSiOrdenNoPerteneceAlUsuario() {
        ordenMoc.setIdCliente(UUID.randomUUID()); // Otro usuario
        when(repositorioOrden.findById(idOrden)).thenReturn(Optional.of(ordenMoc));

        assertThrows(ExcepcionOperacionInvalida.class, () -> {
            casoUsoProcesarPago.ejecutar(idOrden, idUsuario);
        });
    }

    @Test
    void debeLanzarExcepcionSiOrdenNoEstaPendiente() {
        ordenMoc.setEstado(EstadoOrden.PAID);
        when(repositorioOrden.findById(idOrden)).thenReturn(Optional.of(ordenMoc));

        assertThrows(ExcepcionOperacionInvalida.class, () -> {
            casoUsoProcesarPago.ejecutar(idOrden, idUsuario);
        });
    }

    @Test
    void debeProcesarPagoCorrectamente() {
        when(repositorioOrden.findById(idOrden)).thenReturn(Optional.of(ordenMoc));
        when(fabricaProcesadorPago.obtenerProcesador(idTienda)).thenReturn(procesadorPago);
        
        ResultadoProcesamientoPago resultadoMock = new ResultadoProcesamientoPago("ext-123", "http://checkout.url");
        when(procesadorPago.procesar(ordenMoc)).thenReturn(resultadoMock);
        when(procesadorPago.obtenerIdentificador()).thenReturn("STRIPE");

        doAnswer(invocation -> {
            Pago pagoGuardado = invocation.getArgument(0);
            pagoGuardado.setId(UUID.randomUUID());
            return pagoGuardado;
        }).when(repositorioPago).save(any(Pago.class));

        Map<String, Object> resultado = casoUsoProcesarPago.ejecutar(idOrden, idUsuario);

        assertNotNull(resultado);
        assertEquals("http://checkout.url", resultado.get("checkoutUrl"));
        assertNotNull(resultado.get("paymentId"));

        verify(repositorioPago, times(1)).save(argThat(pago -> 
            pago.getIdExterno().equals("ext-123") && 
            pago.getMetodoPago().equals("STRIPE") &&
            pago.getEstado() == EstadoPago.PENDING
        ));
    }
}
