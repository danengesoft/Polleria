# Hallazgos Fase17 — diagnóstico previo a corrección

## F17-01 — MEDIO — truncamiento de cantidad decimal

Reproducción HTTP real con CLIENTE: POST `/api/v1/pedidos/mios`, una línea
producto10/cantidad1.5 y una segunda línea inexistente para garantizar rollback.
Esperado400 por cantidad no entera. Observado404 por la segunda línea: Jackson
había convertido1.5 a1 antes de validar el request. No se creó pedido parcial.
Causa: coerción ACCEPT_FLOAT_AS_INT habilitada por defecto, anterior a Bean Validation.
Se añade regresión MockMvc antes de deshabilitar esa coerción globalmente.
La corrección conservará BigDecimal y cantidades fraccionarias de Abastecimiento.
Estado: RESUELTO. Se deshabilitó `accept-float-as-int` en application.yml. La regresión falló antes del cambio (404 en vez de400) y pasó después; HTTP real devuelve400. Las cantidades BigDecimal de Compra siguen aceptando fracciones: Compra111 verificada con2.500 y3.125.

## F17-02 — BAJO — .env frontend no ignorado

El .gitignore frontend cubre node_modules/dist/logs, pero no .env/.env.*.
No se identificó todavía una credencial real frontend; falta defensa preventiva
solicitada explícitamente por la revisión. Agregar patrones con excepción .env.example.
Estado: RESUELTO. Patrones .env/.env.* y excepción .env.example añadidos. git check-ignore comprobado; ninguna credencial real encontrada en archivos versionables.

## F17-03 — dependencias — auditoría inicial

Siete paquetes señalados (5 altos,2 críticos) en auditoría inicial. Se evalúa
corrección compatible dentro de Angular22 con peers estrictos y suites de regresión.
No utilizar force/legacy-peer-deps. Análisis por ruta, uso real y referencias
se documentará en el informe final. No se considerará aceptado el riesgo por defecto.
Estado: RESUELTO. Angular permanece22.x; framework22.2.3, CLI/build22.2.4, Vite8.3.3 por override. Auditoría final0, build correcto,105 tests correctos y E2E real. Ver ../polleria-frontend/AUDITORIA-DEPENDENCIAS-FASE-17.md. Severidades iniciales ALTAS/CRÍTICAS cerradas, sin aceptación de riesgo alto pendiente.

## F17-04 — MEDIO — error de negociación HTTP tratado como500

Reproducción real: POST Categorias con JSON pero Content-Type text/plain devuelve500,
en lugar de415. La excepción HttpMediaTypeNotSupportedException cae en el handler
inespecífico. Se añaden regresiones de media type y Accept no soportados antes de
completar el GlobalExceptionHandler, sin cambiar reglas comerciales.
Estado: RESUELTO. Handler específico devuelve415 con mensaje seguro. Se completó también manejo explícito406 para Accept no soportado (ese status ya era correcto antes). Dos regresiones, HTTP real415/406. La suite final de122 requests no produjo500. El500 inicial se conserva documentado, no se oculta.


## Cierre

Sin hallazgos BLOQUEANTES ni ALTOS pendientes. Dos defectos MEDIOS corregidos
(F17-01 y F17-04); la defensa BAJA F17-02 quedó corregida. No hubo refactor comercial,
DDL, roles ni módulos nuevos. Riesgo BAJO pendiente: mantenimiento del override Vite.
Warnings preexistentes no bloqueantes: instrumentación Mockito/Byte Buddy, fetch JDBC10
y avisos de Springdoc habilitado en dev. Los ORA de pruebas negativas de integridad
son estímulos esperados, manejados como409; no son datos inconsistentes persistidos.
