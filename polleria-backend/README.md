# POLLERIA — backend, Fase 6 (Consultas y reportes iniciales)

Monolito modular LP2. Java 21, Spring Boot 4.0.7, Modulith 2.0.7,
MapStruct 1.6.3, Springdoc 3.0.2 y Maven Wrapper 3.9.16.

## Desarrollo en Fedora / Bash

Desde `/home/systemseng/Polleria/polleria-backend`:

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-temurin-jdk
export PATH="$JAVA_HOME/bin:$PATH"
java -version
javac -version
./mvnw -version
```

`.env` contiene secretos locales, tiene permisos 0600 y está excluido por `.gitignore`.
Para otra instalación, copiar `.env.example` a `.env` y reemplazar los placeholders.
No publicar `.env` ni salidas de Compose que muestren las contraseñas.

```bash
docker compose -f compose-dev.yml config --quiet
docker compose -f compose-dev.yml up -d --wait oracle
```

**Solo en una base nueva**, aplicar los scripts en orden, una sola vez por tabla:

```bash
docker exec -i polleria-oracle sqlplus -s / as sysdba < scripts/01-catalogo.sql
docker exec -i polleria-oracle sqlplus -s / as sysdba < scripts/02-clientes.sql
docker exec -i polleria-oracle sqlplus -s / as sysdba < scripts/03-pedidos.sql
```

El script falla ante errores y no elimina/reemplaza tablas existentes. Oracle hace
commit implícito del DDL: no volver a ejecutar desde el inicio sobre un esquema parcial.
La instalación de desarrollo actual ya tiene las tablas; no repetir el script.

```bash
set -a
source .env
set +a
./mvnw clean compile
./mvnw test
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Esperar el mensaje `Started PolleriaBackendApplication` antes de consultar la API.
Las pruebas necesitan Oracle y el esquema aplicado; los datos de las pruebas MockMvc
se revierten al finalizar cada caso. No utilizan H2 ni sustituyen Oracle.
Cambiar `.env` no modifica contraseñas de un volumen ya inicializado.

## Infraestructura

- Oracle: `localhost:1522/FREEPDB1`, usuario de aplicación `POLLERIA`.
- Contenedor: `polleria-oracle`; volumen: `polleria-oracle-data`.
- API: `http://127.0.0.1:8080`.
- Salud: `/actuator/health`, incluye JDBC.
- OpenAPI: `/v3/api-docs`; Swagger: `/swagger-ui.html`.
- `ddl-auto: validate` y `open-in-view: false`.
- Endpoints sin autenticación únicamente durante desarrollo; Keycloak es una fase posterior.

## Catálogo (S02/S03)

`catalogo/{categoria,producto}/{controller,dto,entity,mapper,repository,service}`.
El servicio resuelve la categoría y aplica reglas; el mapper solo transforma.
Las respuestas no exponen entidades. Producto → Categoria es LAZY y unidireccional;
las consultas de producto usan EntityGraph para traer la categoría sin N+1.

Ambos recursos ofrecen GET colección, GET /{id}, POST, PUT /{id}, DELETE /{id}:
`/api/v1/categorias` y `/api/v1/productos`.
Productos admite `?categoriaId=...`; una categoría inexistente devuelve 404.
POST devuelve 201 con Location, PUT/GET 200 y DELETE 204.

CategoriaRequest:

```json
{"nombre":"Pollos","descripcion":"Pollo a la brasa","activo":true}
```

ProductoRequest (descripcion e imagenUrl son opcionales):

```json
{"nombre":"1/4 Pollo","precio":18.50,"disponible":true,"categoriaId":1}
```

- Nombre de categoría: máximo 80; se normalizan espacios y se ignoran mayúsculas
  al buscar duplicados. Índice único Oracle UPPER(TRIM(NOMBRE)) protege concurrencia.
  La normalización de espacios interiores se hace en el servicio; clientes SQL directos
  deben respetarla también.
- Nombre de producto: máximo 120; descripción: máximo 500.
- Precio BigDecimal/NUMBER(10,2), mayor que cero, hasta 8 enteros y 2 decimales.
- `activo` y `disponible` son obligatorios y representan conceptos independientes.
  Desactivar una categoría no elimina ni cambia automáticamente sus productos.
  Esta API administrativa muestra también recursos inactivos/no disponibles.
- imagenUrl opcional, máximo 1000, HTTP/HTTPS; no se descargan ni almacenan archivos.
- No existe stock. No hay unicidad de nombres de producto sin una regla solicitada.
- Categoría utilizada: DELETE devuelve 409; puede desactivarse con PUT.
- Producto sin referencias: eliminación física; las FK impiden borrar productos históricos
  utilizados en pedidos, con respuesta 409 de integridad.
- 400 entrada inválida; 404 recurso/referencia inexistente; 409 conflicto/integridad;
  500 error inesperado sin SQL ni stack trace en HTTP.
- X-Trace-ID (máximo 64 caracteres seguros) se refleja en respuesta y MDC; si no es
  válido se genera UUID. Logs diarios en logs/polleria.log (retención de 7 días).

Modulith verifica Catálogo, Clientes y Pedidos. Los paquetes públicos de consulta
producto/api y cliente/api son las únicas interfaces nombradas expuestas a Pedidos.
Las excepciones compartidas usan el paquete técnico abierto, según el patrón académico.

Security, Angular, Delivery y Abastecimiento quedan fuera de esta fase.
Git fue inicializado manualmente por el usuario; Codex no administra commits ni remotos.
El informe de Fase 2 conserva los nombres históricos originales.


## Clientes (S02 y transacciones S03)

`clientes/cliente/{controller,dto,entity,mapper,repository,service}`.
Es el segundo módulo funcional y no depende de Catálogo ni representa identidades.
No tiene contraseña, rol, sub/Keycloak, documentos ni direcciones múltiples.

CRUD: GET colección, GET /{id}, POST, PUT /{id}, DELETE /{id} sobre
`/api/v1/clientes`. POST 201 con Location, GET/PUT 200, DELETE 204.

```json
{"nombres":"Ana María","apellidos":"Pérez López","telefono":"+51912345678",
 "correo":"ana@example.com","direccionPrincipal":null,"referencia":null,"activo":true}
```

- Nombres y apellidos obligatorios, máximo 100 cada uno; el servicio retira espacios
  exteriores y reduce espacios interiores consecutivos.
- Teléfono String obligatorio: 7–9 dígitos nacionales, prefijo literal +51 opcional,
  sin separadores. No se añade prefijo ni se pierden ceros; puede compartirse.
  Es una validación sintáctica local, no acredita titularidad ni existencia de la línea.
- Correo opcional, máximo 254. El setter del Request retira espacios exteriores antes
  de @Email; ServiceImpl convierte a minúsculas con Locale.ROOT y blanco a null.
  La regla de duplicados y normalización comercial permanece en el servicio.
- Repository compara LOWER(TRIM(correo)) al crear y excluye el propio id al actualizar.
  Índice único Oracle LOWER(TRIM(CORREO)) garantiza unicidad ante concurrencia y clientes
  SQL externos. Oracle permite múltiples NULL. El formato detallado se valida con @Email.
- Dirección principal y referencia opcionales, máximo 300; blancos se guardan como null.
- Activo obligatorio en el modelo, inicial true cuando se omite; null explícito es 400.
  PUT permite desactivar; no se inventa una acción adicional.
- DELETE físico solo sin referencias: si el cliente tiene pedidos, la FK impide el borrado
  y se devuelve 409. Desactivar con PUT (`activo:false`) conserva su historial, sin
  dependencia Clientes → Pedidos.
- Reutiliza errores 400/404/409/500 y X-Trace-ID/MDC, sin otro handler o filtro.
- Solo tabla CLIENTES, con PK identity, campos requeridos NOT NULL, DEFAULT 1 y checks
  de nombres no blancos, teléfono y booleano. No hay unicidad de teléfono.

Las pruebas automatizadas usan Oracle real y rollback. Los clientes manuales se
eliminaron por HTTP después de comprobar su persistencia SQL; Catálogo se conservó.


## Pedidos y detalles (S04)

`pedidos/pedido/{controller,dto,entity,mapper,repository,service}`. DetallePedido es
parte del agregado, sin CRUD ni Repository independientes. No se importan entidades
ni repositories de otros módulos. Los contratos públicos mínimos son:

- `catalogo.producto.api.ProductoConsultaService` → `ProductoParaPedido`.
- `clientes.cliente.api.ClienteConsultaService` → `ClienteResumen`.

Los implementan los ServiceImpl existentes; no se expone el CRUD completo de otro módulo.
La anotación de Pedidos limita dependencias a esas interfaces nombradas y excepciones.
A nivel Oracle hay FK reales: PEDIDOS.CLIENTE_ID → CLIENTES.ID,
DETALLE_PEDIDOS.PEDIDO_ID → PEDIDOS.ID, DETALLE_PEDIDOS.PRODUCTO_ID → PRODUCTOS.ID.

```json
{"clienteId":1,"tipoPedido":"RECOJO","metodoPago":"EFECTIVO",
 "detalles":[{"productoId":10,"cantidad":2}]}
```

- POST `/api/v1/pedidos`: 201 con Location, agregado transaccional.
- GET `/api/v1/pedidos` y GET `/api/v1/pedidos/{id}`: 200.
- PATCH `/api/v1/pedidos/{id}/estado` con `{"estado":"CONFIRMADO"}`: 200.
- No hay PUT genérico ni DELETE de Pedido (405); tampoco endpoint de pago.

Tipo: PRESENCIAL, RECOJO, DELIVERY. Estado inicial PENDIENTE; pago inicial PENDIENTE.
Métodos EFECTIVO, YAPE, PLIN, TARJETA; estados de pago PENDIENTE, PAGADO, ANULADO.
El método se registra, sin pasarelas ni confirmación de cobros en esta fase.

Todos los pedidos requieren cliente existente y al menos una línea con cantidad
Integer positiva. No se añade un máximo comercial de cantidad: el límite SQL es el
rango representable por Integer. Subtotales/total NUMBER(19,2) tienen límite técnico
comprobado en el servicio (400 si se supera), sin redondeos silenciosos.
Productos deben existir y estar disponibles, con precio positivo. No hay stock.

Precio, nombre, subtotal y total son obtenidos/calculados en ServiceImpl con BigDecimal;
los valores ajenos al contrato enviados por HTTP no son autoritativos. Mapper solo
convierte el agregado a respuesta. Cabecera y detalles se persisten con cascade ALL y
orphanRemoval, ambos lados de la asociación correctamente establecidos.
Una excepción revierte la operación completa. No hay escritura parcial de líneas.

Precio/nombre del producto y dirección/referencia de entrega se copian al crear.
Modificar Catálogo o Cliente no cambia esas fotografías históricas. El resumen del
cliente se consulta mediante su contrato público y no se duplica en la tabla PEDIDOS.
DELIVERY exige dirección explícita no vacía (máximo 300), no se toma silenciosamente
la dirección del perfil. PRESENCIAL/RECOJO admiten null. Fecha LocalDateTime generada
por backend con America/Lima, persistida como TIMESTAMP(6).

Transiciones de esta fase:

- PENDIENTE → CONFIRMADO → EN_PREPARACION → LISTO.
- LISTO → ENTREGADO solo para PRESENCIAL/RECOJO.
- PENDIENTE/CONFIRMADO → CANCELADO solo si estadoPago no es PAGADO.
- CANCELADO y ENTREGADO son finales; no se permiten saltos, repetición ni retrocesos.
- Cancelaciones EN_PREPARACION/LISTO quedan bloqueadas hasta la operación administrativa
  con motivo y Seguridad. No se simulan roles ni se incorpora un campo futuro sin uso.
- DESPACHADO y su transición a ENTREGADO se integrarán mediante Delivery; no están
  habilitados por este endpoint. No se cancelan pedidos despachados.

Se bloquea la cabecera durante cambios de estado para serializar transiciones concurrentes.
Cliente activo y categoría activa no añaden reglas nuevas de admisión no solicitadas;
la restricción solicitada de producto disponible sí se verifica.

400: forma inválida o delivery sin dirección. 404: cliente/producto/pedido inexistentes.
409: no disponible, transición conflictiva o integridad. X-Trace-ID y handler reutilizados.
Las pruebas de rollback incluyen una petición sin transacción externa y comprueban
conteos JDBC posteriores. Los 32 casos anteriores siguen presentes y pasan.


## Consultas y reportes de Pedidos (S05)

Sin tablas ni módulo funcional Reportes, sin Pageable ni índices nuevos.
Las consultas usan JPQL/@Query y proyecciones tipadas dentro de PedidoRepository.
Los reportes no cargan entidades/detalles para agregarlos en memoria.

GET `/api/v1/pedidos` permite combinar:

- `estado`, `tipoPedido`, `clienteId` (id positivo; si no existe, devuelve lista vacía).
- `fechaDesde` y `fechaHasta`: días ISO `YYYY-MM-DD` de **creación del pedido**,
  calendario de Perú, ambos inclusive. Se comparan con `>= inicio del primer día` y
  `< inicio del día posterior al último`, incluyendo microsegundos finales sin errores.
  Pueden enviarse por separado. Rango invertido/formato inválido: 400.
- `ordenarPor`: solo `fecha` o `total`; `direccion`: ASC o DESC.
  Predeterminado fecha DESC, desempate por id en el mismo sentido. Valores inválidos: 400.

Ejemplo: `/api/v1/pedidos?tipoPedido=RECOJO&fechaDesde=2026-04-01&fechaHasta=2026-04-30&ordenarPor=total&direccion=DESC`.

Todos los reportes aceptan los mismos cinco filtros (no parámetros de orden):

- GET `/api/v1/pedidos/reportes/resumen`: cantidadPedidos, totalMonetario, ticketPromedio.
- GET `/api/v1/pedidos/reportes/productos-mas-vendidos`: productoId, nombreProducto,
  cantidadVendida, importeGenerado.
- GET `/api/v1/pedidos/reportes/por-estado`: estado y cantidadPedidos para los siete
  estados, incluidos ceros y CANCELADO.
- GET `/api/v1/pedidos/reportes/por-tipo`: tipoPedido y cantidadPedidos para los tres
  tipos, incluidos ceros.

**Regla económica:** venta efectiva = ENTREGADO **y** PAGADO. Resumen y productos
vendidos aplican esa condición además de los filtros. Su cantidadPedidos cuenta solo
esas ventas, no todos los pedidos creados. Se excluyen cancelados incluso si quedaron
pagados, anticipos sin entrega y entregas sin pago confirmado. Es un indicador de
operaciones completadas y cobradas, no un reporte de caja ni contabilidad por devengo.
No hay aún endpoint de confirmación de pago; hasta tener pagos registrados, un pedido
nuevo no suma a ventas. Las pruebas usan fixtures controlados, no una API de pagos nueva.

Ticket = totalMonetario / cantidadPedidos; BigDecimal, escala 2 y HALF_UP. Sin filas,
COUNT=0 y COALESCE(SUM,0BD) devuelve cero; ticket 0.00 sin división por cero.

Las agrupaciones por estado/tipo son operativas: cuentan todos los pedidos que cumplan
filtros, incluso cancelados/no pagados. No se les aplica la regla económica.

Productos se agrupan **solo por productoId**. Suman cantidad y subtotal histórico.
Si cambió el nombre, nombreProducto muestra MIN(nombreProducto) entre los históricos
del conjunto filtrado (según orden de Oracle): etiqueta representativa determinista,
no nombre actual ni necesariamente el último. No divide el producto en varias filas.
Ranking completo: cantidad DESC, importe DESC, productoId ASC, sin límite inventado.
Nunca consulta Producto actual para reconstruir nombre o importe de una venta anterior.

CORS de desarrollo sigue WebMvcConfigurer de S05, configurable con
`polleria.cors.allowed-origins=http://localhost:4200` en perfil dev. Solo /api/**;
métodos GET/POST/PUT/PATCH/DELETE; headers Content-Type y X-Trace-ID. Expone
X-Trace-ID/Location. Sin credenciales ni origen comodín ni dominios de producción.
Preflight permitido/prohibido comprobado por MockMvc y HTTP. Cuando llegue Spring
Security, revisar la integración CORS según S11; no se incorpora seguridad ahora.
