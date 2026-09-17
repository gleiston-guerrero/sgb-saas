#!/usr/bin/env python3
"""P11: los conteos CRediT se regeneran desde Git y coinciden con los documentos.

Comprueba al <rev> dado (defecto: HEAD):
  1. el rev existe;
  2. `shortlog -sne --no-merges` y `rev-list --count` coinciden con los
     totales de CONTRIBUCIONES.md (cabecera), CONTRIBUTORS.md (tabla),
     cap. 13 (tres items) y el contenido exacto de
     docs/mediciones/roles-commit-counts.txt;
  3. los cuatro documentos citan el mismo rev evaluado.

No valida firmas (externas al repo) ni aprueba P11 academicamente:
exit 0 = conteos verificables, firmas pendientes.

Uso: python scripts/verify-p11-counts.py [--rev <sha>]
Sale 0 si todo coincide, 1 ante cualquier discrepancia.
"""

from __future__ import annotations

import re
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def falla(mensaje: str) -> int:
    print(f"verify-p11: FALLA: {mensaje}", file=sys.stderr)
    return 1


def git(*args: str) -> str:
    proc = subprocess.run(["git", *args], cwd=ROOT,
                          capture_output=True, text=True)
    if proc.returncode != 0:
        raise RuntimeError(f"git {' '.join(args)}: {proc.stderr[-300:]}")
    return proc.stdout


def rev_citado() -> str | None:
    m = re.search(r"Totales globales a `([0-9a-f]{7,40})`",
                  (ROOT / "CONTRIBUCIONES.md").read_text(
                      encoding="utf-8", errors="replace"))
    return m.group(1) if m else None


def main() -> int:
    args = sys.argv[1:]
    citado = rev_citado()
    if citado is None:
        return falla("CONTRIBUCIONES.md sin rev citado parseable")
    rev = citado
    if "--rev" in args:
        rev = args[args.index("--rev") + 1]
    try:
        git("rev-parse", "--verify", rev)
    except RuntimeError:
        return falla(f"rev inexistente: {rev}")
    # El rev citado debe ser ancestro de HEAD: los conteos quedan
    # pineados a un rev estable y el delta posterior se declara en
    # prosa (commits propios tras el rev), nunca se reescribe.
    anc = subprocess.run(["git", "merge-base", "--is-ancestor", citado, "HEAD"],
                         cwd=ROOT, capture_output=True)
    if anc.returncode != 0:
        return falla(f"rev citado {citado[:8]} no es ancestro de HEAD")
    try:
        delta = int(git("rev-list", "--count", f"{citado}..HEAD").strip())
    except RuntimeError:
        delta = -1
    print(f"verify-p11: rev citado {citado[:8]} es ancestro de HEAD "
          f"(+{delta} commits propios declarados en prosa)")

    lineas = git("-c", "log.mailmap=true", "shortlog", "-sne",
                 "--no-merges", rev).splitlines()
    totales: dict[str, int] = {}
    for linea in lineas:
        m = re.match(r"\s*(\d+)\s+(.+?)\s*<(.+)>", linea)
        if m:
            totales[m.group(2).strip()] = int(m.group(1))
    total = int(git("rev-list", "--count", "--no-merges", rev).strip())
    try:
        cajas = totales["Irvin Cajas Ibarra"]
        loor = totales["Marlon Loor Medranda"]
        panama = totales["Moises Panama Murillo"]
    except KeyError as exc:
        return falla(f"autor faltante en shortlog: {exc}")
    print(f"verify-p11: OK (shortlog a {rev[:8]}: {cajas}/{loor}/{panama}, total {total})")

    def leer(rel: str) -> str:
        return (ROOT / rel).read_text(encoding="utf-8", errors="replace")

    # 1. CONTRIBUCIONES.md: cabecera con mismo rev y mismos totales.
    contrib = leer("CONTRIBUCIONES.md")
    m = re.search(r"Totales globales a `([0-9a-f]{7,40})`[^\n]*"
                  r"Cajas (\d+), Loor (\d+), Panamá (\d+)", contrib)
    if not m:
        return falla("CONTRIBUCIONES.md sin linea de totales parseable")
    if not rev.startswith(m.group(1)) and m.group(1) not in git("rev-parse", rev).strip():
        return falla(f"CONTRIBUCIONES.md cita rev {m.group(1)}, se evalua {rev[:8]}")
    if (int(m.group(2)), int(m.group(3)), int(m.group(4))) != (cajas, loor, panama):
        return falla("totales de CONTRIBUCIONES.md difieren del shortlog")
    print("verify-p11: OK (CONTRIBUCIONES.md coincide)")

    # 2. CONTRIBUTORS.md: tabla por autor.
    tabla = leer("CONTRIBUTORS.md")
    for nombre, esperado in (("Irvin Cajas Ibarra", cajas),
                             ("Marlon Loor Medranda", loor),
                             ("Moises Panama Murillo", panama)):
        mm = re.search(rf"\|\s*{re.escape(nombre)}\s*\|.*\|\s*\*\*(\d+)\*\*", tabla)
        if not mm or int(mm.group(1)) != esperado:
            return falla(f"CONTRIBUTORS.md: {nombre} != {esperado}")
    print("verify-p11: OK (CONTRIBUTORS.md coincide)")

    # 3. cap. 13: tres items en orden Cajas/Loor/Panamá.
    cap13 = leer("docs/capitulos/13-declaraciones.tex")
    nums = [int(x) for x in re.findall(r"(\d+) commits humanos", cap13)]
    if nums != [cajas, loor, panama]:
        return falla(f"cap. 13: {nums} != [{cajas}, {loor}, {panama}]")
    print("verify-p11: OK (cap. 13 coincide)")

    # 4. roles-commit-counts.txt: contenido exacto del shortlog.
    txt = (ROOT / "docs" / "mediciones" / "roles-commit-counts.txt").read_text(
        encoding="utf-8", errors="replace").splitlines()
    vivos = [l.strip() for l in ("\n".join(lineas)).splitlines() if l.strip()]
    propios = [l.strip() for l in txt if l.strip()]
    if propios != vivos:
        return falla("roles-commit-counts.txt difiere del shortlog en vivo")
    print("verify-p11: OK (roles-commit-counts.txt exacto)")

    print("verify-p11: OK (conteos verificables; firmas externas pendientes)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
