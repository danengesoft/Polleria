# Guía para levantar Pollería Neyos en Windows

Guía paso a paso para clonar y levantar el proyecto **directamente en Windows 10/11**
(por ejemplo, en las máquinas de la universidad), usando **Git para Windows**,
**Docker Desktop** y **PowerShell**.

## Índice

1. [Antes de empezar: requisitos](#1-antes-de-empezar-requisitos)
2. [Qué llevar en la USB](#2-qué-llevar-en-la-usb)
3. [Instalar los programas](#3-instalar-los-programas)
4. [Clonar el proyecto](#4-clonar-el-proyecto)
5. [Copiar los archivos de configuración](#5-copiar-los-archivos-de-configuración)
6. [Levantar el sistema](#6-levantar-el-sistema)
7. [Crear los usuarios](#7-crear-los-usuarios)
8. [Abrir el sistema y trabajar](#8-abrir-el-sistema-y-trabajar)
9. [Diferencias con Fedora](#9-diferencias-con-fedora-lo-que-no-funciona-en-windows)
10. [Problemas comunes](#10-problemas-comunes)
11. [Checklist para el día de la evaluación](#11-checklist-para-el-día-de-la-evaluación)

---

## 1. Antes de empezar: requisitos

| Requisito | Detalle |
|---|---|
| Sistema | Windows 10 (versión 2004 o superior) o Windows 11 |
| Permisos | **Administrador** (para instalar Docker Desktop) |
| Virtualización | Activada en la BIOS (Intel VT-x / AMD-V). Docker Desktop la necesita siempre |
| Memoria RAM | 16 GB recomendado (8 GB libres como mínimo) |
| Disco | 20 GB libres |

**Comprobar si la virtualización está activa:** Administrador de tareas (`Ctrl + Shift + Esc`)
→ **Rendimiento** → **CPU** → debe decir **Virtualización: Habilitado**.

> Docker Desktop usa por dentro WSL2 o Hyper-V aunque tú trabajes solo en Windows. Si la PC no
> tiene permisos de administrador o la virtualización está desactivada, **no se podrá levantar
> el proyecto**. Consúltalo con el encargado del laboratorio antes del día de la evaluación.

---

## 2. Qué llevar en la USB

Estos archivos **no están en GitHub** (el `.gitignore` los excluye por seguridad). Cópialos
desde tu Fedora:

| Archivo (en `~/Polleria`) | Para qué sirve | ¿Obligatorio? |
|---|---|---|
| `.env.prodtest` | Contraseñas y configuración del sistema | **Sí** (o crear uno nuevo, sección 5.2) |
| `production/generated/polleria-prodtest/polleria-realm.json` | Configuración del login (Keycloak) | **Sí** (o generarlo, sección 5.3) |
| `polleria-imagenes.tar` | Imágenes de Docker ya construidas | Recomendado (ahorra mucho tiempo) |

**En tu Fedora**, desde `~/Polleria`, con el sistema ya construido:

Generar el archivo de imágenes (puede pesar varios GB):

```bash
docker save -o polleria-imagenes.tar $(docker compose --env-file .env.prodtest -f compose.prod.yml -f compose.prodtest.yml config --images)
```

Copiar todo a la USB (cambia `/run/media/$USER/USB` por la ruta de tu USB):

```bash
mkdir -p /run/media/$USER/USB/polleria
```

```bash
cp .env.prodtest polleria-imagenes.tar production/generated/polleria-prodtest/polleria-realm.json /run/media/$USER/USB/polleria/
```

> El `.env.prodtest` tiene contraseñas: no lo subas a GitHub ni lo compartas.

---

## 3. Instalar los programas

### 3.1 Git para Windows

Descárgalo de la página oficial de Git (*Git for Windows*) e instálalo con las opciones por
defecto.

### 3.2 Docker Desktop

1. Descarga **Docker Desktop para Windows** desde la página oficial de Docker e instálalo
   (deja marcada **Use WSL 2 instead of Hyper-V** si aparece).
2. Reinicia la PC si lo pide.
3. Abre **Docker Desktop** y espera a que abajo a la izquierda diga **Engine running**.
4. Déjalo **abierto** mientras uses el sistema.

**Opcional:** en Docker Desktop → **Settings** → **Resources**, dale al menos **8 GB** de
memoria (si la opción aparece).

### 3.3 Comprobar que todo funciona

Abre **PowerShell** (no hace falta como administrador) y ejecuta:

```powershell
git --version
```

```powershell
docker info --format '{{.ServerVersion}}'
```

```powershell
docker compose version
```

Los tres deben mostrar un número de versión. `docker compose` debe ser **2.24 o superior**.

> **A partir de aquí, todos los comandos se ejecutan en PowerShell**, dentro de la carpeta del
> proyecto.

---

## 4. Clonar el proyecto

Crea una carpeta de trabajo **fuera de OneDrive y de Escritorio** (para evitar sincronizaciones
y rutas largas), por ejemplo `C:\Proyectos`:

```powershell
mkdir C:\Proyectos
```

```powershell
cd C:\Proyectos
```

Clona **desactivando la conversión de saltos de línea** (muy importante):

```powershell
git clone -c core.autocrlf=false https://github.com/danengesoft/Polleria.git
```

```powershell
cd C:\Proyectos\Polleria
```

> **¿Por qué `-c core.autocrlf=false`?** Git para Windows suele convertir los saltos de línea
> al formato de Windows (CRLF). Los archivos del proyecto se usan dentro de contenedores Linux,
> y con CRLF el backend **no compila** (error `/bin/sh^M: bad interpreter` en `mvnw`). Esta
> opción queda guardada en el repositorio, así que solo se pone al clonar.

**Si el repositorio es privado**, Git abrirá una ventana del navegador para iniciar sesión en
GitHub. Inicia sesión y autoriza; el clon continúa solo.

**Comprobar que los saltos de línea quedaron bien:**

```powershell
git config core.autocrlf
```

Debe responder `false`.

---

## 5. Copiar los archivos de configuración

Todos los comandos, desde `C:\Proyectos\Polleria`. En los ejemplos la USB es la unidad `D:`;
cámbiala por la letra de tu USB.

### 5.1 Copiar desde la USB

```powershell
Copy-Item D:\polleria\.env.prodtest .
```

```powershell
New-Item -ItemType Directory -Force production\generated\polleria-prodtest | Out-Null
```

```powershell
Copy-Item D:\polleria\polleria-realm.json production\generated\polleria-prodtest\
```

Si trajiste las imágenes de Docker, cárgalas (tarda unos minutos):

```powershell
docker load -i D:\polleria\polleria-imagenes.tar
```

> Usa **el mismo `.env.prodtest` de tu Fedora**: el valor `IMAGE_TAG` de ese archivo debe
> coincidir con el nombre de las imágenes que cargaste.

Si copiaste ambos archivos, **salta a la sección 6**.

### 5.2 Si no tienes el `.env.prodtest` (crear uno nuevo)

Copia y pega este bloque completo en PowerShell. Crea el archivo con contraseñas aleatorias,
en formato UTF-8 sin BOM y con saltos de línea de Linux (si se crea con el Bloc de notas,
Docker puede no leerlo bien):

```powershell
$clave = { [guid]::NewGuid().ToString('N') }
$contenido = @"
COMPOSE_PROJECT_NAME=polleria-prodtest
IMAGE_TAG=prod-test-f19
APP_DOMAIN=127.0.0.1
PUBLIC_APP_URL=http://127.0.0.1:18080
BIND_ADDRESS=127.0.0.1
ORACLE_PASSWORD=$(& $clave)
ORACLE_APP_USER=POLLERIA
ORACLE_APP_PASSWORD=$(& $clave)
KEYCLOAK_DB_PASSWORD=$(& $clave)
KEYCLOAK_ADMIN_USERNAME=adminpolleria
KEYCLOAK_ADMIN_PASSWORD=$(& $clave)
"@
[IO.File]::WriteAllText("$PWD\.env.prodtest", ($contenido -replace "`r`n", "`n") + "`n", (New-Object Text.UTF8Encoding $false))
```

Para ver las contraseñas generadas (la de `KEYCLOAK_ADMIN_PASSWORD` la necesitarás en la
sección 7):

```powershell
Get-Content .env.prodtest
```

### 5.3 Si no tienes el `polleria-realm.json` (generarlo)

En Fedora este archivo lo genera `scripts/production/preflight.py`, pero ese script **no
funciona en Windows** (exige permisos de archivo `0600`, que Windows no maneja). Este bloque
hace lo mismo: copia el realm del proyecto y le pone la dirección local. Pégalo completo en
PowerShell:

```powershell
$url = 'http://127.0.0.1:18080'
$realm = Get-Content -Raw -Encoding UTF8 polleria-backend\keycloak\polleria-realm.json | ConvertFrom-Json
$cliente = $realm.clients[0]
$cliente.redirectUris = @("$url/auth/callback")
$cliente.webOrigins = @($url)
$cliente.attributes.'post.logout.redirect.uris' = "$url/"
$realm | Add-Member -NotePropertyName sslRequired -NotePropertyValue external -Force
New-Item -ItemType Directory -Force production\generated\polleria-prodtest | Out-Null
[IO.File]::WriteAllText("$PWD\production\generated\polleria-prodtest\polleria-realm.json", ($realm | ConvertTo-Json -Depth 100), (New-Object Text.UTF8Encoding $false))
```

Comprobar que se creó:

```powershell
Test-Path production\generated\polleria-prodtest\polleria-realm.json
```

Debe responder `True`.

---

## 6. Levantar el sistema

Todos los comandos, desde `C:\Proyectos\Polleria`, con Docker Desktop abierto.

> **El orden importa.** El backend no arranca si las tablas no existen, y el frontend espera a
> que el backend esté listo. Por eso primero se levanta Oracle, luego se crean las tablas y al
> final se levanta todo lo demás.

### 6.1 Levantar solo Oracle

```powershell
docker compose --env-file .env.prodtest -f compose.prod.yml -f compose.prodtest.yml up -d oracle
```

Espera a que esté **healthy** (la primera vez puede tardar de 2 a 5 minutos). Revisa con:

```powershell
docker compose --env-file .env.prodtest -f compose.prod.yml -f compose.prodtest.yml ps oracle
```

Repite el comando hasta que en la columna `STATUS` aparezca `(healthy)`.

### 6.2 Crear las tablas (solo la primera vez)

Primero copia los scripts SQL dentro del contenedor de Oracle (así se ejecutan en Linux y se
evitan problemas de codificación de PowerShell):

```powershell
docker cp polleria-backend\scripts polleria-prodtest-oracle-1:/tmp/scripts
```

Luego ejecútalos en orden (pega el bloque completo):

```powershell
foreach ($f in '01','02','03','04','07','08','11','12','13','14') {
  docker exec polleria-prodtest-oracle-1 bash -c "{ echo 'WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK'; echo 'ALTER SESSION SET CONTAINER=FREEPDB1;'; cat /tmp/scripts/$f-*.sql; echo 'COMMIT;'; echo 'EXIT'; } | sqlplus -s / as sysdba"
  if ($LASTEXITCODE -ne 0) { Write-Host "Error en el script $f" -ForegroundColor Red; break }
}
```

Si no aparece ningún mensaje de error (`ORA-...` o `Error en el script`), las tablas quedaron
creadas.

> Ejecuta este paso **una sola vez**. Si lo repites, dará errores porque las tablas ya existen
> (no se pierde nada, solo no hace falta).

### 6.3 Levantar todo lo demás

**Si cargaste las imágenes desde la USB** (más rápido, no compila nada):

```powershell
docker compose --env-file .env.prodtest -f compose.prod.yml -f compose.prodtest.yml up -d
```

**Si NO cargaste las imágenes** (compila el backend y el frontend; tarda varios minutos):

```powershell
docker compose --env-file .env.prodtest -f compose.prod.yml -f compose.prodtest.yml up -d --build
```

Comprueba que todos los servicios estén `healthy` o `running`:

```powershell
docker compose --env-file .env.prodtest -f compose.prod.yml -f compose.prodtest.yml ps
```

Deben aparecer: `oracle`, `keycloak-db`, `keycloak`, `backend` y `frontend`.

---

## 7. Crear los usuarios

En una instalación nueva **no existe ningún usuario**. Los clientes se registran solos desde la
web; los usuarios del personal se crean así.

> El backend solo reconoce los roles del personal si el usuario tiene **correo** y el correo
> está **verificado** (`emailVerified=true`). No te saltes esa parte.

**1. Iniciar sesión como administrador de Keycloak** (una vez por sesión). El usuario es el
valor de `KEYCLOAK_ADMIN_USERNAME` del `.env.prodtest`; la contraseña te la pedirá
(`KEYCLOAK_ADMIN_PASSWORD`):

```powershell
docker exec -it polleria-prodtest-keycloak-1 /opt/keycloak/bin/kcadm.sh config credentials --server http://localhost:8080/auth --realm master --user USUARIO_ADMIN_DEL_ENV
```

**2. Crear un usuario** (cambia el correo y el nombre):

```powershell
docker exec polleria-prodtest-keycloak-1 /opt/keycloak/bin/kcadm.sh create users -r polleria -s username=admin@polleria.pe -s email=admin@polleria.pe -s emailVerified=true -s enabled=true -s firstName=Admin -s lastName=Neyos
```

**3. Ponerle contraseña** (te la pedirá):

```powershell
docker exec -it polleria-prodtest-keycloak-1 /opt/keycloak/bin/kcadm.sh set-password -r polleria --username admin@polleria.pe
```

**4. Asignarle el rol** (`ADMINISTRADOR`, `CAJERO`, `OPERACIONES` o `MESERO`):

```powershell
docker exec polleria-prodtest-keycloak-1 /opt/keycloak/bin/kcadm.sh add-roles -r polleria --uname admin@polleria.pe --rolename ADMINISTRADOR
```

Repite los pasos 2 a 4 para cada usuario. Para la evaluación conviene tener **uno por rol**:

| Correo de ejemplo | Rol | Pantalla a la que entra |
|---|---|---|
| `admin@polleria.pe` | `ADMINISTRADOR` | Todo el portal interno |
| `caja@polleria.pe` | `CAJERO` | Caja |
| `cocina@polleria.pe` | `OPERACIONES` | Operaciones (cocina) |
| `mesero@polleria.pe` | `MESERO` | Mesero |

**Ver los usuarios creados:**

```powershell
docker exec polleria-prodtest-keycloak-1 /opt/keycloak/bin/kcadm.sh get users -r polleria --fields username,email,emailVerified,enabled
```

---

## 8. Abrir el sistema y trabajar

Abre en el navegador: **http://127.0.0.1:18080**

1. Entra con el usuario administrador y crea **categorías** y **productos** (con sus fotos).
2. En **Local y página de inicio** configura banners, promociones, destacados y ubicación.

### Editar el código

Abre la carpeta `C:\Proyectos\Polleria` en VS Code (o el editor que uses).

> Si el editor tiene activado **Format On Save**, desactívalo para este proyecto: reformatea
> los archivos al guardar y aparecen cambios que no hiciste (como pasó con
> `producto-form.html`).

### Recarga automática al guardar

En Windows no funciona `scripts/desarrollo.sh` (es un script de bash). Se usan directamente los
dos comandos que ese script ejecuta por dentro.

**1. Levantar el frontend de desarrollo** (una vez):

```powershell
docker compose --env-file .env.prodtest -f compose.prod.yml -f compose.prodtest.yml -f compose.live.yml up -d --build --no-deps frontend-dev frontend
```

**2. Vigilar el backend** (deja esta ventana de PowerShell abierta):

```powershell
docker compose --env-file .env.prodtest -f compose.prod.yml -f compose.prodtest.yml -f compose.live.yml -f compose.watch.yml watch --no-up --prune=false backend
```

Los cambios del frontend se ven al guardar (en Windows pueden tardar unos segundos más que en
Fedora). Los del backend se reconstruyen solos en 1–2 minutos.

### Apagar el sistema

```powershell
docker compose --env-file .env.prodtest -f compose.prod.yml -f compose.prodtest.yml down
```

> **Nunca** agregues `-v` a ese comando: borra la base de datos y los usuarios.

### Volver a levantarlo otro día

Las tablas y los usuarios se conservan. Abre Docker Desktop y ejecuta:

```powershell
cd C:\Proyectos\Polleria
```

```powershell
docker compose --env-file .env.prodtest -f compose.prod.yml -f compose.prodtest.yml up -d
```

---

## 9. Diferencias con Fedora (lo que no funciona en Windows)

| En Fedora | En Windows | Qué hacer |
|---|---|---|
| `bash scripts/desarrollo.sh` | No funciona (bash, `flock`) | Usar los dos comandos de la sección 8 |
| `python3 scripts/production/preflight.py` | Falla (permisos `0600`) | Traer el `polleria-realm.json` o generarlo (5.3) |
| Tarea de VS Code que se ejecuta al abrir la carpeta | Falla (llama a `desarrollo.sh`) | Ignorar el error; usar la sección 8 |
| `scripts/production/backup.py` | No probado en Windows | Hacer los respaldos desde Fedora |
| Comandos con `{ ...; } \| docker exec ...` | No existen en PowerShell | Usar los bloques de esta guía (sección 6.2) |

---

## 10. Problemas comunes

| Síntoma | Causa probable | Solución |
|---|---|---|
| Docker Desktop no inicia o dice que falta la virtualización / WSL | Virtualización desactivada o WSL no instalado | Activar la virtualización en la BIOS; en PowerShell como administrador: `wsl --install` y reiniciar |
| `Cannot connect to the Docker daemon` / `error during connect` | Docker Desktop cerrado o iniciando | Abrir Docker Desktop y esperar a **Engine running** |
| `Set ORACLE_PASSWORD` u otro `Set ...` | Falta el `.env.prodtest`, está en otra carpeta o tiene BOM | Revisar la sección 5; si lo creaste a mano, créalo con el bloque de la 5.2 |
| `keycloak` no arranca: no encuentra `polleria-realm.json` | Falta la configuración del login | Copiarla de la USB (5.1) o generarla (5.3) |
| `/bin/sh^M: bad interpreter` al compilar el backend | Se clonó sin `-c core.autocrlf=false` | Borrar la carpeta y clonar de nuevo como en la sección 4 |
| Error con `:Z` o `:z` en los volúmenes (*invalid mode*) | Opción de volumen de Fedora (SELinux) | Avisar: hay que quitarla de los archivos `compose` para Windows |
| `backend` queda `unhealthy` o se reinicia | Faltan las tablas | Hacer la sección 6.2 y luego reiniciar el backend (ver abajo) |
| `dependency failed to start: container ... backend is unhealthy` | Se levantó todo antes de crear las tablas | Hacer la sección 6.2 y volver a ejecutar el `up -d` |
| `No such container: polleria-prodtest-oracle-1` | El nombre del contenedor es otro | Ver los nombres con `docker ps --format '{{.Names}}'` y usar el correcto |
| `port is already allocated` / `18080` en uso | Otro programa usa ese puerto | Cerrar ese programa o reiniciar Docker Desktop |
| El usuario entra pero aparece **"sin permiso"** | Correo no verificado o rol sin asignar | Repetir los pasos 2 y 4 de la sección 7 (`emailVerified=true`) |
| Todo va muy lento | Poca memoria para Docker | Subir la memoria en Docker Desktop → Settings → Resources; cerrar otros programas |
| Oracle tarda mucho en estar `healthy` | Es normal la primera vez | Esperar hasta 5 minutos |
| `git status` muestra archivos modificados que no tocaste | El editor reformateó o cambió saltos de línea | `git diff` para revisar; `git restore <archivo>` para descartar |

**Reiniciar el backend:**

```powershell
docker compose --env-file .env.prodtest -f compose.prod.yml -f compose.prodtest.yml restart backend
```

**Ver los errores de un servicio** (cambia `backend` por `keycloak`, `oracle` o `frontend`):

```powershell
docker compose --env-file .env.prodtest -f compose.prod.yml -f compose.prodtest.yml logs --tail 100 backend
```

---

## 11. Checklist para el día de la evaluación

**Antes (en casa, con tiempo):**

- [ ] Confirmar con el laboratorio que las PCs tienen **Git y Docker Desktop**, o permisos de administrador para instalarlos, y la virtualización activada.
- [ ] Copiar a la USB: `.env.prodtest`, `polleria-realm.json` y `polleria-imagenes.tar`.
- [ ] Tener a mano esta guía y la de desarrollo.
- [ ] Ensayar la instalación completa al menos una vez en una PC con Windows.

**En la universidad (orden rápido):**

1. [ ] Abrir Docker Desktop y esperar **Engine running**.
2. [ ] `git clone -c core.autocrlf=false ...` en `C:\Proyectos` (sección 4).
3. [ ] Copiar `.env.prodtest` y `polleria-realm.json`; `docker load` de las imágenes (5.1).
4. [ ] Levantar Oracle y esperar `healthy` (6.1).
5. [ ] Copiar y ejecutar los scripts SQL (6.2).
6. [ ] Levantar todo **sin** `--build` (6.3).
7. [ ] Crear los usuarios por rol (sección 7).
8. [ ] Abrir **http://127.0.0.1:18080**, entrar como administrador y cargar algunos productos.
