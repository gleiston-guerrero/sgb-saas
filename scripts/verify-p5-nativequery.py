#!/usr/bin/env python3
"""P5: inventario pineado de `nativeQuery = true` (33 = 23 rutinas + 10 ordinarias).

Clasifica cada ocurrencia por bloque @Query (scanner con estado de
strings Java, no regex ingenuo): es "rutina" si el bloque invoca
FROM fn_*/sp_*; el resto es consulta ordinaria. Compara contra lo
pineado abajo y falla ante CUALQUIER alta, baja o reclasificacion:
todo cambio del inventario exige actualizar justificacion y pruebas
en el mismo commit.

Este verificador NO aprueba P5: exit 0 significa "inventario fiel a
lo documentado (excepcion tecnica ADR-006 vigente)", no punto cerrado.

Uso: python scripts/verify-p5-nativequery.py
Sale 0 si el inventario coincide, 1 ante cualquier diferencia.
"""

from __future__ import annotations

import re
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
BASE_JAVA = ROOT / "backend-springboot" / "src" / "main" / "java"

TOTAL_ESPERADO = 33
RUTINA_ESPERADA = 23

# Rutinas pineadas por archivo (nombres distintos esperados).
RUTINAS_POR_ARCHIVO: dict[str, set[str]] = {
    "FineProcedureRepository.java": {
        "sp_pago_parcial_multa",
        "fn_reporte_resumen_financiero_multas",
        "fn_pagos_recientes",
    },
    "LoanProcedureRepository.java": {
        "fn_listar_prestamos_activos_por_usuario",
        "fn_reporte_libros_mas_prestados",
        "fn_reporte_indice_morosidad",
        "fn_reporte_uso_por_periodo",
        "fn_reporte_libros_mas_prestados_detallado",
        "fn_reporte_inventario",
        "fn_reporte_prestamos_vencidos",
        "fn_reporte_categorias_demandadas",
    },
}
SITIOS_RUTINA_POR_ARCHIVO = {
    "FineProcedureRepository.java": 3,
    "LoanProcedureRepository.java": 20,
}

# Consultas ordinarias pineadas: archivo -> [(ordinal, motivo tecnico)].
# Son SELECT planos sin rutinas (la guia exige cero solo para
# procedimientos); cada una lleva motivo individual.
ORDINARIAS: dict[str, list[tuple[int, str]]] = {
    "AuditLogAuditRepository.java": [
        (1, "SELECT con filtros nativos sobre bitacora_auditoria; sin rutinas"),
    ],
    "BookRepository.java": [
        (1, "SELECT planos sobre libros (listados/inventario); sin rutinas"),
        (2, "SELECT planos sobre libros (listados/inventario); sin rutinas"),
        (3, "SELECT planos sobre libros (listados/inventario); sin rutinas"),
        (4, "SELECT plano con LIMIT 10 sobre libros; sin rutinas"),
        (5, "SELECT planos sobre libros (listados/inventario); sin rutinas"),
        (6, "SELECT planos sobre libros (listados/inventario); sin rutinas"),
    ],
    "LoanRepository.java": [
        (1, "SELECT prestamos+libros con ORDER BY; sin rutinas"),
    ],
    "ReservationRepository.java": [
        (1, "SELECT con filtros sobre reservaciones; sin rutinas"),
        (2, "SELECT con filtros sobre reservaciones; sin rutinas"),
    ],
}

PATRON_RUTINA = re.compile(r"FROM\s+(fn_\w+|sp_\w+)", re.IGNORECASE)


def falla(mensaje: str) -> int:
    print(f"verify-p5: FALLA: {mensaje}", file=sys.stderr)
    return 1


def bloques_query(texto: str) -> list[str]:
    """Extrae cada bloque @Query(...) con scanner de strings Java."""
    bloques = []
    i = 0
    while True:
        j = texto.find("@Query", i)
        if j < 0:
            break
        k = texto.find("(", j)
        if k < 0:
            break
        prof, p = 0, k
        literal = None  # None, '"', "'", '"""'
        while p < len(texto):
            c = texto[p]
            if literal is None:
                if texto.startswith('"""', p):
                    literal = '"""'
                    p += 3
                    continue
                if c in "\"'":
                    literal = c
                    p += 1
                    continue
                if c == "(":
                    prof += 1
                elif c == ")":
                    prof -= 1
                    if prof == 0:
                        bloques.append(texto[j:p + 1])
                        break
                p += 1
            else:
                if c == "\\":
                    p += 2
                    continue
                if literal == '"""' and texto.startswith('"""', p):
                    literal = None
                    p += 3
                    continue
                if literal != '"""' and c == literal:
                    literal = None
                p += 1
        i = k + 1
    return bloques


def main() -> int:
    proc = subprocess.run(
        ["git", "grep", "-n", "nativeQuery = true", "--",
         "backend-springboot/src/main/java"],
        cwd=ROOT, capture_output=True, text=True)
    if proc.returncode not in (0, 1):
        return falla(f"git grep fallo: {proc.stderr[-300:]}")
    total = len([l for l in proc.stdout.splitlines() if l.strip()])
    if total != TOTAL_ESPERADO:
        return falla(f"total nativeQuery = true: {total} != {TOTAL_ESPERADO} pineado")

    sitios_rutina = 0
    rutinas_vistas: dict[str, set[str]] = {}
    ordinarias_vistas: dict[str, int] = {}
    detalle = []
    archivos = sorted({l.split(":", 2)[0]
                       for l in proc.stdout.splitlines() if l.strip()})
    for rel in archivos:
        nombre = Path(rel).name
        texto = (ROOT / rel).read_text(encoding="utf-8", errors="replace")
        for bloque in bloques_query(texto):
            if not re.search(r"\bnativeQuery\s*=\s*true\b", bloque):
                continue
            rutinas = sorted(set(PATRON_RUTINA.findall(bloque)))
            if rutinas:
                sitios_rutina += 1
                rutinas_vistas.setdefault(nombre, set()).update(rutinas)
                detalle.append(f"  RUTINA {nombre}: {', '.join(rutinas)}")
            else:
                ordinarias_vistas[nombre] = ordinarias_vistas.get(nombre, 0) + 1
                detalle.append(f"  ordinaria {nombre} #{ordinarias_vistas[nombre]}")

    if sitios_rutina != RUTINA_ESPERADA:
        return falla(f"sitios de rutina: {sitios_rutina} != {RUTINA_ESPERADA}")
    for arch, esperado in RUTINAS_POR_ARCHIVO.items():
        if rutinas_vistas.get(arch, set()) != esperado:
            return falla(f"{arch}: rutinas {sorted(rutinas_vistas.get(arch, set()))} != pineado")
    for arch, sitios in SITIOS_RUTINA_POR_ARCHIVO.items():
        n = sum(1 for d in detalle if d.startswith(f"  RUTINA {arch}:"))
        if n != sitios:
            return falla(f"{arch}: {n} sitios de rutina != {sitios} pineados")
    for arch, items in ORDINARIAS.items():
        if ordinarias_vistas.get(arch, 0) != len(items):
            return falla(f"{arch}: {ordinarias_vistas.get(arch, 0)} ordinarias != {len(items)} pineadas")
    if set(ordinarias_vistas) != set(ORDINARIAS):
        return falla(f"archivos con ordinarias difieren: {sorted(ordinarias_vistas)}")

    print(f"verify-p5: OK (inventario {total}={sitios_rutina}+{total - sitios_rutina} coincide)")
    for d in detalle:
        print(f"verify-p5: {d}")
    print("verify-p5: pendiente documentado (excepcion tecnica ADR-006; sin migraciones a ciegas)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
