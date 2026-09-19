#!/usr/bin/env python3
"""P10: la política productiva de refreshToken no puede perder Secure.

El perfil por defecto debe construir cookies Secure, HttpOnly y SameSite=None.
La única excepción permitida es el bean explícito ``dev-local-http``. Esto
convierte en fallo reproducible una regresión de ``secure(false)`` en prod.
"""

from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "backend-springboot" / "src" / "main" / "java" / "com" / "uteq" / "backend" / "config" / "RefreshCookieConfig.java"


def main() -> int:
    text = SOURCE.read_text(encoding="utf-8", errors="replace")
    production = re.search(r'@Profile\("!dev-local-http"\)(.*?)(?=@Bean\s+@Profile|\Z)', text, re.DOTALL)
    development = re.search(r'@Profile\("dev-local-http"\)(.*?)(?=\n\})', text, re.DOTALL)
    if not production or not development:
        print("verify-p10-cookie: FALLA: perfiles productivo/dev no encontrados", file=sys.stderr)
        return 1
    prod = production.group(1)
    dev = development.group(1)
    expected_prod = (".httpOnly(true)", ".secure(true)", '.sameSite("None")')
    if any(value not in prod for value in expected_prod) or ".secure(false)" in prod:
        print("verify-p10-cookie: FALLA: política productiva no es Secure+HttpOnly+SameSite=None", file=sys.stderr)
        return 1
    if ".secure(false)" not in dev or '.sameSite("Lax")' not in dev:
        print("verify-p10-cookie: FALLA: excepción dev-local-http no está aislada", file=sys.stderr)
        return 1
    print("verify-p10-cookie: OK (prod Secure+HttpOnly+SameSite=None; false solo dev-local-http)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
