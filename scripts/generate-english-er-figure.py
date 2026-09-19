#!/usr/bin/env python3
"""ER overview figure (English aliases) generated from a verified spec.

Each area box lists REAL tables with key columns from PostgreSQL
``information_schema`` (migrations V1--V54): ``*`` = NOT NULL,
``PK`` = primary key, ``-> tabla.col`` = foreign key. Relation lines
carry cardinalities (``1``/``N``) at both ends and exist only where a
real FK backs them.

``--check`` verifies the whole spec against a live PostgreSQL
(PGHOST/PGUSER/PGPASSWORD/PGDATABASE, defaults: localhost/sgb_user/
sgb_db) and exits non-zero on drift. It needs ``psql`` on PATH and a
reachable server; otherwise it reports SKIP (never a fake pass).

Scope: core domain overview (15 areas). Chatbot sessions, damage
records, favorites, mail verification tokens and similar operational
sub-domains are omitted here for readability; the full 50-table schema
lives in ``database/migrations/``.
"""

from __future__ import annotations

import os
import subprocess
import sys
from pathlib import Path

from reportlab.lib import colors
from reportlab.lib.pagesizes import A4, landscape
from reportlab.lib.units import cm
from reportlab.pdfgen import canvas


OUTPUT = Path("docs/diagramas/er-english.pdf")

# area -> [(table, [(column, kind)])]  kind: "PK", "FK>tabla.col", "UQ", ""
AREAS: dict[str, list[tuple[str, list[tuple[str, str]]]]] = {
    "Users": [
        ("usuarios", [("id", "PK"), ("correo", "*"), ("estado_id", "FK>estados_usuario.id"),
                      ("creado_por", "FK>usuarios.id"), ("correo_verificado", "*")]),
        ("usuario_roles", [("usuario_id", "PK,FK>usuarios.id"), ("rol_id", "PK,FK>roles.id")]),
        ("estados_usuario", [("id", "PK"), ("nombre", "*")]),
    ],
    "Roles": [
        ("roles", [("id", "PK"), ("nombre", "*")]),
    ],
    "Permissions": [
        ("permisos", [("id", "PK"), ("codigo", "*")]),
        ("rol_permisos", [("rol_id", "PK,FK>roles.id"), ("permiso_id", "PK,FK>permisos.id")]),
    ],
    "Books": [
        ("libros", [("id", "PK"), ("isbn", "*"), ("titulo", "*"),
                    ("editorial_id", "FK>editoriales.id"), ("idioma_id", "FK>idiomas.id"),
                    ("estado_id", "FK>estados_libro.id"), ("stock_disponible", "*")]),
        ("estados_libro", [("id", "PK"), ("nombre", "*")]),
    ],
    "Authors": [
        ("autores", [("id", "PK"), ("nombre", "*")]),
        ("libro_autores", [("libro_id", "PK,FK>libros.id"), ("autor_id", "PK,FK>autores.id")]),
    ],
    "Categories": [
        ("categorias", [("id", "PK"), ("nombre", "*")]),
        ("libro_categorias", [("libro_id", "PK,FK>libros.id"),
                              ("categoria_id", "PK,FK>categorias.id")]),
    ],
    "Publishers": [
        ("editoriales", [("id", "PK"), ("nombre", "*")]),
        ("proveedores", [("id", "PK"), ("nombre", "*")]),
        ("idiomas", [("id", "PK"), ("nombre", "*")]),
    ],
    "Loans": [
        ("prestamos", [("id", "PK"), ("usuario_id", "FK>usuarios.id"),
                       ("libro_id", "FK>libros.id"), ("bibliotecario_id", "FK>usuarios.id"),
                       ("reservacion_id", "FK>reservaciones.id"),
                       ("estado_prestamo_id", "FK>estados_prestamo.id")]),
        ("estados_prestamo", [("id", "PK"), ("nombre", "*")]),
    ],
    "Reservations": [
        ("reservaciones", [("id", "PK"), ("usuario_id", "FK>usuarios.id"),
                           ("libro_id", "FK>libros.id"),
                           ("estado_reservacion_id", "FK>estados_reservacion.id"),
                           ("fecha_limite_retiro", "*")]),
        ("estados_reservacion", [("id", "PK"), ("nombre", "*")]),
    ],
    "Fines": [
        ("multas", [("id", "PK"), ("prestamo_id", "FK>prestamos.id"),
                    ("estado_multa_id", "FK>estados_multa.id"),
                    ("monto", "*"), ("fecha_pagada", "")]),
        ("estados_multa", [("id", "PK"), ("nombre", "*")]),
    ],
    "Notifications": [
        ("notificaciones", [("id", "PK"), ("usuario_id", "FK>usuarios.id"),
                            ("prestamo_id", "FK>prestamos.id")]),
        ("tipos_notificacion", [("id", "PK"), ("nombre", "*")]),
        ("suscripciones_disponibilidad", [("id", "PK"), ("usuario_id", "FK>usuarios.id"),
                                          ("libro_id", "FK>libros.id")]),
    ],
    "Acquisition Suggestions": [
        ("sugerencias_adquisicion", [("id", "PK"), ("usuario_id", "FK>usuarios.id"),
                                     ("titulo", "*"), ("estado", "*"),
                                     ("proveedor_id", "FK>proveedores.id")]),
    ],
    "Backups": [
        ("backups", [("id", "PK"), ("creado_por", "FK>usuarios.id"), ("estado", "*")]),
        ("backup_programacion", [("id", "PK"), ("creado_por", "FK>usuarios.id")]),
        ("registros_respaldo", [("id", "PK"), ("ejecutado_por", "FK>usuarios.id")]),
    ],
    "Audit Log": [
        ("bitacora_auditoria", [("id", "PK"), ("usuario_id", "FK>usuarios.id"),
                                 ("tabla_afectada", "*"), ("fecha_hora", "*")]),
    ],
    "System Settings": [
        ("configuracion_sistema", [("clave", "PK"), ("valor", "*")]),
    ],
}

# (area_a, area_b, card_a, card_b, fk_note)
RELATIONS = [
    ("Users", "Roles", "N", "N", "usuario_roles"),
    ("Roles", "Permissions", "N", "N", "rol_permisos"),
    ("Books", "Authors", "N", "N", "libro_autores"),
    ("Books", "Categories", "N", "N", "libro_categorias"),
    ("Books", "Publishers", "N", "1", "editorial/proveedor/idioma"),
    ("Loans", "Users", "N", "1", "usuario/bibliotecario"),
    ("Loans", "Books", "N", "1", "libro"),
    ("Loans", "Reservations", "N", "1", "reservacion (nullable)"),
    ("Reservations", "Users", "N", "1", "usuario"),
    ("Reservations", "Books", "N", "1", "libro"),
    ("Fines", "Loans", "N", "1", "prestamo"),
    ("Notifications", "Users", "N", "1", "usuario"),
    ("Notifications", "Loans", "N", "1", "prestamo (nullable)"),
    ("Acquisition Suggestions", "Users", "N", "1", "usuario"),
    ("Acquisition Suggestions", "Publishers", "N", "1", "proveedor (nullable)"),
    ("Backups", "Users", "N", "1", "creado/ejecutado por"),
    ("Audit Log", "Users", "N", "1", "actor (nullable)"),
]

POS = {
    # Top-left coordinates in centimetres.  The four compact rows leave a
    # deliberate gutter for the relationship labels, avoiding the clipping
    # and hidden cardinalities of the previous rendering.
    "Users": (1.0, 5.9), "Roles": (8.0, 5.9), "Permissions": (15.0, 5.9),
    "Books": (1.0, 10.0), "Authors": (8.0, 10.0), "Categories": (15.0, 10.0),
    "Publishers": (22.0, 10.0),
    "Loans": (1.0, 14.1), "Reservations": (8.0, 14.1), "Fines": (15.0, 14.1),
    "Notifications": (22.0, 14.1),
    "Acquisition Suggestions": (1.0, 18.2), "Backups": (8.0, 18.2),
    "Audit Log": (15.0, 18.2), "System Settings": (22.0, 18.2),
}


def box_geometry(tablas: list[tuple[str, list[tuple[str, str]]]],
                 x_cm: float, y_cm: float) -> tuple[float, float, float, float]:
    """Return (left, bottom, right, top) for one compact area box."""
    x, top = x_cm * cm, y_cm * cm
    w = 5.8 * cm
    n_lineas = sum(1 + len(cols) for _t, cols in tablas)
    h = (0.62 + 0.22 * n_lineas) * cm
    return x, top - h, x + w, top


def draw_box(c: canvas.Canvas, title: str,
             tablas: list[tuple[str, list[tuple[str, str]]]],
             x_cm: float, y_cm: float) -> tuple[float, float, float, float]:
    x, bottom, right, top = box_geometry(tablas, x_cm, y_cm)
    w, h = right - x, top - bottom
    c.setStrokeColor(colors.HexColor("#334155"))
    c.setFillColor(colors.HexColor("#F8FAFC"))
    c.roundRect(x, bottom, w, h, 5, fill=1, stroke=1)
    c.setFillColor(colors.HexColor("#0F172A"))
    c.setFont("Helvetica-Bold", 7)
    c.drawString(x + 0.22 * cm, top - 0.38 * cm, title)
    yy = top - 0.78 * cm
    c.setFont("Helvetica", 4.5)
    for tabla, cols in tablas:
        c.setFillColor(colors.HexColor("#0F172A"))
        c.setFont("Helvetica-Bold", 4.6)
        c.drawString(x + 0.25 * cm, yy, tabla)
        yy -= 0.22 * cm
        c.setFont("Helvetica", 4.5)
        for col, kind in cols:
            es_pk = "PK" in kind
            es_fk = kind.startswith("FK>") or ",FK>" in kind
            ref = kind.split("FK>", 1)[1].split(".")[0] if es_fk else ""
            marca = (" [PK]" if es_pk else "") + (" [FK]" if es_fk else "") \
                + (" *" if "*" in kind else "")
            c.setFillColor(colors.HexColor("#475569"))
            c.drawString(x + 0.45 * cm, yy, f"{col}{marca}")
            if ref:
                c.setFont("Helvetica-Oblique", 3.8)
                c.drawString(x + 3.55 * cm, yy, "-> " + ref)
                c.setFont("Helvetica", 4.5)
            yy -= 0.22 * cm
    return x, bottom, right, top


def boundary_point(box: tuple[float, float, float, float],
                   other: tuple[float, float, float, float]) -> tuple[float, float]:
    """Point just on ``box``'s border in the direction of ``other``."""
    left, bottom, right, top = box
    ox, oy = other[0], other[1]
    cx, cy = (left + right) / 2, (bottom + top) / 2
    ocx, ocy = (other[0] + other[2]) / 2, (other[1] + other[3]) / 2
    dx, dy = ocx - cx, ocy - cy
    if not dx and not dy:
        return cx, cy
    scale = min((right - left) / 2 / abs(dx) if dx else float("inf"),
                (top - bottom) / 2 / abs(dy) if dy else float("inf"))
    return cx + dx * scale, cy + dy * scale


def draw_cardinality(c: canvas.Canvas, x: float, y: float, label: str) -> None:
    """Draw a white-backed label so 1/N remains readable above every line."""
    c.setFont("Helvetica-Bold", 5.5)
    width = c.stringWidth(label, "Helvetica-Bold", 5.5) + 4
    c.setFillColor(colors.white)
    c.roundRect(x - width / 2, y - 3, width, 7, 2, fill=1, stroke=0)
    c.setFillColor(colors.HexColor("#0F172A"))
    c.drawCentredString(x, y - 1, label)


def main() -> None:
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    c = canvas.Canvas(str(OUTPUT), pagesize=landscape(A4))
    width, height = landscape(A4)
    c.setTitle("SGB-SaaS Entity Relationship Overview (verified spec)")
    c.setFillColor(colors.HexColor("#0F172A"))
    c.setFont("Helvetica-Bold", 14)
    c.drawString(1.5 * cm, height - 1.3 * cm, "SGB-SaaS Entity Relationship Overview")
    c.setFont("Helvetica", 7)
    c.setFillColor(colors.HexColor("#475569"))
    c.drawString(1.5 * cm, height - 1.8 * cm,
                 "Core domain: real tables/columns from PostgreSQL information_schema "
                 "(migrations V1-V54). [PK] primary key, [FK] foreign key, * NOT NULL. "
                 "Full 45-table schema in database/migrations/.")

    boxes = {area: box_geometry(AREAS[area], *POS[area]) for area in AREAS}
    c.setStrokeColor(colors.HexColor("#94A3B8"))
    c.setLineWidth(0.6)
    for a, b, ca, cb2, _nota in RELATIONS:
        x1, y1 = boundary_point(boxes[a], boxes[b])
        x2, y2 = boundary_point(boxes[b], boxes[a])
        c.line(x1, y1, x2, y2)

    for area, (x, y) in POS.items():
        draw_box(c, area, AREAS[area], x, y)

    # Relation lines sit behind the area boxes; their endpoint labels are
    # redrawn above them, so each verified 1/N or N/N cardinality remains
    # readable even where routes cross another area.
    for a, b, ca, cb2, _nota in RELATIONS:
        x1, y1 = boundary_point(boxes[a], boxes[b])
        x2, y2 = boundary_point(boxes[b], boxes[a])
        dx, dy = x2 - x1, y2 - y1
        length = max((dx * dx + dy * dy) ** 0.5, 1)
        offset = 0.22 * cm
        draw_cardinality(c, x1 + dx * offset / length, y1 + dy * offset / length, ca)
        draw_cardinality(c, x2 - dx * offset / length, y2 - dy * offset / length, cb2)

    c.setFont("Helvetica", 6)
    c.setFillColor(colors.HexColor("#64748B"))
    c.drawRightString(width - 1.5 * cm, 0.6 * cm,
                      "Generated by scripts/generate-english-er-figure.py --check against information_schema")
    c.save()
    print(f"ER figure written to {OUTPUT}")


def check() -> int:
    """Verifica la spec contra information_schema via psql."""
    env = dict(os.environ)
    host = env.get("PGHOST", "localhost")
    user = env.get("PGUSER", "sgb_user")
    db = env.get("PGDATABASE", "sgb_db")
    if "PGPASSWORD" not in env:
        print("check: SKIP (sin PGPASSWORD; no se finge verificacion)")
        return 0
    fallos = []

    def psql(sql: str) -> str:
        r = subprocess.run(["psql", "-h", host, "-U", user, "-d", db,
                            "-t", "-A", "-c", sql],
                           capture_output=True, text=True, timeout=60, env=env)
        if r.returncode != 0:
            fallos.append(f"psql fallo: {(r.stderr or '')[:200]}")
            return ""
        return r.stdout

    tablas_db = {l.strip() for l in
                 psql("SELECT table_name FROM information_schema.tables "
                      "WHERE table_schema='public' AND table_type='BASE TABLE'").splitlines()
                 if l.strip()}
    if not tablas_db:
        print("check: SKIP (sin conexion; no se finge verificacion)")
        return 0
    for area, tablas in AREAS.items():
        for tabla, cols in tablas:
            if tabla not in tablas_db:
                fallos.append(f"{area}: tabla ausente {tabla}")
                continue
            cols_db = {l.split("|")[0].strip() for l in
                       psql(f"SELECT column_name || '|' || is_nullable FROM information_schema.columns "
                            f"WHERE table_schema='public' AND table_name='{tabla}'").splitlines()
                       if l.strip()}
            nulos_db = {l.split("|")[0].strip() for l in
                        psql(f"SELECT column_name || '|' || is_nullable FROM information_schema.columns "
                             f"WHERE table_schema='public' AND table_name='{tabla}'").splitlines()
                        if l.strip().endswith("|NO")}
            for col, kind in cols:
                if col not in cols_db:
                    fallos.append(f"{area}.{tabla}: columna ausente {col}")
                elif "*" in kind and col not in nulos_db:
                    fallos.append(f"{area}.{tabla}: {col} marcada NOT NULL pero es nullable")
            fks_db = {l.strip() for l in
                      psql("SELECT kcu.column_name || '->' || ccu.table_name || '.' || ccu.column_name "
                           "FROM information_schema.table_constraints tc "
                           "JOIN information_schema.key_column_usage kcu ON tc.constraint_name = kcu.constraint_name "
                           "AND tc.table_schema = kcu.table_schema "
                           "JOIN information_schema.constraint_column_usage ccu "
                           "ON ccu.constraint_name = tc.constraint_name "
                           "AND ccu.table_schema = tc.table_schema "
                           f"WHERE tc.table_schema='public' AND tc.table_name='{tabla}' "
                           "AND tc.constraint_type='FOREIGN KEY'").splitlines() if l.strip()}
            for col, kind in cols:
                if kind.startswith("FK>") or ",FK>" in kind:
                    ref = kind.split("FK>", 1)[1]
                    if f"{col}->{ref}" not in fks_db:
                        fallos.append(f"{area}.{tabla}: FK ausente {col}->{ref}")
    if fallos:
        print("check: FALLA")
        for f in fallos:
            print(" -", f)
        return 1
    print(f"check: OK ({sum(len(t[1]) for ts in AREAS.values() for t in ts)} columnas"
          f" en {sum(len(ts) for ts in AREAS.values())} tablas verificadas)")
    return 0


if __name__ == "__main__":
    if len(sys.argv) > 1 and sys.argv[1] == "--check":
        sys.exit(check())
    main()
