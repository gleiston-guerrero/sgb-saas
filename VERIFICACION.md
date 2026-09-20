# Expediente de verificación — examen suspenso SGB-SaaS (EV-1)

Base histórica: `fix/fase01-sus-n0` (rev `24885d18`, 2026-09-17).
Cierre SUS vigente: rama `fix/cierre-p3-sus-real`, 19-sep-2026. La etiqueta
`v1.1.0` se actualizará únicamente tras aprobar y fusionar este cierre.

> Nota de cierre EV-4:
> La verificación técnica integral fue ejecutada sobre el cierre técnico previo documentado.
> El commit final posterior incorpora únicamente el cierre documental de EV-4 en `CONTRIBUCIONES.md`
> (firmas/aceptaciones institucionales y trazabilidad de autoría), sin cambios funcionales.
> El tag `v1.1.0` debe apuntar al SHA final que incluye esta aceptación EV-4.

> Regla: ninguna salida está escrita a mano. Todo bloque `Salida` viene de
> ejecutar el `Comando` tal cual sobre el SHA y fecha indicados. Los puntos
> con `Estado: PENDIENTE` indican qué falta y quién lo cierra; nunca cuentan
> como éxito.
> Evidencia completa de la corrida: `docs/evidencia/examen/24885d18/`
> (`verify-all.txt` con cabecera SHA/fecha/comando/salida/código).
> Corridas anteriores (`d65b8ade/`, `31f4b30e/`, `b500878d/`, `d92ba03a/`)
> quedan como histórico.

## Pisos 1–4

### Comando

```powershell
git log --oneline --graph -5
git rev-parse v1.1.0
git rev-parse "v1.1.0^{}"
git status --short
```

### Salida (2026-09-17, rev 24885d18)

```text
* 24885d18 fix(p4): staging fijo ignorado en vez de mkdtemp inaccesible
* ae57fb53 docs(cierre): expediente sobre H1 + evidencia final
* d65b8ade docs(blindaje-final): P11 a e55f43b0, PDF FD54, P4 unificado, verificadores
* e55f43b0 docs(cierre): evidencia verify-all final, exit 0
* f590bb6b docs(cierre): PDF final 109pp + SHA real + derivados
v1.1.0 -> 080450c22e02551c6dfe1248060e243298aae43c (commit, etiqueta ligera)
M VERIFICACION.md
?? docs/evidencia/examen/24885d18/
```

### Archivo que respalda

- Historial del repositorio (`git log`)
- Sin movimientos de tags, sin merges, sin reescritura de historial

### Resultado

Piso 1 en regla a esta fecha (tag existe, anterior al cierre); el admin
mueve `v1.1.0` al SHA final de entrega. Piso 4: identidad de trabajo
`MoisesPanama <mpanamam@uteq.edu.ec>` (institucional); ver EV-4 para
autoría por punto. Para Piso 3, la evidencia vigente es una corrida SUS
anonimizada de $N=15$, recalculable y con consentimiento original privado;
la autorización adicional de publicar las filas se declara como confirmación
oral grupal, no como segunda firma escrita.

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
`evidencia válida`, `PENDIENTE` (visible, por ejemplo una dependencia
local sin Docker) o `FALLO`; sale 0 solo si no hay ningún
`FALLO`. Solo lectura: ningún verificador modifica NDJSON, figuras, PDF
ni evidencia (P4 genera su gráfico en temporal vía `SGB_PERF_GRAFICO`).
UTF-8 interno: no requiere `PYTHONUTF8=1` en Windows.

### Comando

```powershell
python scripts/verify-all.py
```

### Salida histórica (2026-09-17, rev 24885d18, en `docs/evidencia/examen/24885d18/verify-all.txt`)

```text
===== verify-all: resumen P1-P12 =====
P1: evidencia válida
P2: PENDIENTE — red pública inaccesible localmente; reintentar en CI
P3: PENDIENTE — no puntuable
P4: evidencia válida
P5: migrado (0 nativeQuery + 0 CALL nativos)
P6: evidencia válida
P7: evidencia válida
P8/P9: evidencia válida
P10: PENDIENTE (sin Docker local; el job CI lo ejecuta)
P11: conteos verificables (aceptaciones personales registradas por alcance)
P12: evidencia válida
Javadoc: evidencia válida (BUILD SUCCESS)
verify-all: exit 0 = coherencia/reproducibilidad de la evidencia disponible, NO cumplimiento academico total (P3 y firmas no puntuan)
```

Código de salida: 0.

La salida anterior se conserva como antecedente y no describe el estado
vigente de P3. El cierre SUS de 19-sep-2026 se verifica en la sección P3
de este expediente y con el mismo orquestador; no altera las declaraciones
personales EV-4 de los demás integrantes.

P10 local queda PENDIENTE-bloqueado sin Docker; la evidencia contra el
despliegue (login LECTOR + JWT + doble 403) vive en la sección P10.

### Archivo que respalda

- `Makefile` (target `verify` → `python scripts/verify-all.py`)
- `scripts/verify-all.py` + `verify-p{1,2,3,4,5,6,7,8-p9,11,12}.py`
- `.github/workflows/verify.yml` (job CI)
- `docs/evidencia/examen/24885d18/verify-all.txt` (cabecera SHA/fecha + salidas + códigos)
- `docs/evidencia/examen/b500878d/verify-all.txt` y `docs/evidencia/examen/d92ba03a/verify-all.txt` (históricos)

### Resultado

Operativo (orquestador completo, solo lectura, UTF-8, exit 0 sin FALLO;
PENDIENTEs visibles: P2 mientras `doi.org`/Zenodo estén inaccesibles
desde la máquina que verifica; P10 solo queda bloqueado en
máquinas locales sin Docker.

---

## P1 — Procedencia de datos (peso 1,4)

### Comando

```powershell
python scripts/verify-p1-hashes.py
```

### Salida reproducida el 2026-09-18 (verificador ampliado con vínculos directos)

```text
[OK] 00b2630 (commit)
[OK] 00b26306 (commit)
[OK] 1028ad02 (commit)
[OK] 437ebe5 (commit)
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
[VINCULO] 9f370270 -> docs/mediciones/perf/REPORT.md
[VINCULO] e8477021 -> docs/mediciones/jacoco/report.xml
[VINCULO] e8477021 -> docs/mediciones/jacoco/report.csv
[VINCULO] 6549becb -> docs/mediciones/perf/p95-comparacion-escenarios.svg
[VINCULO] 6549becb -> docs/mediciones/perf/p95-comparacion-escenarios.pdf
[VINCULO] 4d69f244 -> docs/mediciones/sec/zap/2026-08-17-zap-ajax-full-report.html
[VINCULO] 00b26306 -> docs/capitulos/08-resultados.tex
[VINCULO] 862672b2 -> docs/bibliografia.bib
[VINCULO] 9a467125 -> docs/capitulos/03-trabajos-relacionados.tex
[VINCULO] 6bce6257 -> docs/adr/README.md
[VINCULO] 6bce6257 -> docs/arquitectura/ISO25010.md
[VINCULO] 5f3e5e5b -> docs/capitulos/06-diseno-arquitectura.tex
[VINCULO] 7debd0b1 -> docs/trazabilidad/matriz.csv
[VINCULO] e3f3f7fa -> docs/capitulos/09-ingenieria-requisitos.tex
[VINCULO] d8443e66 -> docs/capitulos/13-declaraciones.tex
[VINCULO] df09f0db -> docs/mediciones/DATA-PROVENANCE.md
verify-p1: OK (35 hashes existen; 16 vínculos directos coinciden)
```

### Archivo que respalda

- `docs/mediciones/DATA-PROVENANCE.md`
- `scripts/verify-p1-hashes.py` (falla si un hash citado no es commit)

### Resultado

Cerrado: los 35 hashes existen. Además, 16 vínculos directos
archivo↔commit se comprueban con `git show --name-only`, incluida la
medición JaCoCo (`e8477021`), los NDJSON de rendimiento y las figuras
P4; el verificador falla si cualquiera deja de tocar la ruta declarada.

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

### Estado de red al cierre

En la comprobación local final del 2026-09-18, Windows devolvió
`WinError 10061` para los cuatro DOI tanto en `doi.org` como en Zenodo.
No se reinterpretó como un DOI válido: `verify-p2-dois.py` devuelve
estado **PENDIENTE** solamente cuando ninguna URL pudo evaluarse por red
inaccesible; si cualquier URL responde y no es válida, conserva
**FALLO**. El job CI debe ejecutar la comprobación desde su red pública
y quedar verde antes del PR a `main`.

---

## P3 — SUS (peso 1,4)

### Comando

```powershell
python scripts/verify-p3-sus.py
```

### Salida (2026-09-19, cierre SUS local, completa)

```text
verify-p3: OK (15 respuestas; códigos y manifiesto de custodia coinciden)
verify-p3: OK (Brooke recalculado: N=15, media=66.00, IC95=[56.95, 75.05])
verify-p3: OK (instrumento y cuatro derivados SUS presentes; figuras en inglés)
verify-p3: OK (documentación vigente, sin N=0)
verify-p3: evidencia válida (instrumento, CSV anónimo, manifiesto de custodia y recálculo reproducible)
```

### Resultado

Evidencia válida con alcance explícitamente limitado: $N=15$ respuestas
reales de una muestra por conveniencia, CSV anonimizado, consentimiento
original bajo custodia privada y cálculo de Brooke reproducible. La
autorización adicional de publicación de filas anonimizadas fue oral y
grupal según el responsable de difusión; no se presenta como consentimiento
escrito individual adicional. La media es 66,00 (IC95%: 56,95--75,05).
El export original de 17 registros permanece privado por contener PII; su
preservación el 20-sep-2026 a las 16:12 y su SHA-256 constan en
`docs/mediciones/sus/RAW-EXPORT-ATTESTATION.md`. La confirmación de esta
evidencia se realiza mediante contraste privado solicitado por el docente,
no mediante publicación del export identificable.

---

## P4 — k6 (peso 1,0)

### Comando

```powershell
python scripts/verify-p4-k6.py
```

### Salida (2026-09-17, rev 24885d18, completa)

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

### Salida (reproducida sobre el árbol de cierre)

```text
verify-p5: OK (inventario 0=0+0 coincide)
verify-p5: migrada ReservationRepository.searchReservationsToday -> JPQL cartesiana + ventana :start/:end desde el service (zona sistema) [P5SpikeIT.s5_ventanaHoyYProximas + ReservationServiceTest 21/21]
verify-p5: migrada ReservationRepository.searchReservationsNexts -> JPQL cartesiana + :start desde el service (zona sistema) [P5SpikeIT.s5_ventanaHoyYProximas + ReservationServiceTest 21/21]
verify-p5: migrada LoanRepository.findActivesByUserId -> JPQL cartesiana + diasRestantes en Java (zona sistema, igual que NOW()::date) [P5SpikeIT.s6_activosPorUsuarioJpqlYDiasJava + LoanServiceTest 31/31]
verify-p5: migrada BookRepository.searchByTextOIsbn -> Criteria searchText (sin categoria/autor, available tri-estado) [P5SpikeIT.s7 + BookServiceTest 21/21]
verify-p5: migrada BookRepository.searchByTextOIsbnYCategory -> Criteria searchText con join categories + countDistinct [P5SpikeIT.s7 + BookServiceTest 21/21]
verify-p5: migrada BookRepository.searchByTextOrIsbnAndAuthor -> Criteria searchText con join authors [P5SpikeIT.s7 + BookServiceTest 21/21]
verify-p5: migrada BookRepository.suggestByTitle -> Criteria function(similarity) + maxResults 10 [P5SpikeIT.s7 + BookServiceTest 21/21]
verify-p5: migrada BookRepository.searchPendientes -> Eliminada: sin llamadores en src/main ni tests [compilacion + suite (sin referencias)]
verify-p5: migrada BookRepository.searchByStatuses -> Criteria searchByStatusesCriteria (q/year opcionales) [P5SpikeIT.s7 + BookServiceTest 21/21]
verify-p5: migrada AuditLogAuditRepository.searchWithFilters -> Criteria dinamico + Sort fecha_hora->dateTime (controller y exportCsv a dateTime) [P5SpikeIT.s8 + AuditServiceTest 4/4]
verify-p5: migrada LoanProcedureRepositoryCustomImpl.spCreateLoanProcedure/spRegisterLoanReturn -> createStoredProcedureQuery posicional (proc_crear_prestamo/proc_registrar_devolucion) [P5SpikeIT.s1/s9a + LoanFineProcedureIntegrationTest (CI)]
verify-p5: migrada FineProcedureRepositoryCustomImpl.spPayFineProcedure/spVoidFineProcedure -> createStoredProcedureQuery posicional (proc_pagar_multa/proc_anular_multa) [P5SpikeIT.s9b/s9c + LoanFineProcedureIntegrationTest (CI)]
verify-p5: migrada ReservationProcedureRepositoryCustomImpl.spExpireReservationsVencidasProcedure -> createStoredProcedureQuery posicional (proc_expirar_reservaciones_vencidas) [P5SpikeIT.s9d]
verify-p5: migrada FineProcedureRepository.spPaymentParcialFine -> wrapper V54 proc_pago_parcial_multa + StoredProcedureQuery posicional (mismas 4 claves) [P5SpikeIT.s9e]
verify-p5: migrada FineProcedureRepository.fnReportSummaryFinancial -> Criteria CASE (mismo patron que summaryByCategory) + cero NUMERIC(12,2) [P5SpikeIT.s10 + FineServiceTest 9/9]
verify-p5: migrada FineProcedureRepository.fnPaymentsRecientes -> Criteria cartesiana PAGADA + setMaxResults (default 5) [P5SpikeIT.s10 + FineServiceTest 9/9]
verify-p5: migrada LoanProcedureRepository.fnListLoansActivesByUser -> JPQL cartesiana (base sin dias; LoanService solo usa size) [P5TabularSpikeIT.t1]
verify-p5: migrada LoanProcedureRepository.fnReportBooksMostLoaned -> Criteria GROUP/COUNT + maxResults (NULL = todo, como LIMIT NULL) [P5TabularSpikeIT.t2]
verify-p5: migrada LoanProcedureRepository.fnReportIndexDelinquency[+Paginated] -> Criteria filas PENDIENTE + AVG Java ROUND(,1) + ORDER/LIMIT en Java [P5TabularSpikeIT.t3]
verify-p5: migrada LoanProcedureRepository.fnReportUsageByPeriod[+Paginated] -> Criteria date_trunc + FULL OUTER en Java (TreeMap) [P5TabularSpikeIT.t4]
verify-p5: migrada LoanProcedureRepository.fnReportBooksMostLoanedDetailed[+Paginated] -> Criteria filas + string_agg en Java (TreeSet, defaults) + pct [P5TabularSpikeIT.t5]
verify-p5: migrada LoanProcedureRepository.fnReportInventory[+Paginated] -> Criteria 14 filtros + agregados en Java + estado_disponibilidad [P5TabularSpikeIT.t6]
verify-p5: migrada LoanProcedureRepository.fnReportLoansOverdues[+Paginated] -> Criteria ventana + dias/multa en Java (tarifa config, default 1) [P5TabularSpikeIT.t7]
verify-p5: migrada LoanProcedureRepository.fnReportCategoriesDemanded[+Paginated] -> Criteria GROUP/COUNT + pct en Java (limite ignorado como la funcion) [P5TabularSpikeIT.t8]
verify-p5: OK (CALL nativos en CustomImpl pineados)
verify-p5: OK (unico createNativeQuery fuera de repositorios: AuditAspect set_config, justificado)
verify-p5: OK (0 nativeQuery + 0 CALL nativos: P5 migrado)
```

(Salida íntegra en `docs/evidencia/examen/24885d18/verify-all.txt`.)

Nota `AuditAspect.java:59`: único `createNativeQuery` fuera de
repositorios, pineado por archivo:línea en el verificador. Invoca la
función built-in `SELECT set_config(...)` para el trigger de auditoría
— no es un stored procedure del dominio ni acceso a datos; parámetro
bindeado, sin concatenación. La guía exige cero `nativeQuery=true`
para invocar procedimientos almacenados; aquí no hay SP.

### Migración ejecutada (era: 33 nativas = 23 rutinas + 10 ordinarias)

- **10 ordinarias** → JPQL/Criteria (`ReservationRepository`,
  `LoanRepository`, `BookRepositoryCustom`, `AuditLogAuditRepositoryCustom`).
- **6 side-effects** → `StoredProcedureQuery` posicional (incluye
  wrapper V54 `proc_pago_parcial_multa`).
- **17 tabulares** → Criteria/JPQL con cómputo Java idéntico
  (ROUND, porcentajes, `string_agg`, FULL OUTER, `date_trunc`).
- Pruebas: equivalencia fila a fila en PG real (`P5SpikeIT` 12/12,
  `P5TabularSpikeIT` 8/8), H2 (`ReportRepositoriesH2Test` 11/11),
  suite completa 655/0/0. Matriz: `docs/basedatos/P5-MATRIZ-33.md`.

### Resultado

Cerrado 100 %: 0 apariciones de `nativeQuery = true` y 0 `CALL` nativos
en `*CustomImpl` (verificador pineado con 22 migraciones auditadas);
side-effects por `StoredProcedureQuery` posicional (V54 incluida);
tabulares en Criteria/JPQL con equivalencia fila a fila probada en PG
real; ordinarias en JPQL/Criteria. Funciones SQL intactas en BD.

---

## P6 — Javadoc ≥ 90% (peso 0,9)

### Comando

```powershell
python scripts/verify-p6-javadoc.py
cd backend-springboot; ./mvnw -B javadoc:javadoc
```

### Salida literal del auditor

```text
Javadoc audit
source=backend-springboot\src\main\java
java_files=278
public_methods=613
documented_methods=555
documented_pct=90.54
javadoc_param_tags=1192
javadoc_return_tags=536
javadoc_throws_tags=142
files_with_incomplete_javadocs=31
incomplete_details=python scripts/audit-javadocs.py --verbose
verify-p6: OK (90.54% >= 90.00%)
```

### Archivo que respalda

- `scripts/verify-p6-javadoc.py` → `scripts/audit-javadocs.py`

### Resultado

90,54 % (555/613) mediante conteo amplio de contratos públicos que
incluye interfaces y proyecciones y exige `@param`/`@return` cuando
corresponde. El detalle de los 31 archivos pendientes se obtiene con
`python scripts/audit-javadocs.py --verbose`. El build `mvn javadoc:javadoc`
debe terminar sin error en la revisión final; no se declara ausencia de
advertencias.

---

## P7 — Tipos en español ≤ 5% (peso 0,7)

### Comando

```powershell
python scripts/verify-p7-names.py
```

### Salida (reproducida sobre el árbol de cierre)

```text
Types: 8/300 flagged (2.67%)
Methods: 1/696 flagged (0.14%)
Worst rubric percentage: 2.67%
verify-p7: OK (2.67% <= 5.00%)
```

### Archivo que respalda

- `scripts/verify-p7-names.py` → `scripts/audit-english-names.py`

### Resultado

Cerrado respecto del umbral: el auditor ahora cubre clases, interfaces,
enums y records. Los ocho tipos y un método españoles permanecen
visibles y el peor porcentaje real es 2,67 %, bajo el 5 %.

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

### Salida (2026-09-19, cierre local; los logs históricos permanecen en `docs/evidencia/examen/`)

```text
verify-p8-p9: OK (17 entornos figure)
verify-p8-p9: OK (17 labels unicos)
verify-p8-p9: OK (las 17 citadas; 0 rotas)
verify-p8-p9: OK (16 includegraphics existen en disco)
verify-p8-p9: OK (0 palabras espanolas en .svg versionados)
verify-p8-p9: OK (captions de figuras en ingles)
verify-p8-p9: OK (17/17 figuras referenciadas; figuras en ingles)
xelatex x1/x2/x3: exit 0; bibtex: exit 0
Output written on informe-final.pdf (110 pages).
```

Sin errores (`^!`), sin referencias indefinidas en la pasada final; solo
avisos benignos (inputenc ignorado con motor utf8, tokens hyperref en
strings PDF, `h`→`ht` en floats). Compilación local reproducida (110
páginas, exit 0; el SHA varía entre builds por metadatos de xelatex,
esperado). El PDF versionado es el de la fase PDF con SHA en
README/`CITATION.cff`.

Generador de las 9 figuras nuevas (`scripts/generar-figuras-evaluacion.py`,
corrida 2026-09-17): k6 caliente n=9821/frío n=10006; 79 `@PreAuthorize`;
manifiesto 9 figuras × (svg+pdf) en `docs/mediciones/figuras/`. Total:
6 previas + 9 nuevas = **15 figuras**.

### Archivo que respalda

- `scripts/generar-figuras-evaluacion.py` (fuentes solo versionadas)
- `docs/mediciones/figures-captions-en.md`
- `docs/evidencia/examen/xelatex-c2-3.txt` (109 páginas, exit 0; los de `d92ba03a/` quedan como históricos)

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

### Salida (2026-09-17, rev 24885d18; re-verificado el mismo día — completa en `docs/evidencia/examen/p10-deploy.txt`)

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

### Salida (base de conteos `3489e2c5`)

```text
verify-p11: rev citado 3489e2c5 es ancestro de HEAD (conteos pineados; commits posteriores no alteran la base)
verify-p11: OK (shortlog a 3489e2c5: 764/344/370, total 1480)
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

Base verificable exacta al rev citado + artefactos. Las aceptaciones
personales de Panamá, Cajas y Loor están registradas en sus propios
commits; sus alcances no se infieren ni se amplían automáticamente.
Los conteos miden commits por área, no autoría exclusiva.

---

## P12 — Credencial en historial (peso 0,4)

### Comando

```powershell
python scripts/verify-p12-secrets.py
```

### Salida (2026-09-17, rev 24885d18)

```text
verify-p12: OK (árbol limpio, 1010 archivos revisados)
```

### Archivo que respalda

- `scripts/verify-p12-secrets.py`
- `docs/despliegue/NEON-ROTATION-ACTA.md` (rotación declarada con fecha y responsable)

### Resultado

Cerrado 100 % (árbol limpio; rotación declarada; la credencial antigua
solo vive en el historial).
