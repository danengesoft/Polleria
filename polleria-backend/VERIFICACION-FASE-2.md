# Verificación Fase 2

Ejecución: 24 de octubre de 2026, America/Lima.

## Resultados

- Temurin Java/javac 21.0.12.1; Maven Wrapper 3.9.16 operativo.
- `./mvnw clean compile`: BUILD SUCCESS, release 21.
- `./mvnw test`: 3 pruebas, 0 fallos, 0 errores, 0 omitidas.
- ModularityTests: verificación y generación de documentación satisfactorias.
  No existen módulos comerciales todavía: los límites se comprobarán al añadirlos.
- OracleConnectionTests: contexto Spring, usuario INTICRUNCH y PDB FREEPDB1 correctos.
- Perfil dev: datasource Hikari y EntityManagerFactory iniciados; Tomcat en 127.0.0.1:8080.
- GET /actuator/health: HTTP 200, status UP y componente db UP.
- GET /v3/api-docs: HTTP 200, título INTI CRUNCH API, versión v1, paths vacío.
- GET /swagger-ui.html: redirige a /swagger-ui/index.html, HTTP 200.
- Compose validado con `config --quiet`; contenedor healthy.
- inticrunch-oracle publica 127.0.0.1:1522 hacia 1521.
- Volumen inticrunch-oracle-data montado en /opt/oracle/oradata.
- Usuario INTICRUNCH OPEN; esquema con cero tablas, intencionadamente.
- ddl-auto validate activo; sin entidades no hay tablas comerciales que validar aún.
- Otros contenedores conservan su estado previo. Sin Git, Angular ni fases posteriores.
- La primera consulta HTTP se hizo antes de completar Tomcat y recibió conexión rechazada;
  se repitió después del mensaje Started y todas las comprobaciones pasaron.

## Imagen y avisos

El tag académico gvenzl/oracle-free:23-slim resolvió a:
`gvenzl/oracle-free@sha256:6d61d267a3b978c24c5ac1790e62e927416a0aec446bd86e4b3a1527562757bd`.
SQL reporta Oracle AI Database 26ai Free, Release/Version 23.26.3.0.0.
No se cambió la imagen solicitada; el nombre comercial actual difiere de S01.

- Oracle registra `Unable to obtain current patch information ... 20013` al arrancar.
  Instancia OPEN, healthcheck y conexiones JDBC correctos; dba_registry_sqlpatch
  no contiene filas. El aviso de metadatos de parches permanece sin alterar la imagen.
- Hibernate advierte fetch size predeterminado 10: ajuste de rendimiento pendiente
  de consultas reales, sin cambiar valores arbitrariamente en esta fase.
- Springdoc recuerda que documentación está habilitada: intencional en desarrollo;
  debe revisarse en producción.
- Las pruebas emiten aviso de carga dinámica de Byte Buddy en Java 21.
- DevTools informa referencia opcional oraclepki.jar ausente en manifiesto JDBC;
  la conexión JDBC local funciona; no se ha configurado wallet/TCPS.

## Alcance y siguientes pasos

Sin Categoria mínima: no es necesaria para comprobar infraestructura.
Sin excepciones de S02, endpoints artificiales ni paquetes vacíos.
MapStruct/Lombok configurados y procesadores resueltos en compilación;
no se ha probado conversión de negocio porque no existen entidades/mappers.

Antes de Fase 3: aprobación del usuario, SQL explícito de Catálogo y pruebas de
alineación de esquema/entidades. No usar ddl-auto update/create.

Logs locales ignorados por Git: build-compile.log, build-test.log,
backend-dev.log y oracle-start.log. .env tiene permisos 0600 y está ignorado.
