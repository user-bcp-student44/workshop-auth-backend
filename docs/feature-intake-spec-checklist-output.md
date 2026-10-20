## Summary
- Se definirá una confirmación previa al envío de transferencias para reducir transferencias duplicadas.
- La funcionalidad incluirá un resumen de la operación y validación de datos antes de enviar.
- Impacto esperado: reducir en 20% las quejas por transferencias duplicadas en un mes.

## Scope
- **Dentro del alcance**
  - Confirmación antes del envío.
  - Mensaje de resumen de la transferencia.
  - Validación de datos de transferencia.
- **Fuera del alcance**
  - Cambios de infraestructura.
  - Rediseño completo de la interfaz.

## Supuestos
- La implementación se realizará sobre el frontend web y la API REST existentes.
- Se mantendrá el esquema de logging estandarizado.
- No se asumen aún campos exactos del resumen ni reglas detalladas de validación; quedan pendientes de definición.

## Decisiones técnicas por tomar (pendientes o resueltas)
- **Pendiente:** definir qué servicio/versionado API será dueño del contrato de confirmación.
- **Pendiente:** acordar reglas exactas de validación de importes y datos obligatorios.
- **Pendiente:** definir eventos de logging del flujo de confirmación y política de enmascaramiento/redacción.

## Riesgos y mitigaciones
- **Riesgo:** exposición de datos sensibles en logs.  
  **Mitigación:** revisar y aprobar estructura de logs antes de liberar; validar que no se registren datos sensibles en pruebas.
- **Riesgo:** errores de validación de montos que bloqueen operaciones válidas o permitan inválidas.  
  **Mitigación:** definir reglas con PO/Tech Lead y cubrir casos límite con pruebas unitarias y funcionales.
- **Riesgo:** dependencia con servicios de cuentas y transferencias que retrase pruebas integradas.  
  **Mitigación:** coordinar ventanas de prueba tempranas y usar stubs/mocks donde aplique dentro del stack actual.

## Criterios de aceptación (medibles)
- Al menos el **95%** de los intentos con datos válidos muestran pantalla/mensaje de confirmación antes del envío.
- El **100%** de los intentos con datos inválidos bloquean el envío y muestran mensaje de validación.
- Los logs del flujo de confirmación en ambiente de prueba muestran **0 ocurrencias** de datos sensibles en la revisión definida.
- Las quejas por transferencias duplicadas disminuyen **20%** en un mes respecto al baseline acordado.

## Plan de validación
- **Pruebas unitarias**
  - Validación de datos válidos/ inválidos.
  - Cobertura de reglas de monto y campos requeridos.
  - Verificación de bloqueo de envío ante errores.
- **Pruebas funcionales**
  - Flujo end-to-end: captura → resumen → confirmar/cancelar.
  - Verificar que no se envía transferencia sin confirmación explícita.
  - Verificar mensajes de error y comportamiento en datos inválidos.
- **Seguridad (comprobaciones mínimas)**
  - Revisión de logs generados por el flujo para confirmar ausencia de datos sensibles.
  - Verificación de cumplimiento de lineamientos de logging existentes.
- **Evidencia requerida (qué adjuntar)**
  - Salida de pruebas unitarias/funcionales ejecutadas.
  - Checklist de revisión de logs de seguridad.
  - Documento breve actualizado con reglas de validación.
  - PR revisado conforme al DoD.

## Tareas sugeridas por rol
- **Chapter/PO**
  - Confirmar baseline y fuente para medir reducción del 20%.
  - Definir criterios de negocio para confirmar/cancelar transferencia.
  - Aprobar campos del resumen mostrado al usuario.
- **Tech Lead**
  - Definir ownership y contrato técnico de confirmación entre frontend y servicios.
  - Acordar estrategia de validación y manejo de errores.
  - Aprobar diseño de logging seguro y plan de pruebas.
- **Backend**
  - Implementar/ajustar validaciones de transferencia según reglas acordadas.
  - Exponer/ajustar endpoint(s) necesarios para soportar confirmación.
  - Garantizar logging sin datos sensibles en eventos del flujo.
- **Frontend**
  - Implementar UI de confirmación previa al envío.
  - Mostrar resumen con campos aprobados y acciones confirmar/cancelar.
  - Bloquear envío cuando existan errores de validación.
- **QA**
  - Diseñar casos positivos, negativos y de borde para validaciones.
  - Ejecutar pruebas funcionales del flujo completo de confirmación.
  - Validar evidencia de logs y consolidar reporte de resultados.
- **Cyber**
  - Revisar cumplimiento de controles mínimos de exposición de datos.
  - Validar checklist de seguridad de logs para el flujo.
  - Emitir visto bueno o hallazgos con acciones correctivas.

## Preguntas abiertas
- ¿Cuál será el baseline exacto (periodo/fuente) para medir la reducción del 20%?
- ¿Qué campos exactos debe incluir el resumen previo al envío?
- ¿Cuáles son las reglas definitivas de validación de monto y formato?
- ¿Qué servicio/versionado API será el propietario final del contrato de confirmación?
- ¿Qué datos están explícitamente permitidos/prohibidos en logs para este flujo?
