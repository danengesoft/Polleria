# Auditoría de dependencias — Fase 17

Fecha: 25 de octubre de 2026. Resultado final: **0 vulnerabilidades** en `npm audit`.
Los JSON inicial, intermedio y final se conservan en esta carpeta. Siete paquetes
señalados inicialmente (5 altos, 2 críticos) no equivalen a siete CVE independientes:
la auditoría también propaga severidad a los padres que incorporan paquetes vulnerables.

## Clasificación y decisión

A = corrección compatible con Angular 22; B = exige cambio mayor/incompatible;
C = tooling sin inclusión directa en el bundle de la aplicación;
D = componente runtime con riesgo aplicable según configuración.
Las letras pueden coexistir. No quedó un caso B ni un riesgo ALTO aceptado pendiente.

| Grupo inicial | Dependencia y ruta | Uso / aplicabilidad en POLLERIA | Corrección aplicada | Clase |
|---|---|---|---|---|
| `@angular/router` 22.1.6, alta | Directa; router → rutas SPA | Runtime. Los tres avisos identificados afectan SSR. Esta aplicación se construye como SPA, sin servidor Angular SSR; esas condiciones no están presentes. Sigue siendo necesario mantener el router corregido. | Router y framework coherentes 22.2.3; avisos requieren al menos 22.2.0 / 22.2.1 | A; componente runtime, D condicionado a SSR |
| `@angular/build` 22.1.8, crítica | Directa de desarrollo → piscina / Vite | La severidad crítica procede de piscina; no es una segunda ejecución remota independiente del builder. Build y servidor de desarrollo, fuera del bundle de negocio. | 22.2.4 y Vite corregido por override | A + C |
| `piscina` 5.2.0, crítica | Transitiva: build → piscina | Pool de workers Node. El ataque requiere contaminación de prototipos que alcance opciones de workers. No se identificó esa entrada desde formularios HTTP de la SPA hacia el proceso de build. No se acepta dejarlo vulnerable por ese motivo. | 5.3.2 en lock | A + C |
| `@angular/cli` 22.1.8, alta | Directa de desarrollo → MCP SDK / Zod | CLI y herramientas, no AuthService ni backend. La app no expone un servidor MCP. | 22.2.4 | A + C |
| `@modelcontextprotocol/sdk` 1.30.0, alta | Transitiva del CLI | Avisos de OAuth/redirecciones, sesiones/tasks, validación y audiencia del servidor MCP requieren utilizar los componentes afectados. No describen el login Keycloak de esta SPA. | El CLI nuevo sustituye la rama SDK 1.x por `@modelcontextprotocol/server` 2.3.1; SDK 1.x ya no está en el árbol. La rama 1.x requería 1.32.0 para abarcar el conjunto de avisos. | A + C |
| `zod` 4.4.3, alta | Transitiva: CLI / herramientas MCP → zod | Agotamiento de memoria con colecciones sobredimensionadas. Validación del tooling, no contrato Bean Validation de Spring. | 4.6.6 | A + C |
| `vite` 8.1.5, alta | Transitiva: build → vite; Vitest también utiliza Vite | Servidor dev, editor y restricciones de archivos. Algunos vectores dependen de Windows (no es este host); otros afectan configuraciones Linux o middleware. Escuchar en loopback reduce exposición, pero no reemplaza el parche. | 8.3.3 mediante `overrides.vite`; builder 22.2.4 aún fijaba 8.3.0 vulnerable | A + C |

Referencias primarias: [Router SSR / parámetros](https://github.com/advisories/GHSA-ff3f-86qr-9cv3),
[Router outlets](https://github.com/advisories/GHSA-62vg-58rm-qff7),
[Router query params](https://github.com/advisories/GHSA-57xq-rjx2-v5xh),
[piscina](https://github.com/advisories/GHSA-67c8-pqhq-4rmx),
[MCP OAuth](https://github.com/advisories/GHSA-6qxp-vccf-f47h),
[MCP tasks](https://github.com/advisories/GHSA-22jm-h49p-29qw),
[MCP redirects](https://github.com/advisories/GHSA-6prh-2h8m-c8cw),
[MCP validación](https://github.com/advisories/GHSA-46jr-4wx8-v453),
[MCP audiencia](https://github.com/advisories/GHSA-rvq5-wwqv-78pq),
[Zod](https://github.com/advisories/GHSA-mrhx-c7xc-m3w5).
Para Vite: [archivos Linux](https://github.com/advisories/GHSA-rq7h-c2jc-7f22),
[Windows/editor](https://github.com/advisories/GHSA-587h-7wmj-h5pm),
[primeros bytes](https://github.com/advisories/GHSA-vfpm-58rq-9qcg).
El JSON de auditoría contiene además los avisos de editor y middleware del mismo grupo.

## Resolución y regresión

Se evaluaron las actualizaciones antes de aplicarlas. Angular permanece en 22.x;
framework/compiler-cli 22.2.3, CLI/build 22.2.4, Material/CDK 22.2.0, TypeScript 6.0.3.
La instalación sobre el lock antiguo produjo ERESOLVE. Se resolvió el manifiesto
compatible en una carpeta temporal sin el árbol anterior, con peers estrictos,
y se comprobó el lock resultante con `npm ci --ignore-scripts` en el proyecto.
No se emplearon `--force`, `--legacy-peer-deps` ni `npm audit fix`.

La resolución intermedia dejó Vite 8.3.0 y dos paquetes señalados (Vite y su padre).
El override 8.3.3 es un parche de la misma rama 8.3. El aviso automático proponía
retroceder el builder; no se siguió esa propuesta. Se probó la resolución del
parche, el build, Vitest y `ng serve` real con los flujos E2E.

Resultado: build de producción correcto, **105/105 tests**, árbol `npm ls --depth=0`
sin peers inválidos y auditoría final cero en todas las severidades.
Bundle inicial 330.81 kB; transferencia estimada 92.64 kB, sin warning de budgets.
Los chunks de checkout, pedidos, compras y reportes siguen siendo lazy.
Vitest permanece 4.1.12; jsdom resuelto 28.1.0 ya figuraba en el lock inicial.

## Revisión de uso

Angular common/core/forms/router/platform-browser se usa en componentes, formularios,
rutas y bootstrap; compiler acompaña la familia framework. Material/CDK se usa en
los dialogs y foco; RxJS en HttpClient y composición de peticiones; tslib suministra
helpers de TypeScript. CLI/build/compiler-cli/TypeScript sostienen build y pruebas;
Vitest/jsdom están configurados para unit tests. Prettier permanece como herramienta
de formato; no es una dependencia de negocio ni se elimina por no tener un import.
No se identificó una eliminación necesaria y segura de dependencias directas.

Riesgo residual BAJO: mantener el override Vite y retirarlo cuando una actualización
compatible del builder incorpore una versión corregida, tras repetir build/tests/audit.
Cero avisos npm no constituye una auditoría exhaustiva de todos los artefactos del
backend o de imágenes Docker; esas versiones no se actualizaron por estética.
