# Examen Base de Datos II — Proyecto Pollería

Oracle 21c XE en Docker (`mi_oracle`), servicio `XEPDB1`, SQL Developer.

Ejecutar cada archivo con **F5 (Ejecutar script)**, en este orden y con la conexión indicada:

| # | Archivo | Conexión | Tema de clase |
|---|---|---|---|
| 00 | `00_system_almacenamiento_usuarios.sql` | SYSTEM | Tablespaces, datafiles, usuarios, cuotas, roles |
| 01 | `01_tablas.sql` | POLLERIA_ADMIN | Tablas, PK, FK, UNIQUE, CHECK, secuencias, triggers de ID |
| 02 | `02_datos.sql` | POLLERIA_ADMIN | INSERT de datos base |
| 03 | `03_plsql_funciones_procedimientos_triggers.sql` | POLLERIA_ADMIN | Funciones, procedimientos (IN/OUT), cursor, excepciones, triggers con :NEW/:OLD |
| 04 | `04_pruebas_plsql.sql` | POLLERIA_ADMIN | Una semana de ventas (10 por día) y pruebas de cada regla |
| 05 | `05_particiones_indices_optimizacion.sql` | POLLERIA_ADMIN | Particionamiento por rango, índices locales/globales, DBMS_STATS, EXPLAIN PLAN |
| 06 | `06_seguridad_permisos.sql` | POLLERIA_ADMIN, luego POLL_CAJERO / POLL_ANALISTA / SYSTEM | GRANT, REVOKE, pruebas de privilegios |
| 07 | `07_consultas.sql` | POLLERIA_ADMIN | 18 consultas con JOIN, GROUP BY, subconsultas |
| 08 | `08_exportar_importar.md` | Terminal + SYSTEM | EXP / IMP a otro esquema |
| 99 | `99_limpieza.sql` | SYSTEM | Borrar todo para repetir |

## Conexiones en SQL Developer

Host `localhost`, puerto `1521`, servicio `XEPDB1`.

| Usuario | Contraseña |
|---|---|
| SYSTEM | la de tu contenedor (en clase: `oracle`) |
| POLLERIA_ADMIN | `Polleria2026` |
| POLL_CAJERO | `Cajero2026` |
| POLL_ANALISTA | `Analista2026` |
| POLL_AUDITOR | `Auditor2026` |

## Modelo

```
CATEGORIAS 1:N PRODUCTOS
CLIENTES   1:N PEDIDOS N:1 MESAS (solo pedidos de tipo MESA)
PEDIDOS    1:N DETALLE_PEDIDOS N:1 PRODUCTOS
PEDIDOS    1:1 PAGOS
PEDIDOS    1:N AUDITORIA_PEDIDOS (la llena un trigger)
PEDIDOS_HIST  (particionada por año: P2023, P2024, P2025, P2026, P_FUTURO)
```

## Tablespaces

| Tablespace | Contenido |
|---|---|
| TBS_POLLERIA_DATOS | Tablas operacionales |
| TBS_POLLERIA_HIST | PEDIDOS_HIST (particionada) |
| TBS_POLLERIA_IDX | Índices |

## Objetos PL/SQL

- Funciones: `OBTENER_PRECIO_PRODUCTO`, `OBTENER_STOCK_PRODUCTO`, `PRODUCTO_MAS_VENDIDO`,
  `OBTENER_NOMBRE_CLIENTE`, `TOTAL_VENTAS_DIA`.
- Procedimientos: `REGISTRAR_PEDIDO` (con OUT), `AGREGAR_PRODUCTO_PEDIDO` (calcula base e IGV 18 %),
  `REGISTRAR_PAGO` (cobra, entrega y libera mesa), `REPORTE_VENTAS_DIA` (cursor).
- Triggers: `TRG_DETALLE_STOCK` (valida y descuenta stock), `TRG_PEDIDOS_OCUPAR_MESA`,
  `TRG_PEDIDOS_CONTROL` (impide DELETE, impide cambiar pedidos cerrados, devuelve stock al cancelar
  y registra auditoría), y un trigger de ID por tabla.

## Errores comunes

- Ruta de datafile no válida (ORA-01119): mira la ruta real con la primera consulta del script 00 y cámbiala.
- ORA-01920 / ORA-01543 (usuario o tablespace ya existe): ejecuta `99_limpieza.sql` y empieza de nuevo.
- Objeto INVALID: `SELECT NAME, LINE, TEXT FROM USER_ERRORS;`
- No se ve DBMS_OUTPUT: el script ya trae `SET SERVEROUTPUT ON`; ejecútalo con F5, no con F9.
