---
name: feature-project-knowledge
description: Revisa el proyecto para asegurar que se comprende el contexto, alcance, decisiones técnicas y riesgos antes de la implementación.
---

# Skill: Feature Project Knowledge

## 1) Metadata
-- **Skill ID:** feature-project-knowledge
- **Version:** 0.1.1
- **Purpose:** Estandarizar el entendimiento de un proyecto java antes de realizar cambios.
- **Audience (roles):** Tech Lead, Backend developer
- **When to use:** Antes de realizar cambios sobre el proyecto.
- **Sensitive inputs:** No incluyas datos reales de clientes, credenciales, secretos, tokens ni URLs internas no autorizadas.
- **Expected output:** Creacion de un markdown en la carpeta docs estructurado mediante la sección Output a continuación.

## 2) Inputs
Proporciona la información en este formato:

- Proyecto en Java

## 3) Output
Devuelve **solo** un documento Markdown con estas secciones y bullets que se guarde bajo la ruta docs del proyecto, puedes utilizar comandos cmd para la creacion del archivo:

1. **Summary**
2. **Contexto del proyecto**
3. **Diagrama de componentes**
4. **Arquitectura del proyecto**
5. **Apis implementadas, inputs y outputs**
6. **Ejemplos de uso** Incluye para cada API un JSON de ejemplo para el request y el response.
## 4) Constraints
- No inventes información, si no existe código fuente disponible, indícalo claramente.
- No incluyas datos sensibles ni secretos.
