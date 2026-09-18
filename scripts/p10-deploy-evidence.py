#!/usr/bin/env python3
"""P10: evidencia real contra el despliegue (cuenta demo LECTOR).

Flujo: health -> login LECTOR (200) -> JWT decodificado localmente
(rol/roles) -> recurso solo-ADMIN (403) -> recurso ajeno (403).
NUNCA imprime ni guarda token, password ni cookies: la evidencia solo
contiene status, claims no sensibles y cuerpos de error sanitizados.

Cuenta demo LECTOR publicada en README.md (u@uteq.edu.ec); password por
entorno DEMO_LECTOR_PASSWORD o el valor documentado para demo.

Uso: python scripts/p10-deploy-evidence.py [--out docs/evidencia/...]
Sale 0 si login=200, rol=LECTOR y ambos 403; 1 en caso contrario.
"""

from __future__ import annotations

import base64
import datetime
import json
import os
import subprocess
import sys
import urllib.request
from pathlib import Path

BASE = os.environ.get("SGB_DEPLOY_URL", "https://sgb-backend-b058.onrender.com")
CORREO = "u@uteq.edu.ec"
PASSWORD = os.environ.get("DEMO_LECTOR_PASSWORD", "usuario1")


def llamada(metodo: str, ruta: str, token: str | None = None,
            cuerpo: dict | None = None) -> tuple[int, str]:
    data = json.dumps(cuerpo).encode() if cuerpo is not None else None
    peticion = urllib.request.Request(BASE + ruta, data=data, method=metodo,
                                      headers={"Content-Type": "application/json"})
    if token:
        # El token real viaja en este header; jamás se imprime ni se guarda.
        peticion.add_header("Authorization", "Bearer " + token)
    try:
        with urllib.request.urlopen(peticion, timeout=90) as respuesta:
            return respuesta.status, respuesta.read().decode("utf-8", "replace")[:400]
    except Exception as exc:  # noqa: BLE001 - HTTPError trae el status
        codigo = getattr(exc, "code", 0) or 0
        try:
            cuerpo_err = exc.read().decode("utf-8", "replace")[:400]  # type: ignore[union-attr]
        except Exception:
            cuerpo_err = ""
        return int(codigo), cuerpo_err


def claims(token: str) -> dict:
    payload = token.split(".")[1]
    datos = json.loads(base64.urlsafe_b64decode(payload + "=" * (-len(payload) % 4)))
    return {k: datos.get(k) for k in ("rol", "roles", "sub", "correo")}


def rev_corta() -> str:
    r = subprocess.run(["git", "rev-parse", "--short", "HEAD"],
                       capture_output=True, text=True,
                       cwd=Path(__file__).resolve().parents[1])
    return r.stdout.strip() or "<sin-git>"


def main() -> int:
    lineas: list[str] = []
    fallos = 0

    def check(nombre: str, ok: bool, detalle: str = "") -> None:
        nonlocal fallos
        lineas.append(f"[{'OK' if ok else 'FALLA'}] {nombre} {detalle}".rstrip())
        if not ok:
            fallos += 1

    st, _ = llamada("GET", "/actuator/health")
    check("health del despliegue", st == 200, f"HTTP {st}")

    st, cuerpo = llamada("POST", "/api/auth/login",
                         cuerpo={"correo": CORREO, "password": PASSWORD})
    check("login LECTOR", st == 200, f"HTTP {st}")
    if st != 200:
        print("\n".join(lineas))
        return 1
    token = json.loads(cuerpo)["accessToken"]
    partes = token.split(".")
    check("JWT con 3 partes", len(partes) == 3)
    decl = claims(token)
    lineas.append(f"claims: rol={decl.get('rol')} roles={decl.get('roles')} "
                  f"sub={decl.get('sub')} correo={decl.get('correo')}")
    check("rol LECTOR en claims", decl.get("rol") == "LECTOR"
          and "LECTOR" in (decl.get("roles") or []))

    st, cuerpo403a = llamada("GET", "/api/v1/admin/usuarios", token=token)
    check("recurso solo-ADMIN -> 403", st == 403, f"HTTP {st}")
    lineas.append(f"cuerpo 403 ADMIN: {cuerpo403a[:200]}")

    st, cuerpo403b = llamada("GET", "/api/v1/multas/usuario/1", token=token)
    check("recurso ajeno -> 403", st == 403, f"HTTP {st}")
    lineas.append(f"cuerpo 403 ajeno: {cuerpo403b[:200]}")

    if len(sys.argv) > 2 and sys.argv[1] == "--out":
        destino = Path(sys.argv[2])
        destino.parent.mkdir(parents=True, exist_ok=True)
        ahora = datetime.datetime.now(datetime.timezone.utc).astimezone().isoformat()
        destino.write_text(
            f"Fecha ISO real: {ahora}\nSHA: {rev_corta()}\n"
            f"URL: {BASE}\nCuenta: {CORREO} (demo LECTOR, password no registrada)\n\n"
            + "\n".join(lineas) + "\n",
            encoding="utf-8")
        lineas.append(f"evidencia: {destino}")
    print("\n".join(lineas))
    print("verify-p10-deploy: " + ("OK (login + LECTOR + doble 403)" if not fallos else "FALLA"))
    return 1 if fallos else 0


if __name__ == "__main__":
    sys.exit(main())
