#!/usr/bin/env python3
"""Audita Javadoc en metodos publicos del backend Spring Boot.

El objetivo es producir una cifra reproducible para la rubrica E2. El
analizador es deliberadamente conservador: solo cuenta metodos publicos con
cuerpo o declaracion de interfaz cuando puede ver una firma Java completa, e
ignora constructores, clases de test y metodos generados por Lombok.
"""

from __future__ import annotations

import re
from dataclasses import dataclass
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "backend-springboot" / "src" / "main" / "java"

METHOD_RE = re.compile(
    r"(?P<javadoc>/\*\*.*?\*/\s*)?"
    r"(?P<annotations>(?:@\w+(?:\([^)]*\))?\s*)*)"
    r"public\s+"
    r"(?!class\b|interface\b|enum\b|record\b|@interface\b)"
    r"(?:static\s+|final\s+|default\s+|synchronized\s+)*"
    r"(?P<return>[\w<>\[\], ? extends super.&]+)\s+"
    r"(?P<name>[A-Za-z_][A-Za-z0-9_]*)\s*"
    r"\((?P<params>[^;{}()]*(?:\([^)]*\)[^;{}()]*)*)\)\s*"
    r"(?:throws\s+[\w.,\s]+)?\s*(?:\{|;)",
    re.DOTALL,
)

TYPE_RE = re.compile(r"\b(?:class|interface|enum|record)\s+([A-Za-z_][A-Za-z0-9_]*)")


@dataclass
class FileStats:
    path: Path
    public_methods: int = 0
    documented_methods: int = 0
    params: int = 0
    returns: int = 0
    throws: int = 0


def strip_comments_except_javadoc(text: str) -> str:
    text = re.sub(r"/\*(?!\*)(?:.|\n)*?\*/", "", text)
    text = re.sub(r"^[ \t]*//.*$", "", text, flags=re.MULTILINE)
    return text


def top_level_type_names(text: str) -> set[str]:
    return set(TYPE_RE.findall(text))


def audit_file(path: Path) -> FileStats:
    raw = path.read_text(encoding="utf-8", errors="ignore")
    text = strip_comments_except_javadoc(raw)
    type_names = top_level_type_names(text)
    stats = FileStats(path=path)

    for match in METHOD_RE.finditer(text):
        name = match.group("name")
        if name in type_names:
            continue

        stats.public_methods += 1
        javadoc = match.group("javadoc") or ""
        if javadoc.strip():
            stats.documented_methods += 1
            stats.params += len(re.findall(r"@param\b", javadoc))
            stats.returns += len(re.findall(r"@return\b", javadoc))
            stats.throws += len(re.findall(r"@throws\b", javadoc))

    return stats


def main() -> int:
    files = sorted(SRC.rglob("*.java"))
    stats = [audit_file(path) for path in files]
    total_methods = sum(item.public_methods for item in stats)
    documented = sum(item.documented_methods for item in stats)
    params = sum(item.params for item in stats)
    returns = sum(item.returns for item in stats)
    throws = sum(item.throws for item in stats)
    pct = (documented / total_methods * 100) if total_methods else 0.0

    print("Javadoc audit")
    print(f"source={SRC.relative_to(ROOT)}")
    print(f"java_files={len(files)}")
    print(f"public_methods={total_methods}")
    print(f"documented_methods={documented}")
    print(f"documented_pct={pct:.2f}")
    print(f"javadoc_param_tags={params}")
    print(f"javadoc_return_tags={returns}")
    print(f"javadoc_throws_tags={throws}")

    missing = [item for item in stats if item.public_methods > item.documented_methods]
    print(f"files_with_missing_javadocs={len(missing)}")
    for item in missing[:20]:
        rel = item.path.relative_to(ROOT)
        missing_count = item.public_methods - item.documented_methods
        print(f"missing {rel}: {missing_count}/{item.public_methods}")

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
