# POLLERIA — SPA base, Fase 7

Una sola aplicación standalone, Router con carga diferida y detección zoneless.
Referencia LP2 S07. Sin CRUD, formularios comerciales, autenticación ni frontend público completo.

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
No se instaló Material/CDK: S07 no lo necesita en esta base; llegará cuando haya uso real.

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
    └── shared/navigation/app-navigation.ts
```

`core` contiene infraestructura global; `shared` contiene navegación efectivamente
reutilizada por ambos layouts. `features/` se incorporará al existir el primer feature
en Fase 8; no se crearon carpetas de negocio vacías. No hay auth/guards ficticios.
El único interceptor es trazabilidad real S07, sin JWT; solo aplica a URLs /api del backend.

Rutas:
- `/`: layout público con inicio mínimo.
- `/interno`: layout interno con su inicio hijo.
- `/interno/conexion`: diagnóstico técnico dentro del layout interno.
- Ruta desconocida: vuelve al inicio.

El componente App contiene únicamente el router-outlet superior. Cada layout tiene
su outlet hijo. Navegación y títulos pertenecen al Router. El portal interno es
accesible en desarrollo sin autenticación, explícitamente indicado en pantalla.

## API y comprobación real

`environment.ts` concentra `apiBaseUrl: http://localhost:8080`.
`ApiService.buildUrl` forma URLs; futuros servicios no repetirán el host.
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

Ver VERIFICACION-FASE-7.md. Build y 4 pruebas correctos. Navegación real, refresco
directo y comunicación CORS comprobados en navegador, consola sin errores.
Layout comprobado a 390/768/1440 px sin desbordamiento horizontal.
Backend sin cambios: no se repitió innecesariamente su suite de 75 pruebas.

`npm audit` reportó 7 hallazgos (5 high, 2 critical), con detalle en npm-audit.json.
La auditoría omitiendo dev reporta 2 high, en npm-audit-runtime.json. Router señala
escenarios SSR (SSR está desactivado); los demás hallazgos deben revisarse antes de
publicar. No se afirma que todos sean explotables en esta SPA ni que estén resueltos.
Las correcciones propuestas por npm requieren versiones fuera de las fijadas LP2;
no se ejecutó audit fix --force ni se añadieron overrides de dependencias.

npm también avisó de scripts de esbuild/lmdb/msgpackr-extract/parcel-watcher sin política
allowScripts explícita. Build, servidor y pruebas funcionan; no se ampliaron permisos
ni se ejecutó approve-scripts automáticamente. No hubo warnings de presupuesto del build.

Git lo administra el usuario. El generador se ejecutó con --skip-git; sin commit/push.
