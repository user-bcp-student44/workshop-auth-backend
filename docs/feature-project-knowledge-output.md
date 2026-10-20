## Summary
- Proyecto backend en Java 21 con Quarkus (REST + JSON + Health), orientado a autenticación.
- Implementa un flujo de login en `/auth/login` que valida credenciales contra SQLite y retorna un token.
- Incluye pruebas de integración con QuarkusTest y RestAssured para el login exitoso.

## Contexto del proyecto
- Stack principal:
  - Java 21.
  - Quarkus 3.27.4.
  - Maven.
  - SQLite (`sqlite-jdbc`).
- Componentes de dominio en el paquete `com.bcp.workshop.auth`:
  - `AuthResource`: expone endpoint REST de autenticación.
  - `LegacyDatabase`: inicializa y consulta base SQLite local.
  - `LegacyTokenUtil`: genera token codificado en Base64.
  - `LoginRequest`: DTO de entrada del login.
  - `UserRecord`: modelo de usuario autenticado.
- Persistencia:
  - Base SQLite en `./data/workshop.db` (o variable `AUTH_DB_URL`).
  - Tabla `legacy_users` creada al iniciar si no existe.
- Calidad/validación existente:
  - Test `AuthResourceTest` para login exitoso.
  - Endpoint de health reportado en documentación (`GET /q/health`).

## Diagrama de componentes
- Flujo principal de autenticación:
  - Cliente HTTP
  - → `AuthResource` (`POST /auth/login`)
  - → `LegacyDatabase.findByCredentials(username, password)`
  - → SQLite (`legacy_users`)
  - → `LegacyTokenUtil.create(username, role)`
  - → Respuesta JSON con token y datos de usuario.
- Inicialización de datos:
  - Arranque de aplicación
  - → `LegacyDatabase.initialize()`
  - → crea carpeta `data/`
  - → crea tabla `legacy_users` (si no existe)
  - → inserta usuarios de taller por defecto (si no existen).

## Apis implementadas, inputs y outputs
- `POST /auth/login`
  - Input (JSON):
    - `username` (string, requerido en validación).
    - `password` (string, requerido en validación).
    - `client` (string, presente en DTO, no usado en la lógica actual).
  - Outputs:
    - `200 OK`:
      - `token` (string).
      - `expiresIn` (number, 3600).
      - `user`:
        - `username` (string).
        - `displayName` (string, escapado HTML).
        - `role` (string).
      - `debug` (string: `authenticated-with-legacy-flow`).
    - `400 Bad Request`:
      - `message` con motivo (`Request is null`, `Username is required`, `Password is required`).
    - `401 Unauthorized`:
      - `message` (`Invalid user <username> or password`).
    - `500 Internal Server Error`:
      - `message`.
      - `exception`.
- `GET /q/health`
  - Input: sin cuerpo.
  - Output: endpoint de health provisto por extensión `quarkus-smallrye-health` (documentado en README).

## Ejemplos de uso
- `POST /auth/login`
  - Request (JSON):
    ```json
    {
      "username": "<username>",
      "password": "<password>",
      "client": "<client>"
    }
    ```
  - Response 200 (JSON):
    ```json
    {
      "token": "BASE64_TOKEN",
      "expiresIn": 3600,
      "user": {
        "username": "<username>",
        "displayName": "<displayName>",
        "role": "<role>"
      },
      "debug": "authenticated-with-legacy-flow"
    }
    ```
  - Response 400 (JSON):
    ```json
    {
      "message": "Username is required"
    }
    ```
  - Response 401 (JSON):
    ```json
    {
      "message": "Invalid user <username> or password"
    }
    ```
- `GET /q/health`
  - Request (JSON):
    ```json
    {}
    ```
  - Response (JSON):
    ```json
    {
      "status": "UP"
    }
    ```
