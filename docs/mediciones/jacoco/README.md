# Fuente canonica de cobertura JaCoCo

Este directorio conserva corridas historicas y una medicion vigente.

## Medicion vigente

La unica cifra defendida por el informe final es la de los archivos ubicados
directamente en este directorio:

- `docs/mediciones/jacoco/report.csv`
- `docs/mediciones/jacoco/report.xml`

`docs/mediciones/jacoco/html/` ya no se versiona (commit `7debd0b1`,
higiene del arbol: el reporte HTML de JaCoCo es voluminoso y se regenera
localmente con `mvnw clean verify` en `backend-springboot/target/site/jacoco/`
cuando se necesita inspeccionarlo visualmente -- `report.csv`/`report.xml`
bastan como fuente de cierre versionada. Esta nota reemplaza la mencion
anterior a `html/` como tercer artefacto canonico, que habia quedado
desactualizada tras esa limpieza.

Estos artefactos corresponden a la corrida de cierre citada en
`docs/capitulos/08-resultados.tex`: 87,41 % de lineas
(`1805 / 2065`) y 76,19 % de ramas (`448 / 588`), calculado desde
`report.csv` (corrida `mvnw clean verify` del 2026-09-13 sobre el HEAD que
incluye V51 -- procedures nativos para P4 -- y la migracion de 3
`nativeQuery` a JPQL: 620 tests, 0 fallos; servicios y controladores por
capa quedan en el XML versionado, no se repiten aqui para evitar otra
cifra que se desincronice por separado).

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
