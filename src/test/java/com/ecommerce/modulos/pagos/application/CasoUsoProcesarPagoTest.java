package com.ecommerce.modulos.pagos.application;

import com.ecommerce.modulos.compartido.domain.Dinero;
import com.ecommerce.modulos.compartido.domain.ExcepcionEntidadNoEncontrada;
import com.ecommerce.modulos.compartido.domain.ExcepcionOperacionInvalida;
import com.ecommerce.modulos.compartido.infrastructure.ContextoInquilino;
import com.ecommerce.modulos.ordenes.domain.EstadoOrden;
import com.ecommerce.modulos.ordenes.domain.Orden;
import com.ecommerce.modulos.ordenes.domain.RepositorioOrden;
import com.ecommerce.modulos.pagos.domain.Pago;
import com.ecommerce.modulos.pagos.domain.RepositorioPago;
import com.stripe.exception.StripeException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

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

        ReflectionTestUtils.setField(casoUsoProcesarPago, "stripeApiKey", "sk_test_mock");

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

    // Nota: No testeamos la creación real de Stripe Session aquí porque hace una llamada de red a la API real de Stripe.
    // En un entorno de producción, aislaríamos la lógica de Stripe en un "StripeGateway" o usaríamos mockito-inline para mockear métodos estáticos de Stripe.
}
