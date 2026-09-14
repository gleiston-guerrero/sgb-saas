# SUS — System Usability Scale (estado del dataset)

Resumen ejecutivo: el equipo no presenta evidencia SUS válida en este
entregable. El dataset `sus.csv` se conserva en el repositorio únicamente
como "dataset retirado" para auditoría del pipeline y validación metodológica,
pero NO se utiliza como evidencia empírica. El entregable declara formalmente
N = 0 para el bloque SUS en todas las secciones relevantes del informe.

Qué contiene esta carpeta:

- `sus.csv` — datos anonimizados derivados que se mantienen como referencia
  metodológica (dataset retirado). No son evidencia pública.
- `sus_boxplot.*`, `sus_items_breakdown.*` — figuras generadas a partir del
  dataset retirado; se conservan con fines metodológicos y no se citan como
  evidencia definitiva.
- `CONSENT.md` — política sobre consentimientos y por qué no se publican
  consentimientos individuales en este repositorio.

Por qué se retiró el dataset (resumen):
- Se detectaron patrones incompatibles con respuestas independientes (ítems
  con varianza cero, alternancia sistemática de sexo, patrón del dispositivo)
  y ausencia de trazabilidad hacia el commit de origen citado en la traza de
  procedencia. Estas inconsistencias impiden aceptar el dataset como
  evidencia reproducible.

Qué hacer si el equipo obtiene evidencia válida (Camino A):

1. Versionar el instrumento original (formato markdown o PDF) en
   `docs/mediciones/sus/instrumento/` con su propio manifiesto.
2. Versionar los consentimientos firmados en un deposito controlado o
   publicar solo un manifiesto con metadatos y referencia externa segura.
3. Actualizar `docs/mediciones/DATA-PROVENANCE.md` y levantar la nota de
   "dataset retirado" para declarar la nueva evidencias con N real.

Mientras no se cumplan esos pasos, el bloque SUS permanece en estado
"retirado — N=0" en todo el entregable.
