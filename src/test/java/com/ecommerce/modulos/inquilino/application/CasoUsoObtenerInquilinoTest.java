package com.ecommerce.modulos.inquilino.application;

import com.ecommerce.modulos.compartido.domain.ExcepcionEntidadNoEncontrada;
import com.ecommerce.modulos.identidad.application.DetallesUsuarioPersonalizado;
import com.ecommerce.modulos.identidad.domain.Usuario;
import com.ecommerce.modulos.inquilino.application.dto.RespuestaInquilino;
import com.ecommerce.modulos.inquilino.domain.Inquilino;
import com.ecommerce.modulos.inquilino.domain.RepositorioInquilino;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CasoUsoObtenerInquilinoTest {

    @Mock
    private RepositorioInquilino repositorioInquilino;

    @InjectMocks
    private CasoUsoObtenerInquilino casoUsoObtenerInquilino;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void autenticarComo(UUID idUsuario) {
        Usuario usuario = new Usuario("dueño@test.com", "hash", "Juan", "Perez");
        usuario.setId(idUsuario);
        DetallesUsuarioPersonalizado userDetails = new DetallesUsuarioPersonalizado(usuario);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));
    }

    @Test
    void debeDevolverInquilinoCuandoElEnlaceCortoExiste() {
        // Arrange
        UUID idPropietario = UUID.randomUUID();
        Inquilino inquilino = new Inquilino("Mi Tienda", "mi-tienda", idPropietario);
        inquilino.setId(UUID.randomUUID());
        when(repositorioInquilino.findByEnlaceCorto("mi-tienda")).thenReturn(Optional.of(inquilino));

        // Act
        RespuestaInquilino respuesta = casoUsoObtenerInquilino.bySlug("mi-tienda");

        // Assert
        assertNotNull(respuesta);
        assertEquals("mi-tienda", respuesta.getEnlaceCorto());
        assertEquals(inquilino.getId(), respuesta.getId());
        assertEquals("TRIAL", respuesta.getEstado());
    }

    @Test
    void debeLanzarExcepcionCuandoElEnlaceCortoNoExiste() {
        // Arrange
        when(repositorioInquilino.findByEnlaceCorto("no-existe")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ExcepcionEntidadNoEncontrada.class,
                () -> casoUsoObtenerInquilino.bySlug("no-existe"));
    }

    @Test
    void debeDevolverMiTiendaCuandoElUsuarioAutenticadoTieneUnaTienda() {
        // Arrange
        UUID idPropietario = UUID.randomUUID();
        autenticarComo(idPropietario);
        Inquilino inquilino = new Inquilino("Mi Tienda", "mi-tienda", idPropietario);
        inquilino.setId(UUID.randomUUID());
        when(repositorioInquilino.findByIdPropietario(idPropietario)).thenReturn(Optional.of(inquilino));

        // Act
        RespuestaInquilino respuesta = casoUsoObtenerInquilino.myTenant();

        // Assert
        assertNotNull(respuesta);
        assertEquals(idPropietario, respuesta.getIdPropietario());
    }

    @Test
    void debeLanzarExcepcionCuandoSeConsultaMiTiendaSinAutenticacion() {
        // Arrange
        SecurityContextHolder.clearContext();

        // Act & Assert
        ExcepcionEntidadNoEncontrada excepcion = assertThrows(ExcepcionEntidadNoEncontrada.class,
                () -> casoUsoObtenerInquilino.myTenant());

        assertEquals("Inquilino not found - not authenticated", excepcion.getMessage());
    }

    @Test
    void debeLanzarExcepcionCuandoElUsuarioAutenticadoNoTieneTiendaPropia() {
        // Arrange
        UUID idPropietario = UUID.randomUUID();
        autenticarComo(idPropietario);
        when(repositorioInquilino.findByIdPropietario(idPropietario)).thenReturn(Optional.empty());

        // Act & Assert
        ExcepcionEntidadNoEncontrada excepcion = assertThrows(ExcepcionEntidadNoEncontrada.class,
                () -> casoUsoObtenerInquilino.myTenant());

        assertEquals("Inquilino not found for current usuario", excepcion.getMessage());
    }
}
