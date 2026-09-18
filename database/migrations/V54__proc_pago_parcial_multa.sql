-- V54__proc_pago_parcial_multa.sql
-- Wrapper PROCEDURE (estilo V51) para sp_pago_parcial_multa (FUNCTION V16
-- con 4 parametros OUT): expone la operacion via CALL para que JPA la
-- invoque por el mecanismo @Procedure con binding posicional (P5).
-- La funcion original queda intacta (contrato SQL directo via SELECT).
CREATE OR REPLACE PROCEDURE proc_pago_parcial_multa(
    IN p_multa_id BIGINT,
    IN p_monto_pagado NUMERIC(8,2),
    OUT o_multa_id BIGINT,
    OUT o_estado VARCHAR(20),
    OUT o_saldo_restante NUMERIC(8,2),
    OUT o_usuario_desbloqueado BOOLEAN
)
LANGUAGE plpgsql
AS $procedure$
BEGIN
    SELECT r.o_multa_id, r.o_estado, r.o_saldo_restante, r.o_usuario_desbloqueado
      INTO o_multa_id, o_estado, o_saldo_restante, o_usuario_desbloqueado
      FROM sp_pago_parcial_multa(p_multa_id, p_monto_pagado) r;
END;
$procedure$;
