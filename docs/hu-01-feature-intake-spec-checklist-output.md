## Summary
- Se requiere que el token de autenticación incluya claims mínimos (`sub`, `client`, `iat`, `exp`) y expiración consistente con la respuesta de login.
- La historia HU-01 enfoca seguridad, configuración y validación del flujo `POST /auth/login`.
- Se deben reforzar controles para no exponer secretos ni tokens en logs y documentar la decisión técnica sobre HMAC vs librería JWT.

## Scope
- **In scope**
  - Exigir claims `sub`, `client`, `iat` y `exp` en el token emitido por login.
  - Obtener y validar `client` desde la solicitud y reflejarlo en el token.
  - Verificar coherencia entre `expiresIn` y `exp - now` usando una tolerancia documentada.
  - Evitar registro de tokens, secretos y valores sensibles en logs.
  - Documentar configuración de `AUTH_TOKEN_SECRET`.
  - Documentar decisión técnica entre HMAC y librería JWT.
- **Out of scope**
  - No se especifican cambios de infraestructura ni de nuevos servicios externos.
  - No se definen en la HU cambios de UI/frontend adicionales al envío de `client`.

## Assumptions
- La HU aplica al endpoint existente `POST /auth/login`.
- La validación de `client` se realizará en backend con reglas aún no detalladas en la HU.
- El uso de secreto por defecto solo podría admitirse en contexto de laboratorio, sujeto a confirmación formal.

## Technical decisions to make
- Definir si se mantiene firma HMAC actual o se migra a una librería JWT.
- Definir la tolerancia exacta permitida para validar `expiresIn` respecto de `exp - now`.
- Definir reglas de validación de `client` (formato, longitud, valores permitidos y manejo de errores).
- Definir política operativa para `AUTH_TOKEN_SECRET` fuera de laboratorio (obligatoriedad, rotación, fallback permitido/no permitido).
- Definir criterio de “valores sensibles” para logging y mecanismo de redacción.

## Risks and mitigations
- **Riesgo:** exposición de token o secreto en logs.  
  **Mitigación:** revisión explícita de puntos de logging en login/token y pruebas de evidencia de redacción.
- **Riesgo:** secreto por defecto usado fuera de laboratorio.  
  **Mitigación:** bloquear/invalidar secreto por defecto fuera de laboratorio y documentar política de configuración obligatoria.
- **Riesgo:** inconsistencia temporal entre `expiresIn` y `exp` por latencia/reloj.  
  **Mitigación:** definir tolerancia documentada y validarla con pruebas automatizadas.
- **Riesgo:** claim `client` ausente o inválido que rompa compatibilidad de consumidores.  
  **Mitigación:** validar entrada con mensajes de error claros y acordar contrato con consumidores.

## Acceptance criteria
- Todo token emitido en login válido contiene `sub`, `client`, `iat` y `exp`.
- Para login válido, `expiresIn` coincide con `exp - now` dentro de la tolerancia documentada.
- Existen pruebas automatizadas para: login válido, credenciales inválidas y token expirado.
- En los logs del flujo autenticación no aparecen tokens, secretos ni valores sensibles.
- La decisión técnica HMAC vs librería JWT queda documentada y versionada.

## Validation plan
- **Pruebas unitarias**
  - Verificar presencia de claims requeridos en token emitido.
  - Verificar cálculo/consistencia de expiración con tolerancia definida.
  - Verificar validación de `client` (casos válidos e inválidos).
- **Pruebas funcionales**
  - Flujo de login válido con emisión de token y respuesta `expiresIn`.
  - Flujo con credenciales inválidas y respuesta de error esperada.
  - Flujo con token expirado según el escenario definido por la HU.
- **Seguridad (comprobaciones mínimas)**
  - Revisar que logs no incluyan token completo, secreto ni datos sensibles.
  - Confirmar configuración de `AUTH_TOKEN_SECRET` según política acordada.
- **Evidencia requerida (qué adjuntar)**
  - Salida de pruebas de claims, expiración y errores de autenticación.
  - Documento de configuración de `AUTH_TOKEN_SECRET`.
  - Evidencia de logs redactados (checklist o extracto validado).
  - Documento de decisión técnica HMAC vs librería JWT.

## Suggested tasks by role
- **Chapter/PO**
  - Confirmar alcance funcional de HU-01 y criterio de éxito de negocio.
  - Aprobar política esperada para uso de secreto por defecto en laboratorio/no laboratorio.
  - Alinear dependencias y fechas para validación y evidencia.
- **Tech Lead**
  - Liderar y cerrar decisión HMAC vs librería JWT.
  - Definir contrato técnico de claims obligatorios y validación de `client`.
  - Definir tolerancia temporal oficial para `expiresIn` vs `exp - now`.
  - Aprobar lineamientos de logging seguro para autenticación.
- **Backend**
  - Implementar emisión de claims obligatorios (`sub`, `client`, `iat`, `exp`).
  - Implementar lectura y validación de `client` desde la solicitud.
  - Ajustar cálculo de expiración para coherencia con `expiresIn`.
  - Endurecer manejo de `AUTH_TOKEN_SECRET` según decisión aprobada.
  - Eliminar/evitar logs de token, secreto y datos sensibles.
- **Frontend**
  - Enviar `client` en la solicitud de login según contrato acordado.
  - Ajustar manejo de errores para validación de `client` si aplica.
  - Verificar compatibilidad con cambios de respuesta/expiración si aplica.
- **QA**
  - Diseñar y ejecutar casos para claims obligatorios y expiración.
  - Diseñar y ejecutar casos para credenciales inválidas y token expirado.
  - Validar que no exista exposición de token/secretos en logs.
  - Consolidar evidencia requerida para cierre de HU-01.
- **Cyber**
  - Revisar riesgo de manejo de secretos y política fuera de laboratorio.
  - Revisar cumplimiento de no exposición de datos sensibles en logs.
  - Validar controles mínimos de seguridad del token y emitir observaciones.

## Open questions
- ¿Cuál es la tolerancia exacta (en segundos) aceptada para validar `expiresIn` contra `exp - now`?
- ¿Qué reglas concretas debe cumplir `client` (formato, longitud, obligatoriedad y catálogo permitido)?
- ¿Se prohíbe totalmente el secreto por defecto fuera de laboratorio o se permite bajo excepciones controladas?
- ¿Qué definición operativa de “valores sensibles” se usará para la redacción de logs?
- ¿Qué criterio se usará para considerar “token expirado” en pruebas funcionales (tiempo real, reloj simulado u otro)?
