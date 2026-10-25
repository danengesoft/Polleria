# POLLERIA — SPA, Fase 11

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
REPARTIDOR y ABASTECIMIENTO acceden únicamente al inicio y diagnóstico existentes.
CLIENTE inicia/cierra sesión en el área pública y no entra al portal interno.
No hay UI de reportes, Delivery, Abastecimiento, Mis pedidos ni vínculo Cliente-sub.

Las rutas comerciales conservan `/interno/categorias`, `/interno/productos`,
`/interno/clientes` y `/interno/pedidos`, con sus formularios existentes.
El detalle transaccional y sus precios históricos siguen gobernados por el backend.

## Verificación

Ver [VERIFICACION-FASE-11.md](VERIFICACION-FASE-11.md): build correcto,
48 pruebas correctas, cinco roles probados con Keycloak real y refresh real.
Los documentos de fases anteriores conservan el historial de decisiones y pruebas.

`npm-audit-fase-11.json` conserva 7 hallazgos: 5 altos y 2 críticos.
No se ejecutó audit fix, no se cambiaron versiones ni se agregaron dependencias.
La aplicación usa configuración local HTTP; no constituye un despliegue productivo.
Git lo administra el usuario; no se realizaron operaciones de escritura sobre Git.
