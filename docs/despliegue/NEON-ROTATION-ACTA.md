# Acta de rotacion de la credencial Neon

## Estado: EJECUTADA Y VERIFICADA (Validado por Irvin Marcelo Cajas Ibarra)

La rotacion ocurre fuera del repositorio (dashboards de Neon y Render),
asi que este acta deja el procedimiento exacto, la verificacion del lado
repo (ya ejecutada) y los campos para firmar la ejecucion. El propietario
de ambas cuentas completa la seccion "Ejecucion" y el acta pasa a ser la
evidencia exigida ("me basta con que declaren por escrito
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
3. Render dashboard -> microservicio -> **Environment** ->
   `DATABASE_URL` si embebe la clave = valor nuevo ->
   **Save**.
4. Render -> **Manual Deploy** -> verificar
   `curl https://sgb-backend-b058.onrender.com/actuator/health` -> `UP`.
5. Completar la seccion Ejecucion y commitear este acta.

## Ejecucion (rotacion realizada y verificada)

- Fecha/hora de reset en Neon: 2026-09-13 alrededor de las 11:22:43 AM (declarado por Irvin Marcelo Cajas Ibarra; el dashboard de Neon no muestra marca temporal del reset, por lo que es un tiempo estimado).
- Responsable: Irvin Marcelo Cajas Ibarra, propietario de ambas cuentas (Neon + Render), despliegue manual via Dashboard.
- Secreto afectado: password del rol `neondb_owner` (valor nunca publicado en el repo ni en este acta).
- Commit desplegado en los tres servicios: `3ae75db` ("fix(schedulers): tolerar seed parcial y apagar jobs en tests").
- Redeploys en Render (todos `Deploy succeeded | Live`, trigger manual via Dashboard):
  - backend: 2026-09-13 12:18:36 PM GMT-5, duracion 4m51s.
  - frontend: 2026-09-13 12:21:45 PM GMT-5, duracion 40,1s.
  - microservicio (backup-service): 2026-09-13 12:21:57 PM GMT-5, duracion 23,6s.
- Health check tras redeploy (`GET /actuator/health`): verificado 2026-09-13, respuesta `{"status":"UP"}` (incluye grupos `liveness` y `readiness`). Los tres servicios además figuran `Live` en Render.
- Resultado: rotacion ejecutada; la credencial nueva vive solo en Neon y en el dashboard de Render (`sync: false`), fuera del arbol evaluado.
- Afirmacion del propietario: yo, Irvin Marcelo Cajas Ibarra, afirmo que este cambio se hizo correctamente.
