#!/usr/bin/env python3
"""P8/P9: 15 figuras referenciadas y texto dentro de figuras en ingles.

P8: cuenta \\begin{figure}, \\includegraphics, \\label{fig:...} en
  docs/capitulos/*.tex; exige 15 figuras con label unico, cada label
  citado al menos una vez (\\autoref) y cada grafico existente en disco.
P9: busca texto espanol SOLO dentro de figuras (archivos .svg
  versionados + captions de entornos figure). Tablas y sus captions
  quedan fuera de alcance. docs/mediciones/sus/** se excluye a
  proposito: dataset retirado, no evidencia.

Uso: python scripts/verify-p8-p9-figures.py
Sale 0 si todo cumple, 1 con el primer incumplimiento.
"""

from __future__ import annotations

import re
import sys

# Salida UTF-8 en Windows sin exigir PYTHONUTF8=1: el locale cp1252
# rompe print() con tildes o U+FFFD. Solo reconfigura, no imprime.
if hasattr(__import__("sys").stdout, "reconfigure"):
    __import__("sys").stdout.reconfigure(encoding="utf-8", errors="replace")
    __import__("sys").stderr.reconfigure(encoding="utf-8", errors="replace")
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CAPS = sorted((ROOT / "docs" / "capitulos").glob("*.tex"))

N_FIGURAS = 15

# Palabras que delatan texto espanol dentro de una figura (svg o caption
# de entorno figure). Lista acotada a vocabulario del dominio; los
# textos en ingles del informe no las contienen.
ES_SVG = [
    "préstamo", "prestamo", "usuario", "categoría", "categoria",
    "riesgo", "autor", "libro", "multa", "deuda", "morosidad",
    "rendimiento", "seguridad", "cobertura", "gráfico", "grafico",
    "compromisos", "meses",
]
ES_CAPTION = [
    " los ", " las ", " una ", " para ", " con ", " por ", " del ",
    " este ", " esta ", " como ", " entre ", " sobre ", " muestra ",
    " figura ",
]


def falla(mensaje: str) -> int:
    print(f"verify-p8-p9: FALLA: {mensaje}", file=sys.stderr)
    return 1


def main() -> int:
    cuerpos = {p: p.read_text(encoding="utf-8", errors="replace") for p in CAPS}

    n_fig = sum(len(re.findall(r"\\begin\{figure\}", t)) for t in cuerpos.values())
    if n_fig != N_FIGURAS:
        return falla(f"figuras: {n_fig} != {N_FIGURAS} esperadas")
    print(f"verify-p8-p9: OK ({n_fig} entornos figure)")

    labels: dict[str, int] = {}
    for p, t in cuerpos.items():
        for m in re.finditer(r"\\label\{(fig:[^}]+)\}", t):
            labels[m.group(1)] = labels.get(m.group(1), 0) + 1
    dup = sorted(l for l, c in labels.items() if c > 1)
    if dup:
        return falla(f"labels duplicados: {dup}")
    if len(labels) != N_FIGURAS:
        return falla(f"labels fig: {len(labels)} != {N_FIGURAS}")
    print(f"verify-p8-p9: OK ({len(labels)} labels unicos)")

    refs: set[str] = set()
    for t in cuerpos.values():
        refs.update(re.findall(r"\\autoref\{(fig:[^}]+)\}", t))
    huerfanas = sorted(set(labels) - refs)
    if huerfanas:
        return falla(f"figuras sin citar: {huerfanas}")
    rotas = sorted(refs - set(labels))
    if rotas:
        return falla(f"autoref a inexistente: {rotas}")
    print(f"verify-p8-p9: OK (las {len(labels)} citadas; 0 rotas)")

    incs = []
    for t in cuerpos.values():
        incs += re.findall(r"\\includegraphics\[[^\]]*\]\{([^}]+)\}", t)
    faltan = [g for g in incs if not (ROOT / "docs" / (g if g.endswith((".pdf", ".png")) else g + ".pdf")).exists()
              and not (ROOT / "docs" / g).exists()]
    if faltan:
        return falla(f"graficos inexistentes: {faltan}")
    print(f"verify-p8-p9: OK ({len(incs)} includegraphics existen en disco)")

    # P9: texto de los .svg versionados (fuera sus/ retirado).
    svg_dirs = [ROOT / "docs" / "mediciones" / "figuras",
                ROOT / "docs" / "mediciones" / "perf",
                ROOT / "docs" / "arquitectura"]
    patron = re.compile(r"|".join(rf"\b{w}\b" for w in ES_SVG), re.IGNORECASE)
    for d in svg_dirs:
        if not d.is_dir():
            continue
        for svg in sorted(d.glob("*.svg")):
            textos = " ".join(re.findall(r"<text[^>]*>(.*?)</text>", svg.read_text(
                encoding="utf-8", errors="replace"), re.DOTALL))
            limpio = re.sub(r"<[^>]+>", "", textos)
            halladas = sorted(set(patron.findall(limpio)))
            if halladas:
                return falla(f"P9: espanol en {svg.relative_to(ROOT)}: {halladas}")
    print("verify-p8-p9: OK (0 palabras espanolas en .svg versionados)")

    # P9: captions de entornos figure (no de tablas).
    for p, t in cuerpos.items():
        for m in re.finditer(r"\\begin\{figure\}.*?\\caption\{(.*?)\}.*?\\end\{figure\}",
                             t, re.DOTALL):
            cap = " " + re.sub(r"\\[a-zA-Z]+\{?", " ", m.group(1)) + " "
            cap = re.sub(r"[{}]", " ", cap).lower()
            for w in ES_CAPTION:
                if w in cap:
                    return falla(f"P9: caption en espanol ({p.name}): ...{w.strip()}...")
    print("verify-p8-p9: OK (captions de figuras en ingles)")

    print("verify-p8-p9: OK (15/15 figuras referenciadas; figuras en ingles)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
