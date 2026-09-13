-- V51: PROCEDURE nativos (CREATE PROCEDURE, invocables con CALL) que envuelven
-- las 5 funciones de db/procs/ con efectos secundarios: sp_crear_prestamo,
-- sp_expirar_reservaciones_vencidas, sp_registrar_devolucion, sp_pagar_multa,
-- sp_anular_multa.
--
-- Contexto (rubrica examen final, punto P4): el backend usaba @Query
-- (nativeQuery = true) contra estas 5 funciones porque en PostgreSQL nunca
-- existio un objeto CREATE PROCEDURE real -- todo estaba creado como
-- FUNCTION (decision de diseno documentada en docs/basedatos/CATALOGO-SP.md,
-- motivada en su momento por una incompatibilidad real entre Hibernate 6 y
-- pgjdbc con la sintaxis de parametros nombrados que JPA 2.1 genera para
-- @NamedStoredProcedureQuery, ver spring-projects/spring-data-jpa#3393).
--
-- Este archivo NO revierte esa decision ni borra las funciones: son
-- aditivas. Las funciones sp_* siguen existiendo, invocables directamente
-- desde psql/Postman igual que antes (compatibilidad hacia atras). Cada
-- PROCEDURE de aqui simplemente delega en la funcion correspondiente y
-- expone el resultado como parametro(s) OUT, para que el backend pueda
-- invocarlas con CALL (mecanismo de stored procedure real) en vez de
-- SELECT ... FROM funcion(...).
--
-- Orden de migraciones: las 5 funciones envueltas ya existen desde V1-V9
-- (db/procs/*.sql se aplica antes que las migraciones Flyway V1+ en el
-- init automatico, ver scripts/build-init-sql.sh); esta migracion puede
-- asumir con seguridad que ya existen.

-- ============================================================================
-- 1. proc_crear_prestamo — envuelve sp_crear_prestamo (FUNCTION, retorno BIGINT)
-- ============================================================================
CREATE OR REPLACE PROCEDURE proc_crear_prestamo(
    IN p_usuario_id BIGINT,
    IN p_libro_id BIGINT,
    IN p_bibliotecario_id BIGINT,
    IN p_dias_prestamo INTEGER,
    OUT o_prestamo_id BIGINT
)
LANGUAGE plpgsql
AS $$
BEGIN
    o_prestamo_id := sp_crear_prestamo(p_usuario_id, p_libro_id, p_bibliotecario_id, p_dias_prestamo);
END;
$$;

COMMENT ON PROCEDURE proc_crear_prestamo(BIGINT, BIGINT, BIGINT, INTEGER) IS
    'Wrapper CREATE PROCEDURE (invocable con CALL) de la funcion sp_crear_prestamo. Ver V51 y docs/basedatos/CATALOGO-SP.md.';

-- ============================================================================
-- 2. proc_expirar_reservaciones_vencidas — envuelve sp_expirar_reservaciones_vencidas
--    (FUNCTION, retorno INTEGER). Sin valor DEFAULT en p_ahora: el llamador
--    (backend) siempre pasa el timestamp explicito -- CALL de PostgreSQL con
--    parametros OUT exige un placeholder posicional para cada parametro
--    (IN y OUT), lo que en la practica hace poco confiable depender de un
--    DEFAULT al invocar desde JDBC.
-- ============================================================================
CREATE OR REPLACE PROCEDURE proc_expirar_reservaciones_vencidas(
    IN p_ahora TIMESTAMPTZ,
    OUT o_cantidad INTEGER
)
LANGUAGE plpgsql
AS $$
BEGIN
    o_cantidad := sp_expirar_reservaciones_vencidas(p_ahora);
END;
$$;

COMMENT ON PROCEDURE proc_expirar_reservaciones_vencidas(TIMESTAMPTZ) IS
    'Wrapper CREATE PROCEDURE (invocable con CALL) de la funcion sp_expirar_reservaciones_vencidas. Ver V51 y docs/basedatos/CATALOGO-SP.md.';

-- ============================================================================
-- 3. proc_registrar_devolucion — envuelve sp_registrar_devolucion
--    (FUNCTION, 3 parametros OUT)
-- ============================================================================
CREATE OR REPLACE PROCEDURE proc_registrar_devolucion(
    IN p_prestamo_id BIGINT,
    OUT o_prestamo_id BIGINT,
    OUT o_hubo_multa BOOLEAN,
    OUT o_monto_multa NUMERIC(8,2)
)
LANGUAGE plpgsql
AS $$
BEGIN
    SELECT r.o_prestamo_id, r.o_hubo_multa, r.o_monto_multa
      INTO o_prestamo_id, o_hubo_multa, o_monto_multa
      FROM sp_registrar_devolucion(p_prestamo_id) AS r;
END;
$$;

COMMENT ON PROCEDURE proc_registrar_devolucion(BIGINT) IS
    'Wrapper CREATE PROCEDURE (invocable con CALL) de la funcion sp_registrar_devolucion. Ver V51 y docs/basedatos/CATALOGO-SP.md.';

-- ============================================================================
-- 4. proc_pagar_multa — envuelve sp_pagar_multa (FUNCTION, 2 parametros OUT)
-- ============================================================================
CREATE OR REPLACE PROCEDURE proc_pagar_multa(
    IN p_multa_id BIGINT,
    OUT o_multa_id BIGINT,
    OUT o_usuario_desbloqueado BOOLEAN
)
LANGUAGE plpgsql
AS $$
BEGIN
    SELECT r.o_multa_id, r.o_usuario_desbloqueado
      INTO o_multa_id, o_usuario_desbloqueado
      FROM sp_pagar_multa(p_multa_id) AS r;
END;
$$;

COMMENT ON PROCEDURE proc_pagar_multa(BIGINT) IS
    'Wrapper CREATE PROCEDURE (invocable con CALL) de la funcion sp_pagar_multa. Ver V51 y docs/basedatos/CATALOGO-SP.md.';

-- ============================================================================
-- 5. proc_anular_multa — envuelve sp_anular_multa (FUNCTION, 2 parametros OUT).
--    SECURITY DEFINER + search_path fijo: sp_anular_multa escribe en
--    bitacora_auditoria, tabla en la que los roles de aplicacion no tienen
--    INSERT directo (ver db/roles-privilegios.sql, seccion 5). El wrapper
--    corre con los privilegios de su propietario (quien aplico esta
--    migracion) durante toda la llamada, incluida la funcion anidada que
--    invoca -- necesario para que el INSERT de auditoria funcione sin
--    otorgarle ese permiso directamente al rol de conexion del backend.
--    SET search_path fijo evita que un search_path manipulado en la sesion
--    redirija la resolucion de sp_anular_multa/bitacora_auditoria a otro
--    esquema (mismo criterio que cualquier funcion SECURITY DEFINER).
-- ============================================================================
CREATE OR REPLACE PROCEDURE proc_anular_multa(
    IN p_multa_id BIGINT,
    IN p_motivo VARCHAR(255),
    IN p_rol_ejecutor VARCHAR(30),
    OUT o_multa_id BIGINT,
    OUT o_usuario_desbloqueado BOOLEAN
)
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
    SELECT r.o_multa_id, r.o_usuario_desbloqueado
      INTO o_multa_id, o_usuario_desbloqueado
      FROM sp_anular_multa(p_multa_id, p_motivo, p_rol_ejecutor) AS r;
END;
$$;

COMMENT ON PROCEDURE proc_anular_multa(BIGINT, VARCHAR, VARCHAR) IS
    'Wrapper CREATE PROCEDURE (invocable con CALL, SECURITY DEFINER) de la funcion sp_anular_multa. Ver V51 y docs/basedatos/CATALOGO-SP.md.';
