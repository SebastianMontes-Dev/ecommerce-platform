# Aislamiento Multi-Tenant — Plan de Remediación

> **Completado y mergeado.** Las 10 tareas de este plan (más una ola de fixes de la revisión final) se implementaron en la rama `worktree-aislamiento-multi-tenant` y se mergearon a `master` en el PR #1 (`fd95fb3`). Archivado aquí como referencia histórica — no requiere más acción.

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Cerrar la vulnerabilidad crítica de aislamiento multi-tenant: hoy el `idTienda` de cada request sale de un header controlado por el cliente (`X-Inquilino-ID`) sin validarlo contra el usuario autenticado, y el filtro `@Filter` de Hibernate que debería ser la defensa de fondo nunca se activa. Cualquier usuario autenticado puede leer/escribir datos de otro tenant.

**Architecture:** Se resuelve el `idTienda` "de confianza" del vendedor autenticado consultando `Inquilino.idPropietario` (relación que ya existe), cacheado en Redis. `FiltroInquilino` usa ese valor cuando existe y solo cae al header para usuarios sin tienda propia (compra/navegación de clientes). Los endpoints de gestión de tienda (crear producto, cupones, analíticas, etc.) se cambian para exigir explícitamente el valor de confianza, nunca el header. Además se activa el filtro `@Filter` de Hibernate en cada acceso a repositorio vía un aspecto AOP, como defensa de fondo.

**Tech Stack:** Java 21, Spring Boot 3.4.4, Spring Security, Spring Data JPA (Hibernate 6), Redis (cache), Spring AOP, JUnit 5 + Mockito + Testcontainers (Postgres).

**Spec:** Este plan documenta su propio spec (no hay documento separado) — surgió de una auditoría de seguridad y arquitectura completa del proyecto realizada en esta sesión. Los hallazgos que motivan cada tarea están citados en cada tarea con archivo:línea.

## Global Constraints

- Mantener la convención de nombres en español del proyecto (`ServicioX`, `CasoUsoX`, `ControladorX`, `RepositorioX`) — ver `CLAUDE.md`.
- No introducir dependencias nuevas de gestor de paquetes (solo se agrega `spring-boot-starter-aop`, que es un starter oficial de Spring Boot, ya usa Gradle/Maven Central existente).
- Todas las clases nuevas siguen la estructura de paquetes `modulos/<dominio>/{domain,application,infrastructure}` ya establecida.
- No modificar el comportamiento de navegación pública de clientes (catálogo GET, búsqueda, carrito, checkout) — el header `X-Inquilino-ID` sigue siendo válido para esos flujos porque un `CUSTOMER` no es dueño de ningún `Inquilino`.
- Commits en español, uno por tarea completada (siguiendo el estilo ya usado en el repo — mensajes cortos y descriptivos).

---

### Task 1: `ServicioResolutorInquilino` — resolución cacheada de la tienda propia del usuario

**Contexto del hallazgo:** No existe ningún mecanismo que derive el tenant de forma confiable desde el usuario autenticado. La única relación real usuario→tienda es `Inquilino.idPropietario` (`src/main/java/com/ecommerce/modulos/inquilino/domain/Inquilino.java:45-46`), usada hoy solo en `CasoUsoRegistrarInquilino`.

**Files:**
- Create: `src/main/java/com/ecommerce/modulos/inquilino/application/ServicioResolutorInquilino.java`
- Modify: `src/main/java/com/ecommerce/modulos/compartido/infrastructure/RedisConfig.java`
- Test: `src/test/java/com/ecommerce/modulos/inquilino/application/ServicioResolutorInquilinoTest.java`

**Interfaces:**
- Produces: `ServicioResolutorInquilino.resolverTiendaPropia(UUID idUsuario) -> Optional<UUID>` — usado por Task 3 (`FiltroInquilino`).
- Consumes: `RepositorioInquilino.findByIdPropietario(UUID) -> Optional<Inquilino>` (ya existe, `RepositorioInquilino.java:14`).

- [ ] **Step 1: Escribir el test unitario que falla**

```java
package com.ecommerce.modulos.inquilino.application;

import com.ecommerce.modulos.inquilino.domain.Inquilino;
import com.ecommerce.modulos.inquilino.domain.RepositorioInquilino;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServicioResolutorInquilinoTest {

    @Mock
    private RepositorioInquilino repositorioInquilino;

    @InjectMocks
    private ServicioResolutorInquilino servicioResolutorInquilino;

    @Test
    void debeDevolverIdTiendaCuandoElUsuarioEsPropietario() {
        UUID idUsuario = UUID.randomUUID();
        Inquilino inquilino = new Inquilino("Mi Tienda", "mi-tienda", idUsuario);
        when(repositorioInquilino.findByIdPropietario(idUsuario)).thenReturn(Optional.of(inquilino));

        Optional<UUID> resultado = servicioResolutorInquilino.resolverTiendaPropia(idUsuario);

        assertTrue(resultado.isPresent());
        assertEquals(inquilino.getId(), resultado.get());
    }

    @Test
    void debeDevolverVacioCuandoElUsuarioNoTieneTiendaPropia() {
        UUID idUsuario = UUID.randomUUID();
        when(repositorioInquilino.findByIdPropietario(idUsuario)).thenReturn(Optional.empty());

        Optional<UUID> resultado = servicioResolutorInquilino.resolverTiendaPropia(idUsuario);

        assertTrue(resultado.isEmpty());
    }
}
```

- [ ] **Step 2: Correr el test y verificar que falla**

Run: `./gradlew test --tests "com.ecommerce.modulos.inquilino.application.ServicioResolutorInquilinoTest"`
Expected: FAIL — no existe la clase `ServicioResolutorInquilino`.

- [ ] **Step 3: Implementar `ServicioResolutorInquilino`**

```java
package com.ecommerce.modulos.inquilino.application;

import com.ecommerce.modulos.inquilino.domain.Inquilino;
import com.ecommerce.modulos.inquilino.domain.RepositorioInquilino;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * Resuelve la tienda que un usuario posee (Inquilino.idPropietario) de forma
 * cacheada. Es la única fuente confiable de idTienda para acciones de
 * gestión de tienda — nunca debe sustituirse por un valor provisto por el cliente.
 */
@Service
@RequiredArgsConstructor
public class ServicioResolutorInquilino {

    private final RepositorioInquilino repositorioInquilino;

    @Cacheable(cacheNames = "tenant-por-propietario", key = "#idUsuario", unless = "#result == null || !#result.isPresent()")
    public Optional<UUID> resolverTiendaPropia(UUID idUsuario) {
        return repositorioInquilino.findByIdPropietario(idUsuario).map(Inquilino::getId);
    }
}
```

- [ ] **Step 4: Correr el test y verificar que pasa**

Run: `./gradlew test --tests "com.ecommerce.modulos.inquilino.application.ServicioResolutorInquilinoTest"`
Expected: PASS

- [ ] **Step 5: Agregar TTL corto para el cache `tenant-por-propietario` en `RedisConfig`**

En `src/main/java/com/ecommerce/modulos/compartido/infrastructure/RedisConfig.java`, reemplazar el bean `cacheManager`:

```java
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory, ObjectMapper objectMapper) {
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer))
                .disableCachingNullValues();

        RedisCacheConfiguration tenantOwnershipConfig = config.entryTtl(Duration.ofMinutes(2));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .withCacheConfiguration("tenant-por-propietario", tenantOwnershipConfig)
                .build();
    }
```

TTL corto (2 min) porque este valor decide autorización: si un dueño transfiere/pierde su tienda, el request siguiente no debe seguir confiando en el valor viejo por 10 minutos completos.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/ecommerce/modulos/inquilino/application/ServicioResolutorInquilino.java src/main/java/com/ecommerce/modulos/compartido/infrastructure/RedisConfig.java src/test/java/com/ecommerce/modulos/inquilino/application/ServicioResolutorInquilinoTest.java
git commit -m "feat: agregar resolución cacheada de tienda propia por usuario"
```

---

### Task 2: `ContextoInquilino` — distinguir tenant "de confianza" del tenant "por header"

**Contexto del hallazgo:** `ContextoInquilino` (`src/main/java/com/ecommerce/modulos/compartido/infrastructure/ContextoInquilino.java`) hoy expone un solo valor sin indicar su origen. Los endpoints de gestión de tienda necesitan poder exigir explícitamente el valor de confianza y fallar si no existe, en vez de aceptar silenciosamente lo que venga del header.

**Files:**
- Modify: `src/main/java/com/ecommerce/modulos/compartido/infrastructure/ContextoInquilino.java`
- Test: `src/test/java/com/ecommerce/modulos/compartido/infrastructure/ContextoInquilinoTest.java`

**Interfaces:**
- Produces: `ContextoInquilino.setIdTiendaPropia(UUID)`, `ContextoInquilino.getIdTiendaPropia() -> UUID` (lanza `ExcepcionNoAutorizado` si es null) — usado por Task 3 y Task 5.
- Consumes: `ExcepcionNoAutorizado` (`com.ecommerce.modulos.compartido.domain.ExcepcionNoAutorizado`, ya existe).

- [ ] **Step 1: Escribir el test que falla**

```java
package com.ecommerce.modulos.compartido.infrastructure;

import com.ecommerce.modulos.compartido.domain.ExcepcionNoAutorizado;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ContextoInquilinoTest {

    @AfterEach
    void limpiar() {
        ContextoInquilino.clear();
    }

    @Test
    void getIdTiendaPropiaDevuelveElValorConfiable() {
        UUID idTienda = UUID.randomUUID();
        ContextoInquilino.setIdTiendaPropia(idTienda);

        assertEquals(idTienda, ContextoInquilino.getIdTiendaPropia());
    }

    @Test
    void getIdTiendaPropiaLanzaExcepcionSiNoHayTiendaPropia() {
        assertThrows(ExcepcionNoAutorizado.class, ContextoInquilino::getIdTiendaPropia);
    }

    @Test
    void clearLimpiaAmbosValores() {
        ContextoInquilino.setIdTienda(UUID.randomUUID());
        ContextoInquilino.setIdTiendaPropia(UUID.randomUUID());

        ContextoInquilino.clear();

        assertNull(ContextoInquilino.getIdTienda());
        assertThrows(ExcepcionNoAutorizado.class, ContextoInquilino::getIdTiendaPropia);
    }
}
```

- [ ] **Step 2: Correr el test y verificar que falla**

Run: `./gradlew test --tests "com.ecommerce.modulos.compartido.infrastructure.ContextoInquilinoTest"`
Expected: FAIL — no existen `setIdTiendaPropia`/`getIdTiendaPropia`.

- [ ] **Step 3: Reescribir `ContextoInquilino`**

Reemplazar el contenido completo de `src/main/java/com/ecommerce/modulos/compartido/infrastructure/ContextoInquilino.java`:

```java
package com.ecommerce.modulos.compartido.infrastructure;

import com.ecommerce.modulos.compartido.domain.ExcepcionNoAutorizado;

import java.util.UUID;

public final class ContextoInquilino {

    private static final ThreadLocal<UUID> CURRENT_TENANT = new ThreadLocal<>();
    private static final ThreadLocal<UUID> OWNED_TENANT = new ThreadLocal<>();

    private ContextoInquilino() {}

    public static void setIdTienda(UUID idTienda) {
        CURRENT_TENANT.set(idTienda);
    }

    public static UUID getIdTienda() {
        return CURRENT_TENANT.get();
    }

    /**
     * Tienda que el usuario autenticado posee (Inquilino.idPropietario), resuelta
     * server-side vía ServicioResolutorInquilino. Nunca proviene de un header.
     */
    public static void setIdTiendaPropia(UUID idTienda) {
        OWNED_TENANT.set(idTienda);
    }

    /**
     * Para endpoints de gestión de tienda. Lanza si el usuario autenticado no
     * es dueño de ninguna tienda — nunca cae de vuelta al header del cliente.
     */
    public static UUID getIdTiendaPropia() {
        UUID idTienda = OWNED_TENANT.get();
        if (idTienda == null) {
            throw new ExcepcionNoAutorizado("No tienes una tienda registrada");
        }
        return idTienda;
    }

    public static void clear() {
        CURRENT_TENANT.remove();
        OWNED_TENANT.remove();
    }
}
```

- [ ] **Step 4: Correr el test y verificar que pasa**

Run: `./gradlew test --tests "com.ecommerce.modulos.compartido.infrastructure.ContextoInquilinoTest"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/ecommerce/modulos/compartido/infrastructure/ContextoInquilino.java src/test/java/com/ecommerce/modulos/compartido/infrastructure/ContextoInquilinoTest.java
git commit -m "feat: distinguir tenant de confianza del tenant por header en ContextoInquilino"
```

---

### Task 3: `FiltroInquilino` — resolver el tenant de confianza en vez de confiar en el header

**Contexto del hallazgo:** `FiltroInquilino.extractTenantId` (`src/main/java/com/ecommerce/modulos/compartido/infrastructure/FiltroInquilino.java:41-57`) intenta primero `auth.getDetails() instanceof PrincipalInquilino`, pero `PrincipalInquilino` nunca se instancia en ningún lugar del código (confirmado por búsqueda global) — `FiltroAutenticacionJwt.java:43` solo pone `WebAuthenticationDetails` estándar. Resultado: siempre cae al header `X-Inquilino-ID`, controlado 100% por el cliente, sin cruzarlo contra el usuario autenticado.

**Files:**
- Modify: `src/main/java/com/ecommerce/modulos/compartido/infrastructure/FiltroInquilino.java`
- Test: `src/test/java/com/ecommerce/modulos/compartido/infrastructure/FiltroInquilinoTest.java`

**Interfaces:**
- Consumes: `ServicioResolutorInquilino.resolverTiendaPropia(UUID) -> Optional<UUID>` (Task 1), `ContextoInquilino.setIdTienda/setIdTiendaPropia` (Task 2), `DetallesUsuarioPersonalizado.getUserId() -> UUID` (ya existe, `src/main/java/com/ecommerce/modulos/identidad/application/DetallesUsuarioPersonalizado.java:31`).

- [ ] **Step 1: Escribir el test que falla**

```java
package com.ecommerce.modulos.compartido.infrastructure;

import com.ecommerce.modulos.identidad.application.DetallesUsuarioPersonalizado;
import com.ecommerce.modulos.identidad.domain.Usuario;
import com.ecommerce.modulos.inquilino.application.ServicioResolutorInquilino;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FiltroInquilinoTest {

    @Mock
    private ServicioResolutorInquilino servicioResolutorInquilino;

    @InjectMocks
    private FiltroInquilino filtroInquilino;

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
        ContextoInquilino.clear();
    }

    @Test
    void ignoraElHeaderSpoofeadoCuandoElUsuarioTieneTiendaPropia() throws Exception {
        UUID idUsuario = UUID.randomUUID();
        UUID idTiendaPropia = UUID.randomUUID();
        UUID idTiendaSpoofeada = UUID.randomUUID();

        Usuario usuario = new Usuario();
        usuario.setId(idUsuario);
        usuario.setCorreo("vendedor@test.com");
        DetallesUsuarioPersonalizado principal = new DetallesUsuarioPersonalizado(usuario);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

        when(servicioResolutorInquilino.resolverTiendaPropia(idUsuario)).thenReturn(Optional.of(idTiendaPropia));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Inquilino-ID", idTiendaSpoofeada.toString());
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        final UUID[] idTiendaDuranteRequest = new UUID[1];
        final UUID[] idTiendaPropiaDuranteRequest = new UUID[1];
        doAnswer(invocation -> {
            idTiendaDuranteRequest[0] = ContextoInquilino.getIdTienda();
            idTiendaPropiaDuranteRequest[0] = ContextoInquilino.getIdTiendaPropia();
            return null;
        }).when(chain).doFilter(request, response);

        filtroInquilino.doFilter(request, response, chain);

        assertEquals(idTiendaPropia, idTiendaDuranteRequest[0], "el header spoofeado no debe ganar sobre la tienda propia");
        assertEquals(idTiendaPropia, idTiendaPropiaDuranteRequest[0]);
    }

    @Test
    void usaElHeaderCuandoElUsuarioNoTieneTiendaPropia() throws Exception {
        UUID idUsuario = UUID.randomUUID();
        UUID idTiendaHeader = UUID.randomUUID();

        Usuario usuario = new Usuario();
        usuario.setId(idUsuario);
        usuario.setCorreo("cliente@test.com");
        DetallesUsuarioPersonalizado principal = new DetallesUsuarioPersonalizado(usuario);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

        when(servicioResolutorInquilino.resolverTiendaPropia(idUsuario)).thenReturn(Optional.empty());

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Inquilino-ID", idTiendaHeader.toString());
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        final UUID[] idTiendaDuranteRequest = new UUID[1];
        doAnswer(invocation -> {
            idTiendaDuranteRequest[0] = ContextoInquilino.getIdTienda();
            return null;
        }).when(chain).doFilter(request, response);

        filtroInquilino.doFilter(request, response, chain);

        assertEquals(idTiendaHeader, idTiendaDuranteRequest[0]);
    }
}
```

- [ ] **Step 2: Correr el test y verificar que falla**

Run: `./gradlew test --tests "com.ecommerce.modulos.compartido.infrastructure.FiltroInquilinoTest"`
Expected: FAIL — `FiltroInquilino` no tiene constructor que reciba `ServicioResolutorInquilino`, y el comportamiento actual siempre usa el header.

- [ ] **Step 3: Reescribir `FiltroInquilino`**

Reemplazar el contenido completo de `src/main/java/com/ecommerce/modulos/compartido/infrastructure/FiltroInquilino.java`:

```java
package com.ecommerce.modulos.compartido.infrastructure;

import com.ecommerce.modulos.identidad.application.DetallesUsuarioPersonalizado;
import com.ecommerce.modulos.inquilino.application.ServicioResolutorInquilino;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@Component
@Order(1)
@RequiredArgsConstructor
public class FiltroInquilino extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(FiltroInquilino.class);
    private static final String TENANT_HEADER = "X-Inquilino-ID";

    private final ServicioResolutorInquilino servicioResolutorInquilino;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            Optional<UUID> idTiendaPropia = resolverTiendaPropiaDelUsuarioAutenticado();
            idTiendaPropia.ifPresent(ContextoInquilino::setIdTiendaPropia);

            UUID idTienda = idTiendaPropia.orElseGet(() -> extractTenantIdFromHeader(request));
            if (idTienda != null) {
                ContextoInquilino.setIdTienda(idTienda);
                log.debug("Inquilino context set to: {}", idTienda);
            }
            filterChain.doFilter(request, response);
        } finally {
            ContextoInquilino.clear();
        }
    }

    private Optional<UUID> resolverTiendaPropiaDelUsuarioAutenticado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof DetallesUsuarioPersonalizado userDetails) {
            return servicioResolutorInquilino.resolverTiendaPropia(userDetails.getUserId());
        }
        return Optional.empty();
    }

    private UUID extractTenantIdFromHeader(HttpServletRequest request) {
        String header = request.getHeader(TENANT_HEADER);
        if (header != null && !header.isBlank()) {
            try {
                return UUID.fromString(header);
            } catch (IllegalArgumentException e) {
                log.debug("Invalid X-Inquilino-ID header format: {}", header);
            }
        }
        return null;
    }
}
```

Nota: `PrincipalInquilino` deja de usarse aquí — se elimina en el Task 4.

- [ ] **Step 4: Correr el test y verificar que pasa**

Run: `./gradlew test --tests "com.ecommerce.modulos.compartido.infrastructure.FiltroInquilinoTest"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/ecommerce/modulos/compartido/infrastructure/FiltroInquilino.java src/test/java/com/ecommerce/modulos/compartido/infrastructure/FiltroInquilinoTest.java
git commit -m "fix: resolver idTienda de forma confiable en vez de confiar en el header X-Inquilino-ID"
```

---

### Task 4: Eliminar `PrincipalInquilino` (código muerto)

**Contexto:** `PrincipalInquilino` (`src/main/java/com/ecommerce/modulos/compartido/infrastructure/PrincipalInquilino.java`) queda sin ningún uso tras el Task 3 — nunca se instanciaba en producción, era el mecanismo roto que este plan reemplaza.

**Files:**
- Delete: `src/main/java/com/ecommerce/modulos/compartido/infrastructure/PrincipalInquilino.java`

- [ ] **Step 1: Confirmar que no queda ninguna referencia**

Run: `grep -r "PrincipalInquilino" src/main/java src/test/java`
Expected: sin resultados (ya se reescribió `FiltroInquilino` en el Task 3).

- [ ] **Step 2: Eliminar el archivo**

```bash
git rm src/main/java/com/ecommerce/modulos/compartido/infrastructure/PrincipalInquilino.java
```

- [ ] **Step 3: Compilar para confirmar que nada más lo referenciaba**

Run: `./gradlew compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git commit -m "chore: eliminar PrincipalInquilino, reemplazado por ServicioResolutorInquilino"
```

---

### Task 5: Endpoints de gestión de tienda usan `getIdTiendaPropia()` en vez de `getIdTienda()`

**Contexto del hallazgo:** Con el Task 3, `ContextoInquilino.getIdTienda()` ya no puede spoofearse para vendedores reales — pero un `CUSTOMER` (o cualquier usuario sin tienda propia) todavía puede llamar a estos endpoints y su `idTienda` seguiría cayendo al header, porque `getIdTienda()` conserva el fallback. Los endpoints de **gestión de tienda** (no de navegación de cliente) deben exigir el valor de confianza y fallar con 401 si no existe.

**Files:**
- Modify: `src/main/java/com/ecommerce/modulos/catalogo/infrastructure/ControladorCatalogo.java`
- Modify: `src/main/java/com/ecommerce/modulos/catalogo/infrastructure/ControladorProducto.java`
- Modify: `src/main/java/com/ecommerce/modulos/ordenes/infrastructure/ControladorCupon.java`
- Modify: `src/main/java/com/ecommerce/modulos/analiticas/infrastructure/ControladorAnaliticas.java`
- Modify: `src/main/java/com/ecommerce/modulos/ordenes/infrastructure/ControladorOrden.java`
- Modify: `src/main/java/com/ecommerce/modulos/logistica/infrastructure/ControladorLogistica.java`

**Interfaces:**
- Consumes: `ContextoInquilino.getIdTiendaPropia()` (Task 2).

**Nota de alcance:** `ControladorOrden.createOrder`, `.getOrder`, `.cancelOrder` y `ControladorLogistica.rastrearEnvio` **no cambian** — son acciones de cliente (compra/consulta de su propia orden, rastreo de un envío por número de guía), donde elegir "en qué tienda estoy" vía header sigue siendo el comportamiento correcto. El Task 6 cierra el hueco real de `cancelOrder` (falta de verificación de tenant) sin tocar la fuente del `idTienda`.

- [ ] **Step 1: `ControladorCatalogo` — categorías y productos**

En `src/main/java/com/ecommerce/modulos/catalogo/infrastructure/ControladorCatalogo.java`, reemplazar:

```java
    @PostMapping("/categorias")
    @Operation(summary = "Crear una categoría")
    @CacheEvict(value = "categorias", key = "T(com.ecommerce.modulos.compartido.infrastructure.ContextoInquilino).getIdTienda()")
    public ResponseEntity<RespuestaCategoria> createCategory(@Valid @RequestBody SolicitudCrearCategoria request) {
        RespuestaCategoria response = casoUsoCrearCategoria.execute(request, ContextoInquilino.getIdTienda());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/categorias")
    @Operation(summary = "Listar categorías")
    @Cacheable(value = "categorias", key = "T(com.ecommerce.modulos.compartido.infrastructure.ContextoInquilino).getIdTienda()")
    public ResponseEntity<List<RespuestaCategoria>> listCategories() {
        return ResponseEntity.ok(casoUsoCrearCategoria.getCategories(ContextoInquilino.getIdTienda()));
    }

    @PostMapping("/productos")
    @Operation(summary = "Crear un producto")
    @CacheEvict(value = {"productos", "product_details"}, allEntries = true) // Limpiar todo el caché de productos por ahora
    public ResponseEntity<RespuestaProducto> createProduct(@Valid @RequestBody SolicitudCrearProducto request) {
        RespuestaProducto response = casoUsoCrearProducto.execute(request, ContextoInquilino.getIdTienda());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
```

por:

```java
    @PostMapping("/categorias")
    @Operation(summary = "Crear una categoría")
    @CacheEvict(value = "categorias", key = "T(com.ecommerce.modulos.compartido.infrastructure.ContextoInquilino).getIdTiendaPropia()")
    public ResponseEntity<RespuestaCategoria> createCategory(@Valid @RequestBody SolicitudCrearCategoria request) {
        RespuestaCategoria response = casoUsoCrearCategoria.execute(request, ContextoInquilino.getIdTiendaPropia());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/categorias")
    @Operation(summary = "Listar categorías")
    @Cacheable(value = "categorias", key = "T(com.ecommerce.modulos.compartido.infrastructure.ContextoInquilino).getIdTienda()")
    public ResponseEntity<List<RespuestaCategoria>> listCategories() {
        return ResponseEntity.ok(casoUsoCrearCategoria.getCategories(ContextoInquilino.getIdTienda()));
    }

    @PostMapping("/productos")
    @Operation(summary = "Crear un producto")
    @CacheEvict(value = {"productos", "product_details"}, allEntries = true) // Limpiar todo el caché de productos por ahora
    public ResponseEntity<RespuestaProducto> createProduct(@Valid @RequestBody SolicitudCrearProducto request) {
        RespuestaProducto response = casoUsoCrearProducto.execute(request, ContextoInquilino.getIdTiendaPropia());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
```

`listCategories`/`getProduct`/`listProducts` (GET, navegación pública) quedan sin cambios — siguen usando `getIdTienda()`.

- [ ] **Step 2: `ControladorProducto` — subida de imágenes**

En `src/main/java/com/ecommerce/modulos/catalogo/infrastructure/ControladorProducto.java`, reemplazar:

```java
        UUID idTienda = ContextoInquilino.getIdTienda();
```

por:

```java
        UUID idTienda = ContextoInquilino.getIdTiendaPropia();
```

- [ ] **Step 3: `ControladorCupon` — los 3 endpoints**

En `src/main/java/com/ecommerce/modulos/ordenes/infrastructure/ControladorCupon.java`, reemplazar las 3 ocurrencias de `ContextoInquilino.getIdTienda()` por `ContextoInquilino.getIdTiendaPropia()`:

```java
    @PostMapping
    @Operation(summary = "Crear un nuevo cupón de descuento")
    public ResponseEntity<Cupon> crearCupon(@Valid @RequestBody SolicitudCrearCupon request) {
        Cupon cupon = casoUsoGestionarCupon.crearCupon(ContextoInquilino.getIdTiendaPropia(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(cupon);
    }

    @GetMapping
    @Operation(summary = "Listar cupones de la tienda")
    public ResponseEntity<RespuestaPaginada<Cupon>> listarCupones(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(casoUsoGestionarCupon.listarCupones(
                ContextoInquilino.getIdTiendaPropia(),
                PageRequest.of(page, size)));
    }

    @PatchMapping("/{idCupon}/estado")
    @Operation(summary = "Activar o desactivar un cupón")
    public ResponseEntity<Void> alternarEstado(@PathVariable UUID idCupon) {
        casoUsoGestionarCupon.alternarEstadoCupon(ContextoInquilino.getIdTiendaPropia(), idCupon);
        return ResponseEntity.noContent().build();
    }
```

Nota: `/api/v1/cupones` no está en la lista `permitAll()` de `SecurityConfig`, así que ya exige autenticación — este cambio cierra el hueco de que un `CUSTOMER` autenticado pudiera crear cupones para cualquier tienda vía header.

- [ ] **Step 4: `ControladorAnaliticas`**

En `src/main/java/com/ecommerce/modulos/analiticas/infrastructure/ControladorAnaliticas.java`, reemplazar:

```java
        ResumenDashboard resumen = casoUsoAnaliticas.obtenerResumen(ContextoInquilino.getIdTienda());
```

por:

```java
        ResumenDashboard resumen = casoUsoAnaliticas.obtenerResumen(ContextoInquilino.getIdTiendaPropia());
```

- [ ] **Step 5: `ControladorOrden` — listado y reporte (no `createOrder`/`getOrder`/`cancelOrder`)**

En `src/main/java/com/ecommerce/modulos/ordenes/infrastructure/ControladorOrden.java`, reemplazar:

```java
    @GetMapping
    @Operation(summary = "List ordenes for current inquilino")
    public ResponseEntity<com.ecommerce.modulos.compartido.infrastructure.RespuestaPaginada<RespuestaOrden>> listOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(casoUsoOrden.listOrdersByTenant(
                ContextoInquilino.getIdTienda(),
                org.springframework.data.domain.PageRequest.of(page, size)));
    }
```

por:

```java
    @GetMapping
    @Operation(summary = "List ordenes for current inquilino")
    public ResponseEntity<com.ecommerce.modulos.compartido.infrastructure.RespuestaPaginada<RespuestaOrden>> listOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(casoUsoOrden.listOrdersByTenant(
                ContextoInquilino.getIdTiendaPropia(),
                org.springframework.data.domain.PageRequest.of(page, size)));
    }
```

Y reemplazar:

```java
    @GetMapping(value = "/reporte/excel", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    @Operation(summary = "Generate Excel report of orders")
    public ResponseEntity<byte[]> generateExcelReport() {
        byte[] report = servicioReporteOrdenes.generarReporteExcel(ContextoInquilino.getIdTienda());
```

por:

```java
    @GetMapping(value = "/reporte/excel", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    @Operation(summary = "Generate Excel report of orders")
    public ResponseEntity<byte[]> generateExcelReport() {
        byte[] report = servicioReporteOrdenes.generarReporteExcel(ContextoInquilino.getIdTiendaPropia());
```

- [ ] **Step 6: `ControladorLogistica.actualizarEstado` (no `rastrearEnvio`)**

En `src/main/java/com/ecommerce/modulos/logistica/infrastructure/ControladorLogistica.java`, reemplazar:

```java
    @PostMapping("/admin/rastreo/{numeroGuia}/estado")
    @Operation(summary = "Actualizar estado de envío (Uso de proveedores/Admin)")
    public ResponseEntity<Envio> actualizarEstado(
            @PathVariable String numeroGuia,
            @RequestParam EstadoEnvio estado,
            @RequestParam String ubicacion,
            @RequestParam String descripcion) {
        
        Envio envio = casoUsoLogistica.actualizarEstado(ContextoInquilino.getIdTienda(), numeroGuia, estado, ubicacion, descripcion);
        return ResponseEntity.ok(envio);
    }
```

por:

```java
    @PostMapping("/admin/rastreo/{numeroGuia}/estado")
    @Operation(summary = "Actualizar estado de envío (Uso de proveedores/Admin)")
    public ResponseEntity<Envio> actualizarEstado(
            @PathVariable String numeroGuia,
            @RequestParam EstadoEnvio estado,
            @RequestParam String ubicacion,
            @RequestParam String descripcion) {
        
        Envio envio = casoUsoLogistica.actualizarEstado(ContextoInquilino.getIdTiendaPropia(), numeroGuia, estado, ubicacion, descripcion);
        return ResponseEntity.ok(envio);
    }
```

- [ ] **Step 7: Compilar**

Run: `./gradlew compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 8: Commit**

```bash
git add src/main/java/com/ecommerce/modulos/catalogo/infrastructure/ControladorCatalogo.java src/main/java/com/ecommerce/modulos/catalogo/infrastructure/ControladorProducto.java src/main/java/com/ecommerce/modulos/ordenes/infrastructure/ControladorCupon.java src/main/java/com/ecommerce/modulos/analiticas/infrastructure/ControladorAnaliticas.java src/main/java/com/ecommerce/modulos/ordenes/infrastructure/ControladorOrden.java src/main/java/com/ecommerce/modulos/logistica/infrastructure/ControladorLogistica.java
git commit -m "fix: endpoints de gestion de tienda exigen tenant de confianza, no el header"
```

---

### Task 6: Cerrar el IDOR en `CasoUsoOrden.cancelOrder`

**Contexto del hallazgo:** `CasoUsoOrden.getOrder` (`src/main/java/com/ecommerce/modulos/ordenes/application/CasoUsoOrden.java:117-124`) valida `ordenes.getIdTienda().equals(idTienda)`, pero `cancelOrder` (líneas 138-147) no — cualquier cliente autenticado que conozca/adivine un UUID de orden de otra tienda puede cancelarla.

**Files:**
- Modify: `src/main/java/com/ecommerce/modulos/ordenes/application/CasoUsoOrden.java:138-147`
- Test: `src/test/java/com/ecommerce/modulos/ordenes/application/CasoUsoOrdenTest.java`

**Interfaces:**
- Consumes: `ExcepcionEntidadNoEncontrada` (ya importado en el archivo).

- [ ] **Step 1: Agregar el test que falla**

En `src/test/java/com/ecommerce/modulos/ordenes/application/CasoUsoOrdenTest.java`, agregar (junto a los `@Mock`/campos existentes no hace falta ningún mock nuevo — usa `repositorioOrden`):

```java
    @Test
    void cancelOrderLanzaExcepcionSiLaOrdenEsDeOtraTienda() {
        UUID idOrden = UUID.randomUUID();
        UUID idTiendaDueña = UUID.randomUUID();
        UUID idTiendaAtacante = UUID.randomUUID();

        Orden orden = new Orden();
        orden.setIdTienda(idTiendaDueña);
        when(repositorioOrden.findById(idOrden)).thenReturn(Optional.of(orden));

        assertThrows(ExcepcionEntidadNoEncontrada.class, () -> {
            casoUsoOrden.cancelOrder(idOrden, idTiendaAtacante, "spoofed");
        });

        verify(repositorioOrden, never()).save(any());
    }
```

Import necesario ya presente (`ExcepcionEntidadNoEncontrada` se importa en `CasoUsoOrden.java`; en el test agregar `import com.ecommerce.modulos.compartido.domain.ExcepcionEntidadNoEncontrada;` si no está).

- [ ] **Step 2: Correr el test y verificar que falla**

Run: `./gradlew test --tests "com.ecommerce.modulos.ordenes.application.CasoUsoOrdenTest.cancelOrderLanzaExcepcionSiLaOrdenEsDeOtraTienda"`
Expected: FAIL — `cancelOrder` cancela la orden sin verificar el tenant, el mock de `save` nunca se llamaría a verificar como "never" pero sí se ejecuta.

- [ ] **Step 3: Agregar la verificación de tenant en `cancelOrder`**

En `src/main/java/com/ecommerce/modulos/ordenes/application/CasoUsoOrden.java`, reemplazar:

```java
    @Transactional
    public RespuestaOrden cancelOrder(UUID idOrden, UUID idTienda, String reason) {
        Orden ordenes = repositorioOrden.findById(idOrden)
                .orElseThrow(() -> new ExcepcionEntidadNoEncontrada("Orden", idOrden));
        ordenes.cancel(reason);
        ordenes = repositorioOrden.save(ordenes);
        eventPublisher.publish(ordenes.getDomainEvents());
        ordenes.clearDomainEvents();
        return mapToResponse(ordenes);
    }
```

por:

```java
    @Transactional
    public RespuestaOrden cancelOrder(UUID idOrden, UUID idTienda, String reason) {
        Orden ordenes = repositorioOrden.findById(idOrden)
                .orElseThrow(() -> new ExcepcionEntidadNoEncontrada("Orden", idOrden));
        if (!ordenes.getIdTienda().equals(idTienda)) {
            throw new ExcepcionEntidadNoEncontrada("Orden", idOrden);
        }
        ordenes.cancel(reason);
        ordenes = repositorioOrden.save(ordenes);
        eventPublisher.publish(ordenes.getDomainEvents());
        ordenes.clearDomainEvents();
        return mapToResponse(ordenes);
    }
```

- [ ] **Step 4: Correr el test y verificar que pasa**

Run: `./gradlew test --tests "com.ecommerce.modulos.ordenes.application.CasoUsoOrdenTest"`
Expected: PASS (todos los tests de la clase, incluidos los preexistentes)

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/ecommerce/modulos/ordenes/application/CasoUsoOrden.java src/test/java/com/ecommerce/modulos/ordenes/application/CasoUsoOrdenTest.java
git commit -m "fix: cancelOrder valida que la orden pertenezca al tenant solicitante"
```

---

### Task 7: Activar el filtro `@Filter` de Hibernate como defensa de fondo

**Contexto del hallazgo:** `EntidadInquilino` declara `@FilterDef`/`@Filter` (`src/main/java/com/ecommerce/modulos/compartido/domain/EntidadInquilino.java:15-16`), pero `ConfiguracionFiltroInquilinoHibernate.enableFilter` (`src/main/java/com/ecommerce/modulos/compartido/infrastructure/ConfiguracionFiltroInquilinoHibernate.java:39-44`) — el único punto que lo activaría — nunca se llama desde ningún lado. El `@EventListener(ApplicationReadyEvent.class)` de esa misma clase solo loguea, no activa nada. El `ADR-002` documenta que "el filtro se aplica automáticamente", lo cual es falso hoy.

**Files:**
- Modify: `build.gradle`
- Create: `src/main/java/com/ecommerce/modulos/compartido/infrastructure/AspectoFiltroInquilino.java`
- Modify: `src/main/java/com/ecommerce/modulos/compartido/infrastructure/ConfiguracionFiltroInquilinoHibernate.java`

**Interfaces:**
- Consumes: `ContextoInquilino.getIdTienda()` (Task 2), `RepositorioJpaBase` (ya existe, es el padre común de todos los repositorios tenant-aware y no-tenant-aware).

- [ ] **Step 1: Agregar la dependencia `spring-boot-starter-aop`**

En `build.gradle`, en el bloque `dependencies`, agregar después de la línea `implementation 'org.springframework.boot:spring-boot-starter-validation'`:

```groovy
    implementation 'org.springframework.boot:spring-boot-starter-aop'
```

- [ ] **Step 2: Simplificar `ConfiguracionFiltroInquilinoHibernate` — quitar el listener muerto, conservar el helper**

Reemplazar el contenido completo de `src/main/java/com/ecommerce/modulos/compartido/infrastructure/ConfiguracionFiltroInquilinoHibernate.java`:

```java
package com.ecommerce.modulos.compartido.infrastructure;

import org.hibernate.Session;

import java.util.UUID;

/**
 * Activa el filtro Hibernate "filtroInquilino" (declarado en EntidadInquilino)
 * sobre una Session. Se invoca desde AspectoFiltroInquilino antes de cada
 * acceso a repositorio — es la defensa de fondo del aislamiento multi-tenant,
 * independiente de que cada caso de uso filtre manualmente por idTienda.
 */
public final class ConfiguracionFiltroInquilinoHibernate {

    private ConfiguracionFiltroInquilinoHibernate() {}

    public static void enableFilter(Session session, UUID idTienda) {
        if (idTienda != null && session.getEnabledFilter("filtroInquilino") == null) {
            session.enableFilter("filtroInquilino")
                    .setParameter("idTienda", idTienda);
        }
    }
}
```

- [ ] **Step 3: Crear `AspectoFiltroInquilino`**

```java
package com.ecommerce.modulos.compartido.infrastructure;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.hibernate.Session;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Activa el filtro Hibernate de aislamiento por tenant antes de cualquier
 * llamada a un repositorio Spring Data. Corre dentro de la transacción del
 * caso de uso/servicio que invoca al repositorio, por lo que el EntityManager
 * ya está vinculado a una Session activa.
 */
@Aspect
@Component
public class AspectoFiltroInquilino {

    @PersistenceContext
    private EntityManager entityManager;

    @Before("execution(* com.ecommerce.modulos.compartido.infrastructure.RepositorioJpaBase+.*(..))")
    public void activarFiltroInquilino() {
        UUID idTienda = ContextoInquilino.getIdTienda();
        if (idTienda == null) {
            return;
        }
        Session session = entityManager.unwrap(Session.class);
        ConfiguracionFiltroInquilinoHibernate.enableFilter(session, idTienda);
    }
}
```

- [ ] **Step 4: Compilar**

Run: `./gradlew compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Correr la suite completa de tests existentes (regresión)**

Run: `./gradlew test`
Expected: BUILD SUCCESSFUL — ningún test existente debe romperse por tener el filtro activo (los casos de uso ya filtran manualmente por `idTienda`, así que el filtro debe ser transparente).

- [ ] **Step 6: Commit**

```bash
git add build.gradle src/main/java/com/ecommerce/modulos/compartido/infrastructure/AspectoFiltroInquilino.java src/main/java/com/ecommerce/modulos/compartido/infrastructure/ConfiguracionFiltroInquilinoHibernate.java
git commit -m "fix: activar el filtro Hibernate de aislamiento por tenant via aspecto AOP"
```

---

### Task 8: Test de integración — el filtro Hibernate realmente aísla los datos

**Contexto:** Este es el test de "backstop" que prueba el Task 7: incluso una query que NO filtra manualmente por `idTienda` (como `JpaRepository.findAll()`) debe devolver solo las filas del tenant activo, gracias al filtro de Hibernate.

**Files:**
- Test: `src/test/java/com/ecommerce/modulos/compartido/infrastructure/AislamientoMultiTenantIntegrationTest.java`

**Interfaces:**
- Consumes: `RepositorioProducto` (ya existe), `ContextoInquilino` (Task 2), `Producto` (`src/main/java/com/ecommerce/modulos/catalogo/domain/Producto.java`).

- [ ] **Step 1: Escribir el test de integración**

Sigue el patrón de Testcontainers ya usado en `src/test/java/com/ecommerce/modulos/ordenes/application/CasoUsoOrdenIntegrationTest.java`.

```java
package com.ecommerce.modulos.compartido.infrastructure;

import com.ecommerce.modulos.catalogo.domain.EstadoProducto;
import com.ecommerce.modulos.catalogo.domain.Producto;
import com.ecommerce.modulos.catalogo.domain.RepositorioProducto;
import com.ecommerce.modulos.compartido.domain.Dinero;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
@Transactional
class AislamientoMultiTenantIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");
    }

    @Autowired
    private RepositorioProducto repositorioProducto;

    @AfterEach
    void limpiar() {
        ContextoInquilino.clear();
    }

    @Test
    void findAllSoloDevuelveProductosDelTenantActivoAunqueNoSeFiltrePorIdTiendaExplicitamente() {
        UUID idTiendaA = UUID.randomUUID();
        UUID idTiendaB = UUID.randomUUID();

        repositorioProducto.save(crearProducto(idTiendaA, "producto-tienda-a"));
        repositorioProducto.save(crearProducto(idTiendaB, "producto-tienda-b"));
        repositorioProducto.flush();

        ContextoInquilino.setIdTienda(idTiendaA);
        List<Producto> visiblesParaA = repositorioProducto.findAll();

        assertEquals(1, visiblesParaA.size());
        assertEquals(idTiendaA, visiblesParaA.get(0).getIdTienda());
    }

    private Producto crearProducto(UUID idTienda, String enlaceCorto) {
        Producto producto = new Producto();
        producto.setIdTienda(idTienda);
        producto.setNombre("Producto de prueba");
        producto.setEnlaceCorto(enlaceCorto);
        producto.setPrecio(Dinero.of(new BigDecimal("10.00"), "USD"));
        producto.setEstado(EstadoProducto.DRAFT);
        return producto;
    }
}
```

Nota: `repositorioProducto.save(...)` en el `@BeforeEach`/setup del test corre SIN `ContextoInquilino` seteado (idTienda null), así que el aspecto no activa el filtro para esas dos escrituras — se guardan ambos productos igual (el filtro de Hibernate solo restringe lecturas por defecto, no escrituras). Recién al setear `ContextoInquilino.setIdTienda(idTiendaA)` antes del `findAll()` se activa el filtro para esa lectura.

- [ ] **Step 2: Correr el test**

Run: `./gradlew test --tests "com.ecommerce.modulos.compartido.infrastructure.AislamientoMultiTenantIntegrationTest"`
Expected: PASS. Si falla con `visiblesParaA.size() == 2`, el filtro no se está activando — revisar que `AspectoFiltroInquilino` esté siendo tejido (verificar que `spring-boot-starter-aop` quedó en el classpath y que `RepositorioProducto` extiende `RepositorioJpaBase`).

- [ ] **Step 3: Commit**

```bash
git add src/test/java/com/ecommerce/modulos/compartido/infrastructure/AislamientoMultiTenantIntegrationTest.java
git commit -m "test: verificar que el filtro Hibernate aisla productos entre tenants"
```

---

### Task 9: Test de integración end-to-end — spoofing de header vía HTTP real

**Contexto:** Prueba, a nivel HTTP completo (igual que lo haría un atacante real), que un vendedor autenticado no puede crear un producto en la tienda de otro vendedor mandando `X-Inquilino-ID` falsificado.

**Files:**
- Test: `src/test/java/com/ecommerce/modulos/compartido/infrastructure/SpoofingHeaderInquilinoIntegrationTest.java`

**Interfaces:**
- Consumes: `MockMvc`, endpoints reales `/api/v1/auth/registro`, `/api/v1/auth/login`, `/api/v1/inquilinos`, `/api/v1/catalogo/productos`.

- [ ] **Step 1: Escribir el test de integración**

```java
package com.ecommerce.modulos.compartido.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class SpoofingHeaderInquilinoIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void unVendedorNoPuedeCrearProductosEnLaTiendaDeOtroVendedorSpoofeandoElHeader() throws Exception {
        String tokenVendedorA = registrarLoguearYCrearTienda("vendedor-a@test.com", "Tienda A", "tienda-a");
        String tokenVendedorB = registrarLoguearYCrearTienda("vendedor-b@test.com", "Tienda B", "tienda-b");

        String bodyTiendaB = mockMvc.perform(get("/api/v1/inquilinos/tienda-b"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String idTiendaB = JsonPath.read(bodyTiendaB, "$.id");

        Map<String, Object> nuevoProducto = Map.of(
                "nombre", "Producto atacante",
                "enlaceCorto", "producto-atacante",
                "precio", 10.00
        );

        String respuestaCreacion = mockMvc.perform(post("/api/v1/catalogo/productos")
                        .header("Authorization", "Bearer " + tokenVendedorA)
                        .header("X-Inquilino-ID", idTiendaB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nuevoProducto)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String idTiendaDelProductoCreado = JsonPath.read(respuestaCreacion, "$.idTienda");

        assertNotEquals(idTiendaB, idTiendaDelProductoCreado,
                "el producto no debe haberse creado en la tienda spoofeada");

        mockMvc.perform(get("/api/v1/catalogo/productos")
                        .header("X-Inquilino-ID", idTiendaB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", org.hamcrest.Matchers.empty()));
    }

    private String registrarLoguearYCrearTienda(String correo, String nombreTienda, String enlaceCorto) throws Exception {
        Map<String, Object> registro = Map.of(
                "correo", correo,
                "contrasena", "Password123!",
                "confirmarContrasena", "Password123!",
                "nombre", "Test",
                "apellido", "Vendedor"
        );
        mockMvc.perform(post("/api/v1/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registro)))
                .andExpect(status().isCreated());

        Map<String, Object> login = Map.of("correo", correo, "contrasena", "Password123!");
        String loginResponse = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(loginResponse, "$.accessToken");

        Map<String, Object> tienda = Map.of("nombre", nombreTienda, "enlaceCorto", enlaceCorto);
        mockMvc.perform(post("/api/v1/inquilinos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tienda)))
                .andExpect(status().isCreated());

        return token;
    }
}
```

- [ ] **Step 2: Correr el test**

Run: `./gradlew test --tests "com.ecommerce.modulos.compartido.infrastructure.SpoofingHeaderInquilinoIntegrationTest"`
Expected: PASS. Si `idTiendaDelProductoCreado` resulta igual a `idTiendaB`, el fix del Task 3/5 no está tomando efecto en el flujo real (revisar que `ControladorCatalogo.createProduct` haya quedado usando `getIdTiendaPropia()`).

- [ ] **Step 3: Commit**

```bash
git add src/test/java/com/ecommerce/modulos/compartido/infrastructure/SpoofingHeaderInquilinoIntegrationTest.java
git commit -m "test: verificar end-to-end que el spoofing de X-Inquilino-ID no funciona"
```

---

### Task 10: Verificación final

**Files:** ninguno — solo comandos de verificación.

- [ ] **Step 1: Compilación completa**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL — compila, corre toda la suite de tests (unitarios + integración con Testcontainers).

- [ ] **Step 2: Revisar que no quedó ningún uso de `getIdTienda()` en un endpoint de gestión de tienda**

Run: `grep -rn "ContextoInquilino.getIdTienda()" src/main/java/com/ecommerce/modulos/catalogo/infrastructure/ControladorCatalogo.java src/main/java/com/ecommerce/modulos/catalogo/infrastructure/ControladorProducto.java src/main/java/com/ecommerce/modulos/ordenes/infrastructure/ControladorCupon.java src/main/java/com/ecommerce/modulos/analiticas/infrastructure/ControladorAnaliticas.java`
Expected: sin resultados en `ControladorProducto`/`ControladorCupon`/`ControladorAnaliticas`; en `ControladorCatalogo` solo deben quedar los 3 usos de lectura pública (`listCategories`, `listProducts`, `getProduct`).

- [ ] **Step 3: Confirmar que `PrincipalInquilino` no resucitó**

Run: `grep -rn "PrincipalInquilino" src/`
Expected: sin resultados.

- [ ] **Step 4: Revisar el diff completo antes de considerar la tarea terminada**

Run: `git log --oneline -10` y `git diff master --stat` (o la rama base que corresponda)
Expected: 9 commits nuevos (uno por task 1-9), diff acotado a los archivos listados en este plan.

---

## Seguimientos fuera de alcance (no implementar en este plan)

Quedaron identificados pero **no se tocan aquí** porque no forman parte del alcance aprobado ("arreglar el aislamiento multi-tenant"):

- **Aislamiento cliente-a-cliente dentro del mismo tenant**: `CasoUsoOrden.getOrder`/`cancelOrder` verifican tenant pero no que `orden.getIdCliente()` sea el usuario autenticado — un cliente del mismo tenant podría ver/cancelar la orden de otro cliente si adivina el UUID.
- **`@PreAuthorize` por rol** (`SELLER`/`CUSTOMER`/`PLATFORM_ADMIN`): el proyecto tiene `@EnableMethodSecurity` pero cero anotaciones `@PreAuthorize` en todo el código. Este plan cierra el hueco de tenant vía ownership (`idPropietario`), que es más fuerte que un chequeo de rol para estos endpoints, pero la falta de RBAC declarativo sigue siendo deuda técnica separada.
- Los demás hallazgos altos/medios de la auditoría (secretos con default hardcodeado, `/actuator/**` sin auth, SSRF en webhooks salientes, sanitización de subida de archivos, transacciones envolviendo I/O externo a Stripe, N+1 de catálogo, etc.) — quedan para un plan posterior.
