# ADR-004: Autenticación con JWT Sin Estado (Stateless)

## Estado
Aceptado

## Contexto
El sistema necesita autenticar usuarios (vendedores, compradores, administradores) sin estado de sesión en el servidor para escalar horizontalmente.

## Decisión
Usar **JWT (JSON Web Tokens) sin estado** con token de acceso (15 min) + token de actualización (7 días).

## Flujo
1. `POST /api/v1/auth/register` → Crea el usuario, retorna el perfil
2. `POST /api/v1/auth/login` → Retorna `accessToken` + `refreshToken`
3. Cada petición incluye `Authorization: Bearer <accessToken>`
4. `POST /api/v1/auth/refresh` → Nuevo token de acceso con token de actualización rotado

## Detalles
- Algoritmo: HMAC-SHA256 con clave secreta configurable
- Declaraciones (Claims): `sub` (email), `roles`, `iat`, `exp`
- Tokens de actualización: almacenados en base de datos, rotados en cada uso (anti-replay)
- Hashing de contraseñas: BCrypt

## Consecuencias
- Sin estado → escala horizontalmente sin sesiones adherentes
- Token de acceso corto (15 min) limita la ventana de ataque
- Token de actualización rotado previene el reuso malicioso
- Sin OAuth2 externo en el MVP (se puede agregar después)
