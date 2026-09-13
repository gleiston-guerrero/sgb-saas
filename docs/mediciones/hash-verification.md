# Verificacion de hashes rotos (punto 6 de la rubrica)

Fecha: 2026-09-13. Rama: `fix/correciones`.

La revision del 2 de septiembre señalo tres hashes rotos mas un hash
faltante. Esta tabla los corrige uno por uno con comandos reproducibles.
Comando base (todos verificados con el):

```powershell
git cat-file -t <hash>          # el objeto existe en la base local
git branch --all --contains <hash>  # refs que lo contienen
```

## Tabla de correccion (las 4 partes)

| Parte | Pedido | Estado anterior | Valor vigente | Verificacion |
|---|---|---|---|---|
| (a) OBS-18 | `63ca63d` en OBS-18 | No existe: `fatal: Not a valid object name 63ca63d` (objeto previo a la reescritura del historial, ya sin referente) | `e946d48` (`fix(release): revert premature v1.0.0 tag and metadata`), citado en la fila OBS-18 de `docs/observaciones/OBSERVACIONES.md` | `git cat-file -t e946d48` -> `commit`; contenido en `fix/correciones` y otras ramas (`git branch --all --contains e946d48`) |
| (b) `5d404b6` en DATA-PROVENANCE | hash inexistente | No existe: `fatal: Not a valid object name 5d404b6`; ya no es citado por ninguna version vigente del archivo (la fila fue reescrita en `df09f0db fix(docs): restaurar hashes validos tras reescritura de historial (Punto 6)`) | Fila equivalente vigente con hashes verificables (ver auditoria completa abajo) | `Select-String -Pattern "5d404b6" docs/mediciones/DATA-PROVENANCE.md` -> sin resultados; todos los hashes actuales existen como objetos |
| (c) `c6de386` en DATA-PROVENANCE | hash inexistente | No existe: `fatal: Not a valid object name c6de386`; mismo caso que (b) | Fila equivalente vigente con hashes verificables | Idem (b) |
| (d) hash de OBS-13 | agregar | Agregado: `105807c` (`fix(tests): agrega Testcontainers Postgres para tests de integracion con SPs`), citado en la fila OBS-13 de `docs/observaciones/OBSERVACIONES.md` | `git cat-file -t 105807c` -> `commit`; contenido en ramas (`git branch --all --contains 105807c`) | Verificado 2026-09-13 |

## Auditoria completa de hashes citados (2026-09-13)

Se extrajeron con regex todos los hashes de 7-40 hex de
`docs/observaciones/OBSERVACIONES.md` (35) y
`docs/mediciones/DATA-PROVENANCE.md` (20, mas 1 falso positivo de fecha
`20260731` en un nombre de archivo lighthouse). Resultado:

- Los 55 objetos existen (`git cat-file -t` -> `commit`/`tag` segun caso).
- Los citados por las 4 partes (a)-(d) estan contenidos en refs vivas
  (incl. `fix/correciones`).
- Varios hashes de DATA-PROVENANCE corresponden a la historia previa a la
  reescritura y hoy solo existen como objetos no alcanzados por refs
  (`git branch --all --contains` vacio). Se conservan porque documentan el
  proceso real (corrida 1 -> corrida 5, etc.) y el propio archivo ya
  declara ese criterio: donde un hash historico ya no existe en el
  repositorio actual, se sustituye por el commit funcional equivalente
  mas cercano. Ninguno de ellos es parte de las 4 partes exigidas.

Script de re-verificacion (versionado, solo lectura):

```powershell
python scripts\check-hashes.py
```

Sale 0 si las 4 partes verifican. Replica esta tabla: existencia del
objeto (`git cat-file -t`), cita en el archivo y ausencia de los hashes
retirados, mas la auditoria completa de los 55 hashes citados.
