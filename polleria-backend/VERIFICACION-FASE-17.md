# FASE 17 — Pruebas integrales y endurecimiento

**FASE 17 COMPLETADA — 25 de octubre de 2026.** Verificación en desarrollo local;
no constituye despliegue ni preparación automática de producción. No quedan
hallazgos BLOQUEANTES ni ALTOS sin resolver. Se conservaron los datos históricos.

Rutas: backend `/home/systemseng/Polleria/polleria-backend`, frontend
`/home/systemseng/Polleria/polleria-frontend`. Package `pe.edu.upeu.polleria`.
Se corrigieron dos defectos HTTP reproducidos y se actualizaron dependencias
vulnerables dentro de Angular22, con regresión completa y flujos reales.

## Evidencia técnica

Los logs de ejecución permanecen locales e ignorados por Git. Los siguientes
artefactos contienen resultados sanitizados, sin credenciales ni tokens:

- [Inventario inicial](verificacion-inventario-fase-17.json):45 archivos con cambios previos y sus hashes.
- [Infraestructura y Compose](verificacion-infra-fase-17.json).
- [Oracle inicial](verificacion-oracle-inicial-fase-17.jsonl) y [Oracle final tras reinicio](verificacion-oracle-final-fase-17.jsonl).
- [SQL de integridad reutilizable](scripts/verificar-integridad-fase-17.sql), únicamente SELECT y cambio de sesión al PDB.
- [HTTP real](verificacion-http-fase-17.json):122 comprobaciones registradas y auditoría completada.
- [Contraste Angular/HTTP/Oracle](verificacion-e2e-fase-17.json), renovado después del reinicio final.
- [Navegador real](../polleria-frontend/verificacion-navegador-fase-17.json).
- [Reinicio/Keycloak/PostgreSQL](verificacion-reinicio-fase-17.json).
- [Secretos, ignores y preservación de cambios](verificacion-seguridad-fase-17.json).
- [Hallazgos y causas](HALLAZGOS-FASE-17.md).
- [Auditoría detallada de dependencias](../polleria-frontend/AUDITORIA-DEPENDENCIAS-FASE-17.md), con clasificación A/B/C/D y referencias primarias.
- [npm inicial](../polleria-frontend/npm-audit-inicial-fase-17.json), [intermedio](../polleria-frontend/npm-audit-resolucion-fase-17.json) y [final](../polleria-frontend/npm-audit-fase-17.json).
- [Árbol directo instalado](../polleria-frontend/dependencias-fase-17.txt).

El script [HTTP](scripts/verificar-http-fase-17.py) **escribe casos de prueba**:
no debe ejecutarse esperando una auditoría de solo lectura. Cada ejecución crea
nuevos pedidos/insumos/movimientos identificables F17. Solo elimina su categoría y
producto efímeros sin referencias; mantiene los pedidos y su historial. Lee las
credenciales del .env ignorado y no las imprime. No utiliza password grant del SPA:
implementa Authorization Code con PKCE para las identidades de prueba.

## Resumen técnico solicitado:65 puntos

| N.º | Comprobación | Resultado y límite de la evidencia |
|---|---|---|
| 1 | Versiones | Java21.0.12.1, Maven3.9.16, Node24.18.0, npm11.16.0, Docker29.9.1, Compose5.6.0. Boot4.0.7, Modulith2.0.7, MapStruct1.6.3, springdoc3.0.2 sin cambios. Oracle23.26.3; Keycloak26.8.0; PostgreSQL17.11. Angular framework22.1.6→22.2.3 y CLI/build22.1.8→22.2.4 por correcciones evaluadas; Material/CDK22.2.0, TS6.0.3, Vitest4.1.12. |
| 2 | Contenedores | `polleria-oracle`, `polleria-keycloak-db`, `polleria-keycloak`:healthy después de reinicio. Puertos loopback1522,35432,38080. Volúmenes propios `polleria-oracle-data`, `polleria-keycloak-postgres-data`. Compose válidos. `punto-u-dev-postgres-1` y `mi_oracle` no alterados ni usados por el proyecto. |
| 3 | clean compile | `./mvnw clean compile`:BUILD SUCCESS tanto inicial como final. Logs `fase17-inicial-compile.log`, `fase17-compile.log`. |
| 4 | Backend tests | `./mvnw test`:BUILD SUCCESS antes y después de correcciones. Logs `fase17-inicial-test.log`, `fase17-test.log`. Sin pruebas desactivadas o eliminadas. |
| 5 | Total backend | Inicial198/198; final201/201, cero fallos, errores u omitidas. |
| 6 | Frontend build | `npm run build` correcto inicial/final. Bundle inicial final330.81kB, transferencia estimada92.64kB. Sin warnings ni presupuestos excedidos; límite inicial warning500kB/error1MB. |
| 7 | Frontend tests | `npm test -- --watch=false`:correcto antes y después de actualización. |
| 8 | Total frontend | 105/105, nueve archivos, sin cambios artificiales de selectores ni pruebas desactivadas. |
| 9 | Modulith | Tres pruebas arquitectónicas correctas:exactamente catalogo, clientes, pedidos, delivery, abastecimiento; sin ciclos. No se creó módulo reportes/auth. |
| 10 | Oracle |12 tablas comerciales reales; JPA continúa `ddl-auto:validate`. PK/FK/UNIQUE/CHECK inspeccionadas por ALL_CONSTRAINTS; todas ENABLED/VALIDATED. Sin DDL en esta fase. |
| 11 | Huérfanos | Cero en once relaciones verificadas, incluyendo detalles, clientes, categorías, repartidores, insumos y compras opcionales en movimientos. |
| 12 | Pedidos consistentes | Todos los pedidos:total igual a suma de detalles y subtotal igual a ROUND(precio histórico×cantidad,2). Cero diferencias. |
| 13 | Compras consistentes | Todas las compras:total igual a suma de detalles, subtotales con redondeo monetario2 decimales. Compra111:30.86+13.13=43.99. |
| 14 | Inventario | Cero stocks o movimientos negativos, cero diferencias aritméticas, de continuidad o stock final. AJUSTE se verifica por stock anterior/posterior y delta firmado; no se suma el stock objetivo como entrada. |
| 15 | Rollback Pedido | HTTP con línea válida e ID inexistente rechazado; contadores de pedido/detalle y stocks sin variación. Suite de integración confirma atomicidad. |
| 16 | Rollback Compra | HTTP con insumo válido e inexistente rechazado; cero compra/detalle/movimiento parcial y stock intacto. |
| 17 | Concurrencia | Insumo831:stock10; dos SALIDA8 simultáneas devuelven201 y409; stock2 y solo una salida persistida. Sin stress agresivo. |
| 18 | Estados Pedido | Saltos PENDIENTE→ENTREGADO, ENTREGADO→PENDIENTE y CANCELADO→CONFIRMADO rechazados409 por Service. Flujo autorizado completo probado. |
| 19 | Delivery/Pedido | Cero inconsistencias SQL. ASIGNADO inicial acepta LISTO; reasignación puede mantener Pedido DESPACHADO. EN_CAMINO/NO_ENTREGADO requieren DESPACHADO y ENTREGADO coincide con ENTREGADO. |
| 20 | Precio histórico | Cambio temporal Producto10 de20 a25 no cambió precio20 del Pedido1737. Precio actual restaurado a20. |
| 21 | Dirección histórica | Cambio temporal del perfil Cliente676 no alteró dirección histórica de Pedido1737/Entrega21. Perfil restaurado; Entrega utiliza snapshot del Pedido. |
| 22 | Compra histórica | Renombrar temporalmente Insumo84 no alteró nombre del DetalleCompra7. Nombre original restaurado. |
| 23 | ADMINISTRADOR | Login real; Catálogo CRUD y conflicto de categoría, consultas/operación interna y reportes permitidos. Navegación global correcta. |
| 24 | CAJERO | Login real; Cliente2434 creado/actualizado, DELETE403; lectura Catálogo y escritura403; estados Pedido y asignación/reasignación Delivery permitidos. Abastecimiento/reportes403. |
| 25 | REPARTIDOR | Login real; operaciones propias permitidas y datos mínimos. Globales Clientes/Pedidos/Delivery/Abastecimiento/Reportes403; pago explícito403. No correo ni sub innecesario en entrega. |
| 26 | ABASTECIMIENTO | Login real; proveedores/insumos/compra/inventario operativos. Clientes/Pedidos/Delivery/Reportes y escritura Catálogo403. |
| 27 | CLIENTE | Login real; catálogo, perfil propio, checkout, Mis pedidos y cancelación propia PENDIENTE operativos. Sin acceso administrativo ni otros clientes/pedidos. |
| 28 | 401 | Sin token en recurso protegido, token malformado y firma inválida:401. Público200. Sin500 en esos casos. |
| 29 | 403 | Tokens válidos sin rol:403 en matriz de siete identidades (cinco roles y dos identidades B). Sin confundir autenticación/autorización; interceptor existente no refresca ante403. |
| 30 | IDOR Cliente | ClienteB intenta GET/cancelar Pedido3896 de A:404 y recurso sin cambios. A puede cancelar su propio PENDIENTE. |
| 31 | IDOR Repartidor | B intenta consultar/iniciar/entregar/NO_ENTREGADO Entrega21 asignada a A:404, sin modificaciones. |
| 32 | Mass assignment | Pedido ignora autoridad enviada clienteId/total/estado/estadoPago/precioUnitario y calcula por usuario autenticado y catálogo; Pedido3896 total20, no0.01. Perfil no acepta sub/rol/activación arbitraria. Intento de mutación de perfil Repartidor existente rechazado409. |
| 33 | CORS | Origen exacto localhost4200 autorizado; origen ajeno rechazado403 sin permiso CORS. Sin wildcard. |
| 34 | Trace ID | X-Trace-ID presente y correlacionado en respuestas exitosas y errores; ID de prueba `fase17-http`, sin credenciales en evidencia. |
| 35 | OpenAPI | `/v3/api-docs` operativo, POLLERIA API y esquema Bearer. operationId únicos; no endpoints obsoletos/duplicados detectados en muestra y contrato actual. |
| 36 | Actuator | `/actuator/health`200/UP tras arranque final; env/beans/metrics/configprops no expuestos (404 incluso con administrador). |
| 37 | Keycloak | Realm polleria, client público polleria-frontend, Code habilitado, PKCE S256 obligatorio, password grant SPA deshabilitado, callback exacto4200/auth/callback, origen exacto4200, cinco roles comerciales. Siete usuarios de prueba del realm; PostgreSQL conserva2 realms/8 usuarios totales incluyendo master. |
| 38 | Secretos | Búsqueda de valores reales del .env y JWT completos sin coincidencias en archivos versionables/logs revisados. Revisión de usos password/token:placeholders, referencias a entorno y tokens sintéticos de tests distinguibles. .env backend0600. Ignore correcto para .env/.env.*, excepción .env.example, target/logs/node_modules/dist. Sin credenciales guardadas en evidencias. |
| 39 | Carrito | Navegador anónimo:agregar, incrementar, decrementar, F5 y login conservan contenido. Éxito vacía; POST409 por producto deshabilitado conserva carrito y no crea pedido. Producto10 restaurado disponible. |
| 40 | RECOJO | Pedido3896 creado por HTTP sin dirección obligatoria y sin Entrega automática; luego cancelado por su propietario. También cubierto por tests de checkout existentes. |
| 41 | DELIVERY | Dirección obligatoria validada400. Pedido3898 creado desde Angular con snapshot de dirección, sin Entrega antes de asignación. |
| 42 | Delivery E2E | Angular CLIENTE crea3898→CAJERO confirma/prepara/listo→asigna Entrega263 a A→REPARTIDOR inicia (Oracle DESPACHADO/EN_CAMINO)→entrega→CLIENTE consulta ENTREGADO/PAGADO. EFECTIVO se paga transaccionalmente al entregar. |
| 43 | NO_ENTREGADO | HTTP Pedido3897/Entrega262:A inicia→NO_ENTREGADO; Pedido sigue DESPACHADO; CAJERO reasigna a B→ASIGNADO→EN_CAMINO→ENTREGADO. YAPE conserva pago PENDIENTE hasta confirmación explícita CAJERO; REPARTIDOR403. YAPE/PLIN/TARJETA están además cubiertos por casos parametrizados de integración; no se afirma haber hecho tres compras electrónicas desde UI. |
| 44 | Compra E2E | Angular ABASTECIMIENTO creó Compra111 con dos detalles, total43.99, dos ENTRADA y stocks2.500L/3.125KG. HTTP y Oracle coinciden exactamente. |
| 45 | Inventario E2E | Angular SALIDA0.625L deja1.875L; AJUSTE objetivo1.250L genera delta−0.625; salida9 rechazada409 sin cambio. Oracle confirma cada movimiento. |
| 46 | Reportes E2E | Angular/HTTP/SQL del25/10 coinciden:4 ventas efectivas,80.00, ticket20.00;2 compras,87.98, promedio43.99. Stock y detalles coinciden; compras no se interpretan como costo de ventas. |
| 47 | Refresh/reload | Protocolo real refresh token exitoso y JWT renovado aceptado por API. F5 real en checkout, detalle propio, Mis pedidos y rutas internas recupera sesión. Tras reinicio de DB/KC/backend vuelve a cargar Reportes protegido. |
| 48 | Logout | UI limpia sesión y redirige; volver a interno exige login Keycloak. Logout de sesión aislada por protocolo204 y refresh posterior400. Sesiones de prueba cerradas al finalizar. |
| 49 | Reinicio/persistencia | Dos secuencias controladas propias; la final usa código corregido y nuevos E2E. Oracle/Postgres healthy, KC OIDC200, Boot inició en6.306s, Hikari conectado, ActuatorUP, Angular200. Persisten12 pedidos/4 entregas/2 compras/10 movimientos y configuración Keycloak. Sin borrar volúmenes. |
| 50 | Paquetes npm afectados | Inicial build/cli/router/MCP SDK/piscina/vite/zod:5 altos y2 críticos por paquetes, incluyendo propagación a padres. Auditoría final0. |
| 51 | Runtime/tooling | Router es runtime; avisos detectados condicionados a SSR ausente en esta SPA. Build/CLI/piscina/Vite/MCP/Zod corresponden al tooling y no al bundle comercial. Análisis de vectores y rutas en documento dedicado. |
| 52 | Correcciones disponibles | Aplicadas dentro de Angular22:framework22.2.3, build/CLI22.2.4, piscina5.3.2, zod4.6.6, reemplazo de SDK1.x en CLI y Vite8.3.3 por override. Peers estrictos, sin force ni legacy. |
| 53 | Riesgos pendientes | BAJO:mantenimiento del override Vite y warnings dev conocidos. npm audit0 no sustituye auditoría exhaustiva de imágenes/JVM. No se aceptó riesgo ALTO pendiente. Infraestructura sigue siendo dev local; configuración de producción pertenece a Fase18. |
| 54 | BLOQUEANTES | Ninguno pendiente. |
| 55 | ALTOS | Vulnerabilidades de dependencias detectadas, evaluadas y corregidas; ninguna ALTA/CRÍTICA reportada por npm al cierre. |
| 56 | MEDIOS | Dos corregidos:coerción de cantidad decimal a entero y Content-Type inválido tratado como500. |
| 57 | BAJOS | Falta de ignore .env frontend corregida; mantenimiento de override y warnings preexistentes documentados. |
| 58 | Bugs/correcciones | Jackson `accept-float-as-int:false` evita truncar1.5 a1 antes de validación. Handler específico retorna415 por Content-Type y406 por Accept no soportados, con JSON seguro. Ninguna regla de negocio nueva. |
| 59 | Regresiones | `rechazaCantidadDecimalSinTruncarlaNiCrearPedidoParcial`; nueva HttpContractIntegrationTests con415 y406. Antes del fix, cantidad esperaba400 y obtuvo404; media esperaba415 y obtuvo500. Caso406 ya pasaba antes y ahora tiene manejo explícito consistente. |
| 60 | Archivos | Lista detallada debajo.45 archivos previos conservan hashes exactos. Solo se usó Git de lectura; ningún add/commit/push/merge/reset ni modificación intencional de .git. |
| 61 | Datos generados | Pedido3896 RECOJO cancelado,3897 YAPE entregado/pagado,3898 EFECTIVO entregado/pagado; Entregas262/263. Compra111/proveedor469/insumos832 y833; Insumo831 y movimientos de concurrencia; Cliente2434 sintético inactivo. Históricos anteriores conservados. |
| 62 | Warnings | Mockito/Byte Buddy instrumentación dinámica; fetch JDBC por defecto10; Springdoc habilitado en dev. ORA-02292 de pruebas negativas es esperado y se responde409. Build Angular sin warnings; consola navegador revisada sin errores/warnings persistentes. |
| 63 | Borrado total | No se ejecutó. No down-v, eliminación de volúmenes ni recreación de schema. Solo CRUD de recursos efímeros propios no referenciados; históricos E2E se mantienen. |
| 64 | Despliegue | No realizado. Sin Nginx, VPS, dominio, certificados, puertos públicos, URLs de producción ni inicio de Fase18. |
| 65 | Commit sugerido | `fix: endurecer contratos HTTP y dependencias con verificación integral` (solo sugerencia; no ejecutado). |

## Archivos de implementación

Modificados exclusivamente para esta fase:

- `src/main/resources/application.yml`:coerción de números enteros.
- `src/main/java/pe/edu/upeu/polleria/exception/GlobalExceptionHandler.java`:415/406 y Content-Type seguro del error.
- `src/test/java/pe/edu/upeu/polleria/PedidosIntegrationTests.java`:regresión decimal.
- `../polleria-frontend/.gitignore`:entornos privados.
- `../polleria-frontend/package.json` y `package-lock.json`:resolución compatible de seguridad.

Creado `src/test/java/pe/edu/upeu/polleria/HttpContractIntegrationTests.java`,
los dos scripts de verificación y los documentos/evidencias enlazados al inicio.
No se cambió pom.xml ni se añadieron dependencias JVM. Starters de prueba tienen
scope test; devtools runtime/optional, ojdbc y Modulith actuator runtime. Lombok
opcional y excluido del paquete, MapStruct/procesador alineados1.6.3. Sin duplicación
obvia o versiones incoherentes identificadas; los starters transitivos compartidos
no justifican una limpieza de dependencias.

## Alcance de las comprobaciones adicionales

En navegador se verificaron390,768 y1440 durante formularios/diálogos E2E, sin
regresión obvia de desbordamiento del documento. Teclado Tab enfoca «Saltar al
contenido»; diálogo de entrega enfoca «Volver», controles con labels y confirmación
explícita. Esto es una revisión básica, no una certificación WCAG.

URLs directas y F5:catálogo, carrito, Mis pedidos con CLIENTE, productos, pedidos,
delivery, compras y reportes con ADMINISTRADOR. Ruta inexistente redirige claramente
a Inicio; no se añadió pantalla404. No se validó Nginx de producción.

Doble clic real en Confirmar Pedido, Registrar Compra y Registrar Salida produjo
una operación de cada tipo, confirmado por Oracle. La entrega tiene confirmación
y transición transaccional; reintentos de transición/pago ya aplicado se rechazan.
Esto verifica protección razonable, no idempotencia universal entre clientes.

La revisión de rendimiento se limitó a bundle, chunks lazy y flujos observados;
no aparecieron llamadas duplicadas con escrituras repetidas ni errores de imágenes
que impidieran el flujo. No se realizó benchmark ni auditoría exhaustiva de imágenes.

El único500 reproducido por input ordinario fue el media type del hallazgo F17-04;
se diagnosticó antes de corregir y quedó cubierto por regresión. La pasada final de
122 comprobaciones HTTP no tiene500. No se ocultan las reproducciones fallidas
previas en los logs locales. Los errores al resolver el lock viejo y un sondeo de
arranque interrumpido fueron condiciones de tooling/inicio, no defectos comerciales.

No se hizo ensayo limpio aislado opcional:la infraestructura existente y datos se
verificaron directamente, sin recrearlos. PostgreSQL de Keycloak permanece separado
de Oracle comercial. Se detiene el trabajo al cierre de Fase17.
