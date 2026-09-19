#!/usr/bin/env python3
"""P1: hashes y vínculos de procedencia de DATA-PROVENANCE.md.

Extrae tokens hexadecimales de 7-40 caracteres del archivo y exige que
`git cat-file -t` devuelva `commit` para cada uno. Además verifica que
los commits usados como respaldo directo realmente tocan el artefacto
declarado. Así ningún hash inexistente ni una atribución archivo--commit
sin relación puede volver a colarse como evidencia.

Uso: python scripts/verify-p1-hashes.py
Sale 0 si todos existen, 1 si falta alguno.
"""

from __future__ import annotations

import re
import subprocess
import sys

# Salida UTF-8 en Windows sin exigir PYTHONUTF8=1: el locale cp1252
# rompe print() con tildes o U+FFFD. Solo reconfigura, no imprime.
if hasattr(__import__("sys").stdout, "reconfigure"):
    __import__("sys").stdout.reconfigure(encoding="utf-8", errors="replace")
    __import__("sys").stderr.reconfigure(encoding="utf-8", errors="replace")
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ARCHIVO = ROOT / "docs" / "mediciones" / "DATA-PROVENANCE.md"
PATRON = re.compile(r"\b[0-9a-f]{7,40}\b")

# Vínculos directos de las filas cuantitativas de DATA-PROVENANCE.md.
# Los rangos y síntesis manuales se describen como tales en el documento;
# aquí solo se automatizan las afirmaciones de que un commit respalda un
# archivo concreto.
VINCULOS_DIRECTOS = {
    "9f370270": ("docs/mediciones/perf/REPORT.md",),
    "e8477021": ("docs/mediciones/jacoco/report.xml", "docs/mediciones/jacoco/report.csv"),
    "6549becb": ("docs/mediciones/perf/p95-comparacion-escenarios.svg", "docs/mediciones/perf/p95-comparacion-escenarios.pdf"),
    "4d69f244": ("docs/mediciones/sec/zap/2026-08-17-zap-ajax-full-report.html",),
    "00b26306": ("docs/capitulos/08-resultados.tex",),
    "862672b2": ("docs/bibliografia.bib",),
    "9a467125": ("docs/capitulos/03-trabajos-relacionados.tex",),
    "6bce6257": ("docs/adr/README.md", "docs/arquitectura/ISO25010.md"),
    "5f3e5e5b": ("docs/capitulos/06-diseno-arquitectura.tex",),
    "7debd0b1": ("docs/trazabilidad/matriz.csv",),
    "e3f3f7fa": ("docs/capitulos/09-ingenieria-requisitos.tex",),
    "d8443e66": ("docs/capitulos/13-declaraciones.tex",),
    "df09f0db": ("docs/mediciones/DATA-PROVENANCE.md",),
}


def touched_files(revision: str) -> set[str]:
    proc = subprocess.run(["git", "show", "--format=", "--name-only", revision],
                          cwd=ROOT, capture_output=True, text=True,
                          encoding="utf-8", errors="replace")
    return {line.strip().replace("\\", "/") for line in proc.stdout.splitlines() if line.strip()}


def main() -> int:
    texto = ARCHIVO.read_text(encoding="utf-8", errors="replace")
    todos = sorted(set(PATRON.findall(texto)))
    # Los nombres de archivo tipo lhci-20260731-0300.json aportan tokens
    # YYYYMMDD puramente numéricos que no son hashes; se excluyen.
    hashes = [h for h in todos
              if not (len(h) == 8 and h.isdigit())]
    if not hashes:
        print("verify-p1: sin hashes que verificar", file=sys.stderr)
        return 1
    faltan = []
    for digest in hashes:
        proc = subprocess.run(
            ["git", "cat-file", "-t", digest],
            cwd=ROOT,
            capture_output=True,
            text=True,
            encoding="utf-8",
            errors="replace",
        )
        tipo = proc.stdout.strip()
        if proc.returncode != 0 or tipo != "commit":
            faltan.append(digest)
            print(f"[FALTA] {digest}")
        else:
            print(f"[OK] {digest} (commit)")
    if faltan:
        print(f"verify-p1: {len(faltan)} hash(es) inexistentes: "
              + ", ".join(faltan), file=sys.stderr)
        return 1
    desvinculados = []
    for revision, paths in VINCULOS_DIRECTOS.items():
        changed = touched_files(revision)
        for path in paths:
            if path not in changed:
                desvinculados.append(f"{revision} no toca {path}")
                print(f"[DESVINCULADO] {revision} -> {path}")
            else:
                print(f"[VINCULO] {revision} -> {path}")
    if desvinculados:
        print("verify-p1: vínculos inválidos: " + "; ".join(desvinculados),
              file=sys.stderr)
        return 1
    print(f"verify-p1: OK ({len(hashes)} hashes existen; "
          f"{sum(len(paths) for paths in VINCULOS_DIRECTOS.values())} vínculos directos coinciden)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
