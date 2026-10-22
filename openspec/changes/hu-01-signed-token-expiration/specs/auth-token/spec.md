# Spec Delta

## Purpose

Define el contrato observable de los tokens firmados emitidos tras un login exitoso, incluyendo identidad, cliente, vigencia y protección de valores sensibles.

## ADDED Requirements

### Requirement: El login exitoso emite un token firmado con claims explícitos
El sistema MUST emitir un token verificablemente firmado para un login exitoso, con los claims `sub`, `client`, `iat` y `exp`. El sistema MUST conservar los campos documentados `token` y `user` en la respuesta.

#### Scenario: Login exitoso con un cliente válido
- **WHEN** un usuario envía credenciales válidas y un valor `client` no vacío a `POST /auth/login`
- **THEN** la respuesta exitosa contiene `token` y `user`, y los claims verificables del token identifican el username autenticado como `sub`, contienen el `client` enviado e incluyen `iat` y `exp` como timestamps en segundos desde epoch, con `exp` posterior a `iat`

### Requirement: El login valida y vincula el cliente
El sistema MUST rechazar una solicitud de login cuyo `client` falte o esté vacío y MUST vincular cada token emitido al valor no vacío de `client` enviado en esa solicitud.

#### Scenario: El cliente falta o está vacío
- **WHEN** una solicitud a `POST /auth/login` contiene credenciales válidas, pero omite `client` o solo envía espacios en blanco
- **THEN** la solicitud se rechaza con un error de cliente y no se emite un token

#### Scenario: El token identifica al cliente enviado
- **WHEN** una solicitud con credenciales válidas y un `client` no vacío tiene éxito
- **THEN** el claim `client` del token es igual al valor enviado en esa solicitud

### Requirement: El login informa la vigencia restante del token
El sistema MUST incluir `expiresIn` en la respuesta exitosa de login como duración en segundos coherente con el claim `exp` del token emitido, con una diferencia máxima de un segundo por la precisión de los timestamps.

#### Scenario: La respuesta exitosa informa una duración coherente
- **WHEN** un cliente compara `expiresIn` con la diferencia entre `exp` del token y la hora actual al generarse la respuesta
- **THEN** `expiresIn` está expresado en segundos y la diferencia absoluta no supera un segundo

### Requirement: Los tokens expirados dejan de ser válidos
Un token emitido MUST considerarse expirado en el instante `exp` y después de este, y MUST ser rechazado como credencial bearer válida por una API protegida.

#### Scenario: Se presenta un token expirado a la API de tareas
- **WHEN** un cliente presenta un token emitido a `GET /api/tasks` en el instante `exp` o después de este
- **THEN** la API protegida rechaza la solicitud como no autorizada o prohibida, preservando el manejo `401`/`403 documentado para el frontend

### Requirement: Los despliegues fuera del laboratorio requieren un secreto de firma configurado
Un despliegue que no esté designado explícitamente como laboratorio MUST NOT emitir tokens usando un secreto de firma predeterminado, implícito o incorporado.

#### Scenario: El secreto de firma no está configurado fuera del laboratorio
- **WHEN** un despliegue fuera del laboratorio no tiene un secreto de firma configurado y recibe una solicitud de login válida
- **THEN** no emite un token usando un valor predeterminado incorporado ni una alternativa sin firma

### Requirement: Los logs no exponen valores de tokens ni secretos
Los logs de autenticación MUST NOT contener el valor sin redactar de un token emitido ni el secreto de firma configurado.

#### Scenario: Se registra una autenticación exitosa
- **WHEN** se procesa un login exitoso y se inspeccionan los logs de autenticación resultantes
- **THEN** ni el valor del token devuelto ni el secreto de firma configurado aparecen en esos logs

### Requirement: Las credenciales inválidas no emiten tokens
El sistema MUST rechazar credenciales inválidas sin emitir un token y MUST preservar el contrato existente de manejo genérico de errores consumido por el frontend.

#### Scenario: Login con credenciales inválidas
- **WHEN** un cliente envía credenciales inválidas a `POST /auth/login`
- **THEN** la respuesta indica un fallo de autenticación (`401` o `403`), no contiene un token y el frontend continúa mostrando su mensaje genérico actual
