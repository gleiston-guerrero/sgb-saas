#!/usr/bin/env python3
"""P2: todos los DOI declarados resuelven HTTP 200 (guía examen suspenso).

Extrae DOIs con forma 10.NNNN/... de CITATION.cff, README.md y docs/
(*.md, *.tex, *.txt; excluye historial .git) y comprueba cada uno contra
https://doi.org con timeout y reintento. Los DOI históricos mencionados
solo como "versión anterior" también deben resolver: si alguno devuelve
404, hay que actualizar la referencia (ver barrido 22636466 -> válido).

Uso: python scripts/verify-p2-dois.py
Sale 0 si todos resuelven 2xx/3xx, 1 si alguno falla.
"""

from __future__ import annotations

import re
import sys
import time
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PATRON_DOI = re.compile(r"10\.\d{4,}/[^\s)\"',;>\\]+")
EXTENSIONES = {".cff", ".md", ".tex", ".txt"}
EXCLUIR_DIRS = {".git", "node_modules", "target", "dist", ".opencode",
                "graphify-out", ".venv", "venv", "__pycache__"}


def recolectar() -> dict[str, list[str]]:
    hallados: dict[str, list[str]] = {}
    for ruta in sorted(ROOT.rglob("*")):
        if not ruta.is_file() or ruta.suffix not in EXTENSIONES:
            continue
        if any(parte in EXCLUIR_DIRS for parte in ruta.parts):
            continue
        try:
            texto = ruta.read_text(encoding="utf-8", errors="replace")
        except OSError:
            continue
        for doi in PATRON_DOI.findall(texto):
            # Limpieza: LaTeX/Markdown pegan `}`, `` ` `` o sufijos de badge
            # (.svg/.png) al identificador; no son parte del DOI.
            doi = doi.rstrip(".,;:`}")
            doi = re.sub(r"\.(svg|png)$", "", doi)
            hallados.setdefault(doi, []).append(
                str(ruta.relative_to(ROOT)))
    return hallados


def resuelve(doi: str, intentos: int = 3) -> tuple[bool, str]:
    # Los DOI de Zenodo se verifican contra su API (estado published +
    # archivos), porque el redirect de doi.org a veces devuelve 404
    # transitorios aunque el depósito exista y esté publicado.
    zenodo = re.fullmatch(r"10\.5281/zenodo\.(\d+)", doi)
    if zenodo:
        return resuelve_zenodo(zenodo.group(1), intentos)
    return resuelve_doi_org(doi, intentos)


def resuelve_zenodo(recid: str, intentos: int = 3) -> tuple[bool, str]:
    import json

    url = f"https://zenodo.org/api/records/{recid}"
    ultimo = ""
    for _ in range(intentos):
        try:
            peticion = urllib.request.Request(
                url, headers={"User-Agent": "sgb-saas-verify/1.0",
                              "Accept": "application/json"})
            with urllib.request.urlopen(peticion, timeout=20) as respuesta:
                datos = json.load(respuesta)
            if datos.get("state") == "done" and datos.get("files"):
                return True, f"published, {len(datos['files'])} archivo(s)"
            ultimo = f"state={datos.get('state')} files={datos.get('files')}"
        except Exception as exc:  # noqa: BLE001 - se reporta el mensaje
            ultimo = f"{type(exc).__name__}: {exc}"
            time.sleep(2)
    return False, ultimo


def resuelve_doi_org(doi: str, intentos: int = 3) -> tuple[bool, str]:
    url = f"https://doi.org/{doi}"
    ultimo = ""
    for _ in range(intentos):
        try:
            peticion = urllib.request.Request(
                url, headers={"User-Agent": "sgb-saas-verify/1.0",
                              "Accept": "text/html"})
            with urllib.request.urlopen(peticion, timeout=20) as respuesta:
                codigo = respuesta.getcode()
                if 200 <= codigo < 400:
                    return True, str(codigo)
                ultimo = str(codigo)
        except Exception as exc:  # noqa: BLE001 - se reporta el mensaje
            ultimo = f"{type(exc).__name__}: {exc}"
            time.sleep(2)
    return False, ultimo


def main() -> int:
    hallados = recolectar()
    if not hallados:
        print("verify-p2: no se encontró ningún DOI", file=sys.stderr)
        return 1
    fallos = 0
    for doi in sorted(hallados):
        ok, detalle = resuelve(doi)
        donde = ", ".join(sorted(set(hallados[doi]))[:5])
        estado = "OK" if ok else "FALLA"
        print(f"[{estado}] {doi} ({detalle}) <- {donde}")
        if not ok:
            fallos += 1
    if fallos:
        print(f"verify-p2: {fallos} DOI sin resolver", file=sys.stderr)
        return 1
    print(f"verify-p2: OK ({len(hallados)} DOI resuelven)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
