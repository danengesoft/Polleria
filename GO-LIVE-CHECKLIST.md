# POLLERIA — Barreras de go-live

**No ejecutado.** Aprobar la preparación técnica no autoriza limpiar ni publicar.

- [ ] AUTORIZACIÓN DEL PROPIETARIO PARA LIMPIAR DATOS
- [ ] AUTORIZACIÓN DEL PROPIETARIO PARA PUBLICAR
- [ ] Servidor elegido, capacidad/disco comprobados y acceso administrado autorizado.
- [ ] Dominio real aprobado, DNS revisado; sin cambios DNS antes de orden explícita.
- [ ] Backup final Oracle y PostgreSQL, hashes, copia externa cifrada y prueba de recuperación acordada.
- [ ] Inventario de las doce tablas comparado con [inventario F18](production/inventario-datos-fase-18.json).
- [ ] Decisión por IDs sobre datos a conservar/borrar; no asumir que Pollos o1/4 Pollo se conservan ni se eliminan.
- [ ] Manifiesto de limpieza aprobado, conteos esperados, orden FK y transacción revisados.
- [ ] Solo tras autorización:adaptar/ejecutar limpieza aprobada y verificar integridad antes de COMMIT.
- [ ] Usuarios dev A/B de las cinco funciones inventariados y decisión de eliminar/recrear/rotar aprobada.
- [ ] Cuentas reales de negocio y administrador con credenciales únicas; vínculo keycloakSub preservado.
- [ ] Bootstrap de KC revisado y acceso administrativo privado definido; consola no publicada.
- [ ] Entorno production0600, contraseñas aleatorias diferentes; ningún secreto en Git/build/Angular.
- [ ] Certificado del dominio real válido, cadena correcta, clave privada0600, renovación definida.
- [ ] Retirar certificados autofirmados locales del procedimiento de publicación.
- [ ] PUBLIC_APP_URL HTTPS, APP_DOMAIN e issuer exactos; no localhost en URLs públicas.
- [ ] Redirect URI, logout URI y webOrigin exactos del cliente; PKCE S256, password grant deshabilitado.
- [ ] CORS del origen real verificado; ningún wildcard.
- [ ] Revisión autorizada de barrera BIND_ADDRESS/preflight para entrada pública.
- [ ] Firewall del servidor y reglas Docker:solo80/443 y SSH administrado; no DB/Spring/KC internos.
- [ ] Imágenes de release inmutables y anterior conservada; digests documentados.
- [ ] Schema/migraciones incrementales aplicados solo si faltan y con backup previo.
- [ ] Health cinco servicios, Oracle/PostgreSQL, Actuator interno y Nginx correctos.
- [ ] HTTP→HTTPS, headers, SPA fallback, API path/Trace ID y docs/Actuator públicos bloqueados.
- [ ] Smoke público:inicio, catálogo, carrito; smoke interno:login, consulta por rol, logout.
- [ ] Refresh/sesión y navegador sin errores; sin logs con tokens/códigos OAuth.
- [ ] Plan de rollback entendido; no restauración automática sobre DB activa.
- [ ] Retención/límites de logs y backups, alertas operativas básicas y responsable inicial definidos.
- [ ] NTP/hora host y calendario Perú revisados sin alterar históricos.
- [ ] Monitoreo inicial de errores, salud, CPU/RAM/disco después de publicación autorizada.

La [plantilla cleanup](scripts/production/cleanup-plan.sql) rechaza ejecución por
defecto y no contiene DELETE operativo. El DML final depende de IDs/decisiones que
el propietario todavía no proporcionó. No usar TRUNCATE/DROP ni reset de identidades.
No borrar datos como requisito para terminar Fase18.
