#!/usr/bin/env python3
"""Verifica los 4 hashes del punto 6 de la rubrica mas la auditoria completa.

Uso: python scripts/check-hashes.py
Sale 0 si las 4 partes verifican, 1 en caso contrario.
"""

from __future__ import annotations

import re
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

# Las 4 partes exigidas: (etiqueta, hash_vigente, archivo_que_lo_cita).
PARTS = [
    ("OBS-18", "e946d48", "docs/observaciones/OBSERVACIONES.md"),
    ("OBS-13", "105807c", "docs/observaciones/OBSERVACIONES.md"),
]

# Hashes rotos que ya no deben ser citados por ninguna version vigente.
RETIRED = ["63ca63d", "5d404b6", "c6de386"]

AUDITED_FILES = [
    "docs/observaciones/OBSERVACIONES.md",
    "docs/mediciones/DATA-PROVENANCE.md",
]


def git(*args: str) -> tuple[int, str]:
    proc = subprocess.run(["git", *args], cwd=ROOT, capture_output=True, text=True)
    return proc.returncode, proc.stdout.strip()


def main() -> int:
    failures: list[str] = []
    for label, digest, cited_in in PARTS:
        code, kind = git("cat-file", "-t", digest)
        cited = digest in (ROOT / cited_in).read_text(encoding="utf-8", errors="ignore")
        code_refs, refs = git("branch", "--all", "--contains", digest)
        status = "ok" if (code == 0 and cited) else "FAIL"
        print(f"[{status}] ({label}) {digest} objeto={kind or 'ausente'} citado={cited} refs={len(refs.split())}")
        if status != "ok":
            failures.append(label)

    for retired in RETIRED:
        hits = []
        for rel in AUDITED_FILES:
            text = (ROOT / rel).read_text(encoding="utf-8", errors="ignore")
            if retired in text:
                hits.append(rel)
        status = "ok" if not hits else "FAIL"
        print(f"[{status}] (retirado) {retired} citado_en={hits or 'ninguno'}")
        if hits:
            failures.append(retired)

    total = found = 0
    for rel in AUDITED_FILES:
        text = (ROOT / rel).read_text(encoding="utf-8", errors="ignore")
        for digest in sorted(set(re.findall(r"\b[0-9a-f]{7,40}\b", text))):
            if re.fullmatch(r"20\d{6}", digest):
                continue  # fecha en nombre de archivo, no hash
            total += 1
            code, _ = git("cat-file", "-t", digest)
            if code == 0:
                found += 1
            else:
                print(f"[FAIL] objeto ausente: {digest} ({rel})")
                failures.append(digest)
    print(f"auditoria completa: {found}/{total} objetos existen")

    if failures:
        print("FALLOS:", sorted(set(failures)))
        return 1
    print("punto 6: 4/4 partes verifican")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
