# FASE 15 COMPLETADA — Reportes ampliados y análisis operativo

Verificado el 25/10/2026, horario America/Lima. Alcance limitado a Fase 15.
Referencia: [S05 — Consultas Empresariales, Reportes y CORS](https://github.com/262ciclo4/bomerp/blob/main/docs/lp2/sesiones/S05_Consultas_Empresariales_Reportes_CORS.md).
Se aplican proyecciones JPQL tipadas, agregados en Oracle, Service/ServiceImpl con transacciones de lectura y separación de contratos HTTP. No se alteró CORS ni la autenticación existente.

## 1–4. Inventario de archivos

Las rutas del inventario al final son relativas a `/home/systemseng/Polleria`.
Incluyen archivos creados y modificados del backend/frontend y evidencias de esta fase. Los logs de ejecución quedan ignorados, como en fases anteriores.

## 5–8. API y contratos

Todos los endpoints siguientes son GET y requieren ADMINISTRADOR.

| Prefijo | Ruta | Estado | Contrato |
|---|---|---|---|
| `/api/v1/pedidos/reportes` | `/resumen` | conservado | PedidoResumenReporte: cantidadPedidos, totalMonetario, ticketPromedio |
| mismo | `/productos-mas-vendidos` | ampliado | ProductoVendidoReporte; top=20, 1–100 |
| mismo | `/por-estado` | conservado | EstadoPedidoReporte, siete estados incluso ceros |
| mismo | `/por-tipo` | conservado | TipoPedidoReporte, tres tipos incluso ceros |
| mismo | `/ventas-por-dia` | nuevo | VentaDiaReporte: fecha, cantidadVentas, totalVentas |
| mismo | `/metodos-pago` | nuevo | MetodoPagoReporte: metodoPago, cantidadVentas, totalVentas; cuatro métodos incluso ceros |
| mismo | `/clientes` | nuevo | ClienteCompradorReporte: clienteId, cantidadPedidos, totalComprado; top=20, 1–100 |
| `/api/v1/delivery/reportes` | `/por-estado` | nuevo | EntregaEstadoReporte: estado, cantidadEntregas; cuatro estados incluso ceros |
| mismo | `/resumen` | nuevo | DeliveryResumenReporte: entregasAbiertas, repartidoresActivos |
| `/api/v1/abastecimiento/reportes` | `/resumen-compras` | nuevo | CompraResumenReporte: cantidadCompras, totalComprado, compraPromedio |
| mismo | `/compras-por-proveedor` | nuevo | ProveedorCompraReporte: proveedorId, razonSocial, cantidadCompras, totalComprado |
| mismo | `/insumos-comprados` | nuevo | InsumoCompradoReporte: insumoId, nombreInsumo, unidadMedida, cantidadComprada, importeComprado |
| mismo | `/movimientos` | nuevo | MovimientoTipoReporte: tipoMovimiento, cantidadMovimientos; tres tipos incluso ceros |
| mismo | `/stock-actual` | nuevo | StockActualReporte: id, nombre, unidadMedida, stockActual, activo |

También se crean CompraAgregado y filtros ReporteFiltro locales en Delivery y Abastecimiento. No se crea un paquete funcional reportes en backend ni nuevas interfaces públicas entre módulos. Pedidos reutiliza PedidoFiltro y PedidoAgregado. Las consultas viven en los repositorios propietarios existentes. No hay Object[], Map de reporte, EntityGraph sobre agregados, carga de entidades para agrupar en Java ni consultas individuales de Cliente por fila. Solo se completan enumeraciones pequeñas en Java.

## 9–17. Fechas y métricas de Pedidos

9. `fechaDesde` y `fechaHasta` opcionales, ISO YYYY-MM-DD. Desde >= inicio del día; hasta < inicio del día siguiente. Ambos días incluidos, con calendario Perú y timestamps de la estrategia existente. Se rechazan rango inverso y años fuera de Oracle; hasta menor que 9999 evita desbordar plusDays. No se usa el reloj del navegador para decidir el período. Pedidos admite además sus filtros existentes estado/tipoPedido/clienteId.
10. Venta efectiva = ENTREGADO **y** PAGADO. Se excluyen cancelados, otros estados y entregados pendientes de pago.
11. Resumen conserva los nombres del contrato de Fase 6. COUNT/SUM/COALESCE y BigDecimal; ticket divide total/cantidad con escala 2 HALF_UP. Cero operaciones produce 0 / 0.00 / 0.00. No se usa AVG JPQL porque devuelve Double.
12. Ventas por día agrupa por `cast(truncate(p.fecha, day) as LocalDate)`, ascendente. En Oracle CAST a DATE conserva la hora: una prueba detectó esa diferencia y se resolvió truncando explícitamente. No se inventan días sin ventas.
13. Métodos EFECTIVO/YAPE/PLIN/TARJETA: conteo e importe, únicamente ventas efectivas, ceros para métodos sin ventas.
14. Productos se agrupan por productoId. Nombre representativo = MIN(nombreProducto) entre detalles filtrados, decisión de Fase 6 conservada. SUM(cantidad) y SUM(subtotal) históricos, sin join a Catálogo. top limita en base de datos mediante Pageable; desempate por importe e ID.
15. Ranking de clientes por cantidad descendente, total descendente e ID; top limitado en Oracle. Solo IDs y métricas, sin nombres personales ni consulta N+1 de Clientes. No se importan Cliente/ClienteRepository.
16. Estado incluye los siete estados y ceros; es operacional e incluye cancelados según filtros.
17. Tipo incluye PRESENCIAL/RECOJO/DELIVERY y ceros. Los filtros previos permanecen intactos.

## 18–25. Delivery y Abastecimiento

18. Delivery agrupa por estado y fechaAsignacion vigente. Incluye ASIGNADO/EN_CAMINO/ENTREGADO/NO_ENTREGADO, con ceros.
19. Reasignación sustituye fechaAsignacion y reutiliza fila; no existe historial completo de intentos. No se calcula rendimiento individual, porcentaje de éxito, tiempos ni fallas históricas por repartidor. Resumen muestra abiertos actuales y conteo de perfiles activos, sin rango.
20. Compras: COUNT, SUM/COALESCE y promedio exacto calculado con BigDecimal, dos decimales HALF_UP. Fecha de registro de Compra, rango inclusivo y ceros sin operaciones.
21. Proveedor: agrupa por proveedorId con razón social **vigente**, no histórica: Compra no guarda fotografía de razón social. No se expone RUC, correo ni dirección.
22. Insumos: agrupa por insumoId y unidad del DetalleCompra, nombre MIN histórico representativo; SUM(cantidad) y SUM(subtotal). La protección existente impide cambiar unidad cuando hay historial. Nunca suma cantidades entre insumos/unidades diferentes.
23. Movimientos: solo COUNT por ENTRADA/SALIDA/AJUSTE y fecha de registro. No suma KG con L.
24. Stock: proyección actual de todos los Insumos, unidad, cantidad y activo. No depende del rango histórico.
25. No hay inventario valorizado, stock bajo, umbrales inventados, utilidad, márgenes, costo de plato ni resta Ventas−Compras presentada como ganancia.

## 26–27. Angular y seguridad

`/interno/reportes` usa guard ADMINISTRADOR y enlace condicional del portal. Composición de servicios existentes: PedidoService, DeliveryService, CompraService, InventarioService e InsumoService. Contratos tipados en cada feature dueño; no otro servicio global que duplique sus endpoints.

Secciones Ventas/Pedidos/Delivery/Abastecimiento, filtros con labels, top, rango aplicado visible, explicación de fechas y fotografías actuales. CurrencyPipe PEN dos decimales y DecimalPipe para cantidades, locale es-PE registrado. No hay cálculo monetario crítico en frontend ni nueva dependencia de gráficos.

Signals por reporte con loading/error/filas. Fallo de una subsección no borra las demás. Al reaplicar filtros o salir se cancelan solicitudes anteriores. Listas vacías muestran mensaje y agregados en cero; sin NaN/Infinity/undefined. Tablas con caption/headers y scroll interno accesible por teclado. Roles operativos conservan sus permisos; los matchers de reportes Delivery/Abastecimiento preceden permisos generales.

## 28–33. Comparación real HTTP ↔ Angular ↔ Oracle

Rango: **2026-10-25 a 2026-10-25**.

| Métrica | HTTP | Angular | Oracle |
|---|---|---|---|
| Ventas efectivas | 2 | 2 | 2 |
| Total / ticket | 40.00 / 20.00 | S/40.00 / S/20.00 | 40.00 / 20.00 |
| Ventas por día | 25/10, 2, 40.00 | igual | igual |
| EFECTIVO / YAPE | 1 y 20.00 cada uno | igual | igual |
| PLIN / TARJETA | ceros | ceros | sin filas, equivalente a cero |
| Producto 10 | 2 unidades, 40.00 | igual | detalle histórico: 20.00 cada uno |
| Cliente 676 | 2 compras, 40.00 | igual | igual |
| Pedidos estado/tipo | ENTREGADO 2, DELIVERY 2 | igual; otros ceros | igual |
| Entregas | ENTREGADO 2; otros ceros | igual | igual |
| Operación Delivery actual | abiertos 0; activos 2 | igual | igual |
| Compra #7 / resumen | 1, 43.99, promedio 43.99 | igual | igual |
| Proveedor 44 | 1, 43.99 | igual | igual |
| Aceite 84 | 2.500 L, 30.86 | igual | detalle histórico igual |
| Papas 85 | 3.125 KG, 13.13 | igual | detalle histórico igual |
| Movimientos | ENTRADA 2, SALIDA 1, AJUSTE 1 | igual | igual |
| Stock actual | Aceite 1.250 L; Papas 3.125 KG | igual | igual |

Comparación SQL independiente por COUNT/SUM/GROUP BY contra los 14 reportes, filas enum ausentes interpretadas como cero. Evidencia estructurada en JSON HTTP y JSONL Oracle; no solo JPA ni BUILD SUCCESS. Consulta histórica usa fecha >= TIMESTAMP '2026-10-25 00:00:00' y < TIMESTAMP '2026-10-26 00:00:00'. Ventas SQL exige estado='ENTREGADO' y estado_pago='PAGADO', productos SUM(DETALLE_PEDIDOS.SUBTOTAL), compras SUM(COMPRAS.TOTAL), insumos SUM(DETALLE_COMPRAS.SUBTOTAL).

28. Rango vacío **2000-01-01**: ventas/compras cero, rankings/días/insumos sin datos; enumeraciones cero. Stock y Delivery actual se mantienen y la UI explica que son actuales.
29. Oracle confirma 12 tablas existentes, 8 Pedidos, 2 Entregas y 4 Movimientos. Ninguna tabla de reporte.
30. Prueba HTTP controlada: Producto10 de S/20 a S/25 temporalmente; ranking conserva 2 unidades y S/40.00. Precio original restaurado a S/20. Las pruebas automatizadas también cubren cambio de nombre/precio y fecha.
31. Prueba HTTP controlada: Insumo84 renombrado temporalmente a Aceite vegetal prueba F15; reporte mantiene Aceite prueba F14 y 30.86. Nombre original restaurado, sin cambio de stock/unidad. Prueba automatizada equivalente Aceite→Aceite vegetal.
32. Dos Entregas actuales ENTREGADO, cero abiertas, dos repartidores activos. Fixture transaccional comprueba los cuatro estados y filtro de asignación real en Oracle.
33. Compra7 S/43.99 intacta; detalles 30.86 + 13.13, unidades L/KG separadas. Fixtures prueban dos compras, total48.99/promedio24.50, rango y redondeo.

## 34–48. Resultados técnicos

34. `./mvnw clean compile`: **BUILD SUCCESS**, Java21.0.12.1.
35. `./mvnw test`: **BUILD SUCCESS**, contexto real con Oracle.
36. **198 pruebas backend**, 0 fallos/errores/omitidas; mantiene las 180 y agrega 18. ReportesIntegrationTests24 (antes19), ReportesAmpliadosIntegrationTests13, restantes161.
37. `npm run build`: **correcto**, bundle inicial aproximado328kB, reportes lazy; sin nueva dependencia.
38. `npm test -- --watch=false`: **correcto**.
39. **98 pruebas frontend**, ocho archivos, mantiene89 y agrega9. Carga/filtros/moneda/vacíos/error aislado/cancelación/roles/ruta/navegación.
40. Spring Modulith: tres pruebas correctas, verify sin ciclos y documentación generada.
41. Cinco módulos comerciales exactos: catalogo, clientes, pedidos, delivery, abastecimiento. exception/filter siguen infraestructura compartida anterior.
42. Spring Boot real iniciado en8080; Oracle1522/FREEPDB1 POLLERIA; Actuator **UP** sin ampliar exposición. Angular real en4200 devuelve200.
43. OpenAPI **POLLERIA API**, Bearer existente, catorce endpoints de reporte registrados, filtros/semántica documentados. Etiquetas Delivery/Abastecimiento reportes y Pedidos. Swagger conserva exposición de desarrollo aprobada.
44. Keycloak comprobado por Admin API solo lectura: realm polleria, public client, Standard Flow, PKCE S256, password grant deshabilitado, callback exacto y origen4200. Cinco roles conservados; logins reales Code+PKCE y logout. Sin cambios de configuración ni usuarios.
45. HTTP real en **14 endpoints × 6 identidades**: ADMIN200, CAJERO403, ABASTECIMIENTO403, REPARTIDOR403, CLIENTE403, anónimo401. Browser: CAJERO y ABASTECIMIENTO sin enlace y Acceso denegado por URL directa. Permisos operativos verificados por GET real y suites anteriores.
46. Responsive390/768/1440: ancho documento375/753/1425 respectivamente, sin desbordamiento horizontal global. En390, wrappers de341px y tablas anchas con scroll interno. Labels de filtros, estados escritos y no solo colores.
47. Consola del navegador sin errores relevantes, CORS ni bucles401; ningún token impreso. Regresión de Catálogo/Clientes/Pedidos/Delivery/Abastecimiento por HTTP real y 180+89 pruebas existentes. Browser cliente: catálogo/carrito/checkout (solo carga, sin enviar pedido), perfil y Mis pedidos; repartidor: Mis entregas; login/logout y portal. El ítem añadido al carrito durante comprobación fue retirado.
48. `npm audit`: **7 hallazgos conocidos, 5 altos y 2 críticos**; sin fix --force, instalaciones ni cambios de familia Angular. Ver npm-audit-fase-15.json. No se corrigen vulnerabilidades generales fuera del alcance autorizado.

## 49–53. Conservación, límites y cierre

49. Sin nuevos registros comerciales permanentes. Fixtures nuevos transaccionales con rollback; anteriores conservan su limpieza exacta propia. Pedidos1737/1738, Entregas21/22, Compra7 y sus cuatro movimientos permanecen. Cambios temporales de precio/nombre restaurados mediante finally. No se eliminó historial ni recursos ajenos.
50. Warnings previos: fetch size JDBC10, ByteBuddy/agente dinámico en pruebas, avisos Springdoc de desarrollo, allowScripts npm documentado previamente. Sin limpieza general de infraestructura. Los warnings ORA esperados en pruebas de restricciones no son fallos de contexto.
51. Adaptaciones a S05: dominio POLLERIA con cinco propietarios, venta efectiva ENTREGADO+PAGADO, filtros de día ISO con final exclusivo (frente a timestamps de la guía), IDs para ranking de clientes por privacidad/modularidad; razón social vigente por no tener fotografía en Compra. Se conserva contrato resumen Fase6; top por defecto20 se agrega por requerimiento, 1–100. Oracle requiere TRUNC explícito para día. Servicios de reporte están dentro del recurso dueño y siguen interfaz/ServiceImpl, sin sexto módulo. No hay otra desviación funcional.
52. **Sin módulo Reportes backend, tablas nuevas, migración07, índices prematuros, NamedInterface anticipadas ni cambios Keycloak**. Tampoco recetas, relación Producto-Insumo, consumo automático, inventario valorizado, utilidades, exportaciones o fases16–18. Git usado solo con status/diff de lectura; sin add/commit/push/pull/merge/rebase/reset.
53. Commit sugerido (no ejecutado): `feat(reportes): ampliar consultas administrativas de ventas delivery y abastecimiento`.

## Evidencias

- `verificacion-http-fase-15.json`: resultados reales, matriz de roles, vacío, reglas y cambios históricos restaurados.
- `verificacion-oracle-fase-15.jsonl`: resultados SQL directos en Oracle, sin datos personales innecesarios.
- `verificacion-keycloak-fase-15.json`: configuración revisada, sin credenciales/tokens.
- `../polleria-frontend/verificacion-navegador-fase-15.json`: tablas visibles, vacío, roles y responsive.
- `../polleria-frontend/npm-audit-fase-15.json`: auditoría de dependencias.
- Logs ignorados: fase15-compile.log, fase15-test.log, fase15-backend.log; frontend-build-fase-15.log, frontend-test-fase-15.log.

## Inventario exacto

### polleria-backend — creados (24)

- `polleria-backend/VERIFICACION-FASE-15.md`
- `polleria-backend/src/main/java/pe/edu/upeu/polleria/abastecimiento/compra/controller/CompraReporteController.java`
- `polleria-backend/src/main/java/pe/edu/upeu/polleria/abastecimiento/compra/dto/CompraAgregado.java`
- `polleria-backend/src/main/java/pe/edu/upeu/polleria/abastecimiento/compra/dto/CompraResumenReporte.java`
- `polleria-backend/src/main/java/pe/edu/upeu/polleria/abastecimiento/compra/dto/InsumoCompradoReporte.java`
- `polleria-backend/src/main/java/pe/edu/upeu/polleria/abastecimiento/compra/dto/ProveedorCompraReporte.java`
- `polleria-backend/src/main/java/pe/edu/upeu/polleria/abastecimiento/compra/dto/ReporteFiltro.java`
- `polleria-backend/src/main/java/pe/edu/upeu/polleria/abastecimiento/compra/service/CompraReporteService.java`
- `polleria-backend/src/main/java/pe/edu/upeu/polleria/abastecimiento/compra/service/CompraReporteServiceImpl.java`
- `polleria-backend/src/main/java/pe/edu/upeu/polleria/abastecimiento/insumo/dto/StockActualReporte.java`
- `polleria-backend/src/main/java/pe/edu/upeu/polleria/abastecimiento/inventario/dto/MovimientoTipoReporte.java`
- `polleria-backend/src/main/java/pe/edu/upeu/polleria/delivery/entrega/controller/EntregaReporteController.java`
- `polleria-backend/src/main/java/pe/edu/upeu/polleria/delivery/entrega/dto/DeliveryResumenReporte.java`
- `polleria-backend/src/main/java/pe/edu/upeu/polleria/delivery/entrega/dto/EntregaEstadoReporte.java`
- `polleria-backend/src/main/java/pe/edu/upeu/polleria/delivery/entrega/dto/ReporteFiltro.java`
- `polleria-backend/src/main/java/pe/edu/upeu/polleria/delivery/entrega/service/EntregaReporteService.java`
- `polleria-backend/src/main/java/pe/edu/upeu/polleria/delivery/entrega/service/EntregaReporteServiceImpl.java`
- `polleria-backend/src/main/java/pe/edu/upeu/polleria/pedidos/pedido/dto/ClienteCompradorReporte.java`
- `polleria-backend/src/main/java/pe/edu/upeu/polleria/pedidos/pedido/dto/MetodoPagoReporte.java`
- `polleria-backend/src/main/java/pe/edu/upeu/polleria/pedidos/pedido/dto/VentaDiaReporte.java`
- `polleria-backend/src/test/java/pe/edu/upeu/polleria/ReportesAmpliadosIntegrationTests.java`
- `polleria-backend/verificacion-http-fase-15.json`
- `polleria-backend/verificacion-keycloak-fase-15.json`
- `polleria-backend/verificacion-oracle-fase-15.jsonl`

### polleria-backend — modificados (12)

- `polleria-backend/README.md`
- `polleria-backend/src/main/java/pe/edu/upeu/polleria/SecurityConfig.java`
- `polleria-backend/src/main/java/pe/edu/upeu/polleria/abastecimiento/compra/repository/CompraRepository.java`
- `polleria-backend/src/main/java/pe/edu/upeu/polleria/abastecimiento/insumo/repository/InsumoRepository.java`
- `polleria-backend/src/main/java/pe/edu/upeu/polleria/abastecimiento/inventario/repository/MovimientoInventarioRepository.java`
- `polleria-backend/src/main/java/pe/edu/upeu/polleria/delivery/entrega/repository/EntregaRepository.java`
- `polleria-backend/src/main/java/pe/edu/upeu/polleria/delivery/repartidor/repository/RepartidorRepository.java`
- `polleria-backend/src/main/java/pe/edu/upeu/polleria/pedidos/pedido/controller/PedidoController.java`
- `polleria-backend/src/main/java/pe/edu/upeu/polleria/pedidos/pedido/repository/PedidoRepository.java`
- `polleria-backend/src/main/java/pe/edu/upeu/polleria/pedidos/pedido/service/PedidoService.java`
- `polleria-backend/src/main/java/pe/edu/upeu/polleria/pedidos/pedido/service/PedidoServiceImpl.java`
- `polleria-backend/src/test/java/pe/edu/upeu/polleria/ReportesIntegrationTests.java`

### polleria-frontend — creados (12)

- `polleria-frontend/VERIFICACION-FASE-15.md`
- `polleria-frontend/npm-audit-fase-15.json`
- `polleria-frontend/src/app/features/abastecimiento/reporte.model.ts`
- `polleria-frontend/src/app/features/delivery/reporte.model.ts`
- `polleria-frontend/src/app/features/pedidos/reporte.model.ts`
- `polleria-frontend/src/app/features/reportes/reporte-tabla.ts`
- `polleria-frontend/src/app/features/reportes/reportes.css`
- `polleria-frontend/src/app/features/reportes/reportes.html`
- `polleria-frontend/src/app/features/reportes/reportes.spec.ts`
- `polleria-frontend/src/app/features/reportes/reportes.ts`
- `polleria-frontend/src/app/shared/reporte-filtro.ts`
- `polleria-frontend/verificacion-navegador-fase-15.json`

### polleria-frontend — modificados (8)

- `polleria-frontend/README.md`
- `polleria-frontend/src/app/app.routes.ts`
- `polleria-frontend/src/app/core/layout/interno/interno.html`
- `polleria-frontend/src/app/features/abastecimiento/compra-service.ts`
- `polleria-frontend/src/app/features/abastecimiento/insumo-service.ts`
- `polleria-frontend/src/app/features/abastecimiento/inventario-service.ts`
- `polleria-frontend/src/app/features/delivery/delivery-service.ts`
- `polleria-frontend/src/app/features/pedidos/pedido-service.ts`

