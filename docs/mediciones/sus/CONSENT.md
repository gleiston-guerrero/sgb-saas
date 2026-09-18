# Consentimientos y acceso — SUS

Estado: bloque SUS en **N = 0**. No se incluyen consentimientos
firmados ni respuestas en este repositorio: no existe una corrida
válida que respaldar.

No hay dataset SUS en el árbol evaluado: el `sus.csv` previo (15 filas
aparentes) se retiró del árbol en Fase 0.1 por los motivos técnicos de
abajo. Solo permanece en el historial Git; el commit/tag evaluado no lo
contiene. Tampoco se conservan sus figuras derivadas (`sus_boxplot.*`,
`sus_items_breakdown.*`).

Procedimiento para una futura corrida válida (si el equipo la ejecuta):

1. Obtener consentimiento explícito firmado por cada participante para
   publicación (escaneo PDF o firma electrónica) y almacenar los archivos
   en un repositorio de artefactos privado/controlado (no en el repo
   público) o en un ZIP cifrado con clave gestionada por el equipo.
2. Publicar en este repositorio solo un manifiesto (`docs/mediciones/sus/consents-manifest.md`)
   que contenga metadatos: `codigo` (ID de participante), fecha de firma,
   alcance del consentimiento (p. ej. publicación agregada, reuso), y
   referencia al artefacto depositado externamente (URL segura o DOI).
3. Incluir en DATA-PROVENANCE.md la traza hacia el manifiesto y la política
   de acceso a los consentimientos.

Motivo técnico del retiro (registro de auditoría, se conserva como
constancia histórica):

- Análisis del CSV retirado detectó señales incompatibles con respuestas
  independientes: tres ítems (Q8, Q9, Q10) con varianza exactamente cero;
  el campo `sexo` alterna sistemáticamente Femenino/Masculino en las 15
  filas; el campo `dispositivo` muestra un patrón repetitivo por paridad;
  además el commit de origen referido en la traza no existe en este árbol.
  Estas inconsistencias impiden aceptar estas respuestas como evidencia
  reproducible sin trazabilidad y consentimientos verificables.

Decisión tomada:

- NO se reescribió el historial Git (permanece como estaba, por
  integridad del repositorio).
- El dataset retirado NO forma parte del árbol evaluado; el entregable
  declara formalmente N=0 y no utiliza puntajes SUS como evidencia. Si el
  equipo aporta el instrumento original y los consentimientos
  verificables, se revertirá la retirada siguiendo el procedimiento
  documentado arriba.
