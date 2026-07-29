package com.ecommerce.modulos.carrito.infrastructure;

import com.ecommerce.modulos.carrito.application.ServicioCarrito;
import com.ecommerce.modulos.carrito.domain.ArticuloCarrito;
import com.ecommerce.modulos.carrito.domain.Carrito;
import com.ecommerce.modulos.identidad.application.DetallesUsuarioPersonalizado;
import com.ecommerce.modulos.ordenes.application.CasoUsoGestionarCupon;
import com.ecommerce.modulos.ordenes.domain.Cupon;
import com.ecommerce.modulos.ordenes.domain.TipoDescuento;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ControladorCarrito.class)
@AutoConfigureMockMvc(addFilters = false) // Disable security filters for simple unit test
class ControladorCarritoTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ServicioCarrito servicioCarrito;

    @MockBean
    private CasoUsoGestionarCupon casoUsoGestionarCupon;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID userId;
    private DetallesUsuarioPersonalizado userDetails;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        com.ecommerce.modulos.identidad.domain.Usuario usuario = new com.ecommerce.modulos.identidad.domain.Usuario();
        usuario.setId(userId);
        usuario.setCorreo("user@test.com");
        usuario.setHashContrasena("password");
        usuario.setEnabled(true);
        usuario.setRoles(java.util.Set.of(com.ecommerce.modulos.identidad.domain.RolUsuario.CUSTOMER));
        
        userDetails = new DetallesUsuarioPersonalizado(usuario);
    }

    @Test
    void debeObtenerCarritoParaUsuarioAutenticado() throws Exception {
        Carrito carrito = new Carrito();
        carrito.setId("carrito:" + userId);

        when(servicioCarrito.getOrCreateCart(eq(userId), any())).thenReturn(carrito);

        mockMvc.perform(get("/api/v1/carrito")
                        .with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("carrito:" + userId));
    }

    @Test
    void debeAgregarArticuloParaUsuarioAutenticado() throws Exception {
        ArticuloCarrito item = new ArticuloCarrito();
        item.setIdProducto(UUID.randomUUID());
        item.setCantidad(2);

        Carrito carrito = new Carrito();
        carrito.agregarArticulo(item);

        when(servicioCarrito.agregarArticulo(eq(userId), any(), any())).thenReturn(carrito);

        mockMvc.perform(post("/api/v1/carrito/articulos")
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(item)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.articulos[0].cantidad").value(2));
    }

    @Test
    void debeAplicarCupon() throws Exception {
        String codigoCupon = "DESC10";
        Cupon cupon = new Cupon();
        cupon.setCodigo(codigoCupon);
        cupon.setTipo(TipoDescuento.MONTO_FIJO);
        cupon.setValor(new BigDecimal("10.00"));

        Carrito carrito = new Carrito();
        ArticuloCarrito item = new ArticuloCarrito();
        item.setPrecioUnitario(new BigDecimal("50.00"));
        item.setCantidad(1);
        carrito.agregarArticulo(item);

        when(casoUsoGestionarCupon.validarYObtenerCupon(any(), eq(codigoCupon))).thenReturn(cupon);
        when(servicioCarrito.getOrCreateCart(eq(userId), any())).thenReturn(carrito);
        when(servicioCarrito.aplicarCupon(eq(userId), any(), eq(codigoCupon), any())).thenReturn(carrito);

        mockMvc.perform(post("/api/v1/carrito/cupones/{codigo}", codigoCupon)
                        .with(user(userDetails)))
                .andExpect(status().isOk());
                
        verify(servicioCarrito).aplicarCupon(eq(userId), any(), eq(codigoCupon), eq(new BigDecimal("10.00")));
    }
}
