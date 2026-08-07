package com.ecommerce.modulos.catalogo.infrastructure;

import com.ecommerce.modulos.catalogo.application.CasoUsoObtenerProducto;
import com.ecommerce.modulos.catalogo.application.dto.RespuestaProducto;
import com.ecommerce.modulos.compartido.infrastructure.ContextoInquilino;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class ControladorGraphQLProducto {

    private final CasoUsoObtenerProducto casoUsoObtenerProducto;

    @QueryMapping
    public RespuestaProducto obtenerProductoPorId(@Argument UUID id) {
        // En consultas GraphQL, puede ser necesario resolver el tenant context desde la request si no está inyectado globalmente,
        // pero asumiremos que el ContextoInquilino ya cuenta con el ID, o usaremos un fallback si no está seteado.
        UUID idTienda = ContextoInquilino.getIdTienda();
        if (idTienda == null) {
            // Manejo por defecto en caso de que GraphQL no cuente con el filtro de tenant context configurado para este endpoint
            // Esto requeriría que casoUsoObtenerProducto lo maneje o lanzar excepción.
            throw new RuntimeException("No se encontró el contexto de tienda para la petición GraphQL");
        }
        return casoUsoObtenerProducto.byId(id, idTienda);
    }
}
