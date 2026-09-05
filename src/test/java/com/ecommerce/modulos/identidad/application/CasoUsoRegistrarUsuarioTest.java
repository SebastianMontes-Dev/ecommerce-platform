package com.ecommerce.modulos.identidad.application;

import com.ecommerce.modulos.identidad.application.dto.RespuestaUsuario;
import com.ecommerce.modulos.identidad.application.dto.SolicitudRegistro;
import com.ecommerce.modulos.identidad.domain.RepositorioUsuario;
import com.ecommerce.modulos.identidad.domain.RolUsuario;
import com.ecommerce.modulos.identidad.domain.Usuario;
import com.ecommerce.modulos.compartido.domain.ExcepcionViolacionReglaNegocio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CasoUsoRegistrarUsuarioTest {

    @Mock
    private RepositorioUsuario repositorioUsuario;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private CasoUsoRegistrarUsuario casoUsoRegistrarUsuario;

    private SolicitudRegistro request;

    @BeforeEach
    void setUp() {
        request = SolicitudRegistro.builder()
                .correo("Nuevo@Correo.com")
                .contrasena("password123")
                .confirmarContrasena("password123")
                .nombre("Ana")
                .apellido("Perez")
                .build();
    }

    @Test
    void debeLanzarExcepcionSiLasContrasenasNoCoinciden() {
        request.setConfirmarContrasena("otraPassword123");
        when(repositorioUsuario.existsByCorreo(anyString())).thenReturn(false);

        ExcepcionViolacionReglaNegocio excepcion = assertThrows(ExcepcionViolacionReglaNegocio.class,
                () -> casoUsoRegistrarUsuario.execute(request));

        assertTrue(excepcion.getViolations().contains("Las contraseñas no coinciden"));
        verify(repositorioUsuario, never()).save(any());
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void debeLanzarExcepcionSiElCorreoYaEstaRegistrado() {
        when(repositorioUsuario.existsByCorreo("Nuevo@Correo.com")).thenReturn(true);

        ExcepcionViolacionReglaNegocio excepcion = assertThrows(ExcepcionViolacionReglaNegocio.class,
                () -> casoUsoRegistrarUsuario.execute(request));

        assertTrue(excepcion.getViolations().contains("El correo ya está registrado"));
        verify(repositorioUsuario, never()).save(any());
    }

    @Test
    void debeAcumularAmbasViolacionesSiContrasenasNoCoincidenYCorreoYaExiste() {
        request.setConfirmarContrasena("otraPassword123");
        when(repositorioUsuario.existsByCorreo("Nuevo@Correo.com")).thenReturn(true);

        ExcepcionViolacionReglaNegocio excepcion = assertThrows(ExcepcionViolacionReglaNegocio.class,
                () -> casoUsoRegistrarUsuario.execute(request));

        assertEquals(2, excepcion.getViolations().size());
        verify(repositorioUsuario, never()).save(any());
    }

    @Test
    void debeRegistrarUsuarioConPasswordHasheadoYRolCustomerEnHappyPath() {
        when(repositorioUsuario.existsByCorreo("Nuevo@Correo.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hash-seguro");
        when(repositorioUsuario.save(any(Usuario.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        RespuestaUsuario respuesta = casoUsoRegistrarUsuario.execute(request);

        assertNotNull(respuesta);
        assertEquals("nuevo@correo.com", respuesta.getCorreo());
        assertTrue(respuesta.getRoles().contains(RolUsuario.CUSTOMER.name()));

        verify(passwordEncoder).encode("password123");
        verify(repositorioUsuario).save(argThat(usuario ->
                usuario.getHashContrasena().equals("hash-seguro")
                        && usuario.getCorreo().equals("nuevo@correo.com")
                        && usuario.hasRole(RolUsuario.CUSTOMER)
        ));
    }

    @Test
    void debeNormalizarCorreoAMinusculasYSinEspaciosAlRegistrar() {
        request.setCorreo("  Espacios@Correo.COM  ");
        when(repositorioUsuario.existsByCorreo("  Espacios@Correo.COM  ")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hash");
        when(repositorioUsuario.save(any(Usuario.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        RespuestaUsuario respuesta = casoUsoRegistrarUsuario.execute(request);

        assertEquals("espacios@correo.com", respuesta.getCorreo());
    }
}
