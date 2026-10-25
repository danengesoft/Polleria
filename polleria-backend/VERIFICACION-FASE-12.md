# Fase 12 — Experiencia pública del cliente

Verificación local del 24–25 de octubre de 2026, America/Lima.
Backend: `/home/systemseng/Polleria/polleria-backend`.
Frontend: `/home/systemseng/Polleria/polleria-frontend`.

## Archivos backend

Creados:
- `scripts/04-cliente-keycloak.sql`.
- `clientes/cliente/dto/PerfilRequest.java` y `controller/MiPerfilController.java`.
- `pedidos/pedido/dto/MiPedidoRequest.java` y `controller/MisPedidosController.java`.
- `src/test/java/pe/edu/upeu/polleria/AutoservicioIntegrationTests.java`.
- Este informe.

Modificados bajo `src/main/java/pe/edu/upeu/polleria`:
- `SecurityConfig.java`: matchers de autoservicio antes de reglas internas.
- `clientes/cliente/entity/Cliente.java`: vínculo nullable único.
- `clientes/cliente/repository/ClienteRepository.java`: búsqueda por sub.
- `clientes/cliente/mapper/ClienteMapper.java`: ignorar vínculo en requests internos;
  actualizar campos comerciales no puede sobrescribirlo.
- `clientes/cliente/service/ClienteService.java` y `ClienteServiceImpl.java`.
- `clientes/cliente/api/ClienteConsultaService.java`: contrato mínimo por sub.
- `pedidos/pedido/service/PedidoService.java` y `PedidoServiceImpl.java`.
- README backend.

El diff de `keycloak/polleria-realm.json` ya existía al comenzar Fase 12, procedente
de Fase 11. Esta fase no cambió el realm, los roles ni los permisos del cliente OAuth.
Solo agregó una segunda identidad CLIENTE de prueba para verificar propiedad.
Credenciales locales exclusivamente en `.env` ignorado; no se incluyen aquí.

## Identidad, perfil y seguridad

`Cliente.keycloakSub` corresponde a `CLIENTES.KEYCLOAK_SUB VARCHAR2(255 CHAR)`.
Es nullable, longitud 255, constraint `UK_CLIENTES_KEYCLOAK_SUB` UNIQUE habilitada.
Oracle permite varios NULL; rechaza duplicados no nulos. La suite comprueba la
violación real, sin H2. El Cliente 172 existente conserva NULL y sus datos previos.
La migración no creó tablas ni alteró scripts anteriores; `ddl-auto` sigue validate.

GET/POST/PUT `/api/v1/clientes/me` requieren CLIENTE. El Controller obtiene
`Jwt.getSubject()` mediante `@AuthenticationPrincipal`, después de validación JWT
por Spring Resource Server. El Service resuelve el Cliente exclusivamente por sub.
No se usan email ni username como vínculo, ni se hace auto-link.
GET sin perfil responde 404; POST duplicado responde 409. Correo en uso responde
409 aunque pertenezca a un Cliente sin vincular. PUT preserva el vínculo y activo.
No existe parámetro de selección de Cliente para `/me`.
El DTO público solo admite campos comerciales; campos extra no otorgan autoridad.
Respuestas no exponen keycloakSub ni entidades JPA.

GET/POST `/api/v1/pedidos/mios`, GET `/api/v1/pedidos/mios/{id}` y PATCH
`/api/v1/pedidos/mios/{id}/cancelar` requieren CLIENTE. Los endpoints globales
siguen restringidos a ADMINISTRADOR/CAJERO; reportes a ADMINISTRADOR.
La creación propia transforma el request después de resolver Cliente por sub y
reutiliza `crear(PedidoRequest)`, con las mismas validaciones, cálculos y transacción.
Consulta individual y cancelación verifican clienteId persistido contra el Cliente
del sub actual. Recurso ajeno e inexistente reciben el mismo 404 sin revelar datos.
Cancelación bloquea la cabecera, exige PENDIENTE y aplica la regla existente de no
cancelar PAGADO. No se agregaron reglas ni entidades de Delivery.

Request público exacto:

```json
{
  "tipoPedido": "RECOJO",
  "metodoPago": "EFECTIVO",
  "direccionEntrega": null,
  "referenciaEntrega": null,
  "detalles": [{"productoId": 10, "cantidad": 1}]
}
```

No define clienteId, precio, subtotal, total, fecha, estado ni estadoPago.
La prueba envía además valores falsos de identidad/total/estado y comprueba que
no tienen autoridad: el backend persiste Cliente del JWT, S/20 y PENDIENTE.
El servidor restringe el flujo público a RECOJO/DELIVERY; PRESENCIAL permanece interno.
Métodos EFECTIVO/YAPE/PLIN/TARJETA registran elección, sin integrar pasarelas.

## Frontend y contratos

Creados en `src/app/features/publico/`:
- `catalogo-publico.ts`.
- `carrito-service.ts`, `carrito.ts`.
- `cliente-publico-service.ts`.
- `perfil.ts`, `perfil.html`.
- `checkout.ts`, `checkout.html`.
- `mis-pedidos.ts`, `mi-pedido.ts` (incluye diálogo de cancelación).
- `publico.css`, `publico.spec.ts`.

Modificados: `app.routes.ts`, `core/inicio/inicio-publico.ts`,
`shared/navigation/app-navigation.ts` y README frontend.
Informes creados: `http-verificacion-fase-12.json`, `infra-verificacion-fase-12.json`,
`keycloak-verificacion-fase-12.json`, `npm-audit-fase-12.json` y verificación frontend.
Sin dependencias, versiones, AuthService, guards ni interceptores adicionales.

`/catalogo`: lectura anónima con endpoints existentes y filtro por categoría.
No disponibles se muestran explícitamente sin botón de agregar. Precio en soles.
Se usa imagenUrl HTTP/HTTPS si existe; en ausencia, placeholder propio sin imágenes
aleatorias. No hay upload. Carrito agrupa por productoId, modifica/elimina/vacía,
calcula importes estimados y admite cantidades enteras 1–999.
Se guarda en sessionStorage `polleria_carrito`, separado de autenticación.
Solo guarda id, nombre, precio informativo y cantidad; no tokens ni perfil.

`/perfil`, `/checkout`, `/mis-pedidos` y `/mis-pedidos/:id` usan autenticación y
rol CLIENTE mediante los guards existentes. Usuarios internos ven el área pública
pero no pueden comprar como un Cliente. Si falta perfil, checkout dirige a completarlo
y regresa al guardar; carrito permanece. El formulario no muestra keycloakSub ni roles.
Checkout precarga dirección, permite modificarla para ese Pedido y no altera perfil.
RECOJO no exige domicilio; DELIVERY sí. El POST se construye campo por campo sin
clienteId/precio/total. Solo al recibir éxito se vacía el carrito y se abre el detalle.
Si hay error se conserva. La pantalla usa total y precios históricos del servidor.
Mis pedidos muestra exclusivamente los del sub; detalle usa el PedidoResponse.
Cancelar exige diálogo de confirmación y se ofrece solo para PENDIENTE no pagado.

## Pruebas reales

1. Anónimo: Inicio → Catálogo → Pollos → 1/4 Pollo S/20; producto F9 no disponible.
   Agregar, aumentar a 2, recargar (S/40 conservado), disminuir a 1 y checkout.
2. Login CLIENTE por Keycloak Code+PKCE; regresó al flujo con carrito intacto.
   Sin perfil, abrió formulario; creó Cliente 676 y volvió a checkout.
3. Perfil 676 actualizado a teléfono de prueba y dirección principal nueva;
   lectura `/me` y consulta directa Oracle confirmaron persistencia.
   Correo de otro Cliente provocó 409; SPA mostró el mensaje comercial.
4. RECOJO 1106 creado desde SPA: total confirmado S/20, pago PENDIENTE,
   estado PENDIENTE, carrito vacío después del éxito. Cancelación con confirmación
   desde detalle produjo CANCELADO; una segunda cancelación real respondió 409.
5. DELIVERY 1107: dirección vacía mostró error explícito y no creó Pedido.
   Con domicilio efectivo y YAPE creó Pedido por S/20, PENDIENTE y pago PENDIENTE.
   Conserva domicilio/referencia históricos distintos del perfil actualizado.
6. Cliente B 677, segunda identidad Keycloak CLIENTE real: GET y PATCH al Pedido
   1107 de A respondieron 404; listado propio vacío. GET al perfil global ajeno 403.
   También en navegador B vio lista vacía y “Pedido no encontrado”, sin detalles ajenos.
7. Precio de Producto 10 cambiado temporalmente a S/25 por API autorizada;
   GET propio 1107 conservó precioUnitario y total S/20. Restaurado a S/20 en finally.
8. Producto 10 deshabilitado temporalmente: POST propio dio 409. Restablecido a
   disponible=true en finally; consulta Oracle final confirmó precio 20 y disponible=1.
9. Regresión HTTP Code+PKCE real para los cinco roles: ADMINISTRADOR y CAJERO
   conservan Clientes/Pedidos; solo ADMINISTRADOR accede a reportes; los demás 403.
   Catálogo responde 200 para todos. Cada respuesta conserva X-Trace-ID.
10. Navegador ADMINISTRADOR: portal y listados Categorías/Productos/Clientes/Pedidos
    funcionaron. `/checkout` para ADMINISTRADOR mostró acceso denegado.
    Login/logout de CLIENTE, ADMINISTRADOR y CLIENTE B comprobados; fin anónimo.

La prueba CONFIRMADO → cancelación propia 409 se cubre en integración automatizada
sobre Oracle con rollback. En HTTP real se usó CANCELADO → cancelación 409 para
no avanzar innecesariamente el pedido de prueba DELIVERY. No se falsearon estados.
Los historiales de pedidos se conservan, sin romper FK para limpiar. Solo se añadieron
los Clientes 676/677 y Pedidos 1106/1107 necesarios; el carrito de revisión quedó vacío.
Identidad B de prueba se conserva para reproducir el aislamiento; no se publican credenciales.

## Resultados finales

| Verificación | Resultado |
|---|---|
| Java | 21.0.12.1 |
| `./mvnw clean compile` | BUILD SUCCESS |
| `./mvnw test` | 110 correctas: 93 anteriores + 17 nuevas, 0 fallos/errores/omitidas |
| Spring Modulith | 3 pruebas correctas; solo Catálogo, Clientes y Pedidos como módulos comerciales |
| Arranque limpio final | Started PolleriaBackendApplication; puerto 8080, validate correcto |
| Oracle real | columna, UNIQUE habilitada, NULL histórico, asociación y precios comprobados |
| Actuator | HTTP 200, UP |
| OpenAPI | HTTP 200; las cuatro rutas de autoservicio documentadas, 17 paths totales |
| Keycloak | cliente público, S256 obligatorio, password grant desactivado, cinco roles conservados |
| CORS | preflight POST /pedidos/mios 200, origen localhost:4200 permitido |
| Angular | servidor real localhost:4200 operativo |
| `npm run build` | correcto, sin warning de presupuesto |
| Pruebas frontend | 60 correctas: 48 anteriores + 12 nuevas, 5 archivos |
| Responsive | catálogo, carrito, checkout, mis pedidos y perfil a 390/768/1440, sin overflow horizontal |
| Consola final | sin warnings/errores relevantes, sin tokens, bucles OAuth ni errores CORS |
| npm audit | 7 pendientes: 5 altas y 2 críticas; sin force ni cambios de dependencias |

Las pruebas nuevas backend cubren perfil por sub, actualización, inexistencia,
prohibición de perfil global, unicidad Oracle, correo sin auto-link, validación,
creación propia/autoridad del servidor, listado/detalle propios, aislamiento entre
usuarios, cancelación propia/ajena/fuera de PENDIENTE, domicilio y disponibilidad.
Las frontend cubren carrito/persistencia, guards, perfil requerido, contratos,
conservación ante 409, limpieza después de éxito, perfil, listado y cancelación.

Registro público de cuentas Keycloak sigue deshabilitado: pendiente explícito,
sin formulario Angular de contraseña ni Admin API en el frontend. Se verificó con
usuarios CLIENTE reales. Pendientes no bloqueantes anteriores: fetch size Oracle,
agente dinámico de Mockito/ByteBuddy, advertencias de Springdoc en desarrollo y
política npm allowScripts; sin limpieza de infraestructura ni audit fix --force.
Las advertencias Oracle esperadas en pruebas de integridad no son fallos de suite.

Se conserva la limitación de la herramienta de navegador sobre inspección directa
de Network; el Bearer se verifica con pruebas del interceptor y API real protegida.
No hubo cambios de arquitectura: autoservicio pertenece a Clientes/Pedidos, carrito
es estado SPA. No se agregó módulo PUBLICO/CHECKOUT/CARRITO backend, Delivery,
Abastecimiento, mapas, GPS, pagos reales, Nginx ni despliegue.

Git se consultó solo en lectura. Sin add/commit/push/pull/merge/rebase/reset.

Commit sugerido: `feat: implementar experiencia pública del cliente con perfil, carrito y pedidos propios`
