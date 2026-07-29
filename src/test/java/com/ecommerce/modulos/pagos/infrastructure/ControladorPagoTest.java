package com.ecommerce.modulos.pagos.infrastructure;

import com.ecommerce.modulos.identidad.application.DetallesUsuarioPersonalizado;
import com.ecommerce.modulos.pagos.application.CasoUsoProcesarPago;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ControladorPago.class)
@AutoConfigureMockMvc(addFilters = false)
class ControladorPagoTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CasoUsoProcesarPago casoUsoProcesarPago;

    private UUID userId;
    private DetallesUsuarioPersonalizado userDetails;
    private UUID idOrden;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        com.ecommerce.modulos.identidad.domain.Usuario usuario = new com.ecommerce.modulos.identidad.domain.Usuario();
        usuario.setId(userId);
        usuario.setCorreo("pago@test.com");
        usuario.setHashContrasena("password");
        usuario.setEnabled(true);
        usuario.setRoles(java.util.Set.of(com.ecommerce.modulos.identidad.domain.RolUsuario.CUSTOMER));
        
        userDetails = new DetallesUsuarioPersonalizado(usuario);
        idOrden = UUID.randomUUID();
    }

    @Test
    void debeIniciarProcesoPago() throws Exception {
        Map<String, Object> respuestaEsperada = Map.of(
                "paymentId", UUID.randomUUID().toString(),
                "checkoutUrl", "http://checkout.url"
        );

        when(casoUsoProcesarPago.ejecutar(idOrden, userId)).thenReturn(respuestaEsperada);

        mockMvc.perform(post("/api/v1/pagos/iniciar/{idOrden}", idOrden)
                        .with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.checkoutUrl").value("http://checkout.url"));
                
        verify(casoUsoProcesarPago).ejecutar(idOrden, userId);
    }
}
