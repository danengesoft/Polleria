# Fase 7 — Arquitectura base Angular

24 de octubre de 2026, America/Lima. Ruta /home/systemseng/Polleria/polleria-frontend.

## Preparación

Se verificaron ruta raíz, existencia del backend, Node 24.18.0, npm 11.16.0 y Git limpio.
S07 revisada; versiones exactas confirmadas en el registro antes de instalar.
Generación CLI 22.1.8 con standalone, routing, CSS, zoneless, sin SSR, sin Git,
sin integración AI y sin instalación automática. Framework fijado en 22.1.6 y
CLI/build/compiler coherentes antes de npm install. TypeScript 6.0.3, RxJS 7.8.2.
No se cambió Node/npm ni se instaló CLI global ni Material/CDK.

## Archivos y alcance

Proyecto Angular completo generado, con package.json/lock, angular.json, tsconfigs,
main.ts e index.html en español. App solo outlet superior, configuración Router/HttpClient.
core: layouts público/interno, dos inicios mínimos, diagnóstico HTTP, ApiService,
ConexionService e interceptor de trazabilidad S07. shared: navegación real reutilizada.
No se creó features vacío: aparecerá con funcionalidad en Fase 8.
Sin CRUD, modelos comerciales Angular, carrito, checkout, Pedido, Keycloak, guards,
JWT, login falso, sesiones simuladas, Nginx ni dependencias ajenas al generador.

Rutas lazy: /, /interno, /interno/conexion. Layouts con outlet hijo, navegación por links,
jerarquía visual básica, CSS responsive, enlace de salto al contenido y estados accesibles.
El portal interno indica ausencia temporal de autenticación en desarrollo.

API central en environment.ts, localhost:8080 verificado realmente.
ConexionService solo diagnóstico GET /api/v1/categorias, descartando datos del recurso.
Se evita Actuator porque su ruta no forma parte del CORS /api existente; no se modifica
backend para habilitarlo. HttpClient + traceIdInterceptor envían X-Trace-ID al API propio.
No se implementa interceptor de autenticación. S07 sí introduce trazabilidad funcional.

## Comprobaciones

- npm run build: correcto, sin errores ni avisos de presupuesto.
  Bundle inicial 240.69 kB, transferencia estimada 66.96 kB; rutas lazy separadas.
- npm test -- --watch=false: 4 pruebas, 1 archivo, todo correcto (Vitest 4.1.12).
  Raíz/outlet, navegación de layouts/hijos, diagnóstico HTTP exitoso con trace ID,
  tratamiento de error HTTP sin romper el layout.
- npm start -- --host localhost --port 4200: servidor real activo.
- HTTP directo /interno/conexion: 200, fallback SPA operativo.
- Navegador real localhost:4200: inicio público → portal interno → conexión.
- Pulsación de Comprobar conexión realizó HttpClient hacia Spring: HTTP 200;
  X-Trace-ID 6635d9b7-3d97-4848-96e4-ad04de79fe6a visible en pantalla.
  Eso confirma solicitud cross-origin aceptada y lectura del header expuesto por CORS.
- Refresco directo de /interno/conexion renderizó layout e hijo correctos.
- Navegación de vuelta al inicio público correcta; consola sin errores ni warnings.
- Responsive observado a 390, 768 y 1440 px, scrollWidth igual al ancho visible.
  En móvil el portal pasa a una columna y navegación superior; override del navegador
  restaurado al finalizar la comprobación.
- Ningún archivo backend modificado; no se repiten sus 75 pruebas por cambios solo frontend.
- Sin escritura en Git: solo inspección; generador con skip-git.

## Avisos reales y pendientes

npm install completó, pero npm audit informa 7 hallazgos: 5 high y 2 critical.
Detalle reproducible en npm-audit.json. Auditoría omit=dev: 2 high (Router y zod),
en npm-audit-runtime.json. Las alertas Router se refieren a SSR, deshabilitado aquí.
Los otros hallazgos incluyen tooling/dependencias; no se ocultan ni se consideran resueltos.
Los arreglos sugeridos por npm pasan Angular CLI/build a 22.2.4 y Router a 22.2.3,
fuera de la versión académica fijada. Se conserva LP2 y se documenta la revisión necesaria
antes de publicación; sin force, overrides ni cambios de versión automáticos.

Aviso npm allow-scripts sobre esbuild/lmdb/msgpackr-extract/parcel-watcher sin autorización
explícita en package.json; no impidió build/pruebas/servidor. No se ejecutó approve-scripts.
No existe script lint y no se añadió uno ni ESLint.

Desviaciones intencionales de S07: se pospone su CRUD por alcance explícito de esta fase;
dos layouts en una SPA; no carpetas vacías; prueba HTTP técnica aislada; versiones exactas
mediante CLI local y skip-install para evitar resolver otra versión reciente por rangos.
No se prepara producción ni se aplica la estética final.

Angular y Spring permanecen ejecutándose localmente. Trabajo detenido antes de Fase 8.
Commit sugerido: `feat: crear SPA Angular base con layouts público e interno y diagnóstico HTTP`
