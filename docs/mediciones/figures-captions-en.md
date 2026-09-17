# Figures captions (English)

This file collects short English captions for key figures used in the
mediciones report, to support reviewers who expect English figure text.
The authoritative captions live in the LaTeX chapters
(\texttt{docs/capitulos/}); this file mirrors them. Figures under
\texttt{docs/mediciones/figuras/} are generated reproducibly with
\texttt{scripts/generar-figuras-evaluacion.py} (Okabe-Ito palette,
English in-figure text).

- p95-comparacion-escenarios.svg / p95-comparacion-escenarios.pdf:
  "p95 latency comparison between cache_hot and cache_cold across 5 runs.
  Error bars show 95% confidence intervals estimated by bootstrap (2000
  replicates, seed=42). Palette: Okabe-Ito (color-blind accessible)."

- sus_items_breakdown.svg / sus_items_breakdown.pdf:
  "Mean SUS item scores (1-5 Likert) for the mock dataset. This dataset is
  withdrawn from the deliverable (N=0) due to lack of provenance; figure
  is retained for methodological illustration only."

- p95-comparacion-escenarios.pdf (also PDF):
  "Same caption as the SVG: p95 latency comparison between cache_hot and cache_cold across 5 runs. Error bars show 95% CI by bootstrap (2000 replicates, seed=42)."

- jacoco coverage summary figures (if present):
  "Coverage summary derived from JaCoCo XML export (report.csv). Use `docs/mediciones/jacoco/report.xml` as the canonical source for line/branch coverage figures."

- diagramas/er-english.pdf (ER diagram):
  "Entity-relationship diagram (real schema reconstructed from database
  migrations). Use this diagram to map tables and foreign key relations
  mentioned in the evaluation."

- figuras/fig-k6-distribucion-latencia (new, P8):
  "Latency distribution of http_req_duration per scenario, 5 runs pooled
  (2026-09-17 series)."

- figuras/fig-k6-throughput (new, P8):
  "Throughput (req/s) per run and scenario, GET /api/v1/libros under
  50 VUs (2026-09-17 series)."

- figuras/fig-jacoco-paquetes (new, P8):
  "JaCoCo instruction coverage by package (top 12), from the canonical
  docs/mediciones/jacoco/report.csv (49 instrumented classes)."

- figuras/fig-lighthouse-puntajes (new, P8):
  "Mean Lighthouse scores per category and profile, post-fix production
  round (2026-08-18, 3 runs per profile)."

- figuras/fig-zap-riesgos (new, P8):
  "ZAP Ajax Spider alerts by risk level (local stack, 2026-08-17,
  11 alerts)."

- figuras/fig-commits-mensuales (new, P8):
  "Commit activity per month (no merges)."

- figuras/fig-commits-autores (new, P8):
  "Commits by author (no merges, identities unified with .mailmap)."

- figuras/fig-endpoints-roles (new, P8):
  "Backend API authorization annotations per role (79 method-level
  @PreAuthorize annotations)."

- figuras/fig-migraciones-acumuladas (new, P8):
  "Cumulative versioned Flyway migrations over time
  (database/migrations/V*.sql)."
