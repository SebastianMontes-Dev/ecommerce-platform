package com.ecommerce.modulos.pagos.domain;

import com.ecommerce.modulos.compartido.domain.ExcepcionOperacionInvalida;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class FabricaProcesadorPago {

    private final Map<String, ProcesadorPago> procesadores;

    public FabricaProcesadorPago(List<ProcesadorPago> listaProcesadores) {
        // Mapea los procesadores disponibles usando su identificador único
        this.procesadores = listaProcesadores.stream()
                .collect(Collectors.toMap(ProcesadorPago::obtenerIdentificador, Function.identity()));
    }

    /**
     * Obtiene el procesador de pagos adecuado para la Tienda especificada.
     */
    public ProcesadorPago obtenerProcesador(UUID idTienda) {
        // En un futuro, aquí buscaríamos en la base de datos la configuración de la tienda.
        // Ejemplo: String proveedorConfigurado = repositorioInquilino.obtenerProveedorPago(idTienda);
        
        // Por ahora, devolvemos STRIPE por defecto.
        String proveedorConfigurado = "STRIPE";

        ProcesadorPago procesador = procesadores.get(proveedorConfigurado);
        if (procesador == null) {
            throw new ExcepcionOperacionInvalida("No existe un procesador de pago configurado para: " + proveedorConfigurado);
        }
        
        return procesador;
    }
}
