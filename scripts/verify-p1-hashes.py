#!/usr/bin/env python3
"""P1: cada hash citado en DATA-PROVENANCE.md existe (guía examen suspenso).

Extrae tokens hexadecimales de 7-40 caracteres del archivo y exige que
`git cat-file -t` devuelva `commit` para cada uno. Así ningún hash
inexistente puede volver a colarse como evidencia.

Uso: python scripts/verify-p1-hashes.py
Sale 0 si todos existen, 1 si falta alguno.
"""

from __future__ import annotations

import re
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ARCHIVO = ROOT / "docs" / "mediciones" / "DATA-PROVENANCE.md"
PATRON = re.compile(r"\b[0-9a-f]{7,40}\b")


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
    print(f"verify-p1: OK ({len(hashes)} hashes existen)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
