# jwt-auth

Simple JWT-based authentication service using Spring Boot, Spring Data JPA and Spring Security.

## Overview
- Provides user registration, login (issues access + refresh tokens), and refresh endpoint.
- Roles stored in `roles` table and mapped many-to-many with `users` via `user_roles`.
- Refresh tokens persisted in `refresh_tokens`.
- Java 17, Maven, Spring Boot, MySQL (or any JPA-compatible DB).

## End-to-end flow
1. Register (`POST /auth/register`)
    - Client: sends `username`, `password`, `roles` (array/set).
    - Server: `AuthService` loads roles via `RoleRepository.findAllByNameIn(...)`, encodes password, assigns roles to `UserEntity` and saves the user.
2. Login (`POST /auth/login`)
    - Client: supplies credentials.
    - Server: authenticates via `AuthenticationManager`. On success:
        - generate access token (short-lived) and refresh token (longer-lived),
        - persist refresh token in `refresh_tokens` with `expiry` and `user` relation,
        - return both tokens in JSON: `{ "token": "<access>", "refreshToken": "<refresh>" }`.
3. Use protected endpoints
    - Client: send `Authorization: Bearer <access-token>` header.
    - `JwtFilter` extracts token, validates access token, loads `UserDetails`, and sets `SecurityContext`.
4. Refresh (`POST /auth/refresh`)
    - Client: send refresh token in `Authorization` header (preferred): `Authorization: Bearer <refresh-token>` or send bare token.
    - Controller: normalizes header (strip `Bearer `, surrounding quotes and whitespace).
    - Lookup refresh token with `RefreshTokenRepository.findByToken(String)`.
    - If token not found -> respond with invalid refresh token error.
    - If found and expired -> issue new refresh token, update DB entry (`token` + `expiry`) and persist.
    - Always issue a fresh access token and return both tokens (or return new access + current refresh depending on implementation).

## API examples
- Register
    - Endpoint: `POST /auth/register`
    - Body:
      ```json
      {
        "username": "alice",
        "password": "P@ssw0rd!",
        "roles": ["USER", "ADMIN"]
      }
      ```
    - Response: `200 OK` (message or created user details).

- Login
    - Endpoint: `POST /auth/login`
    - Body:
      ```json
      {
        "username": "alice",
        "password": "P@ssw0rd!"
      }
      ```
    - Response:
      ```json
      {
        "token": "<access-token>",
        "refreshToken": "<refresh-token>"
      }
      ```

- Refresh
    - Endpoint: `POST /auth/refresh`
    - Header (preferred): `Authorization: Bearer <refresh-token>`
    - Response:
      ```json
      {
        "token": "<new-access-token>",
        "refreshToken": "<new-or-same-refresh-token>"
      }
      ```

## SQL schema (MySQL) — exact tables used
- Use `utf8mb4` / `InnoDB` where shown.

```sql
CREATE TABLE `roles` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_roles_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `users` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(255) NOT NULL,
  `password` VARCHAR(255) NOT NULL,
  `enabled` TINYINT(1) NOT NULL DEFAULT 1,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_users_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `user_roles` (
  `user_id` BIGINT NOT NULL,
  `role_id` BIGINT NOT NULL,
  PRIMARY KEY (`user_id`,`role_id`),
  KEY `fk_user_roles_role_idx` (`role_id`),
  CONSTRAINT `fk_user_roles_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_user_roles_role` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `refresh_tokens` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `token` VARCHAR(500) NOT NULL,
  `expiry` DATETIME NOT NULL,
  `user_id` BIGINT,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_refresh_tokens_token` (`token`),
  KEY `fk_refresh_tokens_user_idx` (`user_id`),
  CONSTRAINT `fk_refresh_tokens_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
