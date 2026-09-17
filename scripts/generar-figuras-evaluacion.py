#!/usr/bin/env python3
"""Genera las 9 figuras de evaluacion del informe (P8: 6 -> 15).

Fuentes UNICAMENTE versionadas en el repo (nada a mano):
  k6-run*.json (gitignored pero con SHA-256 registrado en REPORT.md),
  docs/mediciones/jacoco/report.csv, lighthouse/*.json,
  sec/zap/*ajax-full-report.json, git log (trazable por commit),
  @PreAuthorize del backend.

Salida: docs/mediciones/figuras/*.svg + *.pdf (matplotlib, sin latex).
Paleta Okabe-Ito (accesible a daltonismo, misma que p95 SVG).
Todo el texto dentro de las figuras va en ingles (rubrica P9).
Determinista: ordenes fijos, sin RNG (los histogramas usan bins fijos).

Uso: python3 scripts/generar-figuras-evaluacion.py
"""
import csv
import json
import subprocess
import sys
from collections import Counter, defaultdict
from datetime import datetime, timezone
from pathlib import Path


def a_epoch_ms(s):
    # k6 v2.x emite time ISO-8601 con nanosegundos; se trunca a microsegundos.
    if s.endswith("Z"):
        s = s[:-1] + "+00:00"
    m = __import__("re").sub(r"(\.\d{6})\d+", r"\1", s)
    return int(datetime.fromisoformat(m).timestamp() * 1000)

import matplotlib

matplotlib.use("Agg")
import matplotlib.pyplot as plt

# Okabe-Ito, mismo orden que p95-comparacion-escenarios.svg
NARANJA = "#E69F00"
CELESTE = "#56B4E9"
VERDE = "#009E73"
ROSA = "#CC79A7"
AZUL = "#0072B2"
AMARILLO = "#F0E442"
GRIS = "#999999"

ROOT = Path(__file__).resolve().parent.parent
FIG = ROOT / "docs" / "mediciones" / "figuras"
FIG.mkdir(parents=True, exist_ok=True)

plt.rcParams.update({"font.size": 10, "axes.titlesize": 11, "figure.dpi": 150})


def guardar(fig, nombre):
    for ext in ("svg", "pdf"):
        fig.savefig(FIG / f"{nombre}.{ext}", format=ext, bbox_inches="tight")
    plt.close(fig)
    print(f"OK docs/mediciones/figuras/{nombre}.svg + .pdf")


def leer_puntos_k6():
    """Devuelve {escenario: [(dur_ms, t_ms), ...]} solo metric http_req_duration."""
    datos = defaultdict(list)
    for path in sorted((ROOT / "docs" / "mediciones" / "perf").glob("k6-run*.json")):
        with open(path, encoding="utf-8") as fh:
            for linea in fh:
                linea = linea.strip()
                if not linea:
                    continue
                try:
                    obj = json.loads(linea)
                except json.JSONDecodeError:
                    continue
                if obj.get("type") != "Point" or obj.get("metric") != "http_req_duration":
                    continue
                tags = (obj.get("data") or {}).get("tags") or {}
                esc = tags.get("scenario")
                if esc not in ("cache_caliente", "cache_frio"):
                    continue
                datos[esc].append((float((obj["data"])["value"]), a_epoch_ms(obj["data"]["time"])))
    return datos


def fig_k6_distribucion(datos):
    fig, ax = plt.subplots(figsize=(7.2, 4.2))
    ax.hist([v for v, _ in datos["cache_caliente"]], bins=60, range=(0, 300),
            alpha=0.65, color=NARANJA, label="cache_hot (n=%d)" % len(datos["cache_caliente"]))
    ax.hist([v for v, _ in datos["cache_frio"]], bins=60, range=(0, 300),
            alpha=0.65, color=CELESTE, label="cache_cold (n=%d)" % len(datos["cache_frio"]))
    ax.set_xlabel("http_req_duration (ms)")
    ax.set_ylabel("requests")
    ax.set_title("Latency distribution: cache_hot vs cache_cold (5 runs)")
    ax.legend()
    guardar(fig, "fig-k6-distribucion-latencia")


def fig_k6_throughput(datos):
    corridas = sorted((ROOT / "docs" / "mediciones" / "perf").glob("k6-run*.json"))
    etiquetas, cal, fri = [], [], []
    for i, path in enumerate(corridas, 1):
        por_esc = defaultdict(list)
        with open(path, encoding="utf-8") as fh:
            for linea in fh:
                linea = linea.strip()
                if not linea:
                    continue
                try:
                    obj = json.loads(linea)
                except json.JSONDecodeError:
                    continue
                if obj.get("type") != "Point" or obj.get("metric") != "http_req_duration":
                    continue
                tags = (obj.get("data") or {}).get("tags") or {}
                esc = tags.get("scenario")
                if esc in ("cache_caliente", "cache_frio"):
                    por_esc[esc].append(a_epoch_ms(obj["data"]["time"]))
        etiquetas.append(f"run {i}")
        for lista, dest in ((por_esc["cache_caliente"], cal), (por_esc["cache_frio"], fri)):
            dest.append(len(lista) / ((max(lista) - min(lista)) / 1000.0) if len(lista) > 1 else 0.0)
    x = range(len(etiquetas))
    w = 0.38
    fig, ax = plt.subplots(figsize=(7.2, 4.2))
    ax.bar([i - w / 2 for i in x], cal, w, color=NARANJA, label="cache_hot")
    ax.bar([i + w / 2 for i in x], fri, w, color=CELESTE, label="cache_cold")
    ax.set_xticks(list(x), etiquetas)
    ax.set_ylabel("throughput (req/s)")
    ax.set_title("Throughput per run and scenario (GET /api/v1/libros)")
    ax.legend()
    guardar(fig, "fig-k6-throughput")


def fig_jacoco_paquetes():
    # Top 12 por CLASE (solo hay 5 paquetes: por paquete el "top 12"
    # mostraba 5 barras con título falso).
    clases = {}
    with open(ROOT / "docs" / "mediciones" / "jacoco" / "report.csv", encoding="utf-8") as fh:
        for fila in csv.DictReader(fh):
            clave = fila["PACKAGE"].split(".")[-1] + "." + fila["CLASS"]
            m, c = int(fila["INSTRUCTION_MISSED"]), int(fila["INSTRUCTION_COVERED"])
            anterior = clases.get(clave, [0, 0])
            clases[clave] = [anterior[0] + m, anterior[1] + c]
    items = sorted(((100 * c / (m + c), p, m + c) for p, (m, c) in clases.items() if m + c > 0),
                   reverse=True)[:12]
    nombres = [p for _, p, _ in reversed(items)]
    valores = [v for v, _, _ in reversed(items)]
    fig, ax = plt.subplots(figsize=(7.2, 4.6))
    barras = ax.barh(nombres, valores, color=CELESTE)
    ax.set_xlabel("instruction coverage (%)")
    ax.set_title("JaCoCo instruction coverage by class (top 12)")
    ax.set_xlim(0, 100)
    for b, v in zip(barras, valores):
        ax.text(v + 1, b.get_y() + b.get_height() / 2, f"{v:.0f}%", va="center", fontsize=9)
    guardar(fig, "fig-jacoco-paquetes")


def fig_lighthouse():
    # Serie canonica del capitulo: 6 corridas post-fix contra produccion
    # (18-ago-2026, 3 desktop + 3 mobile), mismas de tab:res-lighthouse-perfiles.
    import glob
    cats = ["performance", "accessibility", "best-practices", "seo"]
    vals = {"desktop": [], "mobile": []}
    for perfil, patron in (("desktop", "lhci-desktop-prod-20260818-*.json"),
                           ("mobile", "lhci-mobile-prod-20260818-*.json")):
        archivos = sorted(glob.glob(str(ROOT / "docs" / "mediciones" / "lighthouse" / patron)))
        assert len(archivos) == 3, f"{patron}: {len(archivos)} archivos"
        medias = []
        for c in cats:
            puntajes = []
            for a in archivos:
                d = json.load(open(a, encoding="utf-8"))
                puntajes.append(d["categories"][c]["score"] * 100)
            medias.append(sum(puntajes) / len(puntajes))
        vals[perfil] = medias
    print(f"lighthouse post-fix means: {vals}")
    x = range(len(cats))
    w = 0.38
    fig, ax = plt.subplots(figsize=(7.2, 4.2))
    b1 = ax.bar([i - w / 2 for i in x], vals["desktop"], w, color=AZUL, label="desktop (n=3)")
    b2 = ax.bar([i + w / 2 for i in x], vals["mobile"], w, color=NARANJA, label="mobile (n=3)")
    ax.set_xticks(list(x), ["Performance", "Accessibility", "Best practices", "SEO"])
    ax.set_ylabel("mean score (0-100)")
    ax.set_title("Lighthouse post-fix means: desktop vs mobile (prod, 2026-08-18)")
    ax.set_ylim(0, 115)
    for b in list(b1) + list(b2):
        ax.text(b.get_x() + b.get_width() / 2, b.get_height() + 1,
                f"{b.get_height():.1f}", ha="center", fontsize=9)
    ax.legend()
    guardar(fig, "fig-lighthouse-puntajes")


def fig_zap():
    d = json.load(open(ROOT / "docs" / "mediciones" / "sec" / "zap"
                       / "2026-08-17-zap-ajax-full-report.json", encoding="utf-8"))
    alertas = d["site"][1]["alerts"]
    orden = ["High", "Medium", "Low", "Informational"]
    conteo = Counter(a["riskdesc"].split(" ")[0] for a in alertas)
    colores = [ROSA, NARANJA, AMARILLO, CELESTE]
    fig, ax = plt.subplots(figsize=(7.2, 4.2))
    barras = ax.bar(orden, [conteo[o] for o in orden], color=colores)
    ax.set_ylabel("alerts")
    ax.set_title("ZAP AJAX spider alerts by risk (2026-08-17, 11 alerts)")
    for b in barras:
        ax.text(b.get_x() + b.get_width() / 2, b.get_height() + 0.05,
                str(int(b.get_height())), ha="center", fontsize=10)
    guardar(fig, "fig-zap-riesgos")


def git(*args):
    out = subprocess.run(["git", *args], cwd=ROOT, capture_output=True, text=True, check=True)
    return out.stdout


def fig_commits_semanales():
    fechas = git("log", "--no-merges", "--format=%ad", "--date=short", "HEAD").split()
    por_semana = Counter(f[:7] + "-S" + str((int(f[8:10]) - 1) // 7 + 1) for f in fechas)
    # Eje mensual simple y legible: agrega por mes
    por_mes = Counter(f[:7] for f in fechas)
    meses = sorted(por_mes)
    fig, ax = plt.subplots(figsize=(7.2, 4.2))
    ax.plot(meses, [por_mes[m] for m in meses], marker="o", color=AZUL)
    ax.set_xticks(meses[::1])
    plt.setp(ax.get_xticklabels(), rotation=45, ha="right", fontsize=8)
    ax.set_ylabel("commits (no merges)")
    ax.set_title("Commit activity per month (no merges)")
    guardar(fig, "fig-commits-mensuales")


def fig_commits_autores():
    out = git("-c", "log.mailmap=true", "shortlog", "-sne", "--no-merges", "HEAD")
    filas = []
    otros = 0
    for linea in out.splitlines():
        partes = linea.strip().split("\t")
        n = int(partes[0].strip())
        nombre = partes[1].split("<")[0].strip()
        if any(k in nombre for k in ("Cajas", "Loor", "Panama", "Panamá")):
            corto = {"Cajas": "I. Cajas", "Loor": "M. Loor"}.get(
                next(k for k in ("Cajas", "Loor", "Panama", "Panamá") if k in nombre),
                "M. Panama")
            filas.append((corto, n))
        else:
            otros += n
    filas.append(("Others", otros))
    filas.sort(key=lambda t: -t[1])
    fig, ax = plt.subplots(figsize=(7.2, 4.2))
    barras = ax.bar([n for n, _ in filas], [c for _, c in filas],
                    color=[AZUL, NARANJA, VERDE, GRIS][: len(filas)])
    ax.set_ylabel("commits (no merges, mailmap)")
    ax.set_title("Commits by author (no merges)")
    for b, (_, c) in zip(barras, filas):
        ax.text(b.get_x() + b.get_width() / 2, b.get_height() + 8, str(c),
                ha="center", fontsize=10)
    guardar(fig, "fig-commits-autores")


def fig_endpoints_roles():
    import re
    conteo = Counter()
    total = 0
    for path in (ROOT / "backend-springboot" / "src" / "main" / "java").rglob("*Controller.java"):
        texto = path.read_text(encoding="utf-8")
        for m in re.finditer(r"@PreAuthorize\(\"hasAnyRole\(([^)]*)\)\"\)", texto):
            total += 1
            for rol in re.findall(r"'(LECTOR|BIBLIOTECARIO|GERENTE|ADMIN)'", m.group(1)):
                conteo[rol] += 1
    orden = ["LECTOR", "BIBLIOTECARIO", "GERENTE", "ADMIN"]
    fig, ax = plt.subplots(figsize=(7.2, 4.2))
    barras = ax.bar(orden, [conteo[r] for r in orden], color=[GRIS, CELESTE, NARANJA, AZUL])
    ax.set_ylabel("@PreAuthorize annotations mentioning the role")
    ax.set_title(f"API authorization annotations per role ({total} method annotations)")
    for b in barras:
        ax.text(b.get_x() + b.get_width() / 2, b.get_height() + 0.3,
                str(int(b.get_height())), ha="center", fontsize=10)
    print(f"endpoints: {total} @PreAuthorize method annotations; {dict(conteo)}")
    guardar(fig, "fig-endpoints-roles")


def fig_migraciones():
    out = git("log", "--no-merges", "--diff-filter=A", "--format=%ad", "--date=short",
              "--", "database/migrations/V*.sql")
    fechas = sorted(out.split())
    xs, ys = [], []
    for i, f in enumerate(fechas, 1):
        xs.append(f)
        ys.append(i)
    fig, ax = plt.subplots(figsize=(7.2, 4.2))
    ax.step(xs, ys, where="post", color=VERDE, linewidth=2)
    paso = max(1, len(xs) // 8)
    ax.set_xticks(xs[::paso])
    plt.setp(ax.get_xticklabels(), rotation=45, ha="right", fontsize=8)
    ax.set_ylabel("cumulative versioned migrations")
    ax.set_title(f"Flyway migrations over time ({len(xs)} versioned V-files)")
    guardar(fig, "fig-migraciones-acumuladas")


def main():
    datos = leer_puntos_k6()
    print(f"k6: caliente n={len(datos['cache_caliente'])}, frio n={len(datos['cache_frio'])}")
    fig_k6_distribucion(datos)
    fig_k6_throughput(datos)
    fig_jacoco_paquetes()
    fig_lighthouse()
    fig_zap()
    fig_commits_semanales()
    fig_commits_autores()
    fig_endpoints_roles()
    fig_migraciones()
    print("MANIFIESTO: 9 figuras x (svg+pdf) en docs/mediciones/figuras/")


if __name__ == "__main__":
    sys.exit(main())
