# Verificación frontend — Fase 14

Informe completo, archivos, decisiones y pruebas reales en
[VERIFICACION-FASE-14 backend](../polleria-backend/VERIFICACION-FASE-14.md).

- Insumos, Proveedores, Compras e Inventario para ADMINISTRADOR/ABASTECIMIENTO.
- Reactive Forms y FormArray de compra; cantidades/costos decimales preservados;
  contrato sin subtotal/total ni stock elegido desde Angular.
- Flujo real: Compra 7 por S/43.99, dos entradas, salida de 0.625 L, rechazo por
  stock insuficiente y ajuste a 1.250 L; histórico preservado tras renombrar Insumo.
- Datos/listados se actualizan tras éxito; errores conservan formulario para corregir.
- Build correcto y 89 pruebas correctas (73 anteriores + 16 nuevas).
- Cuatro pantallas comprobadas a 390/768/1440 px sin overflow; consola sin errores relevantes.
- npm audit mantiene 5 altas/2 críticas; sin force, dependencias nuevas ni cambio de versiones.

Evidencias sin secretos: `http-verificacion-fase-14.json`,
`infra-verificacion-fase-14.json`, `npm-audit-fase-14.json`.

Sin recetas, relación Producto-Insumo, consumo automático desde Pedidos ni Fase 15.
