# Verificación Fase 8 — CRUD Angular de Catálogo y Clientes

Ejecución local: 24 de octubre de 2026, America/Lima (25 de octubre UTC).
Raíz `/home/systemseng/Polleria`, frontend `polleria-frontend`.

## Alcance y referencias

Se consultó S08 antes de implementar:
https://github.com/262ciclo4/bomerp/blob/main/docs/lp2/sesiones/S08_CRUD_Tablas_Dependientes.md

Se conserva la base S07: standalone, zoneless, Router, lazy loading, HttpClient,
ApiService y único interceptor X-Trace-ID. El backend no se modificó.
Se implementaron solo Categoria, Producto y Cliente dentro del portal interno.
No hay Pedido frontend, carrito, checkout, Keycloak, guards, JWT, Delivery,
Abastecimiento, pagos, Nginx ni reportes visuales.

## Versiones

- Angular framework 22.1.6, CLI/Build 22.1.8: conservados.
- Material/CDK 22.2.0: instalados explícitamente, peers compatibles con Angular 22.
- TypeScript 6.0.3, Node 24.18.0, npm 11.16.0: conservados.
- Vitest 4.1.12: runner existente; no se agregaron scripts de pruebas.

## Archivos creados

Cada carpeta de recurso contiene seis archivos:

| Carpeta bajo src/app/features | Archivos |
| --- | --- |
| catalogo/categoria | categoria.model.ts, categoria-service.ts, categoria-list.ts, categoria-list.html, categoria-form.ts, categoria-form.html |
| catalogo/producto | producto.model.ts, producto-service.ts, producto-list.ts, producto-list.html, producto-form.ts, producto-form.html |
| clientes/cliente | cliente.model.ts, cliente-service.ts, cliente-list.ts, cliente-list.html, cliente-form.ts, cliente-form.html |

Otros archivos nuevos:

- `src/app/features/crud.spec.ts`: 14 pruebas útiles del contrato y componentes.
- `src/app/shared/http-error.ts`: mensajes seguros del contrato HTTP.
- `src/app/shared/form-validation.ts`: validación de texto y precio, mensajes.
- `src/app/shared/confirmar-eliminacion.ts`: diálogo reutilizado por los tres recursos.
- `src/material-theme.scss`: tema Material local sin fuentes remotas.
- `npm-audit-fase-8.json` y `npm-audit-runtime-fase-8.json`: auditorías actuales.
- Este documento.

## Archivos modificados

- package.json / package-lock.json: Material y CDK 22.2.0, sin cambios de familia Angular.
- angular.json: incorporación del tema.
- src/app/app.routes.ts: nueve rutas lazy dentro de Interno.
- src/app/core/layout/interno/interno.html: enlaces reales Categorías, Productos y Clientes.
- src/styles.css: tablas, formularios, estados, móvil y compatibilidad con botones Material.
- README.md: arquitectura y comandos actuales.

Las cuatro pruebas anteriores de app.spec.ts permanecen intactas.
No se modificaron archivos backend ni configuración de Oracle.

## Rutas

| Recurso | Listado | Nuevo | Edición |
| --- | --- | --- | --- |
| Categoría | /interno/categorias | /interno/categorias/nuevo | /interno/categorias/:id/editar |
| Producto | /interno/productos | /interno/productos/nuevo | /interno/productos/:id/editar |
| Cliente | /interno/clientes | /interno/clientes/nuevo | /interno/clientes/:id/editar |

Siguen disponibles `/`, `/interno` y `/interno/conexion`.

## Contratos, formularios y estados

Modelos request/response con interfaces TypeScript, sin `any`. Producto distingue
`categoriaId` del request y `categoria` resumida del response. Las consultas se
centralizan en CategoriaService, ProductoService y ClienteService usando ApiService.

Los tres formularios usan Reactive Forms. Las consultas HTTP y estados se reflejan
con signals. Las suscripciones de componentes terminan al destruirlos; las consultas
de filtro anteriores se cancelan para que una respuesta tardía no pise la selección.
Durante el guardado se deshabilita el formulario y se impiden envíos duplicados.
Los fallos de carga de un recurso de edición no permiten crear por accidente.

Validaciones relevantes:

- Categoría: nombre obligatorio sin solo espacios, máximo 80; descripción máximo 500.
- Producto: nombre máximo 120, descripción máximo 500; precio > 0, hasta 8 enteros y
  2 decimales, sin operaciones aritméticas; categoría numérica obligatoria; imagen URL
  HTTP(S) opcional hasta 1000. No hay stock, inventario ni upload.
- Cliente: nombres y apellidos obligatorios hasta 100; teléfono String con exactamente
  `^(\+51)?[0-9]{7,9}$`; correo opcional hasta 254; dirección/referencia hasta 300.
- Opcionales vacíos se envían como null. La edición conserva los valores existentes.

ProductoForm carga opciones mediante CategoriaService y preselecciona el ID actual.
Sin categorías no permite guardar y ofrece enlace para crear una. Si el backend
rechaza una categoría desaparecida con 404, muestra el mensaje y recarga las opciones.
Las categorías inactivas se identifican en la selección, sin inventar una prohibición
que el contrato backend no impone.

ProductoList muestra el nombre de categoría incluido en la respuesta, sin consultas
por fila. El filtro usa `?categoriaId=...`; “Todas” no envía el parámetro. CurrencyPipe
presenta `S/ 20.00`. Disponible/No disponible no significa stock.

400, 404 y 409 muestran el campo message seguro del GlobalExceptionHandler. Un 500
no expone su contenido interno. La desconexión permite reintentar. No hay JSON crudo,
SQL ni stack traces en pantalla. El único interceptor sigue siendo el de Fase 7.

Todos los DELETE requieren confirmación Material. Cancelar no envía DELETE; un 409
conserva el registro. No se crean endpoints activar/desactivar: estados mediante PUT.

## Pruebas automatizadas

Comando existente: `npm test -- --watch=false`.
Resultado final: **18 pruebas exitosas, 2 archivos** (4 anteriores + 14 nuevas).

Las nuevas pruebas comprueban:

1. Cinco operaciones de CategoriaService y URL central.
2. ProductoService: CRUD, filtro, ausencia de parámetro en Todas y request sin entidad.
3. ClienteService: CRUD, teléfono String y correo nulo.
4. Categoría: espacios inválidos, bloqueo durante guardado y 409 recuperable.
5. Lista de productos: categoría, moneda y filtro en servidor.
6. Edición de producto: categoría numérica y opcionales preservados.
7. Precio inválido y categoría sin seleccionar.
8. Ausencia de categorías y enlace de creación.
9. Dependencia desaparecida: 404 y recarga de opciones.
10. Teléfono exacto y correo opcional/inválido.
11. Edición de Cliente conservando dirección/referencia.
12. Recurso de edición inexistente.
13. Cancelación de eliminación y conflicto que conserva el producto.
14. Mensajes 400/404/409, desconexión y ocultación de detalles de 500.

Build final: `npm run build` correcto; sin warnings de presupuestos.
El servidor real se ejecuta con `npm start -- --host localhost --port 4200`.

## Prueba funcional real desde Angular

Se utilizó el navegador contra localhost:4200 y Spring Boot real en localhost:8080,
con datos persistidos en Oracle. No se sustituyeron las API por mocks en estos flujos.

### Categoría

- Pollos existente, id 24, visible.
- Alta temporal id 126: Temporal F8, descripción Verificación Angular.
- Editada a Temporal F8 editada e inactiva; persistencia visible al volver al listado.
- Alta duplicada rechazada: “Ya existe una categoría con ese nombre” (contrato 409).
- Con producto temporal asociado, DELETE confirmado rechazado:
  “No se puede eliminar una categoría utilizada; puede desactivarla”. Registro conservado.
- Eliminada desde interfaz después de eliminar su producto; listado final solo Pollos.

### Producto

- 1/4 Pollo id 10 visible, categoría Pollos, precio S/ 20.00.
- Alta temporal id 94 mediante selector de categoría, precio S/ 25.50.
- Edición con categoría preseleccionada; precio S/ 26.75 y No disponible persistidos.
- Filtro por categoría temporal muestra solo su producto; filtro Pollos/Todas funciona.
- Precio 0 y categoría sin elegir muestran errores visibles sin crear registro.
- Eliminación confirmada del temporal; listado final conserva 1/4 Pollo.

### Cliente

- Alta id 170 con teléfono +51987654321, correo fase8@example.com, dirección/referencia.
- Correo duplicado rechazado con “Ya existe un cliente con ese correo” (contrato 409).
- Alta id 171 sin correo y con teléfono de 7 dígitos, correcta.
- Edición del primero, estado Inactivo y opcionales precargados/preservados.
- Cancelación del diálogo conserva el Cliente.
- Eliminación confirmada de ambos temporales; listado final vacío.

### Regresión y responsive

- Navegación real entre listas, formularios, diagnóstico y área pública.
- Refresco directo de rutas internas correcto.
- Pantallas comprobadas a 390, 768 y 1440 px; sin desbordamiento horizontal.
- En móvil, tablas con filas etiquetadas; formularios de Producto y Cliente utilizables.
- Tema Material sobrio, tipografía local del sistema, sin descarga de fuentes.
- Diagnóstico real: HTTP 200 y Trace ID `65e87370-321e-4c63-a7a4-e1dfb312397b`.
  Esto comprueba HttpClient, CORS y exposición de X-Trace-ID, sin nuevo mecanismo.
- Área pública sigue mostrando su inicio mínimo.
- Consola final consultada: sin errores ni advertencias relevantes.

## Backend y datos al terminar

Sin cambios de código backend. No se repitió la suite de 75 pruebas: el usuario la
requiere si hay cambios backend; no se afirma una nueva ejecución de dicha suite.

Comprobaciones reales finales:

- Actuator UP, componente Oracle db UP.
- OpenAPI `/v3/api-docs` responde correctamente.
- GET Categorías: solo Pollos id 24, activo true.
- GET Productos: solo 1/4 Pollo id 10, precio 20, disponible true, categoría Pollos.
- GET Clientes: [].
- Los cuatro registros temporales de esta fase fueron eliminados desde Angular.
- No se tocaron contenedores, volúmenes, esquema ni recursos ajenos.

## Auditoría npm y warnings

Resultado sin cambios respecto de Fase 7: **7 hallazgos: 5 altos y 2 críticos**.
Paquetes señalados: @angular/build (crítico), piscina (crítico), @angular/cli,
@angular/router, @modelcontextprotocol/sdk, vite y zod (altos).

Omitiendo dev: 2 altos, @angular/router y zod. Los avisos de Router describen
escenarios SSR; esta SPA no utiliza SSR. No se afirma que todos los hallazgos sean
explotables ni que estén corregidos. Material/CDK no añaden un hallazgo en este reporte.
Las soluciones que propone npm requieren salir de las versiones académicas fijadas.
No se ejecutó `npm audit fix --force`, ni upgrades incompatibles ni overrides.
La revisión definitiva sigue pendiente antes de despliegue.

npm conserva avisos allowScripts de @parcel/watcher, esbuild, lmdb y msgpackr-extract.
No se aprobó ejecución adicional de scripts para silenciarlos. Build, pruebas y servidor
funcionan. No se hizo limpieza general de warnings backend.

## Adaptaciones de S08

- Dominio POLLERIA: disponible, descripción, activo e imagenUrl; sin Marca ni stock BOMERP.
- Cliente sigue el mismo patrón por petición expresa, con el contrato Java existente.
- Rutas dentro de `/interno`, preservando los dos layouts de Fase 7.
- Se usa MatSelect con valores numéricos; autocomplete opcional no es necesario con
  esta lista corta. Material en inputs, selectores, botones y confirmación; tablas
  HTML como en la sección 3.11, con adaptación móvil.
- Diálogo Material en lugar de confirm nativo: la confirmación nativa bloqueó la
  automatización del navegador. El diálogo final se verificó con Cancelar y Confirmar.
- Tema Material local con fuente del sistema para evitar depender de Roboto remoto.
- No se produjeron PDF ni capturas de entrega, por indicación del usuario.

## Git y cierre

Solo inspección con git status/diff. Sin add/commit/push/pull/merge/rebase/reset.
No se modificó el backend. No se inició Fase 9.

Mensaje de commit sugerido:

`feat: implementar CRUD Angular de catálogo y clientes con formularios reactivos`
