# Fase 5 — Pedidos y DetallePedido

24 de octubre de 2026, America/Lima. Backend en /home/systemseng/Polleria/polleria-backend.

## Alcance y decisiones

Patrón S04: agregado cabecera-detalle, sin CRUD/Repository de detalle.
Modelo Pedido: id, clienteId, fecha, tipoPedido, estado, metodoPago, estadoPago,
direccionEntrega, referenciaEntrega, total, detalles.
Detalle: id, pedido, productoId, nombreProducto, precioUnitario, cantidad, subtotal.
Enums exactamente solicitados; sin usuarios, roles, pagos externos ni stock.

Cliente y Producto se consultan por servicios/records mínimos expuestos con NamedInterface:
producto/api (producto-consulta) y cliente/api (cliente-consulta). Los ServiceImpl
existentes implementan esos contratos. No se exponen Repository/Entity ni el CRUD completo.
Pedidos declara únicamente esas dependencias y las excepciones; sin ciclos inversos.

Precio/subtotal/total se calculan en ServiceImpl con BigDecimal y dos decimales.
Mapper solo transforma respuestas; no reproduce la expresión de cálculo en mapper de S04,
porque el requerimiento de esta fase fija explícitamente esos cálculos en servicio.
Fecha generada con America/Lima. Snapshot de nombre/precio y dirección/referencia.
ClienteResumen se consulta sin duplicar los datos personales en PEDIDOS.

DELETE de Cliente: se conserva para clientes libres, pero la FK bloquea clientes
referenciados con 409 y mensaje para desactivarlos. PUT activo=false sigue permitido.
Clientes no conoce Pedidos. FK de Producto también protege productos ya utilizados.
No hay DELETE ni PUT genérico de Pedido; PATCH /{id}/estado controla el flujo.
Se añadió manejo 405 al advice existente para no convertir métodos no permitidos en 500.

Cancelación solo PENDIENTE/CONFIRMADO y nunca pagado; EN_PREPARACION/LISTO se reservan
para operación administrativa futura con motivo y Seguridad. No se habilitó despacho
sin Delivery. No hay endpoint de pago. Los estados admitidos siguen siendo los pedidos.
No se añadieron máximos comerciales: Integer limita cantidad y NUMBER(19,2) limita importes.

## SQL y regresión

03-pedidos.sql crea solo PEDIDOS y DETALLE_PEDIDOS con PK identity, 3 FK reales,
NOT NULL, checks de enums/dirección delivery/importes/cantidad y sus índices de FK.
Cantidad SQL admite hasta Integer.MAX_VALUE por correspondencia con el tipo Java.
Check de subtotal también exige precio_unitario * cantidad.
Hibernate mantiene validate. Sin modificaciones en tablas previas, Compose o POM.

## Comprobaciones ejecutadas

- Temurin 21.0.12.1; compilación release 21.
- ./mvnw clean compile: BUILD SUCCESS, 56 fuentes.
- ./mvnw test: 56 pruebas, 0 fallos, 0 errores, 0 omitidas.
  Catálogo 14, Clientes 14, Modulith 3, Oracle 1, Pedidos 24.
- Las 32 pruebas previas se conservan; Modulith amplía su reconocimiento a Pedidos.
- Pedido válido/múltiples líneas, subtotal y total exactos, precio/nombre históricos,
  dirección histórica, obligatoriedad, inexistentes/no disponibles, cantidades inválidas,
  transiciones/cancelaciones, límites de API y protección del Cliente comprobados.
- Histórica explícita: producto 20, pedido 20, producto actualizado a 25, pedido sigue 20.
- Rollback explícito sin transacción de prueba externa: A válido + B inexistente devuelve
  404 y conteos JDBC de pedidos/detalles permanecen iguales, sin filas parciales.
- Contexto Spring, Hikari y EntityManagerFactory iniciados realmente.
- JDBC localhost:1522/FREEPDB1, esquema POLLERIA; sin ERROR de arranque.
- Actuator HTTP 200: aplicación/db UP; OpenAPI y Swagger HTTP 200.
- OpenAPI mantiene POLLERIA API e incluye Pedidos y operación de estado.
- Spring Modulith verifica límites y dependencia exclusiva de contratos públicos.

## HTTP real y Oracle directo

- Cliente temporal 55 creado por HTTP (201), producto existente 10 a S/20.
- Pedido 18 RECOJO creado (201), consultado (200): 1 detalle, cantidad 2,
  precioUnitario 20, subtotal 40, total 40, estados iniciales PENDIENTE.
- Cambio CONFIRMADO 200; salto a LISTO 409.
- Delivery sin dirección 400; dos líneas con segunda inexistente 404.
- GET confirmó únicamente un pedido/una línea: no hay operaciones parciales.
- DELETE de Pedido 405; DELETE de Cliente referenciado 409.
- Producto actualizado temporalmente a S/25 por HTTP; pedido continuó S/20 y total S/40.
- SQL directo confirmó precio de Catálogo 25 frente a histórico 20, subtotal/total 40,
  cliente 55, pedido CONFIRMADO, estadoPago PENDIENTE y 0 detalles huérfanos.
- SQL verificó PK, las 3 FK a CLIENTES/PRODUCTOS/PEDIDOS, precisión, NOT NULL y checks ENABLED.
- X-Trace-ID fase5-pedidos-http conservado; errores sin SQL ni detalles Oracle en HTTP.

Limpieza segura: precio original del producto 10 restaurado a 20 mediante HTTP.
Tras comprobar identidad exacta de cliente/pedido de prueba, se limpiaron sus filas
por SQL en una transacción. Esto es limpieza excepcional de datos manuales propios,
no una operación comercial ni un DELETE público. No se eliminaron Pollos ni 1/4 Pollo.
Conteos finales: 0 pedidos, 0 detalles, 0 clientes; Catálogo conserva su producto original.

## Archivos y pendientes

Creados: pedidos/package-info.java; pedidos/pedido con enums, 2 entidades, 5 DTO,
mapper, repository, Service/ServiceImpl y controller; 2 contratos/records y package-info
por cada módulo proveedor; InvalidRequestException; scripts/03-pedidos.sql;
PedidosIntegrationTests.java y este informe.
Modificados: ProductoServiceImpl, ClienteServiceImpl, GlobalExceptionHandler,
ModularityTests y README.

Avisos heredados no bloqueantes de Fase 2 continúan; las pruebas de FK deliberadas
producen ORA-02292 internos, correctamente convertidos a 409. No se limpiaron warnings.
No se modificó Git: únicamente lecturas. Backend/Oracle quedan en ejecución.
Sin S05/reportes, Angular, Delivery, Abastecimiento ni Keycloak.

Commit sugerido (no ejecutado):
`feat: implementar pedidos transaccionales con detalle histórico y estados controlados`
