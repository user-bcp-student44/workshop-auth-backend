# Tasks

Checklist de implementacion del cambio `hu-01-signed-token-expiration`, subordinado al [Issue #10](https://github.com/user-bcp-student44/workshop-auth-backend/issues/10), que sigue siendo la unidad de trabajo del producto. Referencias aprobadas: [proposal.md](proposal.md), [specs/auth-token/spec.md](specs/auth-token/spec.md) y [design.md](design.md).

Todas las tareas de codigo y documentacion se limitan a workshop-auth-backend. No incluyen cambios de credenciales, persistencia, CORS, frontend ni implementacion de `/api/tasks`. Completar caracterizacion antes de cambiar comportamiento funcional; no marcar una tarea como terminada sin su evidencia.

## 1. Caracterizacion antes del cambio funcional

- [x] 1.1 Ampliar `AuthResourceTest` para fijar el login valido actual: HTTP 200, `token` no vacio, `user.username`, `user.displayName`, `user.role` y `expiresIn=3600`; verificar que `mvn -B -Dtest=AuthResourceTest test` pasa sin modificar codigo funcional.
- [x] 1.2 Agregar casos de credenciales invalidas con `client` valido y comprobar el HTTP 401 actual sin token; verificar que pasan con la implementacion original y sin depender del texto detallado del error.
- [x] 1.3 Caracterizar solicitudes con `client` omitido, null, vacio y solo espacios, registrando su aceptacion actual con credenciales validas; verificar esos casos antes de actualizar sus expectativas en el cambio intencional de validacion.
- [x] 1.4 Ejecutar conjuntamente las pruebas de caracterizacion con `mvn -B -Dtest=AuthResourceTest test` y registrar el resultado en la evidencia de implementacion del cambio; verificar que existe un baseline verde anterior a los cambios funcionales.

	Evidencia: antes de ampliar el test, con Java 25 y `QUARKUS_HTTP_TEST_PORT=18081`, `mvn -B -Dtest=AuthResourceTest test` termino en `BUILD SUCCESS` (1 prueba, 0 fallos, 0 errores), aunque JaCoCo emitio avisos por version de class files. Tras agregar la caracterizacion, el mismo comando con Java 21 y el puerto 18081 termino en `BUILD SUCCESS` (6 pruebas, 0 fallos, 0 errores). El intento inicial sin override no pudo iniciar Quarkus porque el puerto 8081 ya estaba ocupado.

## 2. Compatibilidad y configuracion de firma

- [x] 2.1 Registrar en la documentacion local del cambio la evidencia disponible de compatibilidad del consumidor con JWT/JWS HS256, claims y distribucion del secreto, conforme al design; verificar que se identifica la fuente de esa evidencia o se declara explicitamente bloqueado el cambio de emision y el despliegue. No consultar ni modificar otros repositorios sin autorizacion.

	Evidencia 2.1: el usuario confirma que el consumidor acepta el contrato consultado (JWS HS256, clave compartida, claims y expiracion). Esta confirmacion no es una prueba de integracion; la tarea 5.2 y el despliegue quedan pendientes de evidencia del servicio consumidor. No se consultaron otros repositorios.
- [x] 2.2 Seleccionar una biblioteca JWT/JWS mantenida y compatible con Java 21 y Quarkus 3.27.4, agregar solo la dependencia necesaria en `pom.xml` y registrar la seleccion en `design.md`; verificar resolucion y compilacion mediante `mvn -B -DskipTests compile`, sin actualizar dependencias ajenas al Issue.

	Evidencia 2.2: se selecciono `io.quarkus:quarkus-smallrye-jwt-build`, gestionada por Quarkus BOM 3.27.4. Con Java 21, `mvn -B -DskipTests compile` termino en `BUILD SUCCESS`.
- [x] 2.3 Implementar configuracion de firma respaldada por `AUTH_TOKEN_SECRET`, sin valor implicito incorporado, y proporcionar una clave exclusivamente de prueba; verificar con pruebas de configuracion que un secreto ausente, vacio o no valido para HS256 impide emitir tokens, sin alternativa sin firma ni filtracion del valor en errores.
- [x] 2.4 Documentar en `README.md` la configuracion explicita del secreto para el laboratorio aislado y para pruebas, el fallo cerrado sin configuracion valida y la ausencia de un secreto predeterminado; verificar las instrucciones en un entorno de prueba sin publicar claves reales.

	Evidencia 2.3-2.4: `TokenIssuer` requiere `auth.token.secret`, valida longitud minima de 32 bytes y no tiene fallback; `%test.auth.token.secret` es ficticio y local. `TokenIssuerTest` cubre secreto ausente, blanco y corto. `README.md` documenta el secreto local no productivo y el fallo cerrado.

## 3. Emisor firmado y tiempo determinista

- [x] 3.1 Agregar pruebas focalizadas del emisor para firma HS256, `sub` igual al usuario autenticado, `client` igual al solicitado y `iat`/`exp` como segundos epoch; verificar que detectan el comportamiento legacy y usar un reloj controlable para evitar esperas reales.
- [x] 3.2 Sustituir la generacion reversible por emision JWT/JWS mediante la biblioteca seleccionada, conservando 3600 segundos de vigencia y eliminando el secreto embebido; verificar que las pruebas del emisor validan la firma con la clave de prueba y que el payload no contiene el secreto. Depende de la compatibilidad confirmada en 2.1.
- [x] 3.3 Agregar pruebas negativas que rechacen una firma alterada o una clave diferente y pruebas de validez justo antes de `exp`, en `exp` y despues; verificar mediante un verificador de pruebas con tiempo controlado, sin agregar un endpoint protegido ni atribuir estas pruebas al consumidor real.
- [x] 3.4 Derivar la duracion informada del mismo instante de expiracion del token, sin una constante independiente en la respuesta; verificar con tiempo controlado que `exp > iat`, `exp - iat = 3600` y que la diferencia absoluta entre `expiresIn` y `exp - now` al generar la respuesta no supera un segundo.

	Evidencia 3.1-3.4: `TokenIssuerTest` valida firma HS256, claims, epoch seconds, secreto ausente/blanco/corto, payload sin secreto, firma alterada, clave diferente y expiracion antes/en/despues de `exp`; usa reloj fijo. `mvn -B -Dtest=TokenIssuerTest test` termino en `BUILD SUCCESS` (3 pruebas, 0 fallos, 0 errores).

## 4. Integracion incremental del login y proteccion de valores

- [x] 4.1 Validar `client` antes de emitir el token y actualizar los casos de caracterizacion de 1.3 para esperar error HTTP de cliente sin token; verificar omision, null, cadena vacia y espacios, sin introducir una lista permitida ni normalizar el valor no vacio que debe reflejarse en el claim.
- [x] 4.2 Conectar el emisor y su duracion a `AuthResource`, manteniendo HTTP 200, el objeto `user` y el flujo de credenciales existente; verificar con pruebas HTTP la firma y los cuatro claims del token devuelto, el `client` solicitado, la coherencia temporal y la regresion de login valido e invalido.
- [x] 4.3 Agregar captura de logs y pruebas para login exitoso, errores de cliente/credenciales y fallos de configuracion o firma; verificar que no aparecen tokens, contrasenas ni secretos sin redactar, y corregir solo las rutas afectadas por este cambio que expongan esos valores.
- [x] 4.4 Actualizar `docs/ENDPOINTS.md` con `expiresIn` en segundos, claims, tolerancia de un segundo y rechazo de `client` ausente o blanco; verificar la documentacion contra las pruebas HTTP conservando los campos consumidos y el manejo generico del frontend, sin cambiar el contrato de tareas.

	Evidencia 4.1-4.2: `mvn -B '-Dtest=AuthResourceTest,TokenIssuerTest' test` con Java 21 y `QUARKUS_HTTP_TEST_PORT=18081` termino en `BUILD SUCCESS` (9 pruebas, 0 fallos, 0 errores). La prueba HTTP verifica firma, claims y coherencia de `expiresIn`; las cuatro variantes invalidas de `client` reciben 400 y no contienen token. Evidencia 4.4: `docs/ENDPOINTS.md` conserva endpoint y datos de UI y documenta los campos aditivos y el error de cliente.

	Evidencia 4.3: `AuthResourceTest.authenticationLogsDoNotContainTokenOrCredentialValues` captura logs de login exitoso, credenciales invalidas y cliente invalido, y comprueba ausencia de token, contrasena y clave. `TokenIssuerTest.invalidSigningSecretIsNotWrittenToLogs` comprueba el fallo de configuracion. `mvn -B -Dtest=TokenIssuerTest test` termino en `BUILD SUCCESS` (4 pruebas, 0 fallos, 0 errores); la ejecucion integrada termino con 10 pruebas, 0 fallos, 0 errores.

## 5. Validacion integrada y evidencia de cierre

- [x] 5.1 Ejecutar `mvn -B test` con configuracion de prueba explicita y registrar comando y resultado; verificar que la suite conjunta cubre caracterizacion preservada, firma, claims, tiempos, client, credenciales invalidas, configuracion y logs.
- [ ] 5.2 Registrar en este cambio evidencia autorizada del consumidor aceptando un token vigente y rechazandolo en/despues de `exp` con 401/403, segun el design; verificar que la evidencia distingue el servicio real del verificador local. Si no esta disponible, mantener la tarea pendiente y bloquear despliegue/cierre por esa dependencia, sin operar sobre otro repositorio ni implementar `/api/tasks` aqui.
- [x] 5.3 Registrar en la evidencia local del cambio la comprobacion del procedimiento de rollback aprobado: detener login o volver solo a una version conforme fuera del laboratorio, reautenticacion tras cambio de clave/formato y ausencia de secretos en artefactos; verificar que no se propone reactivar el emisor legacy como rollback seguro.
- [x] 5.4 Ejecutar `openspec validate "hu-01-signed-token-expiration" --strict` y revisar el diff final; verificar trazabilidad al Issue #10 y que no se incluyen cambios funcionales o documentales ajenos al alcance, y registrar cualquier validacion pendiente sin marcarla como exitosa.

	Evidencia 5.1: con Java 21, `QUARKUS_HTTP_TEST_PORT=18081` y secreto ficticio de `%test`, `mvn -B verify` termino en `BUILD SUCCESS`; 11 pruebas, 0 fallos, 0 errores. JaCoCo genero el reporte; `TokenIssuer` cubre 19/21 lineas y 6/6 ramas.

	Evidencia 5.3: `design.md`, seccion Rollback, prescribe detener/deshabilitar login o volver solo a una version conforme, no reactivar el emisor legacy fuera del laboratorio, reautenticar tras cambio de clave/formato y no incluir secretos en artefactos.

	Evidencia 5.4: `openspec validate "hu-01-signed-token-expiration" --strict` informo que el cambio es valido. El diff staged se reviso y contiene solo 11 archivos de implementacion, pruebas, documentacion y OpenSpec del Issue #10; 5.2 permanece pendiente por falta de prueba de integracion con el consumidor real.
