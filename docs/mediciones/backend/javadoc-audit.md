# Auditoria Javadoc del backend

Fecha de cierre: 2026-09-12

Esta evidencia respalda el punto E2 de la rubrica: comentarios que generan
documentacion. El conteo se obtiene desde el codigo versionado del backend, no
desde una cifra manual del informe.

## Comando reproducible

```powershell
python scripts\audit-javadocs.py
```

## Alcance

- Fuente auditada: `backend-springboot/src/main/java`
- Archivos Java auditados: 271
- Se excluyen tests, clases generadas y codigo fuera de `src/main/java`.
- El analizador es conservador: cuenta metodos publicos declarados con bloque
  Javadoc inmediatamente anterior.

## Resultado de cierre

```text
Javadoc audit
source=backend-springboot\src\main\java
java_files=271
public_methods=386
documented_methods=352
documented_pct=91.19
javadoc_param_tags=564
javadoc_return_tags=339
javadoc_throws_tags=80
files_with_missing_javadocs=9
missing backend-springboot\src\main\java\com\uteq\backend\config\RedisConfig.java: 4/7
missing backend-springboot\src\main\java\com\uteq\backend\controller\FineController.java: 2/3
missing backend-springboot\src\main\java\com\uteq\backend\controller\FullBackupController.java: 1/7
missing backend-springboot\src\main\java\com\uteq\backend\controller\LoanController.java: 19/23
missing backend-springboot\src\main\java\com\uteq\backend\controller\NotificationController.java: 1/1
missing backend-springboot\src\main\java\com\uteq\backend\controller\ReservationController.java: 1/5
missing backend-springboot\src\main\java\com\uteq\backend\controller\UserAdminController.java: 2/6
missing backend-springboot\src\main\java\com\uteq\backend\repository\FineProcedureRepositoryCustomImpl.java: 2/2
missing backend-springboot\src\main\java\com\uteq\backend\repository\LoanProcedureRepositoryCustomImpl.java: 2/2
```

## Interpretacion

El cierre documenta 352 de 386 metodos publicos con Javadoc, equivalente a
91,19 %. La evidencia deja visibles los 34 metodos pendientes por archivo para
que una auditoria posterior pueda reproducir el calculo y completar las brechas
sin reinterpretar la metrica.
