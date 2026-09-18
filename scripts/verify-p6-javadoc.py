#!/usr/bin/env python3
"""P6: Javadoc >= 90% de métodos públicos (guía examen suspenso).

Reejecuta scripts/audit-javadocs.py y exige documented_pct >= 90.
La comprobación `mvn javadoc:javadoc sin error` va aparte en
VERIFICACION.md (tarda minutos; este script es el gate rápido).

Uso: python scripts/verify-p6-javadoc.py
Sale 0 si cumple, 1 si no.
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
UMBRAL = 90.0


def main() -> int:
    proc = subprocess.run(
        [sys.executable, "scripts/audit-javadocs.py"],
        cwd=ROOT,
        capture_output=True,
        text=True,
    )
    print(proc.stdout, end="")
    if proc.returncode != 0:
        print("verify-p6: audit-javadocs.py falló", file=sys.stderr)
        return 1
    match = re.search(r"documented_pct=([0-9.]+)", proc.stdout)
    if not match:
        print("verify-p6: no se encontró documented_pct en la salida", file=sys.stderr)
        return 1
    pct = float(match.group(1))
    if pct < UMBRAL:
        print(f"verify-p6: {pct:.2f}% < umbral {UMBRAL:.2f}%", file=sys.stderr)
        return 1
    print(f"verify-p6: OK ({pct:.2f}% >= {UMBRAL:.2f}%)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
