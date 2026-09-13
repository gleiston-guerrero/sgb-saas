# Fuente canonica de cobertura JaCoCo

Este directorio conserva corridas historicas y una medicion vigente.

## Medicion vigente

La unica cifra defendida por el informe final es la de los archivos ubicados
directamente en este directorio:

- `docs/mediciones/jacoco/report.csv`
- `docs/mediciones/jacoco/report.xml`
- `docs/mediciones/jacoco/html/`

Estos artefactos corresponden a la corrida de cierre citada en
`docs/capitulos/08-resultados.tex`: 88,06 % de lineas
(`1793 / 2036`) y 76,90 % de ramas (`446 / 580`), calculado desde
`report.csv` (corrida `mvnw clean verify` del 2026-09-13: 615 tests,
0 fallos; servicios 87,38 %/77,70 %, controladores 97,03 %/75,51 %).

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
