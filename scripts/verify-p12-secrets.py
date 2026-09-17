#!/usr/bin/env python3
"""P12: ninguna credencial real en el árbol evaluado (guía examen suspenso).

Rastrea el árbol de trabajo (sin .git, node_modules, target, dist,
graphify-out, .opencode) buscando patrones de secretos reales:
URLs postgres con password, jdbc con password, claves privadas,
tokens conocidos (AKIA, xox-, ghp_, etc.). Los placeholders de
desarrollo y academia están permitidos explícitamente.

La rotación en sí se evidencia en docs/despliegue/NEON-ROTATION-ACTA.md
(ver VERIFICACION.md); este script cubre "ninguna credencial real en
el árbol evaluado".

Uso: python scripts/verify-p12-secrets.py
Sale 0 si está limpio, 1 si hay hallazgos.
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
EXCLUIR_DIRS = {".git", "node_modules", "target", "dist", ".opencode",
                "graphify-out", ".venv", "venv", "__pycache__", ".idea",
                ".vscode", "jacoco", "surefire-reports"}

PATRONES = [
    # postgres://usuario:password@host (password no vacía ni placeholder)
    (re.compile(r"postgres(?:ql)?://[^/\s:]+:([^@/\s]+)@"), "postgres-url-con-password"),
    (re.compile(r"jdbc:postgresql://[^\s\"']*password\s*=\s*([^\\s\"'&;]+)", re.IGNORECASE),
     "jdbc-con-password"),
    (re.compile(r"-----BEGIN (?:RSA |OPENSSH |EC )?PRIVATE KEY-----"), "clave-privada"),
    (re.compile(r"\bAKIA[0-9A-Z]{16}\b"), "aws-access-key"),
    (re.compile(r"\bxox[baprs]-[0-9A-Za-z-]{10,}\b"), "slack-token"),
    (re.compile(r"\bghp_[0-9A-Za-z]{36}\b"), "github-token"),
    (re.compile(r"\bSG\.[0-9A-Za-z_-]{22}\.[0-9A-Za-z_-]{43}\b"), "sendgrid-key"),
]

# Placeholders y credenciales académicas/demo: nunca son hallazgo.
PERMITIDOS = re.compile(
    r"changeme|dummy|placeholder|ejemplo|example|CAMBIAR_|cambiar_"
    r"|Admin123!|Password123!|ClaveSegura123!|usuario1|Lector123!|Bibliotecario123!"
    r"|Gerente123!|password123|\$\{|<[^>]*>|xxx+|test123|12345678"
    r"|usuario:password|user:pass|postgres:postgres@localhost",
    re.IGNORECASE,
)


def main() -> int:
    hallazgos: list[str] = []
    archivos = 0
    for ruta in sorted(ROOT.rglob("*")):
        if not ruta.is_file():
            continue
        if any(parte in EXCLUIR_DIRS for parte in ruta.parts):
            continue
        try:
            if ruta.stat().st_size > 2_000_000:
                continue
            texto = ruta.read_text(encoding="utf-8", errors="strict")
        except (OSError, UnicodeDecodeError, ValueError):
            continue
        archivos += 1
        rel = str(ruta.relative_to(ROOT))
        for numero, linea in enumerate(texto.splitlines(), start=1):
            for patron, nombre in PATRONES:
                for coincidencia in patron.findall(linea):
                    valor = coincidencia if isinstance(coincidencia, str) else coincidencia[0]
                    if PERMITIDOS.search(valor) or PERMITIDOS.search(linea):
                        continue
                    hallazgos.append(f"{rel}:{numero} [{nombre}]")
    if hallazgos:
        print("verify-p12: HALLAZGOS (credencial posible):", file=sys.stderr)
        for hallazgo in sorted(set(hallazgos))[:50]:
            print(f"  {hallazgo}", file=sys.stderr)
        return 1
    print(f"verify-p12: OK (árbol limpio, {archivos} archivos revisados)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
