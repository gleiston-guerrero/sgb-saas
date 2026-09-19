#!/usr/bin/env python3
"""P2: todos los DOI declarados resuelven HTTP 200 (guía examen suspenso).

Extrae DOIs con forma 10.NNNN/... de CITATION.cff, README.md y docs/
(*.md, *.tex, *.txt; excluye historial .git) y comprueba cada uno contra
https://doi.org con timeout y reintento (sigue redirects, exige 200
final). Para DOIs 10.5281/zenodo además exige la API de Zenodo
(published + archivos) como evidencia adicional. Los DOI históricos
mencionados también deben resolver: si alguno devuelve 404, hay que
actualizar la referencia (barrido P2: solo DOIs vigentes).

Uso: python scripts/verify-p2-dois.py
Sale 0 si todos resuelven, 1 si algún DOI responde pero no es válido,
y 2 si la red pública no estuvo disponible para ninguno de ellos. El
código 2 no convierte un fallo académico en éxito: el orquestador lo
declara PENDIENTE para que el mismo comando pueda reintentarse en CI.
"""

from __future__ import annotations

import re
import sys

# Salida UTF-8 en Windows sin exigir PYTHONUTF8=1: el locale cp1252
# rompe print() con tildes o U+FFFD. Solo reconfigura, no imprime.
if hasattr(__import__("sys").stdout, "reconfigure"):
    __import__("sys").stdout.reconfigure(encoding="utf-8", errors="replace")
    __import__("sys").stderr.reconfigure(encoding="utf-8", errors="replace")
import time
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PATRON_DOI = re.compile(r"10\.\d{4,}/[^\s)\"',;>\\]+")
EXTENSIONES = {".cff", ".md", ".tex", ".txt"}
EXCLUIR_DIRS = {".git", "node_modules", "target", "dist", ".opencode",
                "graphify-out", ".venv", "venv", "__pycache__",
                # docs/evidencia/: registros históricos fechados de corridas
                # (como el historial Git): citan DOIs del momento de la
                # corrida y no son declaraciones vigentes.
                "evidencia"}
# Documento local de trabajo del equipo (gitignored, no entregable):
# no forma parte del tag evaluado y no debe condicionar el resultado.
EXCLUIR_ARCHIVOS = {"PLAN_RECUPERACION_NOTA_8_HONESTO.md"}


def recolectar() -> dict[str, list[str]]:
    hallados: dict[str, list[str]] = {}
    for ruta in sorted(ROOT.rglob("*")):
        if not ruta.is_file() or ruta.suffix not in EXTENSIONES:
            continue
        if ruta.name in EXCLUIR_ARCHIVOS:
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
    # Primario: doi.org con redirects (exigencia literal de la guía).
    # Para Zenodo se suma la API (published + archivos) como adicional.
    zenodo = re.fullmatch(r"10\.5281/zenodo\.(\d+)", doi)
    ok_doi, det_doi = resuelve_doi_org(doi, intentos)
    if not zenodo:
        return ok_doi, det_doi
    ok_zen, det_zen = resuelve_zenodo(zenodo.group(1), intentos)
    ok = ok_doi and ok_zen
    return ok, f"doi.org: {det_doi}; zenodo: {det_zen}"


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
                final = respuesta.geturl()
                if 200 <= codigo < 400:
                    return True, f"{codigo} -> {final}"
                ultimo = f"{codigo} -> {final}"
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
    red_inaccesible = 0
    for doi in sorted(hallados):
        ok, detalle = resuelve(doi)
        donde = ", ".join(sorted(set(hallados[doi]))[:5])
        estado = "OK" if ok else "FALLA"
        print(f"[{estado}] {doi} ({detalle}) <- {donde}")
        if not ok:
            fallos += 1
            # URLError/timeout significa que no hubo respuesta para evaluar
            # el DOI. Un 404 o un estado Zenodo inválido sigue siendo FALLO.
            if "URLError:" in detalle or "TimeoutError:" in detalle:
                red_inaccesible += 1
    if fallos:
        if red_inaccesible == fallos:
            print("verify-p2: PENDIENTE — red pública inaccesible; reintentar en CI",
                  file=sys.stderr)
            return 2
        print(f"verify-p2: {fallos} DOI sin resolver", file=sys.stderr)
        return 1
    print(f"verify-p2: OK ({len(hallados)} DOI resuelven)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
