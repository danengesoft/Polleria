# POLLERIA — backend, Fase 3 (Catálogo)

Monolito modular LP2. Java 21, Spring Boot 4.0.7, Modulith 2.0.7,
MapStruct 1.6.3, Springdoc 3.0.2 y Maven Wrapper 3.9.16.

## Desarrollo en Fedora / Bash

Desde `/home/systemseng/Polleria/polleria-backend`:

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-temurin-jdk
export PATH="$JAVA_HOME/bin:$PATH"
java -version
javac -version
./mvnw -version
```

`.env` contiene secretos locales, tiene permisos 0600 y está excluido por `.gitignore`.
Para otra instalación, copiar `.env.example` a `.env` y reemplazar los placeholders.
No publicar `.env` ni salidas de Compose que muestren las contraseñas.

```bash
docker compose -f compose-dev.yml config --quiet
docker compose -f compose-dev.yml up -d --wait oracle
```

**Solo en una base nueva y sin tablas de Catálogo**, aplicar el DDL una vez:

```bash
docker exec -i polleria-oracle sqlplus -s / as sysdba < scripts/01-catalogo.sql
```

El script falla ante errores y no elimina/reemplaza tablas existentes. Oracle hace
commit implícito del DDL: no volver a ejecutar desde el inicio sobre un esquema parcial.
La instalación de desarrollo actual ya tiene las tablas; no repetir el script.

```bash
set -a
source .env
set +a
./mvnw clean compile
./mvnw test
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Esperar el mensaje `Started PolleriaBackendApplication` antes de consultar la API.
Las pruebas necesitan Oracle y el esquema aplicado; los datos de las pruebas MockMvc
se revierten al finalizar cada caso. No utilizan H2 ni sustituyen Oracle.
Cambiar `.env` no modifica contraseñas de un volumen ya inicializado.

## Infraestructura

- Oracle: `localhost:1522/FREEPDB1`, usuario de aplicación `POLLERIA`.
- Contenedor: `polleria-oracle`; volumen: `polleria-oracle-data`.
- API: `http://127.0.0.1:8080`.
- Salud: `/actuator/health`, incluye JDBC.
- OpenAPI: `/v3/api-docs`; Swagger: `/swagger-ui.html`.
- `ddl-auto: validate` y `open-in-view: false`.
- Endpoints sin autenticación únicamente durante desarrollo; Keycloak es una fase posterior.

## Catálogo (S02/S03)

`catalogo/{categoria,producto}/{controller,dto,entity,mapper,repository,service}`.
El servicio resuelve la categoría y aplica reglas; el mapper solo transforma.
Las respuestas no exponen entidades. Producto → Categoria es LAZY y unidireccional;
las consultas de producto usan EntityGraph para traer la categoría sin N+1.

Ambos recursos ofrecen GET colección, GET /{id}, POST, PUT /{id}, DELETE /{id}:
`/api/v1/categorias` y `/api/v1/productos`.
Productos admite `?categoriaId=...`; una categoría inexistente devuelve 404.
POST devuelve 201 con Location, PUT/GET 200 y DELETE 204.

CategoriaRequest:

```json
{"nombre":"Pollos","descripcion":"Pollo a la brasa","activo":true}
```

ProductoRequest (descripcion e imagenUrl son opcionales):

```json
{"nombre":"1/4 Pollo","precio":18.50,"disponible":true,"categoriaId":1}
```

- Nombre de categoría: máximo 80; se normalizan espacios y se ignoran mayúsculas
  al buscar duplicados. Índice único Oracle UPPER(TRIM(NOMBRE)) protege concurrencia.
  La normalización de espacios interiores se hace en el servicio; clientes SQL directos
  deben respetarla también.
- Nombre de producto: máximo 120; descripción: máximo 500.
- Precio BigDecimal/NUMBER(10,2), mayor que cero, hasta 8 enteros y 2 decimales.
- `activo` y `disponible` son obligatorios y representan conceptos independientes.
  Desactivar una categoría no elimina ni cambia automáticamente sus productos.
  Esta API administrativa muestra también recursos inactivos/no disponibles.
- imagenUrl opcional, máximo 1000, HTTP/HTTPS; no se descargan ni almacenan archivos.
- No existe stock. No hay unicidad de nombres de producto sin una regla solicitada.
- Categoría utilizada: DELETE devuelve 409; puede desactivarse con PUT.
- Producto: eliminación física en esta fase, sin lógica de Pedidos.
- 400 entrada inválida; 404 recurso/referencia inexistente; 409 conflicto/integridad;
  500 error inesperado sin SQL ni stack trace en HTTP.
- X-Trace-ID (máximo 64 caracteres seguros) se refleja en respuesta y MDC; si no es
  válido se genera UUID. Logs diarios en logs/polleria.log (retención de 7 días).

Modulith verifica Catálogo y la infraestructura técnica. No se exponen NamedInterface
anticipadamente: aún no hay un segundo módulo que necesite contratos de Catálogo.
Las excepciones compartidas usan el paquete técnico abierto, según el patrón académico.

Security, Angular, otros módulos comerciales y reportes quedan fuera de esta fase.
No se inicializa Git. El informe de Fase 2 conserva los nombres históricos originales.
