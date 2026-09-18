# P5 — Matriz 33/33 de `nativeQuery = true` (Fase P5, sin migración aún)

Rev: rama `fix/fase01-sus-n0`. Inventario verificado con
`python scripts/verify-p5-nativequery.py` → **33 = 23 rutinas + 10 ordinarias**.
Este documento NO migra nada: decide la estrategia por caso y exige prueba
técnica real (PostgreSQL local, sin mocks) antes de cada migración.

Restricciones duras del proyecto (ver código real, no asumir):

- Entidades sin relaciones JPA a propósito (Loan/Reservation/Fine:
  FK planas). JPQL no puede hacer JOIN por relación donde no existe.
- Bug PG + parámetro NULL en JPQL (`could not determine data type`):
  filtros opcionales exigen Criteria con predicados dinámicos o métodos
  separados — nunca `(:p IS NULL OR ...)` en JPQL.
- `ProcedureMappingContractTest` (test vigente) exige `@Procedure` en las
  5 rutinas con efectos secundarios y asserts que las tabulares sean
  `@Query` nativas: **ese test se reescribe junto con la migración**
  (codifica el diseño anterior, no la rúbrica externa).
- `AuditService` construye `Sort.by("fecha_hora")` (columna física): pasar
  a JPQL exige cambiar el Sort del llamador a la propiedad (`dateTime`).
- `searchPendientes` (BookRepository) no tiene llamadores en `src/main`.

Convenciones: RUTINA = invoca `fn_*`/`sp_*`; ORDINARIA = SELECT plano.
"Contrato" = DTO/proyección + orden + paginación + error observable.

## A. Rutinas con efectos secundarios (5 CALL + 1 FUNCTION) → `@Procedure` real posicional

El spike previo demostró que la ruta posicional alcanza el motor (error
de negocio LB404, no de sintaxis). Estrategia: `em.createStoredProcedureQuery`
o `@NamedStoredProcedureQuery` con binding **posicional**, OUT leídos por
índice; los 5 `createNativeQuery("CALL ...")` de los `*CustomImpl`
desaparecen. Las anotaciones `@Procedure`/`@NamedStoredProcedureQuery`
vigentes se conservan (contract test sigue verde).

| # | Método | Rutina (V51 wrapper) | Params | Retorno | Consumidor | Test actual |
|---|---|---|---|---|---|---|
| R-S1 | `LoanProcedureRepositoryCustomImpl.spCreateLoanProcedure` | `proc_crear_prestamo` (IN×4 + OUT) | userId, bookId, librarianId, daysLoan | Long (id préstamo) | `LoanService:121` | `LoanFineProcedureIntegrationTest` |
| R-S2 | `...CustomImpl.spRegisterLoanReturn` | `proc_registrar_devolucion` (IN + 3 OUT) | loanId | Map o_prestamo_id/o_hubo_multa/o_monto_multa | `LoanService:190`, `LoanReturnService:119` | `LoanFineProcedureIntegrationTest` |
| R-S3 | `FineProcedureRepositoryCustomImpl.spPayFineProcedure` | `proc_pagar_multa` (IN + 2 OUT) | fineId | Map o_multa_id/o_usuario_desbloqueado | `FineService:145` | mocks en `FineControllerTest` (insuficiente) |
| R-S4 | `...CustomImpl.spVoidFineProcedure` | `proc_anular_multa` (IN×3 + 2 OUT) | fineId, reason, roleExecutor | Map o_multa_id/o_usuario_desbloqueado | `FineService:165` | mocks (insuficiente) |
| R-S5 | `ReservationProcedureRepositoryCustomImpl.spExpireReservationsVencidasProcedure` | `proc_expirar_reservaciones_vencidas` (IN now + OUT) | — | Integer expiradas | `ReservationScheduler:73` | revisar |
| R-S6 | `FineProcedureRepository.spPaymentParcialFine` | `sp_pago_parcial_multa` (FUNCTION, 4 OUT, V16) | fineId, amountPaid | Map o_multa_id/o_estado/o_saldo/o_desbloqueo | `FineService:132` | solo mocks (declarado en el archivo) |

R-S6 necesita **nueva migración versionada** (siguiente V disponible,
nunca editar aplicadas): wrapper `PROCEDURE proc_pago_parcial_multa`
estilo V51 + `@Procedure` posicional. Sin wrapper no hay vía JPA.

## B. Funciones tabulares de reportes (17 sites, 8 funciones) → reimplementación JPQL/Criteria + prueba de equivalencia

Las `fn_* RETURNS TABLE` no son mapeables por `@Procedure`/`CallableStatement`
(solo escalar/OUT o REF_CURSOR). Reescribirlas a REF_CURSOR rompería el
contrato SQL público (`SELECT * FROM fn_...` desde psql). Estrategia:
JPQL/Criteria que replica el cuerpo de la función (leer cada cuerpo en
`db/procs/` + `R__stored_procedures.sql`), misma proyección/orden/paginación,
con prueba de equivalencia fila a fila contra la función en PG real. Las
funciones quedan en BD (compat psql) pero JPA deja de invocarlas.

| # | Método | Función | Proyección | Consumidor |
|---|---|---|---|---|
| R-T1 | `fnListLoansActivesByUser` | `fn_listar_prestamos_activos_por_usuario` | `LoanActiveProjection` | `LoanService:782` |
| R-T2 | `fnReportBooksMostLoaned` (+Paginated +Count) | `fn_reporte_libros_mas_prestados` | `BookMostLoanedProjection` | `LoanService:352` |
| R-T3 | `fnReportIndexDelinquency` (+Paginated +Count) | `fn_reporte_indice_morosidad` | `ReportDelinquencyProjection` | `LoanService:369,387` |
| R-T4 | `fnReportUsageByPeriod` (+Paginated +Count) | `fn_reporte_uso_por_periodo` | `ReportUsageByPeriodProjection` | `LoanService:417,441` |
| R-T5 | `fnReportBooksMostLoanedDetailed` (+Paginated +Count) | `fn_reporte_libros_mas_prestados_detallado` | `BookMostLoanedDetailedProjection` | `LoanService:516,538` |
| R-T6 | `fnReportInventory` (+Paginated +Count) | `fn_reporte_inventario` (14 filtros, varios NULL) | `ReportInventoryProjection` | `LoanService:559,584,626,666` |
| R-T7 | `fnReportLoansOverdues` (+Paginated +Count) | `fn_reporte_prestamos_vencidos` (ya usa CAST NULL-safe) | `ReportOverduesProjection` | `LoanService:692,712` |
| R-T8 | `fnReportCategoriesDemanded` (+Paginated +Count) | `fn_reporte_categorias_demandadas` | `ReportCategoriesDemandedProjection` | `LoanService:734,754` |
| R-T9 | `fnReportSummaryFinancial` | `fn_reporte_resumen_financiero_multas` | `SummaryFinancialFinesProjection` | `FineService:199,204` |
| R-T10 | `fnPaymentsRecientes` | `fn_pagos_recientes` | `RecentPaymentProjection` | `FineService:208` |

R-T6 es el caso más riesgoso (14 filtros opcionales con NULL → Criteria
dinámico obligatorio). R-T7 ya trae el patrón CAST que debe replicarse
en Criteria (no en JPQL plano).

## C. Ordinarias (10) → JPQL/Criteria/derivadas

| # | Método | Particularidad PG | Consumidor | Reemplazo |
|---|---|---|---|---|
| O-1 | `AuditLogAuditRepository.searchWithFilters` | CASTs NULL-safe + `Sort.by("fecha_hora")` del llamador | `AuditService:61,120` | Criteria dinámico + cambiar Sort a `dateTime` (+ tests del service) |
| O-2 | `BookRepository.searchByTextOIsbn` | `:available IS NULL`, `isbn::text`, Pageable | `BookService:130` | Criteria dinámico o JPQL con `function('str', ...)`; sort por propiedad |
| O-3 | `BookRepository.searchByTextOIsbnYCategory` | JOIN `libro_categorias` (sin relación JPA) | `BookService:125` | JPQL con EXISTS subquery o Criteria; `IN` por ids vía `findByIdIn` + filtro en memoria NO (rompe paginación) |
| O-4 | `BookRepository.searchByTextOrIsbnAndAuthor` | JOIN `libro_autores` | `BookService:128` | igual que O-3 |
| O-5 | `BookRepository.suggestByTitle` | `similarity()` pg_trgm + LIMIT 10 | `BookService:341` | JPQL/Criteria con `function('similarity', ...)` + `setMaxResults(10)` |
| O-6 | `BookRepository.searchPendientes` | **sin llamadores en `src/main`** | — | Eliminar (verificar tests) o migrar como O-2 |
| O-7 | `BookRepository.searchByStatuses` | `IN :statusIds` + NULL-safe | `BookService:250` | Criteria dinámico |
| O-8 | `LoanRepository.findActivesByUserId` | `(fecha::date - NOW()::date)::INTEGER` | `LoanService:330`, `QueryLoansTool:57` | JPQL trae fecha estimada y `daysRemaining` se calcula en el service (mismo DTO; probar) o `function('date_part',...)` |
| O-9 | `ReservationRepository.searchReservationsToday` | `CURRENT_DATE`, `INTERVAL`, `\|\|` | `ReservationService:219` | JPQL con `:today`/`:tomorrow` pasados desde el service (determinista y testeable) |
| O-10 | `ReservationRepository.searchReservationsNexts` | igual | `ReservationService:239` | igual que O-9 |

`nativeSort(pageable)` en `BookService` (O-2..O-4,O-7) mapea sorts a
columnas físicas: al pasar a JPQL debe mapear a propiedades o eliminarse
con prueba de los endpoints de catálogo.

## D. Pruebas técnicas exigidas antes de migrar (spikes en PG real)

1. **S1 (side-effects):** `@Procedure` posicional contra `proc_crear_prestamo`
   en PG real (crear préstamo + verificar fila y stock). Si verde → patrón
   para R-S1..R-S6.
2. **S2 (ordinaria con NULLs):** Criteria dinámico para O-1/O-2 con todas
   las combinaciones de filtros NULL vs query nativa (`EXCEPT ALL` vacío).
3. **S3 (función pg específica):** `function('similarity',...)` (O-5) y
   ventana de fechas por parámetro (O-9) en PG real.
4. **S4 (tabular):** una función (R-T1, la más simple) reimplementada en
   JPQL con equivalencia fila a fila vs `SELECT * FROM fn_...`.

Sin S1–S4 en verde no se migra. Vehículo: `@DataJpaTest` contra PG local
(`sgb_spike`, sin Docker) + probes `psql` con `EXCEPT ALL`.
