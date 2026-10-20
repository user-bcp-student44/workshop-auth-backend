Autenticación vulnerable por SQL concatenado en login
Estado: CANDIDATO
Evidencia:
Construcción de query con username y password sin parametrización en LegacyDatabase.java:32.
Impacto:
Permite intentos de bypass de autenticación/inyección SQL en POST /auth/login.
Prioridad propuesta:
Alta.
Contrato potencialmente afectado:
Contrato de autenticación de POST /auth/login (validación de credenciales).
Cómo validarlo:
Prueba de integración enviando payload con caracteres de inyección en username/password y verificando si responde distinto a 401.
Ejemplo (manual): curl -X POST http://localhost:8080/auth/login -H "Content-Type: application/json" -d "{\"username\":\"' OR '1'='1\",\"password\":\"x\"}".
Política CORS abierta para endpoint de autenticación
Estado: CANDIDATO
Evidencia:
CORS global habilitado y origins permisivo en application.properties:4 y application.properties:5.
Impacto:
Cualquier origen puede intentar consumir el flujo de login desde navegador, ampliando superficie de abuso en entornos no aislados.
Prioridad propuesta:
Media-Alta.
Contrato potencialmente afectado:
Contrato de acceso cross-origin de POST /auth/login.
Cómo validarlo:
Ejecutar preflight desde origen no confiable y verificar Access-Control-Allow-Origin aceptado.
Revisar con devtools/red de navegador o curl -X OPTIONS con header Origin arbitrario.