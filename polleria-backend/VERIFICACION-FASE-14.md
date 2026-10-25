# Fase 14 — Abastecimiento: verificación técnica

Fecha: 25 de octubre de 2026, America/Lima.
Proyecto: `/home/systemseng/Polleria`. Backend y frontend conservan sus rutas.
Alcance exclusivo: Insumo, Proveedor, Compra/DetalleCompra e Inventario manual.

## Archivos backend

Creados bajo `src/main/java/pe/edu/upeu/polleria/abastecimiento/`:

- `package-info.java`: módulo independiente, dependencia solo de excepciones compartidas.
- `insumo/entity/Insumo.java`, `UnidadMedida.java`.
- `insumo/dto/InsumoRequest.java`, `InsumoResponse.java`.
- `insumo/mapper/InsumoMapper.java`, `repository/InsumoRepository.java`.
- `insumo/service/InsumoService.java`, `InsumoServiceImpl.java`.
- `insumo/controller/InsumoController.java`.
- `proveedor/entity/Proveedor.java`.
- `proveedor/dto/ProveedorRequest.java`, `ProveedorResponse.java`.
- `proveedor/mapper/ProveedorMapper.java`, `repository/ProveedorRepository.java`.
- `proveedor/service/ProveedorService.java`, `ProveedorServiceImpl.java`.
- `proveedor/controller/ProveedorController.java`.
- `compra/entity/Compra.java`, `DetalleCompra.java`.
- `compra/dto/CompraRequest.java`, `DetalleCompraRequest.java`, `CompraResponse.java`,
  `DetalleCompraResponse.java`, `CompraFiltro.java`.
- `compra/mapper/CompraMapper.java`, `repository/CompraRepository.java`.
- `compra/service/CompraService.java`, `CompraServiceImpl.java`.
- `compra/controller/CompraController.java`.
- `inventario/entity/MovimientoInventario.java`, `TipoMovimiento.java`.
- `inventario/dto/MovimientoResponse.java`, `MovimientoFiltro.java`,
  `SalidaRequest.java`, `AjusteRequest.java`.
- `inventario/mapper/MovimientoMapper.java`, `repository/MovimientoInventarioRepository.java`.
- `inventario/service/InventarioService.java`, `InventarioServiceImpl.java`.
- `inventario/controller/InventarioController.java`.

Otros creados: `scripts/06-abastecimiento.sql`,
`src/test/java/pe/edu/upeu/polleria/AbastecimientoIntegrationTests.java` y este informe.

Modificados: `SecurityConfig.java`, `ModularityTests.java` y README backend.
No se modificaron entidades, servicios ni repositorios de los cuatro módulos anteriores,
configuración Oracle/Keycloak/CORS, pom ni scripts SQL anteriores.

## Archivos frontend

Creados en `src/app/features/abastecimiento/`:

- `abastecimiento.model.ts`, `abastecimiento-nav.ts`, `abastecimiento.css`.
- `insumo-service.ts`, `proveedor-service.ts`, `compra-service.ts`, `inventario-service.ts`.
- `insumos.ts`, `insumos.html`, `proveedores.ts`, `proveedores.html`.
- `compras.ts`, `compras.html`, `inventario.ts`, `inventario.html`.
- `abastecimiento.spec.ts`.

Modificados: `app.routes.ts`, `core/layout/interno/interno.html` y README frontend.
Creados: verificación frontend, `http-verificacion-fase-14.json`,
`infra-verificacion-fase-14.json`, `npm-audit-fase-14.json`.
Sin cambios de versiones ni dependencias; se reutilizan autenticación, guards,
interceptor Bearer, X-Trace-ID y contrato global de errores.

## Modelos, precisión y decisiones

| Modelo | Campos |
|---|---|
| Insumo | id, nombre, descripcion, unidadMedida, stockActual, activo |
| Proveedor | id, razonSocial, ruc, telefono, correo, direccion, activo |
| Compra | id, proveedor, fecha del servidor, total, detalles |
| DetalleCompra | id, compra, insumoId, nombreInsumo histórico, unidadMedida histórica, cantidad, costoUnitario, subtotal |
| MovimientoInventario | id, insumo, tipo, cantidad/diferencia, stockAnterior, stockPosterior, fecha del servidor, motivo, compraId nullable |

Unidades enum: UNIDAD, KG, G, L, ML. Etiquetas UI: Unidad, kg, g, L, ml.
Tipos de movimiento exactamente ENTRADA, SALIDA, AJUSTE.

Stock/cantidades: BigDecimal y Oracle NUMBER(19,3): hasta 16 enteros y 3 decimales.
Costo unitario: BigDecimal y NUMBER(19,4): 15 enteros y 4 decimales, útil para compra
fraccionaria. Subtotal/total: BigDecimal y NUMBER(19,2), redondeo HALF_UP por línea;
el total suma esos subtotales monetarios. Oracle comprueba el mismo ROUND(...,2).
Se rechaza desbordamiento de stock/importes; no se cambia la escala para ocultarlo.

Stock inicial siempre 0.000. Los DTO de crear/editar Insumo no contienen stock y
MapStruct lo ignora al construir/actualizar entidades. El formulario tampoco lo edita.
Cambiar unidad solo es posible con stock cero y sin movimientos; toda Compra válida
genera movimientos en su misma transacción. Nombre/descripción/activo pueden editarse.

RUC opcional String normalizado con trim, 11 dígitos si existe y UNIQUE Oracle.
No se consulta SUNAT ni se afirma validez tributaria. Correo/teléfono/dirección son
opcionales. Correo se normaliza a minúsculas, espacios exteriores se retiran.
Teléfono reutiliza la convención de Clientes: 7–9 dígitos y +51 opcional.
Dos proveedores pueden compartir teléfono.

Se eligió desactivación mediante PUT activo=false para Insumos y Proveedores;
no se expone DELETE ni siquiera para filas sin historial. Compras no tienen PUT
ni DELETE. No hay cascadas de eliminación destructivas. Las FK protegen el historial.
No se creó tabla local de usuarios ni campos opcionales de auditoría sin uso en v1.

## Compra transaccional y stock

Request público:

```json
{
  "proveedorId": 44,
  "detalles": [
    {"insumoId": 84, "cantidad": "2.500", "costoUnitario": "12.3456"},
    {"insumoId": 85, "cantidad": "3.125", "costoUnitario": "4.2000"}
  ]
}
```

No envía nombre, unidad, subtotal, total ni fecha como autoridad. Angular conserva
cantidades/costos como texto decimal para no convertir el valor introducido a float
antes del envío. Jackson los convierte a BigDecimal; la API también acepta números
JSON. La estimación visual de Angular es informativa; la respuesta backend prevalece.

CompraServiceImpl valida proveedor activo y colección no vacía/sin Insumos repetidos.
Bloquea todos los Insumos en orden ascendente de ID, comprueba existencia/activo,
guarda fotografías históricas y calcula importes. Persiste Compra/DetalleCompra y
por cada línea reutiliza Inventario para generar ENTRADA con compraId y aumentar stock.
Todo comparte una transacción JPA/Oracle; no hay commits parciales entre recursos.

SALIDA recibe insumoId, cantidad positiva y motivo. Bloquea la fila del Insumo,
comprueba stock real y registra anterior/posterior. Si quedaría negativo devuelve
409 sin modificar stock ni registrar un movimiento exitoso.

AJUSTE recibe insumoId, nuevoStock físico no negativo y motivo. Calcula cantidad
como diferencia firmada: posterior − anterior. No se pide una diferencia al usuario.
Cero como conteo final está permitido. Un conteo idéntico al stock responde 409,
sin crear un movimiento sin cambio; esta decisión evita ruido en el historial.
No se admite ENTRADA manual: solo Compras la generan, evitando dos flujos de ingreso.

Todas las fechas las establece el servidor con America/Lima, como Pedidos/Delivery.
StockAnterior y StockPosterior nunca se aceptan desde el frontend.
Movimiento muestra ID/nombre actual del Insumo y la unidad, que ya no puede cambiar
tras historial; sus saldos quedan guardados. DetalleCompra conserva nombre/unidad
históricos aunque el Insumo se renombre.

## Concurrencia y rollback

PESSIMISTIC_WRITE sobre Insumo en operaciones de stock y edición de sus datos.
La edición no puede sobrescribir un stock concurrente; el mapper no lo modifica.
Las Compras bloquean IDs ordenados para evitar bloqueos cruzados entre dos compras.
No se introdujo infraestructura distribuida ni relaciones con otros módulos.

Pruebas obligatorias comprobadas en Oracle real:

- Compra con Insumo A válido y segundo ID inexistente: 404, sin Compra, detalles,
  movimientos ni aumento de A.
- Fallo tardío después de guardar y hacer flush de Compra, detalles y primera entrada:
  el segundo Insumo excedería precisión de stock. Se revierte toda la transacción;
  A conserva cero, B conserva su saldo previo y no quedan filas económicas parciales.
- Dos conexiones intentan SALIDA 8 desde stock 10 simultáneamente: solo una confirma,
  la otra recibe conflicto; stock final 2 y exactamente una SALIDA 10 → 2.

Los tests usan fixtures propios confirmados para leer el resultado de transacciones
realmente terminadas. La limpieza se limita a sus IDs y sigue el orden de FK.
No se deshabilitan restricciones ni se borran registros históricos ajenos.

## API y seguridad

Base: `/api/v1/abastecimiento`.

| Recurso | Operaciones |
|---|---|
| /insumos | GET colección, POST |
| /insumos/{id} | GET, PUT campos permitidos |
| /proveedores | GET colección, POST |
| /proveedores/{id} | GET, PUT campos permitidos |
| /compras | GET colección, POST transaccional |
| /compras/{id} | GET histórico |
| /movimientos | GET historial |
| /movimientos/salidas | POST {insumoId,cantidad,motivo} |
| /movimientos/ajustes | POST {insumoId,nuevoStock,motivo} |

Movimientos: filtros insumoId, tipo, fechaDesde, fechaHasta.
Compras: proveedorId, fechaDesde, fechaHasta. El día final es inclusivo mediante
límite exclusivo al inicio del siguiente día. Rango inverso/inválido se rechaza.
No se añadieron reportes ampliados ni consultas de Fase 15.

Todas las rutas requieren ADMINISTRADOR o ABASTECIMIENTO. Sin token 401;
CAJERO, REPARTIDOR, CLIENTE 403. Se conserva el contrato global seguro de errores:
400 validación/formato, 404 recurso inexistente, 409 duplicado, falta de stock,
inactivo, cambio de unidad con historial u otra integridad. Sin SQL/stack traces al cliente.

## Angular y pruebas reales

Rutas protegidas existentes más:
`/interno/abastecimiento/insumos`, `/proveedores`, `/compras`, `/inventario`, todas
bajo el mismo prefijo interno y con guards ADMINISTRADOR/ABASTECIMIENTO.
La navegación solo presenta Abastecimiento a esos dos roles.

Reactive Forms en Insumos/Proveedores; Compras con FormArray y validación de líneas
no vacías, positivas y sin repetir Insumo. Solo se seleccionan proveedores/insumos
activos. El formulario de Inventario admite salida o ajuste con motivo obligatorio.
La vista filtra por Insumo/Tipo; los servicios admiten también filtros de fechas.
Después de Compra se muestra total real y entradas generadas, y se recargan stock
y listado automáticamente. Un error conserva formulario para corregir/reintentar.

Flujo real completo con ABASTECIMIENTO desde Keycloak Code + PKCE y Angular:

1. Creó Aceite prueba F14, unidad L, stock 0.000; editó descripción. Creó Papas prueba
   F14, unidad KG y stock 0.000. IDs 84 y 85 respectivamente.
2. Creó Proveedor 44 con RUC de prueba de 11 dígitos y lo editó a
   “Proveedor F14 actualizado”. Intento duplicado dio 409 y mensaje comercial.
3. Registró Compra 7 con dos líneas:
   aceite 2.500 × 12.3456 = S/30.86; papas 3.125 × 4.2000 = S/13.13.
   Total confirmado S/43.99. Entradas automáticas 21/22 y stocks 2.500/3.125 visibles.
4. Registró SALIDA 0.625 L con motivo: movimiento 23, stock 2.500 → 1.875.
5. Intentó SALIDA 8.000: 409, stock permanece 1.875 y no se creó movimiento.
   Se repitió por HTTP verificando también el conteo de filas antes/después.
6. Registró AJUSTE a conteo físico 1.250 L: movimiento 24, diferencia −0.625,
   stock anterior 1.875 y posterior 1.250, motivo persistido.
7. Renombró temporalmente Aceite a “Aceite vegetal temporal F14”; abrió Compra 7
   y comprobó nombre histórico “Aceite prueba F14” y unidad L. Restauró el nombre.
8. ADMINISTRADOR inició sesión y comprobó navegación de los cinco módulos, acceso
   a Abastecimiento y continuidad de Delivery.
9. HTTP con tokens reales de los cinco roles: cuatro recursos permitidos con 200
   para ADMINISTRADOR/ABASTECIMIENTO; 403 para CAJERO/REPARTIDOR/CLIENTE.
   Petición sin token 401. La prueba HTTP de segunda línea inexistente confirmó
   que la única Compra exitosa seguía siendo 7 y el stock no varió.
10. Lecturas de regresión de Catálogo, Clientes, Pedidos y Delivery devolvieron 200
    con ADMINISTRADOR. Las 139 pruebas backend y 73 frontend previas permanecen.

## Oracle directo

Se inspeccionaron las cinco tablas nuevas, cinco PK y cinco FK exigidas:
DetalleCompra → Compra/Insumo; Compra → Proveedor; Movimiento → Insumo/Compra nullable.
UNIQUE de RUC y UNIQUE (COMPRA_ID,INSUMO_ID). Checks habilitados para stock >=0,
activo 0/1, enum, campos obligatorios, cantidades/costos positivos, importes no negativos,
subtotal calculado y ecuaciones de movimientos según tipo. Los checks permiten la
diferencia negativa de AJUSTE sin admitir saldo negativo.
Índices explícitos solo movimientos(insumo,fecha) y compras(proveedor,fecha), además
de los de PK/UNIQUE necesarios. Se comprobaron columnas y orden directamente.

Consulta SQL confirmó:

| Movimiento | Insumo | Tipo | Cantidad/diferencia | Anterior | Posterior | Compra |
|---|---|---|---:|---:|---:|---|
| 21 | 84 | ENTRADA | 2.500 | 0.000 | 2.500 | 7 |
| 22 | 85 | ENTRADA | 3.125 | 0.000 | 3.125 | 7 |
| 23 | 84 | SALIDA | 0.625 | 2.500 | 1.875 | — |
| 24 | 84 | AJUSTE | −0.625 | 1.875 | 1.250 | — |

Compra 7, proveedor 44: total 43.99; detalles 30.86/13.13. Stocks finales 1.250 L
y 3.125 kg. Nombre de Aceite restaurado y activo. No hubo edición de Producto.

## Resultados finales

| Comprobación | Resultado |
|---|---|
| `./mvnw clean compile` | BUILD SUCCESS, Java 21 |
| `./mvnw test` | 180 correctas: 139 anteriores + 41 nuevas; 0 fallos/errores/omitidas |
| Spring Modulith | 3 pruebas correctas, exactamente cinco módulos comerciales, sin ciclos |
| Arranque real | Started PolleriaBackendApplication; Oracle validate correcto |
| Oracle | cinco tablas nuevas, restricciones/índices y saldos reales comprobados |
| Keycloak | cliente público, S256 obligatorio, password grant SPA desactivado; cinco roles sin cambios |
| Actuator | HTTP 200, UP, sin ampliar exposición |
| OpenAPI | HTTP 200; nueve paths, Bearer presente; sin operaciones destructivas inexistentes |
| CORS | preflight POST Compras 200, localhost:4200 permitido |
| `npm run build` | correcto, sin warning de presupuesto |
| Pruebas frontend | 89 correctas: 73 anteriores + 16 nuevas; siete archivos |
| Responsive | cuatro pantallas a 390/768/1440 sin overflow; compra de dos líneas e inventario revisados |
| Consola | sin warnings/errores relevantes, tokens impresos, loops 401 ni fallos CORS |
| npm audit | 7 pendientes: 5 altas y 2 críticas; sin force ni cambios de dependencias |

Responsive: scrollWidth 375/753/1425 frente a innerWidth 390/768/1440 en las
pantallas comprobadas. Botones mínimo 44 px; campos adaptables y formularios etiquetados.
El viewport temporal se restauró. No se creó app móvil ni diseño final de Fase 16.

Las 41 pruebas nuevas backend incluyen los casos requeridos de Insumo/Proveedor,
compra múltiple, importes, autoridad del servidor, inexistencias/inactivos,
validación, dos rollbacks, histórico, salidas/ajustes, filtros, protección de historial,
roles y concurrencia real. Las 16 frontend cubren los cuatro servicios, formularios,
FormArray, contratos sin totales/stock, actualización tras éxito, errores y navegación.

## Datos, límites y pendientes

Se conservan Insumos 84/85, Proveedor 44, Compra 7 con dos detalles y Movimientos
21–24 porque explican el historial económico y de stock probado. No se borraron
Compras/Movimientos ni se manipularon FK para limpiar. Los fixtures de las suites
se eliminaron únicamente por sus IDs propios. No se tocó historial anterior.

Decisiones dentro del alcance: desactivar en vez de eliminar; ajuste de conteo
idéntico rechazado sin movimiento; costo unitario de cuatro decimales y redondeo
monetario por línea; decimales de entrada Angular conservados como texto.
Sin desviaciones de arquitectura: quinto módulo independiente y sin vínculo a Producto.

Pendientes conocidos: npm audit (5 altas/2 críticas), política npm allowScripts,
fetch size Oracle, agente dinámico Mockito/ByteBuddy y avisos Springdoc de desarrollo.
No se hizo limpieza general de warnings. Errores de integridad provocados en tests
son esperados y la suite final tiene cero errores.

Confirmación expresa: NO se implementaron recetas, consumo automático por Pedido,
relación Producto-Insumo, stock en Producto, costos de platos, cuentas por pagar,
SUNAT, almacenes/sucursales múltiples, lotes/vencimientos complejos, escáner,
IA, alertas automáticas, reportes ampliados, diseño final ni Fase 15.
Git se consultó solo en lectura: sin add/commit/push/pull/merge/rebase/reset.

Commit sugerido: `feat: implementar Abastecimiento con compras e inventario transaccional`
