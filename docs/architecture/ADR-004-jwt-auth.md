# ADR-004: Autenticación con JWT Stateless

## Status
Accepted

## Context
El sistema necesita autenticar usuarios (vendedores, compradores, admins) sin estado de sesión en el servidor para escalar horizontalmente.

## Decision
Usar **JWT (JSON Web Tokens) stateless** con access token (15 min) + refresh token (7 días).

## Flujo
1. `POST /api/v1/auth/register` → Crea usuario, retorna perfil
2. `POST /api/v1/auth/login` → Retorna `accessToken` + `refreshToken`
3. Cada request incluye `Authorization: Bearer <accessToken>`
4. `POST /api/v1/auth/refresh` → Nuevo access token con refresh token rotado

## Detalles
- Algoritmo: HMAC-SHA256 con clave secreta configurable
- Claims: `sub` (email), `roles`, `iat`, `exp`
- Refresh tokens: almacenados en BD, rotados en cada uso (anti-replay)
- Password hashing: BCrypt

## Consequences
- Stateless → escala horizontal sin sticky sessions
- Access token corto (15 min) limita ventana de ataque
- Refresh token rotado previene reuso malicioso
- Sin OAuth2 externo en MVP (se puede agregar luego)
