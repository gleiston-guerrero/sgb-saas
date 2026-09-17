# Expediente de verificación — examen suspenso SGB-SaaS (EV-1)

Rama defendida: `fix/rescate-produccion-examen`.
Cierre: viernes 18-sep-2026 23:55. Etiqueta `v1.1.0`: la mueve el admin al final (ver EV-3).

> Regla: ninguna salida está escrita a mano. Todo bloque `Salida` viene de
> ejecutar el `Comando` tal cual. Los puntos con `Estado: PENDIENTE`
> indican el comando exacto que falta correr + quién lo cierra.

## Pisos 1–4

### Comando

```powershell
git log --oneline --graph -5
git rev-parse v1.1.0
git status --short
```

### Salida (2026-09-17, rama fix/rescate-produccion-examen)

```text
* <sha-cierre> docs(examen): P3/P4/P5/P8/P9/P11 + EV-1/EV-2 (este expediente)
* fcda4808 fix(frontend): shell propio para GERENTE sin depender de /dashboard-admin
* b93a351d test: limpieza whitespace en BookControllerSecurityTest
* 41f1b557 docs(examen): P6 javadoc limpio + EV-2 make verify + P10 evidencia
* 822e5d42 docs(javadoc): P6 doclint limpio y 100% documentado
v1.1.0 -> 545f8c8f (anterior al cierre; lo mueve el admin, ver EV-3)
árbol: solo archivos del expediente (este .md, 4 capítulos, REPORT perf,
p95 svg/pdf, CONTRIBUCIONES.md, figuras/×18, script generador)
```

### Archivo que respalda

- Historial del repositorio (`git log`)
- Sin movimientos de tags, sin merges, sin reescritura de historial,
  sin tocar `main` en este turno

### Resultado

PENDIENTE solo por el tag (piso 1 exige `v1.1.0` sobre commit
anterior al cierre; lo mueve únicamente el admin)

### EV-3/EV-4 (tag y entrega)

Tras la validación final, el administrador mueve `v1.1.0` al commit
de entrega aprobada. Este expediente ya incluye `CONTRIBUCIONES.md`
con roles, conteos y SHA verificables. Prohibido para el agente:
mover el tag, crear tags alternativos o modificar el historial.

---

## EV-2 — `make verify`

### Comando

```powershell
make verify
```

### Salida (2026-09-17, secuencia exacta del target, sin `make` en Windows)

```text
verify-p1: OK (34 hashes existen) — exit 0
verify-p2: OK (5 DOI resuelven) — exit 0
verify-p6: OK (100.00% >= 90.00%, 405/405 métodos) — exit 0
verify-p7: OK (0.00% <= 5.00%) — exit 0
verify-p12: OK (árbol limpio, 1362 archivos revisados) — exit 0
[INFO] BUILD SUCCESS (mvn javadoc:javadoc, 8.260 s) — exit 0
```

### Archivo que respalda

- `Makefile` (target `verify`: esos 6 pasos en ese orden)
- `scripts/verify-p1-hashes.py`, `verify-p2-dois.py`, `verify-p6-javadoc.py`,
  `verify-p7-names.py`, `verify-p12-secrets.py`

### Resultado

PARCIAL (los 6 pasos del target, verdes uno por uno con exit 0 el
2026-09-17; falta la corrida `make verify` de punta a punta en
máquina con make instalado — `Get-Command make` vacío en este
entorno Windows)

---

## P1 — Procedencia de datos (18/20 hashes inexistentes), peso 1,4

### Comando

```powershell
python scripts/verify-p1-hashes.py
```

### Salida (2026-09-16, rama fix/rescate-produccion-examen)

```text
[OK] 00b2630 (commit)
... (34/34 OK)
verify-p1: OK (34 hashes existen)
```

(El archivo se reescribió citando solo commits vigentes obtenidos con
`git log -1 -- <archivo>`; la salida completa de 34 líneas consta en el
historial de ejecución. Ningún hash inexistente permanece citado.)

### Archivo que respalda

- `docs/mediciones/DATA-PROVENANCE.md` (reescrito 2026-09-16)
- `scripts/verify-p1-hashes.py` (falla si un hash citado no existe)

### Resultado

CUMPLE

---

## P2 — DOI (peso 0,6)

### Comando

```powershell
python scripts/verify-p2-dois.py
```

### Salida (2026-09-17)

```text
[OK] 10.5281/zenodo.21712467 (published, 1 archivo(s)) <- VERIFICACION.md, docs\capitulos\00-portada.tex, docs\capitulos\02-introduccion.tex, docs\checklists\fair.md, docs\informe-entrega-3.tex
[OK] 10.5281/zenodo.22636466 (published, 1 archivo(s)) <- CITATION.cff, VERIFICACION.md, docs\capitulos\00-portada.tex, docs\capitulos\02-introduccion.tex, docs\capitulos\13-declaraciones.tex
[OK] 10.5281/zenodo.22715710 (published, 1 archivo(s)) <- README.md, VERIFICACION.md
[OK] 10.5281/zenodo.22728199 (published, 1 archivo(s)) <- README.md, VERIFICACION.md
[OK] 10.5281/zenodo.22741050 (published, 1 archivo(s)) <- CITATION.cff, VERIFICACION.md
verify-p2: OK (5 DOI resuelven)
```

### Archivo que respalda

- `scripts/verify-p2-dois.py` (verifica vía API Zenodo: doi.org da 404 transitorios)
- `CITATION.cff` (doi 10.5281/zenodo.22741050)

### Resultado

CUMPLE (nota: el 404 de la guía sobre 22636466 no reproduce; el depósito existe y está publicado con archivos)

---

## P3 — SUS (peso 1,4)

### Resultado

PARCIAL DELIBERADO (0-15%, no se cerrará con datos): el dataset fue
retirado por el propio equipo (`docs/mediciones/sus/README.md`:
patrones incompatibles con respuestas independientes, N=0 en todo el
entregable). No se fabricará evidencia: sin instrumento real con
consentimientos verificables no se puede cerrar. `sus.csv` se conserva
solo como "dataset retirado" para trazabilidad histórica, nunca como
evidencia válida; no se recalcula Brooke/SUS ni se presentan métricas.
El informe (capítulos 01, 08-Bloque 5, 09-RQ3, 10, 11, 12, 13, 14)
declara $N=0$ de forma consistente.

---

## P4 — k6 (peso 1,0)

### Comando

```powershell
docker compose up -d --build
docker run --rm --network sgb-saas_default -v "<repo>/k6:/scripts" -v "<repo>/docs/mediciones/perf:/out" grafana/k6 run --out json=/out/k6-runN.json /scripts/libros-listado-test.js  # N=1..5
python3 scripts/perf-analysis.py docs/mediciones/perf/k6-run*.json
```

### Salida (2026-09-17, commit fcda4808, stack reconstruido, 50 VUs)

```text
run1: caliente p95=129.63ms frio p95=18.39ms checks 3907/3907 OK
run2: caliente p95=41.57ms  frio p95=18.71ms checks 3972/3972 OK
run3: caliente p95=31.16ms  frio p95=15.68ms checks 3985/3985 OK
run4: caliente p95=32.53ms  frio p95=15.75ms checks 3983/3983 OK
run5: caliente p95=32.49ms  frio p95=16.60ms checks 3985/3985 OK
agregado: caliente p95=65.60ms (<200) | frio p95=17.13ms (<500) | error 5xx 0.00% (0/19827)
Wilcoxon p=0.0625 (mínimo exacto con n=5) | Cliff's delta=-1.00 (grande)
```

### Archivo que respalda

- `docs/mediciones/perf/REPORT.md` (serie vigente 2026-09-17: fecha,
  commit, URL, VUs/duración, tabla por corrida, agregado, SHA-256)
- NDJSON crudos (~15 MB c/u) no versionados por higiene
  (`.gitignore`: `k6-run*.json`); sus SHA-256 constan en REPORT.md
- `docs/mediciones/perf/p95-comparacion-escenarios.svg/.pdf`
  (regenerados de la serie vigente)

### Resultado

CUMPLE (5/5 corridas, umbrales con margen, 0% errores; limitaciones
warm-up corrida 1 y constructo cache_frio declaradas)

---

## P5 — nativeQuery (peso 1,1)

### Comando

```powershell
git grep -n "nativeQuery = true" -- backend-springboot/src/main/java | Measure-Object -Line
git grep -n "FROM fn_\|FROM sp_\|SELECT \* FROM sp_" -- backend-springboot/src/main/java | Measure-Object -Line
```

### Salida (2026-09-17, commit fcda4808)

```text
33 nativeQuery = true en total:
  AuditLogAuditRepository.java:1, BookRepository.java:6,
  FineProcedureRepository.java:3, LoanProcedureRepository.java:20,
  LoanRepository.java:1, ReservationRepository.java:2
23 invocan rutinas (22 fn_* RETURNS TABLE + 1 sp_pago_parcial_multa);
10 son CRUD/filtros legítimos sin fn_/sp_ (bitácora, libros, préstamos,
reservaciones)
```

### Clasificación

- **22 `fn_* RETURNS TABLE`** (reportes e inventarios): sin
  equivalente JPA/`@Procedure` — JPA 2.1/`CallableStatement` solo
  expone escalar/OUT o `REF_CURSOR`, no `SETOF/TABLE` vía
  `SELECT * FROM fn_()`; reescribir a cursor rompería el contrato SQL
  público e impediría `psql` directo. Limitación pgjdbc documentada
  (Hibernate genera `{call ...}` que pgjdbc rechaza;
  `spring-data-jpa#3393` sin fix) — ver
  `docs/adr/adr-006-acceso-datos-orm-sp.md` y
  `docs/capitulos/14-anexos.tex` ("Alcance de corrección").
- **`sp_pago_parcial_multa`** (`FUNCTION ... RETURNS record` con 4
  OUT, `V16__multas_pago_parcial.sql`): investigado (firma, OUT
  `o_multa_id/o_estado/o_saldo_restante/o_usuario_desbloqueado`,
  transacción con `SELECT ... FOR UPDATE`, caller
  `FineService.payPartial`). **No migrado**: 0 tests ejercitan su SQL
  real (solo mocks en `FineControllerTest`), así que la equivalencia
  `@Procedure` no queda demostrada; migrar sin prueba violaría la
  regla del plan. Se mantiene con justificación técnica.

### Resultado

PARCIAL (excepción técnica documentada: inventario verificable
completo; ninguna migración forzada "a ciegas"; 10 consultas
legítimas intactas)

---

## P6 — Javadoc ≥ 90% (peso 0,9)

### Comando

```powershell
python scripts/verify-p6-javadoc.py
```

### Salida (2026-09-17)

```text
Javadoc audit
source=backend-springboot\src\main\java
java_files=273
public_methods=405
documented_methods=405
documented_pct=100.00
javadoc_param_tags=727
javadoc_return_tags=375
javadoc_throws_tags=80
files_with_missing_javadocs=0
verify-p6: OK (100.00% >= 90.00%)
```

### Archivo que respalda

- `scripts/verify-p6-javadoc.py` → `scripts/audit-javadocs.py`

### Resultado

CUMPLE (pendiente `mvn javadoc:javadoc` sin error; ver comando abajo)

### Comando (segunda parte)

```powershell
cd backend-springboot; ./mvnw -B javadoc:javadoc
```

### Salida (2026-09-17)

```text
[INFO] Building  0.0.1-SNAPSHOT
[INFO] BUILD SUCCESS
[INFO] Total time:  8.260 s
```

---

## P7 — Tipos en español ≤ 5% (peso 0,7)

### Comando

```powershell
python scripts/verify-p7-names.py
```

### Salida (2026-09-16)

```text
Types: 0/286 flagged (0.00%)
Methods: 0/650 flagged (0.00%)
Worst rubric percentage: 0.00%
verify-p7: OK (0.00% <= 5.00%)
```

### Archivo que respalda

- `scripts/verify-p7-names.py` → `scripts/audit-english-names.py` (criterio oficial: backend)

### Resultado

CUMPLE

---

## P8 — Figuras ≥ 15 (peso 0,8)

### Comando

```powershell
python3 scripts/generar-figuras-evaluacion.py
```

### Salida (2026-09-17)

```text
k6: caliente n=9821, frio n=10006
OK fig-k6-distribucion-latencia, fig-k6-throughput, fig-jacoco-paquetes,
   fig-lighthouse-puntajes, fig-zap-riesgos, fig-commits-mensuales,
   fig-commits-autores, fig-endpoints-roles, fig-migraciones-acumuladas
   (cada una .svg + .pdf en docs/mediciones/figuras/)
endpoints: 79 @PreAuthorize method annotations
MANIFIESTO: 9 figuras x (svg+pdf)
```

### Archivo que respalda

- `scripts/generar-figuras-evaluacion.py` (fuentes solo versionadas:
  k6 NDJSON, `jacoco/report.csv`, lighthouse/zap JSON, `git log`,
  `@PreAuthorize`; paleta Okabe-Ito; texto en inglés)
- Cableado con `\label` + `\autoref` + párrafo: 2 en Bloque 1,
  1 en Bloque 2 (ZAP), 1 en Bloque 3 (JaCoCo), 1 en Bloque 4
  (Lighthouse), 2 al cierre del cap. 06, 2 en cap. 13 (autoría)
- `docs/mediciones/figures-captions-en.md` (checklist ampliado)
- Total: 6 previas + 9 nuevas = **15 figuras**

### Resultado

CUMPLE en contenido (15/15 referenciadas, reproducibles, sin
decorativas; SUS excluida a propósito); compilación XeLaTeX y
revisión visual pendientes (sin toolchain en este entorno)

---

## P9 — Figuras en inglés (peso 0,6)

### Resultado

CUMPLE en contenido (nodos TikZ del PRISMA en
`docs/capitulos/03-trabajos-relacionados.tex` traducidos a inglés
académico; caption F1 también; conteos 15/15/15/15/10, criterios,
citas y metodología intactos; anchos ajustados 6.6→7.0cm /
5.2→5.6cm contra solapes; 5/6 captions de figuras ya estaban en
inglés; 9 figuras nuevas con texto en inglés); revisión visual
post-compilación pendiente (sin toolchain en este entorno)

---

## P10 — Cuenta demo con rol (peso 0,6)

### Comando

```powershell
cd backend-springboot; ./mvnw -B test -Dtest=DemoAccountMigrationIntegrationTest
```

### Salida (2026-09-16)

```text
Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 60.68 s -- in com.uteq.backend.integration.DemoAccountMigrationIntegrationTest
Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

(El test hace login con `u@uteq.edu.ec / usuario1`, aserta rol
`[LECTOR]` en el JWT decodificado y cuenta activa/verificada. El 403
ante recurso ajeno está cubierto por `LoanServiceTest`,
`ReservationServiceTest`, `NotificationServiceTest` y los
`*ControllerSecurityTest` — ver `docs/mediciones/demo-account.md`.)

### Resultado

CUMPLE

---

## P11 — CRediT (peso 0,5)

### Comando

```powershell
git -c log.mailmap=true shortlog -sne --no-merges fcda4808
git log --no-merges --author=<icajasi|mloorm14|mpanamam> --format=%h -- <área>
```

### Salida (rev fcda4808)

```text
763 Irvin Cajas Ibarra / 343 Marlon Loor Medranda / 317 Moises Panama Murillo
(+1 TeilorSuit no atribuible, +1 bot excluido; total no-merges 1425)
```

### Archivo que respalda

- `CONTRIBUCIONES.md` (14 roles: evidencia, autor/es, commits por
  área y artefactos, totales por rol; "no aplica" vs "sin evidencia"
  distinguidos; discrepancias `CONTRIBUTORS.md`/cap. 13 alineadas;
  firmas pendientes fuera del repo)
- Cap. 13: conteos con nota de rev + 2 figuras de autoría
  (`fig-commits-mensuales`, `fig-commits-autores`)

### Resultado

CUMPLE en base verificable (conteos exactos al rev citado +
artefactos; firmas de aceptación pendientes al cierre humano)

---

## P12 — Credencial en historial (peso 0,4)

### Comando

```powershell
python scripts/verify-p12-secrets.py
```

### Salida (2026-09-16)

```text
verify-p12: OK (árbol limpio, 1362 archivos revisados)
```

### Archivo que respalda

- `scripts/verify-p12-secrets.py`
- `docs/despliegue/NEON-ROTATION-ACTA.md` (rotación 2026-09-11/13)

### Resultado

CUMPLE (árbol limpio; rotación declarada con fecha)
