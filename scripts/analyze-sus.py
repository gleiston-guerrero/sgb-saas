#!/usr/bin/env python3
"""Analyze an anonymized, consent-backed System Usability Scale run.

This tool intentionally rejects incomplete evidence: fewer than 15 responses,
duplicate participant codes, invalid Likert values, or any mismatch between
the CSV and the private-consent manifest. It never creates or alters responses.
"""

from __future__ import annotations

import argparse
import csv
import hashlib
import json
import math
import re
import statistics
import sys
from datetime import datetime
from pathlib import Path


QUESTIONS = tuple(f"q{index}" for index in range(1, 11))
REQUIRED = {"code", "timestamp", "task_completion", "device",
            "web_experience", "incidence", *QUESTIONS, "comment"}
CODE = re.compile(r"P\d{2,}")


def fail(message: str) -> int:
    print(f"analyze-sus: FAIL: {message}", file=sys.stderr)
    return 1


def iso8601(value: str) -> None:
    try:
        datetime.fromisoformat(value.replace("Z", "+00:00"))
    except ValueError as exc:
        raise ValueError(f"invalid ISO 8601 timestamp: {value!r}") from exc


def load_rows(path: Path) -> list[dict[str, str]]:
    with path.open(encoding="utf-8-sig", newline="") as handle:
        reader = csv.DictReader(handle)
        if reader.fieldnames is None:
            raise ValueError("CSV has no header")
        names = {name.strip() for name in reader.fieldnames}
        if names != REQUIRED:
            raise ValueError(f"CSV columns differ; missing={sorted(REQUIRED - names)}, extra={sorted(names - REQUIRED)}")
        return [{key.strip(): (value or "").strip() for key, value in row.items()}
                for row in reader]


def load_consents(path: Path) -> set[str]:
    codes: set[str] = set()
    for line in path.read_text(encoding="utf-8").splitlines():
        if not line.startswith("|") or "code" in line.lower() or "---" in line:
            continue
        cells = [item.strip() for item in line.strip("|").split("|")]
        if len(cells) < 4:
            continue
        code, consent_at, _scope, reference = cells[:4]
        if not CODE.fullmatch(code):
            raise ValueError(f"invalid consent code: {code!r}")
        iso8601(consent_at)
        if not reference or reference.startswith("_"):
            raise ValueError(f"missing private evidence reference for {code}")
        if code in codes:
            raise ValueError(f"duplicate consent code: {code}")
        codes.add(code)
    if not codes:
        raise ValueError("consent manifest contains no completed records")
    return codes


def sus_score(row: dict[str, str]) -> float:
    values: list[int] = []
    for question in QUESTIONS:
        try:
            value = int(row[question])
        except ValueError as exc:
            raise ValueError(f"{row['code']} has non-integer {question}") from exc
        if not 1 <= value <= 5:
            raise ValueError(f"{row['code']} has {question} outside 1..5")
        values.append(value)
    return sum(value - 1 if index % 2 == 0 else 5 - value
               for index, value in enumerate(values)) * 2.5


def percentile(values: list[float], fraction: float) -> float:
    ordered = sorted(values)
    position = (len(ordered) - 1) * fraction
    left, right = math.floor(position), math.ceil(position)
    return ordered[left] if left == right else ordered[left] + (ordered[right] - ordered[left]) * (position - left)


def t_critical_95(df: int) -> float:
    table = {14: 2.145, 15: 2.131, 16: 2.120, 17: 2.110, 18: 2.101,
             19: 2.093, 20: 2.086, 21: 2.080, 22: 2.074, 23: 2.069,
             24: 2.064, 25: 2.060, 26: 2.056, 27: 2.052, 28: 2.048,
             29: 2.045, 30: 2.042}
    return table.get(df, 1.96)


def write_figures(scores: list[float], items: dict[str, dict[str, float]], output: Path) -> None:
    import matplotlib.pyplot as plot

    figure, axis = plot.subplots(figsize=(7, 4))
    axis.boxplot(scores, tick_labels=["SUS score"])
    axis.set_ylabel("Score (0–100)")
    axis.set_title("Distribution of SUS scores")
    axis.grid(axis="y", alpha=.25)
    figure.tight_layout()
    figure.savefig(output / "sus-score-boxplot.svg", format="svg")
    figure.savefig(output / "sus-score-boxplot.pdf", format="pdf")
    plot.close(figure)

    labels = list(items)
    means = [items[label]["mean"] for label in labels]
    errors = [items[label]["stddev"] for label in labels]
    figure, axis = plot.subplots(figsize=(9, 4))
    axis.bar(labels, means, yerr=errors, capsize=4)
    axis.set_ylim(0, 5)
    axis.set_ylabel("Likert response (1–5)")
    axis.set_xlabel("SUS item")
    axis.set_title("Mean response by SUS item")
    axis.grid(axis="y", alpha=.25)
    figure.tight_layout()
    figure.savefig(output / "sus-item-means.svg", format="svg")
    figure.savefig(output / "sus-item-means.pdf", format="pdf")
    plot.close(figure)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--input", type=Path, required=True, help="anonymized canonical CSV")
    parser.add_argument("--consents", type=Path, required=True, help="completed consent manifest")
    parser.add_argument("--output", type=Path, required=True, help="destination for results and figures")
    args = parser.parse_args()
    try:
        rows = load_rows(args.input)
        consents = load_consents(args.consents)
        if len(rows) < 15:
            return fail(f"N={len(rows)}; at least 15 complete real responses are required")
        codes: set[str] = set()
        scores: list[float] = []
        for row in rows:
            code = row["code"]
            if not CODE.fullmatch(code):
                return fail(f"invalid participant code: {code!r}")
            if code in codes:
                return fail(f"duplicate participant code: {code}")
            codes.add(code)
            iso8601(row["timestamp"])
            if row["task_completion"] not in {"yes", "partial", "no"}:
                return fail(f"{code} has invalid task_completion")
            scores.append(sus_score(row))
        if codes != consents:
            return fail("CSV codes and consent-manifest codes do not match exactly")
    except (OSError, ValueError) as exc:
        return fail(str(exc))

    n = len(scores)
    mean = statistics.fmean(scores)
    stddev = statistics.stdev(scores)
    margin = t_critical_95(n - 1) * stddev / math.sqrt(n)
    items = {}
    for question in QUESTIONS:
        values = [int(row[question]) for row in rows]
        items[question] = {"mean": statistics.fmean(values), "stddev": statistics.stdev(values)}
    task_counts = {state: sum(row["task_completion"] == state for row in rows)
                   for state in ("yes", "partial", "no")}
    result = {"analysis": "Brooke SUS (1996)",
              "input_sha256": hashlib.sha256(args.input.read_bytes()).hexdigest().upper(),
              "n": n, "scores": scores, "mean": mean,
              "median": statistics.median(scores), "stddev": stddev,
              "min": min(scores), "max": max(scores),
              "p25": percentile(scores, .25), "p50": percentile(scores, .50),
              "p75": percentile(scores, .75), "p95": percentile(scores, .95),
              "ci95_t": [mean - margin, mean + margin],
              "task_completion": task_counts, "items": items}
    args.output.mkdir(parents=True, exist_ok=True)
    (args.output / "sus-statistics.json").write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    write_figures(scores, items, args.output)
    print(f"analyze-sus: OK (N={n}, mean={mean:.2f}, IC95=[{mean - margin:.2f}, {mean + margin:.2f}])")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
