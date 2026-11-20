-- =====================================================================
-- 00. ALMACENAMIENTO, USUARIOS Y ROLES  (Proyecto Pollería)
-- Conexión en SQL Developer: SYSTEM  /  localhost:1521  /  servicio XEPDB1
-- Ejecutar con F5 (Ejecutar script)
-- =====================================================================

-- 0.1 Ver dónde están los datafiles actuales del PDB.
--     Si la carpeta NO es /opt/oracle/oradata/XE/XEPDB1/, cambia la ruta abajo.
SELECT TABLESPACE_NAME, FILE_NAME, BYTES/1024/1024 AS MB
FROM DBA_DATA_FILES
ORDER BY TABLESPACE_NAME;

-- ---------------------------------------------------------------------
-- 1. TABLESPACES: operacional, histórico e índices
-- ---------------------------------------------------------------------
CREATE TABLESPACE TBS_POLLERIA_DATOS
  DATAFILE '/opt/oracle/oradata/XE/XEPDB1/tbs_polleria_datos01.dbf'
  SIZE 100M AUTOEXTEND ON NEXT 10M MAXSIZE 500M;

CREATE TABLESPACE TBS_POLLERIA_HIST
  DATAFILE '/opt/oracle/oradata/XE/XEPDB1/tbs_polleria_hist01.dbf'
  SIZE 100M AUTOEXTEND ON NEXT 10M MAXSIZE 500M;

CREATE TABLESPACE TBS_POLLERIA_IDX
  DATAFILE '/opt/oracle/oradata/XE/XEPDB1/tbs_polleria_idx01.dbf'
  SIZE 50M AUTOEXTEND ON NEXT 5M MAXSIZE 300M;

-- Verificación
SELECT TABLESPACE_NAME, FILE_NAME, BYTES/1024/1024 AS MB, AUTOEXTENSIBLE, MAXBYTES/1024/1024 AS MAX_MB
FROM DBA_DATA_FILES
WHERE TABLESPACE_NAME LIKE 'TBS_POLLERIA%';

SELECT TABLESPACE_NAME, ROUND(SUM(BYTES)/1024/1024, 2) AS MB_LIBRES
FROM DBA_FREE_SPACE
WHERE TABLESPACE_NAME LIKE 'TBS_POLLERIA%'
GROUP BY TABLESPACE_NAME;

-- ---------------------------------------------------------------------
-- 2. USUARIO DUEÑO DEL ESQUEMA
-- ---------------------------------------------------------------------
CREATE USER POLLERIA_ADMIN IDENTIFIED BY "Polleria2026"
  DEFAULT TABLESPACE TBS_POLLERIA_DATOS
  TEMPORARY TABLESPACE TEMP
  QUOTA UNLIMITED ON TBS_POLLERIA_DATOS
  QUOTA UNLIMITED ON TBS_POLLERIA_HIST
  QUOTA UNLIMITED ON TBS_POLLERIA_IDX;

GRANT CREATE SESSION, CREATE TABLE, CREATE VIEW, CREATE SEQUENCE,
      CREATE PROCEDURE, CREATE TRIGGER
TO POLLERIA_ADMIN;

-- ---------------------------------------------------------------------
-- 3. ROLES (los permisos sobre tablas se dan en el script 06,
--    cuando las tablas ya existen)
-- ---------------------------------------------------------------------
CREATE ROLE ROL_POLLERIA_CAJERO;    -- SELECT, INSERT, UPDATE (sin DELETE)
CREATE ROLE ROL_POLLERIA_ANALISTA;  -- solo SELECT
CREATE ROLE ROL_POLLERIA_AUDITOR;   -- solo SELECT sobre pedidos, pagos y auditoría

-- ---------------------------------------------------------------------
-- 4. USUARIOS POR PERFIL
-- ---------------------------------------------------------------------
CREATE USER POLL_CAJERO   IDENTIFIED BY "Cajero2026";
CREATE USER POLL_ANALISTA IDENTIFIED BY "Analista2026";
CREATE USER POLL_AUDITOR  IDENTIFIED BY "Auditor2026";

GRANT CREATE SESSION TO POLL_CAJERO, POLL_ANALISTA, POLL_AUDITOR;

GRANT ROL_POLLERIA_CAJERO   TO POLL_CAJERO;
GRANT ROL_POLLERIA_ANALISTA TO POLL_ANALISTA;
GRANT ROL_POLLERIA_AUDITOR  TO POLL_AUDITOR;

-- Verificación
SELECT USERNAME, DEFAULT_TABLESPACE, TEMPORARY_TABLESPACE, ACCOUNT_STATUS, CREATED
FROM DBA_USERS
WHERE USERNAME LIKE 'POLL%';

SELECT USERNAME, TABLESPACE_NAME,
       CASE MAX_BYTES WHEN -1 THEN 'ILIMITADA' ELSE TO_CHAR(MAX_BYTES/1024/1024) || ' MB' END AS CUOTA
FROM DBA_TS_QUOTAS
WHERE USERNAME LIKE 'POLL%';

SELECT GRANTEE, GRANTED_ROLE, DEFAULT_ROLE
FROM DBA_ROLE_PRIVS
WHERE GRANTEE LIKE 'POLL%';

SELECT GRANTEE, PRIVILEGE
FROM DBA_SYS_PRIVS
WHERE GRANTEE LIKE 'POLL%'
ORDER BY GRANTEE, PRIVILEGE;
