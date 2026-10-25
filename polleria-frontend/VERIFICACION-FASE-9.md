# Fase 9 — Formulario transaccional de Pedidos

Verificado el 24 de octubre de 2026 (America/Lima), 25 de octubre UTC.
Frontend: `/home/systemseng/Polleria/polleria-frontend`.
Backend: `/home/systemseng/Polleria/polleria-backend`, sin cambios.

## Alcance y referencia

Guía S09 consultada antes de implementar:
https://github.com/262ciclo4/bomerp/blob/main/docs/lp2/sesiones/S09_Formularios_Transaccionales_Cabecera_Detalle.md

Se implementa únicamente la operación interna Pedido + DetallePedido: listado,
creación, consulta histórica y cambio de estado. Se conservan S07/S08, standalone,
zoneless, Router lazy, Material, HttpClient, API centralizada y único X-Trace-ID.
No se implementan Keycloak, login, roles, JWT, Entrega, Delivery como módulo,
Abastecimiento, carrito/checkout público, pagos reales, dashboard ni reportes ampliados.

## Archivos creados

`src/app/features/pedidos/`:

- pedido.model.ts: modelos HTTP, enums exactos, etiquetas y transiciones visibles.
- pedido-service.ts: GET colección/id, POST, PATCH estado.
- pedido-list.ts / pedido-list.html: listado con filtros en servidor.
- pedido-form.ts / pedido-form.html: cabecera, FormArray, autocomplete y confirmación.
- pedido-detalle.ts / pedido-detalle.html: consulta histórica y transiciones controladas.
- confirmar-pedido.ts: diálogo Material para creación, cambios y cancelación.
- pedido.css: distribución de cabecera, líneas y detalle; adaptación móvil.
- pedido.spec.ts: 13 pruebas nuevas.

Además: npm-audit-fase-9.json y este documento.

## Archivos modificados

- src/app/app.routes.ts: tres rutas lazy de Pedidos dentro de Interno.
- src/app/core/layout/interno/interno.html: enlace Pedidos.
- README.md: alcance, arquitectura, ejecución y verificaciones actuales.

Sin cambios a package.json/package-lock.json, versiones, componentes CRUD previos,
configuración backend, Oracle, contenedores ni volúmenes.

## Rutas y contrato

- `/interno/pedidos`: listado.
- `/interno/pedidos/nuevo`: creación.
- `/interno/pedidos/:id`: detalle histórico y seguimiento.

No hay ruta editar, DELETE ni PUT de Pedido.

PedidoRequest y DetallePedidoRequest se distinguen de PedidoResponse y
DetallePedidoResponse. Cliente de respuesta contiene id, nombres y apellidos.
Los enums se contrastaron con Java, incluido EstadoPago: PENDIENTE/PAGADO/ANULADO.

Ejemplo exacto de request comprobado automáticamente:

```json
{
  "clienteId": 1,
  "tipoPedido": "RECOJO",
  "metodoPago": "EFECTIVO",
  "direccionEntrega": null,
  "referenciaEntrega": null,
  "detalles": [{ "productoId": 10, "cantidad": 2 }]
}
```

No envía nombreProducto, precioUnitario, subtotal, total, fecha, estado, estadoPago
ni busqueda. PedidoService utiliza ApiService: no repite localhost ni crea endpoints.

## Formulario y reglas

- Cabecera mediante Reactive Forms: cliente, tipo, método y dirección/referencia.
- Clientes cargados desde ClienteService. Selector con nombre, apellido y teléfono;
  apropiado para la cantidad actual. Se rechaza un ID fuera de las opciones cargadas.
- Tipo: PRESENCIAL, RECOJO o DELIVERY; método: EFECTIVO, YAPE, PLIN o TARJETA.
- Dirección obligatoria solo para DELIVERY, máximo 300; referencia opcional, máximo 300.
- La copia de dirección es explícita con Usar dirección del cliente. No hay precarga
  automática que sobrescriba ediciones al cambiar cliente. El request de no Delivery
  envía dirección/referencia null; alternar el tipo conserva el borrador en el formulario.
- FormArray agrega y quita líneas, conservando como mínimo una fila visible.
- Un validador propio también rechaza una colección vacía aunque se vacíe por código.
- Cantidad: entero positivo hasta Integer.MAX_VALUE, coherente con Integer Java;
  el validador no depende de type=number ni del atributo step.
- Autocomplete de Producto con nombre, ID y precio. Escribir no selecciona un ID:
  cada entrada de texto invalida la selección previa hasta elegir una opción.
- Solo productos disponible=true. No existe stock ni lógica de inventario.
- Duplicados: se ocultan productos elegidos en otras líneas, se impide seleccionarlos
  dos veces y se valida la colección. Se ajusta la cantidad en la línea existente.
- FormArray rastrea cada FormGroup por identidad, como S09, para quitar una línea
  intermedia sin desplazar incorrectamente nombres o cantidades.

Los datos, cargas, errores y filtros usan signals. valueChanges se convierte con
toSignal y los precios, subtotales y total visual se derivan con computed.
La estimación usa céntimos para reducir artefactos habituales de coma flotante;
no se envía y no sustituye BigDecimal ni el recálculo del backend. CurrencyPipe
muestra PEN con dos decimales. No se instala otra biblioteca de estado o dinero.

Confirmación Material antes de POST: cliente, tipo, método, líneas y total informativo.
Cancelar conserva el formulario sin POST. Se bloquean formulario y envíos repetidos
mientras se confirma/guarda. El POST devuelve la operación canónica, utilizada al
navegar al detalle; GET también confirma la información y funciona tras refrescar.

Los errores 400/404/409 usan el contrato seguro del GlobalExceptionHandler y los 500
usan mensaje general. No se muestran SQL, stack traces ni JSON crudo. Ante 404/409 de
creación se conservan los campos y se recargan clientes/productos; el backend decide
si las referencias existen y si la disponibilidad sigue siendo válida.

## Listado, histórico y estados

Listado: ID, fecha, cliente, tipo, estado, estadoPago, total backend y Ver detalle.
Filtros estado/tipo se envían como query params al servidor. No se filtra la colección
de pedidos en memoria, ni se construye un dashboard. “Todos” omite el parámetro.

Detalle: cabecera, dirección/referencia si existen, nombre histórico de productos,
precios unitarios históricos, cantidades, subtotales y total real. No consulta
ProductoService para sustituir precios de pedidos guardados.

Transiciones ofrecidas, cotejadas con PedidoServiceImpl:

| Origen | Destinos |
| --- | --- |
| PENDIENTE | CONFIRMADO; CANCELADO si no está PAGADO |
| CONFIRMADO | EN_PREPARACION; CANCELADO si no está PAGADO |
| EN_PREPARACION | LISTO |
| LISTO presencial/recojo | ENTREGADO |
| LISTO delivery | Ninguno: requiere el futuro módulo Delivery |
| ENTREGADO, CANCELADO, DESPACHADO | Ninguno |

Cada PATCH solicita confirmación y usa la respuesta real. Cancelar pedido no elimina
el recurso ni modifica estadoPago. No se ofrece Despachado como acción. Puede aparecer
en el filtro de consulta porque pertenece al enum del backend; no habilita transición.
Un 409 del servidor conserva el pedido y muestra el mensaje, con opción de actualizar.

## Pruebas automatizadas

Comando existente: `npm test -- --watch=false`.
Resultado final: **31 pruebas exitosas en 3 archivos: 18 anteriores + 13 nuevas**.
Las suites anteriores permanecen intactas.

Pruebas nuevas:

1. PedidoService: contrato de GET colección/id, POST, PATCH y filtros; sin PUT/DELETE.
2. Carga de clientes/productos, disponibilidad e ID seleccionado frente a texto libre.
3. FormArray: añadir, quitar primera línea, conservar identidad y recalcular importes.
4. Colección vacía y cantidades cero, negativas, decimales o fuera del rango Integer.
5. Duplicados bloqueados en opciones, selección y validador de colección.
6. Dirección condicional Delivery y protección de dirección manual al cambiar cliente.
7. IDs inexistentes en las opciones de cliente/producto no se envían.
8. Cancelar confirmación no hace POST ni pierde datos.
9. Request exacto sin precios/total/estados; bloqueo de doble envío; respuesta con
   precio/total diferente al estimado se muestra como autoridad; sin consulta de Catálogo.
10. 409 por disponibilidad: formulario recuperable y recarga de opciones.
11. Todas las transiciones permitidas/no ofrecidas, incluida cancelación de pagados.
12. PATCH confirmado, cancelación del diálogo, transición imposible sin PATCH y 409.
13. Listado con importes de servidor y filtros reales HTTP.

Las pruebas compartidas anteriores conservan cobertura de mensajes 400/404/409,
500 y desconexión. Durante la verificación se detectó y corrigió un efecto de
valueChanges al deshabilitar el formulario: disable/enable usa emitEvent=false para
mantener el bloqueo sin disparar la validación condicional durante la confirmación.

`npm run build`: correcto, sin warnings de presupuesto. Log local ignorado por Git:
frontend-build-fase-9.log. Pruebas: frontend-test-fase-9.log.
Servidor real: `npm start -- --host localhost --port 4200`.

## Verificación funcional real en navegador

Angular localhost:4200, Spring Boot localhost:8080, persistencia Oracle real.
No se usaron mocks para estos flujos.

### Preparación y creación

Cliente de prueba creado mediante CRUD: id 172, Prueba Fase 9 Transaccional,
sin correo, con dirección y referencia de prueba.

Pedido **175**: RECOJO, EFECTIVO, producto 10 (1/4 Pollo), cantidad 2.
Subtotal/total visual S/40.00. Se canceló una vez la confirmación antes de confirmar.
POST correcto: ambos estados PENDIENTE, precio unitario S/20 y total S/40.
Detalle y listado reflejan los datos persistidos.

### Histórico y restauración

Se editó Producto 10 desde el CRUD: S/20 → S/25. La tabla de Productos mostró S/25.
Se recargó Pedido 175: siguió mostrando precio histórico S/20, cantidad 2, total S/40.
Se restauró Producto 10 desde el mismo CRUD a S/20, comprobado en listado y API final.
El precio y la disponibilidad originales se conservaron al terminar.

### Estados

Pedido 175 recorrió con PATCH desde la interfaz y respuesta comprobada cada vez:
PENDIENTE → CONFIRMADO → EN_PREPARACION → LISTO → ENTREGADO.
Al terminar ya no se ofrecen transiciones.

Prueba real adicional por HTTP: ENTREGADO → PENDIENTE rechazada **409**:
“Transición de pedido no permitida: ENTREGADO → PENDIENTE”. No se cambió el estado.

### Múltiples detalles y cancelación

Producto de prueba creado mediante CRUD: id 95, Producto prueba F9, S/5.50,
categoría Pollos.

Pedido **176**, PRESENCIAL/EFECTIVO:

- 1/4 Pollo: 2 × S/20 = S/40.
- Producto prueba F9: 3 × S/5.50 = S/16.50.
- Total visual y real: **S/56.50**, dos detalles devueltos/persistidos.

Se canceló mediante diálogo y PATCH: CANCELADO. Se conserva el recurso y las líneas;
no se ejecuta DELETE. El filtro Cancelado en navegador muestra solo este pedido.

### Delivery y concurrencia

Formulario DELIVERY sin dirección: mensaje obligatorio y sin confirmación/POST.
Se copió dirección del cliente con acción explícita y se revisó a Dirección efectiva F9.
Método YAPE, referencia conservada.

Con Producto 95 seleccionado en un formulario ya cargado, se cambió disponible=false
desde Productos en otra pestaña. Al confirmar, el backend rechazó el pedido:
“Producto no disponible: 95” (contrato 409). La pantalla conservó cabecera, dirección,
referencia y cantidad, y recargó opciones. El producto ya no aparecía seleccionable.

Se seleccionó Producto 10 disponible y se confirmó. Pedido **177**: DELIVERY/YAPE,
precio S/20, cantidad 1, total S/20, ambos estados inicialmente PENDIENTE,
dirección efectiva y referencia correctas. Sin crear Entrega.

Recorrió CONFIRMADO → EN_PREPARACION → LISTO. En LISTO la interfaz muestra que
Delivery será necesario y no ofrece ni Entregado ni Despachado.
Solicitud HTTP real LISTO → ENTREGADO: **409**, estado LISTO conservado.

## Datos conservados al terminar

No se borraron pedidos ni se ejecutó SQL para limpiar historial.

| Recurso | Estado final |
| --- | --- |
| Pollos id 24 | Conservada, activa |
| 1/4 Pollo id 10 | S/20, disponible=true, descripción original |
| Producto prueba F9 id 95 | S/5.50, disponible=false |
| Cliente id 172 | Conservado por sus pedidos |
| Pedido 175 | RECOJO, ENTREGADO, pago PENDIENTE, S/40 |
| Pedido 176 | PRESENCIAL, CANCELADO, pago PENDIENTE, S/56.50 |
| Pedido 177 | DELIVERY, LISTO, pago PENDIENTE, S/20 |

Producto 95 no se eliminó: FK_DETALLE_PRODUCTO lo referencia desde el Pedido 176.
No es seguro borrarlo; se dejó no disponible y claramente identificado como prueba.
Cliente 172 tiene FK_PEDIDOS_CLIENTE; también se conserva. Esto respeta el historial
y la instrucción de eliminar el producto temporal únicamente si fuera seguro.

## Responsive, consola y regresión

- FormArray comprobado a 390, 768 y 1440 px, con agregar/quitar y diseño vertical móvil.
- Listado y detalle comprobados en móvil. Sin desbordamiento horizontal en las mediciones.
- Refresco directo del detalle correcto.
- Navegación y listados de Categorías/Clientes correctos.
- CRUD Productos utilizado realmente para alta, precio, restauración y disponibilidad.
- CRUD Cliente utilizado realmente para el alta y sus campos opcionales.
- Diagnóstico real: HTTP 200, CORS y X-Trace-ID legible:
  `2d50a038-b077-4af3-a238-c124dd731d2d`.
- Área pública base conservada.
- Consola final de sesión limpia: sin errores ni advertencias.
- Se corrigió un warning NG0956 detectado durante las transiciones: las posiciones de
  botones de acción se rastrean por índice al sustituirse un estado por el siguiente.
  Las líneas editables FormArray siguen rastreándose por FormGroup, como exige S09.

## Backend e infraestructura

Sin cambios backend: no se volvió a ejecutar innecesariamente su suite de 75 pruebas.
No se declara una nueva ejecución de esa suite. Solo se modificó frontend.
Actuator real UP y componente db Oracle UP después de los flujos. GET /pedidos confirma
los tres pedidos con sus detalles, estados e importes. GET /productos confirma la
restauración a S/20 y producto de prueba no disponible. GET /clientes confirma id 172.
No se tocaron recursos ajenos ni configuración funcional de Fase 2.

## npm audit y warnings pendientes

Nueva auditoría: npm-audit-fase-9.json.
Sin cambios: **7 hallazgos, 5 altos y 2 críticos**. Paquetes señalados:
@angular/build, @angular/cli, @angular/router, @modelcontextprotocol/sdk, piscina,
vite y zod. Los dos críticos son build/piscina. Material/CDK/framework conservan sus
versiones. Sin npm audit fix --force, upgrades ni overrides.
La revisión definitiva sigue pendiente antes de despliegue. Los avisos previos de
scripts npm y warnings backend permanecen documentados en fases anteriores; no se
hizo limpieza general ni se instalaron dependencias nuevas.

## Adaptaciones de S09

- Pedido POLLERIA tiene cabecera editable (cliente/tipo/método/dirección), distinta
  de Venta BOMERP. Se respeta el DTO real y no se copia lógica de stock/anulación.
- Cliente usa MatSelect por la cantidad actual; Producto sí usa autocomplete con
  precio e ID. El manejador recibe el ID y resuelve el objeto ya cargado, sin HTTP extra.
- Duplicados bloqueados por UX y validador; la guía no impone consolidación automática.
- Colección no vacía con validador explícito: Validators.minLength omite el array vacío.
- Confirmación Material, consistente con Fase 8, evitando confirm nativo del navegador.
- Consulta histórica separada en /:id; no se muestran todos los detalles en cada fila.
- Solo filtros estado/tipo; fechas y reportes ampliados quedan fuera de esta fase.
- Cancelación mediante PATCH /estado del backend existente, sin endpoint /anular,
  restauración de stock, cambios de estadoPago ni seguridad simulada.

## Git y cierre

Solo lectura de git status/diff, sin add/commit/push/pull/merge/rebase/reset.
Se detiene al terminar Fase 9, sin comenzar Keycloak, módulo Delivery o checkout público.

Mensaje de commit sugerido:

`feat: implementar pedidos Angular con formulario cabecera-detalle e histórico`
