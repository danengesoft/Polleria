# Verificación frontend — Fase 13

Informe completo y archivos en
[VERIFICACION-FASE-13 backend](../polleria-backend/VERIFICACION-FASE-13.md).

- `/interno/delivery`: ADMINISTRADOR/CAJERO asignan y reasignan.
- `/interno/reparto`: REPARTIDOR activa perfil y solo gestiona sus entregas.
- Flujo real EFECTIVO: ENTREGADO/PAGADO. YAPE permanece PENDIENTE al entregar y
  solo cambia con confirmación interna explícita, comprobada desde CAJERO.
- NO_ENTREGADO exige observación y permite reasignar al segundo repartidor,
  conservando la incidencia y sin cancelar ni retroceder el Pedido.
- CLIENTE observa DESPACHADO/ENTREGADO desde Mis pedidos, sin acceso a Delivery.
- Build correcto, 73 pruebas correctas (60 anteriores + 13 nuevas).
- Reparto revisado a 390/768/1440 px sin overflow, consola del flujo final sin errores.
- npm audit: 5 altas y 2 críticas pendientes; sin force ni cambios de versiones.

Evidencias sin secretos: `http-preparacion-fase-13.json`,
`http-verificacion-fase-13.json`, `infra-verificacion-fase-13.json`,
`npm-audit-fase-13.json`.

No se implementó Fase 14, Abastecimiento, GPS ni mapas. Git lo administra el usuario.
