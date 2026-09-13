# Higiene de ramas (punto 14d de la rubrica)

Fecha: 2026-09-13. Remotas reales en `origin`: **6**
(`git ls-remote --heads origin`). Ramas locales en este clon: **7**
(`git branch --format='%(refname:short)'`).

La observacion anterior de unas 66 ramas no provenia de `origin`, sino
del clutter local acumulado en este clon. Se ejecutaron dos limpiezas:

1. `git push origin --delete ...` para retirar ramas remotas cerradas,
   demo, temporales u obsoletas.
2. `git fetch --prune origin` y `git branch -D ...` para retirar ramas
   locales sin contraparte remota, conservando `main` y la rama de
   correccion `fix/correciones`.

## Conservar (trabajo activo o referencia)

| Rama | Ultimo commit | Motivo |
|---|---|---|
| `main` | 2026-09-12 docs: incluir SRS firmado con acta | rama de entrega |
| `demo/interfaces-completas` | 2026-09-03 | rama base del equipo (integracion) |
| `develop` | 2026-06-20 | desarrollo (solo `origin`) |
| `fix/procedures` | 2026-09-12 | trabajo activo de procedimientos |
| `fix/merge-final-pfc` | 2026-09-11 merge de `main` | integracion en curso |
| `chore/organizar-raiz` | 2026-09-11 | referencia de organizacion previa |
| `fix/correciones` | 2026-09-13 | rama local de esta correccion; aun no subida a `origin` |

## Ramas eliminadas de `origin`

Se retiraron 24 ramas remotas que ya eran cierres historicos, demos,
temporales o trabajo absorbido por las ramas de entrega:

`backup/main-pre-sync-2026-09-01`, `conf-produccion`,
`configurar-biblioteca`, `Demo_PFC`, `DEMO-FINAL`, `DEMO-PRESENTAR`,
`docs/fix-srs`, `docs/flujo-mvc-ta`, `feature/autocompletar-isbn`,
`feature/diagrama-flujo-mvc-cajas`, `feature/diagrama-flujo-mvc-panama`,
`feature/portal-publico-catalogo`, `feature/prestamos-backend`,
`final-biblioteca`, `Fix-Demo`, `fix/fuentes-autohospedadas-fundacion`,
`fix/nombres-codigo`, `frontend/fundacion`, `Presentacion_Final`,
`refactor/mis-prestamos-reservaciones-tailwind`,
`refactor/sonarqube-dedup`, `Test-Backup`,
`tmp/audit-dashboard-gerente` y `tmp/audit-tangled-commit`.

## Verificacion posterior a la limpieza

```text
git ls-remote --heads origin -> 6
git branch --format='%(refname:short)' -> 7
git branch -a --format='%(refname:short)' -> 14
git remote prune origin --dry-run -> sin salida
```

Ramas remotas vigentes:

```text
chore/organizar-raiz
demo/interfaces-completas
develop
fix/merge-final-pfc
fix/procedures
main
```

Ramas locales vigentes:

```text
chore/organizar-raiz
demo/interfaces-completas
develop
fix/correciones
fix/merge-final-pfc
fix/procedures
main
```

## Verificacion de las otras partes del punto 14

- (a) HTML JaCoCo: `git ls-files docs/mediciones/jacoco/ | grep .html` ->
  0 archivos (eliminados en `f69165a2`, `.gitignore` los bloquea).
- (b) JSON k6 pesados: `git ls-files k6/ docs/mediciones/perf/` -> solo
  `REPORT.md`, 1 pdf, 1 svg y 2 `.js`; 0 JSON.
- (c) `.msg`: `git ls-files "*.msg"` -> 0.
- (e) Neon: ver `docs/despliegue/NEON-ROTATION-ACTA.md`.
