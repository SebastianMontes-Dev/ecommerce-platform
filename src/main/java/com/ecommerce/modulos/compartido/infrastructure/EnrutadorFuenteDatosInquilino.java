package com.ecommerce.modulos.compartido.infrastructure;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

@Slf4j
public class EnrutadorFuenteDatosInquilino extends AbstractRoutingDataSource {

    @Override
    protected Object determineCurrentLookupKey() {
        java.util.UUID idTienda = ContextoInquilino.getIdTienda();
        if (idTienda != null) {
            log.debug("Enrutando base de datos para el inquilino: {}", idTienda);
            // En una arquitectura híbrida real, aquí verificaríamos en caché/Redis si este ID de tienda 
            // tiene una base de datos dedicada. Si la tiene, devolvemos su identificador.
            // Si es un inquilino estándar, devolvemos nulo o "default" para que caiga en la DB compartida.
            
            // Simulación: Si el ID empieza por una letra específica, asume DB premium
            // return idTienda.toString();
        }
        return "default";
    }
}
