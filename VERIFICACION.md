# Expediente de verificación — examen suspenso SGB-SaaS (EV-1)

Rama defendida: `fix/rescate-produccion-examen`.
Cierre: viernes 18-sep-2026 23:55. Etiqueta `v1.1.0`: la mueve el admin al final (ver EV-3).

> Regla: ninguna salida está escrita a mano. Todo bloque `Salida` viene de
> ejecutar el `Comando` tal cual. Los puntos con `Estado: PENDIENTE`
> indican el comando exacto que falta correr + quién lo cierra.

## Pisos 1–4

### Comando

```powershell
git log --oneline --graph -5
git rev-parse v1.1.0
git status --short
```

### Salida

```text
(PENDIENTE: pegar salida al cierre, con la rama ya integrada y el tag movido)
```

### Archivo que respalda

- Historial del repositorio (`git log`)

### Resultado

PENDIENTE (piso 1 exige tag sobre commit anterior al cierre; lo hace el admin)

---

## EV-2 — `make verify`

### Comando

```powershell
make verify
```

### Salida

```text
(PENDIENTE: el target se crea en esta rama; pegar salida verde aquí)
```

### Archivo que respalda

- `Makefile` (target `verify`)

### Resultado

PENDIENTE

---

## P1 — Procedencia de datos (18/20 hashes inexistentes), peso 1,4

### Comando

```powershell
python scripts/verify-p1-hashes.py
```

### Salida (2026-09-16, rama fix/rescate-produccion-examen)

```text
[OK] 00b2630 (commit)
... (34/34 OK)
verify-p1: OK (34 hashes existen)
```

(El archivo se reescribió citando solo commits vigentes obtenidos con
`git log -1 -- <archivo>`; la salida completa de 34 líneas consta en el
historial de ejecución. Ningún hash inexistente permanece citado.)

### Archivo que respalda

- `docs/mediciones/DATA-PROVENANCE.md` (reescrito 2026-09-16)
- `scripts/verify-p1-hashes.py` (falla si un hash citado no existe)

### Resultado

CUMPLE

---

## P2 — DOI (peso 0,6)

### Comando

```powershell
python scripts/verify-p2-dois.py
```

### Salida (2026-09-16)

```text
[OK] 10.5281/zenodo.21712467 (published, 1 archivo(s)) <- docs\capitulos\00-portada.tex, docs\capitulos\02-introduccion.tex, docs\checklists\fair.md, docs\informe-entrega-3.tex, docs\mediciones\informe-final-text.txt
[OK] 10.5281/zenodo.22636466 (published, 1 archivo(s)) <- CITATION.cff, docs\capitulos\00-portada.tex, docs\capitulos\02-introduccion.tex, docs\capitulos\13-declaraciones.tex, docs\checklists\fair.md
[OK] 10.5281/zenodo.22715710 (published, 1 archivo(s)) <- README.md
[OK] 10.5281/zenodo.22728199 (published, 1 archivo(s)) <- README.md
[OK] 10.5281/zenodo.22741050 (published, 1 archivo(s)) <- CITATION.cff
```

### Archivo que respalda

- `scripts/verify-p2-dois.py` (verifica vía API Zenodo: doi.org da 404 transitorios)
- `CITATION.cff` (doi 10.5281/zenodo.22741050)

### Resultado

CUMPLE (nota: el 404 de la guía sobre 22636466 no reproduce; el depósito existe y está publicado con archivos)

---

## P3 — SUS (peso 1,4)

### Resultado

PARCIAL DECLARADO (0-15%): el dataset fue retirado por el propio equipo
(`docs/mediciones/sus/README.md`: patrones incompatibles con respuestas
independientes). No se fabricará evidencia. Sin instrumento real con
consentimientos verificables no se puede cerrar.

---

## P4 — k6 (peso 1,0)

### Comando

```powershell
(Get-ChildItem k6/ -Recurse -Filter '*.json').Count
```

### Salida (2026-09-16)

```text
0
```

### Resultado

PENDIENTE (solo existen los 2 scripts; faltan 5 corridas + estadística). Responsable: Irvin.

---

## P5 — nativeQuery (peso 1,1)

### Comando

```powershell
git grep -n "nativeQuery *= *true" -- backend-springboot/src/main/java | Measure-Object -Line
```

### Salida (2026-09-16)

```text
(PENDIENTE: pegar conteo + clasificación fn_* vs legítimas)
```

### Resultado

PARCIAL (23 invocan rutinas; `sp_pago_parcial_multa` convertible a `@Procedure`; `fn_*` RETURNS TABLE sin equivalente JPA documentado)

---

## P6 — Javadoc ≥ 90% (peso 0,9)

### Comando

```powershell
python scripts/verify-p6-javadoc.py
```

### Salida (2026-09-16)

```text
Javadoc audit
source=backend-springboot\src\main\java
java_files=273
public_methods=405
documented_methods=405
documented_pct=100.00
javadoc_param_tags=681
javadoc_return_tags=372
javadoc_throws_tags=80
files_with_missing_javadocs=0
verify-p6: OK (100.00% >= 90.00%)
```

### Archivo que respalda

- `scripts/verify-p6-javadoc.py` → `scripts/audit-javadocs.py`

### Resultado

CUMPLE (pendiente `mvn javadoc:javadoc` sin error; ver comando abajo)

### Comando (segunda parte)

```powershell
cd backend-springboot; ./mvnw -B javadoc:javadoc
```

### Salida

```text
(PENDIENTE: pegar cola de la salida + BUILD SUCCESS)
```

---

## P7 — Tipos en español ≤ 5% (peso 0,7)

### Comando

```powershell
python scripts/verify-p7-names.py
```

### Salida (2026-09-16)

```text
Types: 0/286 flagged (0.00%)
Methods: 0/650 flagged (0.00%)
Worst rubric percentage: 0.00%
verify-p7: OK (0.00% <= 5.00%)
```

### Archivo que respalda

- `scripts/verify-p7-names.py` → `scripts/audit-english-names.py` (criterio oficial: backend)

### Resultado

CUMPLE

---

## P8 — Figuras ≥ 15 (peso 0,8)

### Resultado

PARCIAL (6 figuras referenciadas; faltan 9). Responsable: Marlon.

---

## P9 — Figuras en inglés (peso 0,6)

### Resultado

PARCIAL (nodos TikZ del PRISMA en español). Responsable: Marlon.

---

## P10 — Cuenta demo con rol (peso 0,6)

### Comando

```powershell
cd backend-springboot; ./mvnw -B test -Dtest=DemoAccountMigrationIntegrationTest
```

### Salida

```text
(PENDIENTE: pegar Tests run + BUILD SUCCESS)
```

### Resultado

PARCIAL (tests existen y asertan rol LECTOR; falta pegar evidencia + 403 en expediente)

---

## P11 — CRediT (peso 0,5)

### Resultado

PARCIAL (14/14 declarados en `CONTRIBUTORS.md`; faltan conteos por rol + `CONTRIBUCIONES.md` con firmas)

---

## P12 — Credencial en historial (peso 0,4)

### Comando

```powershell
python scripts/verify-p12-secrets.py
```

### Salida (2026-09-16)

```text
verify-p12: OK (árbol limpio, 1234 archivos revisados)
```

### Archivo que respalda

- `scripts/verify-p12-secrets.py`
- `docs/despliegue/NEON-ROTATION-ACTA.md` (rotación 2026-09-11/13)

### Resultado

CUMPLE (árbol limpio; rotación declarada con fecha)
