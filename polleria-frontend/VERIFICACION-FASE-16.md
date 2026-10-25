# FASE 16 COMPLETADA — Diseño visual final y responsive global

Verificación real del 25/10/2026, America/Lima. Cambios exclusivos de frontend y documentación de esta fase. No se inicia Fase17 ni despliegue.

## Resumen técnico solicitado

1. **Archivos principales:** styles.css, material-theme.scss, layouts público/interno, navegación compartida, Inicio, estilos públicos/Pedidos/Delivery/Abastecimiento/Reportes, plantillas de checkout/perfil/compra/inventario/detalle y presentación de acceso denegado. Inventario exacto al final.
2. **Componentes visuales:** EstadoBadge (`app-estado`) para estado de Pedido/pago/Entrega; SeguimientoPedido (`app-seguimiento-pedido`) con pasos según modalidad; ConfirmarVaciado dentro de carrito.ts, utilizando MatDialog existente. Navegación existente ampliada; no nuevo framework de componentes.
3. **Paleta:** CSS custom properties centralizadas: fondo cálido neutro, superficie blanca, texto carbón, secundario, acento terracota, éxito/advertencia/error/info, superficies suaves, bordes y on-accent. Tema Material usa estos tokens mediante las APIs estables ya utilizadas. No marca/logotipo definitivo, gradientes, neón ni imágenes comerciales inventadas.
4. **Tipografía y ritmo:** system-ui con fallback del sistema; ninguna fuente remota. Jerarquías comunes de página/sección/metadata/labels, medidas moderadas y espaciado consistente. Controles nativos y Material cómodos de tocar, mínimo44px en botones; formularios con anchura máxima y gaps comunes.
5. **Layout público:** header con nombre neutro Pollería, navegación real, contenido y footer sencillo. Se diferencia del portal operativo por la presentación comercial y el espacio de lectura.
6. **Navegación pública:** Inicio/Catálogo/Carrito visibles; menú responsive para sesión y enlaces adicionales. CLIENTE ve Mis pedidos/Perfil; internos pueden acceder al Portal. Nombre discreto, roles solo en contexto interno. Sin enlaces inexistentes ni permisos nuevos.
7. **Inicio:** bienvenida y CTA Ver catálogo, con tres pasos que describen funciones reales. Sin dirección, horarios, teléfono, reseñas, ofertas ni promesas comerciales inventadas.
8. **Catálogo:** grid3/2/1 columnas según ancho, categoría compacta, precio, descripción, botón principal; no disponible escrito y botón deshabilitado. Placeholder CSS neutro para imagen null/URL inválida y fallback reactivo tras error; se eliminó la asignación directa hidden sobre el elemento de imagen. No descargas de imágenes aleatorias.
9. **Carrito:** líneas con producto/precio/cantidad/controles/subtotal/quitar, total estimado, checkout como acción principal y vaciado secundario con confirmación. Estado vacío con acceso a catálogo. No cambian cálculos ni límites de cantidad existentes.
10. **Checkout:** secciones Pedido, Tipo de entrega/dirección, Método de pago y Resumen/Confirmar. Campos Delivery condicionados como antes, Recojo sin validación de dirección visible. Botón bloqueado durante envío, sin dobles envíos. Solo se añadió estado de navegación para mostrar confirmación después del éxito; payload/contrato permanecen idénticos.
11. **Mis pedidos:** lista comercial compacta con número/fecha/tipo/estado/pago/total. Detalle con confirmación, importes reales y progresión visual sin inventar fechas o ubicación.
12. **Perfil:** formulario dividido en contacto y dirección habitual, ancho compacto. No muestra sub, JWT ni roles técnicos.
13. **Estados:** badges textuales reutilizados en listas/detalles públicos e internos y Delivery; colores semánticos con texto siempre presente. Seguimiento identifica un único estado actual, excluye Despachado en Recojo y muestra cancelación sin simular una progresión exitosa. No se cambiaron transiciones ni estados comerciales.
14. **Layout interno:** header discreto, sidebar de escritorio, superficie de trabajo con columnas minmax(0,1fr); acciones/títulos de página coherentes. Detalle de Pedido separa Pedido/Cliente/Entrega/Pago/Productos, sin información duplicada innecesaria.
15. **Navegación interna responsive:** menú del portal colapsado en tablet/móvil, abierto por botón con aria-expanded/controls; cierre al seleccionar y Escape. Al cerrar con Escape se devuelve foco al botón visible mediante referencia de plantilla Angular: única operación imperativa de foco, necesaria para no ocultar el control enfocado. Sin queries/manipulación estructural del DOM ni listeners manuales de resize.
16. **CRUD:** base común para formularios Categoría/Producto/Cliente y controles de Insumo/Proveedor; Guardar principal, acciones secundarias discretas, validación/mensajes existentes preservados. Material continúa instalado y temado; no Bootstrap/Tailwind/otra biblioteca.
17. **Pedidos internos:** tabla mantiene fecha/cliente/tipo/estado/pago/total/acciones, sin líneas completas; FormArray de Pedido y cabecera diferenciados, responsive. No cambia la operación transaccional.
18. **Delivery:** formulario de asignación claro y lista de entregas/repartidor/estado; reasignación permanece donde corresponde. Badges y acciones coherentes, sin mapa ni rendimiento nuevo.
19. **Repartidor:** vista móvil prioritaria, bloques con cliente/teléfono/dirección/referencia/total/pago/estado y acciones condicionadas existentes. Teléfono como texto seleccionable, sin integración WhatsApp/llamadas. Controles visibles medidos >=44px en390; la entrega histórica #1737 sigue entregada y no se modificó.
20. **Abastecimiento:** formularios/estilos comunes de Insumos/Proveedores/Compras/Inventario. Compra distingue proveedor de detalle, muestra total; Salida y Ajuste tienen título y explicación distintos. Stock permanece visible con unidad. Sin escrituras de inventario durante las pruebas visuales.
21. **Reportes:** jerarquía de secciones, filtros claros, superficies/controles coherentes y números tabulares; scroll interno de tablas. Sin métricas, endpoints ni cálculos nuevos.
22. **Loading:** texto de estado y botones deshabilitados existentes con presentación uniforme, signals conservados; sin spinners/animaciones añadidas ni consultas disparadas desde plantillas.
23. **Errores:** tratamiento comercial existente, presentación semántica uniforme; sin JSON/SQL/stack trace. Acceso denegado tiene bloque claro y retorno al Inicio. Se verificó Pedido inexistente mostrando “Pedido no encontrado”; lógica401/403/refresh de Fase11 permanece intacta.
24. **Vacíos:** carrito con CTA y listas con mensajes útiles conservados/pulidos; tablas con vacíos centrados, reportes con ceros/mensaje por subsección. Sin NaN/undefined.
25. **Diálogos:** se conserva MatDialog para eliminar/cancelar/entregar/incidencia; tema y botones comunes. Vaciado de carrito utiliza el mismo mecanismo, con Conservar/Vaciar y foco inicial seguro. Probados rechazo y aceptación reales sin generar otro Pedido.
26. **Accesibilidad básica:** labels, captions, nombres de botones, foco visible, Escape, aria-expanded/controls, aria-current para página/paso y texto en estados. Contraste calculado entre tokens: texto14.31:1, secundario5.43:1, acento/botón7.07:1, éxito6.33:1, advertencia6.13:1, error6.18:1. No se presenta como auditoría WCAG completa.
27. **390px:** las19 pantallas mínimas pasan, sin overflow horizontal global; menús colapsados y controles táctiles. Tablas administrativas adaptadas a bloques con labels; reportes conservan scroll interno.
28. **768px:** mismas19 pantallas correctas, navegación colapsable y grids ajustados; sin overflow global.
29. **1440px:** mismas19 pantallas correctas, sidebar y tablas de escritorio; sin overflow global. También se revisaron formularios Categoría/Producto/Cliente/Pedido y variantes de checkout/carrito vacío.
30. **Flujo público real:** anónimo Inicio→Catálogo→agregar1/4 Pollo→Carrito→loginCLIENTE→Checkout→confirmación Pedido#3223→Mis pedidos→Detalle. Un único Pedido nuevo, Recojo/Efectivo, totalS/20.00, estadoPENDIENTE y pagoPENDIENTE. Dirección Delivery también se visualizó sin enviar otro pedido.
31. **ADMINISTRADOR:** login real, portal y todas sus pantallas; enlaces y menú móvil correctos. Reportes y Abastecimiento accesibles, formularios revisados sin guardar registros adicionales.
32. **CAJERO:** no ve Nuevo producto/Eliminar ni Reportes/Abastecimiento; URL directa a Reportes termina en Acceso denegado. Conserva Catálogo/Clientes/Pedidos/Delivery autorizados.
33. **REPARTIDOR:** solo navegación de su área, Mis entregas con datos reales, revisión390/768/1440 y controles táctiles. No se enviaron cambios de estado operativos.
34. **ABASTECIMIENTO:** navegación exclusiva de su módulo y diagnóstico, acceso mediante enlaces a Insumos/Compras y Compra#7; sin áreas administrativas ajenas.
35. **CLIENTE:** sin enlace Portal interno, puede usar Catálogo/Carrito/Checkout/Perfil/Mis pedidos; login/logout reales, carrito limpiado tras creación y tras prueba de diálogo. Ninguna credencial se imprime.
36. **npm run build:** correcto. Bundle inicial331.44kB (aprox92.71kB transferidos), bajo warning500kB/error1MB. Lazy loading conservado, sin dependencia/fuente/imagen pesada nueva.
37. **Pruebas frontend:** ejecución completa `npm test -- --watch=false`, sin fallos ni errores no controlados.
38. **Cantidad:**105 pruebas correctas, nueve archivos. Se mantienen98 anteriores y se añaden7 para menú público/Escape/foco, menú portal/cierre, permisos en menú colapsado, seguimiento según modalidad, cancelación sin pasos falsos, confirmación de vaciado y fallback de imagen. Un assert existente de navegación se actualiza para la confirmación visual; no se reescribe la suite.
39. **Consola:** vacía para warn/error en la comprobación real, sin errores Angular/Material/CORS, tokens ni bucles. La consulta de recurso inexistente fue intencional y mostró error comercial seguro. No se observaron recargas constantes; las consultas se mantienen en carga/acciones existentes. No se afirma una auditoría exhaustiva de Network.
40. **Warnings de build:** ninguno al cierre. Se detectó exceso CSS inicial de28bytes y se redujo sin ampliar budgets. Compilación/pruebas finales limpias; no se alteró angular.json para ocultar advertencias.
41. **npm audit:**7 hallazgos conocidos:5 altos y2 críticos. Sin npm audit fix --force, sin cambio de versión Angular/dependencias/lockfile.
42. **Backend:** cero archivos modificados. Contratos/reglas/estados/seguridad/configuración intactos; ActuatorUP, OpenAPI POLLERIA API y catálogo HTTP200. No se repitieron las198 pruebas Maven de Fase15, conforme a la instrucción75 para cambios exclusivamente Angular/CSS. Los cinco módulos comerciales y las12 tablas permanecen; sin migración nueva.
43. **Desviaciones:** ninguna funcional. Se eligieron menús colapsables simples en lugar de añadir un drawer complejo, placeholder CSS sin fotografías y layout adaptado para tablas. Restaurar foco mediante referencia Angular es una necesidad de accesibilidad, no manipulación del DOM para el diseño. No se detectó necesidad de corregir contratos/backend.
44. **Alcance conservado:** no se agregó funcionalidad comercial, módulos, tablas, métricas, recetas, pagos reales, GPS ni despliegue. Oracle confirma que se conservaron historial de Pedidos, dos Entregas, Compra#7/S43.99 y cuatro Movimientos. Stock84=1.250L y85=3.125KG intacto; Pedidos pasa8→9 únicamente por el flujo controlado. Sin eliminación de historial. Git usado solo para status/diff de lectura.
45. **Commit sugerido, no ejecutado:** `style(frontend): unificar diseño visual y responsive global`.

## Matriz mínima real de responsive

Las mediciones se realizaron en navegador con contenido real cargado. Se verificó `document.documentElement.scrollWidth <= innerWidth`. Los wrappers de reportes pueden desplazarse internamente sin desplazar la página completa. La evidencia JSON conserva mediciones adicionales/repetidas y casos de formulario/estado vacío.

| Pantalla |390px|768px|1440px|
|---|---|---|---|
|Inicio público|correcto|correcto|correcto|
|Catálogo público|correcto|correcto|correcto|
|Carrito con producto|correcto|correcto|correcto|
|Checkout Recojo|correcto|correcto|correcto|
|Perfil cliente|correcto|correcto|correcto|
|Mis pedidos|correcto|correcto|correcto|
|Detalle pedido cliente|correcto|correcto|correcto|
|Categorías cargadas|correcto|correcto|correcto|
|Productos cargados|correcto|correcto|correcto|
|Clientes cargados|correcto|correcto|correcto|
|Pedidos con datos|correcto|correcto|correcto|
|Detalle interno cargado|correcto|correcto|correcto|
|Delivery con entregas|correcto|correcto|correcto|
|Reparto móvil con entrega|correcto|correcto|correcto|
|Insumos con stock|correcto|correcto|correcto|
|Proveedores con datos|correcto|correcto|correcto|
|Compras con historial|correcto|correcto|correcto|
|Inventario con movimientos|correcto|correcto|correcto|
|Reportes cargados|correcto|correcto|correcto|

## Evidencias locales

- `verificacion-navegador-fase-16.json`: matriz mínima y mediciones, roles, flujo público, diálogo, recurso inexistente, teclado/foco, contraste y consola.
- `verificacion-integracion-fase-16.json`: Oracle antes/después, Pedido#3223/detalle, stock/historial conservado y HTTP/Actuator.
- `npm-audit-fase-16.json`: auditoría de dependencias sin cambios automáticos.
- Logs ignorados: frontend-build-fase-16.log y frontend-test-fase-16.log.

## Inventario exacto de archivos

Rutas relativas a `/home/systemseng/Polleria`. No hay archivos backend en este inventario.

### Creados

- `polleria-frontend/VERIFICACION-FASE-16.md`
- `polleria-frontend/npm-audit-fase-16.json`
- `polleria-frontend/src/app/shared/estado-badge.ts`
- `polleria-frontend/src/app/shared/seguimiento-pedido.ts`
- `polleria-frontend/src/app/shared/visual.spec.ts`
- `polleria-frontend/verificacion-integracion-fase-16.json`
- `polleria-frontend/verificacion-navegador-fase-16.json`

### Modificados

- `polleria-frontend/README.md`
- `polleria-frontend/src/app/core/auth/sin-permiso.ts`
- `polleria-frontend/src/app/core/conexion/conexion.ts`
- `polleria-frontend/src/app/core/inicio/inicio-interno.ts`
- `polleria-frontend/src/app/core/inicio/inicio-publico.ts`
- `polleria-frontend/src/app/core/layout/interno/interno.css`
- `polleria-frontend/src/app/core/layout/interno/interno.html`
- `polleria-frontend/src/app/core/layout/interno/interno.ts`
- `polleria-frontend/src/app/core/layout/publico/publico.css`
- `polleria-frontend/src/app/features/abastecimiento/abastecimiento.css`
- `polleria-frontend/src/app/features/abastecimiento/compras.html`
- `polleria-frontend/src/app/features/abastecimiento/insumos.html`
- `polleria-frontend/src/app/features/abastecimiento/inventario.html`
- `polleria-frontend/src/app/features/abastecimiento/proveedores.html`
- `polleria-frontend/src/app/features/delivery/delivery-list.html`
- `polleria-frontend/src/app/features/delivery/delivery-list.ts`
- `polleria-frontend/src/app/features/delivery/delivery.css`
- `polleria-frontend/src/app/features/delivery/mis-entregas.html`
- `polleria-frontend/src/app/features/delivery/mis-entregas.ts`
- `polleria-frontend/src/app/features/pedidos/pedido-detalle.html`
- `polleria-frontend/src/app/features/pedidos/pedido-detalle.ts`
- `polleria-frontend/src/app/features/pedidos/pedido-list.html`
- `polleria-frontend/src/app/features/pedidos/pedido-list.ts`
- `polleria-frontend/src/app/features/pedidos/pedido.css`
- `polleria-frontend/src/app/features/publico/carrito.ts`
- `polleria-frontend/src/app/features/publico/catalogo-publico.ts`
- `polleria-frontend/src/app/features/publico/checkout.html`
- `polleria-frontend/src/app/features/publico/checkout.ts`
- `polleria-frontend/src/app/features/publico/mi-pedido.ts`
- `polleria-frontend/src/app/features/publico/mis-pedidos.ts`
- `polleria-frontend/src/app/features/publico/perfil.html`
- `polleria-frontend/src/app/features/publico/publico.css`
- `polleria-frontend/src/app/features/publico/publico.spec.ts`
- `polleria-frontend/src/app/features/reportes/reporte-tabla.ts`
- `polleria-frontend/src/app/features/reportes/reportes.css`
- `polleria-frontend/src/app/shared/navigation/app-navigation.ts`
- `polleria-frontend/src/material-theme.scss`
- `polleria-frontend/src/styles.css`

