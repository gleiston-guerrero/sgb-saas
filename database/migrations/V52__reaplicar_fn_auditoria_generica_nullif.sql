-- V52: re-aplicar fn_auditoria_generica() con el NULLIF de V48.
-- Motivo: prod devolvía "invalid input syntax for type bigint: '' ...
-- fn_auditoria_generica() line 17", firma de la versión ANTERIOR a V48.
-- Causa probable: BD creada/baselineada con schema.sql viejo (baseline-version
-- 48 marca V1-V48 como aplicadas sin correrlas). CREATE OR REPLACE la hace
-- idempotente: en BDs al día no cambia nada, en BDs con drift las nivela.
-- Cuerpo idéntico a V48__fix_fn_auditoria_generica_empty_string.sql.

CREATE OR REPLACE FUNCTION fn_auditoria_generica()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_usuario_id  BIGINT;
    v_registro_id BIGINT;
    v_old_data    JSONB;
    v_new_data    JSONB;
    v_detalles    JSONB;
BEGIN
    -- NULLIF evita "invalid input syntax for type bigint: ''" cuando la
    -- variable existe pero está vacía (sesión sin usuario autenticado).
    v_usuario_id := NULLIF(current_setting('app.current_user_id', true), '')::BIGINT;

    IF TG_OP = 'DELETE' THEN
        v_registro_id := (to_jsonb(OLD) ->> 'id')::BIGINT;
    ELSE
        v_registro_id := (to_jsonb(NEW) ->> 'id')::BIGINT;
    END IF;

    IF TG_OP IN ('INSERT', 'UPDATE') THEN
        v_new_data := to_jsonb(NEW);
    END IF;
    IF TG_OP IN ('UPDATE', 'DELETE') THEN
        v_old_data := to_jsonb(OLD);
    END IF;

    -- password_hash nunca sale hacia la bitácora (ver V48).
    IF TG_TABLE_NAME = 'usuarios' THEN
        v_old_data := v_old_data - 'password_hash';
        v_new_data := v_new_data - 'password_hash';
    END IF;

    IF TG_OP = 'INSERT' THEN
        v_detalles := v_new_data;
    ELSIF TG_OP = 'UPDATE' THEN
        v_detalles := jsonb_build_object('antes', v_old_data, 'despues', v_new_data);
    ELSE -- DELETE
        v_detalles := v_old_data;
    END IF;

    INSERT INTO bitacora_auditoria
        (usuario_id, tipo_operacion, tabla_afectada, registro_id, detalles, ip_origen)
    VALUES
        (v_usuario_id, TG_OP, TG_TABLE_NAME, v_registro_id, v_detalles::TEXT, NULL);

    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
    END IF;
    RETURN NEW;
END;
$$;
