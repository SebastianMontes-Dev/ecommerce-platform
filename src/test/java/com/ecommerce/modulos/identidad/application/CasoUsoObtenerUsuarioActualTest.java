package com.ecommerce.modulos.identidad.application;

import com.ecommerce.modulos.identidad.application.dto.RespuestaUsuario;
import com.ecommerce.modulos.identidad.domain.RepositorioUsuario;
import com.ecommerce.modulos.identidad.domain.RolUsuario;
import com.ecommerce.modulos.identidad.domain.Usuario;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CasoUsoObtenerUsuarioActualTest {

    @Mock
    private RepositorioUsuario repositorioUsuario;

    @InjectMocks
    private CasoUsoObtenerUsuarioActual casoUsoObtenerUsuarioActual;

    @AfterEach
    void limpiarContextoDeSeguridad() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void debeRetornarUsuarioActualSiEstaAutenticado() {
        UUID idUsuario = UUID.randomUUID();
        Usuario usuario = new Usuario("actual@correo.com", "hash", "Nombre", "Apellido");
        usuario.addRole(RolUsuario.CUSTOMER);

        DetallesUsuarioPersonalizado userDetails = crearDetalles(idUsuario, usuario);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));
        when(repositorioUsuario.findById(idUsuario)).thenReturn(Optional.of(usuario));

        RespuestaUsuario respuesta = casoUsoObtenerUsuarioActual.execute();

        assertNotNull(respuesta);
        assertEquals("actual@correo.com", respuesta.getCorreo());
    }

    @Test
    void debeRetornarNullSiNoHayAutenticacion() {
        SecurityContextHolder.getContext().setAuthentication(null);

        RespuestaUsuario respuesta = casoUsoObtenerUsuarioActual.execute();

        assertNull(respuesta);
    }

    @Test
    void debeRetornarNullSiElPrincipalNoEsDetallesUsuarioPersonalizado() {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("anonymousUser", null));

        RespuestaUsuario respuesta = casoUsoObtenerUsuarioActual.execute();

        assertNull(respuesta);
    }

    @Test
    void debeRetornarNullSiElUsuarioAutenticadoYaNoExisteEnElRepositorio() {
        UUID idUsuario = UUID.randomUUID();
        Usuario usuario = new Usuario("fantasma@correo.com", "hash", "Nombre", "Apellido");
        DetallesUsuarioPersonalizado userDetails = crearDetalles(idUsuario, usuario);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));
        when(repositorioUsuario.findById(idUsuario)).thenReturn(Optional.empty());

        RespuestaUsuario respuesta = casoUsoObtenerUsuarioActual.execute();

        assertNull(respuesta);
    }

    /**
     * DetallesUsuarioPersonalizado deriva su id del id del Usuario recibido (via getId()),
     * asi que para fijar un UUID conocido en el test se usa un Usuario cuyo id ya fue
     * asignado a traves de reflexion minima (setId es expuesto por Lombok @Setter en EntidadBase).
     */
    private static DetallesUsuarioPersonalizado crearDetalles(UUID idUsuario, Usuario usuario) {
        usuario.setId(idUsuario);
        return new DetallesUsuarioPersonalizado(usuario);
    }
}
