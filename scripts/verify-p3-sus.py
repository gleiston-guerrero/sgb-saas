#!/usr/bin/env python3
"""P3: coherencia documental de la ausencia de evidencia SUS (N=0).

Comprueba que el entregable declara correctamente que NO hay evidencia
SUS valida, y detecta afirmaciones que la contradigan. Este verificador
NUNCA aprueba P3: su salida es siempre PENDIENTE — no puntuable, con
N=0. Solo falla (exit 1) ante contradiccion documental.

Controles:
   0. el dataset retirado y sus derivados NO existen en el arbol
      evaluado (sus.csv, sus_boxplot.*, sus_items_breakdown.*);
   1. docs/mediciones/sus/README.md existe y declara N=0 + retirada;
  2. ningun .tex de docs/capitulos o informe cita los artefactos
     retirados (sus.csv, sus_items_breakdown, sus_boxplot) como evidencia;
  3. la seccion P3 de VERIFICACION.md declara N=0 y estado no aprobado
     (PARCIAL/pendiente/no puntuable/no aprobado) y no contiene una
     linea de CUMPLE dentro de esa seccion.

Uso: python scripts/verify-p3-sus.py
Sale 0 si la ausencia esta bien declarada (P3 sigue PENDIENTE),
1 ante cualquier contradiccion.
"""

from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
README_SUS = ROOT / "docs" / "mediciones" / "sus" / "README.md"
VERIFICACION = ROOT / "VERIFICACION.md"
SUS_DIR = ROOT / "docs" / "mediciones" / "sus"

ARTEFACTOS_RETIRADOS = ("sus.csv", "sus_items_breakdown", "sus_boxplot")


def falla(mensaje: str) -> int:
    print(f"verify-p3: FALLA: {mensaje}", file=sys.stderr)
    return 1


def main() -> int:
    # 0. Artefactos retirados ausentes del arbol evaluado (Piso 3).
    presentes = sorted(p.name for p in SUS_DIR.iterdir()
                       if p.name == "sus.csv"
                       or p.name.startswith("sus_boxplot")
                       or p.name.startswith("sus_items_breakdown"))
    if presentes:
        return falla(f"artefactos SUS retirados presentes en el arbol: {', '.join(presentes)}")
    print("verify-p3: OK (sin sus.csv ni derivados en el arbol evaluado)")

    # 1. Declaracion de retirada con N=0.
    try:
        texto = README_SUS.read_text(encoding="utf-8", errors="replace")
    except OSError:
        return falla(f"no existe {README_SUS.relative_to(ROOT)}")
    bajo = texto.lower()
    if not re.search(r"\bN\s*=\s*0\b", texto):
        return falla("sus/README.md no declara N=0")
    if "retirad" not in bajo:
        return falla("sus/README.md no declara el dataset como retirado")
    print("verify-p3: OK (sus/README.md declara N=0 y dataset retirado)")

    # 2. Artefactos retirados no citados como evidencia en el informe.
    for tex in sorted((ROOT / "docs").rglob("*.tex")):
        cuerpo = tex.read_text(encoding="utf-8", errors="replace")
        for art in ARTEFACTOS_RETIRADOS:
            if art in cuerpo:
                return falla(f"{tex.relative_to(ROOT)} cita artefacto retirado: {art}")
    print("verify-p3: OK (ningun .tex cita artefactos SUS retirados)")

    # 3. VERIFICACION.md: P3 declara N=0 y estado no aprobado.
    ver = VERIFICACION.read_text(encoding="utf-8", errors="replace")
    m = re.search(r"^## P3\b(.*?)(?=^## \S|\Z)", ver, re.DOTALL | re.MULTILINE)
    if not m:
        return falla("VERIFICACION.md sin seccion P3")
    seccion = m.group(1)
    if not re.search(r"\bN\s*=\s*0\b", seccion):
        return falla("seccion P3 sin N=0")
    bajo_sec = seccion.lower()
    if not any(k in bajo_sec for k in ("parcial", "pendiente", "no puntuable", "no aprobado")):
        return falla("seccion P3 no declara estado no aprobado")
    if re.search(r"^CUMPLE\b", seccion, re.MULTILINE):
        return falla("seccion P3 contiene linea de CUMPLE: P3 no esta aprobado")
    print("verify-p3: OK (P3 declara N=0 y estado no aprobado, sin CUMPLE)")

    print("verify-p3: PENDIENTE — no puntuable (N=0, sin respuestas reales)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
