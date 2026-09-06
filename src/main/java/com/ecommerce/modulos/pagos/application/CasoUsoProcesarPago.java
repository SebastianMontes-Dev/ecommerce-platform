package com.ecommerce.modulos.pagos.application;

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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CasoUsoProcesarPago {

    private final RepositorioPago repositorioPago;
    private final RepositorioOrden repositorioOrden;
    private final FabricaProcesadorPago fabricaProcesadorPago;

    /**
     * Deliberadamente NO @Transactional: procesador.procesar(orden) es una llamada HTTP
     * externa (Stripe u otra pasarela). Envolverla en una transaccion de DB mantendria
     * una conexion del pool retenida durante todo el round-trip de red, y de todos modos
     * no daria atomicidad real (Stripe no participa de un rollback de nuestra DB). Cada
     * repositorio de Spring Data ya corre en su propia transaccion corta por metodo.
     */
    public Map<String, Object> ejecutar(UUID idOrden, UUID idUsuario) {
        Orden orden = repositorioOrden.findById(idOrden)
                .orElseThrow(() -> new ExcepcionEntidadNoEncontrada("Orden", idOrden));

        if (!orden.getIdCliente().equals(idUsuario)) {
            throw new ExcepcionOperacionInvalida("La orden no pertenece al usuario autenticado.");
        }

        if (orden.getEstado() != EstadoOrden.PENDING) {
            throw new ExcepcionOperacionInvalida("Solo se pueden pagar órdenes en estado pendiente.");
        }

        // Obtener la pasarela de pago configurada para esta tienda (ej. Stripe, PayPal)
        ProcesadorPago procesador = fabricaProcesadorPago.obtenerProcesador(ContextoInquilino.getIdTienda());

        // Ejecutar el pago de manera abstracta
        ResultadoProcesamientoPago resultado = procesador.procesar(orden);

        // Guardar el registro del pago en nuestra base de datos
        Pago pago = new Pago();
        pago.setIdTienda(ContextoInquilino.getIdTienda());
        pago.setIdOrden(idOrden);
        pago.setMonto(orden.getTotal().getMonto());
        pago.setMoneda(orden.getTotal().getMoneda());
        pago.setMetodoPago(procesador.obtenerIdentificador());
        pago.setEstado(EstadoPago.PENDING);
        pago.setIdExterno(resultado.idExterno());
        repositorioPago.save(pago);

        return Map.of(
                "paymentId", pago.getId(),
                "checkoutUrl", resultado.checkoutUrl()
        );
    }
}
