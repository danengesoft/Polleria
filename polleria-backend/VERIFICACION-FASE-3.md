# Verificación de migración y Fase 3 — POLLERIA

24 de octubre de 2026, America/Lima.

## Migración previa a Catálogo

Se inspeccionaron todos los archivos fuente/configuración/documentación y se localizaron
las referencias de nombre de proyecto antes de cambiarlas. Se migraron carpeta, artifact,
application.name, paquetes Java, clase principal, bean/título OpenAPI, Compose, esquema
y expectativas de conexión de las pruebas. Credenciales de desarrollo preservadas.

Los labels de Docker identificaron exclusivamente recursos del proyecto Fase 2.
El esquema antiguo no tenía tablas ni otros objetos comerciales. Por autorización del
usuario se detuvieron/eliminaron solo el contenedor, volumen y red propios antiguos,
y se crearon polleria-oracle / polleria-oracle-data / polleria-dev_default.
No se modificaron mi_oracle ni punto-u-dev-postgres-1 ni sus volúmenes.
VERIFICACION-FASE-2.md y los logs antiguos conservan referencias históricas.

Antes de añadir Catálogo:
- Java/javac Temurin 21.0.12.1, Maven Wrapper 3.9.16.
- clean compile: BUILD SUCCESS.
- test: 3 pruebas satisfactorias, incluidas modularidad y conexión POLLERIA/FREEPDB1.
- Arranque real correcto; Actuator, OpenAPI (POLLERIA API) y Swagger HTTP 200.
- Fuente/configuración activa sin referencias al nombre anterior.

## Implementación

Catálogo sigue módulo → recurso → capa. Categoria y Producto tienen entity,
repository, service/ServiceImpl, mapper MapStruct, DTO Request/Response y controller.
CategoriaResumen evita exponer entidades. Producto resuelve categoría en su servicio,
no en mapper; relación LAZY unidireccional con EntityGraph en consultas.

DDL explícito en scripts/01-catalogo.sql, sin Flyway/Liquibase y sin cambiar validate.
Tablas CATEGORIAS y PRODUCTOS, PK identity NUMBER(19,0), FK CATEGORIA_ID obligatoria,
checks de nombre, precio y booleanos; índice de FK e índice único de nombre de categoría.
Precio BigDecimal / NUMBER(10,2), positivo y máximo dos decimales en el contrato HTTP.
No hay stock ni otros módulos funcionales. No hay NamedInterface anticipadas.

GlobalExceptionHandler: 400, 404, 409 y 500 saneados, sin SQL/stack trace en HTTP.
X-Trace-ID/MDC con formato de logs S02; se validan longitud y caracteres del header.
Desactivar categoría no elimina productos. No se inventa una regla para rechazar productos
asociados a categorías inactivas; su disponibilidad efectiva para compra se definirá al
implementar ese caso de uso. Los endpoints actuales son administrativos sin Keycloak.

## Diagnósticos y correcciones acotadas

1. Oracle rechazó REGEXP_REPLACE en índice funcional con ORA-01743. Se sustituyó por
   UPPER(TRIM(NOMBRE)), manteniendo normalización de espacios interiores en Service.
   Se continuó el DDL desde el índice fallido sin borrar/recrear la tabla ya creada.
   Un cliente SQL directo debe respetar la normalización interior documentada.
2. Una prueba de eliminación compartía el contexto JPA de la creación y recibió
   TransientPropertyValueException antes de llegar a la FK. Se limpió el contexto de
   esa prueba para simular una petición independiente; no se ocultó el fallo mediante
   cambios del código productivo. Prueba específica y suite completa repetidas con éxito.
3. El lector del primer flujo HTTP esperaba application/json y no interpretó el tipo
   vendor JSON de Actuator. Se corrigió únicamente el lector y se repitió el flujo completo.

## Verificación final

- ./mvnw clean compile: BUILD SUCCESS, 24 fuentes, release 21.
- ./mvnw test: 17 pruebas, 0 fallos, 0 errores, 0 omitidas.
  CatalogoIntegrationTests: 14; ModularityTests: 2; OracleConnectionTests: 1.
- Pruebas con Oracle real, incluyendo validación, duplicados, relación, actualización,
  filtro por categoría, eliminación y conflicto, errores de formato y trace ID.
- Spring Boot arrancó realmente, perfil dev, JPA y Hikari inicializados, sin ERROR.
- Hibernate informa jdbc:oracle:thin:@localhost:1522/FREEPDB1, esquema POLLERIA.
- Actuator HTTP 200: aplicación y db UP.
- OpenAPI HTTP 200: POLLERIA API, contratos de categorías y productos presentes.
- Swagger UI HTTP 200.
- Compose config --quiet correcto; polleria-oracle healthy y puerto 127.0.0.1:1522.

Flujo HTTP real comprobado:
- Crear/consultar categoría (201/200), crear/consultar producto asociado (201/200).
- Actualizar precio a 20.00 (200), comprobar CategoriaResumen y filtro (200).
- CategoriaId inexistente (404), precio cero/exceso de decimales y URL inválida (400).
- Nombre duplicado con mayúsculas/espacios (409).
- DELETE de categoría utilizada (409), categoría permanece consultable (200).
- Desactivar/reactivar categoría con PUT (200).
- Crear categoría/producto temporales, eliminar producto y categoría (204), consultas 404.
- X-Trace-ID fase3-http conservado; errores sin detalles SQL/Oracle.

Consulta SQL posterior confirma:
- 1 categoría: Pollos, ID 24; 1 producto: 1/4 Pollo, ID 10, precio 20.00, CATEGORIA_ID 24.
- FK_PRODUCTOS_CATEGORIA: PRODUCTOS.CATEGORIA_ID → PK_CATEGORIAS.
- Claves/checks ENABLED, precio NUMBER(10,2), categoría NOT NULL.
- Índice único UQ_CATEGORIAS_NOMBRE: UPPER(TRIM(NOMBRE)).
- No hay tablas de otros módulos.

Git no inicializado en host, sin commit/push. Backend y Oracle permanecen ejecutándose.
No se empezó Clientes, Angular, Pedidos ni otra fase.

## Avisos pendientes

Se conservan los avisos documentados en Fase 2: fetch size 10, endpoints Springdoc
habilitados en desarrollo, Byte Buddy/Mockito y referencia opcional oraclepki.jar.
La imagen académica 23-slim continúa reportando Oracle 26ai Free 23.26.3.0.0.
El conflicto FK deliberadamente probado genera ORA-02292 en logs internos; HTTP
responde 409 saneado. No es un fallo de arranque ni una prueba fallida.

## Archivos y revisión

Renombrados/actualizados: carpeta backend; paquetes/clase principal; pom.xml;
compose-dev.yml; application.yml/application-dev.yml; OpenApiConfig;
ModularityTests; OracleConnectionTests; README.md.

Nuevos: catalogo/categoria y catalogo/producto (8 clases/interfaces por recurso),
CategoriaResumen, dos excepciones, GlobalExceptionHandler, exception/package-info.java,
CorrelationIdFilter, logback-spring.xml, scripts/01-catalogo.sql,
CatalogoIntegrationTests y este informe. .env, .env.example, Wrapper y .gitignore
se conservaron. No se expusieron secretos.

Commit sugerido (no ejecutado):
`feat: renombrar proyecto a POLLERIA e implementar catálogo con Oracle`
