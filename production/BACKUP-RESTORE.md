# Backup y recuperación — procedimientos controlados

Los backups contienen datos personales, configuración y hashes de credenciales
Keycloak. No son archivos de documentación pública. Usar directorio externo al repo,
0700, archivos0600, copia cifrada/offline y retención acordada. No imprimir dumps,
contraseñas, tokens ni importar un backup sobre la base actual como prueba.

## Backup ejecutado y método reproducible

```bash
python3 scripts/production/backup.py \
  --oracle polleria-oracle \
  --postgres polleria-keycloak-db \
  --output /home/systemseng/polleria-backups/fase18
```

Para producción sustituir nombres por IDs obtenidos del Compose correcto, verificando
etiquetas, proyecto y mounts. El script exige nombres polleria-; con nombres Compose
propios como polleria-production-oracle-1 funciona. Confirmar FREEPDB1/POLLERIA antes.
Requiere privilegio administrativo Docker, no credenciales en argumentos.

Oracle: `expdp '"/ as sysdba"' schemas=POLLERIA directory=DATA_PUMP_DIR
flashback_time=systimestamp`, ejecutado con `ORACLE_PDB_SID=FREEPDB1` en la imagen real.
El directorio se consulta en DBA_DIRECTORIES del PDB, no se presupone una ruta fija.
Snapshot consistente de tablas mediante Flashback; operación de export solo lectura
comercial, aunque Data Pump crea su job administrativo temporal y archivos de dump/log.
La copia .dmp se extrae con docker cp y se comprueba tamaño/hash. No es una copia
binaria de un volumen activo ni un backup completo de SYS/CDB. Los scripts explícitos,
credenciales privadas del entorno y versión Oracle son parte del paquete recuperable.

PostgreSQL: `pg_dump -U polleria_keycloak -d polleria_keycloak --format=custom
--no-owner --no-acl`. Incluye realm, usuarios, credenciales cifradas/hasheadas y sesiones
según tablas KC; no solo un export público del realm. El rol fijo y DB se aprovisionan
por Compose. Se verificó `pg_restore --list` con TABLE DATA. Para consistencia entre
ambas bases en una recuperación comercial completa, acordar una ventana sin escrituras:
los dos backups son individualmente consistentes, no una transacción distribuida.

Ejecución F18: export Oracle exit0, archivo1,196,032bytes; PostgreSQL265,511bytes.
[Manifiesto sanitizado](verificacion-backup-fase-18.json) contiene SHA256 y modos0600.
Los archivos están fuera del repo, no se copiaron a Git ni se subieron. Revisar hash
antes de cada restauración. El directorio Data Pump del contenedor conserva su copia
administrativa de export; gestionar su retención después de asegurar otra copia,
sin borrar datos comerciales. No se ejecutó restauración sobre los volúmenes actuales.

## Restauración Oracle — no ejecutada sobre datos actuales

1. Acordar autorización y ventana. Detener escrituras/backend. Para ensayo usar un
   proyecto/volumen nuevo independiente, nunca polleria-oracle-data.
2. Levantar la misma imagen Oracle fijada y crear APP_USER=POLLERIA con secretos
   privados. Confirmar PDB FREEPDB1 y schema vacío. No crear las tablas01–06 si se
   restaurará metadata completa del dump; evitar colisión con tablas existentes.
3. Consultar DATA_PUMP_DIR dentro de FREEPDB1 como en backup.py. Copiar .dmp verificado
   a esa ruta del contenedor destino. Mantener permisos/propietario oracle apropiados;
   `docker cp` puede preservar un propietario distinto, verificar lectura por Oracle.
4. Comando conceptual exacto con variables ya verificadas y sin passwords:

```bash
docker exec -e ORACLE_PDB_SID=FREEPDB1 "$DEST_ORACLE" \
  impdp '"/ as sysdba"' schemas=POLLERIA directory=DATA_PUMP_DIR \
  dumpfile="$DUMP_BASENAME" logfile=restore-polleria.log exclude=USER
```

   `exclude=USER` porque APP_USER ya creó el usuario vacío. Revisar log completo y
   cada warning de roles/cuotas; no aceptar errores de carga o restricciones. No usar
   TABLE_EXISTS_ACTION=REPLACE/TRUNCATE sobre una base con datos. Si no está vacía,
   detenerse y diseñar un plan específico, no improvisar switches destructivos.
5. Verificar tablas, PK/FK/UNIQUE/CHECK ENABLED/VALIDATED, conteos del manifiesto,
   identidad siguiente y el SQL de integridad F17. Iniciar backend validate, health,
   consultas y smoke autorizado. Si faltó un grant/cuota explícito, corregirlo según
   script/configuración aprobada; nunca cambiar ddl-auto a update.

Se puede inspeccionar metadata con impdp SQLFILE en un destino temporal antes de
importar, sin ejecutar el SQL resultante. Un dump no reemplaza el respaldo de entorno
privado ni el procedimiento probado de aprovisionamiento. La restauración completa
queda como ejercicio controlado recomendado antes del servidor definitivo; no se
presenta este documento como evidencia de haberla ejecutado.

## Restauración PostgreSQL Keycloak — no ejecutada sobre datos actuales

1. Destino aislado o ventana de recuperación autorizada; KC detenido. Levantar misma
   versión PostgreSQL17, rol/DB polleria_keycloak según Compose y volumen vacío.
2. Restaurar en la DB **vacía** con error inmediato y transacción única:

```bash
docker exec -i "$DEST_POSTGRES" pg_restore \
  -U polleria_keycloak -d polleria_keycloak --no-owner --no-acl \
  --exit-on-error --single-transaction < "$PG_DUMP_ABS"
```

3. No usar `--clean` contra la DB actual. Si el destino ya tiene tablas, detenerse;
   no borrar/recrear como atajo. Comprobar realm/roles/clientes/usuarios y versión KC,
   luego iniciar Keycloak de la misma versión. Un upgrade de KC puede haber migrado
   su DB: el rollback de binario exige compatibilidad o restauración del backup previo.
4. Revisar hostname público, redirect/logout/webOrigins del cliente restaurado. No
   perder vínculo entre keycloakSub comercial Oracle y los UUID de usuarios KC;
   regenerar usuarios en lugar de restaurar puede romper ese vínculo.
5. Discovery, issuer, Code+PKCE, API protegida y logout deben pasar antes de tráfico.

## Realm reproducible

El JSON generado por preflight es configuración inicial sin usuarios/credenciales.
El realm dev versionado se usa como plantilla, con URLs estrictas sustituidas y SSL
all en producción. No exportar usuarios con contraseñas/secretos hacia Git. Para un
sistema existente, pg_dump es el respaldo de identidad; el import de arranque omite
realms que ya existen y no debe considerarse migración automática.

Referencias: [Oracle Data Pump](https://docs.oracle.com/en/database/oracle/oracle-database/23/sutil/oracle-data-pump-export-utility.html),
[PostgreSQL pg_dump17](https://www.postgresql.org/docs/17/app-pgdump.html),
[pg_restore17](https://www.postgresql.org/docs/17/app-pgrestore.html).
