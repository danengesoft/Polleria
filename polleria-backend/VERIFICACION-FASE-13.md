# Fase 13 — Delivery: verificación técnica

Fecha: 25 de octubre de 2026, America/Lima. Alcance exclusivo Delivery y su
integración mínima con Pedidos, Clientes y Angular. Sin Fase 14.

## Archivos backend

Creados bajo `src/main/java/pe/edu/upeu/polleria/`:

- `delivery/package-info.java`.
- `delivery/repartidor/entity/Repartidor.java`.
- `delivery/repartidor/dto/RepartidorResponse.java`.
- `delivery/repartidor/repository/RepartidorRepository.java`.
- `delivery/repartidor/service/RepartidorService.java`, `RepartidorServiceImpl.java`.
- `delivery/repartidor/controller/RepartidorController.java`.
- `delivery/entrega/entity/Entrega.java`, `EstadoEntrega.java`.
- `delivery/entrega/dto/AsignacionRequest.java`, `ReasignacionRequest.java`,
  `IncidenciaRequest.java`, `EntregaResponse.java`.
- `delivery/entrega/repository/EntregaRepository.java`.
- `delivery/entrega/service/EntregaService.java`, `EntregaServiceImpl.java`.
- `delivery/entrega/controller/EntregaController.java`.
- `pedidos/pedido/api/package-info.java`, `PedidoDeliveryView.java`,
  `PedidoDeliveryService.java`.
- `clientes/cliente/api/ClienteContacto.java`.

Otros creados: `scripts/05-delivery.sql`,
`src/test/java/pe/edu/upeu/polleria/DeliveryIntegrationTests.java` y este informe.

Modificados:

- `SecurityConfig.java`.
- `clientes/cliente/api/ClienteConsultaService.java` y
  `clientes/cliente/service/ClienteServiceImpl.java`.
- `pedidos/pedido/service/PedidoService.java`, `PedidoServiceImpl.java` y
  `pedidos/pedido/controller/PedidoController.java`.
- `src/test/java/pe/edu/upeu/polleria/ModularityTests.java`: conserva las tres
  pruebas y actualiza la expectativa de módulos comerciales a cuatro.
- README backend.

## Archivos frontend

Creados en `src/app/features/delivery/`:

- `delivery.model.ts`, `delivery-service.ts`.
- `delivery-list.ts`, `delivery-list.html`.
- `mis-entregas.ts`, `mis-entregas.html`.
- `confirmar-entrega.ts`, `delivery.css`, `delivery.spec.ts`.

Modificados: `app.routes.ts`, `core/layout/interno/interno.html`,
`features/pedidos/pedido-service.ts`, `pedido-detalle.ts`, `pedido-detalle.html`,
`features/publico/mi-pedido.ts` y README frontend.
Informes creados: `http-preparacion-fase-13.json`, `http-verificacion-fase-13.json`,
`infra-verificacion-fase-13.json`, `npm-audit-fase-13.json` y verificación frontend.
No se instalaron paquetes ni se cambiaron versiones.

## Modelos y Oracle

Repartidor: id, keycloakSub, nombreVisible, activo. Identidad obtenida únicamente
del JWT validado; nombre de `name` y, como alternativa, `preferred_username`.
El perfil no autentica, no almacena contraseña/username de autenticación ni roles.
Sus respuestas solo exponen id/nombreVisible/activo.

Entrega: id, pedidoId, repartidor (FK por repartidorId), fechaAsignacion,
fechaSalida, fechaEntrega, estado enum y observacion. Solo una fila por Pedido.
No se crean métodos de eliminación de Entrega ni de repartidores con historial.

`05-delivery.sql` se aplicó una sola vez al contenedor `polleria-oracle`, esquema
POLLERIA en FREEPDB1. `ddl-auto: validate` se mantiene. No se modificaron scripts
anteriores ni tablas de otros proyectos. Ahora hay siete tablas comerciales.

| Tabla | Restricciones comprobadas directamente en Oracle |
|---|---|
| REPARTIDORES | PK_REPARTIDORES, KEYCLOAK_SUB NOT NULL y UK_REPARTIDORES_SUB, nombre y activo NOT NULL, checks activo 0/1 y textos no blancos |
| ENTREGAS | PK_ENTREGAS, PEDIDO_ID NOT NULL y UK_ENTREGAS_PEDIDO, FK_ENTREGAS_PEDIDO → PK_PEDIDOS, FK_ENTREGAS_REPARTIDOR → PK_REPARTIDORES, estado/fechaAsignacion NOT NULL |

Checks de Entrega: enum permitido, fechas coherentes con el estado, orden cronológico
asignación/salida/entrega y observación no vacía en NO_ENTREGADO. Índice por
REPARTIDOR_ID. Todas las restricciones inspeccionadas están ENABLED.
La suite además provoca duplicados reales de sub y pedido y comprueba rechazo Oracle.

## Contratos y transacciones

Cuatro módulos comerciales: Catálogo, Clientes, Pedidos y Delivery.
Delivery solo depende de `pedidos::pedido-delivery` y excepciones compartidas.
Pedidos no depende de Delivery. No se importa PedidoRepository ni Pedido entity
fuera de su módulo. El contrato público específico de Pedidos es:

- `consultar(id)`: proyección mínima PedidoDeliveryView.
- `validarAsignacion(id)`: bloquea y exige DELIVERY/LISTO.
- `validarReasignacion(id)`: bloquea y exige DELIVERY/DESPACHADO.
- `despachar(id)`: exige DELIVERY y LISTO o DESPACHADO para reintento.
- `entregar(id)`: exige DELIVERY/DESPACHADO y aplica la regla de efectivo.

La vista contiene estadoPedido, nombreCliente, teléfono, domicilio/referencia
históricos, total, metodoPago y estadoPago. No correo, sub ni historial del Cliente.
Pedidos obtiene nombre/teléfono por `ClienteContacto` mediante la interfaz pública
existente `clientes::cliente-consulta`; Delivery no importa ClienteRepository/entity.

Asignación bloquea Pedido antes de comprobar existencia y persistir la entrega.
UNIQUE de PEDIDO_ID es la segunda garantía. Las mutaciones de una Entrega bloquean
su fila con PESSIMISTIC_WRITE, comprueban propiedad y estado y usan la misma
transacción JPA/Oracle para actualizar Pedido mediante su contrato público.
El propio módulo Pedidos bloquea la cabecera al cambiarla.

Se comprueba rollback real: las pruebas guardan y hacen flush del estado de Entrega,
provocan una falla comercial al actualizar Pedido y leen Oracle después de terminar
la transacción fallida. Se conservan ASIGNADO sin fechaSalida y EN_CAMINO sin
fechaEntrega respectivamente. También hay pruebas con dos conexiones concurrentes:
solo una asignación, un inicio y una entrega tienen éxito.

## Flujo y pagos

Estados enum: ASIGNADO → EN_CAMINO → ENTREGADO, o EN_CAMINO → NO_ENTREGADO.
ASIGNADO exige Pedido DELIVERY/LISTO y repartidor existente activo. Fechas siempre
del servidor en America/Lima. PRESENCIAL, RECOJO, no LISTO, perfil inactivo o
inexistente y una segunda fila para el mismo Pedido se rechazan.

Iniciar cambia Entrega a EN_CAMINO y Pedido a DESPACHADO atómicamente.
Entregar cambia ambos a ENTREGADO con fechaEntrega. Solo EFECTIVO PENDIENTE pasa
a PAGADO automáticamente en esa misma operación. YAPE/PLIN/TARJETA no cambian
por acción del repartidor, aunque el Pedido ya quede ENTREGADO.

NO_ENTREGADO exige observación de 1 a 500 caracteres no blancos. Pedido permanece
DESPACHADO y pago sin cambios; nunca se cancela automáticamente.
Reasignar solo desde NO_ENTREGADO, a perfil activo, en la misma fila: ASIGNADO,
nueva fechaAsignacion, fechaSalida/fechaEntrega NULL. Se conserva la observación
previa; nuevas incidencias se agregan con fecha y salto de línea, máximo acumulado
2000 caracteres. Al alcanzar el límite se responde 409, sin truncar incidencias.
No se crea historial de intentos; una futura versión podrá separarlo en otra tabla.
Al iniciar el segundo intento se acepta Pedido ya DESPACHADO, sin retroceder a LISTO.

Operación mínima adicional de Pedidos: PATCH `/api/v1/pedidos/{id}/confirmar-pago`,
solo ADMINISTRADOR/CAJERO. Confirma PENDIENTE → PAGADO para YAPE/PLIN/TARJETA
de un Pedido no cancelado. No revierte pagos ni acepta un estado enviado por el
cliente. UI con confirmación explícita; no integra pasarela ni realiza cobro bancario.

## Endpoints y permisos

Bases: `/api/v1/delivery/repartidores` y `/api/v1/delivery/entregas`.

| Método y recurso | Permiso |
|---|---|
| GET repartidores | ADMINISTRADOR/CAJERO, solo perfiles activos |
| GET y POST repartidores/me | REPARTIDOR, sub/nombre desde JWT; POST duplicado 409 |
| GET entregas y entregas/{id} | ADMINISTRADOR/CAJERO |
| POST entregas | ADMINISTRADOR/CAJERO; body {pedidoId,repartidorId} |
| PATCH entregas/{id}/reasignar | ADMINISTRADOR/CAJERO; body {repartidorId} |
| GET entregas/mias y entregas/mias/{id} | REPARTIDOR por sub actual |
| PATCH entregas/mias/{id}/iniciar | REPARTIDOR propietario; sin body de estado |
| PATCH entregas/mias/{id}/entregar | REPARTIDOR propietario; sin estadoPago |
| PATCH entregas/mias/{id}/no-entregado | REPARTIDOR propietario; body {observacion} |

Identidad de reparto nunca se elige desde Angular. Recursos ajenos dan 404 en
consulta/iniciar/entregar/no-entregado. Sin token 401; CLIENTE y ABASTECIMIENTO 403.
Repartidor tampoco puede consultar el listado global ni confirmar pago digital.
Los endpoints internos previos y Mis pedidos mantienen sus permisos.

## Angular

`/interno/delivery` exige ADMINISTRADOR/CAJERO: entregas, asignación, repartidores
activos y reasignación solo tras NO_ENTREGADO. Reutiliza el filtro de Pedidos
DELIVERY/LISTO y descarta IDs con entrega existente. No duplica consultas backend.

`/interno/reparto` exige REPARTIDOR: activar perfil si GET /me responde 404,
lista únicamente propia, domicilio histórico y datos mínimos de contacto/pago.
ASIGNADO ofrece Iniciar; EN_CAMINO ofrece Entregado/No entregado, con diálogo de
confirmación. Observación vacía bloquea el envío. ENTREGADO/NO_ENTREGADO no ofrecen
acciones operativas hasta la reasignación correspondiente.
No hay enlaces a administración comercial global para REPARTIDOR.
Se reutilizan AuthService, guards e interceptor existentes. Sin nuevas librerías.
El Cliente no consume Delivery: Mis pedidos refleja los estados de Pedido al consultar.

## Pruebas reales realizadas

1. REPARTIDOR inició sesión por Keycloak Code+PKCE, abrió Mis entregas y activó su
   perfil desde SPA sin introducir ID/sub/rol. Perfil operativo 60.
2. Se preparó otra identidad de prueba REPARTIDOR y su perfil 61. La preparación
   de la cuenta usa únicamente herramientas locales de prueba; el backend/Angular
   no dependen de Keycloak Admin API ni tienen client secret administrativo.
3. Se crearon Pedidos propios nuevos 1737 EFECTIVO y 1738 YAPE, S/20 cada uno,
   para Cliente 676. Se avanzaron legítimamente por CONFIRMADO, EN_PREPARACION y LISTO.
   No se alteraron los Pedidos históricos anteriores, incluido 1107.
4. ADMINISTRADOR asignó ambos desde SPA a perfil 60: Entregas 21 y 22 ASIGNADO.
5. A inició 21 desde SPA: EN_CAMINO/DESPACHADO/fechaSalida comprobados por HTTP y
   consulta Oracle. CLIENTE vio DESPACHADO en Mis pedidos, sin acceder a Delivery.
6. B intentó GET/iniciar/entregar/no-entregado de 21: los cuatro respondieron 404.
   Matriz real con tokens Code+PKCE: ADMINISTRADOR/CAJERO GET global 200;
   REPARTIDOR/CLIENTE/ABASTECIMIENTO 403; petición sin token 401.
7. A inició 22. Diálogo de No entregado rechazó observación vacía y aceptó
   “Cliente ausente en el primer intento de prueba F13”. Entrega NO_ENTREGADO,
   Pedido DESPACHADO, pago PENDIENTE verificados por HTTP. No hubo cancelación.
8. CAJERO reasignó 22 a perfil 61 desde SPA. Mismo ID, nueva fechaAsignacion,
   fechas de salida/entrega NULL, observación conservada y Pedido DESPACHADO.
   A dejó de verla; B solo vio su entrega reasignada.
9. A confirmó Entregado en 21: Entrega y Pedido ENTREGADO, efectivo PAGADO.
10. B inició y entregó el segundo intento de 22 desde SPA: Entrega/Pedido
    ENTREGADO y YAPE permaneció PENDIENTE. REPARTIDOR no pudo confirmar pago (403).
11. CAJERO confirmó el pago YAPE desde detalle interno con diálogo explícito:
    pasó a PAGADO. Se conserva evidencia del PENDIENTE tras la entrega en JSON.
12. Después de entregar se cambió temporalmente la dirección principal del Cliente
    por su endpoint /me; ambas Entregas conservaron las direcciones históricas.
    El perfil original quedó restaurado dentro de finally.
13. CLIENTE volvió a Mis pedidos y vio 1737/1738 ENTREGADO y PAGADO, conservando
    los demás pedidos de fases anteriores. No hubo polling ni acceso público a Delivery.

Responsive de reparto comprobado a 390/768/1440 px: scrollWidth 375/753/1425 frente
a innerWidth 390/768/1440, sin overflow horizontal. Revisión visual móvil con botones
mínimo 44 px, cards y diálogo adaptables. Consola del último flujo completo: []
para warnings/errores; sin tokens impresos, loops 401 ni errores CORS.
La herramienta de navegador perdió sincronización durante la limpieza final de
la pestaña, después de terminar todas las verificaciones. No es un error de la SPA.
El override de viewport se restauró mediante la capacidad documentada del navegador.

## Resultados finales

| Comprobación | Resultado |
|---|---|
| `./mvnw clean compile` | BUILD SUCCESS, Java 21 |
| `./mvnw test` | 139 correctas: 110 anteriores + 29 nuevas; 0 fallos/errores/omitidas |
| Spring Modulith | 3 pruebas correctas; cuatro módulos comerciales sin ciclos |
| Arranque real final | Started PolleriaBackendApplication; puerto 8080, validate correcto |
| Oracle | REPARTIDORES/ENTREGAS y constraints reales; dos perfiles y dos entregas persistentes |
| Keycloak | cliente público, PKCE S256, password grant desactivado; roles sin cambios |
| Actuator | HTTP 200, UP, exposición sin ampliar |
| OpenAPI | HTTP 200, diez paths Delivery más confirmar-pago; esquema Bearer existente |
| CORS | preflight PATCH de entregar 200, localhost:4200 permitido |
| `npm run build` | correcto, sin warning de presupuesto |
| Pruebas frontend | 73 correctas: 60 anteriores + 13 nuevas; seis archivos |
| npm audit | 7 pendientes: 5 altas y 2 críticas; sin force ni cambios de dependencias |

Nuevas pruebas backend: identidad de perfil, duplicado/unique, activos, asignaciones,
fechas, tipos inválidos, inexistencias/inactivo, una fila, propiedad en cuatro
operaciones, flujo efectivo, YAPE/PLIN/TARJETA, incidencia, segundo intento,
transiciones inválidas, dos rollbacks reales, domicilio histórico, pago interno,
seguridad y tres casos concurrentes. Las de Delivery usan fixtures identificados
por IDs propios y los eliminan en orden FK; las 110 anteriores permanecen.

Nuevas pruebas frontend: contratos HTTP, listado/filtros, asignación, perfil,
lista propia, acciones por estado, confirmaciones, observación obligatoria,
visibilidad/guards y confirmación explícita de pago digital.

## Datos, límites y pendientes

Permanecen perfiles 60/61 y Entregas 21/22 vinculadas a Pedidos 1737/1738. Ambos
Pedidos y pagos están terminados; se conservan por historial/FK. La cuenta local
repartidor_b se conserva para reproducir propiedad/reasignación. Credenciales solo
en `.env` ignorado con permiso 0600; no se incluyen en código ni informes.
Producto 10 sigue disponible a S/20; perfil Cliente 676 restaurado. No se rompieron
FK ni se tocaron mi_oracle u otros proyectos/contenedores/volúmenes.

Se eligió `/interno/reparto` por coherencia con el portal y guards existentes.
Una sola Entrega por Pedido; observaciones acumuladas sustituyen únicamente en v1
un historial separado de intentos. Son decisiones previstas, sin desviación de alcance.
El único endpoint comercial adicional fuera de Delivery es confirmar-pago, mínimo
expresamente autorizado, sin construir un módulo de pagos.

Persisten warnings anteriores no bloqueantes: fetch size Oracle, agente dinámico
Mockito/ByteBuddy y advertencias Springdoc de desarrollo; npm audit y política npm
allowScripts siguen pendientes. Errores Oracle provocados por tests de constraints
son esperados y no fallos de suite. No se hizo limpieza general de infraestructura.

Sin Abastecimiento, GPS, mapas, rutas, geolocalización, push, WebSockets/SSE,
polling frecuente, pagos bancarios, Nginx ni despliegue. No se comenzó Fase 14.
Git se utilizó solo en lectura, sin add/commit/push/pull/merge/rebase/reset.

Commit sugerido: `feat: implementar Delivery con asignación, reparto seguro y sincronización de pedidos`
