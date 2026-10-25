# Fase 6 — Consultas empresariales y reportes iniciales

24 de octubre de 2026, America/Lima. Ruta /home/systemseng/Polleria/polleria-backend.

## Decisiones

Referencia S05 revisada antes de modificar código. JPQL, filtros opcionales, Sort
seguro, SELECT new tipado, COUNT/SUM/COALESCE y promedio BigDecimal en el servicio.
No se cambió esquema, no hay 04-reportes.sql, tablas, índices, módulos o dependencias nuevas.

Venta efectiva: ENTREGADO y PAGADO. Resumen y ranking suman solo operaciones completadas
y cobradas, por fecha de creación del pedido. No son caja/contabilidad ni ingresos por
fecha de pago. CantidadPedidos del resumen cuenta ventas efectivas, no todos los creados.
Cancelados se excluyen incluso pagados. Pendientes pagados y entregados no pagados tampoco
suman. No se añade operación de cobro: los datos de pago de pruebas son fixtures explícitos.

Agrupaciones por estado/tipo cuentan pedidos operativos con filtros, incluidos cancelados,
y completan en Java los enums ausentes con cero sin cargar entidades para agregarlos.

Ranking agrupa principalmente por productoId y usa SUM de cantidades/subtotales históricos.
El nombre representativo es MIN(nombreProducto) de esos detalles (orden Oracle), no el
nombre actual ni el más reciente; evita duplicar filas al cambiar de nombre.
Orden fijo cantidad DESC, importe DESC, id ASC. No se limita ni pagina sin necesidad.

Fechas públicas fechaDesde/fechaHasta son días ISO inclusivos: [inicio, siguiente día).
Adaptación deliberada frente al LocalDateTime de S05 para evitar excluir el último día
con fechas de calendario. Se filtra fecha de creación, guardada con hora de Perú.
Fecha/total son los únicos campos de Sort; dirección ASC/DESC, desempate por id.
No hay acceso a Repository de Clientes para filtrar clienteId; id inexistente da vacío.

## API

GET /api/v1/pedidos acepta estado, tipoPedido, clienteId, fechaDesde, fechaHasta,
ordenarPor (fecha/total) y direccion (ASC/DESC).
Reportes bajo /api/v1/pedidos/reportes/: resumen, productos-mas-vendidos, por-estado,
por-tipo; mismos cinco filtros, sin orden dinámico. Son recursos de Pedidos y conservan
el patrón colección + consulta GET de S05, con subruta para agrupar las cuatro vistas.

DTO nuevos: PedidoFiltro, PedidoAgregado, PedidoResumenReporte, ProductoVendidoReporte,
EstadoPedidoReporte, TipoPedidoReporte. Todos tipados, sin Object[]/Map en reportes.
CORS: WebMvcConfigurer S05, orígenes por perfil, localhost:4200, /api/**,
headers Content-Type/X-Trace-ID, expone Trace-ID/Location. Sin credenciales ni wildcard.
No se construyó Angular; se verificó el contrato servidor/preflight, no una SPA real.

## Resultados

- ./mvnw clean compile: BUILD SUCCESS, 63 fuentes, Java/release 21.
- ./mvnw test: 75 pruebas, 0 fallos, 0 errores, 0 omitidas.
  56 anteriores conservadas + 19 de ReportesIntegrationTests.
- Tests: cada filtro, combinaciones, fronteras de día/microsegundos, límites unilaterales,
  vacío, orden permitido/inválido, fechas inválidas, resumen exacto/cero/redondeo HALF_UP,
  cancelados pagados excluidos, entregados no pagados excluidos, históricos/renombrado,
  agrupaciones, ranking y CORS origen permitido/prohibido.
- Regression de Catálogo/Clientes/Pedidos: incluido CRUD, foto histórica, rollback y estados.
- Modulith: correcto, mismos tres módulos funcionales; ningún módulo Reportes.
- Spring Boot real, ApplicationContext/Hikari/EntityManagerFactory iniciados sin ERROR.
- Oracle localhost:1522/FREEPDB1, esquema POLLERIA.
- Actuator HTTP 200 db/application UP, OpenAPI/Swagger HTTP 200.
- OpenAPI muestra los siete parámetros del listado y las cuatro rutas nuevas.

## Flujo HTTP y contraste SQL

Cliente temporal 132 y pedidos 171/172/173/174 creados mediante HTTP con producto 10.
171 y 172 recorrieron por API el flujo a ENTREGADO; 173 se canceló; 174 quedó PENDIENTE
DELIVERY. Solo para estos fixtures se fijaron pagos/fechas por SQL con guardas de identidad:
- 171: 2026-04-05 00:00:00, entregado/pagado, 2 × S/20 = S/40.
- 172: 2026-04-05 23:59:59.999999, entregado/pagado, 1 × S/20 = S/20.
- 173: mismo día, cancelado/pagado, S/100 (se debe excluir de ventas).
- 174: 2026-04-06 00:00:00, pendiente/no pagado, S/20.

HTTP verificó filtros individuales/combinados, rango día 5 con tres pedidos, orden por
total, rechazos 400 de orden/rango, resumen con dos ventas por S/60 y ticket S/30,
resumen de cancelados en cero, resumen vacío cero, estados/tipos y ranking 3 unidades/S/60.
Precio actual se cambió temporalmente a S/25; reporte siguió sumando históricos de S/20.
X-Trace-ID fase6-http se conservó. Preflight localhost:4200 200 con Allow-Origin correcto;
origen no permitido 403 sin Allow-Origin. No se comprobó un cliente Angular inexistente.

SQL directo confirmó 2 ventas, S/60 y ticket S/30; cancelado de S/100 separado;
ranking 3 unidades/S/60 frente a precio actual S/25. Tablas exactamente las cinco previas:
CATEGORIAS, PRODUCTOS, CLIENTES, PEDIDOS y DETALLE_PEDIDOS.

Se restauró el producto 10 a S/20 mediante HTTP y se limpiaron exclusivamente los fixtures
identificados por id/cliente/nombre en una transacción SQL. Quedaron 0 clientes/pedidos/
detalles temporales, conservando Pollos y 1/4 Pollo.

## Archivos y límites

Creados: 6 DTO, CorsConfig.java, ReportesIntegrationTests.java y este informe.
Modificados: PedidoRepository, PedidoService, PedidoServiceImpl, PedidoController,
application-dev.yml y README.md. No cambios transaccionales, enums ni SQL existente.

Se mantuvieron los avisos no bloqueantes heredados; no hubo limpieza de infraestructura.
No se añadieron anexos opcionales de observabilidad de S05 ajenos a este alcance.
Sin Git de escritura, Angular, Delivery, Abastecimiento ni Keycloak.
Backend/Oracle permanecen ejecutándose. Se detiene el trabajo al cerrar esta fase.

Commit sugerido (no ejecutado):
`feat: agregar filtros y reportes de pedidos con ventas efectivas y CORS de desarrollo`
