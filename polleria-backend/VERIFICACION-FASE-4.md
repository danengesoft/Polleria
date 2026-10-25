# Fase 4 — Clientes

24 de octubre de 2026, America/Lima. Ruta: /home/systemseng/Polleria/polleria-backend.

## Implementación y alcance

Segundo módulo funcional `clientes`, independiente de `catalogo`.
Un único recurso Cliente con id, nombres, apellidos, telefono, correo,
direccionPrincipal, referencia y activo. Sin identidad/autenticación ni otros módulos.

Patrón S02: Entity, Repository, Service/ServiceImpl, Mapper, DTO Request/Response,
Controller; transacciones según S03. MapStruct 1.6.3 genera mapeo efectivo.
Se reutilizan GlobalExceptionHandler y CorrelationIdFilter sin modificarlos.

Teléfono: 7–9 dígitos, con +51 opcional; String conserva prefijos y ceros,
sin unicidad. Correo opcional normalizado a minúsculas, espacios exteriores retirados,
blanco convertido a null. Request retira espacios de correo antes de @Email; el servicio
realiza normalización comercial, duplicidad y persistencia. Activo inicial true,
null explícito inválido. Dirección/referencia opcionales, sin entidad Direccion.

SQL 02-clientes.sql ejecutado solo sobre polleria-oracle / FREEPDB1 / POLLERIA.
CLIENTES tiene PK identity NUMBER(19,0), tamaños alineados con JPA, campos obligatorios
NOT NULL, activo NUMBER(1,0) DEFAULT 1 y checks de nombres, teléfono y booleano.
Índice único UQ_CLIENTES_CORREO: LOWER(TRIM(CORREO)); permite varios NULL.
No se modificó ddl-auto validate ni tablas de Catálogo.

Eliminación física solo en esta fase; revisar cuando Pedido referencie Cliente en Fase 5.

## Verificaciones

- Java y javac Temurin 21.0.12.1.
- ./mvnw clean compile: BUILD SUCCESS; 32 fuentes, release 21.
- ./mvnw test: 32 pruebas, 0 fallos, 0 errores, 0 omitidas.
  - Catálogo: 14 (regresión sin cambios).
  - Clientes: 14.
  - Modulith: 3 (incluye reconocimiento explícito de catalogo y clientes).
  - Oracle: 1.
- Clientes prueba CRUD, normalización, opcionales, errores de campos, duplicados en
  creación/actualización, correo propio, desactivación, teléfono compartido/ceros/prefijo,
  cliente inexistente y restricción real de unicidad mediante SQL directo.
- Los datos automatizados se revierten por transacción de prueba.
- Arranque real correcto: ApplicationContext, Hikari y EntityManagerFactory.
- JDBC localhost:1522/FREEPDB1, esquema POLLERIA.
- Actuator HTTP 200, status/db UP.
- OpenAPI HTTP 200, título POLLERIA API y rutas Clientes/Catálogo presentes.
- Swagger UI HTTP 200.

## Flujo HTTP y SQL

- Creación de cliente 15 con correo mixto y espacios: 201, nombre/apellidos normalizados,
  correo ana.fase4@example.com, activo true por defecto.
- Consulta 200, actualización 200 con dirección Calle Prueba 123 y activo false.
- Cliente 16 sin correo/dirección: 201; teléfono 01234567 conservado.
- Correo duplicado con mayúsculas y espacios: 409.
- Correo, teléfono, nombres y apellidos inválidos: 400.
- ID inexistente: 404.
- Respuestas con X-Trace-ID fase4-clientes-http; errores sin SQL/stack trace/Oracle.
- Consulta SQL directa confirmó ambas filas, normalización y actualización persistida.
- DELETE 15/16: 204; consultas posteriores: 404.
- Consulta SQL posterior confirmó CLIENTES con 0 filas, sin clientes ficticios sobrantes.
- Catálogo conservado: Pollos y 1/4 Pollo (S/20.00), 1 categoría y 1 producto.
- Oracle verificó columnas, NOT NULL, DEFAULT, checks, PK e índice funcional.
  Únicas tablas comerciales: CATEGORIAS, PRODUCTOS, CLIENTES.

## Cambios y límites

Creados: 8 clases/interfaces en clientes/cliente (entity, 2 DTO, mapper,
repository, Service, ServiceImpl, controller), scripts/02-clientes.sql,
ClientesIntegrationTests.java y este informe.
Modificados: ModularityTests.java y README.md.
No cambios en Catálogo, pom, Compose, perfiles, handler o filtro.

Sin cambios de arquitectura ni dependencias. Validaciones de correo, teléfono,
campos opcionales/activo y duplicidad adaptan S02 al dominio solicitado.
No se crearon búsquedas/reportes ni @NamedInterface sin consumidor real.

Avisos no bloqueantes heredados de Fase 2 permanecen documentados allí.
Las pruebas deliberadas de integridad generan ORA-00001 (correo) y ORA-02292
(Catálogo) en logs internos, con resultados esperados; no se exponen por HTTP.
No se cambiaron configuraciones para ocultar warnings.

Solo se ejecutaron comandos Git de lectura; el usuario administra Git/GitHub.
Backend y Oracle permanecen ejecutándose. No se comenzó Fase 5.

Commit sugerido (no ejecutado):
`feat: implementar módulo Clientes con validaciones y unicidad de correo en Oracle`
