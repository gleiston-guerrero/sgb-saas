#!/usr/bin/env bash
set -euo pipefail

MD_FILE="docs/mediciones/DATA-PROVENANCE.md"
OUT="docs/mediciones/hashes-verification-report.md"

echo "# Hashes verification report" > "$OUT"
echo "Source: $MD_FILE" >> "$OUT"
echo "" >> "$OUT"

if [ ! -f "$MD_FILE" ]; then
  echo "Data provenance file not found: $MD_FILE" >> "$OUT"
  exit 1
fi

grep -oE '\b[0-9a-f]{7,40}\b' "$MD_FILE" | sort -u | while read -r h; do
  # Try to resolve hash to any object
  if git rev-parse --verify --quiet "$h"^0 >/dev/null 2>&1; then
    echo "- $h: OK" >> "$OUT"
  else
    echo "- $h: MISSING" >> "$OUT"
  fi
done

echo "Report written to $OUT"
