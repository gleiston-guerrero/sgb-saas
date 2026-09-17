# Procedencia de datos — tablas y figuras del informe académico

Traza cada tabla y figura con datos cuantitativos citada en
`docs/capitulos/*.tex` hacia el archivo crudo que la origina, el script
que la produce (si existe uno versionado), y el commit en que se generó
por última vez. Requisito R2 de la guía. Complementa a
`docs/mediciones/DATA-DICTIONARY.md` (qué campos tiene cada archivo
crudo) sin repetirlo -- este archivo responde "de dónde viene", no "qué
forma tiene".

**Fecha**: 2026-09-16 (reescritura P1 examen suspenso). **Método**: el
commit de cada archivo crudo se obtuvo con `git log -1 -- <archivo>`
sobre el historial vigente, y cada hash citado se verificó con
`git cat-file -t` (todos devuelven `commit`; ver sección "Verificación
de hashes citados" al final). No se cita ningún commit inexistente: los
hashes históricos que la reescritura del historial dejó sin referente se
sustituyeron por el commit vigente que tocó por última vez el mismo
archivo. No se reescribió el historial para restaurarlos.

**Columna "Script"**: varias tablas se calcularon con un conteo manual
verificado (p. ej. `python3` con `csv.DictReader` ejecutado durante la
tarea correspondiente, no guardado como script versionado en `scripts/`)
-- se declara así explícitamente en vez de sugerir que existe un script
reproducible con un nombre de archivo cuando en realidad no lo hay.

| # | Tabla / Figura | Capítulo | Archivo(s) crudo(s) de origen | Script | Commit |
|---|---|---|---|---|---|
| 1 | `tab:res-perf-descriptivo` (estadística descriptiva de rendimiento) | 08-resultados.tex, §Rendimiento | `docs/mediciones/perf/REPORT.md` y artefactos derivados; los NDJSON `k6-run1.json` … `k6-run5.json` fueron la fuente histórica y se retiraron del árbol final por higiene. | `scripts/perf-analysis.py` (bootstrap 2000 réplicas, semilla `BOOTSTRAP_SEED = 42`) | `REPORT.md`: `bca23c2c`. Tabla en el capítulo: `00b26306`. |
| 2 | `fig:res-perf-comparacion` (`p95-comparacion-escenarios.pdf`) | 08-resultados.tex, §Rendimiento | `docs/mediciones/perf/REPORT.md`, `p95-comparacion-escenarios.svg` y `p95-comparacion-escenarios.pdf`; los NDJSON crudos fueron retirados del árbol final por higiene. | `scripts/perf-analysis.py` genera el `.svg`; conversión a `.pdf` con `svglib`/`reportlab` fue un paso manual de esta redacción, **no** un script versionado en `scripts/`. | `.svg` y `.pdf`: `437ebe5b`. Inclusión en el capítulo: `00b26306`. |
| 3 | `tab:res-owasp` (6 controles OWASP auditados) | 08-resultados.tex, §Seguridad | Los 16 archivos de `docs/mediciones/sec/*.md` (evidencia manual vía `curl` contra el stack Docker real) | Sin script de agregación automática de los 16 en una tabla -- `scripts/owasp-audit.sh` (`make audit`) solo re-verifica 4 de los 6 controles (A01/A03/A07/A09), no agrega la tabla del informe. | Evidencia manual original: rango entre `75d635b1` y `e6909a5d` (2026-07-21). Re-verificación automatizada más reciente: `4d69f244`. Tabla en el capítulo: `00b26306`. |
| 4 | `tab:res-jacoco` (cobertura JaCoCo vigente) | 08-resultados.tex, §Cobertura | `docs/mediciones/jacoco/report.xml`, `report.csv` y `docs/mediciones/jacoco/README.md`; las carpetas fechadas permanecen como historial y no sustituyen la medición raíz | `jacoco-maven-plugin` vía `./mvnw clean verify` (Maven, no un script propio del repositorio). La cifra defendida se recalcula desde `report.csv`: 1805 líneas cubiertas de 2065 (87,41 %) y 448 ramas cubiertas de 588 (76,19 %), con servicios en 87,38 %/77,70 % y controladores en 97,03 %/75,51 % (corrida `mvnw clean verify` con 620 tests, 0 fallos, sobre `main` tras V51 -- procedures nativos P4 -- y la migración de 3 `nativeQuery` a JPQL). | Reporte raíz vigente: re-generado 2026-09-13 sobre `main` (commit `1028ad02` y posteriores). Tabla en el capítulo: actualizada en `00b26306`. Canonicidad documentada en `docs/mediciones/jacoco/README.md`. |
| 5 | `tab:res-lighthouse` (2 corridas móvil) | 08-resultados.tex, §Calidad web | `docs/mediciones/lighthouse/lhci-20260731-0300.json` (corrida 1), `lhci-20260731-0330.json` (corrida 2) | `@lhci/cli` vía `frontend-angular/lighthouserc.js` (herramienta de terceros, no script propio) | Corrida 1: `df11c6ed`. Corrida 2 (post-fix SEO): `ed42c421`. Tabla en el capítulo: `00b26306`. |
| 6 | `tab:res-resumen` (resumen de los 5 bloques) | 08-resultados.tex, §Resumen | Deriva de las filas 1, 3, 4 y 5 de esta tabla (sin archivo crudo propio -- es una síntesis, no una nueva medición) | N/A | Síntesis redactada en `00b26306` (mismo commit que fijó las cifras de las filas fuente). |
| 7 | `tab:trabajos-comparativa` (10 trabajos primarios) | 03-trabajos-relacionados.tex | `docs/bibliografia.bib` (34 referencias, verificadas contra Crossref) + resúmenes indexados de cada trabajo consultados directamente (no descargados como archivo al repositorio) | Sin script -- síntesis narrativa manual, declarada así en el propio capítulo (§Estrategia de búsqueda). | `docs/bibliografia.bib`: `862672b2`. |
| 8 | `fig:prisma-flow` (diagrama de flujo de selección) | 03-trabajos-relacionados.tex | Mismo acervo que la fila anterior; los conteos de cada etapa (15→15→15→15→10) están documentados en prosa en el propio capítulo, no en un archivo de datos separado | Sin script -- conteo manual. | Capítulo con el diagrama: `9a467125`. |
| 9 | Tabla de mapeo ADR ↔ 6 temas obligatorios del Bloque D (sin `\label`, §Decisiones arquitectónicas documentadas) | 06-diseno-arquitectura.tex | Los 13 archivos de `docs/adr/*.md` (y su índice `docs/adr/README.md`) | Sin script -- mapeo manual, documentado también en `docs/adr/README.md`. | ADRs (renumeración OBS-15): `6bce6257`. Tabla en el capítulo: `5f3e5e5b`. |
| 10 | Tabla de atributos de calidad ISO/IEC 25010 (sin `\label`, §Atributos de calidad) | 06-diseno-arquitectura.tex | `docs/arquitectura/ISO25010.md` (reproducida verbatim según declara el propio capítulo, con una nota al pie que corrige la cifra desactualizada de "6 ADRs" del archivo fuente) | Sin script -- documento fuente redactado manualmente, con las cifras de rendimiento tomadas de la fila 1 de esta tabla. | Fuente (`ISO25010.md`): `6bce6257`. Tabla en el capítulo: `5f3e5e5b`. |
| 11 | `tab:matriz-resumen` (distribución por `tipo_acceso`, §Matriz de trazabilidad, `sec:matriz-resumen`) | 06-diseno-arquitectura.tex | `docs/trazabilidad/matriz.csv` (43 filas) | Sin script versionado -- conteo manual verificado con `python3` (`csv.DictReader`) durante la tarea que corrigió esta tabla, no un script guardado en `scripts/`. | `docs/trazabilidad/matriz.csv`: `7debd0b1` (fusión de arreglos finales, mismo commit que recalculó la tabla del capítulo). |
| 12 | Tabla de distribución MoSCoW / tipo / estado (sin `\label`, §Los 43 requisitos: categorización MoSCoW) | 09-ingenieria-requisitos.tex | `docs/trazabilidad/matriz.csv` | Sin script de agregación -- `scripts/validate-traceability.sh` valida el esquema/presencia de columnas (incl.\ `tipo_acceso`, `estado`), pero no calcula estas cuentas/porcentajes; conteo manual. | Fuente (`matriz.csv`): `7debd0b1`. Capítulo: `e3f3f7fa`. |
| 13 | Cifra "100\,% de los `Must` verificados" (prosa, §Proceso de validación) | 09-ingenieria-requisitos.tex | `docs/trazabilidad/matriz.csv` (columna `estado`) | Sin script -- cierre logrado agregando 2 tests nuevos (`AuthControllerTest`, `AuthServiceTest`) que permitieron pasar `REQ-F-001`/`REQ-NF-013` de `implementado` a `verificado`. | Tests que cerraron el gap + matriz: `7debd0b1`. Prosa en el capítulo: `e3f3f7fa`. |
| 14 | Tasa de estabilidad "95,3\,%" (prosa, §Estabilidad del conjunto de requisitos) | 09-ingenieria-requisitos.tex | `docs/requisitos/CHANGELOG-REQ.md` (comparación `SRS-v0.9.0-rc.md` de 30 requisitos vs.\ `SRS-v1.0.0.md` de 43) | Sin script versionado en `scripts/` -- comparación cuerpo-completo (no solo título) hecha de forma ad-hoc para producir `CHANGELOG-REQ.md`, no repetible desde el repositorio tal cual hoy. | `CHANGELOG-REQ.md`: `7debd0b1`. Prosa en el capítulo (incluida la corrección honesta de la cifra de "verificado" desactualizada del propio `CHANGELOG-REQ.md`, 48,8\,% → 53,5\,%): `e3f3f7fa`. |
| 15 | `tab:decl-credit` (distribución de roles CRediT) | 13-declaraciones.tex | Historial completo de `git log`/`git shortlog -sne --all` (recuento de commits por autor) + `docs/observaciones/OBSERVACIONES.md` (asignación de OBS-08 a Panamá) | Sin script versionado -- conteo manual de `git shortlog` ejecutado durante la redacción del capítulo, no un script guardado en `scripts/`. | Confirmación final de la tabla: `df09f0db` (tras la revisión del equipo). Capítulo: `d8443e66`. |
| 16 | Bloque SUS — RETIRADO como resultado evaluable | Resultados, §Usabilidad (pendiente) | Sin datos crudos versionados: $N=0$, sin export del instrumento y sin consentimientos firmados publicables/verificables en el repositorio | N/A para esta entrega; el protocolo e instrumento quedan documentados para una futura corrida con consentimiento | No hay figura, tabla descriptiva ni cifra SUS vigente. |

## Notas de honestidad sobre este archivo

- **Ninguna fila usa un script que no exista de verdad.** Donde la
  columna "Script" dice "sin script", se verificó primero que no hubiera
  uno versionado en `scripts/` para esa tabla específica (búsqueda
  directa en el directorio, no una suposición) -- son conteos manuales
  reales, no automatizados, y se declaran como tales en vez de sugerir
  una reproducibilidad que no existe hoy.
- **`scripts/validate-traceability.sh` no calcula ninguna de las cifras
  citadas** en las filas 11 y 12 -- valida que las columnas de
  `matriz.csv` tengan el esquema y los valores permitidos correctos
  (incluye `tipo_acceso` en su lista de columnas validadas), pero no
  agrega conteos ni porcentajes. Confundir "el CSV pasa la validación de
  esquema" con "la tabla se generó automáticamente" habría sido
  inexacto.
- **Las filas 3 y 8 no tienen un único archivo o commit "fuente"
  limpio** porque agregan evidencia de varios archivos generados en
  fechas distintas (16 archivos OWASP a lo largo de 3 semanas; el acervo
  bibliográfico de 34 referencias construido en dos tandas) -- se citan
  los commits más relevantes de cada extremo del rango en vez de forzar
  un solo commit que no representaría el proceso real.
- Esta tabla no incluye las tablas puramente cualitativas sin datos
  cuantitativos que trazar a un archivo crudo (p.~ej. la tabla de
  autores/C.I./ORCID de la portada, `docs/capitulos/00-portada.tex`) --
  fuera del alcance de "tabla o figura con datos cuantitativos" que pide
  esta tarea.

## Verificación de hashes citados

Todos los hashes citados en la columna "Commit" existen en el árbol
vigente (reescritura P1 del examen suspenso, 2026-09-16). Comprobación:

```powershell
foreach ($h in @('bca23c2','437ebe5','75d635b','e6909a5','4d69f24','1028ad02','df11c6e','ed42c42','00b2630','862672b','9a46712','6bce625','5f3e5e5','7debd0b','e3f3f7f','df09f0d','d8443e6','454be77')) { $r = git cat-file -t $h 2>$null; if ($?) { echo "$h EXISTE ($r)" } else { echo "$h FALTA" } }
```

Salida (2026-09-16, rama `fix/rescate-produccion-examen`):

```text
bca23c2 EXISTE (commit)
437ebe5 EXISTE (commit)
75d635b EXISTE (commit)
e6909a5 EXISTE (commit)
4d69f24 EXISTE (commit)
1028ad02 EXISTE (commit)
df11c6e EXISTE (commit)
ed42c42 EXISTE (commit)
00b2630 EXISTE (commit)
862672b EXISTE (commit)
9a46712 EXISTE (commit)
6bce625 EXISTE (commit)
5f3e5e5 EXISTE (commit)
7debd0b EXISTE (commit)
e3f3f7f EXISTE (commit)
df09f0d EXISTE (commit)
d8443e6 EXISTE (commit)
454be77 EXISTE (commit)
```

Detalle por hash en `docs/mediciones/hashes-verification-report.md`.
No se reescribió el historial para restaurar hashes ausentes.

## Referencias

- `docs/mediciones/DATA-DICTIONARY.md` (qué campos tiene cada archivo crudo)
- `docs/mediciones/README.md` (convención general de evidencia)
- `docs/trazabilidad/matriz.csv`, `scripts/validate-traceability.sh`
