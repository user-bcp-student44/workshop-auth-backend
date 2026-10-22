# Propuesta

## Por qué

El Issue [#10](https://github.com/user-bcp-student44/workshop-auth-backend/issues/10) solicita que un login exitoso emita un token firmado y verificable, con expiración y claims explícitos de identidad y cliente. Hacer observable su validez y duración, y evitar que secretos o tokens aparezcan en logs, reduce la ambigüedad para los clientes y mejora la evidencia de seguridad del flujo de autenticación.

## Qué cambia

- Exigir que un token emitido tras un login exitoso identifique al usuario autenticado y al cliente validado, e indique sus tiempos de emisión y expiración.
- Hacer que `expiresIn` describa correctamente la vigencia restante del token. Es una ampliación solicitada por el Issue al contrato documentado de éxito, que hoy garantiza `token` y `user` pero no documenta `expiresIn`.
- Exigir que `client`, enviado en la solicitud de login, no esté vacío y coincida con el claim de cliente del token. Esto usa el valor actual del frontend (`angular-workshop`) y no introduce una lista de clientes permitidos que no está definida.
- Exigir un secreto de firma configurado fuera del entorno de laboratorio y evitar que los valores de tokens y secretos se expongan en logs.
- Definir escenarios de aceptación observables para login exitoso, cliente inválido, credenciales inválidas, expiración y redacción de valores sensibles.

### Dentro del alcance

- El contrato de emisión de tokens de `POST /auth/login` y su respuesta exitosa.
- Claims de identidad y cliente, semántica de emisión y expiración, duración informada, configuración del secreto y redacción de logs.
- Evidencia de verificación para login válido, credenciales inválidas y expiración.

### Fuera del alcance

- Cambios a la verificación de credenciales, la ruta de login, la información de usuario consumida por el frontend o el comportamiento de login y manejo de errores del frontend.
- Cambios al endpoint `/api/tasks` o a su manejo actual de tokens bearer inválidos o expirados.
- Tokens de refresco, cierre de sesión, persistencia de sesión o un nuevo flujo de autenticación.
- Elegir o prescribir un formato de token, algoritmo de firma o biblioteca; es una decisión técnica para un diseño posterior.

### Comportamiento que debe preservarse

- El frontend continúa enviando `username`, `password` y `client` (actualmente `angular-workshop`) a `POST /auth/login`.
- En un login exitoso, `token` sigue presente y `user` continúa proporcionando el username y el display name opcional usados por la UI. `expiresIn` es aditivo; el contrato documentado del frontend no rechaza campos adicionales.
- Las credenciales inválidas siguen tratándose como un fallo de autenticación; la UI conserva su mensaje genérico y no expone detalles del backend.
- El frontend continúa enviando el token emitido como credencial bearer a `/api/tasks`; el manejo actual de tokens inválidos o expirados de ese endpoint no cambia.
- Aquí no se prescribe un nuevo código HTTP de éxito; el documento del endpoint deja abierto el código exacto.

## Capacidades

### Capacidades nuevas

- `auth-token`: Define las propiedades observables externamente de los tokens emitidos durante el login: identidad, cliente, emisión, expiración, duración informada y protección de valores sensibles.

### Capacidades modificadas

- Ninguna. El repositorio no tiene capacidades OpenSpec existentes; este cambio introduce `auth-token`.

## Impacto

- Contrato API: `POST /auth/login` conserva los campos consumidos actualmente y define claims verificables junto con un `expiresIn` coherente con la vigencia del token.
- Configuración y operación: los despliegues fuera del laboratorio no deben depender de un secreto de firma predeterminado incorporado, y los logs no deben exponer tokens ni secretos.
- Verificación: las pruebas de autenticación deben cubrir de forma observable credenciales válidas, credenciales inválidas y expiración.
- Supuestos para que la propuesta sea verificable: `client` es válido si se proporciona y no está en blanco (no se introduce una lista permitida); los claims de tiempo usan segundos desde epoch; y la comparación de duración admite como máximo un segundo de diferencia por la precisión de los timestamps en segundos. Estos supuestos no seleccionan una implementación.
