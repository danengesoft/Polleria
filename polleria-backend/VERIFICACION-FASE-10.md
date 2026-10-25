# Fase 10 — Seguridad backend con Keycloak

Verificación local: 24 de octubre de 2026, America/Lima; 25 de octubre UTC.
Raíz `/home/systemseng/Polleria`. Backend `polleria-backend`.

## Referencias y alcance

Se consultaron S10b y la configuración de cliente de S11, sin implementar S10 JWT propio
ni autenticación Angular:

- https://github.com/262ciclo4/bomerp/blob/main/docs/lp2/sesiones/S10b_Seguridad_Backend_Keycloak.md
- https://github.com/262ciclo4/bomerp/blob/main/docs/lp2/sesiones/S11_Seguridad_Frontend_Keycloak.md
- Spring Boot 4: starter modular de pruebas de Security, conforme a su guía de migración:
  https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide
- Readiness de Keycloak: https://www.keycloak.org/server/health

Spring Boot valida JWT de Keycloak: no firma, emite ni refresca tokens propios.
Oracle mantiene el único datasource comercial. PostgreSQL pertenece exclusivamente
a Keycloak. No se implementa Delivery, Abastecimiento ni Fase 11.

## Archivos creados

- compose-keycloak.yml: dos servicios, red y volumen propios, independiente de Oracle.
- keycloak/polleria-realm.json: realm, roles funcionales y cliente público, sin usuarios/secretos.
- keycloak/verification-http.json: resultados HTTP reales, sin tokens ni contraseñas.
- scripts/keycloak-dev-users.py: aprovisionamiento explícito de los cinco usuarios locales,
  leyendo secretos de .env, mediante la API Admin de Keycloak.
- scripts/verify-keycloak-http.py: prueba real de tokens/permisos; no almacena JWT.
- src/main/java/pe/edu/upeu/polleria/SecurityConfig.java.
- src/main/java/pe/edu/upeu/polleria/SecurityErrorHandler.java.
- src/test/java/pe/edu/upeu/polleria/SecurityIntegrationTests.java.
- Este documento.

## Archivos modificados

- pom.xml: starters Security, OAuth2 Resource Server y Security test, versiones gestionadas por Boot.
- application-dev.yml: issuer-uri y health sin detalles internos.
- CorsConfig.java: fuente CORS única integrada en Security; admite Authorization.
- OpenApiConfig.java: Bearer JWT; GET públicos de Catálogo sin requisito de seguridad.
- filter/CorrelationIdFilter.java: prioridad anterior a filtros de seguridad.
- CatalogoIntegrationTests, ClientesIntegrationTests, PedidosIntegrationTests y
  ReportesIntegrationTests: principal administrativo simulado, sin desactivar filtros.
- PedidosIntegrationTests: PUT/DELETE inexistentes esperan 403 por denyAll en vez de 405.
- ModularityTests: exige exactamente los tres módulos comerciales, sin añadir Seguridad.
- .env.example: placeholders para secretos de PostgreSQL/Keycloak/usuarios; issuer público.
- .gitignore: caché Python generada excluida.
- README.md: infraestructura, permisos, comandos y comportamiento actual.

El .env local excluido se amplió con secretos aleatorios de desarrollo y mantiene modo
0600. No se muestra su contenido. Ningún archivo de polleria-frontend fue modificado.

## Infraestructura real

Antes de crear servicios se ejecutaron docker ps, docker ps -a y revisión de puertos.
35432 y 38080 estaban libres. PostgreSQL ajeno en 5433 se preservó.

| Servicio | Imagen / versión real | Puerto host | Persistencia |
| --- | --- | --- | --- |
| polleria-keycloak | quay.io/keycloak/keycloak:26.8.0 / 26.8.0 | 127.0.0.1:38080 → 8080 | Realm/usuarios en PostgreSQL |
| polleria-keycloak-db | postgres:17-alpine / PostgreSQL 17.11 Alpine | 127.0.0.1:35432 → 5432 | polleria-keycloak-postgres-data |
| polleria-oracle, preexistente | Oracle Free, sin recrear | 127.0.0.1:1522 → 1521 | polleria-oracle-data, sin modificar |

Compose separado: proyecto polleria-identity-dev. Evita que levantar/recrear identidad
incluya Oracle. No se crea un volumen de datos Keycloak adicional: el estado relevante
vive en PostgreSQL y el realm inicial es un bind mount de solo lectura. En Fedora,
el montaje lleva etiqueta SELinux Z. No se publica el puerto de management 9000.

Compose config --quiet correcto. PostgreSQL healthy mediante pg_isready. Keycloak
healthy mediante HTTP /health/ready interno en 9000, no solo estado running.
La comprobación usa bash /dev/tcp porque la imagen no trae curl. Oracle sigue healthy.

PostgreSQL comprobado directamente con psql: versión 17.11, realms master/polleria,
101 tablas internas Keycloak en su base polleria_keycloak. URL interna:
jdbc:postgresql://keycloak-db:5432/polleria_keycloak.
No se utiliza el contenedor ni volumen punto-u-dev-postgres-1, tampoco mi_oracle.

## Realm, cliente, roles y usuarios

Realm: polleria, habilitado, sin autorregistro; idioma español.
Cliente: polleria-frontend, OpenID Connect público, sin secret ni service account,
Standard Flow activado, Implicit Flow desactivado.
Redirect y post logout: http://localhost:4200/*; Web Origin: http://localhost:4200.
Solo existe comodín de rutas dentro de ese origen local según S10b; no de dominios,
puertos, orígenes ni producción.

Direct Access Grants está activado exclusivamente para las pruebas de laboratorio de
S10b, como prescribe su sección 4. PKCE obligatorio y retirada de este atajo pertenecen
a Fase 11/S11; no se ha implementado código Angular de autenticación.

Cinco roles de aplicación creados: ADMINISTRADOR, CAJERO, REPARTIDOR, ABASTECIMIENTO,
CLIENTE. No se crearon SUPERVISOR/COCINA/OPERACIONES/VENDEDOR ni otros roles de negocio.
Keycloak crea automáticamente offline_access, uma_authorization y default-roles-polleria,
además de roles técnicos de sus clientes internos. Son infraestructura nativa, no permisos
comerciales. El converter ignora todos salvo los cinco explícitos.

Usuarios mínimos: administrador, cajero, repartidor, abastecimiento, cliente, uno por
rol. Sus perfiles tienen nombres y correo ficticio reservado @polleria.test, sin
acciones obligatorias ni contraseñas temporales que bloqueen el password grant de prueba.
Contraseñas solo en .env, nunca dentro del JSON importable o informes.
El usuario bootstrap master es administración de Keycloak, no un sexto usuario comercial.

Para otra máquina: completar .env.example en un .env local, iniciar Compose con --wait,
y ejecutar scripts/keycloak-dev-users.py. El import inicial ignora un realm ya existente;
reiniciar no sobrescribe configuración. El script semilla es explícito y restablece las
contraseñas de esos cinco usuarios al valor de .env al repetirlo.

## Resource Server e identidad

Java 21 y Boot 4.0.7 conservados. issuer-uri:
http://localhost:38080/realms/polleria.
Spring utiliza discovery/JWKS de Keycloak para validar firma y los validadores estándar
de emisor y timestamps. No hay clave de firma propia, PasswordEncoder, AuthController,
AuthenticationManager de login local ni driver/datasource PostgreSQL de negocio.

Stateless, sin request cache de sesión, sin Basic/form login/logout local. CSRF
inhabilitado para esta API Bearer sin cookies de autenticación. CORS se aplica antes
de la autorización mediante su fuente única. Petición JWT real comprobada: sin JSESSIONID.

JwtAuthenticationConverter lee realm_access.roles y crea ROLE_ para los cinco roles
permitidos, sin confiar en roles del body, nombre de usuario ni IDs del frontend.
Principal = JWT sub validado, String. Test explícito conserva el UUID como texto.
Spring Security 7 agrega FACTOR_BEARER como autoridad técnica; no es un rol de negocio.

No se añadió keycloakSub a Cliente porque no tiene uso imprescindible en esta fase.
No se migró ni alteró ninguna tabla Oracle. CLIENTE queda sin acceso global a información
privada hasta implementar un vínculo seguro sub ↔ Cliente en la fase correspondiente.
Rol correcto ≠ propiedad correcta. La autorización futura de mis pedidos/perfil propio
y entregas asignadas necesitará validar la relación con sub, no solo hasRole.

## Matriz aplicada centralmente

| Operación | Público | ADMINISTRADOR | CAJERO | CLIENTE / REPARTIDOR / ABASTECIMIENTO |
| --- | --- | --- | --- | --- |
| GET Catálogo colección/id | Sí | Sí | Sí | Sí, por lectura pública |
| Crear/editar/eliminar Catálogo | No | Sí | No | No |
| Crear/consultar/editar Clientes | No | Sí | Sí | No |
| Eliminar Cliente | No | Sí | No | No |
| Consultar/crear Pedidos y PATCH estado | No | Sí | Sí | No |
| Reportes Pedidos | No | Sí | No | No |
| Health mínimo y Swagger/OpenAPI | Sí en dev | Sí | Sí | Sí |
| Otros endpoints Actuator expuestos | No | Sí | No | No |
| Operaciones/rutas no contempladas | No | No | No | No |

Reportes restringidos a ADMINISTRADOR: no se necesita ampliar privilegios para
verificar la operación de caja. Los matchers de reportes preceden los de Pedidos.
No hay @PreAuthorize dispersos; las reglas comerciales siguen en ServiceImpl.
CAJERO autorizado no puede saltarse estados, disponibilidad ni cancelación de pagados.

## Errores, CORS y OpenAPI

Sin token/token inválido: 401 con WWW-Authenticate Bearer.
Token válido sin rol suficiente: 403. No se convierten en 500.
SecurityErrorHandler implementa AuthenticationEntryPoint y AccessDeniedHandler para
los errores previos a MVC; no es otro GlobalExceptionHandler.
Contrato consistente: timestamp, status, error, message, traceId. No expone causas,
claims, token, SQL ni stack trace. CorrelationIdFilter envuelve Security para mantener
X-Trace-ID y MDC incluso en 401/403, comprobado realmente.

CORS único para /api/**, origen http://localhost:4200, sin wildcard ni credenciales;
Authorization, Content-Type y X-Trace-ID permitidos; X-Trace-ID y Location expuestos.
Preflight con Authorization devuelve 200; origen ajeno no recibe permiso. Respuesta
401 real con Origin correcto conserva Access-Control-Allow-Origin y traceId.

OpenAPI y Swagger públicos en desarrollo. Esquema bearerAuth HTTP bearer JWT; no hay
JWT estático. Se comprobó Swagger en navegador: botón Authorize visible, GET Catálogo
sin candado y operaciones protegidas con esquema. Las llamadas protegidas se probaron
con Bearer real mediante HTTP. Health público muestra solo status/grupos, no datos JDBC,
disco, rutas ni componentes. /actuator/info requiere ADMINISTRADOR; no se exponen todos
los endpoints Actuator (continúa exposure health,info).

## Compilación, pruebas y arranque

./mvnw clean compile: **BUILD SUCCESS**, Java 21.
./mvnw test: **BUILD SUCCESS, 93 pruebas, 0 fallos, 0 errores, 0 omitidas**.

Se mantienen 75 pruebas comerciales y se añaden 18 casos de seguridad, incluidos los
casos parametrizados que comprueban permisos por rol. Conteos por suite:

- Catálogo 14; Clientes 14; Pedidos 24; Reportes 19.
- Oracle 1; Modulith 3; Seguridad 18.

Las pruebas de negocio ahora usan @WithMockUser ADMINISTRADOR; los filtros permanecen
activos. Se añadió spring-boot-starter-security-test para la autoconfiguración de Boot 4,
no se anulan protecciones por perfil test. Se preservan rollback, importes históricos,
reportes/filtros y conflictos. Los datos transaccionales de pruebas se revierten.

Cobertura nueva: Catálogo público, escritura anónima 401, modificación administrativa,
403 en los cuatro roles no administradores, Pedidos/reportes sin token, denegación de
IDs arbitrarios a roles no operativos, CAJERO creando Pedido, permisos Clientes,
converter/sub, health mínimo, OpenAPI, CORS y trazabilidad. Token malformado es 401.

Arranque real reiniciado con el classpath Security: Started PolleriaBackendApplication,
Tomcat localhost:8080, Oracle conectado, ddl-auto validate. Token real aceptado y firma
alterada rechazada prueban la integración con Keycloak, no solo existencia del contenedor.

Modulith verify pasa. Los módulos funcionales siguen siendo catalogo/clientes/pedidos.
El metadato técnico preexistente sigue incluyendo exception y filter; no se oculta ese
hecho ni se cambia su estructura. Las clases nuevas de seguridad están en el paquete
raíz, no crean un módulo funcional ni un sexto módulo técnico.

## Pruebas HTTP con cinco tokens reales

Script: scripts/verify-keycloak-http.py --cliente-id 172 --producto-id 10.
Resultado completo sin secretos: keycloak/verification-http.json.

Se obtuvo un JWT nuevo de cada usuario y se inspeccionó en memoria: issuer exacto,
sub String UUID y rol esperado en realm_access.roles. No se usaron decodificadores
externos ni se imprimieron/guardaron tokens.

| Caso real | Resultado |
| --- | --- |
| GET Catálogo sin token, colección/id | 200 |
| Escritura Catálogo; consulta Pedidos/Clientes/reportes sin token | 401 según operación protegida probada |
| Token malformado | 401 |
| Firma de un token real alterada | 401 |
| ADMINISTRADOR crea/edita/elimina Categoría temporal | 201/200/204 |
| ADMINISTRADOR reportes | 200 |
| CAJERO Catálogo lectura/escritura | 200/403 |
| CAJERO crea Pedido | 201, total S/20, estados iniciales PENDIENTE |
| CAJERO cancela su pedido de prueba | 200, CANCELADO |
| CAJERO reportes | 403 |
| CLIENTE Pedidos globales/id ajeno, Clientes, reportes | 403 |
| REPARTIDOR Pedidos globales/id ajeno, Clientes, reportes | 403 |
| ABASTECIMIENTO Pedidos globales/id ajeno, Clientes, reportes | 403 |
| Actuator info anónimo/CAJERO/ADMINISTRADOR | 401/403/200 |
| CORS preflight, X-Trace-ID en 401/403 | Correctos |

No se fabricó un JWT expirado ni se prolongó la prueba esperando su caducidad; la
verificación real cubre ausencia, token malformado y firma inválida. Se usan los
validadores estándar de expiración de Spring, sin mecanismos propios de aceptación.

La categoría temporal se eliminó. El Pedido de prueba **633**, cliente 172/producto 10,
queda cancelado con S/20 y pago PENDIENTE como historial; no hay API DELETE de pedidos.
No se modificó 1/4 Pollo ni los tres pedidos históricos de Fase 9.

## Oracle, PostgreSQL y secretos

Oracle comprobado directamente: POLLERIA mantiene exactamente CATEGORIAS, PRODUCTOS,
CLIENTES, PEDIDOS, DETALLE_PEDIDOS; cero tablas Keycloak. Producto 10: S/20, disponible=1.
Conteo de pedidos final: cuatro, tres anteriores y el nuevo cancelado de prueba.
Sin ALTER/DDL ni migraciones de identidad en Oracle. Su contenedor/volumen siguen los
mismos. Punto-u PostgreSQL y mi_oracle no se tocaron.

PostgreSQL de Keycloak tiene su propio contenedor, volumen y red Compose; se comprobó
que contiene realm polleria y esquema interno. Boot mantiene solo ojdbc11 como driver
comercial, no driver PostgreSQL. Contraseñas no se guardan en Oracle.

Secrets: solo .env local excluido, permisos 0600; .env.example tiene placeholders.
Realm importable sin credenciales; scripts leen valores localmente; informe de resultados
sin tokens. Se revisaron 175 archivos versionables por valores reales de passwords y patrones
JWT completos: cero coincidencias. .env conserva permisos 0600 y está excluido. Git se utiliza solo en lectura, sin staging ni commits.

## Efecto sobre HTTP y Angular

Éxitos y validaciones comerciales conservan contratos. Nuevos 401/403 de seguridad.
PUT/DELETE genéricos inexistentes de Pedido ahora se deniegan antes de MVC: 403 para
usuario autenticado y 401 sin token, en vez del anterior 405. No habilita esos métodos.
Health deja de exponer detalles. OpenAPI documenta seguridad.

Frontend sin modificaciones. Sus lecturas públicas Catálogo y diagnóstico siguen
permitidas; Clientes/Pedidos/escrituras internas reciben 401 mientras no envíen token.
Es el efecto temporal aprobado para Fase 10; no se relaja la API para evitarlo.
No se trabajó en vulnerabilidades npm ni se cambiaron dependencias Angular.

## Adaptaciones y warnings

- Cinco roles POLLERIA, distintos de los de BOMERP. Reportes solo administrativos.
- Catálogo GET público por requisito del producto, a diferencia de la guía que lo autentica.
- Se pospone propiedad sub ↔ Cliente y no se copia vendedorId a Oracle: CLIENTE no puede
  acceder a recursos privados hasta resolverlo, según el alcance explícito del usuario.
- Compose separado e import/semilla mínima para reproducibilidad; sin copiar secretos
  académicos al repositorio. Direct grants conserva únicamente el laboratorio S10b.
- Health restringido a información mínima; resto de Actuator por rol.
- CORS mediante CorsConfigurationSource como exige la integración S10b.
- Manejo de errores previo a MVC compatible con el handler existente.
- Starter Security test específico de Boot 4, sin versiones manuales nuevas.

Warnings no bloqueantes: Keycloak informa modo desarrollo y deprecaciones nativas
identity-brokering-api/twitter-broker y shouldAttachRoute. Springdoc avisa de Swagger
activo en desarrollo. Persisten avisos previos no bloqueantes de infraestructura Java/JPA;
no se hizo limpieza ajena a esta fase. Las 7 vulnerabilidades npm anteriores quedan
intactas y fuera de esta fase.

## Cierre

Sin cambios Git, Angular, Delivery ni Fase 11. No se publicaron contraseñas ni tokens.

Mensaje de commit sugerido:

`feat: proteger backend con Keycloak y permisos por rol`
