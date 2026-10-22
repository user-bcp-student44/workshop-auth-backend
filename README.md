# Workshop Auth Backend — Java Quarkus

> **Laboratorio intencionalmente vulnerable.** Ejecutar únicamente en local, repositorios privados y ambientes aislados. No desplegar en Internet, CERT o PROD.

Backend Quarkus que autentica usuarios de laboratorio almacenados en SQLite y entrega un token consumido por el frontend Angular y la API Python.

## Ejecución

Requisitos: JDK 21 y Maven 3.9+.

```bash
mvn quarkus:dev
```

Servicio: `http://localhost:8080`

## Secreto de firma para el laboratorio

El servicio requiere `AUTH_TOKEN_SECRET` con al menos 32 bytes UTF-8. No existe un valor predeterminado; si falta, está vacío o es demasiado corto, la aplicación no inicia. Para el laboratorio local aislado, configura un valor no productivo antes de iniciar:

```powershell
$env:AUTH_TOKEN_SECRET = '<non-production-secret-at-least-32-bytes>'
mvn quarkus:dev
```

No guardes secretos reales en el repositorio. El perfil de pruebas usa una clave ficticia local en `src/test/resources/application.properties`; nunca debe reutilizarse fuera de pruebas.

Credenciales locales:

- `student` / `Workshop123!`
- `trainer` / `Trainer123!`

## Endpoints

- `POST /auth/login`
- `GET /q/health`

## Propósito de la rama `main`

La rama contiene deuda técnica y de seguridad deliberada: SQL concatenado, contraseñas en texto plano, token reversible, secreto hardcodeado, CORS permisivo, método largo, manejo genérico de excepciones y dependencias con CVEs.
