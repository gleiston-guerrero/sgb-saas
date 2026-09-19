#!/usr/bin/env python3
"""Audita Javadoc completo de la API publica del backend Spring Boot.

La rubrica incluye tanto metodos ``public`` explicitos como las declaraciones
implicitamente publicas de interfaces y proyecciones.  Un metodo solo cuenta
como documentado cuando su Javadoc declara todos sus parametros y, si no
devuelve ``void``, su resultado.  Asi el porcentaje no puede subir por una
descripcion breve que omite el contrato de la firma.
"""

from __future__ import annotations

import re
from dataclasses import dataclass, field
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "backend-springboot" / "src" / "main" / "java"

NON_PAREN = r"(?:[^()]|\([^()]*\))*"
METHOD_RE = re.compile(
    r"(?P<javadoc>/\*\*.*?\*/\s*)?"
    r"(?P<annotations>(?:@\w+(?:\(" + NON_PAREN + r"\))?\s*)*)"
    r"public\s+"
    r"(?!class\b|interface\b|enum\b|record\b|@interface\b)"
    r"(?:static\s+|final\s+|default\s+|synchronized\s+)*"
    r"(?P<return>[\w<>\[\], ? extends super.&]+)\s+"
    r"(?P<name>[A-Za-z_][A-Za-z0-9_]*)\s*"
    r"\((?P<params>[^;{}()]*(?:\([^;{}()]*\)[^;{}()]*)*)\)\s*"
    r"(?:throws\s+[\w.,\s]+)?\s*(?:\{|;)",
    re.DOTALL,
)

TYPE_RE = re.compile(r"\b(?:class|interface|enum|record)\s+([A-Za-z_][A-Za-z0-9_]*)")
INTERFACE_RE = re.compile(r"\b(?:public\s+)?interface\s+[A-Za-z_][A-Za-z0-9_]*[^\{]*\{")
IMPLICIT_INTERFACE_METHOD_RE = re.compile(
    r"(?P<javadoc>/\*\*.*?\*/\s*)?"
    r"(?P<annotations>(?:@\w+(?:\(" + NON_PAREN + r"\))?\s*)*)"
    r"(?!(?:public|private|protected|default|static)\b)"
    r"(?P<return>[\w<>\[\], ? extends super.&]+)\s+"
    r"(?P<name>[A-Za-z_][A-Za-z0-9_]*)\s*"
    r"\((?P<params>[^;{}()]*(?:\([^;{}()]*\)[^;{}()]*)*)\)\s*"
    r"(?:throws\s+[\w.,\s]+)?\s*;",
    re.DOTALL,
)


@dataclass
class FileStats:
    path: Path
    public_methods: int = 0
    documented_methods: int = 0
    params: int = 0
    returns: int = 0
    throws: int = 0
    incomplete: list[str] = field(default_factory=list)


def strip_comments_except_javadoc(text: str) -> str:
    text = re.sub(r"/\*(?!\*)(?:.|\n)*?\*/", "", text)
    text = re.sub(r"^[ \t]*//.*$", "", text, flags=re.MULTILINE)
    return text


def top_level_type_names(text: str) -> set[str]:
    return set(TYPE_RE.findall(text))


def interface_ranges(text: str) -> list[tuple[int, int]]:
    """Return source ranges occupied by interface declarations.

    A small brace matcher is enough here: Java source has already had ordinary
    block comments removed, and the auditor only needs to know whether a
    semicolon declaration belongs to an interface.
    """
    ranges: list[tuple[int, int]] = []
    for match in INTERFACE_RE.finditer(text):
        start = match.end() - 1
        depth = 0
        for index in range(start, len(text)):
            if text[index] == "{":
                depth += 1
            elif text[index] == "}":
                depth -= 1
                if depth == 0:
                    ranges.append((start, index + 1))
                    break
    return ranges


def parameter_names(params: str) -> list[str]:
    if not params.strip():
        return []
    names: list[str] = []
    for fragment in params.split(","):
        fragment = re.sub(r"@\w+(?:\(" + NON_PAREN + r"\))?\s*", "", fragment)
        fragment = re.sub(r"\b(?:final|volatile)\b\s*", "", fragment)
        match = re.search(r"([A-Za-z_][A-Za-z0-9_]*)\s*(?:\[\])?\s*$", fragment)
        if match:
            names.append(match.group(1))
    return names


def incomplete_contract(javadoc: str, params: str, return_type: str) -> list[str]:
    if not javadoc.strip():
        return ["sin Javadoc"]
    documented_params = set(re.findall(r"@param\s+([A-Za-z_][A-Za-z0-9_]*)", javadoc))
    missing = [f"@param {name}" for name in parameter_names(params)
               if name not in documented_params]
    if return_type.strip() != "void" and not re.search(r"@return\b", javadoc):
        missing.append("@return")
    return missing


def audit_file(path: Path) -> FileStats:
    raw = path.read_text(encoding="utf-8", errors="ignore")
    text = strip_comments_except_javadoc(raw)
    type_names = top_level_type_names(text)
    stats = FileStats(path=path)

    ranges = interface_ranges(text)
    candidates = list(METHOD_RE.finditer(text))
    explicit_spans = {(match.start(), match.end()) for match in candidates}
    for match in IMPLICIT_INTERFACE_METHOD_RE.finditer(text):
        inside_interface = any(start <= match.start() < end for start, end in ranges)
        if inside_interface and (match.start(), match.end()) not in explicit_spans:
            candidates.append(match)

    for match in candidates:
        name = match.group("name")
        if name in type_names:
            continue

        stats.public_methods += 1
        javadoc = match.group("javadoc") or ""
        missing = incomplete_contract(javadoc, match.group("params"), match.group("return"))
        if javadoc.strip():
            stats.params += len(re.findall(r"@param\b", javadoc))
            stats.returns += len(re.findall(r"@return\b", javadoc))
            stats.throws += len(re.findall(r"@throws\b", javadoc))
        if not missing:
            stats.documented_methods += 1
        else:
            stats.incomplete.append(f"{name}: {', '.join(missing)}")

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
    print(f"files_with_incomplete_javadocs={len(missing)}")
    for item in missing[:30]:
        rel = item.path.relative_to(ROOT)
        missing_count = item.public_methods - item.documented_methods
        print(f"incomplete {rel}: {missing_count}/{item.public_methods}")
        for detail in item.incomplete[:8]:
            print(f"  - {detail}")

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
