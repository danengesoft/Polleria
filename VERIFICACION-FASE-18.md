# FASE18 — Preparación de producción y despliegue

**FASE18 COMPLETADA — PRODUCTION READY (paquete validado localmente, NO PUBLICADO).**
25 de octubre de2026. Las órdenes de publicar y limpiar datos siguen pendientes del
propietario. No se ejecutó ninguna de ellas. Dominio, servidor, certificado público,
credenciales definitivas y política final de datos deben decidirse en go-live.

## Evidencia

- [Deployment y comandos](DEPLOYMENT.md), [Go-live y autorizaciones](GO-LIVE-CHECKLIST.md).
- [Compose final](compose.prod.yml), [override exclusivamente local](compose.prodtest.yml).
- [Plantilla de entorno sin secretos](.env.production.example).
- [Inventario inicial Git](production/inventario-inicial-fase-18.json):árbol inicialmente limpio.
- [Inventario comercial sin PII](production/inventario-datos-fase-18.json).
- [Imágenes y digests](production/imagenes-fase-18.json).
- [Estado/persistencia y desarrollo](production/verificacion-stack-fase-18.json).
- [Smoke HTTP vía Nginx](production/verificacion-http-fase-18.json).
- [Navegador real](production/verificacion-navegador-fase-18.json).
- [HTTPS local y redirect](production/verificacion-tls-fase-18.json).
- [Backups reales](production/verificacion-backup-fase-18.json), [restore documentado](production/BACKUP-RESTORE.md).
- [Preflight negativo](production/verificacion-preflight-fase-18.json).
- [Secretos/ignores](production/verificacion-secretos-fase-18.json), [logs](production/verificacion-logs-fase-18.json).
- [Integridad comercial final](production/oracle-integridad-fase-18.jsonl).
- [npm audit F18](polleria-frontend/npm-audit-fase-18.json).

Logs de build, tests, Compose y preflight permanecen locales ignorados por Git. No se
almacenaron tokens, secretos, backups, certificados privados, capturas ni PDFs en
archivos versionables. Las evidencias JSON registran resultados sanitizados.

## Resumen técnico solicitado:60 puntos

| Nº | Aspecto | Resultado |
|---|---|---|
| 1 | Arquitectura | Un origen HTTPS; Nginx sirve SPA, proxy `/api/` a Spring y `/auth/` a Keycloak. Oracle y PostgreSQL independientes en red interna propia. |
| 2 | Archivos creados | Compose prod/prodtest, plantilla env, Dockerfiles/dockerignore, perfil prod, environment production Angular, Nginx, scripts operativos, Deployment/Go-live/Backup/Verificación y evidencias. Lista debajo. |
| 3 | Archivos modificados | logback-spring.xml, angular.json y README de backend/frontend. Sin cambios de dominio, entidades, roles, APIs comerciales o schema actual. |
| 4 | Dockerfile backend | Multietapa Java21/wrapper Maven; JRE21, usuario999 polleria, artefacto único. Build real exitoso. Devtools excluido por empaquetado Spring Boot. |
| 5 | Dockerfile frontend | Node24 con npm ci/lock→ng build→Nginx1.28. Runtime solo estáticos, sin Node/node_modules. Bases por digest. |
| 6 | Nginx | Configuración validada con nginx -t y ejecución real. Proxy, fallback, cache/gzip, límites, TLS y headers. Logging sin query strings y rotación Docker acotada. |
| 7 | SPA fallback | Directas catálogo/carrito/Mis pedidos/perfil/interno productos/pedidos/reportes devuelven index200. Browser real recarga protegida y carrito correcto. |
| 8 | Proxy API | Mantiene `/api/v1/...` sin reescritura incorrecta. Público200, protegido anónimo401; consulta autenticada de Clientes y escrituras mínimas temporales correctas. `/api` exacto404, no SPA. |
| 9 | Keycloak | Imagen26.8.0 real, `start --import-realm`, PostgreSQL propio, health9000 interno, hostname y path/auth. Opciones contrastadas con help de esa versión; nunca start-dev en Compose prod. |
| 10 | Issuer | Spring valida URL pública exacta `<PUBLIC_APP_URL>/auth/realms/polleria`; JWK por DNS interno, sin relajar iss. Discovery devuelve issuer público, sin keycloak:8080 al navegador. |
| 11 | URLs Angular | Reemplazo production del environment:API relativa y Auth del mismo window.location.origin. Sin client secret ni host/puerto backend hardcodeado. Dev no cambia. |
| 12 | CORS | Únicamente PUBLIC_APP_URL exacto; mismo origen SPA/API. Desarrollo sigue permitiendo localhost4200; no wildcard. |
| 13 | Swagger | Deshabilitado en perfil prod y404 por proxy. Sigue operativo en dev. |
| 14 | Actuator | Solo health interno, detalles nunca; proxy público404. Healthcheck backend comprueba agregado UP con DB. |
| 15 | Puertos públicos previstos | Solo80/443 de Nginx, tras autorización. Binding preparado por defecto127.0.0.1 y preflight rechaza apertura pública; SSH administrado depende del servidor futuro. |
| 16 | Puertos internos | Oracle1521, Postgres5432, Spring8080, KC8080/9000; ninguno publicado por Compose prod. Ensayo frontend127.0.0.1:18080; TLS auxiliar18443/18081 solo loopback y posteriormente detenido. |
| 17 | Redes | Red internal:true propia, más edge solo para Nginx. Sin reutilizar redes ajenas. TLS auxiliar necesitó conexión edge además de internal; esto se corrigió en su ensayo. |
| 18 | Volúmenes | `<proyecto>_oracle-data`, `<proyecto>_keycloak-data`. Prodtest separado de `polleria-oracle-data` y `polleria-keycloak-postgres-data`. Sin target/node_modules persistentes. |
| 19 | Healthchecks | Oracle healthcheck.sh, PostgreSQL pg_isready, KC management/health/ready, Spring health UP, Nginx healthz. Cinco healthy y recuperación completa probada. |
| 20 | Restart | unless-stopped para los cinco; logs json-file10MiB×3 por servicio. Dependencias service_healthy. No loops destructivos ni resets. |
| 21 | Variables | Plantilla completa:proyecto/tag/dominio/URL/binding/puertos/usuario Oracle/secretos Oracle, PostgreSQL y KC. Sin valores de contraseñas funcionales. Validación privada y render del realm por proyecto. |
| 22 | Secretos | Revisión final sin contraseñas reales ni JWT completos en archivos versionables. .env.prodtest0600 ignorado; certs/generados/backups/logs ignorados; .env.production.example sí versionable. |
| 23 | Build backend | Imagen `polleria-backend:prod-test-f18` construida realmente, arranque no root y JPA validate contra Oracle aislado. |
| 24 | Tamaño backend |556,565,834bytes, aprox556.57MB decimal. JAR/runtime Java/curl; no .git/env/caches de Maven del builder. ID exacto en manifiesto de imágenes. |
| 25 | Build frontend | `polleria-frontend:prod-test-f18` construida realmente; estáticos Angular servidos por Nginx. |
| 26 | Tamaño frontend |95,222,063bytes, aprox95.22MB decimal. No node_modules en runtime. |
| 27 | Compose config | Config producción y override local válidas; salida completa capturada solo en memoria/quiet para evitar secretos. Preflight positivo y tres negativos (placeholder/password corto/variable ausente) comprobados. |
| 28 | Stack prod-like | Cinco servicios propios levantados con volúmenes nuevos, luego stop→up/health, sin borrar datos. Config final por digest también healthy. |
| 29 | Frontend Nginx | Inicio200, catálogo público, login/callback, consulta protegida, logout y carrito real probados desde navegador. |
| 30 | Rutas directas | Smoke HTTP de las rutas requeridas correcto; F5 real de Clientes protegido y carrito mantiene sesión/contenido respectivamente. |
| 31 | API proxy | API pública200/protegida401 anónima; autenticada200 por Angular. POST categoría/producto temporal exitoso; path relativo e integridad JPA correctos. |
| 32 | Auth prod-like | Login real Code+PKCE S256/state/callback con cuenta temporal ADMINISTRADOR; protegida correcta incluso tras reinicio completo. Solo HTTP cerrado local; no se afirma login público HTTPS con dominio inexistente. |
| 33 | Logout | Keycloak→Inicio, sin sesión local; nuevo login tras reinicio correcto. Cuenta temporal se conserva únicamente en DB prodtest. |
| 34 | Trace ID | X-Trace-ID `fase18-proxy` viaja cliente→Nginx→Spring→respuesta en público200/protegido401. Nginx no lo elimina. |
| 35 | Headers | nosniff, strict-origin-when-cross-origin, SAMEORIGIN y CSP limitada probados. Includes conservan headers también al definir cache por location. Sin romper login/Angular. |
| 36 | HTTPS | Plantilla final443TLS1.2/1.3 y80→308. Ensayo autofirmado local:HTTPS200 confiando CA explícitamente, redirect308; no certificado público, DNS o exposición Internet. |
| 37 | Backup Oracle | Data Pump schema POLLERIA con flashback_time=systimestamp, autenticación OS en FREEPDB1, exit0, dump no vacío1,196,032bytes y SHA256, archivo0600 externo. No copia de volumen activo. |
| 38 | Backup KC | pg_dump custom PostgreSQL,265,511bytes,0600 externo; pg_restore --list con TABLE DATA correcto. Incluye identidad persistida; plantilla realm sin usuarios es solo bootstrap. |
| 39 | Restore | Procedimientos Oracle/PG documentados con destinos vacíos/aislados, verificación de hashes, versiones y keycloakSub. No se ejecutó restore sobre datos actuales ni se presenta como restore integral ensayado. |
| 40 | Inventario |12 tablas actuales inventariadas:1 categoría/2 productos/4 clientes/12 pedidos/13 detalles/4 entregas/2 repartidores/2 compras/4 detallesCompra/5 insumos/2 proveedores/10 movimientos. Catálogo Pollos/1/4 Pollo documentado, sin decidir su destino. |
| 41 | Cleanup | Plantilla SQL guardada, rechazo por defecto, schema/PDB, backup/manifiesto/conteos/orden FK/transacción/rollback. Sin DELETE operativo porque faltan IDs/decisión del propietario. |
| 42 | Cleanup NO ejecutado | Confirmado. Ningún truncate/delete masivo/drop/down-v/recreación de schema actual ni eliminación de volúmenes. Schema inicial creado solo en DB temporal vacía. |
| 43 | Usuarios dev | No son cuentas definitivas; inventariar/rotar/recrear/eliminar solo con decisión previa a go-live. No se alteraron usuarios ni redirects dev. Smoke-f18 vive solo en prodtest. |
| 44 | Go-live checklist | Creado con dos autorizaciones explícitas separadas, backup, datos, usuarios, DNS/HTTPS, CORS/redirects, salud/smoke y monitoreo inicial. Sin casillas de autorización marcadas. |
| 45 | Actualización | Backup, código aprobado por propietario, nueva etiqueta inmutable, SQL incremental solo si corresponde, reemplazo servicios, health/smoke. Sin editar contenedores. |
| 46 | Rollback | Imagen previa conservada; si schema compatible volver a tag previo. DB incompatible exige parar escrituras y restauración coordinada autorizada, nunca automática sobre activo. |
| 47 | Servidor orientativo |4vCPU/8GB ajustado; preferible4–8vCPU,12–16GB RAM ySSD80–120GB más backups/crecimiento. Estimación de baja carga, no garantía de rendimiento. Sin proveedor contratado. |
| 48 | npm audit |0 vulnerabilidades en nueva auditoría F18; sin cambios de dependencias ni force. |
| 49 | Backend tests | clean compile BUILD SUCCESS y201/201 pruebas,0 fallos/errores/omitidas, repetidos tras logging final. Spring Modulith cinco módulos/sin ciclos. |
| 50 | Frontend build/tests | Build producción correcto, inicial330.78kB/transferencia estimada92.65kB sin warnings de budgets;105/105 pruebas, nueve archivos. |
| 51 | Desarrollo | Angular4200/API8080/KC38080/health200; catálogo real dev renderiza Pollos/1/4 Pollo y producto F9 no disponible. Oracle actual conserva22 controles de integridad sin inconsistencias. |
| 52 | Bloqueantes | Ninguno pendiente en la preparación local. Publicación sigue bloqueada intencionalmente por ausencia de autorización/servidor/dominio/certificado público; no es una tarea ejecutada. |
| 53 | Altos | Ninguno pendiente detectado. Secretos fuera de Git y bases/API sin publicación directa. |
| 54 | Medios | Resueltos:log de archivo incompatible con usuario no root; regex Nginx sin comillas impedía arranque; access logs debían omitir query OAuth. Diagnóstico y repetición real correctos. |
| 55 | Bajos | Resuelto falso positivo de preflight por TIME_WAIT usando SO_REUSEADDR (sin permitir un listener ocupado). Recuperación integral de backup aún no ensayada; procedimiento preparado y prueba completa recomendada antes del host definitivo. |
| 56 | Warnings | Preexistentes Byte Buddy/Mockito y fetch JDBC10 en tests/dev. KC advierte contexto HTTP en ensayo cerrado; final exige HTTPS/ssl all. Aviso script oficial Nginx al tener config readonly es no bloqueante. Sin warnings/error persistentes en consola Angular probada. |
| 57 | NO publicación | Confirmado:sin servidor externo, DNS, certificado público, imágenes subidas ni puertos públicos nuevos. |
| 58 | NO borrado global | Confirmado:históricos de Oracle/KC dev conservados. Solo datos mínimos nuevos en DB temporal; no limpieza comercial ejecutada. |
| 59 | Git | Solo status/diff/check/log/ls-files/check-ignore de lectura. Ningún add/commit/push/pull/merge/reset ni escritura intencional .git. |
| 60 | Commit sugerido | `chore: preparar producción con Docker Nginx backups y validación aislada` — sugerencia únicamente. |

## Archivos y decisiones finales

Backend: [Dockerfile](polleria-backend/Dockerfile), [.dockerignore](polleria-backend/.dockerignore),
[application-prod.yml](polleria-backend/src/main/resources/application-prod.yml),
[logback](polleria-backend/src/main/resources/logback-spring.xml).
Frontend: [Dockerfile](polleria-frontend/Dockerfile), [.dockerignore](polleria-frontend/.dockerignore),
[environment production](polleria-frontend/src/environments/environment.production.ts),
[angular.json](polleria-frontend/angular.json). Ambos README enlazan operación.

Nginx en `production/nginx/`:HTTP de ensayo, HTTPS template, locations,
security-headers y00-logging. Scripts en `scripts/production/`:preflight.py,
backup.py, healthcheck.py, smoke-seed.mjs (solo prodtest) y cleanup-plan.sql protegido.
Root `.gitignore`, Compose, plantilla, Deployment, Go-live y este informe son nuevos.
Archivos JSON/JSONL de evidencia enlazados arriba son nuevos; logs ignorados no se
proponen versionar. Se conservan todos los artefactos históricos F17.

No se añadieron funcionalidades, tablas comerciales nuevas en el schema actual,
módulos, roles, métricas, Nginx del host, CI/CD ni dependencia cloud. No se cambió
el motor comercial ni el mecanismo Code+PKCE. Se creó el schema existente01–06 solo
en el Oracle temporal, para validar una instalación inicial sin afectar históricos.

La validación HTTPS fue de transporte/template estático con certificado local efímero.
El login de extremo a extremo se validó en HTTP loopback autorizado. El go-live debe
repetir login/logout por el dominio HTTPS real y verificar certificado/redirects; no
hay un dominio ficticio presentado como configuración final.

Incidencias de herramientas también diagnosticadas:OpenSSL no está instalado en el
host y se usó el de una imagen ya fijada, sin instalar paquetes del host. Primer
Data Pump por credencial no funcionó; se comprobó ORACLE_PDB_SID y se cambió a OS auth,
con export exit0 real. Un intento de aprobación automática quedó sin cuota y no se
ejecutó; tras la orden de continuar se reintentó y completó. No se eludió la revisión.
No se afirma que intentos fallidos fueran validaciones exitosas.

Los certificados autofirmados y secretos de ensayo están ignorados. Backups externos
privados permanecen en `/home/systemseng/polleria-backups/fase18`. Prodtest usa URL
`http://127.0.0.1:18080`; solo sirve en la máquina local. El proxy TLS auxiliar quedó
detenido y sus puertos loopback no son necesarios para revisar el paquete.

Se detiene el trabajo: **NO CLEANUP, NO PUBLICACIÓN, NO DNS, NO PUERTOS PÚBLICOS**.
