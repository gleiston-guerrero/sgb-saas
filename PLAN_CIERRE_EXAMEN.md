# Plan operativo de cierre -- examen SGB-SaaS

Este plan es para un agente que debe dejar la entrega verificable de punta a
punta. La guia del examen es la fuente de criterios; no es autorizacion para
inventar mediciones, cambiar autoria ni modificar el producto sin evidencia de
un defecto.

## Objetivo y regla de salida

La salida aceptable es un commit documentado y reproducible, evaluable desde el
tag `v1.1.0`, con los cuatro entregables minimos (EV-1 a EV-4) consistentes
entre si. No se declara ningun punto como cumplido hasta que el comando, dato o
artefacto correspondiente haya sido revisado en ese commit.

Estado conocido al iniciar: el HEAD de trabajo era `6549becb`, mientras que el
tag `v1.1.0` apuntaba a `545f8c8f` y quedaban 724 commits entre ambos. Esta es
la prioridad absoluta: el profesor evaluara el tag, no la rama activa.

## Limites de modificacion

El agente puede modificar:

- `VERIFICACION.md`, `CONTRIBUCIONES.md`, el informe LaTeX/Markdown y los
  documentos de mediciones, solo para reflejar evidencia real y fechada.
- `scripts/verify-*.py`, `Makefile` y scripts de apoyo, cuando el cambio haga
  la verificacion reproducible y no oculte un incumplimiento.
- Archivos de evidencia generados por ejecuciones reales: JSON/NDJSON de k6,
  JSON de Lighthouse, CSV de SUS, logs, hashes, PDF y SVG derivados.
- Codigo, pruebas o configuracion del producto exclusivamente cuando una
  ejecucion reproduzca un defecto que impida cumplir un criterio. Todo cambio
  de producto exige su prueba focalizada y una nueva medicion afectada.

El agente no puede:

- Mover, crear, borrar ni forzar el tag `v1.1.0` sin confirmacion expresa del
  responsable del repositorio. Puede preparar el commit candidato y reportar
  su SHA.
- Fabricar respuestas SUS, resultados de k6/Lighthouse/ZAP/JaCoCo, DOI,
  capturas, firmas o autoria; tampoco editar fechas para simular antiguedad.
- Reescribir historia, hacer `reset --hard`, `push --force`, mergear ramas,
  abrir PR ni empujar a `main`.
- Cambiar autor, correo Git, ni atribuir commits de otra persona. Las firmas
  CRediT se solicitan fuera del repositorio y se registran solo al recibirlas.
- Borrar datos o evidencia previa. Si una medicion queda obsoleta, se conserva
  y se marca como retirada, con el motivo y el reemplazo.

## Orden de ejecucion

### 1. Congelar el punto de partida

1. Confirmar `git status --short`, rama, `HEAD`, `git show-ref --tags v1.1.0`
   y `git log v1.1.0..HEAD --oneline`.
2. Confirmar `git config user.name` y `git config user.email` antes de pensar
   en commits. Si no existen, detenerse y pedir que el responsable los
   configure.
3. Crear una rama de cierre desde la base indicada por el equipo; no trabajar
   directamente sobre una rama ajena. Registrar SHA inicial y fecha en
   `VERIFICACION.md`.
4. Inspeccionar primero el grafo con `graphify query` o `graphify affected`;
   luego leer solo nodos relacionados. Tras editar codigo, ejecutar
   `graphify update .`.

Criterio: se conoce exactamente cual SHA sera candidato para el tag y no hay
cambios ajenos sin identificar.

### 2. Hacer que EV-2 sea ejecutable

1. Ejecutar el objetivo documentado `make verify` en un entorno con GNU Make.
   En Windows, usar una alternativa real instalada (por ejemplo Git Bash,
   WSL o `mingw32-make`), sin afirmar que PowerShell por si solo lo ejecuta.
2. Corregir el `Makefile` solo si hay un error de portabilidad o un paso no
   determinista. No eliminar controles que fallen.
3. Verificar que el objetivo cubra P1, P2, P6, P7, P12 y `javadoc:javadoc`, y
   que falle con codigo distinto de cero cuando falla cualquier subpaso.
4. Guardar en EV-1 el comando exacto, entorno, fecha, SHA y resultado.

Criterio: un clon limpio puede ejecutar una unica orden de verificacion y
obtener evidencia legible. Si no hay Make disponible, documentar el bloqueo y
los comandos equivalentes; no marcar EV-2 como plenamente cumplido.

### 3. Cerrar los puntos fuertes sin degradarlos

1. Ejecutar P1, P6, P7 y P12; registrar su salida y los artefactos revisados.
2. Ejecutar P2 con red y registrar para cada DOI URL final, HTTP y fecha. Si
   algun DOI falla, corregir la referencia o declararlo incumplido.
3. Ejecutar `backend-springboot\\mvnw.cmd -B javadoc:javadoc` y enlazar el
   resultado con P6.
4. Revisar P10: cuenta LECTOR publica, sin acceso a datos personales ni
   credenciales. Unificar el nombre inconsistente de la prueba en
   `docs/mediciones/demo-account.md` con el nombre que realmente exista.
5. Revisar P11: recalcular conteos desde commits verificables; incorporar
   firmas solo cuando esten recibidas y conservar evidencia de la solicitud si
   siguen pendientes.

Criterio: P1, P2, P6, P7, P10 y P12 conservan evidencia reproducible; P11 no
afirma firmas inexistentes.

### 4. Decidir honestamente P3, P4 y P5

P3 (SUS): hoy figura como N=0 y retirado. Hay dos opciones validas:

- Mantenerlo en cero, explicar su retiro y aceptar perder el puntaje.
- Recoger consentimiento real, datos anonimizados y CSV original; ejecutar
  recuento y calculo Brooke reproducible, sin editar respuestas.

P4 (k6): los cinco JSON locales existentes no estaban versionados, aunque el
informe los cita. Antes de reclamar el punto completo:

1. Validar que cada archivo sea resultado crudo legible y corresponda a la
   fecha, SHA, escenario y servidor declarados.
2. Versionar los datos crudos si la politica y el tamano lo permiten. Si no,
   versionar un manifiesto con hashes, instrucciones de obtencion y una
   ubicacion estable accesible al revisor; consultar al docente si la guia
   exige que queden en Git.
3. Ejecutar o repetir cinco corridas reales cuando la infraestructura este
   disponible. No mezclar series ni etiquetar una corrida nueva como historica.
4. Regenerar tablas, prueba estadistica, PDF/SVG y el informe desde los datos.

P5 (`nativeQuery`): ejecutar la busqueda/validador y clasificar cada uso. Para
los usos inevitables, adjuntar explicacion tecnica puntual y prueba; sustituir
por JPQL/Criteria solo si conserva rendimiento y semantica. No maquillar el
conteo ni renombrar texto para esquivar el validador.

Criterio: cada uno queda como cumplido con evidencia, o como parcial/no
cumplido declarado. La transparencia vale mas que una afirmacion fragil.

### 5. Regenerar P8, P9 y el informe

1. Compilar el informe desde un entorno limpio y conservar log de compilacion.
2. Revisar visualmente las seis figuras exigidas y sus leyendas: sin recortes,
   fuentes legibles, datos consistentes y sin imagenes de relleno.
3. Ejecutar el control de etiquetas en ingles de P9 sobre las figuras finales.
4. Actualizar referencias cruzadas, fecha, SHA y cifras en `VERIFICACION.md`.

Criterio: PDF final y fuentes son regenerables, con figuras y texto que no se
contradicen.

### 6. Verificacion integral y trazabilidad

1. Desde clon limpio, repetir `make verify`, compilacion de informe y los
   validadores de P3/P4/P5/P8/P9/P10/P11 que existan.
2. Ejecutar `make test`, `make audit` y `make bench` solo si el tiempo y la
   infraestructura lo permiten. Si se omiten, escribirlo literalmente en el
   alcance de verificacion: no reutilizar cifras antiguas como si se hubieran
   revalidado.
3. Revisar secretos en todo el arbol y confirmar CORS sin comodines y
   `@EnableMethodSecurity` con archivo y linea.
4. Revisar cada afirmacion de EV-1 contra su archivo fuente; eliminar las que
   no se puedan demostrar.
5. Actualizar `CONTRIBUCIONES.md` con conteos recalculados para el SHA final y
   dejar claras las firmas pendientes.

Criterio: EV-1 describe exactamente lo ejecutado y lo no ejecutado, al estilo
del informe docente; ninguna cifra carece de origen.

### 7. Empaquetar y pedir el tag

1. Hacer commits pequenos con formato conventional commits y cuerpo
   explicativo. Separar: correcciones de producto, evidencia generada,
   verificadores y documento final.
2. Ejecutar por ultima vez `git status --short`, `git diff --check`, los
   comandos de verificacion y registrar resultados.
3. Comunicar al responsable: SHA candidato, lista de verificaciones ejecutadas,
   omisiones, riesgos y solicitud explicita para mover/crear `v1.1.0` en ese
   SHA.
4. Solo despues de autorizacion, el responsable mueve el tag y confirma con
   `git show v1.1.0` que resuelve al SHA candidato. El agente vuelve a ejecutar
   el control minimo desde el tag.

Criterio final: `v1.1.0` resuelve al commit que contiene la evidencia final,
el arbol esta limpio y EV-1/EV-2/EV-3/EV-4 coinciden.

## Formato obligatorio del informe de cierre

El agente debe entregar al responsable, en texto breve:

- SHA candidato y estado exacto de `v1.1.0`.
- Tabla P1--P12: cumplido/parcial/no cumplido, evidencia y comando ejecutado.
- Que se ejecutó y que no se ejecutó, con motivo verificable.
- Archivos modificados, total exacto de pruebas y resultado de compilacion.
- Riesgos que requieren decision humana: tag, firmas, consentimiento SUS,
  alojamiento de JSON crudos y cualquier credencial de despliegue.
