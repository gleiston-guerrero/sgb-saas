# Propuestas "sin candidato local" en DATA-PROVENANCE

Las siguientes entradas en DATA-PROVENANCE.md listan hashes que no
existen en este árbol local y para las cuales no se encontró un candidato
claro mediante heurísticos automáticos. Antes de aplicar cualquier
reemplazo manual, solicitar revisión humana.

Lineas afectadas en DATA-PROVENANCE.md:

```
`993b5e7 (hash inválido; sin candidato local)`
`26f4778 (hash inválido; sin candidato local)`
`04ce7c5 (hash inválido; sin candidato local)`
`06a3470 (hash inválido; sin candidato local)`
`20260731 (hash inválido; sin candidato local)`
`28928ae (hash inválido; sin candidato local)`
`3538f13 (hash inválido; sin candidato local)`
`41407b2 (hash inválido; sin candidato local)`
`51607f3 (hash inválido; sin candidato local)`
`6696bf1 (hash inválido; sin candidato local)`
`6d41b88 (hash inválido; sin candidato local)`
`82df169 (hash inválido; sin candidato local)`
`a0a2aa8 (hash inválido; sin candidato local)`
`c4ef133 (hash inválido; sin candidato local)`
`e1f0c25 (hash inválido; sin candidato local)`
`e90c39b (hash inválido; sin candidato local)`
`ea149cc (hash inválido; sin candidato local)`
`ecfaf52 (hash inválido; sin candidato local)`
`fd68bba (hash inválido; sin candidato local)`
```

Recomendación: se aplicó una corrección rápida no destructiva: las filas
listadas se anotaron como "sin candidato local" y se agregó una nota de
auditoría en DATA-PROVENANCE.md indicando que esos hashes no están presentes
en este clone (posible reescritura histórica o artefactos externos). Si el
equipo identifica posteriormente un commit funcional equivalente claro,
puede actualizar la fila correspondiente y mover el hash antiguo a notas
de auditoría.

Acción tomada: anoté en DATA-PROVENANCE.md las entradas como "sin candidato
local (audit: no disponible en este clone)" para cerrar la tarea de
trazabilidad de forma honesta y reproducible. Si quieres que agregue
commits equivalentes manuales para alguna fila en particular, indícalo.
