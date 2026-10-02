# Publicación del código — 2026-10-01

La actualización se prepara en `codex/publish-studio-update`, conservando el historial de `main`.

## Contenido

Se incluyen fuentes, pruebas, scripts, documentación, manifiestos, workflows, interfaz y recursos oficiales de ejemplo. Las muestras de voz ya versionadas se conservan.

Se excluyen pesos de IA, motores instalados, entornos Python, distribuciones, diagnósticos, estado local, exportaciones y reglas MathCAT extraídas del paquete Maven. No se eliminan esos archivos del equipo.

## Validación

La compilación del reactor se verificó durante la preparación. La suite completa `mvn test` detectó 19 fallos y 1 error en 1582 pruebas del módulo de escritorio (16 omitidas). Esta actualización debe publicarse como pull request **en borrador**, no como release estable.

Se corrigió la expectativa antigua del catálogo de operaciones de IA y la codificación de los mensajes de progreso de ComfyUI. Los demás resultados que requieren revisión son:

- `com.marcosmoreiradev.docupodcaststudio.application.modelsetup.InspectLocalTheatreImageSetupReadinessUseCaseTest.fluxPresetUsesIntegratedWorkflowButStillRequiresEveryComponentAndLicense`
- `com.marcosmoreiradev.docupodcaststudio.application.modelsetup.LocalTheatreImageEngineManagerSmokeTest.smokeRequestFailsClearlyWhenFluxLicenseOrComponentsAreMissing`
- `ArchitectureBoundaryTest.domain_is_independent`
- `ArchitectureBoundaryTest.application_uses_ports_not_adapters`
- `ArchitectureBoundaryTest.presentation_does_not_construct_infrastructure`
- `ArchitectureBoundaryTest.content_analysis_consumers_are_confined_to_document_study`
- `com.marcosmoreiradev.docupodcaststudio.architecture.CompleteReadingProcessingBoundaryTest.reprocessConfirmationPrecedesOverlayAndCompleteProcessingDoesNotStartTts`
- `com.marcosmoreiradev.docupodcaststudio.architecture.DocumentSourceAndAudioFlowArchitectureTest.documentaryVideoUsesTheSharedModalProgressCoordinator`
- `com.marcosmoreiradev.docupodcaststudio.architecture.GuiComponentUsagePolicyTest.sourceAuditHasZeroProductDebtAndWritesTheUsageReport`
- `com.marcosmoreiradev.docupodcaststudio.architecture.GuiComponentUsagePolicyTest.directJavaFxConstructorsExistOnlyInsideTheApprovedFactories`
- `com.marcosmoreiradev.docupodcaststudio.architecture.ProductionSourceBoundaryTest.presentationRemainsProviderNeutral`
- `com.marcosmoreiradev.docupodcaststudio.architecture.ProductionSourceBoundaryTest.presentationDoesNotOwnTransportProcessesOrProviderClients`
- `com.marcosmoreiradev.docupodcaststudio.architecture.Utf8TextIntegrityTest.applicationTextIoDeclaresItsCharset`
- `com.marcosmoreiradev.docupodcaststudio.infrastructure.document.PdfV2CorpusEndToEndTest.importsPreparesReopensQueriesNarratesDiagnosesAndRendersCorpus`
- `com.marcosmoreiradev.docupodcaststudio.infrastructure.theatrepackage.JsonTheatreImportStateRepositoryTest.rejectsCorruptedGeneratedStateWithAnIOException`
- `com.marcosmoreiradev.docupodcaststudio.presentation.dialogs.StudioMessageDialogTest.presentationCodeCannotBypassTheCommonMessageContentPolicy`
- `com.marcosmoreiradev.docupodcaststudio.presentation.ink.composition.SketchBackdropTest.closedInteriorAndStageSurviveExportWithColoredInkOnTop`
- `com.marcosmoreiradev.docupodcaststudio.presentation.sidedock.SideDockLayoutPolicyTest.standardRailUsesTheReferenceRangeInsteadOfAFixedLegacyWidth`
- `com.marcosmoreiradev.docupodcaststudio.presentation.status.StatusBarLayoutTest.everyPertinentStatusActionHasAMonochromeSemanticIcon`

Las pruebas con motores reales y el renderizado completo de un ensayo no se ejecutaron para esta publicación. El estado de los tests debe revisarse de nuevo antes de fusionar el pull request.
