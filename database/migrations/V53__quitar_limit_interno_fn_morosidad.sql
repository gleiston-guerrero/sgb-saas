-- V53: quitar LIMIT interno de fn_reporte_indice_morosidad (doble LIMIT).
-- V41 quitó el LIMIT interno de las demás funciones de reporte para paginar
-- en el wrapper (LIMIT :limit OFFSET :offset del repositorio), pero V46
-- reintrodujo LIMIT COALESCE(p_limite,10) en morosidad: con p_limite=10 y
-- OFFSET>=10 el wrapper paginado devuelve página vacía y el COUNT queda
-- topado a 10. Se deja la firma (p_limite se ignora, compatibilidad con
-- llamadas existentes) y el ORDER BY; la paginación la hace el wrapper,
-- igual que fn_reporte_categorias_demandadas y las demás de V41.

CREATE OR REPLACE FUNCTION fn_reporte_indice_morosidad(p_limite INTEGER DEFAULT NULL)
RETURNS TABLE (usuario_id BIGINT, nombre VARCHAR(100), apellido VARCHAR(100), correo VARCHAR(150), monto_total_adeudado NUMERIC(10,2), cantidad_multas_pendientes BIGINT, dias_atraso_promedio NUMERIC)
LANGUAGE sql STABLE AS $$
    SELECT u.id, u.nombre, u.apellido, u.correo,
           SUM(m.monto)::NUMERIC(10,2) AS monto_total_adeudado,
           COUNT(m.id) AS cantidad_multas_pendientes,
           ROUND(AVG(GREATEST(0, EXTRACT(DAY FROM (COALESCE(p.fecha_devolucion_real, NOW()) - p.fecha_devolucion_estimada)))::NUMERIC),1) AS dias_atraso_promedio
    FROM multas m JOIN estados_multa em ON em.id=m.estado_multa_id
    JOIN prestamos p ON p.id=m.prestamo_id JOIN usuarios u ON u.id=p.usuario_id
    WHERE em.nombre='PENDIENTE' GROUP BY u.id, u.nombre, u.apellido, u.correo
    ORDER BY monto_total_adeudado DESC;
$$;
