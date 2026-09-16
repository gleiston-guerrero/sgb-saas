#!/usr/bin/env python3
"""P7: tipos con raíz española <= 5% (guía examen suspenso).

Reejecuta scripts/audit-english-names.py sobre el backend (criterio
oficial de la guía: 35/283 tipos del backend) y exige worst <= 5%.
El frontend no entra en el conteo oficial (usa contratos JSON en
español a propósito, no identificadores Java).

Uso: python scripts/verify-p7-names.py
Sale 0 si cumple, 1 si no.
"""

from __future__ import annotations

import re
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
UMBRAL = 5.0


def main() -> int:
    proc = subprocess.run(
        [sys.executable, "scripts/audit-english-names.py"],
        cwd=ROOT,
        capture_output=True,
        text=True,
    )
    print(proc.stdout, end="")
    if proc.returncode != 0:
        print("verify-p7: audit-english-names.py falló", file=sys.stderr)
        return 1
    match = re.search(r"Worst rubric percentage:\s*([0-9.]+)%", proc.stdout)
    if not match:
        print("verify-p7: no se encontró el porcentaje en la salida", file=sys.stderr)
        return 1
    pct = float(match.group(1))
    if pct > UMBRAL:
        print(f"verify-p7: {pct:.2f}% > umbral {UMBRAL:.2f}%", file=sys.stderr)
        return 1
    print(f"verify-p7: OK ({pct:.2f}% <= {UMBRAL:.2f}%)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
