#!/usr/bin/env python3
"""P4: las 5 corridas k6 crudas existen, son legibles y coinciden con REPORT.md.

Verifica, sin re-correr k6:
  1. existen docs/mediciones/perf/k6-run1.json ... k6-run5.json;
  2. cada archivo es NDJSON k6 legible con puntos http_req_duration en
     ambos escenarios (cache_caliente, cache_frio);
  3. el SHA-256 de cada archivo coincide con la tabla de la seccion
     "Serie vigente" de docs/mediciones/perf/REPORT.md;
  4. scripts/perf-analysis.py corre sobre los 5 y su agregado coincide
     (p95 redondeado a 2 decimales, tasa de error) con el reporte.

Uso: python scripts/verify-p4-k6.py
Sale 0 si todo coincide, 1 con el primer incumplimiento (mensaje claro).

Efecto colateral conocido: perf-analysis.py reescribe
docs/mediciones/perf/p95-comparacion-escenarios.svg/.pdf con datos
identicos (solo cambian fecha de generacion e IDs aleatorios del SVG).
Si el arbol debe quedar intacto, restaurar con:
  git checkout -- docs/mediciones/perf/p95-comparacion-escenarios.svg \
    docs/mediciones/perf/p95-comparacion-escenarios.pdf
"""

from __future__ import annotations

import hashlib
import json
import re
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PERF = ROOT / "docs" / "mediciones" / "perf"
REPORT = PERF / "REPORT.md"
ANALISIS = ROOT / "scripts" / "perf-analysis.py"

CORRIDAS = [PERF / f"k6-run{n}.json" for n in range(1, 6)]

# Agregado vigente (serie 2026-09-17, REPORT.md). Tolerancia abajo.
ESPERADO = {
    "cache_caliente": {"p95_ms": 65.60, "n": 9821, "tasa_error_5xx": 0.0},
    "cache_frio": {"p95_ms": 17.13, "n": 10006, "tasa_error_5xx": 0.0},
}
TOL_P95 = 0.05


def falla(mensaje: str) -> int:
    print(f"verify-p4: FALLA: {mensaje}", file=sys.stderr)
    return 1


def sha256(path: Path) -> str:
    h = hashlib.sha256()
    with open(path, "rb") as fh:
        for bloque in iter(lambda: fh.read(1 << 20), b""):
            h.update(bloque)
    return h.hexdigest().upper()


def main() -> int:
    # 1. Existencia.
    for path in CORRIDAS:
        if not path.is_file():
            return falla(f"falta {path.relative_to(ROOT)}")
    print(f"verify-p4: OK (existen {len(CORRIDAS)} corridas)")

    # Guardia CRLF (incidente 2026-09-16): con core.autocrlf=true y sin
    # regla -text, un checkout voltea los NDJSON a CRLF y el SHA deja de
    # coincidir sin que cambien los datos. Falla rapido con accion clara.
    for path in CORRIDAS:
        with open(path, "rb") as fh:
            if b"\r" in fh.read(1 << 20):
                return falla(f"{path.name} con CRLF: revisar .gitattributes (-text) o core.autocrlf")
    print("verify-p4: OK (5 archivos en LF, sin CRLF)")

    # 2. NDJSON k6 legible con ambos escenarios.
    for path in CORRIDAS:
        vistos = set()
        lineas = 0
        try:
            with open(path, encoding="utf-8") as fh:
                for linea in fh:
                    linea = linea.strip()
                    if not linea:
                        continue
                    lineas += 1
                    obj = json.loads(linea)
                    if obj.get("type") == "Point" and obj.get("metric") == "http_req_duration":
                        esc = ((obj.get("data") or {}).get("tags") or {}).get("scenario")
                        if esc in ("cache_caliente", "cache_frio"):
                            vistos.add(esc)
                    if lineas > 200000:
                        break
        except (OSError, json.JSONDecodeError) as exc:
            return falla(f"{path.name} no es NDJSON legible: {exc}")
        if vistos != {"cache_caliente", "cache_frio"}:
            return falla(f"{path.name} sin ambos escenarios (vistos: {sorted(vistos)})")
    print("verify-p4: OK (NDJSON legible, ambos escenarios en las 5)")

    # 3. SHA-256 contra la tabla de la serie vigente en REPORT.md.
    texto = REPORT.read_text(encoding="utf-8", errors="replace")
    # Filas como: | 1 | `72C7...` |
    esperados = dict(re.findall(r"\|\s*([1-5])\s*\|\s*`([0-9A-Fa-f]{64})`", texto))
    if len(esperados) != 5:
        return falla(f"REPORT.md no trae tabla SHA de 5 corridas (halladas: {len(esperados)})")
    for n, path in enumerate(CORRIDAS, 1):
        real = sha256(path)
        if real != esperados[str(n)].upper():
            return falla(f"{path.name}: SHA {real[:16]}... != REPORT.md {esperados[str(n)][:16]}...")
    print("verify-p4: OK (SHA-256 de las 5 coincide con REPORT.md)")

    # 4. perf-analysis.py corre y el agregado coincide por escenario
    # (nombre, n_peticiones exacto, p95 a 2 decimales, tasa de error).
    proc = subprocess.run(
        [sys.executable, str(ANALISIS), *(str(p) for p in CORRIDAS)],
        cwd=ROOT, capture_output=True, text=True, timeout=600)
    if proc.returncode != 0:
        return falla(f"perf-analysis.py exit={proc.returncode}: {proc.stderr[-500:]}")
    bloques = re.findall(
        r'"escenario":\s*"(cache_caliente|cache_frio)"(.*?)(?="escenario"\s*:|$)',
        proc.stdout, re.DOTALL)
    vistos = {}
    for esc, cuerpo in bloques:
        p95 = re.search(r'"p95_ms":\s*([0-9.]+)', cuerpo)
        n_pet = re.search(r'"n_peticiones":\s*([0-9]+)', cuerpo)
        tasa = re.search(r'"tasa_error_5xx":\s*([0-9.]+)', cuerpo)
        if not (p95 and n_pet and tasa):
            return falla(f"{esc}: bloque sin p95/n_peticiones/tasa_error_5xx")
        vistos[esc] = (float(p95.group(1)), int(n_pet.group(1)), float(tasa.group(1)))
    if set(vistos) != set(ESPERADO):
        return falla(f"escenarios incompletos: {sorted(vistos)} != {sorted(ESPERADO)}")
    for esc, esp in ESPERADO.items():
        p95, n_pet, tasa = vistos[esc]
        if n_pet != esp["n"]:
            return falla(f"{esc}: n={n_pet} != REPORT.md {esp['n']}")
        if abs(p95 - esp["p95_ms"]) > TOL_P95:
            return falla(f"{esc}: p95={p95:.2f} != REPORT.md {esp['p95_ms']}")
        if tasa != esp["tasa_error_5xx"]:
            return falla(f"{esc}: tasa_error_5xx={tasa} != 0.0")
    print("verify-p4: OK (agregado coincide por escenario: n exacto,"
          f" p95 {[round(vistos[e][0], 2) for e in ESPERADO]}, error 0%)")

    print("verify-p4: OK (5 corridas crudas versionables y fieles al reporte)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
