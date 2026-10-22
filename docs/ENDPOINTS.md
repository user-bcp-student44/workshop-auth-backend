# Endpoints externos consumidos por el frontend (estado actual)

Este documento describe lo que el frontend **envía**, lo que **espera recibir** y cómo **reacciona la UI** según el código actual.

## Endpoint 1: Login (`POST /auth/login`)

### 1) Quién lo llama
- **Componente que inicia el flujo**: `LoginComponent.login()`.
- **Servicio que realiza HTTP**: `AuthService.login(payload)`.

### 2) Qué envía (headers/body/query params)

**URL completa**
- `http://localhost:8080/auth/login` (construida desde `environment.authApiBaseUrl`).

**Headers**

| Header | Valor en código | Observación |
|---|---|---|
| `Content-Type` | No se define explícitamente | `axios.post` suele enviar JSON automáticamente con `application/json`. |

**Query params**
- No envía query params.

**Body**

| Campo | Tipo | Requerido | Valor/Fuente |
|---|---|---|---|
| `username` | `string` | Sí | Campo de entrada `username` en login |
| `password` | `string` | Sí | Campo de entrada `password` en login |
| `client` | `string` | Sí | `"angular-workshop"` fijo en código; no puede faltar ni contener solo espacios |

**Ejemplo real de request**
```http
POST /auth/login HTTP/1.1
Host: localhost:8080
Content-Type: application/json

{
  "username": "student",
  "password": "Workshop123!",
  "client": "angular-workshop"
}
```

### 3) Qué espera recibir en éxito

**Código de estado esperado**
- **Pregunta abierta**: el frontend no valida explícitamente `status` HTTP (200/201/etc.); solo usa `response.data`.

**Campos esperados en `response.data`**

| Campo | Tipo | Requerido por UI | Uso en frontend |
|---|---|---|---|
| `token` | `string` | Sí | Si falta, se muestra error y no navega |
| `user` | objeto | No (fallback `{}`) | Se guarda para saludo/contexto de dashboard |
| `user.username` | `string` | No | Se usa para query `owner` en tareas |
| `user.displayName` | `string` | No | Se usa para saludo en dashboard |
| `expiresIn` | `integer` | No | Duración restante del token, en segundos; difiere como máximo un segundo de `exp - now` |

El token es un JWT/JWS firmado con HS256 e incluye `sub` (username autenticado), `client` (valor enviado), `iat` y `exp` como segundos desde epoch. `exp` es posterior a `iat`; el consumidor debe rechazarlo en `exp` y después. Estos claims no cambian los campos que consume la UI.

**Ejemplo real de respuesta exitosa consumible**
```json
{
  "token": "<signed-jwt>",
  "user": {
    "username": "student",
    "displayName": "Student User"
  },
  "expiresIn": 3600
}
```

### 4) Cómo reacciona la UI ante errores posibles

| Error posible | Dónde ocurre | Reacción de UI |
|---|---|---|
| Usuario vacío | Validación en `LoginComponent` antes de HTTP | Muestra mensaje: `No se pudo autenticar.` + `Ingrese un usuario.` |
| Contraseña vacía | Validación en `LoginComponent` antes de HTTP | Muestra mensaje: `No se pudo autenticar.` + `Ingrese una contraseña.` |
| `client` ausente o en blanco | Backend (`POST /auth/login`, HTTP 400) | Entra a `catch`, mismo mensaje genérico; no se emite token |
| Timeout de red (configurado 5000 ms) | `AuthService` (`axios.post` con `timeout: 5000`) | Entra a `catch`, muestra mensaje genérico: `Verifica tus credenciales e inténtalo de nuevo.` |
| Credenciales inválidas / 401 / 403 | `AuthService` (error HTTP) | Entra a `catch`, mismo mensaje genérico; no expone detalle del backend |
| Respuesta inesperada sin `token` | `LoginComponent` tras respuesta exitosa | Muestra `El backend respondió sin token.` y no navega |

### 5) Notas de seguridad relevantes
- Ya no se usa `[innerHTML]` para mensajes de error en login; el renderizado es por interpolación de texto.
- El token de sesión ya **no** se persiste en `localStorage`; se guarda en memoria dentro de `SessionService`.
- `workshop_user` y `workshop_preferences` sí se persisten en `localStorage`.

---

## Endpoint 2: Tareas (`GET /api/tasks`)

### 1) Quién lo llama
- **Componente que inicia el flujo**: `DashboardComponent.ngOnInit()`.
- **Servicio que realiza HTTP**: `TasksService.getTasks(owner, token)`.

### 2) Qué envía (headers/body/query params)

**URL completa**
- `http://localhost:8000/api/tasks?owner=<username>` (desde `environment.tasksApiBaseUrl`).

**Headers**

| Header | Valor en código | Observación |
|---|---|---|
| `Authorization` | `Bearer <token>` | Token entregado por `SessionService.getToken()` |

**Query params**

| Query param | Tipo | Requerido | Fuente |
|---|---|---|---|
| `owner` | `string` | Sí | `sessionService.getUser().username ?? ''` |

**Body**
- No envía body (request GET).

**Ejemplo real de request**
```http
GET /api/tasks?owner=student HTTP/1.1
Host: localhost:8000
Authorization: Bearer token-123
```

### 3) Qué espera recibir en éxito

**Código de estado esperado**
- **Pregunta abierta**: el frontend no valida explícitamente `status` HTTP; asume éxito si `axios` resuelve y existe `response.data`.

**Campos esperados por elemento de la lista**

| Campo | Tipo | Requerido por UI | Uso en frontend |
|---|---|---|---|
| `title` | `string` | Sí | Título visible de tarjeta |
| `description` | `string` | Sí | Descripción visible |
| `status` | `string` | Sí | Etiqueta y clase visual |
| `priority` | `string` | Sí | Ordenamiento y etiqueta derivada |
| `owner` | `string` | Sí | Etiqueta derivada |
| `label` | `string` | No (se genera en frontend) | Se calcula con `formatTaskLabel` |

**Ejemplo real de respuesta exitosa consumible**
```json
[
  {
    "title": "Preparar reporte",
    "description": "Consolidar hallazgos del sprint",
    "status": "TODO",
    "priority": "HIGH",
    "owner": "student"
  },
  {
    "title": "Revisar backlog",
    "description": "Refinar tareas con producto",
    "status": "IN_PROGRESS",
    "priority": "MEDIUM",
    "owner": "student"
  }
]
```

### 4) Cómo reacciona la UI ante errores posibles

| Error posible | Dónde ocurre | Reacción de UI |
|---|---|---|
| Token ausente en sesión (memoria) | `DashboardComponent.ngOnInit()` antes de llamar API | Redirige a `/login`; no intenta llamada a tareas |
| Token inválido / expirado (401/403) | `TasksService` (error HTTP en `axios.get`) | `DashboardComponent` muestra `No se pudo cargar el dashboard` |
| Timeout / error de red | `TasksService` | Mismo manejo genérico: `No se pudo cargar el dashboard` |
| Respuesta inesperada (payload no compatible) | Durante procesamiento en dashboard o error de `axios` | Entra a `catch`, muestra `No se pudo cargar el dashboard` |

### 5) Notas de seguridad relevantes
- El token se obtiene desde `SessionService` (memoria), no desde `localStorage`.
- Si no hay token, se corta el flujo y se redirige a login.
- El componente no inyecta HTML dinámico para el error; muestra texto plano (`{{ error }}`).

---

## Documentos y archivos consultados

- `docs/GOOD_PRACTICES.md`
- `src/environments/environment.ts`
- `src/app/models/auth.models.ts`
- `src/app/models/tasks.models.ts`
- `src/app/services/auth.service.ts`
- `src/app/services/tasks.service.ts`
- `src/app/services/session.service.ts`
- `src/app/login/login.component.ts`
- `src/app/login/login.component.html`
- `src/app/dashboard/dashboard.component.ts`
- `src/app/dashboard/dashboard.component.html`
