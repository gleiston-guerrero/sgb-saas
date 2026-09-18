# SUS — System Usability Scale (estado del dataset)

Resumen ejecutivo: el equipo no presenta evidencia SUS válida en este
entregable. El bloque SUS queda en **N = 0** en todas las secciones
relevantes del informe. No se fabricará evidencia.

Qué contiene esta carpeta:

- `README.md` — este archivo (estado honesto del bloque).
- `CONSENT.md` — política de consentimientos y motivo técnico del retiro.

Qué NO contiene el árbol evaluado (retirado en Fase 0.1):

- No hay `sus.csv`: el dataset previo de 15 respuestas aparentes se
  retiró del árbol por falta de trazabilidad e indicios de no
  independencia (detalle en `CONSENT.md`). Permanece únicamente en el
  historial Git; el commit/tag evaluado no lo contiene.
- No hay figuras `sus_boxplot.*` ni `sus_items_breakdown.*`: eran
  derivados del dataset retirado.
- No hay instrumento versionado con respuestas ni consentimientos
  firmados publicables/verificables en el repositorio.

Por qué se retiró el dataset (resumen):

- Se detectaron patrones incompatibles con respuestas independientes
  (ítems con varianza cero, alternancia sistemática de sexo, patrón del
  dispositivo) y ausencia de trazabilidad hacia el commit de origen
  citado en la traza de procedencia. Estas inconsistencias impiden
  aceptar el dataset como evidencia reproducible.

Qué hacer si el equipo obtiene evidencia válida (Camino A):

1. Versionar el instrumento original (formato markdown o PDF) en
   `docs/mediciones/sus/instrumento/` con su propio manifiesto.
2. Versionar los consentimientos firmados en un depósito controlado o
   publicar solo un manifiesto con metadatos y referencia externa segura.
3. Actualizar `docs/mediciones/DATA-PROVENANCE.md` y este README con el
   N real de la corrida.
4. Ejecutar `scripts/sus-analysis.ipynb` sobre la corrida real.

Mientras no se cumplan esos pasos, el bloque SUS permanece en estado
"retirado — N=0" en todo el entregable.
