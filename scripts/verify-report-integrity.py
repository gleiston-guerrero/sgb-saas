#!/usr/bin/env python3
"""Comprueba afirmaciones transversales del informe que no cubren P1--P12.

Evita que un cambio manual deje fuera de sincronía el PDF entregado, sus dos
digest publicados o las cifras de cobertura citadas en el capítulo 8.  Lee
el XML JaCoCo versionado como fuente de las coberturas globales.
"""

from __future__ import annotations

import hashlib
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PDF = ROOT / "docs" / "informe-final.pdf"
README = ROOT / "README.md"
CITATION = ROOT / "CITATION.cff"
RESULTADOS = ROOT / "docs" / "capitulos" / "08-resultados.tex"
JACOCO = ROOT / "docs" / "mediciones" / "jacoco" / "report.xml"


def falla(message: str) -> int:
    print(f"verify-report-integrity: FALLA: {message}", file=sys.stderr)
    return 1


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as source:
        for block in iter(lambda: source.read(1 << 20), b""):
            digest.update(block)
    return digest.hexdigest().upper()


def counter(root: ET.Element, kind: str) -> tuple[int, int]:
    for item in root.findall("counter"):
        if item.attrib.get("type") == kind:
            return int(item.attrib["covered"]), int(item.attrib["missed"])
    raise ValueError(f"XML sin contador global {kind}")


def main() -> int:
    if not all(path.is_file() for path in (PDF, README, CITATION, RESULTADOS, JACOCO)):
        return falla("falta un artefacto requerido de informe/cobertura")

    digest = sha256(PDF)
    readme = README.read_text(encoding="utf-8", errors="replace").upper()
    citation = CITATION.read_text(encoding="utf-8", errors="replace").upper()
    if digest not in readme:
        return falla("README.md no contiene el SHA-256 actual del PDF")
    if digest not in citation:
        return falla("CITATION.cff no contiene el SHA-256 actual del PDF")
    print(f"verify-report-integrity: OK (PDF/README/CITATION SHA-256 {digest})")

    try:
        root = ET.parse(JACOCO).getroot()
        line_covered, line_missed = counter(root, "LINE")
        branch_covered, branch_missed = counter(root, "BRANCH")
    except (ET.ParseError, OSError, ValueError) as exc:
        return falla(f"report.xml ilegible: {exc}")
    line_total = line_covered + line_missed
    branch_total = branch_covered + branch_missed
    if (line_covered, line_total, branch_covered, branch_total) != (2337, 2708, 594, 797):
        return falla("cobertura XML distinta de la medición declarada "
                     f"({line_covered}/{line_total}; {branch_covered}/{branch_total})")

    resultados = RESULTADOS.read_text(encoding="utf-8", errors="replace")
    required = ("2337/2708", "594/797", "86,30\\,\\%", "74,53\\,\\%")
    missing = [value for value in required if value not in resultados]
    if missing:
        return falla("08-resultados.tex no coincide con report.xml: " + ", ".join(missing))
    print("verify-report-integrity: OK (JaCoCo XML y capítulo 8: "
          "2337/2708 = 86,30%; 594/797 = 74,53%)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
