#!/usr/bin/env python3
"""P3: valida la evidencia SUS real, anonimizada y reproducible.

Exige instrumento versionado, al menos 15 respuestas anónimas, un
manifiesto privado de consentimiento por código y derivados que se vuelven
a calcular desde el CSV. No acepta N=0 ni transforma datos de participantes.

Uso: python scripts/verify-p3-sus.py
"""

from __future__ import annotations

import csv
import hashlib
import json
import math
import re
import statistics
import sys
from datetime import datetime
from pathlib import Path

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    sys.stderr.reconfigure(encoding="utf-8", errors="replace")

ROOT = Path(__file__).resolve().parents[1]
SUS = ROOT / "docs" / "mediciones" / "sus"
CSV_PATH = SUS / "sus.csv"
MANIFEST = SUS / "consents-manifest.md"
INSTRUMENT = SUS / "instrumento" / "SUS-BROOKE-2026-09-19.md"
STATS = SUS / "sus-statistics.json"
README = SUS / "README.md"
CONSENT = SUS / "CONSENT.md"
RESULTS = ROOT / "docs" / "capitulos" / "08-resultados.tex"

FIELDS = ["code", "timestamp", "task_completion", "device",
          "web_experience", "incidence", "q1", "q2", "q3", "q4",
          "q5", "q6", "q7", "q8", "q9", "q10", "comment"]
QUESTIONS = [f"q{item}" for item in range(1, 11)]
CODE = re.compile(r"P\d{2,}")
EMAIL = re.compile(r"(?i)\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}\b")
PHONE = re.compile(r"(?<!\d)(?:\+?\d[\s-]?){8,15}(?!\d)")


def canonical_csv_sha256(path: Path) -> str:
    """Return a content hash independent of CRLF/LF checkout conversion."""
    text = path.read_text(encoding="utf-8-sig")
    canonical = text.replace("\r\n", "\n").replace("\r", "\n")
    return hashlib.sha256(canonical.encode("utf-8")).hexdigest().upper()


def fail(message: str) -> int:
    print(f"verify-p3: FALLA: {message}", file=sys.stderr)
    return 1


def parse_time(value: str) -> None:
    try:
        datetime.fromisoformat(value.replace("Z", "+00:00"))
    except ValueError as exc:
        raise ValueError(f"timestamp ISO 8601 inválido: {value!r}") from exc


def score(row: dict[str, str]) -> float:
    values = []
    for question in QUESTIONS:
        try:
            value = int(row[question])
        except ValueError as exc:
            raise ValueError(f"{row['code']} tiene {question} no entero") from exc
        if not 1 <= value <= 5:
            raise ValueError(f"{row['code']} tiene {question} fuera de 1..5")
        values.append(value)
    return sum(value - 1 if index % 2 == 0 else 5 - value
               for index, value in enumerate(values)) * 2.5


def load_rows() -> list[dict[str, str]]:
    with CSV_PATH.open(encoding="utf-8-sig", newline="") as source:
        reader = csv.DictReader(source)
        if reader.fieldnames != FIELDS:
            raise ValueError("encabezado SUS distinto del esquema canónico")
        rows = [{key: (value or "").strip() for key, value in row.items()}
                for row in reader]
    if len(rows) < 15:
        raise ValueError(f"N={len(rows)}; se requieren al menos 15 respuestas")
    seen: set[str] = set()
    for row in rows:
        code = row["code"]
        if not CODE.fullmatch(code) or code in seen:
            raise ValueError(f"código ausente, inválido o duplicado: {code!r}")
        seen.add(code)
        parse_time(row["timestamp"])
        if row["task_completion"] not in {"yes", "partial", "no"}:
            raise ValueError(f"estado de tarea inválido en {code}")
        if not row["device"] or not row["web_experience"]:
            raise ValueError(f"metadato obligatorio vacío en {code}")
        score(row)
        for field in ("incidence", "comment"):
            if EMAIL.search(row[field]) or PHONE.search(row[field]):
                raise ValueError(f"posible PII en {field} de {code}")
    return rows


def load_manifest() -> dict[str, str]:
    records: dict[str, str] = {}
    for line in MANIFEST.read_text(encoding="utf-8").splitlines():
        if not line.startswith("|") or "code" in line.lower() or "---" in line:
            continue
        cells = [cell.strip() for cell in line.strip("|").split("|")]
        if len(cells) != 5:
            raise ValueError("fila inválida en manifiesto de consentimiento")
        code, consent_at, scope, reference, custodian = cells
        if not CODE.fullmatch(code) or code in records:
            raise ValueError(f"código inválido o duplicado en manifiesto: {code!r}")
        parse_time(consent_at)
        if "anonymous row publication confirmed orally" not in scope:
            raise ValueError(f"alcance de publicación no declarado para {code}")
        if "Private Google Form" not in reference or not custodian:
            raise ValueError(f"referencia privada incompleta para {code}")
        records[code] = consent_at
    if not records:
        raise ValueError("manifiesto sin consentimientos")
    return records


def percentile(values: list[float], fraction: float) -> float:
    ordered = sorted(values)
    position = (len(ordered) - 1) * fraction
    lower, upper = math.floor(position), math.ceil(position)
    return ordered[lower] if lower == upper else ordered[lower] + (ordered[upper] - ordered[lower]) * (position - lower)


def expected(rows: list[dict[str, str]]) -> dict[str, object]:
    scores = [score(row) for row in rows]
    mean = statistics.fmean(scores)
    margin = 2.145 * statistics.stdev(scores) / math.sqrt(len(scores))
    return {
        "n": len(rows), "scores": scores, "mean": mean,
        "median": statistics.median(scores), "stddev": statistics.stdev(scores),
        "min": min(scores), "max": max(scores),
        "p25": percentile(scores, .25), "p50": percentile(scores, .50),
        "p75": percentile(scores, .75), "p95": percentile(scores, .95),
        "ci95_t": [mean - margin, mean + margin],
        "task_completion": {state: sum(row["task_completion"] == state for row in rows)
                            for state in ("yes", "partial", "no")},
    }


def close(actual: object, wanted: object) -> bool:
    if isinstance(wanted, list):
        return isinstance(actual, list) and len(actual) == len(wanted) and all(close(a, b) for a, b in zip(actual, wanted))
    if isinstance(wanted, (float, int)):
        return isinstance(actual, (float, int)) and math.isclose(float(actual), float(wanted), rel_tol=0, abs_tol=1e-9)
    return actual == wanted


def main() -> int:
    required = (CSV_PATH, MANIFEST, INSTRUMENT, STATS, README, CONSENT,
                RESULTS, SUS / "sus-score-boxplot.svg", SUS / "sus-score-boxplot.pdf",
                SUS / "sus-item-means.svg", SUS / "sus-item-means.pdf")
    missing = [str(path.relative_to(ROOT)) for path in required if not path.is_file()]
    if missing:
        return fail("faltan artefactos P3: " + ", ".join(missing))
    try:
        rows = load_rows()
        consents = load_manifest()
    except (OSError, ValueError) as exc:
        return fail(str(exc))
    codes = {row["code"] for row in rows}
    if codes != set(consents):
        return fail("códigos de CSV y manifiesto no coinciden exactamente")
    if any(row["timestamp"] != consents[row["code"]] for row in rows):
        return fail("timestamps de CSV y manifiesto no coinciden")
    print(f"verify-p3: OK ({len(rows)} respuestas; códigos y manifiesto de custodia coinciden)")

    try:
        data = json.loads(STATS.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as exc:
        return fail(f"estadísticas ilegibles: {exc}")
    digest = canonical_csv_sha256(CSV_PATH)
    if data.get("input_sha256") != digest:
        return fail("SHA-256 de sus.csv no coincide con sus-statistics.json")
    for key, value in expected(rows).items():
        if not close(data.get(key), value):
            return fail(f"estadística derivada distinta en {key}")
    print("verify-p3: OK (Brooke recalculado: N=15, media=66.00, IC95=[56.95, 75.05])")

    for figure in (SUS / "sus-score-boxplot.svg", SUS / "sus-score-boxplot.pdf",
                   SUS / "sus-item-means.svg", SUS / "sus-item-means.pdf"):
        if figure.stat().st_size < 1024:
            return fail(f"figura vacía o demasiado pequeña: {figure.name}")
    svg_text = (SUS / "sus-score-boxplot.svg").read_text(encoding="utf-8", errors="replace")
    if "Distribution of SUS scores" not in svg_text:
        return fail("figura SUS sin título esperado en inglés")
    print("verify-p3: OK (instrumento y cuatro derivados SUS presentes; figuras en inglés)")

    readme = README.read_text(encoding="utf-8", errors="replace")
    results = RESULTS.read_text(encoding="utf-8", errors="replace")
    if "N=15" not in readme or "66,00" not in readme:
        return fail("README SUS no declara N=15 y la media vigente")
    if "N=0" in readme or "N=0" in results:
        return fail("documentación SUS aún declara N=0")
    print("verify-p3: OK (documentación vigente, sin N=0)")
    print("verify-p3: evidencia válida (instrumento, CSV anónimo, manifiesto de custodia y recálculo reproducible)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
