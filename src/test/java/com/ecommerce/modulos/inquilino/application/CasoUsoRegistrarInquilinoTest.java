package com.ecommerce.modulos.inquilino.application;

import com.ecommerce.modulos.compartido.domain.ExcepcionViolacionReglaNegocio;
import com.ecommerce.modulos.identidad.application.DetallesUsuarioPersonalizado;
import com.ecommerce.modulos.identidad.domain.RepositorioUsuario;
import com.ecommerce.modulos.identidad.domain.RolUsuario;
import com.ecommerce.modulos.identidad.domain.Usuario;
import com.ecommerce.modulos.inquilino.application.dto.RespuestaInquilino;
import com.ecommerce.modulos.inquilino.application.dto.SolicitudRegistrarInquilino;
import com.ecommerce.modulos.inquilino.domain.Inquilino;
import com.ecommerce.modulos.inquilino.domain.PlanSuscripcion;
import com.ecommerce.modulos.inquilino.domain.RepositorioInquilino;
import com.ecommerce.modulos.inquilino.domain.RepositorioPlanSuscripcion;
import com.ecommerce.modulos.inquilino.domain.RepositorioSuscripcion;
import com.ecommerce.modulos.inquilino.domain.Suscripcion;
import com.ecommerce.modulos.inquilino.domain.TipoPlanSuscripcion;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CasoUsoRegistrarInquilinoTest {

    @Mock
    private RepositorioInquilino repositorioInquilino;
    @Mock
    private RepositorioPlanSuscripcion planRepository;
    @Mock
    private RepositorioSuscripcion repositorioSuscripcion;
    @Mock
    private RepositorioUsuario repositorioUsuario;

    @InjectMocks
    private CasoUsoRegistrarInquilino casoUsoRegistrarInquilino;

    private UUID idPropietario;
    private SolicitudRegistrarInquilino request;
    private Usuario usuarioAutenticado;

    @BeforeEach
    void setUp() {
        idPropietario = UUID.randomUUID();
        request = SolicitudRegistrarInquilino.builder()
                .nombre("Mi Tienda")
                .enlaceCorto("mi-tienda")
                .descripcion("Una tienda de prueba")
                .build();
        autenticarComo(idPropietario);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void autenticarComo(UUID idUsuario) {
        usuarioAutenticado = new Usuario("dueño@test.com", "hash", "Juan", "Perez");
        usuarioAutenticado.setId(idUsuario);
        DetallesUsuarioPersonalizado userDetails = new DetallesUsuarioPersonalizado(usuarioAutenticado);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));
    }

    @Test
    void debeRegistrarInquilinoConEstadoTrialYPlanFreeCuandoDatosSonValidos() {
        // Arrange
        when(repositorioInquilino.existsByIdPropietario(idPropietario)).thenReturn(false);
        when(repositorioInquilino.existsByEnlaceCorto("mi-tienda")).thenReturn(false);
        when(repositorioInquilino.save(any(Inquilino.class))).thenAnswer(invocacion -> invocacion.getArgument(0));
        when(repositorioUsuario.findById(idPropietario)).thenReturn(Optional.of(usuarioAutenticado));
        when(repositorioUsuario.save(any(Usuario.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        PlanSuscripcion planFree = new PlanSuscripcion();
        planFree.setId(UUID.randomUUID());
        planFree.setTipoPlan(TipoPlanSuscripcion.FREE);
        when(planRepository.findByTipoPlanAndActiveTrue(TipoPlanSuscripcion.FREE)).thenReturn(Optional.of(planFree));

        // Act
        RespuestaInquilino respuesta = casoUsoRegistrarInquilino.execute(request);

        // Assert
        assertNotNull(respuesta);
        assertEquals("mi-tienda", respuesta.getEnlaceCorto());
        assertEquals("TRIAL", respuesta.getEstado());
        assertEquals(idPropietario, respuesta.getIdPropietario());

        ArgumentCaptor<Suscripcion> captor = ArgumentCaptor.forClass(Suscripcion.class);
        verify(repositorioSuscripcion).save(captor.capture());
        Suscripcion suscripcionGuardada = captor.getValue();
        assertEquals(planFree.getId(), suscripcionGuardada.getIdPlan());
        assertEquals("ACTIVE", suscripcionGuardada.getEstado());
        assertNotNull(suscripcionGuardada.getFechaInicio());
    }

    @Test
    void debeAsignarRolSellerAlPropietarioAlRegistrarSuPrimeraTienda() {
        // Arrange
        when(repositorioInquilino.existsByIdPropietario(idPropietario)).thenReturn(false);
        when(repositorioInquilino.existsByEnlaceCorto("mi-tienda")).thenReturn(false);
        when(repositorioInquilino.save(any(Inquilino.class))).thenAnswer(invocacion -> invocacion.getArgument(0));
        when(repositorioUsuario.findById(idPropietario)).thenReturn(Optional.of(usuarioAutenticado));
        when(repositorioUsuario.save(any(Usuario.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        PlanSuscripcion planFree = new PlanSuscripcion();
        planFree.setId(UUID.randomUUID());
        planFree.setTipoPlan(TipoPlanSuscripcion.FREE);
        when(planRepository.findByTipoPlanAndActiveTrue(TipoPlanSuscripcion.FREE)).thenReturn(Optional.of(planFree));

        // Act
        casoUsoRegistrarInquilino.execute(request);

        // Assert
        assertTrue(usuarioAutenticado.hasRole(RolUsuario.SELLER));
        verify(repositorioUsuario).save(usuarioAutenticado);
    }

    @Test
    void debeLanzarExcepcionCuandoElUsuarioYaTieneUnaTiendaRegistrada() {
        // Arrange
        when(repositorioInquilino.existsByIdPropietario(idPropietario)).thenReturn(true);

        // Act & Assert
        ExcepcionViolacionReglaNegocio excepcion = assertThrows(ExcepcionViolacionReglaNegocio.class,
                () -> casoUsoRegistrarInquilino.execute(request));

        assertEquals("You already have a store registered", excepcion.getMessage());
        verify(repositorioInquilino, never()).save(any());
        verify(repositorioSuscripcion, never()).save(any());
    }

    @Test
    void debeLanzarExcepcionCuandoElEnlaceCortoYaEstaTomado() {
        // Arrange
        when(repositorioInquilino.existsByIdPropietario(idPropietario)).thenReturn(false);
        when(repositorioInquilino.existsByEnlaceCorto("mi-tienda")).thenReturn(true);

        // Act & Assert
        ExcepcionViolacionReglaNegocio excepcion = assertThrows(ExcepcionViolacionReglaNegocio.class,
                () -> casoUsoRegistrarInquilino.execute(request));

        assertEquals("Store enlaceCorto is already taken", excepcion.getMessage());
        verify(repositorioInquilino, never()).save(any());
        verify(repositorioSuscripcion, never()).save(any());
    }

    @Test
    void debeLanzarExcepcionCuandoNoHayUsuarioAutenticado() {
        // Arrange
        SecurityContextHolder.clearContext();

        // Act & Assert
        ExcepcionViolacionReglaNegocio excepcion = assertThrows(ExcepcionViolacionReglaNegocio.class,
                () -> casoUsoRegistrarInquilino.execute(request));

        assertEquals("You must be authenticated to register a store", excepcion.getMessage());
        verifyNoInteractions(repositorioInquilino);
    }

    @Test
    void debeLanzarIllegalStateExceptionCuandoNoExisteUnPlanFreeActivo() {
        // Arrange
        when(repositorioInquilino.existsByIdPropietario(idPropietario)).thenReturn(false);
        when(repositorioInquilino.existsByEnlaceCorto("mi-tienda")).thenReturn(false);
        when(repositorioInquilino.save(any(Inquilino.class))).thenAnswer(invocacion -> invocacion.getArgument(0));
        when(repositorioUsuario.findById(idPropietario)).thenReturn(Optional.of(usuarioAutenticado));
        when(planRepository.findByTipoPlanAndActiveTrue(TipoPlanSuscripcion.FREE)).thenReturn(Optional.empty());

        // Act & Assert
        IllegalStateException excepcion = assertThrows(IllegalStateException.class,
                () -> casoUsoRegistrarInquilino.execute(request));

        assertEquals("Default FREE plan not found", excepcion.getMessage());
        verify(repositorioSuscripcion, never()).save(any());
    }
}
