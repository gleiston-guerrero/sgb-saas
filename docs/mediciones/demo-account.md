# Cuenta demo LECTOR (punto 7 de la rubrica)

Fecha de verificacion: 2026-09-13. Rama: `fix/correciones`.

La revision original encontro la cuenta demo ofrecida como `LECTOR` pero
con token `GERENTE` y acceso a prestamos ajenos. Causa raiz: la migracion
`V12__fix_usuario_demo.sql` habia dejado el doble rol `LECTOR+GERENTE`.
La migracion `V42__fix_usuario_demo_roles_unicos.sql` deja la cuenta con
unicamente `LECTOR`, `ACTIVO` y verificado. Semilla:

- `database/migrations/V11__seed_usuario_demo.sql` (BCrypt de `usuario1`)
- `database/migrations/V12__fix_usuario_demo.sql`
- `database/migrations/V42__fix_usuario_demo_roles_unicos.sql`

## Parte (a): el token dice LECTOR

Test `DemoAccountMigrationIntegrationTest`
(`backend-springboot/src/test/java/com/uteq/backend/integration/`):
PostgreSQL 16 en Testcontainers + migraciones Flyway reales. Comprueba
`roles = [LECTOR]` en BD, cuenta activa/verificada, login con
`u@uteq.edu.ec` / `usuario1` y decodifica el JWT (`rol = LECTOR`,
`roles = [LECTOR]`).

Resultado 2026-09-13 (perfil `integration-tests`, Docker disponible):

```text
DemoAccountMigrationIntegrationTest: Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
```

Comando reproducible:

```powershell
cd backend-springboot
./mvnw -B test -Dtest=DemoPublicAccountsIntegrationTest
```

Notas:
- El test usa Testcontainers (requiere Docker en el host). Si Docker no
  está disponible, puede ejecutarse el mismo chequeo levantando una base
  PostgreSQL local y ejecutando `./mvnw -Dtest=DemoPublicAccountsIntegrationTest test`.
- La contraseña del usuario semilla es `usuario1` (documentada solo en README como credencial pública para evaluación).

## Parte (b): sin acceso a prestamos ajenos

Con un unico rol `LECTOR`, el control "propio vs cualquiera" aplica en
cada servicio (`validateAccessUser`: un LECTOR solo ve lo suyo, resto
403 `AuthorizationDeniedException`) y en `@PreAuthorize` de cada
controlador. Evidencia unitaria (suite `mvnw clean verify`: 584 tests,
0 fallos):

- `LoanServiceTest.listByUser_readerPideOtroUser` y
  `listActivesByUser_readerPideOtroUser` -> 403
- `ReservationServiceTest.listByUser_cuandoReaderPideOtroUser` y
  `changeStatus_readerTocaAjena` -> 403
- `NotificationServiceTest.listByUser_readerPideOtroUser` -> 403
- `*ControllerSecurityTest` (Loan, Reservation, Notification, etc.)
  verifican la matriz `@PreAuthorize` por rol
