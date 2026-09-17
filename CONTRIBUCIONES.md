# Contribuciones por rol CRediT — SGB-SaaS (base verificable)

Rev de referencia: `840ba5c17d22e4b60bbafa1232243bf44ef002c6`
(rama `fix/examen-evidencia`, punto tras el commit 1 de evidencia).
Cada número de abajo se verificó con listados explícitos de commits
(SHA visibles en el reporte de ejecución); no se aceptan agregados
sin su lista. Este archivo se commitea en el commit siguiente, que
suma +1 a Panamá (declarado); re-sincronizar al cierre con los
comandos de abajo.
Comandos reproducibles (todos con `--no-merges`; áreas se solapan, no suman):

```powershell
git -c log.mailmap=true shortlog -sne --no-merges <rev>          # totales
git rev-list --count --no-merges <rev>                            # 1434
git log --no-merges --author=<icajasi|mloorm14|mpanamam> --format=%h <rev> -- <ruta>  # por área, listar SHAs
```

Totales globales a `840ba5c1`: **Cajas 763, Loor 343, Panamá 326**
(763+343+326+1+1 = 1434 = `rev-list`; +1 TeilorSuit esqueleto Angular
no atribuible, +1 bot excluido — ver `CONTRIBUTORS.md`). Los conteos
por área cuentan commits que tocan el área de evidencia, no autoría
exclusiva.

Convenciones de esta tabla: cada celda trae conteo + artefacto, o el
estado honesto cuando no hay: **"declarado sin artefacto
individualizado"** (el cap. 13 lo asigna pero no se encontró
artefacto propio), **"—"** (sin asignación ni evidencia),
**"no aplica"** (rol imposible en este proyecto).

| Rol CRediT | Cajas (763) | Loor (343) | Panamá (326) |
|---|---|---|---|
| Conceptualization | Declarado sin artefacto individualizado (cap. 13 lo asigna; el alcance de módulos se infiere del volumen backend, sin ADR propio) | **22** `docs/adr` (ADR-001/007/013, estrategia ramas y versionado) + **10** `docs/arquitectura` (`workspace.dsl`, C4) | Declarado sin artefacto individualizado (cap. 13 lo asigna; sin ADR/arquitectura propios) |
| Data curation | **10** `mediciones/sec` (índice, commit `4b7f50a`) + **5** `mediciones/sus` (esqueleto + script, `1ad718b`) | **21** `mediciones/sec` + **8** `mediciones/perf` + `DATA-DICTIONARY.md`, `DATA-PROVENANCE.md` | **2** `mediciones/sus` (retiro declarado N=0, OBS-08) + **3** `mediciones/perf` (`9f370270` REPORT policy, `b82ae0e2` JSONs, `6549becb` REPORT+p95) + `ETHICS.md`, `DATA-DICTIONARY.md` (sección SUS, 22 campos) |
| Formal analysis | — | **5** `k6/`+`perf-analysis.py` (Wilcoxon pareado + Cliff's delta) + **8** `mediciones/perf` | `scripts/sus-analysis.ipynb` (reescritura: descriptiva, IC95%, desglose por ítem) + **8** `scripts` |
| Funding acquisition | No aplica | No aplica | No aplica (PFC sin financiamiento externo) |
| Investigation | **10** `mediciones/sec` + **76** `src/test` + reporte SQL A.2.3 (`b683847`) | **21** `mediciones/sec` + **6** `bibliografia.bib` (verificación Crossref) | **31** `src/test` + **8** `docs/requisitos` (E2E frontend préstamos/reservas/multas, `84ffc30`) |
| Methodology | **10** `docs/adr` (ADR-012/015/016 según `CONTRIBUTORS.md`) | **22** `docs/adr` + protocolo reproducibilidad notebook | **1** `docs/adr` (matiz posicional ADR-006, 2026-09-18) |
| Project administration | — | **22** `docs/adr` + estructura `docs/`, versionado, ramas | — |
| Resources | **12** `backup-service/` + **15** `docs/despliegue` (`render.yaml`, `docker-compose.yml`, `INTERNAL_API_KEY`) | **8** `docs/despliegue` | **2** `backup-service/` |
| Software | **250** `frontend-angular/src` (fixes reales de router/dashboard/catálogo, ej. `b5eb7cf`) + backend (servicios, controladores) + **76** `src/test` + **56** procs/migraciones (módulos Préstamos/Reservas/Multas, `backup-service/src` `126a7ee`) | **18** `frontend-angular/src` + **16** `scripts` (build, `mediciones-header.sh`) + JWT/cookies/Redis (`GlobalExceptionHandler`, TTL) | **179** `frontend-angular/src` (auth, libros, auditoría, rebrand Leibri `02b3686`, modo oscuro `366cc40`) + **31** `src/test` |
| Supervision | No aplica a integrantes (rol del docente-director, ver nota cap. 13) | No aplica a integrantes | No aplica a integrantes |
| Validation | **76** `src/test` + **10** `mediciones/sec` (ZAP, E2E préstamo-devolución-multa, SP multi-OUT `baa0820`) | **21** `mediciones/sec` + **8** `lighthouse` + **15** `jacoco` (verificación en vivo por cambio) | **31** `src/test` + E2E frontend vs backend real + WCAG AA (`1640dd9`, `b1399a2`) |
| Visualization | **1** `docs/arquitectura` + primeros C4/ER (`2205566`) | **10** `docs/arquitectura` (C4 N1/N2 desde `workspace.dsl`, DER real `der-real.pdf`) + **8** `mediciones/perf` (SVG p95) | **179** `frontend` + 29 mockups `docs/mockups/` (figuras SUS retiradas, no cuentan) |
| Writing – original draft | — | **57** `docs/capitulos` (mayoría de capítulos) | — |
| Writing – review & editing | **12** `CITATION.cff` + **11** `docs/trazabilidad` (unificación cifras, refs rotas) | **57** `docs/capitulos` + **6** `bibliografia.bib` (Peffers, Hevner) + **15** `CITATION.cff` | **8** `docs/requisitos` + **6** `docs/capitulos` (`82fcfec1`, `aceddcc3` conteos + `6549becb` evidencia + 3 históricos) |

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
- Conteos globales: `CONTRIBUTORS.md` (727/329/291) y
  `docs/mediciones/roles-commit-counts.txt` (779/355/298, con
  `--all`) citaban revs distintos; los vigentes a `840ba5c1` son
  **763/343/326** (este archivo). Re-sincronizar al cierre con los
  comandos de la cabecera (listar SHAs explícitos, nunca solo
  agregados).

## Firmas y autorizaciones (bloqueo humano explícito)

Estado: PENDIENTES. Este archivo es la base verificable (roles,
conteos, SHA); las firmas/autorizaciones auténticas las aporta cada
integrante fuera de la automatización. No se ha escrito ni simulado
ninguna firma, correo o aprobación en este documento.

Estructura para cuando el equipo las aporte (no rellenar sin ellas):

| Integrante | Correo institucional | Firma/fecha | Alcance aceptado |
|---|---|---|---|
| (pendiente) | (pendiente) | (pendiente) | Roles CRediT y puntos declarados arriba |
| (pendiente) | (pendiente) | (pendiente) | Roles CRediT y puntos declarados arriba |
| (pendiente) | (pendiente) | (pendiente) | Roles CRediT y puntos declarados arriba |

Para el tag de entrega (EV-3/EV-4, solo el administrador): incluir
este archivo con roles, conteos y SHA verificables. Para superar el
tope del 40%, faltan las firmas/autorizaciones auténticas de los
integrantes en CONTRIBUCIONES.md.
