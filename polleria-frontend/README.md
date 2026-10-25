# POLLERIA — SPA, Fase 14

Angular standalone con Catálogo, Clientes y Pedidos internos. Autenticación real
Keycloak Authorization Code + PKCE S256, sin librería adicional de autenticación.
El área pública `/` permanece anónima. Spring Boot conserva la autoridad de permisos.

## Ejecución local

Node 24.18.0, npm 11.16.0, Angular 22.1.6, CLI/build 22.1.8,
Material/CDK 22.2.0 y TypeScript 6.0.3. Dependencias sin cambios en esta fase.

```bash
npm ci
npm start -- --host localhost --port 4200
npm run build
npm test -- --watch=false
```

Ejecutar desde `/home/systemseng/Polleria/polleria-frontend`, con backend en
`http://localhost:8080` y Keycloak en `http://localhost:38080`.
`src/environments/environment.ts` centraliza API e issuer del realm `polleria`,
cliente público `polleria-frontend` y callback `http://localhost:4200/auth/callback`.
No hay client secret, formulario local de credenciales ni configuración de despliegue.

## Seguridad

`core/auth` contiene AuthService con signals, PKCE, lectura mínima de claims,
callback y acceso denegado. Tokens en memoria y copia en sessionStorage;
ninguno en localStorage. Los roles proceden de `realm_access.roles` del JWT.
La lectura de claims en Angular sirve a la interfaz; el backend valida firma y permisos.

El callback valida state de un solo uso y limpia inmediatamente la URL.
Los guards funcionales protegen `/interno` y sus hijos. El interceptor añade Bearer
exclusivamente a `apiBaseUrl + /api/`, coordina refresh y limita a un reintento por
petición ante 401. Un 403 muestra acceso denegado sin renovar ni cerrar sesión.
Logout utiliza el endpoint de Keycloak y limpia solamente las claves propias.

ADMINISTRADOR administra Catálogo, Clientes y Pedidos. CAJERO lee Catálogo,
crea/consulta/actualiza Clientes y usa Pedidos; no elimina Clientes.
REPARTIDOR accede a Mis entregas, inicio y diagnóstico. ABASTECIMIENTO accede a Insumos, Proveedores, Compras e Inventario.
CLIENTE inicia/cierra sesión en el área pública y no entra al portal interno.
No hay UI de reportes ampliados. Delivery y Abastecimiento tienen sus pantallas operativas. El CLIENTE ahora tiene
perfil comercial vinculado por JWT sub, checkout y pedidos propios.

Las rutas comerciales conservan `/interno/categorias`, `/interno/productos`,
`/interno/clientes` y `/interno/pedidos`, con sus formularios existentes.
El detalle transaccional y sus precios históricos siguen gobernados por el backend.

## Verificación

Ver [VERIFICACION-FASE-14.md](VERIFICACION-FASE-14.md): build correcto,
89 pruebas correctas, compra e inventario reales, y regresión de los módulos previos.
Los documentos de fases anteriores conservan el historial de decisiones y pruebas.

`npm-audit-fase-14.json` conserva 7 hallazgos: 5 altos y 2 críticos.
No se ejecutó audit fix, no se cambiaron versiones ni se agregaron dependencias.
La aplicación usa configuración local HTTP; no constituye un despliegue productivo.
Git lo administra el usuario; no se realizaron operaciones de escritura sobre Git.

## Experiencia pública

`/catalogo` y `/carrito` funcionan sin login. Se muestran productos no disponibles
sin botón de agregar. Los productos sin imagen usan un placeholder propio; no se
agregan imágenes aleatorias ni upload. El carrito agrupa por productoId, admite
cantidades enteras de 1 a 999 y conserva solo líneas comerciales no sensibles en
la clave sessionStorage `polleria_carrito`, separada de autenticación.

`/perfil`, `/checkout`, `/mis-pedidos` y `/mis-pedidos/:id` reutilizan los guards de
Fase 11 y exigen CLIENTE. Si falta perfil, checkout guía a `/perfil?continuar=checkout`.
La creación exitosa del perfil vuelve al checkout con el carrito conservado.
El cliente no elige su identidad comercial ni consulta endpoints internos de Clientes.

Checkout ofrece RECOJO/DELIVERY y EFECTIVO/YAPE/PLIN/TARJETA. No cobra en línea:
pago inicial PENDIENTE. DELIVERY exige dirección y referencia opcional; modificar
el domicilio del pedido no cambia el perfil. El carrito se vacía después de POST
exitoso y permanece ante errores. El total confirmado y precios históricos se leen
siempre de la respuesta de Pedidos, sin sustituirlos por Catálogo actual.

La cancelación requiere confirmación y solo se ofrece para el pedido propio
PENDIENTE no pagado. El backend verifica propiedad y estado con bloqueo de cabecera.
El registro público de cuentas Keycloak sigue pendiente: se usan identidades CLIENTE
reales de desarrollo, sin formulario local de contraseña ni Admin API en Angular.

## Delivery

`/interno/delivery` exige ADMINISTRADOR/CAJERO: lista entregas, asigna pedidos
DELIVERY/LISTO a repartidores activos y reasigna NO_ENTREGADO. Reutiliza los filtros
existentes de Pedidos. `/interno/reparto` exige REPARTIDOR y solo consume endpoints
propios resueltos por JWT sub; invita a activar perfil sin pedir identidad al usuario.
No usa Admin API de Keycloak, contraseñas ni secretos administrativos.

Iniciar, Entregado y No entregado son operaciones explícitas; las dos últimas piden
confirmación. NO_ENTREGADO requiere observación no vacía. Incidencias se conservan
tras reasignación. EFECTIVO pendiente se marca PAGADO al entregar; YAPE/PLIN/TARJETA
no se confirman por repartidor. Detalle interno de Pedido permite confirmar pago
digital explícitamente con diálogo; no hay pasarela ni cobro en línea.

Mis pedidos del CLIENTE sigue consultando solo Pedidos y refleja DESPACHADO/ENTREGADO
al actualizar. Sin mapas, GPS, geolocalización ni actualizaciones en tiempo real.

## Abastecimiento

Rutas `/interno/abastecimiento/insumos`, `/proveedores`, `/compras`, `/inventario`
con ese prefijo común, solo ADMINISTRADOR/ABASTECIMIENTO mediante guards existentes.
Insumo tiene stock inicial cero, no editable en su formulario; unidad controlada.
Proveedor admite RUC opcional de 11 dígitos, sin consulta SUNAT ni validar formalidad.

Compras usa Reactive Forms/FormArray, proveedores e insumos activos y sin duplicados.
Cantidad hasta tres decimales; costo unitario hasta cuatro; punto decimal. El request
solo envía IDs/cantidad/costo; los decimales se mantienen como texto para conservar
la entrada exacta. Backend BigDecimal calcula total, stock y entradas. Después del
éxito se muestran total real/entradas y se actualizan datos; ante error se conserva el formulario.

Inventario consulta historial con filtros Insumo/Tipo. Salida exige cantidad positiva
más motivo. Ajuste solicita nuevo conteo físico no negativo, y el backend registra
la diferencia. No se envían stockAnterior/stockPosterior. No hay entrada manual,
recetas, vínculo con Producto ni descuento automático por ventas.
