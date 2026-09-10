package com.ecommerce.modulos.compartido.infrastructure.websocket;

import com.ecommerce.modulos.compartido.infrastructure.security.ProveedorTokenJwt;
import com.ecommerce.modulos.identidad.application.DetallesUsuarioPersonalizado;
import com.ecommerce.modulos.identidad.application.ServicioDetallesUsuarioPersonalizado;
import com.ecommerce.modulos.identidad.domain.RolUsuario;
import com.ecommerce.modulos.identidad.domain.Usuario;
import com.ecommerce.modulos.inquilino.application.ServicioResolutorInquilino;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InterceptorAutenticacionWebSocketTest {

    @Mock private ProveedorTokenJwt proveedorTokenJwt;
    @Mock private ServicioDetallesUsuarioPersonalizado servicioDetallesUsuario;
    @Mock private ServicioResolutorInquilino servicioResolutorInquilino;

    private InterceptorAutenticacionWebSocket interceptor;

    private DetallesUsuarioPersonalizado userDetails;
    private UUID userId;

    @BeforeEach
    void setUp() {
        interceptor = new InterceptorAutenticacionWebSocket(
                proveedorTokenJwt, servicioDetallesUsuario, servicioResolutorInquilino);

        userId = UUID.randomUUID();
        Usuario usuario = new Usuario();
        usuario.setId(userId);
        usuario.setCorreo("seller@test.com");
        usuario.setHashContrasena("x");
        usuario.setEnabled(true);
        usuario.setRoles(Set.of(RolUsuario.SELLER));
        userDetails = new DetallesUsuarioPersonalizado(usuario);
    }

    private Message<byte[]> frame(StompHeaderAccessor accessor) {
        // Spring's StompSubProtocolHandler entrega los mensajes al canal con headers mutables;
        // el interceptor necesita poder llamar setUser() sobre el frame CONNECT.
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private Message<byte[]> connect(String authHeader) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        if (authHeader != null) {
            accessor.setNativeHeader("Authorization", authHeader);
        }
        return frame(accessor);
    }

    private Message<byte[]> subscribe(String destino, boolean autenticado) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination(destino);
        if (autenticado) {
            accessor.setUser(new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));
        }
        return frame(accessor);
    }

    @Test
    @DisplayName("CONNECT sin token -> rechazado")
    void connectSinToken() {
        assertThatThrownBy(() -> interceptor.preSend(connect(null), null))
                .isInstanceOf(MessagingException.class);
    }

    @Test
    @DisplayName("CONNECT con token inválido -> rechazado")
    void connectTokenInvalido() {
        when(proveedorTokenJwt.validateToken("malo")).thenReturn(false);

        assertThatThrownBy(() -> interceptor.preSend(connect("Bearer malo"), null))
                .isInstanceOf(MessagingException.class);
    }

    @Test
    @DisplayName("CONNECT con token válido -> fija el Principal y deja pasar")
    void connectOk() {
        when(proveedorTokenJwt.validateToken("bueno")).thenReturn(true);
        when(proveedorTokenJwt.getUsernameFromToken("bueno")).thenReturn("seller@test.com");
        when(servicioDetallesUsuario.loadUserByUsername("seller@test.com")).thenReturn(userDetails);

        Message<?> msg = connect("Bearer bueno");
        assertThat(interceptor.preSend(msg, null)).isNotNull();

        StompHeaderAccessor out = StompHeaderAccessor.wrap(msg);
        assertThat(out.getUser()).isNotNull();
    }

    @Test
    @DisplayName("SUBSCRIBE al tópico de la propia tienda -> permitido")
    void subscribeTiendaPropia() {
        UUID tienda = UUID.randomUUID();
        when(servicioResolutorInquilino.resolverTiendaPropia(userId)).thenReturn(Optional.of(tienda));

        assertThatCode(() -> interceptor.preSend(
                subscribe("/topic/tienda/" + tienda + "/ordenes", true), null))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("SUBSCRIBE al tópico de OTRA tienda -> rechazado (fuga cross-tenant cerrada)")
    void subscribeOtraTienda() {
        UUID tiendaPropia = UUID.randomUUID();
        UUID tiendaAjena = UUID.randomUUID();
        when(servicioResolutorInquilino.resolverTiendaPropia(userId)).thenReturn(Optional.of(tiendaPropia));

        assertThatThrownBy(() -> interceptor.preSend(
                subscribe("/topic/tienda/" + tiendaAjena + "/ordenes", true), null))
                .isInstanceOf(MessagingException.class);
    }

    @Test
    @DisplayName("SUBSCRIBE de un usuario sin tienda registrada -> rechazado")
    void subscribeSinTienda() {
        UUID tienda = UUID.randomUUID();
        when(servicioResolutorInquilino.resolverTiendaPropia(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> interceptor.preSend(
                subscribe("/topic/tienda/" + tienda + "/ordenes", true), null))
                .isInstanceOf(MessagingException.class);
    }

    @Test
    @DisplayName("SUBSCRIBE sin sesión autenticada -> rechazado")
    void subscribeNoAutenticado() {
        assertThatThrownBy(() -> interceptor.preSend(
                subscribe("/topic/tienda/" + UUID.randomUUID() + "/ordenes", false), null))
                .isInstanceOf(MessagingException.class);
    }
}
