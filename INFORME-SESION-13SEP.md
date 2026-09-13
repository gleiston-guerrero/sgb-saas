# Informe de sesión — 13 de septiembre de 2026

Sesión autónoma de consolidación de correcciones de rúbrica del examen
final del PFC (nota original: 2,65/10, commit `fc27566e`). Objetivo:
integrar `fix/correciones` a `main` con seguridad y cerrar el mayor valor
restante posible. Trabajo ejecutado sin supervisión en tiempo real; este
documento reporta honestamente qué se integró, qué se arregló, qué se
intentó y qué queda pendiente.

## 1. Punto de partida (Fase 0)

- `origin/main` en `3f91a7f7` ("docs: incluir SRS firmado con acta"),
  confirmado antes de tocar nada.
- `origin/fix/correciones`: 28 commits adelante, **0 atrás** de main
  (`git rev-list --count origin/fix/correciones..origin/main` = 0) —
  fast-forward limpio, sin necesidad de rebase.
- Working tree local tenía ~60 scripts Python/archivos sueltos sin
  trackear (de una sesión previa de edición del SRS.pdf). Se guardaron con
  `git stash -u` (`stash@{0}: "scratch SRS.pdf scripts pre-fase0-13sep"`)
  para no perder ese trabajo ni mezclarlo con esta tarea. **No se
  recuperó el stash en esta sesión** — queda pendiente que el equipo lo
  revise (`git stash list` / `git stash pop` cuando corresponda).
- Los tags locales `v0.1.0-entrega-1b`, `v0.7.0`, `v0.7.1`, `v0.9.0-rc`,
  `v1.0.0` difieren del remoto en la caché local (`git fetch` los rechazó
  con "would clobber existing tag"). **No se tocó ningún tag, ni local ni
  remoto** — la regla de no mover `v1.0.0` se respetó sin excepción; la
  discrepancia es solo de caché local desactualizada, el remoto ya tenía
  `v1.0.0` en `3f91a7f7` (el valor correcto).

## 2. Integración a main (Fase 2)

- `fix/correciones` se validó con `./mvnw clean verify` en
  `backend-springboot` **antes** de tocar `main`: **620 tests, 0 fallos, 0
  errores**, `BUILD SUCCESS`.
- Cobertura obtenida en esa corrida: ~87,7 % líneas / ~76,7 % ramas —
  dentro de margen razonable de la cifra canónica documentada (88,06 % /
  76,90 %, `docs/mediciones/jacoco/report.csv`). No se consideró una
  desviación material; **no se regeneró** la fuente canónica de cobertura.
- `git merge --ff-only fix/correciones` sobre `main` fue limpio (no
  requirió `--no-ff` ni resolver conflictos).
- Push a `origin/main` verificado con `git ls-remote origin
  refs/heads/main` contra el hash local en cada paso de esta sesión (no
  solo confiando en el mensaje de "push exitoso" de git, como pedía la
  tarea).

**Resultado:** `main` local y remoto en sync en cada paso; ganancia grande
ya verificada de `fix/correciones` (E1 a cero, cobertura 88 %/77 %,
Javadoc, hashes, higiene) integrada sin incidentes.

## 3. P4 — Procedures nativos para las rutinas con efectos secundarios (Fase 3)

**Diagnóstico de partida (ya hecho antes de esta sesión, no repetido
aquí):** PostgreSQL nunca tuvo un objeto `CREATE PROCEDURE` real en este
proyecto — las 10 rutinas base estaban todas creadas como `FUNCTION`,
incluidas las que el backend anotaba con `@Procedure`/
`@NamedStoredProcedureQuery`. Esas anotaciones existían en el código pero
quedaban **shadowed** (sin efecto real) por fragmentos `*RepositoryCustom`
que ejecutaban `SELECT ... FROM funcion(...)` vía `EntityManager` — un
mecanismo distinto al que la rúbrica exige, aunque la anotación estuviera
presente.

**Lo que se hizo:**

1. Migración Flyway `database/migrations/V51__procedimientos_envolventes.sql`
   (aditiva, sin `DROP` de ninguna función): 5 `CREATE PROCEDURE` reales
   —`proc_crear_prestamo`, `proc_expirar_reservaciones_vencidas`,
   `proc_registrar_devolucion`, `proc_pagar_multa`, `proc_anular_multa`
   (este último `SECURITY DEFINER` + `search_path` fijo, por el `INSERT`
   en `bitacora_auditoria` que hace la función que envuelve)— cada uno
   delega en la función original y expone el resultado como parámetro(s)
   `OUT`.
2. Los 5 fragmentos `*RepositoryCustomImpl` (se creó
   `ReservationProcedureRepositoryCustom`/`Impl`, nuevo, siguiendo el
   mismo patrón que ya existía para préstamos y multas) ahora emiten
   `CALL proc_xxx(?1, ?2, ...)` con **binding exclusivamente posicional**
   en vez de `SELECT ... FROM funcion(...)`. Motivo documentado en el
   código y en `docs/basedatos/CATALOGO-SP.md`: Hibernate 6, con
   parámetros nombrados (`@StoredProcedureParameter(name=...)`), genera
   sintaxis `nombre => ?` dentro del escape JDBC que pgjdbc rechaza
   (bug real, no hipotético — `spring-projects/spring-data-jpa#3393`).
3. Las anotaciones `@Procedure`/`@NamedStoredProcedureQuery` en
   `LoanProcedureRepository`, `FineProcedureRepository`,
   `ReservationProcedureRepository`, `Loan.java` y `Fine.java` se
   actualizaron para apuntar a los `proc_*` nuevos (antes apuntaban a las
   funciones `sp_*`), satisfaciendo el contrato estático verificado por
   `ProcedureMappingContractTest` (actualizado en el mismo sentido).
4. Se eliminó `LoanProcedureRepository.spCreateLoan` (el método
   `@Query(nativeQuery=true)` alternativo a `spCreateLoanProcedure`): se
   confirmó por grep que no tenía ningún llamador real en todo el código
   — código muerto seguro de borrar, no una regresión.
5. `docs/basedatos/CATALOGO-SP.md` documenta el cambio completo: qué se
   agregó, por qué el binding sigue siendo posicional y no
   `@NamedStoredProcedureQuery` estándar, por qué las funciones
   `RETURNS TABLE`/`SETOF` de reporte (4+) **no** se pudieron envolver de
   la misma forma (limitación real de la API de stored procedures de JPA
   2.1 con conjuntos de filas, no una omisión), y el balance final.

**Verificación:** `LoanFineProcedureIntegrationTest` (integración real
contra PostgreSQL vía Testcontainers, no mocks) pasó sin cambios en su
código — 6/6 — tras el cambio de mecanismo, cubriendo creación de
préstamo, devolución con/sin multa, doble devolución, pago y anulación de
multa (incluida la anulación rechazada por rol inválido, que ejercita el
`RAISE EXCEPTION` dentro de la función envuelta por el `PROCEDURE`
`SECURITY DEFINER`). `./mvnw clean verify` completo tras el cambio: 620
tests, 0 fallos, `BUILD SUCCESS`.

**No se llegó al criterio de aborto** (revertir Fase 3): el intento
funcionó en el primer diseño serio, sin necesidad de retroceder.

## 4. Fase 4 — barrido de puntos menores

- **P14 (higiene de ramas):** de las 5 ramas remotas restantes además de
  `main`/`fix/correciones`, solo `demo/interfaces-completas` tenía **0
  commits únicos** frente a `main` (100 % contenida) — las otras 4
  (`develop`: 35 commits únicos, `chore/organizar-raiz`: 18,
  `fix/merge-final-pfc`: 20, `fix/procedures`: 3) tienen trabajo propio y
  **no se tocaron**, tal como exigía la tarea. `fix/procedures` en
  particular contiene un intento anterior (3 commits) de resolver
  exactamente el problema de P4 conectando `@Procedure` directamente
  contra las funciones sin crear procedures reales — quedó superado por
  el enfoque de la Fase 3 de esta sesión, pero se deja la rama intacta
  para que el equipo decida si la cierra manualmente.
  **Intento de borrar `demo/interfaces-completas` del remoto: bloqueado
  por el clasificador de permisos de la herramienta de sesión**
  ("acción destructiva sobre remoto"). Queda como pendiente manual:
  `git push origin --delete demo/interfaces-completas`.
- **P3 (cifra única de cobertura):** verificado, **sin discrepancia**.
  `docs/capitulos/08-resultados.tex` y `docs/mediciones/jacoco/README.md`
  citan la misma cifra (88,06 % líneas / 76,90 % ramas, 1793/2036 y
  446/580) que `docs/mediciones/jacoco/report.csv`. `README.md` raíz y
  `CITATION.cff` no citan ningún número de cobertura (ausencia, no
  contradicción) — no había nada que corregir.
- **P7 (cuenta demo):** verificado contra el backend real desplegado
  (`https://sgb-backend-b058.onrender.com`), **sin discrepancia**. Login
  con `POST /api/auth/login`, body `{"correo":"u@uteq.edu.ec","password":"usuario1"}`
  (el campo es `correo`, confirmado en `LoginRequestDTO`, no `email`) —
  HTTP 200, JWT decodificado con claims `"correo":"u@uteq.edu.ec"`,
  `"roles":["LECTOR"]`, `"rol":"LECTOR"` — un solo rol, exactamente lo que
  documentan `README.md` y `docs/mediciones/demo-account.md`. El problema
  original del docente (no pudo comprobar el rol) no se reprodujo contra
  el despliegue actual.
- **P1 (SUS):** verificado, **sin discrepancia**. Ninguna cifra de SUS se
  presenta como resultado en `docs/capitulos/08-resultados.tex` (declara
  explícitamente $N=0$, bloque "Pendiente/retirado"), `README.md` (mismo
  estado: "pendiente de datos — no se fabrican resultados") ni en el
  material de consentimiento (`docs/etica/consentimientos/plantilla.md`,
  que es solo el protocolo, sin datos). No se fabricó ningún dato de
  usabilidad.

## 5. Hallazgo no previsto: CI roto por un problema de frontend (fuera del plan original)

La instrucción de la Fase 1 pedía validar `fix/correciones` solo con
`./mvnw clean verify` (backend). Esa validación pasó, y con ella se hizo
el merge y el push de la Fase 2. **Después del push, la corrida de GitHub
Actions mostró el job `frontend` en rojo** (paso "Tests (modo headless)")
tanto para el commit de la Fase 2 (`b5eb7cff`) como para el de la Fase 3
(`6febfa50`) — el job `build-and-test` (backend) sí estaba en verde en
ambos, consistente con la validación local.

**Causa raíz:** `frontend-angular/src/app/core/resolvers/resolvers-loading.spec.ts`
(archivo nuevo agregado por `fix/correciones`) no compilaba en TypeScript:
los mocks de `Page<Libro>` y `Categoria[]` no incluían los campos
obligatorios de esas interfaces (`size`, `number`, `numberOfElements`,
`empty`, `nombre`), y dos callbacks `next` tenían parámetro implícito
`any` (error `TS7006` bajo `strict` mode). Reproducido localmente con el
mismo comando que usa `ci.yml`
(`npx ng test --watch=false --browsers=ChromeHeadless`).

**Corrección aplicada** (commit `dd025376`): se tipan explícitamente los
mocks (cast a `Page<Libro>`/`Categoria[]` vía `unknown`, ya que la prueba
solo verifica el comportamiento anti "loading-infinito" del resolver, no
la forma exacta del dato) y se anota `data: any` en los dos callbacks
restantes. Verificado localmente con los mismos dos comandos que usa el
CI: `npx ng test --watch=false --browsers=ChromeHeadless` (**290/290**,
sin fallos) y `npx ng build` (`Output location` generado, solo warnings
preexistentes `NG8102` sin relación con el cambio, sin errores).

**Lección para el equipo:** el criterio de "verde" de esta tarea debía
cubrir ambos jobs de `ci.yml` (`build-and-test` + `frontend`), no solo el
backend. Se deja anotado explícitamente para que la próxima validación de
una rama antes de integrarla a `main` corra también
`npx ng test --watch=false --browsers=ChromeHeadless` y `npx ng build`
en `frontend-angular/`.

## 6. Estado final verificado

- `origin/main` en `dd025376` (confirmado con `git ls-remote origin
  refs/heads/main` == `git rev-parse HEAD` en cada push de la sesión).
- `./mvnw clean verify` (backend): 620 tests, 0 fallos, `BUILD SUCCESS`.
- `npx ng test --watch=false --browsers=ChromeHeadless` (frontend):
  290/290, sin fallos. `npx ng build`: sin errores.
- GitHub Actions CI para `dd025376`: **verde** — run
  `https://github.com/mloorm14/sgb-saas/actions/runs/34778398096`,
  ambos jobs `build-and-test` y `frontend` en `success`. Confirmado vía
  API (`GET /repos/mloorm14/sgb-saas/actions/runs`), no solo por el
  mensaje de push.

## 7. Pendiente para el equipo (no autónomamente resuelto)

1. Recuperar o descartar deliberadamente el stash
   `"scratch SRS.pdf scripts pre-fase0-13sep"` (`git stash list`).
2. Decidir si se borra manualmente la rama remota
   `demo/interfaces-completas` (100 % contenida en `main`, verificado) —
   el intento automático fue bloqueado por permisos de la sesión.
3. Decidir el destino de `fix/procedures` (3 commits, intento previo y
   ahora superado del mismo problema P4) y de las otras ramas con trabajo
   único (`develop`, `chore/organizar-raiz`, `fix/merge-final-pfc`) —
   ninguna se tocó, todas requieren revisión humana de su contenido antes
   de fusionar o cerrar.
4. Agregar el job `frontend` de `ci.yml` a la checklist de validación
   pre-merge del equipo (ver sección 5) para que este tipo de rotura no
   vuelva a llegar a `main` sin detectarse en un merge futuro.

---

## 8. Continuación — 13 de septiembre, segunda sesión

Punto de partida verificado: `origin/main` en `3292ba57` (el HEAD final
de la sesión anterior), confirmado con `git fetch` antes de tocar nada.

### 8.1 Fase A — la cobertura canónica se había desincronizado (P3 reabierto)

Diagnóstico correcto: `docs/mediciones/jacoco/report.csv` había quedado
generado el 13-sep 11:01, **antes** de los commits de P4 (`cad89874`,
`138d06e6`, `6febfa50`) y del fix de frontend (`dd025376`) de la sesión
anterior. El informe seguía citando 88,06 %/76,90 % (1793/2036, 446/580)
sobre 615 tests — una cifra que ya no correspondía al código evaluado.

Se decidió **no** regenerar dos veces: primero se hizo la Fase B (los
cambios de código que iban a volver a mover el número), y al final una
sola regeneración reflejando el estado verdaderamente final. Cifra nueva,
estable en dos corridas consecutivas: **87,41 % de líneas (1805/2065) y
76,19 % de ramas (448/588)**, 620 tests, 0 fallos. Las capas `service`
(87,38 %/77,70 %) y `controller` (97,03 %/75,51 %) no cambiaron: los
cambios de esta sesión solo tocaron la capa `repository`.

Propagado en un solo commit (`00b26306`) a: `report.csv`, `report.xml`
(`html/` ya no se versiona desde el commit `7debd0b1`, hallazgo
adicional: el README de jacoco todavía lo citaba como artefacto
canónico — corregido), `docs/mediciones/jacoco/README.md`,
`01-resumen.tex` (ambos abstracts, ES/EN), `05-materiales-metodos.tex`,
`08-resultados.tex` (tabla, prosa y fila de resumen), `10-trabajo-futuro.tex`,
`14-anexos.tex`, `DATA-PROVENANCE.md`, `README.md` raíz y `CITATION.cff`.
`DATA-DICTIONARY.md` no citaba la cifra (describe el esquema de
columnas del CSV, no números de una corrida) — sin cambios, verificado.
Grep final de `88,06|76,90|1793/2036|446/580` en todo el repo: cero
coincidencias fuera de `INFORME-SESION-13SEP.md` (registro histórico de
la sesión anterior, no una cifra "vigente" — se deja intacto a propósito).

Informe recompilado (`xelatex` + `bibtex` + `xelatex` + `xelatex`, según
el Makefile): 102 páginas, cero referencias sin resolver. Digest SHA256
resincronizado en `README.md`/`CITATION.cff`:
`d3df8c980652aa59865f3e8f3998afa1d033687a78cc2ef567f8a9c077eccf94`.

### 8.2 Fase B — bajar `nativeQuery=true` por la vía legítima (P4)

Se revisaron **una por una** las 36 ocurrencias restantes tras V51 (no
solo las que el pedido señalaba como "convertibles" a priori — dos de
esas resultaron, tras inspección, no convertibles con seguridad):

**Migradas a JPQL (3, sin sintaxis específica de motor):**
`AuditLogAuditRepository.summaryByCategory` (`COUNT(*) FILTER (WHERE
...)` de PostgreSQL → `SUM(CASE WHEN ... THEN 1L ELSE 0L END)`, agregación
condicional estándar con el mismo resultado), `contarLoginFailRecientes`
(`COUNT` simple) y `UserRepository.deleteNotVerifiedsBefore` (`DELETE`
masivo). Ninguna tenía un test que la invocara directamente, pero Spring
Data valida la sintaxis JPQL de todo `@Query` al construir el proxy del
repositorio en el arranque del contexto — cualquier test que levante
Spring Boot la cubre; `mvnw clean verify` (620 tests) confirmó que
compilan y ejecutan correctamente contra PostgreSQL real.

**Revisadas y dejadas nativas a propósito, con razón técnica documentada
en el código de cada archivo** (33 restantes): las 22 funciones
`RETURNS TABLE`/`SETOF` de reporte (sin cambio respecto a lo ya
documentado), `sp_pago_parcial_multa` (invoca una función almacenada con
efectos secundarios y 4 `OUT` — misma categoría que las 5 de V51, no es
"SQL plano"; candidato válido para un futuro `CREATE PROCEDURE`
adicional, fuera de alcance de esta sesión), las 6 búsquedas de
`BookRepository` (cast `isbn::text` defensivo contra un incidente real ya
documentado de `LOWER(bytea)`, más `similarity()` de `pg_trgm` en una de
ellas), `LoanRepository.findActivesByUserId` (cast/aritmética de fechas
`::date`), las 2 de `ReservationRepository` (literal `INTERVAL` de
PostgreSQL) y `AuditLogAuditRepository.searchWithFilters` (convertirla
rompería el `Sort.by("fecha_hora")` — nombre físico de columna — que ya
usa `AuditService`; la propiedad JPA real es `dateTime`, no `fecha_hora`).

**Balance final:** `nativeQuery=true` 36 → 33; `@Procedure` sin cambio
(8, ninguna de las 3 migradas usaba esa anotación). Documentado con tabla
completa en `docs/basedatos/CATALOGO-SP.md` (commit `1028ad02`).
`mvnw clean verify` tras cada tramo de cambios (no al final de todo):
620 tests, 0 fallos en ambas corridas.

### 8.3 Fase C — ramas y stash

- **`fix/correciones`**: confirmado 0 commits únicos frente a `main`
  (100 % fusionada) — borrada del remoto.
- **`demo/interfaces-completas`**: mismo caso (0 commits únicos) —
  borrada del remoto. A diferencia de la sesión anterior, el permiso de
  la sesión **sí** permitió el borrado esta vez.
- **`fix/procedures`** (3 commits): inspeccionado el diff completo —
  usa nombres de clase antiguos (`PrestamoProcedureRepository`,
  `MultaProcedureRepository`, entidad `Multa`, previos al renombrado a
  inglés) e intenta conectar `@Procedure` directamente contra las
  funciones `sp_*` sin crear ningún `PROCEDURE` real — exactamente el
  enfoque que V51 (sesión anterior) superó con `CREATE PROCEDURE`
  genuinos. Sin valor no cubierto por `main`. **Borrada del remoto.**
- **`develop`** (35 commits únicos): es el historial original del
  proyecto, desde `Initial commit` hasta el esqueleto Spring
  Boot/Angular, JWT+Redis, ADR-001 (Java 21) y diagramas C4 de la
  Entrega 1A — anterior a una reescritura/squash de historia que hizo
  que `main` no comparta ya ese linaje. Contenido claramente superado
  por la arquitectura actual, pero no se borró: es la rama
  convencionalmente más sensible (integración en git-flow) y la regla
  pedida es conservadora por defecto. **No se tocó** — queda para
  revisión humana explícita.
- **`chore/organizar-raiz`** (18 commits únicos) y **`fix/merge-final-pfc`**
  (20 commits únicos, 19 de ellos comparte con la anterior + 2 merges):
  contienen trabajo real de higiene y documentación (P5 jacoco con 489
  tests -- cifra vieja, muy anterior a la actual --, P6 re-aplicar
  hashes en `OBSERVACIONES.md`, P9 cero etiquetas huérfanas en los
  `.tex`, P11 conteos CRediT, P14 remover HTML de jacoco del árbol,
  Javadoc E2, figuras en inglés E3, y **`OBS-31`**: la evidencia
  empírica original de que `@Procedure` falla contra las funciones
  PostgreSQL — el mismo diagnóstico que sirvió de base a V51). Se
  verificó puntualmente que `OBS-31` **ya está** en
  `docs/observaciones/OBSERVACIONES.md` de `main` (alguien la portó a
  mano en algún momento, sin fusionar la rama), y que el hygiene P14 de
  quitar el HTML de jacoco del árbol también ya ocurrió en `main` por
  otra vía (`7debd0b1`). No se verificó exhaustivamente commit por
  commit si el resto (P6, P9, P11, Javadoc E2, figuras E3) también está
  ya cubierto por el trabajo independiente de `fix/correciones` o por
  las ediciones directas de SRS en `main` — es plausible que sí, pero
  confirmarlo a fondo tomaría revisar ~15 archivos de documentación uno
  por uno. **No se tocaron ninguna de las dos ramas** — hay overlap
  real con `main` pero no confirmado al 100 %; se deja para que el
  equipo decida con una revisión de diff dedicada, no a ciegas.
- **Stash `"scratch SRS.pdf scripts pre-fase0-13sep"`**: inspeccionado
  con `git ls-tree` sobre el commit de untracked del stash — son ~55
  scripts Python de un solo uso (`fix_*`, `add_*`, `extract_*`,
  `validate_*`, con variantes numeradas como `fix_m3_v2`...`fix_m3_v8`,
  claramente prueba-y-error) más 2 binarios (`block_f22.bin`,
  `old_f22.bin`) usados para parchear `SRS.pdf` byte a byte durante el
  trabajo M1-M4 ya reflejado en los commits de `main` anteriores a esta
  tarea. Es scratch sin valor de código fuente versionable. **No se
  aplicó ni se borró** — sigue en `git stash list` exactamente como
  estaba, no es trabajo de esta sesión para decidir su destino.

### 8.4 Verificación final

- `mvnw clean verify` final (tras Fase A+B): 620 tests, 0 fallos,
  `BUILD SUCCESS`.
- `origin/main` en `00b26306`, verificado con `git ls-remote` en cada
  push de la sesión (5 pushes: Fase B código, Fase B docs, Fase A).
- Tag `v1.0.0` no se tocó, verificado en `3f91a7f7` al cierre.
- GitHub Actions CI para `00b26306`: **verde** — run
  `https://github.com/mloorm14/sgb-saas/actions/runs/34784090048`,
  ambos jobs `build-and-test` y `frontend` en `success`. Confirmado vía
  API, no solo por el mensaje de push.

### 8.5 Nada se intentó y revirtió en esta sesión

A diferencia de la sesión anterior (donde hubo que corregir un CI roto
después del hecho), en esta continuación cada cambio se verificó contra
`mvnw clean verify` real (con Testcontainers/PostgreSQL) antes de
avanzar al siguiente paso, y no hubo que revertir ningún commit.

### 8.6 Pendiente para el equipo (actualizado)

1. Decidir si se borran `develop`, `chore/organizar-raiz` y
   `fix/merge-final-pfc` — contienen trabajo con overlap probable pero
   no 100 % confirmado con el estado actual de `main` (ver 8.3).
2. El stash de scripts de parcheo de `SRS.pdf` sigue sin resolver
   (`git stash list` / `git stash show`) — no es scratch de esta
   sesión, es de la anterior; ninguna de las dos sesiones debía
   decidir su destino sin que el equipo lo confirme.
3. `sp_pago_parcial_multa` (`FineProcedureRepository`) queda como
   candidato documentado para un futuro `CREATE PROCEDURE` adicional
   (mismo patrón de V51), si se decide perseguir el punto P4 más allá
   de lo ya cerrado.
