# Acta de rotacion de la credencial Neon (punto 14e)

## Estado: PENDIENTE DE EJECUCION EN DASHBOARDS (propietario)

La rotacion ocurre fuera del repositorio (dashboards de Neon y Render),
asi que este acta deja el procedimiento exacto, la verificacion del lado
repo (ya ejecutada) y los campos para firmar la ejecucion. El propietario
de ambas cuentas completa la seccion "Ejecucion" y el acta pasa a ser la
evidencia exigida por la rubrica ("me basta con que declaren por escrito
que la rotaron y que no este en el arbol evaluado").

## Verificacion lado repositorio (2026-09-13, ejecutada)

- `render.yaml`: secretos con `sync: false` (valores solo en el dashboard
  de Render, nunca en el repo).
- `docker-compose.yml`: solo placeholders `changeme` y referencias
  `${VAR}` sin valor real.
- `.env.example`: sin secretos reales.
- La credencial historica recuperable del historial viejo quedo fuera del
  arbol evaluado tras la reescritura (ningun secreto real en
  `git ls-files` actual).

## Procedimiento (propietario: Neon + Render)

1. Neon dashboard -> proyecto -> **Roles** -> rol del backend
   (`neondb_owner`) -> **Reset password** -> copiar el valor nuevo
   (no se publica nunca en el repo ni en este acta).
2. Render dashboard -> servicio backend -> **Environment** ->
   `DB_PASSWORD` (y `DATABASE_URL` si embebe la clave) = valor nuevo ->
   **Save**.
3. Render -> **Manual Deploy** -> verificar
   `curl https://sgb-backend-b058.onrender.com/actuator/health` -> `UP`.
4. Completar la seccion Ejecucion y commitear este acta.

## Ejecucion (completar al rotar)

- Fecha/hora de reset en Neon: _pendiente_
- Responsable: _pendiente (propietario de ambas cuentas)_
- Secreto afectado: password del rol `neondb_owner` (valor nunca publicado)
- Hora de redeploy en Render: _pendiente_
- Health check tras redeploy (`/actuator/health`): _pendiente_
- Resultado: _pendiente_
