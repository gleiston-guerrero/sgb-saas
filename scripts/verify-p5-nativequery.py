#!/usr/bin/env python3
"""P5: inventario pineado de `nativeQuery = true` en descenso hacia 0.

Clasifica cada ocurrencia por bloque @Query (scanner con estado de
strings Java, no regex ingenuo): es "rutina" si el bloque invoca
FROM fn_*/sp_*; el resto es consulta ordinaria. Falla ante CUALQUIER
alta, baja o reclasificacion no declarada: cada migracion actualiza los
pineados + MIGRADAS en el mismo commit, con prueba de equivalencia.

Segunda dimension: los `createNativeQuery("CALL ...")` en *CustomImpl
(tambien pineados por archivo) deben bajar solo via @Procedure real.

Este verificador NO aprueba P5: exit 0 significa "inventario fiel a
lo documentado", no punto cerrado. P5 cierra en 0 nativas + 0 CALL
nativos en CustomImpl.

Uso: python scripts/verify-p5-nativequery.py
Sale 0 si el inventario coincide, 1 ante cualquier diferencia.
"""

from __future__ import annotations

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
BASE_JAVA = ROOT / "backend-springboot" / "src" / "main" / "java"

TOTAL_ESPERADO = 0
RUTINA_ESPERADA = 0

# Rutinas pineadas por archivo (nombres distintos esperados).
# Vacío: las 23 rutinas migraron (ver MIGRADAS). Cualquier nativeQuery
# o FROM fn_/sp_ que reaparezca falla el verificador.
RUTINAS_POR_ARCHIVO: dict[str, set[str]] = {
}
SITIOS_RUTINA_POR_ARCHIVO = {
}

# Consultas ordinarias pineadas: archivo -> [(ordinal, motivo tecnico)].
# Vacío: las 10 ordinarias migraron (ver MIGRADAS). El mapa se conserva
# para que el chequeo de abajo siga exigiendo cero nativas ordinarias.
ORDINARIAS: dict[str, list[tuple[int, str]]] = {
}
# Migraciones cerradas con prueba de equivalencia (metodo, reemplazo, prueba).
# Cada fila resta del inventario; prohibido borrar nativas sin fila aqui.
MIGRADAS = [
    ("ReservationRepository.searchReservationsToday",
     "JPQL cartesiana + ventana :start/:end desde el service (zona sistema)",
     "P5SpikeIT.s5_ventanaHoyYProximas + ReservationServiceTest 21/21"),
    ("ReservationRepository.searchReservationsNexts",
     "JPQL cartesiana + :start desde el service (zona sistema)",
     "P5SpikeIT.s5_ventanaHoyYProximas + ReservationServiceTest 21/21"),
    ("LoanRepository.findActivesByUserId",
     "JPQL cartesiana + diasRestantes en Java (zona sistema, igual que NOW()::date)",
     "P5SpikeIT.s6_activosPorUsuarioJpqlYDiasJava + LoanServiceTest 31/31"),
    ("BookRepository.searchByTextOIsbn",
     "Criteria searchText (sin categoria/autor, available tri-estado)",
     "P5SpikeIT.s7 + BookServiceTest 21/21"),
    ("BookRepository.searchByTextOIsbnYCategory",
     "Criteria searchText con join categories + countDistinct",
     "P5SpikeIT.s7 + BookServiceTest 21/21"),
    ("BookRepository.searchByTextOrIsbnAndAuthor",
     "Criteria searchText con join authors",
     "P5SpikeIT.s7 + BookServiceTest 21/21"),
    ("BookRepository.suggestByTitle",
     "Criteria function(similarity) + maxResults 10",
     "P5SpikeIT.s7 + BookServiceTest 21/21"),
    ("BookRepository.searchPendientes",
     "Eliminada: sin llamadores en src/main ni tests",
     "compilacion + suite (sin referencias)"),
    ("BookRepository.searchByStatuses",
     "Criteria searchByStatusesCriteria (q/year opcionales)",
     "P5SpikeIT.s7 + BookServiceTest 21/21"),
    ("AuditLogAuditRepository.searchWithFilters",
     "Criteria dinamico + Sort fecha_hora->dateTime (controller y exportCsv a dateTime)",
     "P5SpikeIT.s8 + AuditServiceTest 4/4"),
    ("LoanProcedureRepositoryCustomImpl.spCreateLoanProcedure/spRegisterLoanReturn",
     "createStoredProcedureQuery posicional (proc_crear_prestamo/proc_registrar_devolucion)",
     "P5SpikeIT.s1/s9a + LoanFineProcedureIntegrationTest (CI)"),
    ("FineProcedureRepositoryCustomImpl.spPayFineProcedure/spVoidFineProcedure",
     "createStoredProcedureQuery posicional (proc_pagar_multa/proc_anular_multa)",
     "P5SpikeIT.s9b/s9c + LoanFineProcedureIntegrationTest (CI)"),
    ("ReservationProcedureRepositoryCustomImpl.spExpireReservationsVencidasProcedure",
     "createStoredProcedureQuery posicional (proc_expirar_reservaciones_vencidas)",
     "P5SpikeIT.s9d"),
    ("FineProcedureRepository.spPaymentParcialFine",
     "wrapper V54 proc_pago_parcial_multa + StoredProcedureQuery posicional (mismas 4 claves)",
     "P5SpikeIT.s9e"),
    ("FineProcedureRepository.fnReportSummaryFinancial",
     "Criteria CASE (mismo patron que summaryByCategory) + cero NUMERIC(12,2)",
     "P5SpikeIT.s10 + FineServiceTest 9/9"),
    ("FineProcedureRepository.fnPaymentsRecientes",
     "Criteria cartesiana PAGADA + setMaxResults (default 5)",
     "P5SpikeIT.s10 + FineServiceTest 9/9"),
    ("LoanProcedureRepository.fnListLoansActivesByUser",
     "JPQL cartesiana (base sin dias; LoanService solo usa size)",
     "P5TabularSpikeIT.t1"),
    ("LoanProcedureRepository.fnReportBooksMostLoaned",
     "Criteria GROUP/COUNT + maxResults (NULL = todo, como LIMIT NULL)",
     "P5TabularSpikeIT.t2"),
    ("LoanProcedureRepository.fnReportIndexDelinquency[+Paginated]",
     "Criteria filas PENDIENTE + AVG Java ROUND(,1) + ORDER/LIMIT en Java",
     "P5TabularSpikeIT.t3"),
    ("LoanProcedureRepository.fnReportUsageByPeriod[+Paginated]",
     "Criteria date_trunc + FULL OUTER en Java (TreeMap)",
     "P5TabularSpikeIT.t4"),
    ("LoanProcedureRepository.fnReportBooksMostLoanedDetailed[+Paginated]",
     "Criteria filas + string_agg en Java (TreeSet, defaults) + pct",
     "P5TabularSpikeIT.t5"),
    ("LoanProcedureRepository.fnReportInventory[+Paginated]",
     "Criteria 14 filtros + agregados en Java + estado_disponibilidad",
     "P5TabularSpikeIT.t6"),
    ("LoanProcedureRepository.fnReportLoansOverdues[+Paginated]",
     "Criteria ventana + dias/multa en Java (tarifa config, default 1)",
     "P5TabularSpikeIT.t7"),
    ("LoanProcedureRepository.fnReportCategoriesDemanded[+Paginated]",
     "Criteria GROUP/COUNT + pct en Java (limite ignorado como la funcion)",
     "P5TabularSpikeIT.t8"),
]

# CALL nativos en *CustomImpl pineados por archivo (bajan solo con @Procedure real).
# En cero: los 5 CALL migraron a StoredProcedureQuery posicional.
CALL_NATIVOS_POR_ARCHIVO = {
    "LoanProcedureRepositoryCustomImpl.java": 0,
    "FineProcedureRepositoryCustomImpl.java": 0,
    "ReservationProcedureRepositoryCustomImpl.java": 0,
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
    for metodo, reemplazo, prueba in MIGRADAS:
        print(f"verify-p5: migrada {metodo} -> {reemplazo} [{prueba}]")
    # Segunda dimension: CALL nativos ocultos en CustomImpl.
    for arch, n in CALL_NATIVOS_POR_ARCHIVO.items():
        proc2 = subprocess.run(
            ["git", "grep", "-c", "createNativeQuery", "--",
             f"backend-springboot/src/main/java/com/uteq/backend/repository/{arch}"],
            cwd=ROOT, capture_output=True, text=True,
            encoding="utf-8", errors="replace")
        vistos = int(proc2.stdout.strip().split(":")[-1]) if proc2.returncode == 0 else 0
        if vistos != n:
            return falla(f"{arch}: {vistos} createNativeQuery != {n} pineados")
    print("verify-p5: OK (CALL nativos en CustomImpl pineados)")
    print("verify-p5: pendiente documentado (excepcion tecnica ADR-006; sin migraciones a ciegas)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
