# Consentimientos y acceso — SUS

Estado: no se incluyen consentimientos firmados en este repositorio por
razones de privacidad. El conjunto `docs/mediciones/sus/sus.csv` contiene
los datos anónimos agregados derivados del instrumento SUS usado en las
fechas indicadas; los valores personales identificables se mantienen
fuera del repositorio.

Procedimiento para publicar consentimientos (si el equipo lo autoriza):

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

Por ahora: el bloque SUS queda retirado (N=0) como evidencia pública
exigible — el instrumento y los datos agregados se conservan en
`docs/mediciones/sus/` como evidencia metodológica, pero no se publican
consentimientos individuales.

Motivo técnico de la retirada (resumen):
- Análisis del CSV `sus.csv` detectó señales incompatibles con respuestas
  independientes: tres ítems (Q8, Q9, Q10) con varianza exactamente cero;
  el campo `sexo` alterna sistemáticamente Femenino/Masculino en las 15
  filas; el campo `dispositivo` muestra un patrón repetitivo por paridad;
  además el commit de origen referido en la traza no existe en este árbol.
  Estas inconsistencias impiden aceptar estas respuestas como evidencia
  reproducible sin trazabilidad y consentimientos verificables.

Decisión tomada (no destructiva):
- NO se reescribió el historial Git para intentar restaurar hashes; ese
  enfoque fue descartado por riesgo a la integridad del repositorio.
- El dataset `docs/mediciones/sus/sus.csv` se conserva como "dataset
  retirado" para auditoría interna y validación del pipeline, pero el
  entregable declara formalmente N=0 y no utiliza los puntajes SUS como
  evidencia. Si el equipo aporta el instrumento original y los
  consentimientos verificables, se revertirá la retirada siguiendo el
  procedimiento documentado arriba.
