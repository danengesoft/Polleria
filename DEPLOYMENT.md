# POLLERIA — Preparación de producción

**Paquete validado localmente; no publicado.** La publicación y la limpieza de datos
son dos autorizaciones independientes del propietario. Este documento no las concede.
La configuración mantiene `BIND_ADDRESS=127.0.0.1`; el preflight rechaza una apertura
pública. Solo se revisará esa barrera cuando exista orden explícita, servidor y dominio.

## Topología y requisitos

Un origen público HTTPS: Nginx sirve Angular; `/api/` conserva el path hacia
`backend:8080`; `/auth/` conserva el path hacia `keycloak:8080`. El callback exacto
`/auth/callback` pertenece a Angular y tiene prioridad sobre el proxy de Keycloak.
No se requiere dominio de autenticación separado ni configuración runtime adicional
en el navegador. La SPA obtiene su origen de `window.location.origin`; API relativa.

Oracle comercial y PostgreSQL exclusivo de Keycloak no tienen `ports` en Compose.
Backend y Keycloak tampoco. Todos utilizan una red Docker propia `internal:true`;
solo Nginx se conecta además a `edge` para recibir entrada. No reutilizar redes ni
volúmenes dev/ajenos. Docker genera los volúmenes `${COMPOSE_PROJECT_NAME}_oracle-data`
y `${COMPOSE_PROJECT_NAME}_keycloak-data`. No usar los volúmenes dev en ensayos.

Los cinco servicios tienen healthchecks y reinicio `unless-stopped`. Las dependencias
esperan bases/Keycloak saludables. El backend valida el schema explícito y su health
incluye conexión DB. No activar DDL automático para solucionar un arranque fallido.
Si falla repetidamente, revisar logs y detener el servicio; no borrar el volumen.
Logs Docker: configurar límites `max-size/max-file` como incluye Compose.

Estimación prudente para un único host de baja carga: x86_64,4vCPU y8GB RAM son un
punto de partida ajustado; preferible4–8vCPU,12–16GB RAM,SSD80–120GB más capacidad
para crecimiento y backups. No es garantía de concurrencia/rendimiento. Oracle Free
limita su uso de recursos/datos y deben revisarse sus condiciones antes del servidor
real. Oracle + Keycloak hacen inadecuado un VPS demasiado pequeño. En este ensayo se
comprobó la arquitectura, no un benchmark ni una capacidad de ventas garantizada.
Revisar disco, memoria, Docker/Compose compatible con `!override`, y conectividad de
build a registros oficiales. No se contrató ningún proveedor.

Host con hora correcta/NTP administrado: JWT y fechas dependen de ella. No se modificó
la hora ni servicios del host. Contenedores y JVM usan `America/Lima`; Oracle mantiene
DBTIMEZONE UTC. El backend conserva las reglas de calendario Perú de los reportes;
no se convierten históricos ni se cambia DBTIMEZONE a mano.

## Variables y secretos

Copiar `.env.production.example` a `.env.production`, permisos0600. Completar localmente
sin pegar valores en comandos, commits o informes. Solo nombres y URLs públicas viajan
al frontend. Contraseñas de Oracle/SYS, app, PostgreSQL y bootstrap KC son diferentes,
aleatorias y de al menos24 caracteres; no hay valores funcionales por defecto.

`COMPOSE_PROJECT_NAME`, `IMAGE_TAG`, `APP_DOMAIN`, `PUBLIC_APP_URL`, `BIND_ADDRESS`,
`HTTP_PORT`, `HTTPS_PORT`, `ORACLE_APP_USER=POLLERIA`, `ORACLE_PASSWORD`,
`ORACLE_APP_PASSWORD`, `KEYCLOAK_DB_PASSWORD`, `KEYCLOAK_ADMIN_USERNAME`,
`KEYCLOAK_ADMIN_PASSWORD` se describen en la plantilla. `PUBLIC_APP_URL` es un origen
HTTPS sin slash final y su host coincide exactamente con `APP_DOMAIN`. No usar localhost
para una instalación pública. Los únicos valores localhost/127 del paquete son dev,
healthchecks internos y ensayo local.

Secretos se inyectan por environment de Compose en runtime, nunca build args ni Angular.
Acceso al socket Docker equivale a privilegios administrativos: puede leer environment.
Restringir usuarios del host, permisos y backups; no se introduce Vault/KMS.

```bash
chmod 600 .env.production
python3 scripts/production/preflight.py --env-file .env.production
```

El preflight valida campos, fuerza HTTPS en modo final, detecta placeholders, verifica
ignorados/no versionados, permisos, Docker, puertos libres y certificados. Genera solo
configuración pública del realm en `production/generated/<proyecto>/polleria-realm.json`.
No crea usuarios comerciales. Ejecutarlo con puertos libres; para actualización detener
la entrada del stack durante la ventana acordada. Su salida no imprime secretos.
Cada instalación/ensayo debe usar su propio checkout y archivo de entorno.

## Build e inicio controlado

```bash
docker compose --env-file .env.production -f compose.prod.yml config --quiet
docker compose --env-file .env.production -f compose.prod.yml build
# Bases primero. Ninguno de estos comandos publica Internet con el binding por defecto.
docker compose --env-file .env.production -f compose.prod.yml up -d oracle keycloak-db keycloak
```

En una instalación **vacía y aprobada**, obtener el ID de Oracle con
`docker compose ... ps -q oracle`. Confirmar FREEPDB1 y que no hay tablas POLLERIA.
Aplicar una vez cada script01–06, en orden, como SYS local dentro del contenedor:

```bash
# ORACLE_CONTAINER es el ID propio obtenido del Compose correcto; verificarlo antes.
for sql in polleria-backend/scripts/0*.sql; do
  docker exec -i "$ORACLE_CONTAINER" sqlplus -s / as sysdba < "$sql" || exit 1
done
```

Oracle APP_USER ya crea POLLERIA con los permisos básicos. Los scripts contienen
cambio de sesión al PDB; nunca ejecutarlos sobre otro proyecto. En una base existente
no repetirlos: inventariar versiones aplicadas y ejecutar solo incrementales faltantes,
con backup y revisión. No se añadió Flyway/Liquibase ni una migración nueva en Fase18.
DDL Oracle tiene commits implícitos: rollback de imagen no revierte cambios de schema.

```bash
docker compose --env-file .env.production -f compose.prod.yml up -d --wait backend frontend
python3 scripts/production/healthcheck.py --url "$PUBLIC_APP_URL"
```

La shell debe conocer únicamente la URL pública para el último comando; no hacer
`source` de un .env no confiable. No ejecutar estos comandos hacia un servidor externo
sin autorización. El orden operativo es backup→variables/certificados→bases→KC→schema
necesario→backend→Nginx→health→smoke. Conservar logs del preflight/build sanitizados.

Las imágenes de base están fijadas por digest. Backend: build Java21/wrapper Maven,
runtime Java21, usuario999 no root, sin devtools ni fuentes de build en runtime.
Frontend: `npm ci --ignore-scripts` con lock, build Angular y copia exclusiva de estáticos
a Nginx. No incorpora Node/node_modules a runtime. `.dockerignore` usa listas positivas.
Las imágenes de aplicación deben etiquetarse con un identificador inmutable de release;
conservar ID/digest, no sobrescribir la etiqueta anterior ni depender de `latest`.

## Keycloak26.8.0 y autenticación

Se verificaron opciones con `kc.sh start --help` de la imagen real26.8.0, además de
[hostname](https://www.keycloak.org/server/hostname) y
[reverse proxy](https://www.keycloak.org/server/reverseproxy).
Compose usa `start --import-realm`, nunca `start-dev`; DB PostgreSQL persistente,
`KC_HOSTNAME=<PUBLIC_APP_URL>/auth`, `KC_HTTP_RELATIVE_PATH=/auth`,
`KC_PROXY_HEADERS=xforwarded`, listener HTTP solo interno y health management9000
sin publicación. Nginx termina TLS y sobrescribe los headers forwarded; no se confía
en headers del cliente para el scheme/IP. La red interna excluye acceso directo externo.

Issuer Spring exacto `<PUBLIC_APP_URL>/auth/realms/polleria`; JWK por DNS interno
`http://keycloak:8080/auth/realms/polleria/protocol/openid-connect/certs`. Separar la
obtención de claves no elimina validación `iss`. El navegador jamás recibe URL interna
keycloak:8080. El cliente público `polleria-frontend` mantiene Code + PKCE S256,
password/implicit/service accounts deshabilitados, sin client secret en SPA.

El realm generado tiene solo cinco roles, redirect exacto `<PUBLIC_APP_URL>/auth/callback`,
logout exacto `<PUBLIC_APP_URL>/`, webOrigin exacto. En final requiere SSL `all`; el ensayo
HTTP cerrado usa `external` únicamente para loopback. No se alteró el realm dev.
El import de arranque **no actualiza** un realm ya existente: si se restaura PostgreSQL
o se cambia URL, actualizar ese cliente vía Admin API/Console controlada y verificar
los tres campos, PKCE, discovery e issuer antes de habilitar tráfico.

El bootstrap no es una cuenta comercial definitiva. Crear las cuentas de roles del
negocio mediante administración controlada, sin credenciales de desarrollo. Usuarios
dev se inventarían y decidirán antes de go-live; no se eliminaron ni rotaron aquí.
`smoke-seed.mjs` se niega fuera de `polleria-prodtest`; no ejecutarlo en producción.
El proxy responde404 a Admin Console/Admin REST, health/metrics KC. Administración
por túnel SSH privado a la red del stack o procedimiento local autorizado, con hostname
administrativo y TLS adecuados si se habilita una entrada privada. No abrir8080/9000
ni hacer pública la consola para sortear el bloqueo.

## Nginx, TLS y políticas

`https.conf.template` publica80→308 HTTPS y443TLS1.2/1.3. Montar certificado del dominio
aprobado `production/certs/fullchain.pem` y clave0600 `privkey.pem`; ambos ignorados.
El certificado autofirmado IP127 del ensayo no es válido para go-live y no se reutiliza.
No se pidió certificado público, no se cambiaron DNS ni firewall.

Solo tras autorización: revisar binding/firewall del servidor para80/443 y SSH administrado;
no abrir Oracle1521, PostgreSQL5432, Spring8080 ni health KC9000. No modificar la laptop
como sustituto del firewall de producción. Revisar puertos Docker realmente publicados,
pues sus reglas pueden eludir ciertas configuraciones de firewall del host.

SPA fallback para rutas de aplicación; `/api` nunca se convierte en index.html.
Assets hash cache1año/immutable; index y callback no-store. gzip para JS/CSS/JSON/SVG,
límite1MiB, conexión upstream5s y lectura30s. Headers nosniff, referrer, SAMEORIGIN y
CSP limitada a frame-ancestors/object-src/base-uri, probada sin bloquear Angular/KC.
No se impuso una política script-src no evaluada. No HSTS hasta HTTPS real y dominio
verificados. Access log registra método/path/status sin query strings, cuerpos ni
Authorization; evita guardar códigos OAuth. Logs de backend prod a stdout con Trace ID.

Swagger/API docs deshabilitados en perfil prod y bloqueados por Nginx. Actuator expone
solo health, detalles nunca, consultado dentro de Docker; el proxy público responde404.
CORS permite únicamente PUBLIC_APP_URL, aunque SPA/API comparten origen. Se conserva
la protección contra otros orígenes sin wildcard. Dev mantiene localhost4200 permitido.
No se cambiaron respuestas comerciales ni exposición de SQL/stack al cliente.

## Backup y recuperación

Ver [procedimiento de backup/restore](production/BACKUP-RESTORE.md). Backups lógicos
Oracle Data Pump y pg_dump reales, privados, externos al repositorio. No copiar un
volumen activo como único plan. Guardar manifiestos/hash; cifrar una copia offline y
definir retención fuera del host con el propietario. No se subieron respaldos a ningún
servicio. Un solo disco/host no ofrece recuperación ante pérdida total.

## Actualización y rollback

1. Acordar ventana y verificar autorización/estado. Backup de ambas DB y guardar hashes,
   entorno/certificados de forma privada y versiones/IDs de las imágenes actuales.
2. El propietario obtiene el código aprobado; esta fase no hace pull/merge ni commits.
3. Crear etiqueta nueva, revisar env/realm y construir. Aplicar solo SQL incremental
   aprobado, si existe; nunca cambiar ddl-auto ni editar dentro de contenedores.
4. Reemplazar únicamente servicios necesarios (`up -d --wait`). Revisar health, logs,
   login/logout, catálogo y una consulta interna. Monitorear errores y disco.
5. Si falla y schema compatible, volver al IMAGE_TAG anterior y `up -d --wait`.
   Si hubo cambios incompatibles DB, detener escrituras y seguir restauración coordinada
   de backups con decisión explícita; no restaurar en caliente ni automáticamente.

El frontend/API/KC son una unidad de compatibilidad. No cambiar issuer/hostname como
rollback improvisado. Conservar el volumen PostgreSQL; sus usuarios/realm no se
reconstruyen a partir de una exportación de configuración sin credenciales.

## Ensayo local aislado y desarrollo

`.env.prodtest` (privado0600) usa proyecto polleria-prodtest, imagen prod-test-f18,
URL `http://127.0.0.1:18080`, secretos aleatorios y volúmenes independientes.

```bash
python3 scripts/production/preflight.py --env-file .env.prodtest --local-test
docker compose --env-file .env.prodtest -f compose.prod.yml -f compose.prodtest.yml build
docker compose --env-file .env.prodtest -f compose.prod.yml -f compose.prodtest.yml up -d oracle keycloak-db keycloak
# Aplicar01–06 SOLO si el schema temporal está vacío; después:
docker compose --env-file .env.prodtest -f compose.prod.yml -f compose.prodtest.yml up -d --wait backend frontend
python3 scripts/production/healthcheck.py --url http://127.0.0.1:18080
# Ensayo de parada/arranque conservando datos:
docker compose --env-file .env.prodtest -f compose.prod.yml -f compose.prodtest.yml stop
docker compose --env-file .env.prodtest -f compose.prod.yml -f compose.prodtest.yml up -d --wait
```

HTTP cerrado no autoriza autenticación pública por HTTP. Se probó además la plantilla
TLS con CA autofirmada local confiada explícitamente por el cliente de prueba, sin
ignorar validación de certificados. No visitar interstitials ni reutilizar esa CA.
No ejecutar `down -v`. Al cierre se puede detener prodtest conservando sus volúmenes.

Desarrollo continúa con Compose dev originales, Angular4200, Spring8080, KC38080 y
ambiente Angular `environment.ts`; `ng serve` utiliza development. `npm run build`
usa producción con reemplazo de environment, y tests conservan ambiente dev.
No usar el build de producción directamente con ng serve/dev esperando issuer38080.
Consultar [verificación final](VERIFICACION-FASE-18.md) y
[GO-LIVE-CHECKLIST](GO-LIVE-CHECKLIST.md) antes de cualquier publicación.
