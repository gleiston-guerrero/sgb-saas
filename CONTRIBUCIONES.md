# Declaración de aportes por punto — SGB-SaaS (EV-4)

Rev de referencia: `e55f43b0782166b4d0586ccd34eb2d30e3e14c59`
(rama `fix/fase01-sus-n0`).
Base de conteos CRediT verificada: `e55f43b0782166b4d0586ccd34eb2d30e3e14c59`
(re-sincronizar al cierre con los comandos de abajo).

Regla de este documento: la columna **Responsable propuesto** se infiere
del historial (`git log -- <ruta>`) y está **pendiente de aceptación
genuina** hasta que el integrante figure en la sección **Firmas y
aceptaciones** con nombre, correo institucional, fecha real y filas
aceptadas. El agente no firma ni transcribe firmas por otros integrantes:
una fila solo queda confirmada para los responsables que la aceptan
explícitamente al final del documento.

Comandos reproducibles (verificar cada fila):

```powershell
git show --format=fuller --stat <sha>
git diff-tree --no-commit-id --name-only -r <sha>
git log --format='%H %ae %s' -- <ruta-especifica>
```

## A. Titularidad por cada pendiente (P1–P12 + EV-1/EV-2)

| Punto | Estado reclamado | Responsable propuesto (pende aceptación) | Aporte concreto | Archivos | Commits completos | Comando de prueba | Aceptación |
|---|---|---|---|---|---|---|---|
| P1 Procedencia (1,4) | Cerrado 100 % (35 hashes existen; filas 1, 2 y 4 con commits reales) | Panamá Murillo (reescritura + corrección filas) + Cajas Ibarra (anotaciones) | Reescritura con hashes vigentes; corrección fila JaCoCo (`e8477021`) y NDJSON versionados; medición JaCoCo regenerada 86,30/74,53 | `docs/mediciones/DATA-PROVENANCE.md`, `scripts/verify-p1-hashes.py`, `docs/mediciones/jacoco/report.csv` | `ced60e205264c7dad17b7113f8087da2b8f72a57` (Panamá), `8eb3dc34612f2feacb0bbc12e09b9983615bbea9` (Cajas) | `python scripts/verify-p1-hashes.py` | *(vacía)* |
| P2 DOI (0,6) | Cerrado 100 % (DOI roto fuera del árbol; 4 restantes con doi.org 200) | Loor Medranda (CITATION v1.0.1) + Panamá Murillo (barrido y verificador) | `CITATION.cff` a DOI `22741050`; barrido de portada/intro/declaraciones/FAIR; verificador con doi.org primario | `CITATION.cff`, `scripts/verify-p2-dois.py`, `docs/capitulos/00-portada.tex`, `docs/capitulos/02-introduccion.tex`, `docs/capitulos/13-declaraciones.tex`, `docs/checklists/fair.md` | `55785aabf3e2478cddad40cff2c091e7412ff2cb` (Loor), este fix P2 (ver `git log`) | `python scripts/verify-p2-dois.py` | *(vacía)* |
| P3 SUS (1,4) | **No cerrado — N=0 — no se atribuye puntaje** | Cajas Ibarra (retiro) + Panamá Murillo (retiro del árbol Fase 0.1) | Retiro de evidencia mock; retiro de `sus.csv` y derivados del árbol; notebook modo N=0 | `docs/mediciones/sus/README.md`, `docs/mediciones/sus/CONSENT.md`, `scripts/sus-analysis.ipynb`, `scripts/verify-p3-sus.py` | `e6df3ff6ad3796f7ea4d41b97674d0741862043d` (Cajas), `1c28085ffcc66098e558b49634397d6984f4df4b`, `902e5133e929b6c03a7790baf136bf5d7767fa8b` (Panamá) | `python scripts/verify-p3-sus.py` | *(vacía — no hay puntaje que aceptar)* |
| P4 k6 (1,0) | Cerrado 100 % (serie única 65,60/17,13 en todo el informe; Wilcoxon NS + hot/cold declarados; notebooks al día) | Panamá Murillo (serie y verificador) + Loor Medranda (corridas y estadística) | 5 corridas NDJSON, blindaje CRLF, REPORT con IC/Wilcoxon/Cliff; 5.ª corrida y `perf-analysis.py`; unificación de cifras en capítulos; fix encoding notebook | `docs/mediciones/perf/`, `k6/`, `scripts/perf-analysis.py`, `scripts/verify-p4-k6.py`, caps 01/05/06/09/11/12 | `9f3702705eab68971b9d275b59da36da57a0ce06`, `b82ae0e231c7ddd5db4074126c62f40f0301c26a` (Panamá), `ef0600e8c8e533a9622c9845832f20257a3832f9` (Loor) | `python scripts/verify-p4-k6.py` | *(vacía)* |
| P5 nativeQuery (1,1) | Cerrado 100 % (0 `nativeQuery=true` + 0 CALL nativos; 22 migraciones auditadas) | Panamá Murillo (migración completa P5) + Loor Medranda + Cajas Ibarra (trabajo previo: JPQL parciales, V51, alias) | 10 ordinarias a JPQL/Criteria; 6 side-effects a StoredProcedureQuery posicional (V54 incluida); 17 tabulares a Criteria/JPQL con equivalencia PG; matriz P5-MATRIZ-33 | Repositorios + `*CustomImpl`, `database/migrations/V54__proc_pago_parcial_multa.sql`, `scripts/verify-p5-nativequery.py`, `docs/basedatos/P5-MATRIZ-33.md`, `ProcedureMappingContractTest` | `160ef7f693acb304950221f1fa4f01c938c2d672` (O-9/O-10), `eada9c2350b09b97e3c19d23561480db4868bf89` (O-8), `a250caa63845c66bfb5652e08af7033dfaea6d0a` (Book), `9b5c6ba52106a2beed69311d56a73151710f414b` (O-1), `c528742f2f38a6e533394ee56f4bbf5b02ac59f1` (side-effects+V54), `56259a7fb9b61df591ab264d4b7b73aec05eec86` (R-T9/R-T10), `82d52b61113fb5f2ae06c0d2224227e434dab4f7` (tabulares), `3e411995d0a8b6b81925a5f48fd94bb2859c4231` (matriz+spikes) — todos Panamá; previo: `8f551aa65b263866d7109a23c39bacfdbfe21ab0`, `cad89874d25790b4b0434cfd6ee1a5d3587c7e91` (Loor), `2e17fcc25c11dc02c66e1f3870ed8f5ba982cd77` (Cajas), `cba3b508564316850fd9bce488ce423c160c6315` (Panamá) | `python scripts/verify-p5-nativequery.py` | *(vacía)* |
| P6 Javadoc (0,9) | Cerrado 100 % (100 % explícito + 99,6 % amplio; plantillas eliminadas; javadoc sin warnings) | Cajas Ibarra (cobertura E2) + Panamá Murillo (doclint) + equipo (reescritura por paquetes) | Cobertura Javadoc E2; auditoría versionada; doclint limpio; ~500 Javadocs reales | `scripts/audit-javadocs.py`, `scripts/verify-p6-javadoc.py`, fuentes Java documentadas | `3fc642ff7863d405158dcd5a222e7eef828c19c7` (Cajas), `822e5d42dd443d1c4d4875232ab091eedae4d5c6` (Panamá) | `python scripts/verify-p6-javadoc.py` + `mvn javadoc:javadoc` | *(vacía)* |
| P7 Tipos en español (0,7) | Cerrado 100 % (0/283 en `src/main`) | Cajas Ibarra (segunda pasada E1) + Loor Medranda (renombres) | Cero raíces españolas en tipos y métodos | Fuentes renombradas, `scripts/verify-p7-names.py`, `scripts/audit-english-names.py` | `c68e1cfc0ad4429259a9ecdd7c49dbda4eaa7683` (Cajas), `bb22bb1256ce61dd997dbec870b2afb185166af0` (Loor) | `python scripts/verify-p7-names.py` | *(vacía)* |
| P8/P9 Figuras (0,8+0,6) | Cerrado 100 % (15/15 + C4-N2 sin recorte + DER verificado + top-12 + babel; visual humano pendiente) | Panamá Murillo (evidencia y re-render C4/DER) + Loor Medranda (C4 reales) | 15 figuras citadas; C4 N1/N2 desde `workspace.dsl` re-renderizado; DER con spec verificada; verificadores P8/P9 | `docs/arquitectura/`, `docs/diagramas/er-english.pdf`, `docs/mediciones/figuras/`, `scripts/generar-figuras-evaluacion.py`, `scripts/generate-english-er-figure.py`, `scripts/verify-p8-p9-figures.py` | `b82ae0e231c7ddd5db4074126c62f40f0301c26a`, `36b899b26f10ff7911ffeb757063e61e53c7352e` (Panamá), `c16f54872c45f14cf5860a33ce39c4fc87455d02`, `9b3994ea556dbc0d169d197bbfb94c5ed9f06de6` (Loor) | `python scripts/verify-p8-p9-figures.py` | *(vacía)* |
| P10 Cuenta demo (0,6) | Cerrado 100 % (login LECTOR + JWT + doble 403 en despliegue, evidencia sanitizada) | Panamá Murillo (test de autorización + evidencia deploy) + Cajas Ibarra (cuenta demo) | `DemoAccountAuthorizationIntegrationTest` (Testcontainers, cero mocks); cuenta demo LECTOR; script de evidencia contra despliegue | `backend-springboot/.../integration/DemoAccountAuthorizationIntegrationTest.java`, `docs/mediciones/demo-account.md`, `scripts/p10-deploy-evidence.py`, `docs/evidencia/examen/p10-deploy.txt` | `b82ae0e231c7ddd5db4074126c62f40f0301c26a` (Panamá), `b6e696409c263dcedb87c1f1d0862a16b045d5a3` (Cajas) | `python scripts/p10-deploy-evidence.py` | *(vacía)* |
| P11 CRediT (0,5) | Parcial 70 % (conteos por área, no autoría; ver sección B) | Panamá Murillo | 14 roles con conteos y artefacto por celda; verificador de conteos | `CONTRIBUCIONES.md`, `scripts/verify-p11-counts.py` | `f23598724c86c946625e709b313d4f956d4aeaeb` | `python scripts/verify-p11-counts.py` | *(vacía)* |
| P12 Credencial (0,4) | Cerrado 100 % (rotación declarada 13-sep; árbol limpio) | Cajas Ibarra (acta) + Panamá Murillo (verificador) | Acta de rotación Neon; verificador de secretos | `docs/despliegue/NEON-ROTATION-ACTA.md`, `scripts/verify-p12-secrets.py` | `f0e22b474c3e9dbdc9b16a5fbefc1f5d6c37844e` (Cajas), `ca5bee901197c0c28c6932694b45f323f2c75448` (Panamá) | `python scripts/verify-p12-secrets.py` | *(vacía)* |
| EV-1/EV-2 Expediente+verify | Operativo (solo-lectura, UTF-8 sin PYTHONUTF8, exit 0 sin FALLO; PENDIENTEs visibles P3/P10/firmas) | Panamá Murillo | Expediente sin fechas futuras ni elipsis; orquestador `verify-all.py`; verifiers P1-P12; evidencia por SHA; job CI | `VERIFICACION.md`, `Makefile`, `scripts/verify-all.py`, `scripts/verify-*.py`, `docs/evidencia/examen/` | `ca5bee901197c0c28c6932694b45f323f2c75448`, `6549becb6cb5e32a0efdd99fdc691d7a47ada311`, `840ba5c17d22e4b60bbafa1232243bf44ef002c6`, `4a4a26cb8fa1e592f64468bc8449ffde9948a6bf` (PLAN ignorado Fase 0.1), `1c28085ffcc66098e558b49634397d6984f4df4b`, `902e5133e929b6c03a7790baf136bf5d7767fa8b`, `d92ba03a28cebd970a15b841dfbbaf9ac1ab534d` (solo-lectura+UTF-8), `059b46b974b4b180264de2519cb56c7d51a2e2e5` + siguientes (expediente por SHA) | `python scripts/verify-all.py` | *(vacía)* |

Lectura de la columna **Aceptación**: las celdas de la tabla quedan como
marcador histórico, pero la aceptación institucional válida está en la
sección **Firmas y aceptaciones** al final de este documento. Cruce por
fila: P1 Panamá+Cajas; P2 Loor+Panamá; P3 Cajas+Panamá solo como
retiro/N=0 sin puntaje; P4 Panamá+Loor; P5 Panamá como migración final
y Loor+Cajas como trabajo previo parcial; P6 Cajas+Panamá; P7
Cajas+Loor; P8/P9 Panamá+Loor; P10 Panamá+Cajas; P11 Panamá; P12
Cajas+Panamá; EV-1/EV-2 Panamá.

Nota cookie/regresión §1 (punto ya resuelto, restaurado): `3d6d53382550a2c0e6526a3b99bfd63a61e431ad`
(Panamá, degradación a variable) → restaurado a literal en prod por
`c731f117` (`fix(auth): cookie Secure+SameSite=None literal en prod`)
vía `RefreshCookieConfig` (excepción solo en perfil `dev-local-http`);
test 23/23 en `AuthControllerTest`.

## B. Roles CRediT (base verificada a `0ca73a0f`)

Totales globales a `e55f43b0`: **Cajas 763, Loor 343, Panamá 363**
(763+343+363+1+1 = 1471 = `rev-list`; +1 TeilorSuit esqueleto Angular
no atribuible, +1 bot excluido — ver `CONTRIBUTORS.md`). Los conteos
por área cuentan commits que tocan el área de evidencia, no autoría
exclusiva. Re-sincronizar al cierre listando SHAs explícitos, nunca
solo agregados.

Convenciones de esta tabla: cada celda trae conteo + artefacto, o el
estado honesto cuando no hay: **"declarado sin artefacto
individualizado"** (el cap. 13 lo asigna pero no se encontró
artefacto propio), **"—"** (sin asignación ni evidencia),
**"no aplica"** (rol imposible en este proyecto).

| Rol CRediT | Cajas (763) | Loor (343) | Panamá (363) |
|---|---|---|---|
| Conceptualization | Declarado sin artefacto individualizado (cap. 13 lo asigna; el alcance de módulos se infiere del volumen backend, sin ADR propio) | **22** `docs/adr` (ADR-001/007/013, estrategia ramas y versionado) + **10** `docs/arquitectura` (`workspace.dsl`, C4) | Declarado sin artefacto individualizado (cap. 13 lo asigna; sin ADR/arquitectura propios) |
| Data curation | **10** `mediciones/sec` (índice, commit `4b7f50a75f7e0236573721168e8bee61e1e52c5b`) + retiro SUS (`e6df3ff6ad3796f7ea4d41b97674d0741862043d`) | **21** `mediciones/sec` + **8** `mediciones/perf` + `DATA-DICTIONARY.md`, `DATA-PROVENANCE.md` | Retiro SUS del árbol Fase 0.1 (`1c28085ffcc66098e558b49634397d6984f4df4b`) + **3** `mediciones/perf` (`9f3702705eab68971b9d275b59da36da57a0ce06` REPORT policy, `b82ae0e231c7ddd5db4074126c62f40f0301c26a` JSONs, `6549becb6cb5e32a0efdd99fdc691d7a47ada311` REPORT+p95) + `ETHICS.md`, `DATA-DICTIONARY.md` (sección SUS, 22 campos) |
| Formal analysis | — | **5** `k6/`+`perf-analysis.py` (Wilcoxon pareado + Cliff's delta) + **8** `mediciones/perf` | Notebook SUS modo N=0 (`902e5133`) + **8** `scripts` |
| Funding acquisition | No aplica | No aplica | No aplica (PFC sin financiamiento externo) |
| Investigation | **10** `mediciones/sec` + **76** `src/test` + reporte SQL A.2.3 (`b683847`) | **21** `mediciones/sec` + **6** `bibliografia.bib` (verificación Crossref) | **31** `src/test` + **8** `docs/requisitos` (E2E frontend préstamos/reservas/multas, `84ffc30`) |
| Methodology | **10** `docs/adr` (ADR-012/015/016 según `CONTRIBUTORS.md`) | **22** `docs/adr` + protocolo reproducibilidad notebook | **1** `docs/adr` (matiz posicional ADR-006) |
| Project administration | — | **22** `docs/adr` + estructura `docs/`, versionado, ramas | — |
| Resources | **12** `backup-service/` + **15** `docs/despliegue` (`render.yaml`, `docker-compose.yml`, acta Neon `f0e22b474c3e9dbdc9b16a5fbefc1f5d6c37844e`) | **8** `docs/despliegue` | **2** `backup-service/` |
| Software | **250** `frontend-angular/src` + backend (servicios, controladores) + **76** `src/test` + **56** procs/migraciones (módulos Préstamos/Reservas/Multas) | **18** `frontend-angular/src` + **16** `scripts` + JWT/cookies/Redis (`GlobalExceptionHandler`, TTL) | **179** `frontend-angular/src` + **31** `src/test` + verificadores examen + cookie `3d6d53382550a2c0e6526a3b99bfd63a61e431ad` |
| Supervision | No aplica a integrantes (rol del docente-director, ver nota cap. 13) | No aplica a integrantes | No aplica a integrantes |
| Validation | **76** `src/test` + **10** `mediciones/sec` (ZAP, E2E préstamo-devolución-multa, SP multi-OUT `baa0820`) | **21** `mediciones/sec` + **8** `lighthouse` + **15** `jacoco` | **31** `src/test` + E2E frontend vs backend real + WCAG AA (`1640dd9`, `b1399a2`) |
| Visualization | **1** `docs/arquitectura` + primeros C4/ER (`2205566`) | **10** `docs/arquitectura` (C4 N1/N2 desde `workspace.dsl`, DER) + **8** `mediciones/perf` (SVG p95) | **179** `frontend` + 29 mockups `docs/mockups/` (figuras SUS retiradas, no cuentan) |
| Writing – original draft | — | **57** `docs/capitulos` (mayoría de capítulos) | — |
| Writing – review & editing | **12** `CITATION.cff` + **11** `docs/trazabilidad` | **57** `docs/capitulos` + **6** `bibliografia.bib` + **15** `CITATION.cff` | Este documento (EV-4, sección A) + **8** `docs/requisitos` + **6** `docs/capitulos` |

## Totales por rol (autores con evidencia concreta)

13/14 roles con evidencia (Funding acquisition: no aplica;
Supervision: docente-director, fuera de integrantes):
Conceptualization 1+2 declarados, Data curation 3, Formal analysis 2,
Investigation 3, Methodology 2, Project administration 1, Resources 3,
Software 3, Validation 3, Visualization 3, Writing–original 1,
Writing–review 3.

## Discrepancias alineadas

- `CONTRIBUTORS.md` no asignaba Conceptualization a Cajas/Panamá ni
  Writing–review a Cajas; el cap. 13 sí. Se conserva la tabla del
  cap. 13 y aquí constan como "declarado sin artefacto
  individualizado" donde no se encontró artefacto propio — no se
  inflan conteos para cubrirlas.
- `CONTRIBUTORS.md` atribuía Formal analysis (SUS) a Panamá y el
  cap. 13 solo a Loor: ambos constan con artefacto propio arriba.
- Conteos globales: `CONTRIBUTORS.md` y
  `docs/mediciones/roles-commit-counts.txt` (este último, antes con
  `--all`) citaban revs distintos; los vigentes a `b500878d` son
  **763/343/363** (este archivo, verificados por
  `verify-p11-counts.py`).

## Firmas y aceptaciones

Cada integrante acepta las filas de la sección A que reconoce como
suyas y sus celdas de la sección B, con nombre, correo institucional y
fecha reales. Las aceptaciones de terceros solo se registran cuando el
integrante las confirma de forma expresa; el agente no firma ni
transcribe firmas por otros integrantes.

- **Panamá Murillo Moisés Antonio** — `mpanamam@uteq.edu.ec` — Fecha: 2026-09-17.
  Declaración: Yo, Panamá Murillo Moisés Antonio, declaro que revisé
  la atribución EV-4 y acepto como reales y verificables las
  contribuciones asignadas a mi nombre en este documento. Acepto mi
  titularidad en P1, P2, P3 únicamente como retiro honesto de evidencia
  mock y N=0, P4, P5, P6, P8/P9, P10, P11, P12 y EV-1/EV-2, según los
  archivos, commits y comandos indicados. No reclamo puntaje para P3
  SUS ni autoría sobre filas no asignadas a mi nombre.
  Filas aceptadas: P1, P2, P3 retiro/N=0, P4, P5, P6, P8/P9, P10,
  P11, P12, EV-1/EV-2 y sección B CRediT de Panamá.
  Firma: Panamá Murillo Moisés Antonio.
- **Loor Medranda Marlon Taylor** — `mloorm14@uteq.edu.ec` — Fecha: 2026-09-18.
  Declaración de aceptación personal: Yo, Loor Medranda Marlon Taylor,
  revisé personalmente la evidencia y los commits indicados en este
  documento. Acepto únicamente las atribuciones que reconozco como
  propias: P2; P4; P5 como trabajo previo parcial; P7; y P8/P9,
  conforme a los archivos, commits y comandos de verificación
  identificados en cada fila.

  Esta aceptación distingue mis aportes históricos de cualquier cierre
  posterior del examen suspenso. No declaro como trabajo nuevo posterior
  a `8d1b7999` ningún cambio que no haya realizado personalmente.

  No reclamo autoría sobre filas no asignadas a mi nombre, no reclamo
  puntaje para P3 SUS y dejo la sección B de conteos CRediT pendiente de
  recálculo contra el HEAD final.

  Filas aceptadas: P2, P4, P5 como trabajo previo parcial, P7 y P8/P9.
  Firma: Loor Medranda Marlon Taylor.
- **Cajas Ibarra Irvin Marcelo** — `icajasi@msuteq.edu.ec` — Fecha: 2026-09-18.
  Declaración de aceptación personal: Yo, Cajas Ibarra Irvin Marcelo,
  revisé personalmente la evidencia listada en este documento y acepto
  únicamente las atribuciones que reconozco como propias: P1 en las
  anotaciones indicadas; P3 únicamente como retiro honesto de evidencia
  sin puntaje; P5 como trabajo previo parcial; P6; P7; P10; y P12,
  según los archivos, commits y comandos indicados en cada fila.

  No reclamo autoría sobre filas no asignadas a mi nombre, no reclamo
  puntaje para P3 SUS y no declaro como trabajo nuevo posterior a
  `8d1b7999` ningún cambio que no haya realizado personalmente.

  Esta aceptación se emite mediante mi propio commit, con mi identidad
  y correo institucional reales. La sección B de conteos CRediT queda
  pendiente de recálculo contra el HEAD final.

  Filas aceptadas: P1 (anotaciones), P3 retiro/N=0, P5 trabajo previo
  parcial, P6, P7, P10 y P12.
  Firma: Cajas Ibarra Irvin Marcelo.

Para el tag de entrega (EV-3/EV-4, solo el administrador): incluir
este archivo únicamente cuando las tres aceptaciones estén completas.
