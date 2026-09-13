# Fuente canonica de cobertura JaCoCo

Este directorio conserva corridas historicas y una medicion vigente.

## Medicion vigente

La unica cifra defendida por el informe final es la de los archivos ubicados
directamente en este directorio:

- `docs/mediciones/jacoco/report.csv`
- `docs/mediciones/jacoco/report.xml`
- `docs/mediciones/jacoco/html/`

Estos artefactos corresponden a la corrida de cierre citada en
`docs/capitulos/08-resultados.tex`: 85,17 % de lineas
(`1734 / 2036`) y 70,17 % de ramas (`407 / 580`), calculado desde
`report.csv` (corrida `mvnw clean verify` del 2026-09-13: 584 tests,
0 fallos; servicios 83,63 %/70,05 %, controladores 95,54 %/70,41 %).

## Corridas historicas

Las carpetas fechadas y los reportes Markdown fechados se conservan solo como
trazabilidad historica. No sustituyen a `report.csv` ni a `report.xml` como
fuente de cierre:

- `2026-08-25-jacoco-report/`
- `2026-08-25-jacoco-final/`
- `2026-08-27-jacoco-cierre-final/`
- `2026-08-25-cobertura-jacoco-cierre-entrega-final.md`
- `2026-08-25-cobertura-jacoco-final.md`
- `2026-08-27-cobertura-jacoco-cierre-final.md`

Si se genera una nueva medicion, primero debe reemplazar los artefactos raiz
(`report.csv`, `report.xml`, `html/`) y luego actualizar este archivo, el
capitulo de resultados, `DATA-DICTIONARY.md` y `DATA-PROVENANCE.md` en el
mismo commit.
