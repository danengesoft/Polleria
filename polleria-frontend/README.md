# POLLERIA — SPA, Fase 8

Una sola aplicación standalone, Router con carga diferida y detección zoneless.
Referencias LP2 S07 y S08. CRUD internos de Categoría, Producto y Cliente.
Sin autenticación ni catálogo público completo; no incluye Pedidos frontend.

## Versiones y ejecución

Node 24.18.0 y npm 11.16.0 verificados. CLI/build 22.1.8 y framework Angular 22.1.6
fijados exactamente; TypeScript 6.0.3. package-lock.json conserva las dependencias resueltas.

Desde `/home/systemseng/Polleria/polleria-frontend`:

```bash
npm ci
npm start -- --host localhost --port 4200
npm run build
npm test -- --watch=false
```

No existe script lint; no se instala ESLint. El servidor se mantiene limitado a localhost.
El build optimizado aún usa configuración local; no representa una configuración de despliegue.
Angular Material y CDK 22.2.0, fijados y compatibles con Angular 22.1.6.
Tema local en `src/material-theme.scss`, con tipografía del sistema y colores sobrios.

## Arquitectura

```text
src/
├── environments/environment.ts
└── app/
    ├── app.ts / app.html / app.config.ts / app.routes.ts
    ├── core/
    │   ├── layout/publico/
    │   ├── layout/interno/
    │   ├── inicio/
    │   ├── conexion/
    │   ├── services/
    │   └── interceptors/trace-id-interceptor.ts
    ├── shared/
    │   ├── navigation/app-navigation.ts
    │   ├── confirmar-eliminacion.ts
    │   ├── form-validation.ts
    │   └── http-error.ts
    └── features/
        ├── catalogo/
        │   ├── categoria/
        │   └── producto/
        └── clientes/cliente/
```

`core` contiene infraestructura global; `shared` contiene navegación efectivamente
reutilizada por ambos layouts, confirmación de eliminación y utilidades de formularios/errores.
Cada recurso tiene modelo, servicio HTTP, listado y formulario separados.
No se crean carpetas de negocio vacías ni auth/guards ficticios.
El único interceptor es trazabilidad real S07, sin JWT; solo aplica a URLs /api del backend.

Rutas:
- `/`: layout público con inicio mínimo.
- `/interno`: layout interno con su inicio hijo.
- `/interno/conexion`: diagnóstico técnico dentro del layout interno.
- `/interno/categorias`, `/interno/productos`, `/interno/clientes`: listados.
- Cada recurso admite `/nuevo` y `/:id/editar` dentro del layout interno.
- Ruta desconocida: vuelve al inicio.

El componente App contiene únicamente el router-outlet superior. Cada layout tiene
su outlet hijo. Navegación y títulos pertenecen al Router. El portal interno es
accesible en desarrollo sin autenticación, explícitamente indicado en pantalla.

## API y comprobación real

`environment.ts` concentra `apiBaseUrl: http://localhost:8080`.
`ApiService.buildUrl` forma URLs; los servicios CRUD reutilizan esta infraestructura.
No se configura dominio, URL de producción ni HTTPS. El reemplazo de ambiente se
incorporará cuando exista un destino de despliegue, sin modificar cada servicio.

HttpClient se proporciona en app.config.ts con provideHttpClient/withInterceptors.
El diagnóstico utiliza ConexionService con GET /api/v1/categorias: descarta los datos
comerciales y muestra solo HTTP/Trace ID. No es CategoriaService ni CRUD ni usa Actuator
como dependencia comercial. La consulta permite probar Angular → Spring y el CORS
existente de /api/** sin tocar el backend.

Con Spring activo en 8080, abrir http://localhost:4200/interno/conexion y pulsar
Comprobar conexión. Resultado verificado: HTTP 200 con X-Trace-ID generado por Angular,
reflejado por Spring y legible por CORS. Los errores muestran un mensaje recuperable.

## Verificaciones y avisos

Ver VERIFICACION-FASE-8.md para la verificación actual y VERIFICACION-FASE-7.md
para el historial. Build y 18 pruebas correctos (4 anteriores + 14 nuevas). Navegación real, refresco
directo y comunicación CORS comprobados en navegador, consola sin errores.
Layout comprobado a 390/768/1440 px sin desbordamiento horizontal.
Backend sin cambios: no se repitió innecesariamente su suite de 75 pruebas.

`npm audit` mantiene 7 hallazgos (5 high, 2 critical), con detalle actual en
npm-audit-fase-8.json. Omisiones dev: 2 high, en npm-audit-runtime-fase-8.json. Router señala
escenarios SSR (SSR está desactivado); los demás hallazgos deben revisarse antes de
publicar. No se afirma que todos sean explotables en esta SPA ni que estén resueltos.
Las correcciones propuestas por npm requieren versiones fuera de las fijadas LP2;
no se ejecutó audit fix --force ni se añadieron overrides de dependencias.

npm también avisó de scripts de esbuild/lmdb/msgpackr-extract/parcel-watcher sin política
allowScripts explícita. Build, servidor y pruebas funcionan; no se ampliaron permisos
ni se ejecutó approve-scripts automáticamente. No hubo warnings de presupuesto del build.

Git lo administra el usuario. El generador se ejecutó con --skip-git; sin commit/push.

## CRUD internos

Listados y formularios usan signals para datos, cargas y errores, y Reactive Forms
con validaciones alineadas con los DTO Java. Servicios separados para Categoria,
Producto y Cliente; ninguna llamada HttpClient desde componentes de los recursos.

Producto recibe `categoriaId` numérico, presenta `categoria.nombre` y filtra en
servidor; “Todas” omite el parámetro. Precio se valida como texto decimal y se
serializa como número JSON, sin cálculos ni redondeos en frontend. CurrencyPipe
presenta PEN con dos decimales. No hay stock ni carga de archivos.

Cliente acepta teléfono String con `^(\+51)?[0-9]{7,9}$`. Correo y campos de
dirección/referencia son opcionales. Al editar se preservan los campos opcionales.

El diálogo Material pide confirmación antes de cada DELETE. Los mensajes 400/404/409
provienen del contrato seguro del backend; 500 y desconexión usan mensajes generales.
Una categoría utilizada conserva sus datos ante 409; puede editarse `activo` con PUT.
Al guardar se vuelve al listado y se informa el resultado. Las tablas pasan a filas
etiquetadas en pantallas pequeñas.
