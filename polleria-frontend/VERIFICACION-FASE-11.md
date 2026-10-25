# Verificación — Fase 11

Verificación local realizada el 24 de octubre de 2026 (America/Lima).
Alcance: autenticación y autorización frontend; sin comenzar Fase 12.

## Archivos

Creados:
- `src/app/core/auth/{auth-service,auth-callback,jwt,pkce,sin-permiso}.ts`.
- `src/app/core/auth/auth.spec.ts`.
- `src/app/core/guards/{auth-guard,rol-guard}.ts`.
- `src/app/core/interceptors/auth-interceptor.ts`.
- `src/app/testing/auth-fixture.ts` (excluido del build de aplicación).
- Este informe, `npm-audit-fase-11.json`, `oauth-verificacion-fase-11.json`
  y `keycloak-verificacion-fase-11.json`.

Modificados:
- `app.config.ts`, `app.routes.ts`, `environment.ts`, `tsconfig.app.json`.
- Navegación compartida, layout interno e inicio interno.
- Listados TS/HTML de Categoría, Producto y Cliente para acciones según roles.
- `app.spec.ts`, `features/crud.spec.ts`, `features/pedidos/pedido.spec.ts`:
  conservan las 31 pruebas anteriores con proveedor de autenticación para tests.
- README frontend.
- Backend: únicamente `keycloak/polleria-realm.json`, configuración del cliente OAuth.
  Sin cambios Java, SQL, CORS, SecurityConfig, pom ni contratos HTTP.

## Flujo y configuración

Referencia: S11, Seguridad Frontend Keycloak del repositorio académico BOMERP.
Implementación directa OAuth/OIDC, sin keycloak-angular ni dependencias adicionales.
Issuer `http://localhost:38080/realms/polleria`, cliente público `polleria-frontend`.
Callback exacto `http://localhost:4200/auth/callback`, webOrigin exacto
`http://localhost:4200`, retorno logout `http://localhost:4200/`.
Authorization Code habilitado; PKCE S256 exigido también en Keycloak;
Direct Access Grants desactivado. La configuración real y el JSON versionado coinciden.

Verifier aleatorio criptográfico por flujo, challenge SHA-256 base64url y state
aleatorio consumido una sola vez. Se rechaza state incorrecto antes del intercambio.
El destino de retorno admite solo rutas internas seguras. El código OAuth se retira
inmediatamente de la URL mediante reemplazo de historial.

AuthService gestiona exclusivamente identidad y sesión mediante signals. Extrae sub,
nombre y los cinco roles aprobados del JWT. Tokens en memoria con copia en
sessionStorage; no localStorage. No hay contraseñas ni secrets en Angular.
El backend sigue validando criptografía y autorización; el decode frontend es para UX.

Guard de autenticación funcional: restaura/renueva sesión o inicia login.
Guard de roles reutilizable: limita el portal a ADMINISTRADOR, CAJERO, REPARTIDOR,
ABASTECIMIENTO, y cada ruta comercial a los permisos de Fase 10.
CLIENTE no accede al portal. Los menús y botones usan los mismos roles reales.

Interceptor: Bearer solo para el origen/path de nuestra API. No se envía a Keycloak,
terceros ni imágenes externas. Ante 401 se coordina una promesa de renovación compartida,
se reutiliza un token ya renovado por otra petición y se reintenta una sola vez.
Si falla renovación o vuelve 401, se limpia sesión y se solicita autenticación.
Un 403 conserva sesión y muestra acceso denegado; no dispara refresh.
Los resultados tardíos de refresh no restauran una sesión cerrada durante la petición.
Logout local limpia solo claves propias y redirige al logout real de Keycloak.

## Pruebas de navegador real

| Rol | Resultado |
|---|---|
| ADMINISTRADOR | Login real, callback, portal, crear/editar/eliminar categoría temporal 328, Pedidos, F5 sin volver a pedir credenciales, logout real |
| CAJERO | Lectura Categorías/Productos sin acciones administrativas; crear/editar Cliente temporal 399; sin botón eliminar; Pedidos accesible; ruta crear categoría denegada |
| CLIENTE | Login correcto, identidad y logout en público; `/interno` denegado |
| REPARTIDOR | Inicio/diagnóstico mínimos; sin menú comercial; `/interno/pedidos` denegado |
| ABASTECIMIENTO | Inicio/diagnóstico mínimos; `/interno/clientes` denegado; sin funcionalidades inventadas |

Se eliminó el Cliente temporal 399 con ADMINISTRADOR y la Categoría 328 al terminar.
Se preservaron los registros históricos de fases anteriores. No se crearon pedidos.
Después de logout, intentar entrar al portal volvió al formulario de Keycloak,
confirmando cierre de SSO además del estado local.
Callback con code/state inválidos mostró un mensaje seguro y dejó la URL limpia.
El inicio público permaneció anónimo y la consola final no reportó warnings/errores.
Cabeceras con identidad, roles y logout, y acceso denegado comprobados a
390/768/1440 px sin desbordamiento horizontal.

Refresh real: temporalmente se fijó access token de este cliente a 20 segundos.
Se verificó recuperación tras expiración y llamadas posteriores, con eventos
REFRESH_TOKEN registrados por Keycloak (6 en total al cierre; incluyen pruebas
protocolarias, con renovaciones del navegador observadas antes de ellas).
Se restauraron la duración heredada del realm y la configuración previa de eventos.
El override temporal se limpió explícitamente, verificándolo mediante Admin REST.

## Comprobación HTTP complementaria y Bearer

Sesiones independientes obtenidas mediante Authorization Code + PKCE reales:
CAJERO consultó Catálogo y Pedidos (200), pero escritura Catálogo/reportes dieron 403.
CLIENTE, REPARTIDOR y ABASTECIMIENTO: lectura Catálogo 200, escritura Catálogo,
Pedidos y reportes 403. Token con firma alterada recibió 401; password grant recibió
400 por estar desactivado. Resumen sin secretos en `oauth-verificacion-fase-11.json`.
No se relajó SecurityConfig.

Las operaciones protegidas desde Angular funcionaron contra el backend stateless.
Las pruebas HttpTesting verifican el encabezado Authorization y su ausencia fuera de
la API. Limitación: la herramienta de navegador disponible no expone Network ni
cabeceras de peticiones; no se afirma haber inspeccionado directamente esa pestaña.
Los manejos UI de 401/reintento y 403 se verificaron automáticamente; los códigos
HTTP reales se comprobaron además mediante el flujo protocolario anterior.
No se imprimieron ni copiaron tokens en este informe.

## Verificación automatizada y pendientes

- `npm run build`: correcto, bundle optimizado generado, sin warning de presupuesto.
- `npm test -- --watch=false`: 4 archivos, 48 pruebas correctas: 31 anteriores + 17 nuevas.
- Nuevos casos: PKCE con vector conocido, sesión inicial/restauración, claims/roles,
  state inválido, intercambio callback, refresh concurrente/fallido, logout, guards,
  CLIENTE/ADMINISTRADOR, permisos CAJERO y alcance/reintento del interceptor.
- Backend Java sin cambios: no fue necesario repetir las 93 pruebas aprobadas en Fase 10.
  Configuración Keycloak comprobada directamente contra el servicio real.
- `npm audit`: 7 hallazgos pendientes, 5 altos y 2 críticos, sin cambios de dependencias.
  No se ejecutó audit fix --force ni se cambió la familia Angular.
- Persisten los avisos previos de política npm allowScripts; build y pruebas funcionan.
  No se hizo limpieza general de warnings ni se añadieron permisos de ejecución.
- Los datos ficticios de claims y AuthService en tests se usan exclusivamente como
  fixtures unitarios; la aplicación utiliza únicamente tokens emitidos por Keycloak.

Adaptaciones de S11: nombres/roles/rutas de POLLERIA, API restringida por origen y
path exactos, restauración con access expirado pero refresh vigente para evitar un
login innecesario después de F5, coordinación de refresh y protección contra carreras
con logout. Se conserva el patrón de memoria + sessionStorage y OAuth directo.
No se agregó librería de auth, UI de reportes, vínculo Cliente-sub ni nuevos módulos.

Git solo se consultó: sin add, commit, push, pull, merge, rebase ni reset.

Commit sugerido: `feat: integrar autenticación Angular con Keycloak y autorización por roles`
