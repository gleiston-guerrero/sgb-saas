# Higiene de ramas (punto 14d de la rubrica)

Fecha: 2026-09-13. Remotas reales en `origin`: **31**
(`git branch -r`, sin contar `origin/HEAD`).

> Regla del proyecto (`sgb-workflow`): ninguna rama ajena se borra sin
> aprobacion del equipo. Este archivo es el listado con recomendacion;
> el borrado (`git push origin --delete <rama>`) queda pendiente de esa
> aprobacion. Nada se borro en este commit.

## Conservar (trabajo activo o referencia)

| Rama | Ultimo commit | Motivo |
|---|---|---|
| `main` | 2026-09-12 docs: incluir SRS firmado con acta | rama de entrega |
| `demo/interfaces-completas` | 2026-09-03 | rama base del equipo (integracion) |
| `develop` | 2026-06-20 | desarrollo (solo `origin`) |
| `fix/procedures` | 2026-09-12 | trabajo activo de procedimientos |
| `fix/nombres-codigo` | 2026-09-12 | trabajo activo de nombres (fusionada a `main`, conservar hasta cierre) |
| `fix/merge-final-pfc` | 2026-09-11 merge de `main` | integracion en curso |

## Candidatas a borrar: fusionadas a `main` (`--merged`)

Ya integradas, sin contenido unico pendiente:

| Rama | Ultimo commit |
|---|---|
| `DEMO-FINAL` | 2026-09-05 |
| `DEMO-PRESENTAR` | 2026-09-01 |
| `Presentacion_Final` | 2026-09-09 |
| `final-biblioteca` | 2026-08-28 |

Comando por rama (tras aprobacion):
`git push origin --delete DEMO-FINAL` (y asi con cada una).

## Candidatas a borrar: obsoletas no fusionadas

Features/docs/tmp de agosto ya integrados por otra via o abandonados;
requieren confirmacion del autor antes de borrar:

| Rama | Ultimo commit |
|---|---|
| `Demo_PFC` | 2026-09-01 |
| `Fix-Demo` | 2026-09-03 |
| `Test-Backup` | 2026-09-06 |
| `backup/main-pre-sync-2026-09-01` | 2026-08-17 |
| `chore/organizar-raiz` | 2026-09-11 |
| `conf-produccion` | 2026-08-26 |
| `configurar-biblioteca` | 2026-08-27 |
| `docs/fix-srs` | 2026-09-10 |
| `docs/flujo-mvc-ta` | 2026-08-07 |
| `feature/autocompletar-isbn` | 2026-08-16 |
| `feature/diagrama-flujo-mvc-cajas` | 2026-08-09 |
| `feature/diagrama-flujo-mvc-panama` | 2026-08-09 |
| `feature/portal-publico-catalogo` | 2026-08-16 |
| `feature/prestamos-backend` | 2026-07-30 |
| `fix/fuentes-autohospedadas-fundacion` | 2026-08-16 |
| `frontend/fundacion` | 2026-08-16 |
| `refactor/mis-prestamos-reservaciones-tailwind` | 2026-08-16 |
| `refactor/sonarqube-dedup` | 2026-09-07 |
| `tmp/audit-dashboard-gerente` | 2026-08-17 |
| `tmp/audit-tangled-commit` | 2026-08-16 |

## Verificacion de las otras partes del punto 14

- (a) HTML JaCoCo: `git ls-files docs/mediciones/jacoco/ | grep .html` ->
  0 archivos (eliminados en `f69165a2`, `.gitignore` los bloquea).
- (b) JSON k6 pesados: `git ls-files k6/ docs/mediciones/perf/` -> solo
  `REPORT.md`, 1 pdf, 1 svg y 2 `.js`; 0 JSON.
- (c) `.msg`: `git ls-files "*.msg"` -> 0.
- (e) Neon: ver `docs/despliegue/NEON-ROTATION-ACTA.md`.
