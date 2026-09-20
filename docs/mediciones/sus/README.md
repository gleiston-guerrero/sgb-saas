# SUS — System Usability Scale, corrida 2026-09-19

La evidencia vigente contiene $N=15$ respuestas reales, anónimas y completas
de una prueba voluntaria de usabilidad. La fuente privada tuvo 17 envíos:
dos se excluyeron antes del análisis por elegibilidad/trazabilidad, no por
su puntaje. El instrumento, el CSV anónimo, el manifiesto de
consentimientos y el cálculo están versionados en esta carpeta.

| Artefacto | Propósito |
|---|---|
| `instrumento/SUS-BROOKE-2026-09-19.md` | Enunciados, escala, tareas y tratamiento de datos del formulario aplicado. |
| `sus.csv` | 15 filas anónimas con fecha, tarea, dispositivo, experiencia, incidencia, Q1--Q10 y comentario. |
| `consents-manifest.md` | Cruce P01--P15 hacia consentimientos privados, sin PII pública. |
| `RAW-EXPORT-ATTESTATION.md` | Constancia pública de custodia y protocolo privado para contrastar el export original de 17 respuestas, sin publicar PII. |
| `sus-statistics.json` | Resultado regenerado desde el CSV, incluida su huella SHA-256. |
| `sus-score-boxplot.*`, `sus-item-means.*` | Figuras derivadas por `scripts/analyze-sus.py`. |

La media calculada es 66,00/100 con IC 95 % t de [56,95; 75,05]. Es un
resultado descriptivo de una muestra de conveniencia; no se generaliza a
una población y no se etiqueta como aceptable por encima de su evidencia.

El CSV exportado por Google Forms permanece privado porque contiene datos
identificables y consentimientos. Ante solicitud del docente, el custodio
puede mostrarlo en privado y contrastar tres códigos escogidos del
manifiesto con sus registros fuente; el procedimiento no entrega el archivo
ni expone identidades al público.

Para reproducir el análisis:

```powershell
python scripts/analyze-sus.py --input docs/mediciones/sus/sus.csv --consents docs/mediciones/sus/consents-manifest.md --output docs/mediciones/sus
python scripts/verify-p3-sus.py
```
