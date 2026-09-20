# Instrumento aplicado — SUS SGB-SaaS, 2026-09-19

## Propósito y privacidad

Se aplicó una prueba voluntaria de usabilidad de SGB-SaaS. Antes de
enviar el formulario, cada persona confirmó tener 18 años o más, aceptó
el consentimiento y escribió una firma electrónica. El formulario
privado recogió nombre y correo; esos datos no están en `sus.csv` ni en
ningún archivo público. La copia pública asigna códigos P01--P15 por el
orden estable de los registros y conserva únicamente datos anonimizados.

## Tareas indicadas

1. Abrir el despliegue público de SGB-SaaS.
2. Iniciar sesión con la cuenta demo de lector indicada por el formulario.
3. Buscar un libro o recurso del catálogo y revisar su disponibilidad.
4. Abrir el detalle y localizar la opción de reserva.
5. Navegar por las opciones principales y cerrar sesión.

Después se recogieron: cumplimiento de tareas (`Sí`, `Parcialmente` o
`No`), dispositivo, experiencia usando sistemas web y una incidencia o
dificultad opcional.

## Escala y enunciados

El formulario presentó los valores obligatorios 1--5 con esta escala:
1 = Totalmente en desacuerdo; 2 = En desacuerdo; 3 = Neutral;
4 = De acuerdo; 5 = Totalmente de acuerdo.

| Campo | Enunciado |
|---|---|
| Q1 | Creo que me gustaría utilizar este sistema con frecuencia. |
| Q2 | Encontré el sistema innecesariamente complejo. |
| Q3 | Pensé que el sistema era fácil de usar. |
| Q4 | Creo que necesitaría el apoyo de una persona técnica para poder utilizar este sistema. |
| Q5 | Encontré las diversas funciones del sistema bien integradas. |
| Q6 | Pensé que había demasiada inconsistencia en el sistema. |
| Q7 | Imagino que la mayoría de las personas aprendería a utilizar este sistema rápidamente. |
| Q8 | Encontré el sistema muy engorroso de utilizar. |
| Q9 | Me sentí muy seguro al utilizar el sistema. |
| Q10 | Necesité aprender muchas cosas antes de poder usar el sistema. |

El puntaje se recalcula con Brooke (1996): para ítems impares se usa
`valor - 1`, para pares `5 - valor`, y la suma se multiplica por 2,5.
El script `scripts/analyze-sus.py` es la implementación ejecutable de
esta regla.
