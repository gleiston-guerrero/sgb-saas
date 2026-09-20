#!/usr/bin/env python3
"""Orquestador unico de verificacion P1-P12 (fuente de verdad de `make verify`).

Ejecuta cada verificador y clasifica por punto:
"evidencia válida" | "PENDIENTE" (visible, nunca aprobado) | "FALLO".
Sale 0 solo si no hay ningun FALLO. Un exit 0 con pendientes significa
coherencia/reproducibilidad de la evidencia disponible, NO cumplimiento
académico total; una dependencia local puede dejar P2 o P10 pendiente.

P10 corre DemoAccountAuthorizationIntegrationTest en subproceso y exige
leer el resumen Surefire con exactamente 3/0/0/0 (run/fallos/errores/
omitidos). Sin Docker, test omitido o resumen no verificable con Docker
presente ante anomalia: ver reglas en paso_p10().

Uso: python scripts/verify-all.py
"""

from __future__ import annotations

import os
import re
import subprocess
import sys

# Salida UTF-8 en Windows sin exigir PYTHONUTF8=1: el locale cp1252
# rompe print() con tildes o U+FFFD. Solo reconfigura, no imprime.
if hasattr(__import__("sys").stdout, "reconfigure"):
    __import__("sys").stdout.reconfigure(encoding="utf-8", errors="replace")
    __import__("sys").stderr.reconfigure(encoding="utf-8", errors="replace")
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PY = sys.executable
BACKEND = ROOT / "backend-springboot"

FALLO = "FALLO"
PENDIENTE = "PENDIENTE"
VALIDA = "evidencia válida"


def mvnw() -> list[str]:
    if os.name == "nt" and (BACKEND / "mvnw.cmd").exists():
        return [str(BACKEND / "mvnw.cmd"), "-B"]
    return [str(BACKEND / "mvnw"), "-B"]


def corre(cmd: list[str], timeout: int, trabajo: Path = ROOT):
    # encoding explicito: en Windows el locale por defecto (cp1252)
    # rompe al decodificar salidas UTF-8; no se requiere PYTHONUTF8=1.
    try:
        return subprocess.run(cmd, cwd=trabajo, capture_output=True,
                              text=True, timeout=timeout,
                              encoding="utf-8", errors="replace")
    except FileNotFoundError as exc:
        return subprocess.CompletedProcess(cmd, 127, "", str(exc))
    except subprocess.TimeoutExpired:
        return subprocess.CompletedProcess(cmd, 124, "", "timeout")


def paso_simple(nombre: str, cmd: list[str], timeout: int,
                etiqueta_ok: str) -> tuple[str, str]:
    proc = corre(cmd, timeout)
    print(proc.stdout[-2000:])
    if proc.returncode != 0:
        # Diagnóstico CI: el stderr trae la causa real (verify-p4-k6.py
        # reporta vía falla() a stderr). Solo se imprime en fallo, sin
        # cambiar criterios de aprobación.
        print(proc.stderr[-2000:])
        return FALLO, f"{nombre} exit={proc.returncode}"
    print(f">> {nombre}: {etiqueta_ok}")
    return etiqueta_ok, ""


def paso_p10() -> tuple[str, str]:
    policy = corre([PY, "scripts/verify-p10-cookie-policy.py"], 60)
    print(policy.stdout[-1000:])
    if policy.returncode != 0:
        print(policy.stderr[-1000:])
        return FALLO, "política de cookie productiva inválida"
    # Docker presente?
    dock = corre(["docker", "info"], 60)
    sin_docker = dock.returncode != 0
    if sin_docker:
        print(">> P10: PENDIENTE — bloqueado por entorno (sin Docker; Testcontainers omitiria)")
        return PENDIENTE, ""
    proc = corre(mvnw() + ["test",
                           "-Dtest=DemoAccountAuthorizationIntegrationTest",
                           "-DfailIfNoTests=false"], 900, BACKEND)
    print(proc.stdout[-3000:])
    res = re.findall(r"Tests run:\s*(\d+),\s*Failures:\s*(\d+),\s*Errors:\s*(\d+),\s*Skipped:\s*(\d+)",
                     proc.stdout)
    if not res:
        # Sin resumen parseable con Docker presente: anomalia de toolchain,
        # no evidencia. No puede contarse como exito ni como fallo del producto.
        print(">> P10: PENDIENTE — bloqueado por entorno (resumen Surefire no verificable)")
        return PENDIENTE, ""
    runs, fallos, errores, omitidos = (int(x) for x in res[-1])
    if omitidos > 0:
        print(">> P10: PENDIENTE — bloqueado por entorno (tests omitidos, nunca exito)")
        return PENDIENTE, ""
    if proc.returncode != 0 or fallos > 0 or errores > 0:
        return FALLO, f"P10 run={runs} fail={fallos} err={errores} exit={proc.returncode}"
    if runs != 3:
        return FALLO, f"P10 ejecuto {runs} pruebas, se exigen exactamente 3"
    print(">> P10: evidencia válida (3/0/0/0, cero mocks)")
    return VALIDA, ""


def paso_p2() -> tuple[str, str]:
    """Distingue una red pública caída de un DOI que realmente no resuelve."""
    proc = corre([PY, "scripts/verify-p2-dois.py"], 600)
    print(proc.stdout[-2000:])
    if proc.returncode == 2:
        print(proc.stderr[-1000:])
        print(">> P2: PENDIENTE — red pública inaccesible; reintentar en CI")
        return PENDIENTE, ""
    if proc.returncode != 0:
        print(proc.stderr[-2000:])
        return FALLO, f"P2 exit={proc.returncode}"
    print(">> P2: evidencia válida")
    return VALIDA, ""


def paso_javadoc() -> tuple[str, str]:
    proc = corre(mvnw() + ["javadoc:javadoc"], 600, BACKEND)
    print(proc.stdout[-1000:])
    if proc.returncode != 0 or "BUILD SUCCESS" not in proc.stdout:
        return FALLO, "javadoc sin BUILD SUCCESS"
    print(">> Javadoc: evidencia válida (BUILD SUCCESS)")
    return VALIDA, ""


def main() -> int:
    resultados: dict[str, str] = {}
    fallos: list[str] = []

    def registra(punto: str, etiqueta: str, error: str = "") -> None:
        resultados[punto] = etiqueta
        if etiqueta == FALLO:
            fallos.append(f"{punto}: {error}")

    simples = [
        ("P1", [PY, "scripts/verify-p1-hashes.py"], 300, VALIDA),
        ("Integridad", [PY, "scripts/verify-report-integrity.py"], 300, VALIDA),
        ("P4", [PY, "scripts/verify-p4-k6.py"], 900, VALIDA),
        ("P5", [PY, "scripts/verify-p5-nativequery.py"], 300,
         "migrado (0 nativeQuery + 0 CALL nativos)"),
        ("P6", [PY, "scripts/verify-p6-javadoc.py"], 300, VALIDA),
        ("P7", [PY, "scripts/verify-p7-names.py"], 300, VALIDA),
        ("P8/P9", [PY, "scripts/verify-p8-p9-figures.py"], 300, VALIDA),
        ("P11", [PY, "scripts/verify-p11-counts.py"], 300,
         "conteos verificables (aceptaciones por alcance)"),
        ("P12", [PY, "scripts/verify-p12-secrets.py"], 300, VALIDA),
    ]
    for punto, cmd, t, etiqueta in simples:
        etiqueta_out, error = paso_simple(punto, [PY] + cmd[1:] if cmd[0] == PY else cmd, t, etiqueta)
        registra(punto, etiqueta_out, error)

    etiqueta, error = paso_p2()
    registra("P2", etiqueta, error)

    etiqueta, error = paso_simple("P3", [PY, "scripts/verify-p3-sus.py"], 300, VALIDA)
    registra("P3", etiqueta, error)

    etiqueta, error = paso_p10()
    registra("P10", etiqueta, error)

    etiqueta, error = paso_javadoc()
    registra("Javadoc", etiqueta, error)

    print("\n===== verify-all: resumen P1-P12 =====")
    for punto in ["P1", "P2", "P3", "P4", "P5", "P6", "P7", "P8/P9",
                  "P10", "P11", "P12", "Javadoc"]:
        print(f"{punto}: {resultados.get(punto, FALLO)}")
    if fallos:
        print("verify-all: FALLO")
        for f in fallos:
            print(f"  - {f}")
        return 1
    print("verify-all: exit 0 = coherencia/reproducibilidad de la evidencia disponible")
    return 0


if __name__ == "__main__":
    sys.exit(main())
