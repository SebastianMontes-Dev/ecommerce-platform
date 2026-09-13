package com.ecommerce.modulos.compartido.infrastructure;

import com.ecommerce.modulos.compartido.domain.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ManejadorExcepcionGlobalTest {

    private final ManejadorExcepcionGlobal manejador = new ManejadorExcepcionGlobal();

    private MockHttpServletRequest requestCon(String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI(uri);
        return request;
    }

    @Test
    void handleEntityNotFoundDevuelve404ConElMensajeDeLaExcepcion() {
        ExcepcionEntidadNoEncontrada ex = new ExcepcionEntidadNoEncontrada("Producto", "abc-123");

        ResponseEntity<ProblemDetail> respuesta = manejador.handleEntityNotFound(ex, requestCon("/api/v1/productos/abc-123"));

        assertEquals(HttpStatus.NOT_FOUND, respuesta.getStatusCode());
        assertEquals("Producto with id 'abc-123' not found", respuesta.getBody().getDetail());
        assertEquals("Resource Not Found", respuesta.getBody().getTitle());
        assertEquals("/api/v1/productos/abc-123", respuesta.getBody().getProperties().get("path"));
    }

    @Test
    void handleBusinessRuleViolationDevuelve422ConLasViolaciones() {
        ExcepcionViolacionReglaNegocio ex = new ExcepcionViolacionReglaNegocio(List.of("stock insuficiente", "producto descontinuado"));

        ResponseEntity<ProblemDetail> respuesta = manejador.handleBusinessRuleViolation(ex, requestCon("/api/v1/ordenes"));

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, respuesta.getStatusCode());
        assertEquals("Business Rule Violation", respuesta.getBody().getTitle());
        assertEquals(List.of("stock insuficiente", "producto descontinuado"), respuesta.getBody().getProperties().get("violations"));
    }

    @Test
    void handleUnauthorizedDevuelve401ConElMensajeDeLaExcepcion() {
        ExcepcionNoAutorizado ex = ExcepcionNoAutorizado.tokenExpired();

        ResponseEntity<ProblemDetail> respuesta = manejador.handleUnauthorized(ex, requestCon("/api/v1/pedidos"));

        assertEquals(HttpStatus.UNAUTHORIZED, respuesta.getStatusCode());
        assertEquals("Token has expired", respuesta.getBody().getDetail());
    }

    @Test
    void handleInvalidOperationDevuelve409ConElMensajeDeLaExcepcion() {
        ExcepcionOperacionInvalida ex = new ExcepcionOperacionInvalida("La orden ya fue cancelada");

        ResponseEntity<ProblemDetail> respuesta = manejador.handleInvalidOperation(ex, requestCon("/api/v1/ordenes/1/cancelar"));

        assertEquals(HttpStatus.CONFLICT, respuesta.getStatusCode());
        assertEquals("Invalid Operation", respuesta.getBody().getTitle());
        assertEquals("La orden ya fue cancelada", respuesta.getBody().getDetail());
    }

    @Test
    void handleDuplicateDevuelve409ConElMensajeDeLaExcepcion() {
        ExcepcionRecursoDuplicado ex = new ExcepcionRecursoDuplicado("Usuario", "correo", "test@test.com");

        ResponseEntity<ProblemDetail> respuesta = manejador.handleDuplicate(ex, requestCon("/api/v1/usuarios"));

        assertEquals(HttpStatus.CONFLICT, respuesta.getStatusCode());
        assertEquals("Usuario with correo 'test@test.com' already exists", respuesta.getBody().getDetail());
    }

    @Test
    void handleDataIntegrityViolationDevuelve409SinFiltrarElDetalleDeLaConstraint() {
        org.springframework.dao.DataIntegrityViolationException ex =
                new org.springframework.dao.DataIntegrityViolationException(
                        "duplicate key value violates unique constraint \"uk_cupones_tenant_codigo\"");

        ResponseEntity<ProblemDetail> respuesta = manejador.handleDataIntegrityViolation(ex, requestCon("/api/v1/cupones"));

        assertEquals(HttpStatus.CONFLICT, respuesta.getStatusCode());
        assertEquals("La operación entra en conflicto con un dato existente.", respuesta.getBody().getDetail());
        assertFalse(respuesta.getBody().getDetail().contains("constraint"));
    }

    @Test
    void handleServicioExternoDevuelve502ConMensajeGenericoSinFiltrarElDeLaCausa() {
        ExcepcionServicioExterno ex = new ExcepcionServicioExterno(
                "Timeout conectando a http://openai-interno.local:8443", new RuntimeException("boom"));

        ResponseEntity<ProblemDetail> respuesta = manejador.handleServicioExterno(ex, requestCon("/api/v1/chatbot/chat"));

        assertEquals(HttpStatus.BAD_GATEWAY, respuesta.getStatusCode());
        assertEquals("El servicio no está disponible en este momento. Intenta de nuevo más tarde.", respuesta.getBody().getDetail());
        assertFalse(respuesta.getBody().getDetail().contains("openai-interno"));
    }

    @Test
    void handleIllegalArgumentDevuelve400ConElMensajeDeLaExcepcion() {
        IllegalArgumentException ex = new IllegalArgumentException("Monto cannot be null");

        ResponseEntity<ProblemDetail> respuesta = manejador.handleIllegalArgument(ex, requestCon("/api/v1/pagos"));

        assertEquals(HttpStatus.BAD_REQUEST, respuesta.getStatusCode());
        assertEquals("Monto cannot be null", respuesta.getBody().getDetail());
    }

    @Test
    void handleValidationDevuelve400ConLosErroresDeCampoUnidos() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError errorUno = new FieldError("objeto", "correo", "El correo es obligatorio");
        FieldError errorDos = new FieldError("objeto", "nombre", "El nombre es obligatorio");
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(errorUno, errorDos));

        ResponseEntity<ProblemDetail> respuesta = manejador.handleValidation(ex, requestCon("/api/v1/usuarios"));

        assertEquals(HttpStatus.BAD_REQUEST, respuesta.getStatusCode());
        assertEquals("El correo es obligatorio; El nombre es obligatorio", respuesta.getBody().getDetail());
        assertEquals("Validation Error", respuesta.getBody().getTitle());
    }

    @Test
    void handleConstraintViolationDevuelve400ConLasViolacionesUnidas() {
        jakarta.validation.ConstraintViolationException ex = mock(jakarta.validation.ConstraintViolationException.class);
        jakarta.validation.ConstraintViolation<?> violacion = mock(jakarta.validation.ConstraintViolation.class);
        jakarta.validation.Path path = mock(jakarta.validation.Path.class);
        when(path.toString()).thenReturn("searchProducts.size");
        when(violacion.getPropertyPath()).thenReturn(path);
        when(violacion.getMessage()).thenReturn("must be less than or equal to 100");
        when(ex.getConstraintViolations()).thenReturn(java.util.Set.of(violacion));

        ResponseEntity<ProblemDetail> respuesta = manejador.handleConstraintViolation(ex, requestCon("/api/v1/busqueda"));

        assertEquals(HttpStatus.BAD_REQUEST, respuesta.getStatusCode());
        assertEquals("Validation Error", respuesta.getBody().getTitle());
        assertEquals("searchProducts.size: must be less than or equal to 100", respuesta.getBody().getDetail());
    }

    @Test
    void handleBadCredentialsDevuelve401ConMensajeGenerico() {
        BadCredentialsException ex = new BadCredentialsException("Bad credentials");

        ResponseEntity<ProblemDetail> respuesta = manejador.handleBadCredentials(ex, requestCon("/api/v1/auth/login"));

        assertEquals(HttpStatus.UNAUTHORIZED, respuesta.getStatusCode());
        assertEquals("Invalid correo or contrasena", respuesta.getBody().getDetail());
    }

    @Test
    void handleAccessDeniedDevuelve403ConMensajeGenerico() {
        AccessDeniedException ex = new AccessDeniedException("denegado");

        ResponseEntity<ProblemDetail> respuesta = manejador.handleAccessDenied(ex, requestCon("/api/v1/admin"));

        assertEquals(HttpStatus.FORBIDDEN, respuesta.getStatusCode());
        assertEquals("Access denied", respuesta.getBody().getDetail());
    }

    @Test
    void handleGeneralDevuelve500ConMensajeGenericoSinFiltrarDetalleInterno() {
        Exception ex = new RuntimeException("NullPointerException en linea 42 con datos sensibles");

        ResponseEntity<ProblemDetail> respuesta = manejador.handleGeneral(ex, requestCon("/api/v1/cualquier-cosa"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, respuesta.getStatusCode());
        assertEquals("An unexpected error occurred", respuesta.getBody().getDetail());
    }
}
