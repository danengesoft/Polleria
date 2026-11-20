# 08. Exportación e importación lógica con EXP e IMP

Se exporta el esquema `POLLERIA_ADMIN` y se importa en un esquema nuevo, `POLLERIA_IMPORT`.

- Los comandos `docker` se ejecutan en la **terminal de Fedora**.
- Los comandos SQL se ejecutan en **SQL Developer**.
- Cambia `oracle` por la contraseña de SYSTEM si es otra.

## 1. Exportar (terminal de Fedora)

```bash
docker exec -it mi_oracle /opt/oracle/product/21c/dbhomeXE/bin/exp USERID=SYSTEM/oracle@XEPDB1 OWNER=POLLERIA_ADMIN FILE=/tmp/polleria_admin.dmp LOG=/tmp/polleria_admin_exp.log
```

Debe terminar con: `Export terminated successfully without warnings.`
y listar las tablas `CATEGORIAS`, `PRODUCTOS`, `CLIENTES`, `MESAS`, `PEDIDOS`,
`DETALLE_PEDIDOS`, `PAGOS`, `AUDITORIA_PEDIDOS` y las 5 particiones de `PEDIDOS_HIST`.

Copiar el respaldo y el log a Fedora (fíjate en el punto final, sin otros caracteres):

```bash
docker cp mi_oracle:/tmp/polleria_admin.dmp .
```
```bash
docker cp mi_oracle:/tmp/polleria_admin_exp.log .
```
```bash
ls -lh polleria_admin*
```

## 2. Crear el esquema destino (SQL Developer, conexión SYSTEM)

```sql
CREATE USER POLLERIA_IMPORT IDENTIFIED BY "Import2026"
  DEFAULT TABLESPACE TBS_POLLERIA_DATOS
  TEMPORARY TABLESPACE TEMP
  QUOTA UNLIMITED ON TBS_POLLERIA_DATOS
  QUOTA UNLIMITED ON TBS_POLLERIA_HIST
  QUOTA UNLIMITED ON TBS_POLLERIA_IDX;

GRANT CREATE SESSION, CREATE TABLE, CREATE VIEW, CREATE SEQUENCE,
      CREATE PROCEDURE, CREATE TRIGGER
TO POLLERIA_IMPORT;
```

## 3. Importar (terminal de Fedora)

```bash
docker exec -it mi_oracle /opt/oracle/product/21c/dbhomeXE/bin/imp USERID=SYSTEM/oracle@XEPDB1 FROMUSER=POLLERIA_ADMIN TOUSER=POLLERIA_IMPORT FILE=/tmp/polleria_admin.dmp LOG=/tmp/polleria_import.log
```

Debe terminar con: `Import terminated successfully without warnings.`
(si dice "with warnings", revisa el log; los avisos de compilación de objetos
suelen desaparecer al recompilar, ver paso 4).

```bash
docker cp mi_oracle:/tmp/polleria_import.log .
```
```bash
cat polleria_import.log
```

## 4. Verificar (SQL Developer, conexión SYSTEM)

Comparar la cantidad de filas de ambos esquemas:

```sql
SELECT 'PEDIDOS' AS TABLA,
       (SELECT COUNT(*) FROM POLLERIA_ADMIN.PEDIDOS)  AS ORIGEN,
       (SELECT COUNT(*) FROM POLLERIA_IMPORT.PEDIDOS) AS DESTINO FROM DUAL
UNION ALL
SELECT 'DETALLE_PEDIDOS',
       (SELECT COUNT(*) FROM POLLERIA_ADMIN.DETALLE_PEDIDOS),
       (SELECT COUNT(*) FROM POLLERIA_IMPORT.DETALLE_PEDIDOS) FROM DUAL
UNION ALL
SELECT 'CLIENTES',
       (SELECT COUNT(*) FROM POLLERIA_ADMIN.CLIENTES),
       (SELECT COUNT(*) FROM POLLERIA_IMPORT.CLIENTES) FROM DUAL
UNION ALL
SELECT 'PRODUCTOS',
       (SELECT COUNT(*) FROM POLLERIA_ADMIN.PRODUCTOS),
       (SELECT COUNT(*) FROM POLLERIA_IMPORT.PRODUCTOS) FROM DUAL
UNION ALL
SELECT 'PAGOS',
       (SELECT COUNT(*) FROM POLLERIA_ADMIN.PAGOS),
       (SELECT COUNT(*) FROM POLLERIA_IMPORT.PAGOS) FROM DUAL
UNION ALL
SELECT 'PEDIDOS_HIST',
       (SELECT COUNT(*) FROM POLLERIA_ADMIN.PEDIDOS_HIST),
       (SELECT COUNT(*) FROM POLLERIA_IMPORT.PEDIDOS_HIST) FROM DUAL;
```

Objetos importados (tablas, índices, secuencias, triggers, funciones, procedimientos):

```sql
SELECT OBJECT_TYPE, COUNT(*) AS CANTIDAD,
       SUM(CASE WHEN STATUS = 'VALID' THEN 1 ELSE 0 END) AS VALIDOS
FROM DBA_OBJECTS
WHERE OWNER = 'POLLERIA_IMPORT'
GROUP BY OBJECT_TYPE
ORDER BY OBJECT_TYPE;
```

Las particiones se conservan:

```sql
SELECT PARTITION_NAME, TABLESPACE_NAME
FROM DBA_TAB_PARTITIONS
WHERE TABLE_OWNER = 'POLLERIA_IMPORT' AND TABLE_NAME = 'PEDIDOS_HIST'
ORDER BY PARTITION_POSITION;
```

Si algún objeto sale `INVALID`, recompilar el esquema:

```sql
BEGIN
  DBMS_UTILITY.COMPILE_SCHEMA(SCHEMA => 'POLLERIA_IMPORT', COMPILE_ALL => FALSE);
END;
/
```

## Nota

Las tablas usan secuencias + triggers en vez de `GENERATED ... AS IDENTITY`
porque `exp` clásico no exporta columnas IDENTITY (error `EXP-00107`).
Con Data Pump (`expdp` / `impdp` con `REMAP_SCHEMA`) sí se podrían usar.
