# Guía de desarrollo – Pollería Neyos

Guía práctica para encontrar rápido dónde está cada cosa y cómo hacer cambios en el
**frontend** (Angular) y en el **backend** (Spring Boot).

## Índice

1. [Mapa rápido: "quiero cambiar…"](#1-mapa-rápido-quiero-cambiar)
2. [Cómo levantar el sistema y ver los cambios](#2-cómo-levantar-el-sistema-y-ver-los-cambios)
3. [Frontend](#3-frontend-polleria-frontend)
4. [Backend](#4-backend-polleria-backend)
5. [Ejemplo completo: agregar un campo nuevo](#5-ejemplo-completo-agregar-un-campo-nuevo-de-punta-a-punta)
6. [Base de datos](#6-base-de-datos)
7. [Comandos útiles](#7-comandos-útiles)
8. [Resumen para la evaluación](#8-resumen-para-explicar-el-proyecto-en-la-evaluación)

---

## 1. Mapa rápido: "quiero cambiar…"

| Quiero cambiar… | Archivo |
|---|---|
| Colores de la **web pública** (inicio, carta, carrito, checkout) | `polleria-frontend/src/app/core/layout/publico/cliente-base.css` (variables al inicio, dentro de `.cliente-premium`) |
| Colores del **portal interno** (caja, mesero, operaciones, reportes) | `polleria-frontend/src/styles.css`, línea `:root[data-tema="oscuro"]` (casi al final del archivo) |
| Colores de los componentes de Angular Material (botones, inputs) | `polleria-frontend/src/material-theme.scss` (toma los colores de las variables anteriores) |
| **Tipo de letra** de la web pública | `cliente-base.css` → `font-family` de `.cliente-premium` (texto) y de `h1, h2, h3` (títulos) |
| **Tipo de letra** del portal interno | `polleria-frontend/src/styles.css` → `font-family` dentro de `:root` (al inicio) |
| **Tamaño** de los títulos del portal interno | `styles.css` → reglas `h1`, `h2`, `h3` |
| Título grande de la portada ("EL SABOR IMPONE PRESENCIA") | `polleria-frontend/src/app/core/inicio/inicio-publico.html` |
| Imagen principal de la portada | `polleria-frontend/public/images/la-brava/inicio-hero.png` |
| Estilos de la portada | `core/inicio/inicio-hero.css`, `inicio-carta.css`, `inicio-carrusel.css`, `inicio-responsive.css`, `inicio-publico.css` |
| Logo/nombre en la cabecera pública ("Polleria Neyos") | `core/layout/publico/cliente-shell.html` (clase `client-brand`) |
| Nombre en la cabecera del portal interno | `core/layout/interno/interno.html` (clase `brand`) |
| Menú lateral del portal interno | `core/layout/interno/interno.html` |
| Menú de navegación general | `shared/navigation/app-navigation.ts` |
| Texto de la pestaña del navegador | `polleria-frontend/src/index.html` (`<title>`) y `title:` de cada ruta en `src/app/app.routes.ts` |
| Ícono de la pestaña | `polleria-frontend/public/favicon.ico` |
| Nombre visible de estados y tipos de pedido ("En preparación", etc.) | `features/pedidos/pedido.model.ts` → función `etiqueta()` |
| Mensajes de error genéricos ("No se pudo conectar…") | `shared/http-error.ts` |
| Mensajes de validación de formularios | `shared/form-validation.ts` |
| Pantallas de Caja, Mesero y Operaciones | `features/operacion/paneles.ts` y `features/operacion/caja.html` |
| Pantalla de reportes | `features/reportes/reportes.html`, `reportes.ts`, `reportes.css`, `reportes-premium.css` |
| Banners, promociones, destacados y ubicación (pantalla del administrador) | `features/contenido/banners.html` y los `.ts` de esa carpeta |
| Qué rol puede entrar a qué página (frontend) | `src/app/app.routes.ts` → `rolGuard(...)` |
| Qué rol puede usar qué API (backend) | `polleria-backend/src/main/java/pe/edu/upeu/polleria/SecurityConfig.java` |
| Reglas de negocio (validar, calcular, cambiar estados) | `…/service/*ServiceImpl.java` del módulo correspondiente |
| Validaciones de datos que llegan a la API | `…/dto/*Request.java` (anotaciones `@NotBlank`, `@Size`, etc.) |
| Mensajes de error que devuelve la API | Los `throw new …Exception("mensaje")` en los servicios y `exception/GlobalExceptionHandler.java` |
| Tablas y columnas de la base de datos | `polleria-backend/scripts/*.sql` + `…/entity/*.java` |

> **Truco para encontrar cualquier texto visible:** copia la frase que ves en pantalla y búscala
> en todo el proyecto. En VS Code: `Ctrl + Shift + F`. En la terminal:
>
> ```bash
> grep -rn "Texto que ves en pantalla" polleria-frontend/src
> ```

---

## 2. Cómo levantar el sistema y ver los cambios

Todo se ejecuta desde la carpeta `~/Polleria`.

**Levantar todo (Oracle, Keycloak, backend, frontend):**

```bash
docker compose --env-file .env.prodtest -f compose.prod.yml -f compose.prodtest.yml up -d --build
```

**Activar la recarga automática al guardar** (VS Code también lo hace solo al abrir la carpeta):

```bash
bash scripts/desarrollo.sh
```

Abrir en el navegador: **http://127.0.0.1:18080**

| Si cambias… | Qué pasa |
|---|---|
| Un archivo de `polleria-frontend/src` (`.html`, `.css`, `.ts`) | El navegador se actualiza solo en segundos. Si hay un error de compilación, aparece en la página. |
| Un archivo de `polleria-backend/src/main` o `pom.xml` | Docker reconstruye el backend automáticamente (tarda 1–2 minutos). |
| Un script SQL | **No** se aplica solo: hay que ejecutarlo a mano (ver sección 6). |

**Apagar todo** (nunca agregues `-v`, borra las bases de datos):

```bash
docker compose --env-file .env.prodtest -f compose.prod.yml -f compose.prodtest.yml down
```

---

## 3. Frontend (`polleria-frontend/`)

Angular 22 con componentes *standalone*, *signals* y Angular Material.

### 3.1 Estructura de carpetas

```
polleria-frontend/
├── public/                    → archivos estáticos (favicon, imágenes)
│   └── images/la-brava/       → imagen principal de la portada
└── src/
    ├── index.html             → título de la pestaña, HTML base
    ├── styles.css             → estilos GLOBALES y colores del portal interno
    ├── material-theme.scss    → tema de Angular Material
    ├── environments/          → URL de la API y de Keycloak
    └── app/
        ├── app.routes.ts      → TODAS las rutas (URL → pantalla) y permisos por rol
        ├── app.config.ts      → configuración general (interceptores, router)
        ├── core/              → piezas base de la aplicación
        │   ├── auth/          → login con Keycloak, roles, token
        │   ├── guards/        → protegen rutas según sesión y rol
        │   ├── interceptors/  → agregan el token a cada petición
        │   ├── inicio/        → portada pública y portada interna
        │   ├── layout/
        │   │   ├── publico/   → cabecera, menú y estilos de la WEB PÚBLICA
        │   │   └── interno/   → cabecera y menú lateral del PORTAL INTERNO
        │   ├── services/      → API base, eventos en tiempo real, conexión
        │   └── theme/         → tema (siempre oscuro)
        ├── features/          → una carpeta por funcionalidad
        │   ├── catalogo/      → categorías y productos (portal interno)
        │   ├── clientes/      → clientes (portal interno)
        │   ├── contenido/     → banners, promociones, destacados, ubicación
        │   ├── operacion/     → paneles de Caja, Mesero y Operaciones (cocina)
        │   ├── pedidos/       → lista, formulario y detalle de pedidos
        │   ├── publico/       → carta, carrito, checkout, perfil, mis pedidos
        │   └── reportes/      → reportes del administrador
        └── shared/            → piezas reutilizables (badges, errores, navegación)
```

### 3.2 Cómo está hecho cada componente

Cada pantalla es un **componente**, que puede estar repartido en hasta tres archivos:

| Archivo | Contiene |
|---|---|
| `nombre.ts` | La lógica: datos, botones, llamadas a la API |
| `nombre.html` | Lo que se ve (textos, botones, tablas) |
| `nombre.css` | El diseño de esa pantalla |

**Importante:** algunos componentes tienen el HTML y el CSS **dentro del `.ts`** en lugar de
archivos separados. Lo reconoces porque en el `.ts` aparece `template: \`...\`` o `styles: \`...\``
en vez de `templateUrl:` / `styleUrl:`. Pasa, por ejemplo, en:

- `features/operacion/paneles.ts` (Operaciones y Mesero)
- `features/contenido/ubicacion.ts`, `promociones.ts`, `destacados.ts`
- `features/publico/mis-pedidos.ts`, `producto-detalle.ts`, `acceso.ts`
- `shared/registro-detalle.ts`, `shared/seguimiento-pedido.ts`

Para cambiar un texto de esas pantallas, edítalo directamente dentro del `.ts`.

Para saber qué archivos de estilo usa un componente, mira su `.ts`:

```ts
@Component({
  selector: 'app-checkout',
  templateUrl: './checkout.html',                       // ← su HTML
  styleUrls: ['./publico.css', './checkout-premium.css', // ← sus CSS
              './checkout-mobile.css'],
})
```

### 3.3 Cómo saber qué archivo corresponde a una página

1. Mira la URL en el navegador, por ejemplo `/interno/productos`.
2. Busca esa ruta en `src/app/app.routes.ts`:

   ```ts
   { path: 'productos',
     loadComponent: () => import('./features/catalogo/producto/producto-list')... }
   ```

3. El `import(...)` te dice la carpeta y el archivo: `features/catalogo/producto/producto-list.ts`
   (y su `producto-list.html`).

| URL | Archivo principal |
|---|---|
| `/inicio`, `/destacados`, `/promociones` | `core/inicio/inicio-publico.*` |
| `/catalogo` | `features/publico/catalogo-publico.*` |
| `/carrito` | `features/publico/carrito.*` |
| `/checkout` | `features/publico/checkout.*` |
| `/perfil` | `features/publico/perfil.*` |
| `/mis-pedidos`, `/mis-pedidos/:id`, `/seguimiento/:id` | `features/publico/mis-pedidos.ts`, `mi-pedido.*` |
| `/acceso` | `features/publico/acceso.ts` |
| `/interno` | `core/inicio/inicio-interno.ts` |
| `/interno/portada` | `features/contenido/banners.*` |
| `/interno/caja` | `features/operacion/paneles.ts` (clase `Caja`) + `caja.html` |
| `/interno/operaciones` | `features/operacion/paneles.ts` (clase `Operaciones`) |
| `/interno/mesero` | `features/operacion/paneles.ts` (clase `Mesero`) |
| `/interno/reportes` | `features/reportes/reportes.*` |
| `/interno/categorias` | `features/catalogo/categoria/categoria-list.*` / `categoria-form.*` |
| `/interno/productos` | `features/catalogo/producto/producto-list.*` / `producto-form.*` |
| `/interno/clientes` | `features/clientes/cliente/cliente-list.*` / `cliente-form.*` |
| `/interno/pedidos` | `features/pedidos/pedido-list.*`, `pedido-form.*`, `pedido-detalle.*` |

### 3.4 Cambiar colores

El proyecto usa **variables CSS**: cambias el color en un solo lugar y se actualiza en toda la
aplicación.

**Web pública** — `core/layout/publico/cliente-base.css`, al inicio:

```css
.cliente-premium {
  --background: #0c0c0b;   /* fondo */
  --surface: #151512;      /* fondo de tarjetas */
  --ink: #f5f0e6;          /* color del texto */
  --muted: #c0b8aa;        /* texto secundario */
  --accent: #b31f25;       /* color principal (botones, rojo) */
  --accent-hover: #d22b31; /* botón al pasar el mouse */
  --gold: #c8a56b;         /* dorado decorativo */
  ...
}
```

**Portal interno** — `src/styles.css`. La aplicación **siempre usa el tema oscuro**
(lo fuerza `core/theme/selector-tema.ts`), así que los colores que se ven son los de esta línea,
casi al final del archivo:

```css
:root[data-tema="oscuro"]{ --background:#141619; --surface:#202328; --ink:#f2f3f5;
  --accent:#ef737a; --accent-hover:#ff9096; ... }
```

> Los valores de `:root { ... }` al inicio de `styles.css` son del tema claro y hoy no se ven.

| Variable | Para qué se usa |
|---|---|
| `--background` | Fondo de la página |
| `--surface`, `--surface-soft` | Fondo de tarjetas, tablas, formularios |
| `--ink` | Texto principal |
| `--muted` | Texto secundario / descripciones |
| `--line` | Bordes |
| `--accent`, `--accent-hover`, `--accent-soft` | Color de marca: botones, enlaces, resaltados |
| `--success`, `--warning`, `--error`, `--info` (+ `-soft`) | Estados y mensajes |

### 3.5 Cambiar letras (tipografía y tamaños)

| Dónde | Archivo | Qué buscar |
|---|---|---|
| Texto de la web pública | `core/layout/publico/cliente-base.css` | `font-family: Arial, Helvetica, sans-serif;` en `.cliente-premium` |
| Títulos de la web pública | `core/layout/publico/cliente-base.css` | `font-family: Georgia, ...` en `.cliente-premium h1, h2, h3` |
| Todo el portal interno | `src/styles.css` | `font-family: system-ui, ...` dentro de `:root` |
| Tamaño de títulos del portal | `src/styles.css` | reglas `h1 { font-size: ... }`, `h2`, `h3` |
| Componentes de Material | `src/material-theme.scss` | `typography: system-ui` |

Otros archivos que también fijan su propia letra (búscalos si un texto no cambia):
`features/publico/producto-detalle.css`, `publico.css`, `acceso.css`,
`core/inicio/inicio-carta.css`, `cliente-controles.css`, `cliente-tarjetas.css`.

```bash
grep -rn "font-family" polleria-frontend/src
```

**Para usar una fuente de Google Fonts** (por ejemplo, *Poppins*):

1. En `src/index.html`, dentro de `<head>`, agrega:

   ```html
   <link rel="preconnect" href="https://fonts.googleapis.com">
   <link href="https://fonts.googleapis.com/css2?family=Poppins:wght@400;600;700&display=swap" rel="stylesheet">
   ```

2. Úsala en el CSS: `font-family: 'Poppins', Arial, sans-serif;`

### 3.6 Cambiar textos

- **Textos de una pantalla:** en su `.html`, o dentro del `template:` de su `.ts`.
- **Nombre de la marca:** el proyecto usa **dos nombres**: "Polleria Neyos" y "LA BRAVA". Para
  unificarlo, búscalos en todo el frontend:

  ```bash
  grep -rn "LA BRAVA\|La Brava\|Neyos" polleria-frontend/src
  ```

- **Título de la pestaña:** `src/index.html` y los `title:` de `app.routes.ts`.
- **Cómo se muestran los estados** (`PENDIENTE` → "Pendiente"): función `etiqueta()` en
  `features/pedidos/pedido.model.ts`.

### 3.7 Cómo habla el frontend con el backend

Cada funcionalidad tiene un **servicio** (`*-service.ts`) que llama a la API:

```ts
// features/catalogo/categoria/categoria-service.ts
private readonly resource = '/api/v1/categorias';
listar()               { return this.http.get<Categoria[]>(this.api.buildUrl(this.resource)); }
crear(data)            { return this.http.post<Categoria>(this.api.buildUrl(this.resource), data); }
actualizar(id, data)   { return this.http.put<Categoria>(...`/${id}`, data); }
eliminar(id)           { return this.http.delete<void>(...`/${id}`); }
```

y un **modelo** (`*.model.ts`) con la forma de los datos, que debe coincidir con los DTO del
backend:

```ts
// features/catalogo/categoria/categoria.model.ts
export interface CategoriaRequest { nombre: string; descripcion: string | null; activo: boolean; }
export interface Categoria extends CategoriaRequest { id: number; eliminable?: boolean; }
```

El token de Keycloak se agrega solo a cada petición: `core/interceptors/auth-interceptor.ts`.

### 3.8 Roles y permisos en el frontend

Roles definidos en `core/auth/auth-service.ts`:

| Rol | Acceso |
|---|---|
| `ADMINISTRADOR` | Todo el portal interno |
| `CAJERO` | Caja, pedidos, clientes, consulta de catálogo |
| `OPERACIONES` | Panel de cocina |
| `MESERO` | Mesas y entrega de pedidos |
| `CLIENTE` | Web pública: perfil y "mis pedidos" |

- Para proteger una página, en `app.routes.ts`: `canActivate: [rolGuard('ADMINISTRADOR')]`.
- Para mostrar u ocultar un botón: `@if (auth.tieneRol('ADMINISTRADOR')) { ... }`.

> El frontend solo **oculta**; la seguridad real la aplica el backend (`SecurityConfig.java`).

---

## 4. Backend (`polleria-backend/`)

Spring Boot 4 + Java 21, base de datos Oracle, seguridad con Keycloak (JWT), MapStruct, Lombok
y Spring Modulith (módulos separados por paquete).

### 4.1 Estructura de carpetas

```
polleria-backend/
├── pom.xml                         → dependencias (Maven)
├── Dockerfile
├── keycloak/polleria-realm.json    → configuración de Keycloak (roles, cliente)
├── scripts/*.sql                   → creación y cambios de tablas
└── src/main/
    ├── resources/
    │   ├── application.yml         → configuración común
    │   ├── application-dev.yml     → desarrollo fuera de Docker
    │   ├── application-prod.yml    → Docker / producción
    │   └── logback-spring.xml      → logs
    └── java/pe/edu/upeu/polleria/
        ├── PolleriaBackendApplication.java  → punto de inicio
        ├── SecurityConfig.java              → QUÉ ROL PUEDE USAR CADA URL
        ├── CorsConfig.java                  → orígenes permitidos
        ├── SecurityErrorHandler.java        → respuestas 401/403
        ├── exception/                       → errores y manejador global
        ├── filter/                          → ID de correlación en los logs
        ├── catalogo/       → categoria/, producto/ (+ presentaciones, imágenes)
        ├── clientes/       → cliente/ (CRUD y "mi perfil")
        ├── contenido/      → banner/, promocion/, destacados/, ubicacion/
        └── pedidos/        → pedido/, mesa/, web/ (compra pública), eventos/ (tiempo real)
```

### 4.2 Las capas de cada módulo

Todos los módulos siguen el mismo patrón. Ejemplo con `catalogo/categoria/`:

```
Petición HTTP
   │
   ▼
controller/CategoriaController.java   → recibe la URL, valida (@Valid), responde
   │
   ▼
service/CategoriaServiceImpl.java     → REGLAS DE NEGOCIO
   │            ▲
   │            └── mapper/CategoriaMapper.java  → convierte DTO ⇄ Entidad (MapStruct)
   ▼
repository/CategoriaRepository.java   → consultas a la base (Spring Data JPA)
   │
   ▼
entity/Categoria.java                 → la tabla CATEGORIAS de Oracle
```

| Carpeta | Qué contiene | Cuándo la tocas |
|---|---|---|
| `controller/` | Endpoints (`@GetMapping`, `@PostMapping`…) | Agregar o cambiar una URL de la API |
| `dto/` | `*Request` (lo que llega) y `*Response` (lo que se devuelve) | Agregar o quitar campos de la API, cambiar validaciones |
| `entity/` | Clases que representan las tablas (`@Entity`, `@Column`) | Cambió una columna de la base |
| `repository/` | Interfaces `JpaRepository` y consultas | Necesitas una búsqueda nueva |
| `service/` | Interfaz `*Service` + implementación `*ServiceImpl` | Cambiar reglas de negocio |
| `mapper/` | Interfaces MapStruct | Casi nunca: los campos con el mismo nombre se copian solos |
| `api/` | Lo que un módulo expone a otros (Spring Modulith) | Cuando otro módulo necesita datos de este |

### 4.3 Endpoints principales

Todos empiezan con `/api/v1`.

| Módulo | Ruta base | Operaciones |
|---|---|---|
| Categorías | `/categorias` | GET lista, GET `/{id}`, POST, PUT `/{id}`, DELETE `/{id}` |
| Productos | `/productos` | GET, GET `/{id}`, POST, PUT `/{id}`, DELETE `/{id}` |
| Imágenes de productos | `/productos/imagenes` | POST (subir), GET `/{nombre}` |
| Clientes | `/clientes` | CRUD completo |
| Mi perfil (cliente) | `/clientes/me` | GET, POST, PUT |
| Pedidos | `/pedidos` | GET, GET `/{id}`, POST, PATCH `/{id}/estado`, PATCH `/{id}/confirmar-pago`, POST `/{id}/pago-prueba` |
| Reportes | `/pedidos/reportes/...` | `resumen`, `productos-mas-vendidos`, `por-estado`, `por-tipo`, `ventas-por-dia`, `metodos-pago`, `clientes` |
| Mis pedidos (cliente) | `/pedidos/mios` | GET, GET `/{id}`, POST, PATCH `/{id}/cancelar` |
| Colas de trabajo | `/operaciones/pedidos`, `/mesero/pedidos`, `/caja/me` | GET |
| Mesas | `/mesas` | GET, PATCH `/{id}/ocupar`, PATCH `/{id}/liberar` |
| Compra web pública | `/web/...` | sesión, pedidos, pago de prueba, mesas |
| Tiempo real | `/eventos`, `/web/mesas/eventos` | Server-Sent Events |
| Contenido | `/banners`, `/promociones`, `/destacados`, `/ubicacion` | GET (público), PUT (administrador) |

Para encontrar un endpoint:

```bash
grep -rn "Mapping" polleria-backend/src/main/java --include=*Controller.java
```

### 4.4 Seguridad: quién puede usar cada URL

Todo está en **`SecurityConfig.java`**:

```java
.requestMatchers(HttpMethod.GET, "/api/v1/promociones").permitAll()            // público
.requestMatchers(HttpMethod.GET, "/api/v1/pedidos").hasAnyRole("ADMINISTRADOR","CAJERO")
.requestMatchers(HttpMethod.PATCH, "/api/v1/mesas/*/ocupar").hasAnyRole("MESERO","ADMINISTRADOR")
```

Si creas un endpoint nuevo, **agrégalo aquí**; si no, el backend lo rechazará (403).

### 4.5 Validaciones y mensajes de error

- **Validaciones de entrada** en los `*Request.java`:

  ```java
  @NotBlank @Size(max = 80) private String nombre;
  @Size(max = 500)          private String descripcion;
  @NotNull                  private Boolean activo;
  ```

- **Errores de negocio** en los servicios, lanzando una excepción con el mensaje:

  ```java
  throw new BusinessConflictException("Ya existe una categoría con ese nombre"); // → 409
  throw new ResourceNotFoundException("Categoría no encontrada: " + id);         // → 404
  throw new InvalidRequestException("...");                                       // → 400
  ```

- `exception/GlobalExceptionHandler.java` convierte esas excepciones en la respuesta HTTP.
  El frontend muestra el `message` tal cual (ver `shared/http-error.ts`), así que **cambiando el
  texto de la excepción cambias lo que ve el usuario**.

### 4.6 Configuración

| Archivo | Qué se configura |
|---|---|
| `application.yml` | Tamaño máximo de subida (20 MB), carpeta de imágenes |
| `application-prod.yml` | Conexión a Oracle y Keycloak dentro de Docker (usa variables de entorno) |
| `application-dev.yml` | Conexión local (Oracle en puerto 1522, Keycloak en 38080) |

Los banners, promociones, destacados y ubicación **no se guardan en Oracle**: se guardan como
archivos JSON (por ejemplo, `banners.json`) en la carpeta de imágenes (volumen Docker
`imagenes-productos`).

---

## 5. Ejemplo completo: agregar un campo nuevo de punta a punta

Supongamos que queremos agregar **"orden"** (número para ordenar) a las **categorías**.

**Paso 1 — Base de datos.** Crear `polleria-backend/scripts/15-orden-categoria.sql`:

```sql
ALTER TABLE POLLERIA.CATEGORIAS ADD (ORDEN NUMBER(5) DEFAULT 0 NOT NULL);
```

y ejecutarlo (sección 6). **Primero la base, después el código:** el backend valida las tablas
al arrancar (`ddl-auto: validate`) y no inicia si falta la columna.

**Paso 2 — Entidad** `catalogo/categoria/entity/Categoria.java`:

```java
@Column(name = "ORDEN", nullable = false)
private Integer orden = 0;
```

**Paso 3 — DTOs** `dto/CategoriaRequest.java` y `dto/CategoriaResponse.java`:

```java
// CategoriaRequest
@NotNull @Min(0) private Integer orden;
// CategoriaResponse
private Integer orden;
```

**Paso 4 — Mapper:** no hace falta tocarlo, porque MapStruct copia los campos con el mismo nombre.

**Paso 5 — Modelo del frontend** `features/catalogo/categoria/categoria.model.ts`:

```ts
export interface CategoriaRequest { nombre: string; descripcion: string | null; activo: boolean; orden: number; }
```

**Paso 6 — Formulario** `categoria-form.ts` (en `this.fb.nonNullable.group({...})`):

```ts
orden: [0, [Validators.required, Validators.min(0)]],
```

y en `categoria-form.html`, junto a los otros campos:

```html
<mat-form-field appearance="outline">
  <mat-label>Orden</mat-label>
  <input matInput type="number" formControlName="orden" min="0" />
</mat-form-field>
```

**Paso 7 — Listado** `categoria-list.html`: agregar la columna `{{ item.orden }}` donde se
muestran los demás datos.

**Paso 8 — Probar:** guardar los archivos, esperar a que se reconstruya el backend y revisar la
pantalla `/interno/categorias`.

---

## 6. Base de datos

El backend **no crea tablas solo** (`ddl-auto: validate`). Los scripts están en
`polleria-backend/scripts/` y se ejecutan **una sola vez, en orden**:

| Orden | Script | Qué hace |
|---|---|---|
| 1 | `01-catalogo.sql` | Categorías y productos |
| 2 | `02-clientes.sql` | Clientes |
| 3 | `03-pedidos.sql` | Pedidos y detalle |
| 4 | `04-cliente-keycloak.sql` | Vincula clientes con su usuario de Keycloak |
| 5 | `07-polleria-neyos.sql` | Mesas, ocupaciones y nuevo flujo de estados |
| 6 | `08-compra-publica.sql` | Compra web pública |
| 7 | `11-catalogo-eliminacion-logica.sql` | Eliminación lógica |
| 8 | `12-detalle-promocion.sql` | Precio regular y promoción en el detalle |
| 9 | `13-unicidad-categorias-vigentes.sql` | Nombre único entre categorías vigentes |
| 10 | `14-presentaciones-producto.sql` | Presentaciones (tamaños/precios) por producto |

Los números que faltan (05, 06, 09 y 10) eran de módulos retirados (Delivery y Abastecimiento).

**Ejecutar un script nuevo** en el Oracle de Docker:

```bash
docker ps --format '{{.Names}}' | grep oracle
```

```bash
{ echo "WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK"; echo "ALTER SESSION SET CONTAINER=FREEPDB1;"; cat polleria-backend/scripts/15-orden-categoria.sql; echo "COMMIT;"; echo "EXIT"; } | docker exec -i polleria-prodtest-oracle-1 sqlplus -s / as sysdba
```

(Reemplaza `polleria-prodtest-oracle-1` por el nombre que mostró el primer comando.)

**Reglas para scripts nuevos:**

- Usa el siguiente número (`15-...sql`).
- Nunca modifiques un script que ya se ejecutó: crea uno nuevo.
- Las tablas van con el esquema delante: `POLLERIA.NOMBRE_TABLA`.

---

## 7. Comandos útiles

Ver el estado de los contenedores:

```bash
docker compose --env-file .env.prodtest -f compose.prod.yml -f compose.prodtest.yml ps
```

Ver los logs del backend en vivo (errores de Java, SQL, seguridad):

```bash
docker compose --env-file .env.prodtest -f compose.prod.yml -f compose.prodtest.yml logs -f backend
```

Ver los logs del frontend de desarrollo (errores de compilación de Angular):

```bash
docker compose --env-file .env.prodtest -f compose.prod.yml -f compose.prodtest.yml -f compose.live.yml logs -f frontend-dev
```

Reiniciar solo el backend:

```bash
docker compose --env-file .env.prodtest -f compose.prod.yml -f compose.prodtest.yml restart backend
```

Buscar un texto en todo el proyecto:

```bash
grep -rn "texto a buscar" polleria-frontend/src polleria-backend/src
```

Atajos de VS Code:

| Atajo | Acción |
|---|---|
| `Ctrl + P` | Abrir un archivo por su nombre (escribe `categoria-form`, `styles.css`…) |
| `Ctrl + Shift + F` | Buscar texto en todo el proyecto |
| `F12` | Ir a la definición de una clase o función |
| `Ctrl + Shift + O` | Ver las secciones / funciones del archivo abierto |

---

## 8. Resumen para explicar el proyecto en la evaluación

- **Arquitectura:** frontend Angular y backend Spring Boot separados. Nginx los publica en un
  mismo origen (`/` → Angular, `/api` → backend, `/auth` → Keycloak).
- **Backend en capas:** Controller → Service → Repository → Entity, con DTOs de entrada y salida
  y MapStruct para convertirlos. Módulos por dominio (catálogo, clientes, contenido, pedidos)
  separados con Spring Modulith.
- **Seguridad:** login con Keycloak (OAuth2 / OpenID Connect con PKCE). El backend valida el
  token JWT y aplica permisos por rol en `SecurityConfig.java`. El frontend también protege
  las rutas con *guards*.
- **Base de datos:** Oracle, con scripts SQL versionados e incrementales. Hibernate solo valida
  que las tablas coincidan con las entidades.
- **Tiempo real:** Server-Sent Events (`/api/v1/eventos`) para que caja, cocina y mesero vean
  los pedidos al instante.
- **Despliegue:** Docker Compose con Oracle, PostgreSQL (para Keycloak), Keycloak, backend y
  frontend (Nginx).
