# Expediente de verificación — examen suspenso SGB-SaaS (EV-1)

Rama de trabajo: `fix/fase01-sus-n0` (rev `d92ba03a`, 2026-09-17).
Cierre: viernes 18-sep-2026 23:55. Etiqueta `v1.1.0`: la mueve el admin al final (ver EV-3).

> Regla: ninguna salida está escrita a mano. Todo bloque `Salida` viene de
> ejecutar el `Comando` tal cual sobre el SHA y fecha indicados. Los puntos
> con `Estado: PENDIENTE` indican qué falta y quién lo cierra; nunca cuentan
> como éxito.
> Evidencia completa de la corrida: `docs/evidencia/examen/d92ba03a/`
> (`verify-all.txt` con cabecera SHA/fecha/comando/salida/código,
> `xelatex-1/2/3.txt`, `bibtex.txt`).

## Pisos 1–4

### Comando

```powershell
git log --oneline --graph -5
git rev-parse v1.1.0
git rev-parse "v1.1.0^{}"
git status --short
```

### Salida (2026-09-17, rev d92ba03a)

```text
* d92ba03a fix(examen): verificadores de solo lectura y UTF-8 sin PYTHONUTF8
* 50f517d8 docs(examen): plantilla EV-4 con titularidad por punto, sin firmas simuladas
* 902e5133 test(examen): endurece verify-p3 y notebook SUS en modo N=0 ejecutado
* c52fc402 docs(examen): actualiza referencias al retiro SUS del arbol
* 1c28085f docs(examen): retira dataset SUS mock del arbol, N=0 consistente
v1.1.0 -> 6803730704269737bce4e4c17b7b61bb89702380 (tag anotado)
v1.1.0^{} -> bec80ecbd8973d93b45894155981ad4409a405c8 (commit, anterior al cierre)
?? docs/evidencia/
```

### Archivo que respalda

- Historial del repositorio (`git log`)
- Sin movimientos de tags, sin merges, sin reescritura de historial

### Resultado

Piso 1 en regla a esta fecha (tag existe, anterior al cierre); el admin
mueve `v1.1.0` al SHA final de entrega. Piso 4: identidad de trabajo
`MoisesPanama <mpanamam@uteq.edu.ec>` (institucional); ver EV-4 para
autoría por punto. Riesgo Piso 3 en tratamiento explícito: Fase 0.1
retiró `sus.csv` y derivados del árbol (P3 declara N=0); sin fechas
futuras en este expediente.

### EV-3/EV-4 (tag y entrega)

Tras la validación final, el administrador mueve `v1.1.0` al commit
de entrega aprobada. `CONTRIBUCIONES.md` trae titularidad por punto con
SHAs verificables y aceptaciones pendientes (las llena cada integrante).
Prohibido para el agente: mover el tag, crear tags alternativos o
modificar el historial.

---

## EV-2 — `make verify`

### Diseño

El target delega en `scripts/verify-all.py` (única fuente de verdad;
`python scripts/verify-all.py` es el equivalente exacto donde no hay
GNU Make — sin `make` en este Windows). Clasifica cada punto como
`evidencia válida`, `PENDIENTE` (visible, nunca aprobado: P3, P5-parcial,
P10-bloqueado, firmas P11) o `FALLO`; sale 0 solo si no hay ningún
`FALLO`. Solo lectura: ningún verificador modifica NDJSON, figuras, PDF
ni evidencia (P4 genera su gráfico en temporal vía `SGB_PERF_GRAFICO`).
UTF-8 interno: no requiere `PYTHONUTF8=1` en Windows.

### Comando

```powershell
python scripts/verify-all.py
```

### Salida (2026-09-17, rev d92ba03a, salida completa en `docs/evidencia/examen/d92ba03a/verify-all.txt`)

```text
===== verify-all: resumen P1-P12 =====
P1: evidencia válida
P2: evidencia válida
P3: PENDIENTE — no puntuable
P4: evidencia válida
P5: pendiente documentado (excepcion tecnica)
P6: evidencia válida
P7: evidencia válida
P8/P9: evidencia válida
P10: PENDIENTE
P11: conteos verificables (firmas externas pendientes)
P12: evidencia válida
Javadoc: evidencia válida (BUILD SUCCESS)
verify-all: exit 0 = coherencia/reproducibilidad de la evidencia disponible, NO cumplimiento academico total (P3 y firmas no puntuan)
```

Código de salida: 0.

### Archivo que respalda

- `Makefile` (target `verify` → `python scripts/verify-all.py`)
- `scripts/verify-all.py` + `verify-p{1,2,3,4,5,6,7,8-p9,11,12}.py`
- `.github/workflows/verify.yml` (job CI)
- `docs/evidencia/examen/d92ba03a/verify-all.txt` (343 líneas: cabecera + salida + código)

### Resultado

Operativo (orquestador completo, solo lectura, UTF-8, exit 0 sin FALLO;
PENDIENTEs visibles: P3 N=0, P5 parcial, P10 sin Docker, firmas P11).

---

## P1 — Procedencia de datos (peso 1,4)

### Comando

```powershell
python scripts/verify-p1-hashes.py
```

### Salida (2026-09-17, rama fix/fase01-sus-n0, completa)

```text
[OK] 00b2630 (commit)
[OK] 00b26306 (commit)
[OK] 1028ad02 (commit)
[OK] 437ebe5 (commit)
[OK] 437ebe5b (commit)
[OK] 454be77 (commit)
[OK] 4d69f24 (commit)
[OK] 4d69f244 (commit)
[OK] 5f3e5e5 (commit)
[OK] 5f3e5e5b (commit)
[OK] 6549becb (commit)
[OK] 6bce625 (commit)
[OK] 6bce6257 (commit)
[OK] 75d635b (commit)
[OK] 75d635b1 (commit)
[OK] 7debd0b (commit)
[OK] 7debd0b1 (commit)
[OK] 862672b (commit)
[OK] 862672b2 (commit)
[OK] 9a46712 (commit)
[OK] 9a467125 (commit)
[OK] 9f370270 (commit)
[OK] bca23c2 (commit)
[OK] bca23c2c (commit)
[OK] d8443e6 (commit)
[OK] d8443e66 (commit)
[OK] df09f0d (commit)
[OK] df09f0db (commit)
[OK] df11c6e (commit)
[OK] df11c6ed (commit)
[OK] e3f3f7f (commit)
[OK] e3f3f7fa (commit)
[OK] e6909a5 (commit)
[OK] e6909a5d (commit)
[OK] e8477021 (commit)
[OK] ed42c42 (commit)
[OK] ed42c421 (commit)
verify-p1: OK (35 hashes existen)
```

### Archivo que respalda

- `docs/mediciones/DATA-PROVENANCE.md`
- `scripts/verify-p1-hashes.py` (falla si un hash citado no es commit)

### Resultado

Cerrado 100 %: los 35 hashes existen y cada fila apunta al commit que
realmente tocó el archivo (fila 4 → `e8477021`, regeneración JaCoCo del
2026-09-17; filas 1–2 declaran los NDJSON versionados con SHA en
REPORT.md; `.svg`/`.pdf` → `6549becb`). La sección "Verificación de
hashes citados" de `DATA-PROVENANCE.md` pega las 35 salidas tal cual.

---

## P2 — DOI (peso 0,6)

### Comando

```powershell
python scripts/verify-p2-dois.py
```

### Salida (2026-09-17, rama fix/fase01-sus-n0, completa)

```text
[OK] 10.5281/zenodo.21712467 (doi.org: 200 -> https://zenodo.org/records/21712467; zenodo: published, 1 archivo(s)) <- VERIFICACION.md, docs\informe-entrega-3.tex
[OK] 10.5281/zenodo.22715710 (doi.org: 200 -> https://zenodo.org/records/22715710; zenodo: published, 1 archivo(s)) <- README.md, VERIFICACION.md
[OK] 10.5281/zenodo.22728199 (doi.org: 200 -> https://zenodo.org/records/22806568; zenodo: published, 1 archivo(s)) <- README.md, VERIFICACION.md
[OK] 10.5281/zenodo.22741050 (doi.org: 200 -> https://zenodo.org/records/22741050; zenodo: published, 1 archivo(s)) <- CITATION.cff, VERIFICACION.md, docs\capitulos\00-portada.tex, docs\capitulos\02-introduccion.tex, docs\capitulos\13-declaraciones.tex
verify-p2: OK (4 DOI resuelven)
```

### Archivo que respalda

- `scripts/verify-p2-dois.py` (barre `*.cff/*.md/*.tex/*.txt`; primario
  `doi.org` con redirects + API Zenodo adicional; excluye el plan local
  no entregable y `docs/evidencia/` como registro histórico)
- `CITATION.cff` (doi 10.5281/zenodo.22741050)

### Resultado

Cerrado 100 %: el DOI roto `22636466` (doi.org 404, reproducido por el
verificador endurecido) desapareció de portada, introducción,
declaraciones, FAIR y `CITATION.cff`; los 4 DOI restantes resuelven
`doi.org` 200 final. Derivados regenerables (`informe-final-text.txt`,
extracción del PDF) se retiran del árbol y se regeneran en fase PDF.

---

## P3 — SUS (peso 1,4)

### Comando

```powershell
python scripts/verify-p3-sus.py
```

### Salida (2026-09-17, rev d92ba03a, completa)

```text
verify-p3: OK (sin sus.csv ni derivados en el arbol evaluado)
verify-p3: OK (sus/README.md declara N=0 y dataset retirado)
verify-p3: OK (ningun .tex cita artefactos SUS retirados)
verify-p3: OK (P3 declara N=0 y estado no aprobado, sin CUMPLE)
verify-p3: PENDIENTE — no puntuable (N=0, sin respuestas reales)
```

### Resultado

PENDIENTE deliberado y honesto (0 %, no se cerrará con datos): N=0 en
todo el entregable, sin instrumento versionado con respuestas ni
consentimientos verificables. Fase 0.1 retiró `sus.csv` (15 respuestas
aparentes) y sus 4 derivados del árbol; el notebook
`scripts/sus-analysis.ipynb` corre en modo N=0 con salidas reales
(5/5 celdas ejecutadas, cero puntajes, cero gráficos). No se fabricará
evidencia.

---

## P4 — k6 (peso 1,0)

### Comando

```powershell
python scripts/verify-p4-k6.py
```

### Salida (2026-09-17, rev d92ba03a, completa)

```text
verify-p4: OK (existen 5 corridas)
verify-p4: OK (5 archivos en LF, sin CRLF)
verify-p4: OK (NDJSON legible, ambos escenarios en las 5)
verify-p4: OK (SHA-256 de las 5 coincide con REPORT.md)
verify-p4: OK (agregado coincide por escenario: n exacto, p95 [65.6, 17.13], error 0%)
verify-p4: OK (5 corridas crudas versionables y fieles al reporte)
```

Serie vigente (REPORT.md): por corrida caliente p95 129.63/41.57/31.16/
32.53/32.49 ms y frío p95 18.39/18.71/15.68/15.75/16.60 ms; agregado
caliente p95=65.60 ms (<200), frío p95=17.13 ms (<500), error 5xx 0.00 %;
Wilcoxon p=0.0625, Cliff's delta=-1.00.

### Archivo que respalda

- `docs/mediciones/perf/REPORT.md` (serie vigente: fecha, commit, URL,
  VUs/duración, tabla por corrida, agregado, SHA-256)
- `docs/mediciones/perf/k6-run1..5.json` versionados (~15 MB c/u)
- `docs/mediciones/perf/p95-comparacion-escenarios.svg/.pdf`

### Resultado

Cerrado 100 %: serie única vigente (65,60/17,13) en REPORT.md, Resumen,
Abstract y capítulos 05, 06, 08, 09, 11 y 12; Wilcoxon declarado no
significativo por potencia (p=0,0625, n=5); caché no presentada como
mejora demostrada (hot>cold + limitación frio-página-vacía en
amenazas); notebooks perf ejecutados con la serie vigente
(`scripts/perf-analysis.ipynb` y `docs/mediciones/perf-analysis.ipynb`,
este último con fix de encoding cp1252 en su subprocess).

Incidente 2026-09-16 (transparencia): los 5 JSON del worktree
aparecieron volteados a CRLF a las 21:38 por un proceso local no
identificado, con `core.autocrlf=true` y sin regla en `.gitattributes`.
Los blobs commiteados se verificaron intactos por hash. Fix sistémico:
`docs/mediciones/perf/k6-run*.json -text -diff` en `.gitattributes` y
guardia CRLF dentro de `verify-p4-k6.py`.

---

## P5 — nativeQuery (peso 1,1)

### Comando

```powershell
python scripts/verify-p5-nativequery.py
```

### Salida (2026-09-17, rev d92ba03a)

```text
verify-p5: OK (inventario 33=23+10 coincide)
```

Detalle: 33 `nativeQuery = true` en total (AuditLogAuditRepository:1,
BookRepository:6, FineProcedureRepository:3, LoanProcedureRepository:20,
LoanRepository:1, ReservationRepository:2); 23 invocan rutinas
(`fn_*`/`sp_pago_parcial_multa`), 10 son SELECT sin rutinas.

### Clasificación

- **22 `fn_* RETURNS TABLE`** (reportes e inventarios): sin
  equivalente JPA/`@Procedure` — JPA 2.1/`CallableStatement` solo
  expone escalar/OUT o `REF_CURSOR`, no `SETOF/TABLE` vía
  `SELECT * FROM fn_()`; reescribir a cursor rompería el contrato SQL
  público e impediría `psql` directo. Limitación pgjdbc documentada —
  ver `docs/adr/adr-006-acceso-datos-orm-sp.md` y
  `docs/capitulos/14-anexos.tex` ("Alcance de corrección").
- **`sp_pago_parcial_multa`** (`FUNCTION ... RETURNS record` con 4
  OUT, `V16__multas_pago_parcial.sql`): 0 tests ejercitan su SQL
  real (solo mocks en `FineControllerTest`), así que la equivalencia
  `@Procedure` no queda demostrada; migrar sin prueba violaría la
  regla del plan. Se mantiene con justificación técnica.

### Resultado

PARCIAL documentado, 0 % a criterio estricto (la guía exige cero
`nativeQuery=true` para rutinas): inventario verificable completo;
migración real pendiente en fase P5 (camino crítico a 8,0). Nota: una
sonda desechable no versionada citada antes en este expediente se
retiró del mismo; su conclusión técnica vive en ADR-006.

---

## P6 — Javadoc ≥ 90% (peso 0,9)

### Comando

```powershell
python scripts/verify-p6-javadoc.py
cd backend-springboot; ./mvnw -B javadoc:javadoc
```

### Salida (2026-09-17, rev d92ba03a)

```text
Javadoc audit
source=backend-springboot\src\main\java
java_files=278
public_methods=435
documented_methods=435
documented_pct=100.00
javadoc_param_tags=987
javadoc_return_tags=402
javadoc_throws_tags=142
files_with_missing_javadocs=0
verify-p6: OK (100.00% >= 90.00%)
[INFO] BUILD SUCCESS
[INFO] Total time:  15.389 s
>> Javadoc: evidencia válida (BUILD SUCCESS)
```

### Archivo que respalda

- `scripts/verify-p6-javadoc.py` → `scripts/audit-javadocs.py`

### Resultado

100 % con el conteo del auditor (métodos públicos explícitos) y
`mvn javadoc:javadoc` BUILD SUCCESS sin warnings. Conteo amplio
(métodos + constructores + interfaces/proyecciones): 99,6 % con
 Javadoc real; ~500 Javadocs añadidos/reescritos eliminando plantillas
genéricas (`mvn clean verify`: 655 tests, 0 fallos).

---

## P7 — Tipos en español ≤ 5% (peso 0,7)

### Comando

```powershell
python scripts/verify-p7-names.py
```

### Salida (2026-09-17, rev d92ba03a, completa)

```text
Types: 0/286 flagged (0.00%)
Methods: 0/650 flagged (0.00%)
Worst rubric percentage: 0.00%
verify-p7: OK (0.00% <= 5.00%)
```

### Archivo que respalda

- `scripts/verify-p7-names.py` → `scripts/audit-english-names.py`

### Resultado

Cerrado 100 %.

---

## P8 — Figuras ≥ 15 (peso 0,8)

### Comando

```powershell
python scripts/verify-p8-p9-figures.py
cd docs; xelatex -interaction=nonstopmode -halt-on-error informe-final.tex
bibtex informe-final
xelatex -interaction=nonstopmode -halt-on-error informe-final.tex
xelatex -interaction=nonstopmode -halt-on-error informe-final.tex
```

### Salida (2026-09-17, rev d92ba03a; logs en `docs/evidencia/examen/d92ba03a/xelatex-{1,2,3}.txt`, `bibtex.txt`)

```text
verify-p8-p9: OK (15 entornos figure)
verify-p8-p9: OK (15 labels unicos)
verify-p8-p9: OK (las 15 citadas; 0 rotas)
verify-p8-p9: OK (14 includegraphics existen en disco)
verify-p8-p9: OK (0 palabras espanolas en .svg versionados)
verify-p8-p9: OK (captions de figuras en ingles)
verify-p8-p9: OK (15/15 figuras referenciadas; figuras en ingles)
xelatex x1/x2/x3: exit 0; bibtex: exit 0
Output written on informe-final.pdf (109 pages).
```

Sin errores (`^!`), sin referencias indefinidas en la pasada final; solo
avisos benignos (inputenc ignorado con motor utf8, tokens hyperref en
strings PDF, `h`→`ht` en floats). El PDF recompilado no se versiona en
esta fase (fix Piso 2 pendiente con SHA): se restauró con
`git checkout -- docs/informe-final.pdf`.

Generador de las 9 figuras nuevas (`scripts/generar-figuras-evaluacion.py`,
corrida 2026-09-17): k6 caliente n=9821/frío n=10006; 79 `@PreAuthorize`;
manifiesto 9 figuras × (svg+pdf) en `docs/mediciones/figuras/`. Total:
6 previas + 9 nuevas = **15 figuras**.

### Archivo que respalda

- `scripts/generar-figuras-evaluacion.py` (fuentes solo versionadas)
- `docs/mediciones/figures-captions-en.md`
- `docs/evidencia/examen/d92ba03a/xelatex-3.txt` (109 páginas, exit 0)

### Resultado

Cerrado 100 %: 15/15 + compilación (109 páginas, exit 0) + C4-N2
re-renderizado sin recorte (PostgreSQL y Redis visibles; comandos
exactos en `workspace.dsl`) + DER rehecho con tablas/columnas/PK/FK y
cardinalidades (`--check` contra `information_schema`: 86 columnas en
30 tablas) + JaCoCo top-12 por clase. Revisión visual humana pendiente
al cierre (compilación la verifica la fase PDF).

---

## P9 — Figuras en inglés (peso 0,6)

### Resultado

Cerrado 100 %: PRISMA sin `\autoref` (Table~/Section~\ref en inglés);
verificado por `verify-p8-p9-figures.py` (0 español en .svg
versionados y en captions de figuras; tablas fuera de alcance).
Identificadores técnicos de código conservados a propósito. Revisión
visual humana pendiente al cierre.

---

## P10 — Cuenta demo con rol (peso 0,6)

### Comando

```powershell
python scripts/p10-deploy-evidence.py --out docs/evidencia/examen/p10-deploy.txt
```

### Salida (2026-09-17, rama fix/fase01-sus-n0, completa en `docs/evidencia/examen/p10-deploy.txt`)

```text
[OK] health del despliegue HTTP 200
[OK] login LECTOR HTTP 200
[OK] JWT con 3 partes
claims: rol=LECTOR roles=['LECTOR'] sub=2 correo=u@uteq.edu.ec
[OK] rol LECTOR en claims
[OK] recurso solo-ADMIN -> 403 HTTP 403
[OK] recurso ajeno -> 403 HTTP 403
verify-p10-deploy: OK (login + LECTOR + doble 403)
```

Petición literal: `POST /api/auth/login` (`{"correo":"u@uteq.edu.ec",
"password":"***"}`) → 200 con JWT cuyo payload decodificado localmente
declara `rol=LECTOR`, `roles=[LECTOR]`; `GET /api/v1/admin/usuarios` →
403; `GET /api/v1/multas/usuario/1` (ajeno al LECTOR sub=2) → 403. Sin
token, password ni cookies en la evidencia (verificado por búsqueda).

Registro complementario: `DemoAccountAuthorizationIntegrationTest`
(Testcontainers, cero mocks) existe en el árbol y corre en CI con
Docker; aquí el daemon no está disponible
(`PENDIENTE-bloqueado por entorno` en `verify-all.py`).

### Archivo que respalda

- `scripts/p10-deploy-evidence.py` (reproducible; enmascara secretos)
- `docs/evidencia/examen/p10-deploy.txt` (fecha ISO, SHA, URL, status)
- `backend-springboot/.../integration/DemoAccountAuthorizationIntegrationTest.java`
- `docs/mediciones/demo-account.md`

### Resultado

Cerrado 100 %: login LECTOR + JWT LECTOR + doble 403 contra el
despliegue, con evidencia sanitizada versionada. ADMIN retirado del
README en fase de regresiones (rotación en prod: acción humana).

---

## P11 — CRediT (peso 0,5)

### Comando

```powershell
python scripts/verify-p11-counts.py
```

### Salida (2026-09-17, rev d92ba03a, completa)

```text
verify-p11: rev citado 840ba5c1 es ancestro de HEAD (+11 commits propios declarados en prosa)
verify-p11: OK (shortlog a 840ba5c1: 763/343/326, total 1434)
verify-p11: OK (CONTRIBUCIONES.md coincide)
verify-p11: OK (CONTRIBUTORS.md coincide)
verify-p11: OK (cap. 13 coincide)
verify-p11: OK (roles-commit-counts.txt exacto)
verify-p11: OK (conteos verificables; ver seccion Firmas en CONTRIBUCIONES.md)
```

### Archivo que respalda

- `CONTRIBUCIONES.md` (sección A por punto + 14 roles; aceptaciones pendientes)
- `docs/mediciones/roles-commit-counts.txt`

### Resultado

Base verificable exacta al rev citado + artefactos. Pendiente (fase
P11/EV-4): aceptaciones genuinas de los tres integrantes; los conteos
miden commits por área, no autoría.

---

## P12 — Credencial en historial (peso 0,4)

### Comando

```powershell
python scripts/verify-p12-secrets.py
```

### Salida (2026-09-17, rev d92ba03a)

```text
verify-p12: OK (árbol limpio, 981 archivos revisados)
```

### Archivo que respalda

- `scripts/verify-p12-secrets.py`
- `docs/despliegue/NEON-ROTATION-ACTA.md` (rotación declarada con fecha y responsable)

### Resultado

Cerrado 100 % (árbol limpio; rotación declarada; la credencial antigua
solo vive en el historial).
