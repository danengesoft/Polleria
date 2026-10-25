-- PLANTILLA NO AUTORIZADA. NO EJECUTADA. No basta aprobar la fase para limpiar.
-- Completar un manifiesto de IDs autorizado por el propietario, después del backup.
-- Ejecutar como POLLERIA en FREEPDB1; no SYS. Catálogo queda fuera por defecto.
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK
SET AUTOCOMMIT OFF
DEFINE AUTORIZACION = 'NO'
BEGIN
 IF SYS_CONTEXT('USERENV','CURRENT_SCHEMA') <> 'POLLERIA'
    OR SYS_CONTEXT('USERENV','CON_NAME') <> 'FREEPDB1' THEN
   RAISE_APPLICATION_ERROR(-20001,'Schema/PDB incorrecto');
 END IF;
 IF '&AUTORIZACION' <> 'ORDEN_EXPLICITA_DEL_PROPIETARIO' THEN
   RAISE_APPLICATION_ERROR(-20002,'Limpieza no autorizada');
 END IF;
END;
/
-- 1. Backup completo verificado y conteos previos de las doce tablas.
-- 2. Manifestar IDs aprobados. No seleccionar por nombre Fxx automáticamente.
-- 3. Validar que ningún registro a conservar referencia IDs a borrar.
-- 4. DML en una transacción, sin TRUNCATE ni DROP:
--    ENTREGAS -> DETALLE_PEDIDOS -> PEDIDOS -> CLIENTES / REPARTIDORES.
--    MOVIMIENTOS_INVENTARIO -> DETALLE_COMPRAS -> COMPRAS -> PROVEEDORES.
--    INSUMOS: no borrar movimientos dejando stock inventado. Si se conserva un
--    insumo sin su historial, acordar reinicio de stock y conteo inicial; hacerlo
--    explícitamente en la misma transacción. Nunca limpiar stock parcialmente.
--    PRODUCTOS/CATEGORIAS: decisión separada, solo IDs aprobados sin referencias.
-- 5. Comparar conteos esperados, huérfanos=0, totales y movimientos/stock coherentes.
-- 6. Si cualquier diferencia:ROLLBACK. Sin reset de identity por conveniencia.
-- No contiene DELETE operativo:el DML final depende del manifiesto aún no aprobado.
ROLLBACK;
EXIT
