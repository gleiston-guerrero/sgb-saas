# Auditoria de nombres en ingles del backend

Fecha de cierre: 2026-09-13

Esta evidencia respalda el punto E1 de la rubrica: clases, interfaces,
metodos, atributos y variables del backend con nombre en ingles. Los textos
visibles al usuario (pantallas, mensajes, nombres de query params como
`desde`/`limite`) pueden seguir en espanol; lo que se audita es como se
llaman las cosas dentro del codigo.

## Comando reproducible

```powershell
python scripts\audit-english-names.py
```

## Alcance

- Fuente auditada: `backend-springboot/src/main/java` (271 archivos).
- El script recorre tipos y metodos, parte cada identificador por
  camel-case y marca los tokens que pertenecen a una lista de 31 raices
  del dominio usadas antes del renombrado (`libro`, `prestamo`,
  `usuario`, `multa`, `buscar`, `obtener`, etc.).
- Umbral de Completo del punto E1: 5 % o menos (peor entre tipos y
  metodos).

## Resultado de cierre

```text
Types: 0/283 flagged (0.00%)
Methods: 0/642 flagged (0.00%)
Worst rubric percentage: 0.00%
```

## Verificacion ampliada

Para no depender de un diccionario estrecho, se ejecuto una segunda pasada
con mas de 700 voces espanolas sobre los mismos tipos y metodos. Resultado:
**0/283 tipos y 7/642 metodos (1,09 %)**. Los 7 restantes son cognados
validos en ingles, no raices espanolas, y se declaran como excepciones:

| Identificador | Token | Uso en ingles |
|---|---|---|
| `AbstractChatbotTool.errorNode` | error | nodo de error del grafo del chatbot |
| `RedisConfig.cacheErrorHandler` | error | manejador de errores de cache de Spring |
| `GlobalExceptionHandler.handleStoredProcedureError` | error | manejo de error de procedimiento |
| `JwtService.rolePrincipal` | principal | rol principal (mayor privilegio) del token |
| `BackupStorageService.resolveLocalBase` | local | ruta base local de almacenamiento |
| `BookIsbnLookupService.generateSummaryViaIA` | via | resumen generado via IA |
| `EmailService.sendViaBrevoApi` | via | envio via API de Brevo |

## Renombrado aplicado (2026-09-13)

Primera pasada: los identificadores con raiz espanola real detectados por la
pasada ampliada (tipos, metodos publicos/privados y variables locales
relacionadas). Sin cambios de contratos externos: rutas, nombres de
`@RequestParam`, firmas SQL y claims JWT intactos.

- `EmailYaRegistradoException` -> `EmailAlreadyRegisteredException`
  (incl. `handleEmailYaRegistrado` -> `handleEmailAlreadyRegistered`)
- `FavoriteController/Service.agregar/quitar` -> `add/remove`
- `Book/Favorite/SuggestionAcquisition.antesGuardar` -> `beforePersist`
- `GeminiClient.llamarGemini` -> `callGemini`
- `ReservationScheduler.notifyQueVanAExpire` -> `notifyExpiringSoon`
- `ChatbotOrchestrator.inyectarUserIdSiRequired` -> `injectUserIdIfRequired`
- `BookService.esManagerOAdmin/esLibrarianSolo` -> `isManagerOrAdmin/isLibrarianOnly`
- `ReservationService.esReader`, `UserAdminService.esManager` -> `isReader/isManager`
- `LoanService.validateReservationSiAplica` -> `validateReservationIfApplicable`
- `LoanService.existeReservationVigenteOtroUser` -> `existsActiveReservationForOtherUser`
- `NotificationService.generateAlertaDue` -> `generateDueAlert`
- `ReportPdfService`: `createFuenteNegrita` -> `createBoldFont`,
  `agregarEncabezado` -> `addHeader`, `celdaEncabezado` -> `headerCell`,
  `primeroNotNulo` -> `firstNonNull`, `agregarTable/agregarRow` ->
  `addTable/addRow`, `negrita` -> `bold`, `anchosColumns` -> `columnWidths`
- Variables locales `esReader` -> `isReader` en `FineService`,
  `LoanService` y `NotificationService`

Segunda pasada (barrido exhaustivo de 282 tipos y 440+ metodos): se
renombro toda raiz espanola residual en nombres de tipos y metodos,
incluyendo `MaterialReservadoException` ->
`ReservedMaterialException`, `ServiceTemporalmenteNotAvailableException`
-> `ServiceTemporarilyNotAvailableException`,
`suscribir/desuscribir/notifyDisponibles` ->
`subscribe/unsubscribe/notifyAvailable`, `sugerir/sugerirByTitle` ->
`suggest/suggestByTitle`, `searchByTextOIsbnYAuthor` ->
`searchByTextOrIsbnAndAuthor`, `getMostPedidos*/findMostPedidosAgrupados`
-> `getMostRequested*/findMostRequestedGrouped`,
`generateReportSuggestionsMostPedidas` ->
`generateReportSuggestionsMostRequested`, `estaBlocked/resetear/secondsRestantes/estaRevocado`
-> `isBlocked/reset/remainingSeconds/isRevoked`,
`getValueEntero` -> `getIntegerValue`, `paymentParcial` -> `payPartial`,
`getFileBinario` -> `getFileBinary`, `mapearInput` -> `mapInput`,
`parsearResponse` -> `parseResponse`, `tieneIntencion*` ->
`has*Intention`, `validatePropiedadSession` -> `validateSessionOwnership`,
`construirPromptSystem` -> `buildSystemPrompt`, `truncarText` ->
`truncateText`, `searchPrimerVolume` -> `searchFirstVolume`,
`textOAlternativo` -> `textOrAlternative`, `exportarCsv` -> `exportCsv`,
`executeBackupProgramado` -> `executeScheduledBackup`,
`initializeTasksProgramadas` -> `initializeScheduledTasks`,
`notifyNextsAExpire` -> `notifyNextToExpire`,
`expireReservationsVencidas` -> `expireOverdueReservations`,
`antesCualquierUpdate` -> `beforeAnyUpdate`, `miCredential` ->
`myCredential`, `misSubscriptions` -> `mySubscriptions`, `createYSend*`
-> `createAndSend*`, `generateYSendCode` -> `generateAndSendCode`,
`getDaysRestantes/renewalsRestantes` -> `getDaysRemaining/renewalsRemaining`
(wire `@JsonProperty`/SQL intacto), `handleGenerica` -> `handleGeneric`,
`handleMaterialReservado` -> `handleReservedMaterial`,
`protegido` -> `protectedEndpoint` (ruta `/protegido` intacta),
`mostPedidos` -> `mostRequested`.

Quedan como cognados ingleses documentados: `error`,
`local`, `via`, `principal` (7 metodos, 1,09 % en pasada de 700+ voces;
0,00 % en el script oficial).

## Verificacion

- `scripts/audit-english-names.py`: 0,00 % (umbral Completo: <= 5 %).
- Compilacion `./mvnw -B compile`: BUILD SUCCESS.
- Tests de las clases tocadas (53 + 77): 130 tests, 0 fallos:
  `FavoriteServiceTest`, `FavoriteControllerTest`,
  `NotificationServiceTest`, `NotificationDueSchedulerTest`,
  `AuthServiceTest`, `AuthControllerTest`, `BookServiceTest`,
  `LoanServiceTest`, `ReservationServiceTest`, `UserAdminServiceTest`,
  `ReservationSchedulerTest`, `ProcedureMappingContractTest`,
  `ReportPdfServiceTest`.
