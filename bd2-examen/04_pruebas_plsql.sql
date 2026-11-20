-- =====================================================================
-- 04. PRUEBAS: una semana de ventas (10 pedidos por día) y pruebas
--     de funciones, procedimientos y triggers.
-- Conexión: POLLERIA_ADMIN
-- =====================================================================
SET SERVEROUTPUT ON

-- ---------------------------------------------------------------------
-- 4.1 Generar 7 días x 10 pedidos usando los procedimientos
-- ---------------------------------------------------------------------
DECLARE
  V_ID_PEDIDO NUMBER;
  V_CLIENTE   NUMBER;
  V_PRODUCTO  NUMBER;
  V_MESA      NUMBER;
  V_TIPO      VARCHAR2(15);
  V_METODO    VARCHAR2(10);
  V_FECHA     DATE;
  V_TOTAL     NUMBER;
BEGIN
  DBMS_RANDOM.SEED(2026);
  FOR DIA IN REVERSE 1..7 LOOP
    FOR N IN 1..10 LOOP
      V_FECHA := TRUNC(SYSDATE) - DIA + (11 + N) / 24;      -- de 12:00 a 21:00

      SELECT ID_CLIENTE INTO V_CLIENTE
      FROM (SELECT ID_CLIENTE FROM CLIENTES ORDER BY DBMS_RANDOM.VALUE)
      WHERE ROWNUM = 1;

      V_TIPO := CASE MOD(N, 3) WHEN 0 THEN 'MESA' WHEN 1 THEN 'PARA_LLEVAR' ELSE 'DELIVERY' END;
      V_METODO := CASE MOD(N + DIA, 4) WHEN 0 THEN 'EFECTIVO' WHEN 1 THEN 'YAPE'
                                       WHEN 2 THEN 'PLIN' ELSE 'TARJETA' END;
      V_MESA := NULL;
      IF V_TIPO = 'MESA' THEN
        SELECT ID_MESA INTO V_MESA FROM MESAS WHERE ESTADO = 'DISPONIBLE' AND ROWNUM = 1;
      END IF;

      REGISTRAR_PEDIDO(V_CLIENTE, V_TIPO, V_METODO, V_ID_PEDIDO, V_MESA,
                       CASE WHEN V_TIPO = 'DELIVERY' THEN 'Jr. Moquegua 120, Juliaca' END,
                       V_FECHA);

      FOR I IN 1..TRUNC(DBMS_RANDOM.VALUE(1, 4)) LOOP          -- 1 a 3 productos
        SELECT ID_PRODUCTO INTO V_PRODUCTO
        FROM (SELECT ID_PRODUCTO FROM PRODUCTOS WHERE DISPONIBLE = 1 ORDER BY DBMS_RANDOM.VALUE)
        WHERE ROWNUM = 1;
        AGREGAR_PRODUCTO_PEDIDO(V_ID_PEDIDO, V_PRODUCTO, TRUNC(DBMS_RANDOM.VALUE(1, 3)));
      END LOOP;

      SELECT TOTAL INTO V_TOTAL FROM PEDIDOS WHERE ID_PEDIDO = V_ID_PEDIDO;
      -- En efectivo pagan redondeando a la decena (hay vuelto)
      REGISTRAR_PAGO(V_ID_PEDIDO, CASE WHEN V_METODO = 'EFECTIVO' THEN CEIL(V_TOTAL / 10) * 10 ELSE V_TOTAL END);
    END LOOP;
  END LOOP;
  DBMS_OUTPUT.PUT_LINE('Semana generada: 70 pedidos registrados y cobrados.');
END;
/

-- Pedidos por día
SELECT TRUNC(FECHA_PEDIDO) AS DIA, COUNT(*) AS PEDIDOS, SUM(TOTAL) AS TOTAL_DIA
FROM PEDIDOS
GROUP BY TRUNC(FECHA_PEDIDO)
ORDER BY DIA;

-- ---------------------------------------------------------------------
-- 4.2 Pedidos de HOY que quedan PENDIENTES (para las pruebas)
-- ---------------------------------------------------------------------
DECLARE
  V_ID NUMBER;
BEGIN
  REGISTRAR_PEDIDO(1, 'MESA', 'EFECTIVO', V_ID, 2);
  AGREGAR_PRODUCTO_PEDIDO(V_ID, 2, 1);
  AGREGAR_PRODUCTO_PEDIDO(V_ID, 4, 1);
  DBMS_OUTPUT.PUT_LINE('Pedido en mesa 2 (pendiente): ' || V_ID);

  REGISTRAR_PEDIDO(2, 'DELIVERY', 'YAPE', V_ID, NULL, 'Av. Circunvalación 450, Juliaca');
  AGREGAR_PRODUCTO_PEDIDO(V_ID, 3, 1);
  DBMS_OUTPUT.PUT_LINE('Delivery (pendiente): ' || V_ID);

  REGISTRAR_PEDIDO(3, 'PARA_LLEVAR', 'PLIN', V_ID);
  AGREGAR_PRODUCTO_PEDIDO(V_ID, 1, 2);
  DBMS_OUTPUT.PUT_LINE('Para llevar (pendiente): ' || V_ID);
  COMMIT;
END;
/

SELECT P.ID_PEDIDO, OBTENER_NOMBRE_CLIENTE(P.ID_CLIENTE) AS CLIENTE, P.TIPO_PEDIDO,
       M.NUMERO AS MESA, P.ESTADO, P.BASE_IMPONIBLE, P.IGV, P.TOTAL
FROM PEDIDOS P LEFT JOIN MESAS M ON M.ID_MESA = P.ID_MESA
WHERE P.ESTADO = 'PENDIENTE';

SELECT NUMERO, ESTADO FROM MESAS ORDER BY NUMERO;   -- M02 debe estar OCUPADA

-- ---------------------------------------------------------------------
-- 4.3 Funciones
-- ---------------------------------------------------------------------
SELECT OBTENER_PRECIO_PRODUCTO(2)          AS PRECIO_MEDIO_POLLO,
       OBTENER_STOCK_PRODUCTO(2)           AS STOCK_MEDIO_POLLO,
       PRODUCTO_MAS_VENDIDO                AS MAS_VENDIDO,
       OBTENER_NOMBRE_CLIENTE(1)           AS CLIENTE_1,
       OBTENER_NOMBRE_CLIENTE(999)         AS CLIENTE_999,
       TOTAL_VENTAS_DIA(TRUNC(SYSDATE) - 1) AS VENTAS_AYER
FROM DUAL;

-- Procedimiento con cursor
BEGIN
  REPORTE_VENTAS_DIA(TRUNC(SYSDATE) - 1);
END;
/

-- ---------------------------------------------------------------------
-- 4.4 Pruebas de validaciones (cada una DEBE mostrar su error)
-- ---------------------------------------------------------------------
DECLARE
  V_ID          NUMBER;
  V_PEND_MESA   NUMBER;
  V_PEND_LLEVAR NUMBER;
  V_ENTREGADO   NUMBER;
  V_STOCK_ANTES NUMBER;
BEGIN
  SELECT MIN(ID_PEDIDO) INTO V_PEND_MESA   FROM PEDIDOS WHERE ESTADO = 'PENDIENTE' AND TIPO_PEDIDO = 'MESA';
  SELECT MIN(ID_PEDIDO) INTO V_PEND_LLEVAR FROM PEDIDOS WHERE ESTADO = 'PENDIENTE' AND TIPO_PEDIDO = 'PARA_LLEVAR';
  SELECT MIN(ID_PEDIDO) INTO V_ENTREGADO   FROM PEDIDOS WHERE ESTADO = 'ENTREGADO';

  -- a) Stock insuficiente (trigger TRG_DETALLE_STOCK)
  BEGIN
    AGREGAR_PRODUCTO_PEDIDO(V_PEND_LLEVAR, 1, 9999);
  EXCEPTION WHEN OTHERS THEN DBMS_OUTPUT.PUT_LINE('a) ' || SQLERRM);
  END;

  -- b) Producto no disponible (trigger TRG_DETALLE_STOCK)
  BEGIN
    AGREGAR_PRODUCTO_PEDIDO(V_PEND_LLEVAR, 8, 1);
  EXCEPTION WHEN OTHERS THEN DBMS_OUTPUT.PUT_LINE('b) ' || SQLERRM);
  END;

  -- c) Producto inexistente (función OBTENER_PRECIO_PRODUCTO)
  BEGIN
    AGREGAR_PRODUCTO_PEDIDO(V_PEND_LLEVAR, 999, 1);
  EXCEPTION WHEN OTHERS THEN DBMS_OUTPUT.PUT_LINE('c) ' || SQLERRM);
  END;

  -- d) Mesa ocupada (procedimiento REGISTRAR_PEDIDO)
  BEGIN
    REGISTRAR_PEDIDO(4, 'MESA', 'EFECTIVO', V_ID, 2);
  EXCEPTION WHEN OTHERS THEN DBMS_OUTPUT.PUT_LINE('d) ' || SQLERRM);
  END;

  -- e) Pago insuficiente (procedimiento REGISTRAR_PAGO)
  BEGIN
    REGISTRAR_PAGO(V_PEND_MESA, 1);
  EXCEPTION WHEN OTHERS THEN DBMS_OUTPUT.PUT_LINE('e) ' || SQLERRM);
  END;

  -- f) Eliminar un pedido (trigger TRG_PEDIDOS_CONTROL, BEFORE DELETE)
  BEGIN
    DELETE FROM PEDIDOS WHERE ID_PEDIDO = V_ENTREGADO;
  EXCEPTION WHEN OTHERS THEN DBMS_OUTPUT.PUT_LINE('f) ' || SQLERRM);
  END;

  -- g) Cancelar un pedido ya ENTREGADO (trigger TRG_PEDIDOS_CONTROL)
  BEGIN
    UPDATE PEDIDOS SET ESTADO = 'CANCELADO' WHERE ID_PEDIDO = V_ENTREGADO;
  EXCEPTION WHEN OTHERS THEN DBMS_OUTPUT.PUT_LINE('g) ' || SQLERRM);
  END;

  -- h) Cancelar un pedido PENDIENTE: devuelve el stock y queda en auditoría
  V_STOCK_ANTES := OBTENER_STOCK_PRODUCTO(1);
  UPDATE PEDIDOS SET ESTADO = 'CANCELADO' WHERE ID_PEDIDO = V_PEND_LLEVAR;
  DBMS_OUTPUT.PUT_LINE('h) Pedido ' || V_PEND_LLEVAR || ' cancelado. Stock del 1/4 de pollo: '
                       || V_STOCK_ANTES || ' -> ' || OBTENER_STOCK_PRODUCTO(1));
  COMMIT;
END;
/

-- Lo que registró el trigger de auditoría
SELECT * FROM AUDITORIA_PEDIDOS WHERE ESTADO_NUEVO = 'CANCELADO';

-- Cobrar el pedido de la mesa: la mesa vuelve a DISPONIBLE
DECLARE
  V_ID NUMBER;
BEGIN
  SELECT MIN(ID_PEDIDO) INTO V_ID FROM PEDIDOS WHERE ESTADO = 'PENDIENTE' AND TIPO_PEDIDO = 'MESA';
  REGISTRAR_PAGO(V_ID, 50);
  DBMS_OUTPUT.PUT_LINE('Pedido ' || V_ID || ' cobrado con S/ 50.00');
END;
/

SELECT P.ID_PEDIDO, P.TOTAL, PA.MONTO_RECIBIDO, PA.VUELTO, P.ESTADO, M.NUMERO, M.ESTADO AS ESTADO_MESA
FROM PEDIDOS P
JOIN PAGOS PA ON PA.ID_PEDIDO = P.ID_PEDIDO
JOIN MESAS M  ON M.ID_MESA = P.ID_MESA
WHERE P.ID_PEDIDO = (SELECT MAX(ID_PEDIDO) FROM PAGOS WHERE ID_PEDIDO IN
                       (SELECT ID_PEDIDO FROM PEDIDOS WHERE TIPO_PEDIDO = 'MESA'));
