package com.ecommerce.modulos.compartido.infrastructure.websocket;

import com.ecommerce.modulos.identidad.application.DetallesUsuarioPersonalizado;
import com.ecommerce.modulos.identidad.application.ServicioDetallesUsuarioPersonalizado;
import com.ecommerce.modulos.compartido.infrastructure.security.ProveedorTokenJwt;
import com.ecommerce.modulos.inquilino.application.ServicioResolutorInquilino;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Autentica y autoriza el tráfico STOMP entrante.
 *
 * <p>El handshake HTTP de {@code /ws} está en la lista {@code permitAll} de {@code SecurityConfig}
 * (los navegadores no pueden mandar cabeceras en el upgrade de WebSocket), así que la seguridad
 * se aplica aquí, sobre los frames STOMP:
 * <ul>
 *   <li>{@code CONNECT}: exige un JWT válido en la cabecera nativa {@code Authorization} y fija el
 *       {@link Principal} de la sesión.</li>
 *   <li>{@code SUBSCRIBE} a {@code /topic/tienda/{idTienda}/**}: solo lo permite si el usuario
 *       autenticado es el dueño de esa tienda. Antes, cualquier suscriptor podía espiar las
 *       órdenes en tiempo real de cualquier inquilino conociendo su UUID.</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InterceptorAutenticacionWebSocket implements ChannelInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final Pattern DESTINO_POR_TIENDA =
            Pattern.compile("^/topic/tienda/([0-9a-fA-F-]{36})(?:/.*)?$");

    private final ProveedorTokenJwt proveedorTokenJwt;
    private final ServicioDetallesUsuarioPersonalizado servicioDetallesUsuario;
    private final ServicioResolutorInquilino servicioResolutorInquilino;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }

        StompCommand comando = accessor.getCommand();
        if (StompCommand.CONNECT.equals(comando)) {
            autenticar(accessor);
        } else if (StompCommand.SUBSCRIBE.equals(comando)) {
            autorizarSuscripcion(accessor);
        }
        return message;
    }

    private void autenticar(StompHeaderAccessor accessor) {
        String cabecera = accessor.getFirstNativeHeader("Authorization");
        if (cabecera == null || !cabecera.startsWith(BEARER_PREFIX)) {
            throw new MessagingException("Falta el token de autenticación en el frame CONNECT");
        }

        String jwt = cabecera.substring(BEARER_PREFIX.length());
        if (!proveedorTokenJwt.validateToken(jwt)) {
            throw new MessagingException("Token JWT inválido o expirado");
        }

        String username = proveedorTokenJwt.getUsernameFromToken(jwt);
        UserDetails userDetails = servicioDetallesUsuario.loadUserByUsername(username);

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        accessor.setUser(auth);
        log.debug("WebSocket CONNECT autenticado para {}", username);
    }

    private void autorizarSuscripcion(StompHeaderAccessor accessor) {
        String destino = accessor.getDestination();
        DetallesUsuarioPersonalizado userDetails = principalAutenticado(accessor);

        if (destino == null) {
            return;
        }

        Matcher matcher = DESTINO_POR_TIENDA.matcher(destino);
        if (!matcher.matches()) {
            // Destinos que no son por-tienda: basta con estar autenticado (ya verificado arriba).
            return;
        }

        UUID idTiendaDestino;
        try {
            idTiendaDestino = UUID.fromString(matcher.group(1));
        } catch (IllegalArgumentException e) {
            throw new MessagingException("Destino de suscripción inválido: " + destino);
        }

        UUID tiendaPropia = servicioResolutorInquilino.resolverTiendaPropia(userDetails.getUserId())
                .orElseThrow(() -> new MessagingException("No tienes una tienda registrada"));

        if (!tiendaPropia.equals(idTiendaDestino)) {
            log.warn("Usuario {} intentó suscribirse a un tópico de otra tienda ({})",
                    userDetails.getUserId(), idTiendaDestino);
            throw new MessagingException("No puedes suscribirte a tópicos de otra tienda");
        }
    }

    private DetallesUsuarioPersonalizado principalAutenticado(StompHeaderAccessor accessor) {
        Principal user = accessor.getUser();
        if (user instanceof UsernamePasswordAuthenticationToken auth
                && auth.getPrincipal() instanceof DetallesUsuarioPersonalizado userDetails) {
            return userDetails;
        }
        throw new MessagingException("Sesión WebSocket no autenticada");
    }
}
