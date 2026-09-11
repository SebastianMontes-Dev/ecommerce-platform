package com.ecommerce.modulos.catalogo.infrastructure;

import com.ecommerce.modulos.catalogo.application.CasoUsoObtenerProducto;
import com.ecommerce.modulos.catalogo.application.dto.RespuestaProducto;
import com.ecommerce.modulos.compartido.domain.ExcepcionNoAutorizado;
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
        // FiltroAutenticacionJwt y FiltroInquilino son filtros globales (no restringidos a
        // /api/**), así que corren igual para /graphql y dejan ContextoInquilino resuelto acá
        // -confirmado con AislamientoTenantGraphQLIntegrationTest-. Si de todas formas no hay
        // tenant (ej. un cliente MCP/GraphQL que llama sin header ni tienda propia), es un 401,
        // no un 500: el cliente necesita mandar X-Inquilino-ID como en el resto de la API.
        UUID idTienda = ContextoInquilino.getIdTienda();
        if (idTienda == null) {
            throw new ExcepcionNoAutorizado("Falta especificar la tienda (header X-Inquilino-ID)");
        }
        return casoUsoObtenerProducto.byId(id, idTienda);
    }
}
