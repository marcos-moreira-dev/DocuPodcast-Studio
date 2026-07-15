# Bitácora RF2: refactor de presentación por casos de uso

Esta bitácora es vigente. Si el contexto de trabajo se recorta, continuar desde este archivo antes de tocar código.

## Contrato operativo por vista

- **Inicio**: abrir una fuente documental, abrir proyecto, cargar ejemplo o entrar a configuración inicial.
- **Documento**: raíz de lectura limpia; permite leer, seleccionar, escuchar, generar chunks y asignar voz/audio sin herramientas teatrales.
- **Teatro > Guión**: documento con capa teatral; permite imágenes por fragmento, personajes, mapa textual, mapa espacial, acciones y objetos.
- **Voces**: microaplicación administrativa para gestionar voces, muestras, tonos/emociones y motor/dispositivo.
- **Configuración**: modal de dependencias, settings, readiness, diagnóstico y herramientas locales.
- **Exportar**: salida final del producto; exporta audio/video y muestra estado de exportación.
- **Workspaces heredados**: preparación interna, jobs de audio y storyboard siguen como superficies internas/ocultas mientras se consolidan los flujos reales.

## Paso RF2-01: transporte de playback fuera del ViewModel

**Objetivo**: sacar del `DocuPodcastShellViewModel` el transporte operativo de reproducción sin cambiar comportamiento visible, reglas de buffer, continuidad, velocidad ni diagnóstico.

**Archivos tocados**:

- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/PlaybackTransportCoordinator.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/workspace/WorkspaceDescriptorCatalog.java`
- `src/test/java/com/marcosmoreiradev/docupodcaststudio/productization/PresentationOrchestrationRf2SourceTest.java`
- `DOCUMENTACION_ACTUAL/01_REGISTRO_TANDAS_RECIENTES.md`

**Decisión arquitectónica**: `PlaybackTransportCoordinator` queda como dueño de `SegmentAudioPlayer`, `PlaybackContinuationController`, `PlaybackSequentialQueueDriver`, `PlaybackRuntimeQueue`, `PlaybackCueDeadlineSequencer` y `PlaybackDiagnosticRecorder`. El ViewModel conserva propiedades JavaFX, selección actual, navegación, estado visible y traducción de resultados a mensajes.

**Comportamiento preservado**:

- Pausa, reanudación, stop, velocidad, reproducción de cue exacto y reproducción secuencial mantienen la misma semántica.
- La cola de buffer y el avance por callback/deadline no cambian.
- La instrumentación de diagnóstico sigue registrando eventos; solo cambia el dueño interno que resuelve player/WAV/cola.
- Las pruebas de voz y muestras siguen reproduciéndose con el player local, ahora a través del coordinador.

**Tests corridos**:

- `mvn -q -DskipTests compile`: verde.
- `mvn -q "-Dtest=PresentationOrchestrationRf2SourceTest,PlaybackCore5ImmediateSpeedRestartSourceTest,PlaybackSpeed3ContinuitySourceTest,PlaybackSpeedControlsSourceTest,PlaybackSequentialSessionHf7SourceTest,PresentationRefactorRf1SourceTest" test`: verde tras actualizar contratos fuente antiguos al nuevo coordinador.
- `mvn -q "-Dtest=*Playback*SourceTest,PresentationOrchestrationRf2SourceTest,DocUxHf10GMemoryRailOverlayTablesSourceTest,StreamingPlaybackChunkUxHf2SourceTest" test`: verde.
- `mvn -q test`: verde.

**Pendiente siguiente**:

- Revisar orquestadores grandes restantes por caso de uso: `SettingsDialog`, preparación de dependencias, exportación de video/audio y administración de voces.
- Planificar una segunda extracción solo cuando haya duplicación real o mezcla clara entre UI, caso de uso, ejecución externa y diagnóstico.

## Paso RF3-01: progreso de operaciones largas fuera de SettingsDialog

**Objetivo**: sacar de `SettingsDialog` el widget/heartbeat de progreso usado por preparación de CUDA, Voz IA avanzada, Voz local simple y Video local, sin cambiar el flujo visible ni la lógica de cada motor.

**Archivos tocados**:

- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/SettingsDialog.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/SettingsOperationProgressCoordinator.java`
- `src/test/java/com/marcosmoreiradev/docupodcaststudio/productization/PresentationOrchestrationRf3SettingsSourceTest.java`
- `src/test/java/com/marcosmoreiradev/docupodcaststudio/productization/AdvancedVoiceProgressPf2DSourceTest.java`
- `src/test/java/com/marcosmoreiradev/docupodcaststudio/productization/SettingsOperationProgressPf2ESourceTest.java`
- `DOCUMENTACION_ACTUAL/01_REGISTRO_TANDAS_RECIENTES.md`

**Decisión arquitectónica**: `SettingsOperationProgressCoordinator` queda como módulo transversal de presentación para diálogo de progreso, heartbeat, cierre seguro, `finish(...)`, `fail(...)` y actualización incremental. `SettingsDialog` conserva la orquestación específica de cada caso de uso de configuración: confirmar, llamar use cases, decidir mensaje final y refrescar la página activa.

**Comportamiento preservado**:

- Los botones y textos de Configuración siguen iguales.
- Las operaciones largas siguen mostrando progreso vivo y bloqueo de cierre hasta terminar.
- Los workers de CUDA/XTTS/Piper/FFmpeg siguen siendo los mismos; solo delegan la superficie visual de progreso.
- No cambia schema de settings ni readiness.

**Tests corridos**:

- `mvn -q -DskipTests compile`: verde.
- `mvn -q "-Dtest=PresentationOrchestrationRf3SettingsSourceTest,AdvancedVoiceProgressPf2DSourceTest,SettingsOperationProgressPf2ESourceTest,BuildGreenSettingsDialogUxHf3SourceTest,EmbeddedDependencyAssistantSourceTest" test`: verde.
- `mvn -q "-Dtest=AdvancedVoiceDownloadCompletionHf4SourceTest,VoiceChunksGpuStatusHf1SourceTest,PresentationOrchestrationRf3SettingsSourceTest" test`: verde tras actualizar tests fuente que aún buscaban progreso dentro de `SettingsDialog`.
- `mvn -q test`: verde.

**Pendiente siguiente**:

- Extraer una capa pequeña para ejecución background repetida si se confirma duplicación real entre los workers.
- Revisar `VoiceLibraryWorkspaceView` y exportación desde el objetivo operativo de cada vista, sin rediseñar pantallas.

## Paso RF4-01: Configuración por intención operativa

**Objetivo**: reorganizar Configuración desde el objetivo real del usuario: ajustar lectura, reproducción, motores/dependencias, rendimiento, video final, almacenamiento y soporte. Se eliminan secciones informativas sin controles propios y se evita que detalles técnicos dominen la vista normal.

**Archivos tocados**:

- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/SettingsDialog.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/SettingsFormModel.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/SettingsSupportActions.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellView.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/SettingsOperationProgressCoordinator.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/audio/LocalProcessVoiceTestSynthesisGateway.java`
- `src/test/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/SettingsRf4OperationalUxSourceTest.java`

**Decisión arquitectónica**: `SettingsDialog` conserva la composición de páginas y el cableado de casos de uso. `SettingsFormModel` concentra el mapeo controles/settings para mantener cohesión y el límite de líneas. `SettingsSupportActions` queda como adaptador opcional hacia comandos del Shell; si Configuración se abre sin Shell, las acciones de soporte se muestran deshabilitadas con explicación.

**Comportamiento preservado**:

- No cambia el schema de `OperationalSettings` ni `.docupodcast.json`.
- Preparar dependencias, probar CUDA, probar voz, preparar Piper y preparar FFmpeg siguen usando los mismos use cases.
- La sección `Video final` conserva preferencias de salida, pero la preparación de FFmpeg vive en `Motores y dependencias`.
- La sección `Soporte y diagnóstico` reemplaza al diagnóstico avanzado como superficie concreta para validar proyecto, revisar exportación, exportar reportes y abrir carpetas.
- `Rendimiento` muestra etiquetas humanas y filtra encoders por hardware detectado; los ids crudos quedan para soporte/diagnóstico.

**Tests corridos**:

- `mvn -q -DskipTests compile`: verde.
- `mvn -q "-Dtest=SettingsDialogSourceTest,GuidedEngineSettingsSourceTest,SettingsRf4OperationalUxSourceTest,PresentationOrchestrationRf3SettingsSourceTest,BuildGreenSettingsDialogUxHf3SourceTest,HonestActionableSettingsT119SourceTest,DocPerfHf10BAutoScrollGenerationAndFfmpegUrlSourceTest" test`: verde.
- `mvn -q "-Dtest=DocumentComfortReadingSourceTest,VoiceEngineSettingsSourceTest,VideoRenderModeSourceTest,VoiceLocalSimpleT121V06SourceTest,AdvancedVoiceDownloadUrlHf1SourceTest,AdvancedVoiceProgressPf2DSourceTest,ComputeDeviceSelectorT115SourceTest,DocUxHf10HProgressVideoRailSourceTest,EditableSettingsAndFrontHonestySourceTest,OperationalSettingsProductizationSourceTest" test`: verde.
- `mvn -q test`: verde.

**Pendiente siguiente**:

- Pantalla `Exportar` cerrada en `Paso RF5-01`: combo de FPS y propiedades de salida final viven en `presentation.video`.
- Auditar `Voces` con el mismo criterio operativo, sin rediseñar estética general todavía.

## Paso RF5-01: Exportar y workers de presentación

**Objetivo**: cerrar la primera revisión de `Exportar` separando las propiedades de salida final y el worker de render MP4 del shell principal. El usuario sigue usando el mismo comando `Exportar video`, pero resolución, FPS, encoder, progreso y cancelación viven en componentes de `presentation.video`.

**Archivos tocados**:

- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellView.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/process/FxBackgroundTaskRunner.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/video/VideoExportOptions.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/video/VideoExportOptionsDialog.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/video/VideoExportProgressCoordinator.java`
- `src/test/java/com/marcosmoreiradev/docupodcaststudio/productization/VideoExport1FinalMp4FlowSourceTest.java`
- `src/test/java/com/marcosmoreiradev/docupodcaststudio/productization/VideoExport1HfProgressCancellationSourceTest.java`
- `src/test/java/com/marcosmoreiradev/docupodcaststudio/productization/PresentationOrchestrationRf5ExportWorkersSourceTest.java`

**Decisión arquitectónica**: `DocuPodcastShellView` conserva la decisión de cuándo exportar, el `FileChooser` y la verificación de dependencias. `VideoExportOptionsDialog` concentra la selección de resolución, FPS y encoder. `VideoExportProgressCoordinator` concentra el diálogo de progreso, cancelación y `Task<Void>`. `FxBackgroundTaskRunner` centraliza el arranque de tareas JavaFX en hilos daemon para evitar repetir `new Thread(task, ...)` en cada flujo.

**Comportamiento preservado**:

- No cambia el schema de settings ni `.docupodcast.json`.
- El combo de FPS sigue ofreciendo 24, 30, 48 y 60.
- El nombre sugerido del MP4 conserva resolución y FPS.
- La exportación sigue ofreciendo preparar FFmpeg embebido si falta.
- El render sigue bloqueando con progreso, permite cancelar y usa el mismo `ExportFinalVideoUseCase`.

**Tests corridos**:

- `mvn -q -DskipTests compile`: verde.
- `mvn -q "-Dtest=VideoExport1FinalMp4FlowSourceTest,VideoExport1HfProgressCancellationSourceTest,PresentationOrchestrationRf5ExportWorkersSourceTest,EmbeddedDependencyAssistantSourceTest" test`: verde.
- `mvn -q test`: verde.

**Pendiente siguiente**:

- Extraer gradualmente los workers repetidos de `SettingsDialog` y `EmbeddedDependencySetupAssistant` hacia el mismo patrón, cuidando que cada caso conserve sus textos y confirmaciones.
- Auditar `Voces` con el mismo criterio operativo, sin rediseñar estética general todavía.

## Paso RF6-01: orquestadores restantes y legacy documental

**Objetivo**: dejar una auditoría recuperable de los orquestadores grandes restantes y consolidar la regla de documentación histórica antes de seguir refactorizando. Se aplica además una primera extracción pequeña: `EmbeddedDependencySetupAssistant` deja de crear hilos manuales para sus workers principales y usa `FxBackgroundTaskRunner`.

**Archivos tocados**:

- `DOCUMENTACION_ACTUAL/00_LEEME_PRIMERO.md`
- `DOCUMENTACION_ACTUAL/01_REGISTRO_TANDAS_RECIENTES.md`
- `DOCUMENTACION_ACTUAL/05_DEUDA_TECNICA_Y_REFACTOR.md`
- `DOCUMENTACION_ACTUAL/09_BITACORA_REFACTOR_PRESENTACION.md`
- `DOCUMENTACION_ACTUAL/10_AUDITORIA_ORQUESTADORES_Y_LEGACY.md`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/process/FxBackgroundTaskRunner.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/EmbeddedDependencySetupAssistant.java`
- `src/test/java/com/marcosmoreiradev/docupodcaststudio/productization/PresentationOrchestrationRf6LegacyAuditSourceTest.java`

**Decisión arquitectónica**: la limpieza documental no borra carpetas históricas; crea una auditoría vigente que decide el orden de refactor. La extracción técnica queda limitada al asistente embebido porque sus dos workers principales son homogéneos. `SettingsDialog` conserva workers como deuda explícita para migrarlos por grupos y no mezclar confirmaciones ni microcopy.

**Comportamiento preservado**:

- No cambia UX, schema de settings ni `.docupodcast.json`.
- No se borra documentación histórica.
- El asistente de dependencias conserva los mismos diálogos, pasos y mensajes; solo cambia el mecanismo de arranque del worker.

**Tests corridos**:

- `mvn -q -DskipTests compile`: verde.
- `mvn -q "-Dtest=PresentationOrchestrationRf6LegacyAuditSourceTest,DocumentationConsolidationAndPdfRoadmapSourceTest,PresentationOrchestrationRf5ExportWorkersSourceTest,EmbeddedDependencyAssistantSourceTest" test`: verde.
- `mvn -q test`: verde.

**Pendiente siguiente**:

- Migrar workers de `SettingsDialog` por dominios de operación: CUDA/XTTS, Piper, FFmpeg y primer uso.
- Auditar `VoiceLibraryWorkspaceView` como microaplicación administrativa antes de tocar estética.

## Paso RF7-01: workers de Configuración al runner común

**Objetivo**: cerrar la deuda inmediata de workers manuales en `SettingsDialog` sin cambiar el comportamiento visible. Las operaciones largas de Configuración conservan sus confirmaciones, mensajes, progreso y refresco de página, pero el arranque del hilo queda centralizado en `FxBackgroundTaskRunner`.

**Archivos tocados**:

- `DOCUMENTACION_ACTUAL/01_REGISTRO_TANDAS_RECIENTES.md`
- `DOCUMENTACION_ACTUAL/09_BITACORA_REFACTOR_PRESENTACION.md`
- `DOCUMENTACION_ACTUAL/10_AUDITORIA_ORQUESTADORES_Y_LEGACY.md`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/SettingsDialog.java`
- `src/test/java/com/marcosmoreiradev/docupodcaststudio/productization/PresentationOrchestrationRf6LegacyAuditSourceTest.java`
- `src/test/java/com/marcosmoreiradev/docupodcaststudio/productization/PresentationOrchestrationRf7SettingsWorkersSourceTest.java`

**Decisión arquitectónica**: `FxBackgroundTaskRunner` es el punto común para crear hilos daemon de presentación. `SettingsDialog` todavía contiene la orquestación específica de cada operación porque esta tanda no mezcla refactor de responsabilidades con cambios mecánicos de concurrencia. La siguiente división debe extraer coordinadores por dominio: Voz IA avanzada/CUDA, Voz local simple/Piper, Video local/FFmpeg y configuración inicial.

**Comportamiento preservado**:

- No cambia UX, schema de settings ni `.docupodcast.json`.
- No cambian confirmaciones, microcopy, progreso, errores ni uso de `Platform.runLater`.
- Preparar CUDA, probar GPU, preparar/descargar/importar/probar Voz IA avanzada, preparar/importar Piper, preparar/importar FFmpeg y configuración inicial siguen llamando los mismos casos de uso.

**Tests corridos**:

- `mvn -q -DskipTests compile`: verde.
- `mvn -q "-Dtest=PresentationOrchestrationRf7SettingsWorkersSourceTest,PresentationOrchestrationRf6LegacyAuditSourceTest,PresentationOrchestrationRf3SettingsSourceTest,SettingsRf4OperationalUxSourceTest,EmbeddedDependencyAssistantSourceTest" test`: verde.
- `mvn -q test`: verde.

**Pendiente siguiente**:

- Extraer coordinadores de operaciones de Configuración por dominio para reducir tamaño y acoplamiento de `SettingsDialog`.
- Auditar `VoiceLibraryWorkspaceView` como microaplicación administrativa antes de tocar estética general.

## Paso RF8-01: coordinador de Voz IA avanzada y CUDA

**Objetivo**: sacar de `SettingsDialog` la orquestación larga específica de Voz IA avanzada y CUDA. El diálogo sigue componiendo la tarjeta visual y sus botones, pero la ejecución de preparación, descarga, importación, prueba, reproducción de smoke y selección del motor vive en un coordinador de dominio.

**Archivos tocados**:

- `DOCUMENTACION_ACTUAL/01_REGISTRO_TANDAS_RECIENTES.md`
- `DOCUMENTACION_ACTUAL/09_BITACORA_REFACTOR_PRESENTACION.md`
- `DOCUMENTACION_ACTUAL/10_AUDITORIA_ORQUESTADORES_Y_LEGACY.md`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/AdvancedVoiceSettingsOperations.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/SettingsDialog.java`
- `src/test/java/com/marcosmoreiradev/docupodcaststudio/productization/AdvancedVoiceInAppSetupPf2BSourceTest.java`
- `src/test/java/com/marcosmoreiradev/docupodcaststudio/productization/AdvancedVoiceProgressPf2DSourceTest.java`
- `src/test/java/com/marcosmoreiradev/docupodcaststudio/productization/AdvancedVoiceSmoke4RSourceTest.java`
- `src/test/java/com/marcosmoreiradev/docupodcaststudio/productization/PresentationOrchestrationRf7SettingsWorkersSourceTest.java`
- `src/test/java/com/marcosmoreiradev/docupodcaststudio/productization/PresentationOrchestrationRf8AdvancedVoiceCoordinatorSourceTest.java`
- `src/test/java/com/marcosmoreiradev/docupodcaststudio/productization/XttsReadinessSmokeFinalRepairSourceTest.java`

**Decisión arquitectónica**: `AdvancedVoiceSettingsOperations` es un coordinador específico de presentación para el dominio Voz IA avanzada/CUDA. Usa `FxBackgroundTaskRunner` y `SettingsOperationProgressCoordinator`, pero no construye la página de Configuración. `SettingsDialog` conserva métodos delegadores con nombres históricos para no romper contratos fuente acumulados, mientras deja de importar los casos de uso XTTS/CUDA.

**Comportamiento preservado**:

- No cambia UX, schema de settings ni `.docupodcast.json`.
- No cambian botones, confirmaciones, progreso ni mensajes de estado.
- Los mismos casos de uso de CUDA, preparación, descarga, importación, prueba WAV, confirmación de reproducción y selección del motor siguen ejecutándose.
- `SettingsDialog` baja a menos de 1300 líneas.

**Tests corridos**:

- `mvn -q -DskipTests compile`: verde.
- `mvn -q "-Dtest=PresentationOrchestrationRf8AdvancedVoiceCoordinatorSourceTest,PresentationOrchestrationRf7SettingsWorkersSourceTest,PresentationOrchestrationRf6LegacyAuditSourceTest,AdvancedVoiceProgressPf2DSourceTest,AdvancedVoiceInAppSetupPf2BSourceTest,AdvancedVoiceSmoke4RSourceTest,XttsReadinessSmokeFinalRepairSourceTest,XttsLegacySettingsRepairHf2SourceTest,SettingsRf4OperationalUxSourceTest" test`: verde.
- `mvn -q "-Dtest=AdvancedVoiceDownloadCompletionHf4SourceTest,AdvancedVoiceDownloadDiagnosticsSourceTest,AdvancedVoiceDownloadVisibilityHf5SourceTest,CoquiXttsAssistantT117SourceTest,FirstUseSetupUxSetup2SourceTest,HonestActionableSettingsT119SourceTest,XttsDownloadVisibilityUxHf2SourceTest" test`: verde tras actualizar guardarraíles fuente que inspeccionaban `SettingsDialog`.
- `mvn -q test`: verde.

**Pendiente siguiente**:

- Extraer `PiperSettingsOperations` para Voz local simple.
- Extraer `VideoLocalSettingsOperations` para FFmpeg/FFprobe embebidos.
- Separar configuración inicial y, después, auditar `VoiceLibraryWorkspaceView`.

## Paso RF9-01: coordinador de Voz local simple y Piper

**Objetivo**: sacar de `SettingsDialog` la orquestación larga específica de Voz local simple/Piper. El diálogo sigue componiendo la tarjeta visual, las URLs editables y los botones existentes, pero la verificación, preparación, importación de voz, selección del motor y resumen de requisitos viven en un coordinador de dominio.

**Archivos tocados**:

- `DOCUMENTACION_ACTUAL/01_REGISTRO_TANDAS_RECIENTES.md`
- `DOCUMENTACION_ACTUAL/09_BITACORA_REFACTOR_PRESENTACION.md`
- `DOCUMENTACION_ACTUAL/10_AUDITORIA_ORQUESTADORES_Y_LEGACY.md`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/PiperSettingsOperations.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/SettingsDialog.java`
- `src/test/java/com/marcosmoreiradev/docupodcaststudio/productization/BuildGreenUxHf6SourceTest.java`
- `src/test/java/com/marcosmoreiradev/docupodcaststudio/productization/GuidedEngineCompletionPf5CSourceTest.java`
- `src/test/java/com/marcosmoreiradev/docupodcaststudio/productization/PresentationOrchestrationRf9PiperCoordinatorSourceTest.java`

**Decisión arquitectónica**: `PiperSettingsOperations` es un coordinador específico de presentación para el dominio Voz local simple/Piper. Usa `FxBackgroundTaskRunner` y `SettingsOperationProgressCoordinator`, pero no construye la página de Configuración. `SettingsDialog` conserva métodos delegadores con nombres históricos para no romper contratos fuente acumulados, mientras deja de importar los casos de uso de importación/selección de Piper.

**Comportamiento preservado**:

- No cambia UX, schema de settings ni `.docupodcast.json`.
- No cambian botones, confirmaciones, progreso ni mensajes de estado.
- Preparar, verificar, importar, seleccionar y guardar Voz local simple siguen llamando los mismos casos de uso.
- `SettingsDialog` baja a menos de 1120 líneas.

**Tests corridos**:

- `mvn -q -DskipTests compile`: verde.
- `mvn -q "-Dtest=PresentationOrchestrationRf9PiperCoordinatorSourceTest,PresentationOrchestrationRf8AdvancedVoiceCoordinatorSourceTest,BuildGreenUxHf6SourceTest,GuidedEngineCompletionPf5CSourceTest,PiperAutomaticSetupUxSetup3SourceTest,PiperIntermediateModeT118SourceTest,WrappedSetupDialogsUxHf4SourceTest,FirstUseOnboarding1SourceTest" test`: verde.
- `mvn -q "-Dtest=AdvancedVoiceInAppSetupPf2BSourceTest,PresentationOrchestrationRf7SettingsWorkersSourceTest,PresentationOrchestrationRf9PiperCoordinatorSourceTest" test`: verde tras actualizar guardarraíles fuente que agregan `PiperSettingsOperations` a la superficie inspeccionada.
- `mvn -q test`: verde.

**Pendiente siguiente**:

- Extraer `VideoLocalSettingsOperations` para FFmpeg/FFprobe embebidos.
- Separar configuración inicial de Voz local simple, que aún vive en `SettingsDialog` por su flujo de primer uso.
- Auditar `VoiceLibraryWorkspaceView` como microaplicación administrativa antes de tocar estética general.

## Nota pendiente de producto: Ribbon Exportar, video por texto

**Objetivo futuro**: desde el Ribbon `Exportar`, permitir un modo adicional de exportación de video de todo el documento que no dependa de imágenes subidas. Cada fragmento se renderiza como un frame simple con una plantilla visual, texto del fragmento y audio sincronizado.

**Alcance propuesto**:

- Entrada: documento completo con chunks de audio ya generados o generables por el flujo actual.
- Salida: MP4 final con un frame por fragmento narrado.
- Visual base: fondo blanco por defecto, texto del fragmento como elemento principal.
- Opciones visuales: imagen de fondo opcional, tipografía, color de letra y efecto de tipografía con opciones concretas como borde sólido y sombreado.
- Posición del texto: permitir escoger la región del frame donde vive el texto, porque no siempre estará centrado. La región debe comportarse como área de lectura con márgenes seguros y ajuste de líneas.
- Opción secundaria: miniatura o imagen pequeña en la parte inferior del frame, solo si ese fragmento ya tiene una imagen asignada desde el sidebar derecho. No se elige una imagen nueva desde Exportar.
- Si el fragmento tiene imagen asignada, ofrecer un checkbox para decidir si esa imagen acompaña debajo del texto en miniatura o si se usa como fondo del frame.
- No convertir esto en editor de video complejo; debe ser una salida rápida, legible y coherente con el objetivo de lectura narrada.

**Criterio UX**: el Ribbon `Exportar` debe diferenciar con claridad entre `Video con imágenes del proyecto` y `Video por texto del documento`. El segundo debe funcionar aunque el usuario no haya asignado imágenes visuales, usando fondo blanco y texto legible como base.

**Pendiente técnico**: cuando se aborde, diseñar el caso de uso antes de tocar FFmpeg: modelo de frame por fragmento, layout tipográfico, estilos de texto, región de texto, sincronización con duración WAV, uso opcional de imagen asociada y opciones de exportación reutilizando `VideoExportOptions`.

## Paso RF10-01: coordinador de Video local y FFmpeg

**Objetivo**: sacar de `SettingsDialog` la orquestación larga específica de Video local/FFmpeg. El diálogo sigue componiendo la tarjeta visual, la URL editable y los botones existentes, pero la verificación, descarga, importación de carpeta, persistencia de ruta embebida y mensajes de progreso viven en un coordinador de dominio.

**Archivos tocados**:

- `DOCUMENTACION_ACTUAL/01_REGISTRO_TANDAS_RECIENTES.md`
- `DOCUMENTACION_ACTUAL/09_BITACORA_REFACTOR_PRESENTACION.md`
- `DOCUMENTACION_ACTUAL/10_AUDITORIA_ORQUESTADORES_Y_LEGACY.md`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/SettingsDialog.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/VideoLocalSettingsOperations.java`
- `src/test/java/com/marcosmoreiradev/docupodcaststudio/productization/FfmpegPrep1DownloadPreparationSourceTest.java`
- `src/test/java/com/marcosmoreiradev/docupodcaststudio/productization/GuidedEngineCompletionPf5CSourceTest.java`
- `src/test/java/com/marcosmoreiradev/docupodcaststudio/productization/HonestActionableSettingsT119SourceTest.java`
- `src/test/java/com/marcosmoreiradev/docupodcaststudio/productization/PresentationOrchestrationRf7SettingsWorkersSourceTest.java`
- `src/test/java/com/marcosmoreiradev/docupodcaststudio/productization/PresentationOrchestrationRf10VideoLocalCoordinatorSourceTest.java`

**Decisión arquitectónica**: `VideoLocalSettingsOperations` es un coordinador específico de presentación para el dominio Video local/FFmpeg. Usa `FxBackgroundTaskRunner` y `SettingsOperationProgressCoordinator`, pero no construye la página de Configuración. `SettingsDialog` conserva métodos delegadores con nombres históricos para no romper contratos fuente acumulados, mientras deja de importar los casos de uso FFmpeg y reportes de runtime.

**Comportamiento preservado**:

- No cambia UX, schema de settings ni `.docupodcast.json`.
- No cambian botones, confirmaciones, progreso ni mensajes de estado.
- Verificar, preparar, descargar, importar y guardar Video local siguen llamando los mismos casos de uso.
- `SettingsDialog` baja a menos de 980 líneas.

**Tests corridos**:

- `mvn -q -DskipTests compile`: verde.
- `mvn -q "-Dtest=PresentationOrchestrationRf10VideoLocalCoordinatorSourceTest,FfmpegPrep1DownloadPreparationSourceTest,GuidedEngineCompletionPf5CSourceTest,HonestActionableSettingsT119SourceTest,PresentationOrchestrationRf7SettingsWorkersSourceTest,PresentationOrchestrationRf9PiperCoordinatorSourceTest" test`: verde.
- `mvn -q "-Dtest=DocUxHf10HProgressVideoRailSourceTest,EditableSettingsAndFrontHonestySourceTest,PresentationOrchestrationRf6LegacyAuditSourceTest,PresentationOrchestrationRf10VideoLocalCoordinatorSourceTest" test`: verde tras actualizar guardarraíles fuente que inspeccionaban responsabilidades movidas a `VideoLocalSettingsOperations`.
- `mvn -q test`: verde.

**Pendiente siguiente**:

- Separar configuración inicial de Voz local simple, que aún vive en `SettingsDialog`.
- Auditar `VoiceLibraryWorkspaceView` como microaplicación administrativa.
- Después, atacar `LocalTtsProcessAudioGenerationGateway` y los flujos restantes de Documento en `DocuPodcastShellViewModel`.

## Paso RF11-01: coordinador de configuración inicial

**Objetivo**: sacar de `SettingsDialog` la orquestación larga específica de primer uso. El diálogo sigue exponiendo `showFirstUseSetup`, disparando el prompt inicial y conservando métodos delegadores históricos, pero la inspección de readiness, confirmación, descarga de Voz local simple, selección del motor y mensajes finales viven en un coordinador de dominio.

**Archivos tocados**:

- `DOCUMENTACION_ACTUAL/01_REGISTRO_TANDAS_RECIENTES.md`
- `DOCUMENTACION_ACTUAL/09_BITACORA_REFACTOR_PRESENTACION.md`
- `DOCUMENTACION_ACTUAL/10_AUDITORIA_ORQUESTADORES_Y_LEGACY.md`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/InitialSetupSettingsOperations.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/SettingsDialog.java`
- `src/test/java/com/marcosmoreiradev/docupodcaststudio/productization/FirstUseOnboarding1SourceTest.java`
- `src/test/java/com/marcosmoreiradev/docupodcaststudio/productization/FirstUseSetupUxSetup2SourceTest.java`
- `src/test/java/com/marcosmoreiradev/docupodcaststudio/productization/PresentationOrchestrationRf7SettingsWorkersSourceTest.java`
- `src/test/java/com/marcosmoreiradev/docupodcaststudio/productization/PresentationOrchestrationRf11InitialSetupCoordinatorSourceTest.java`

**Decisión arquitectónica**: `InitialSetupSettingsOperations` es un coordinador específico de presentación para el caso primer uso. Reutiliza `PiperSettingsOperations` para seleccionar/guardar Voz local simple y usa `FxBackgroundTaskRunner` + `SettingsOperationProgressCoordinator` para conservar el mismo progreso. `SettingsDialog` deja de importar la descarga/reporte de Piper para configuración inicial.

**Comportamiento preservado**:

- No cambia UX, schema de settings ni `.docupodcast.json`.
- No cambian confirmación inicial, progreso ni mensajes de éxito/fallo.
- Primer uso sigue priorizando Voz local simple para escuchar rápido; Voz IA avanzada y Video local siguen siendo mejoras posteriores.
- `SettingsDialog` baja a menos de 920 líneas.

**Tests corridos**:

- `mvn -q -DskipTests compile`: verde.
- `mvn -q "-Dtest=PresentationOrchestrationRf11InitialSetupCoordinatorSourceTest,FirstUseSetupUxSetup2SourceTest,FirstUseOnboarding1SourceTest,PresentationOrchestrationRf7SettingsWorkersSourceTest,PresentationOrchestrationRf10VideoLocalCoordinatorSourceTest" test`: verde.
- `mvn -q "-Dtest=BuildGreenSettingsDialogUxHf3SourceTest,PiperAutomaticSetupUxSetup3SourceTest,PresentationOrchestrationRf3SettingsSourceTest,PresentationOrchestrationRf6LegacyAuditSourceTest,SettingsOperationProgressPf2ESourceTest,PresentationOrchestrationRf11InitialSetupCoordinatorSourceTest" test`: verde tras actualizar guardarraíles fuente a la superficie de Configuración compuesta por coordinadores.
- `mvn -q test`: verde.

**Pendiente siguiente**:

- Auditar y refactorizar `VoiceLibraryWorkspaceView` como microaplicación administrativa.
- Después, dividir `LocalTtsProcessAudioGenerationGateway`.
- Luego extraer flujos restantes de Documento en `DocuPodcastShellViewModel`.

## Paso RF12-01: acciones de muestras y motor en Vista Voces

**Objetivo**: iniciar la división de `VoiceLibraryWorkspaceView` como microaplicación administrativa sin rediseñar la pantalla. La primera responsabilidad extraída son las acciones de muestras por tono, porque mezclaban botones, diálogos de archivos, confirmaciones, llamadas al view model y mensajes de soporte dentro del workspace principal. Al cerrar el presupuesto de líneas también se extrae el bloque de motor/dispositivo, que era otro caso de uso autocontenido dentro de `Voces`.

**Archivos tocados**:

- `DOCUMENTACION_ACTUAL/01_REGISTRO_TANDAS_RECIENTES.md`
- `DOCUMENTACION_ACTUAL/09_BITACORA_REFACTOR_PRESENTACION.md`
- `DOCUMENTACION_ACTUAL/10_AUDITORIA_ORQUESTADORES_Y_LEGACY.md`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceEngineSettingsControls.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceLibraryWorkspaceView.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceSampleActions.java`
- `src/test/java/com/marcosmoreiradev/docupodcaststudio/productization/PresentationOrchestrationRf12VoiceSampleActionsSourceTest.java`
- Guardarraíles históricos de Voces que ahora inspeccionan la superficie compuesta vista + acciones.

**Decisión arquitectónica**: `VoiceSampleActions` es un coordinador específico de presentación para el caso "operar muestras de voz". No posee navegación ni render de módulos; recibe un contexto mínimo desde `VoiceLibraryWorkspaceView` para conocer ventana, voz seleccionada, tono seleccionado, refresco y mensajes. `VoiceEngineSettingsControls` concentra los ComboBox de motor/dispositivo, inspección de entorno de cómputo, guardado de `OperationalSettings` y microcopy de estado. Esto preserva la cohesión: la vista orquesta la microaplicación y los coordinadores ejecutan acciones de dominio visible.

**Comportamiento preservado**:

- No cambia UX visible, schema, `.docupodcast.json` ni contratos de biblioteca de voces.
- Importar, grabar, detener, cancelar, reproducir, exportar y eliminar muestras conservan los mismos textos y llamadas de aplicación.
- La confirmación de eliminación de muestra conserva el aviso de archivo gestionado por DocuPodcast.
- Seleccionar motor y dispositivo conserva los mismos valores persistidos y la misma sincronización con Configuración.
- `VoiceLibraryWorkspaceView` baja a cerca de 1000 líneas.

**Tests corridos**:

- `mvn -q -DskipTests compile`
- `mvn -q "-Dtest=PresentationOrchestrationRf12VoiceSampleActionsSourceTest,VoiceUx4R2BEngineSelectorSourceTest,VoiceLibraryOperationalSamplesPf3ASourceTest,VoiceLibraryWizardWiringT121V04CSourceTest,VoiceRegistrationWizard1SourceTest,VoiceSamplesRc1SourceTest,VoiceUx4R2AModularShellSourceTest,VoiceUx4R3ManageVoicesSourceTest,VoiceLibraryOperationalUxPf3BSourceTest" test`
- `mvn -q test`

**Pendiente siguiente**:

- Extraer una segunda pieza de `VoiceLibraryWorkspaceView`: presentación de catálogo/tonos o gestión de voz.
- Después, dividir `LocalTtsProcessAudioGenerationGateway`.
- Luego extraer flujos restantes de Documento en `DocuPodcastShellViewModel` y limpiar `DocuPodcastShellView`.

## Paso RF13-01: Vista Voces por flujo maestro-detalle

**Objetivo**: ordenar `Vista > Voces` como microaplicación administrativa, no como una columna larga de controles. Se conserva el sidebar de tres módulos y el estilo visual actual, pero el contenido adopta disciplina de escritorio: selección a la izquierda, trabajo/detalle a la derecha, acciones visibles y asistente enfocado para registrar una voz.

**Archivos tocados**:

- `DOCUMENTACION_ACTUAL/01_REGISTRO_TANDAS_RECIENTES.md`
- `DOCUMENTACION_ACTUAL/09_BITACORA_REFACTOR_PRESENTACION.md`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceLibraryWorkspaceView.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceWorkspaceLayout.java`
- `src/main/resources/css/voice-library.css`
- `src/test/java/com/marcosmoreiradev/docupodcaststudio/productization/PresentationOrchestrationRf13VoiceUxSourceTest.java`

**Decisión arquitectónica**: `VoiceWorkspaceLayout` concentra primitivas de presentación específicas de `Voces`: raíz de módulo, secciones, maestro-detalle y pasos del asistente. `VoiceLibraryWorkspaceView` mantiene navegación, selección, estado y comandos, pero ya no decide cada detalle de estructura visual. No se extraen todavía catálogo/tonos ni render de detalle como coordinadores separados; esa será la siguiente reducción de cohesión.

**Comportamiento preservado**:

- No cambia schema, `.docupodcast.json`, settings, motores ni rutas.
- El sidebar mantiene solo `Inicio`, `Configurar motor` y `Gestionar voces`.
- `Nueva voz` sigue siendo subflujo de `Gestionar voces`, ahora con pasos visibles: `Identidad`, `Neutral` y `Tonos opcionales`.
- Importar, grabar, detener, cancelar, reproducir, eliminar y exportar muestras siguen usando `VoiceSampleActions`.
- Seleccionar motor/dispositivo sigue usando `VoiceEngineSettingsControls`.

**Tests corridos**:

- `mvn -q -DskipTests compile`: verde antes de documentar la tanda.
- `mvn -q "-Dtest=PresentationOrchestrationRf13VoiceUxSourceTest,PresentationOrchestrationRf12VoiceSampleActionsSourceTest,VoiceUx4R2AModularShellSourceTest,VoiceUx4R2BEngineSelectorSourceTest,VoiceUx4R3ManageVoicesSourceTest,VoiceRegistrationWizard1SourceTest,VoiceLibraryWizardWiringT121V04CSourceTest,VoiceUx4R3DTheatricalToneCatalogSourceTest" test`: verde.
- `mvn -q "-Dtest=VoiceUx4R1HumanListSourceTest,VoiceUx4R3BModuleNavigationPolishSourceTest,VoiceUx4R3CAntiDashboardSourceTest,PresentationOrchestrationRf13VoiceUxSourceTest" test`: verde tras alinear guardarraíles históricos con el layout maestro-detalle.
- `mvn -q test`: verde.

**Pendiente siguiente**:

- Extraer catálogo/tonos o render de detalle de voz fuera de `VoiceLibraryWorkspaceView`.
- Después, dividir `LocalTtsProcessAudioGenerationGateway`.

## Paso RF14-01: wizard claro para registrar voces

**Objetivo**: pulir `Vista > Voces` desde el flujo operativo real. La prioridad es que `Gestionar voces > Nueva voz` se entienda como asistente guiado: nombrar la voz, registrar Neutral, agregar tonos opcionales y guardar. La frase que se interpreta debe dominar visualmente el paso de grabación.

**Decisión previa**: conservar el sidebar de tres módulos y no cambiar motores, schema, rutas ni síntesis. La tanda tocará layout, estilos, botones, tooltips y guardarraíles fuente. Si el cambio se alarga, continuar desde esta entrada antes de tocar más código.

**Archivos tocados**:

- `DOCUMENTACION_ACTUAL/01_REGISTRO_TANDAS_RECIENTES.md`
- `DOCUMENTACION_ACTUAL/09_BITACORA_REFACTOR_PRESENTACION.md`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/components/ActionButtonFactory.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceActionStrip.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceGeneratedTestPanel.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceLibraryWorkspaceView.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceSampleActions.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceWorkspaceLayout.java`
- `src/main/resources/css/voice-library.css`
- Guardarraíles fuente de Voces actualizados a `VoiceActionStrip`.

**Decisión arquitectónica**: `VoiceActionStrip` es un contenedor específico de presentación para `Voces`: envuelve botones y evita que JavaFX recorte etiquetas en columnas estrechas. `ActionButtonFactory` recibe overloads con tooltip para que los botones con símbolo mantengan texto accesible. El wizard mantiene la lógica existente, pero refuerza la jerarquía visual con `voice-interpretation-phrase` y un cuarto paso visible: `Guardar`.

**Comportamiento preservado**:

- No cambia schema, `.docupodcast.json`, motores, rutas, síntesis ni grabación.
- `Nueva voz` sigue dentro de `Gestionar voces`; no aparece como módulo del sidebar.
- Importar, grabar, detener, cancelar, reproducir, repetir, eliminar y exportar muestras siguen llamando a los mismos métodos de aplicación.
- El fondo blanco y separadores se limitan a `Vista > Voces`.
- `VoiceLibraryWorkspaceView` queda en 991 líneas, bajo el límite vigente de 1000.

**Tests corridos**:

- `mvn -q -DskipTests compile`: verde.
- `mvn -q "-Dtest=PresentationOrchestrationRf14VoiceWizardUxSourceTest,PresentationOrchestrationRf13VoiceUxSourceTest,VoiceRegistrationWizard1SourceTest,VoiceLibraryWizardWiringT121V04CSourceTest,VoiceLibraryOperationalUxPf3BSourceTest,VoiceUx4R3ManageVoicesSourceTest,PresentationOrchestrationRf12VoiceSampleActionsSourceTest" test`: verde.
- `mvn -q "-Dtest=VoiceComponentsCssT121V09SourceTest,VoiceLibraryFinalSmokeT121V10SourceTest,VoiceUx4R3CAntiDashboardSourceTest,PresentationOrchestrationRf14VoiceWizardUxSourceTest" test`: verde.
- `mvn -q test`: verde.

**Pendiente siguiente**:

- Auditar visualmente `Vista > Voces` en ejecución real para ajustar márgenes finos si alguna pantalla queda demasiado densa.
- Extraer render de detalle de voz o catálogo/tonos fuera de `VoiceLibraryWorkspaceView`.
- Después, dividir `LocalTtsProcessAudioGenerationGateway`.

## Paso RF14-02: prueba rápida clara en Configurar motor

**Objetivo**: hacer que `Vista > Voces > Configurar motor` se lea como dos regiones operativas: selección de motor/dispositivo a la izquierda y prueba rápida del motor seleccionado a la derecha. Se retira del render el resumen `Motor activo y alcance`, porque duplicaba información y empujaba la acción principal hacia abajo.

**Archivos tocados**:

- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceLibraryWorkspaceView.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java`
- `src/main/resources/css/components/actions.css`
- `src/main/resources/css/voice-library.css`
- Tests fuente de `Voces` y readiness alineados al nuevo flujo.

**Decisión arquitectónica**: la prueba rápida usa los mismos settings compartidos que la columna de selección; por eso el motor y dispositivo elegidos se propagan al caso de uso existente sin crear un flujo paralelo. Los botones secundarios blancos se suavizan desde `components/actions.css`, como componente transversal, no con estilos locales.

**Comportamiento preservado**:

- No cambia schema, motores, rutas, síntesis ni persistencia.
- `Configurar motor` sigue delegando en `VoiceEngineSettingsControls`.
- La prueba muestra estados humanos: `Generando voz...`, `Voz de prueba lista` y `Reproduciendo voz de prueba`.
- La generación sigue síncrona como antes; solo se añade estado visible previo para que el usuario entienda la acción.

**Tests corridos**:

- `mvn -q -DskipTests compile`: verde.
- Tests enfocados de `Voces`, RF13/RF14, readiness y presupuestos RF2: verde.
- `mvn -q test`: verde.

**Pendiente siguiente**:

- Auditar visualmente `Gestionar voces` con el wizard en ejecución real.
- Extraer render de detalle de voz o catálogo/tonos fuera de `VoiceLibraryWorkspaceView`.

## Paso RF14-03: prueba de motor en hilo de fondo

**Objetivo**: corregir el congelamiento visible al generar la voz de prueba desde `Configurar motor`. La síntesis puede tardar por carga de modelo/GPU, así que no debe ejecutarse en el hilo de JavaFX.

**Decisión arquitectónica**: `DocuPodcastShellViewModel` prepara un `Callable` con snapshot de proyecto, biblioteca, voz, tono y descriptor de motor/dispositivo. `VoiceLibraryWorkspaceView` ejecuta ese trabajo en un `Task` daemon y aplica el resultado de vuelta en JavaFX con `completeVoiceTestGeneration(...)`. El panel de prueba rápida vuelve a fondo blanco; solo los botones secundarios mantienen el gris suave transversal.

**Comportamiento preservado**:

- No cambia schema, motores, rutas ni generación.
- La prueba sigue usando el motor y dispositivo seleccionados.
- Los estados visibles se mantienen: `Generando voz...`, `Voz de prueba lista`, `Reproduciendo voz de prueba`.

**Tests corridos**:

- Tests enfocados de `Voces` y presupuestos RF2/RF13: verde.
- `mvn -q test`: verde.

## Paso RF14-04: resumen operativo en Gestionar voces

**Objetivo**: simplificar `Vista > Voces > Gestionar voces` sin tocar la columna izquierda `Voz registrada`. El primer nivel debe servir para revisar la voz seleccionada, elegir una emoción realmente registrada y generar una prueba corta. El editor completo de muestras pasa a un subflujo explícito.

**Archivos tocados**:

- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceLibraryWorkspaceView.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceManageOverviewPanel.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceManagementHeaderPanel.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceNewVoiceWizardPanel.java`
- `src/test/java/com/marcosmoreiradev/docupodcaststudio/productization/PresentationOrchestrationRf14VoiceWizardUxSourceTest.java`
- `src/test/java/com/marcosmoreiradev/docupodcaststudio/productization/VoiceUx4R3ManageVoicesSourceTest.java`
- `src/test/java/com/marcosmoreiradev/docupodcaststudio/productization/VoiceRegistrationWizard1SourceTest.java`
- `src/test/java/com/marcosmoreiradev/docupodcaststudio/productization/VoiceSamplesRc1SourceTest.java`
- `DOCUMENTACION_ACTUAL/09_BITACORA_REFACTOR_PRESENTACION.md`
- `DOCUMENTACION_ACTUAL/01_REGISTRO_TANDAS_RECIENTES.md`

**Decisión arquitectónica**: `VoiceManageMode.LIST` queda como resumen operativo y `VoiceManageMode.EDIT_VOICE` aloja el editor completo de muestras, catálogo teatral, wizard de tonos y prueba avanzada. El selector de emoción del resumen usa `VoiceReferenceSampleSet.registeredTones()`, por lo que una voz avanzada con solo Neutral muestra solo Neutral; las emociones no registradas no se prometen en el primer nivel.

**Comportamiento preservado**:

- No cambia schema, motores, rutas, síntesis ni formato de proyecto.
- La zona izquierda `Voz registrada` conserva lista, nombre y acciones actuales.
- `Nueva voz` sigue abriendo el wizard guiado.
- `Gestionar voz seleccionada` abre el flujo completo para registrar o reemplazar muestras.
- La prueba rápida sigue ejecutándose en hilo de fondo con el motor/dispositivo seleccionados.

**Tests corridos**:

- `mvn -q -DskipTests compile`: verde.
- Tests enfocados de `Voces`/RF14: verde.
- `mvn -q test`: verde.

**Pendiente siguiente**:

- Revisar visualmente el subflujo `Gestionar voz seleccionada` y decidir si conviene extraer el panel de resumen o el editor de muestras a clases dedicadas para bajar más la densidad de `VoiceLibraryWorkspaceView`.

## Paso RF14-05: Gestionar voces como selección y prueba contextual

**Objetivo**: ajustar `Vista > Voces > Gestionar voces` para que el primer nivel no repita el detalle de `Inicio` ni muestre mantenimiento de voz. El usuario selecciona una voz registrada, genera una prueba con el motor correcto y entra a `Gestionar voz seleccionada` solo cuando quiere editar muestras o exportarlas.

**Archivos tocados**:

- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceLibraryWorkspaceView.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceManageOverviewPanel.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceManagementHeaderPanel.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceProfilePresentationPolicy.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java`
- Tests fuente RF14 y VOZ-UX4R.

**Decisión arquitectónica**: la selección de voz sincroniza el motor cuando el usuario está en `Gestionar voces`: voz simple activa `Voz local simple`; voz avanzada activa `Voz IA avanzada`. La prueba se invalida al cambiar de voz para evitar reproducir un WAV generado con otro motor o tono. El editor completo conserva guardar, eliminar y exportar muestras; el resumen conserva solo selección, prueba y navegación.

**Comportamiento preservado**:

- No cambia schema, rutas, motores ni formato de proyecto.
- La generación de prueba sigue en hilo de fondo.
- `Nueva voz` sigue abriendo el wizard guiado.
- `Gestionar voz seleccionada` sigue abriendo el subflujo completo.

**Tests corridos**:

- `mvn -q -DskipTests compile`: verde.
- `mvn -q "-Dtest=PresentationOrchestrationRf14VoiceWizardUxSourceTest,VoiceUx4R3ManageVoicesSourceTest,VoiceRegistrationWizard1SourceTest,VoiceSamplesRc1SourceTest,PresentationOrchestrationRf13VoiceUxSourceTest,PresentationOrchestrationRf2SourceTest" test`: verde.
- `mvn -q test`: verde.

**Pendiente siguiente**:

- Validar visualmente el subworkspace `Gestionar voz seleccionada` con voz avanzada real y decidir si se extrae el editor de muestras a un componente dedicado.

## Paso RF14-06: editor único para voz avanzada

**Objetivo**: unificar `Nueva voz` y `Gestionar voz seleccionada` en un solo workspace enfocado en nombre de voz y muestras por emoción. El editor deja de mostrar andamio técnico, progreso tipo wizard, catálogo teatral visible o detalle duplicado de la voz.

**Archivos tocados**:

- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceLibraryWorkspaceView.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceProfileSampleEditorPanel.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceManagementHeaderPanel.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceSampleActions.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceManageOverviewPanel.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceWorkspaceLayout.java`
- `src/main/resources/css/voice-library.css`
- Tests fuente de RF13/RF14 y guardarraíles de `Voces`.

**Decisión arquitectónica**: `VoiceProfileSampleEditorPanel` es el único editor de voz avanzada. `Nueva voz` lo abre vacío con `Neutral`; `Gestionar voz seleccionada` lo abre con los datos existentes. La voz simple no entra a este editor porque no usa muestras ni emociones. El botón `Escuchar` reproduce la muestra grabada o importada de la emoción seleccionada, no una prueba TTS generada.

**Comportamiento preservado**:

- No cambia schema, motores, rutas ni formato de proyecto.
- Crear, guardar, eliminar, exportar, importar, grabar, detener, cancelar, escuchar, repetir y eliminar muestra siguen usando los flujos existentes.
- La prueba contextual del primer nivel de `Gestionar voces` sigue generando un WAV con el motor correcto.

**Tests corridos**:

- `mvn -q -DskipTests compile`: verde.
- `mvn -q "-Dtest=VoiceGeneratedTestT121V05SourceTest,VoiceLibraryFinalRedesignT121V08SourceTest,VoiceLibraryFinalSmokeT121V10SourceTest,VoiceLocalSimpleT121V06SourceTest,VoiceLibraryDeconstructionT121V01SourceTest,VoiceLibraryWizardWiringT121V04CSourceTest,VoiceUx4R2AModularShellSourceTest" test`: verde.
- `mvn -q test`: verde.

**Pendiente siguiente**:

- Hacer una revisión visual del editor con voz avanzada real y muestra Neutral para ajustar densidad si hace falta.

## Paso RF14-07: estados de muestra, emociones listas y bloqueo de prediseñadas

**Objetivo**: cerrar detalles operativos del editor común de `Nueva voz` / `Gestionar voz`: el usuario debe ver qué emociones ya tienen muestra, poder seleccionarlas rápido, entender que grabar/importar reemplaza esa emoción y no poder editar voces prediseñadas.

**Archivos tocados**:

- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceLibraryWorkspaceView.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceSampleActions.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceProfileSampleEditorPanel.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceReadyToneMicroCard.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceManageOverviewPanel.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceProfilePresentationPolicy.java`
- `src/main/resources/css/voice-library.css`
- Tests fuente RF14 y VOZ-UX4R.

**Decisión arquitectónica**: `VoiceProfileSampleEditorPanel` sigue siendo el editor único. La lista compacta `Emociones listas` queda en `VoiceReadyToneMicroCard` y se calcula desde `VoiceReferenceSampleSet`, no desde una nueva entidad ni un nuevo contrato de proyecto. `VoiceSampleActions` mantiene el transporte de muestras y expone callbacks mínimos para que el panel muestre `Reproduciendo muestra...` hasta que termine la reproducción estimada. Las voces prediseñadas se bloquean en el botón y en el handler.

**Comportamiento preservado**:

- No cambia schema, motores, rutas ni formato de proyecto.
- Importar o grabar una emoción reemplaza la muestra anterior de esa misma emoción, como ya hacía el dominio.
- `Escuchar` reproduce la muestra de la emoción seleccionada, no una prueba TTS generada.

**Tests corridos**:

- `mvn -q -DskipTests compile`: verde.
- `mvn -q "-Dtest=PresentationOrchestrationRf14VoiceWizardUxSourceTest,VoiceUx4R3ManageVoicesSourceTest,VoiceRegistrationWizard1SourceTest,VoiceSamplesRc1SourceTest,PresentationOrchestrationRf13VoiceUxSourceTest" test`: verde.
- `mvn -q test`: verde.

**Pendiente siguiente**:

- Revisar visualmente el editor con varias emociones registradas para ajustar ancho de chips si el catálogo real crece demasiado.

## Paso RF14-08: Inicio de Voces balanceado y tonos registrados legibles

**Objetivo**: corregir el inicio de `Vista > Voces` para que `Voces creadas` y `Detalle de voz seleccionada` tengan peso visual similar, y para que `Tonos registrados` no muestre chips o textos comprimidos con puntos suspensivos.

**Archivos tocados**:

- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceWorkspaceLayout.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceLibraryWorkspaceView.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceSampleRow.java`
- `src/main/resources/css/voice-library.css`
- Tests fuente RF13 y componentes de `Voces`.

**Decisión arquitectónica**: `VoiceWorkspaceLayout` agrega `balancedMasterDetail(...)` solo para superficies donde ambas regiones tienen función equivalente. `VoiceSampleRow` conserva el chip de tono, pero separa origen y detalle de archivo en una columna legible.

**Comportamiento preservado**:

- No cambia navegación, motores, muestras, guardado ni formato del proyecto.
- El panel izquierdo puede crecer y mantener scroll propio cuando haya más voces.
- Las muestras registradas siguen mostrando el tono y el archivo asociado, pero sin comprimir la información principal.

**Tests corridos**:

- `mvn -q -DskipTests compile`: verde.
- `mvn -q "-Dtest=VoiceHomeModuleModeSourceTest,VoiceUx4R3CAntiDashboardSourceTest,PresentationOrchestrationRf13VoiceUxSourceTest,VoiceComponentsCssT121V09SourceTest" test`: verde.
- `mvn -q test`: verde.

**Pendiente siguiente**:

- Validar visualmente `Inicio` con varias voces y varios tonos para confirmar que el balance responde bien a anchos menores.

## Paso RF15: carcasas homogéneas de sidebars de Documento

**Objetivo**: homogeneizar la carcasa de los paneles laterales de `Documento`: cabecera, botón de colapso, separadores, fondo, borde y rail compacto, sin tocar el contenido interno de `Fragmento`, `Audio`, `Imagen` ni `Visual`.

**Archivos tocados**:

- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/components/DocumentSidePanelChrome.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/components/SidePanelToggleButton.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/components/RailToggleButton.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/components/AppStyles.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/components/CollapsibleMediaRail.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/sidedock/WorkspaceSideDock.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/components/GuiComponentCatalog.java`
- `src/main/resources/css/compat-legacy.css`
- Tests fuente RF15 de `Documento` y catálogo GUI.

**Decisión arquitectónica**: `DocumentSidePanelChrome` construye la cabecera compartida y `SidePanelToggleButton` concentra el botón iconográfico con tooltip y texto accesible. `WorkspaceSideDock` y `CollapsibleMediaRail` comparten esas piezas; `RailToggleButton` queda como wrapper legacy para catálogo/compatibilidad.

**Comportamiento preservado**:

- No cambia el contenido interno de los paneles de Documento.
- El rail derecho sigue siendo plegable; la manija visual de redimensionado se elimina para evitar la barra tipo split.
- Los títulos `Fragmento` y `Visual` se conservan.
- El rail izquierdo conserva sus módulos, pero usa un acento azul grisáceo más sobrio.

**Tests corridos**:

- `mvn -q -DskipTests compile`: verde.
- `mvn -q "-Dtest=DocumentSidePanelChromeRf15SourceTest,DocumentSidebarsAndGlassPlaybarT105ASourceTest,DocumentLayerAssignmentRailSourceTest,RibbonAndRailCleanupUxRibbon3SourceTest,GuiComponentCatalogTest,GuiComponentFreezeT97SourceTest" test`: verde.
- `mvn -q test`: verde.

**Pendiente siguiente**:

- Revisar visualmente Documento con `Fragmento`, `Audio`, `Imagen` y `Visual` abiertos para confirmar que la nueva carcasa no altera densidad ni alineación interna.

## Ajuste RF15-01: divisores de split invisibles en Documento

**Objetivo**: quitar la barra visual de split que separaba los sidebars del área central. El usuario conserva sidebars plegables, pero sin la línea/manija rígida que competía con la carcasa limpia.

**Archivos tocados**:

- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/components/CollapsibleMediaRail.java`
- `src/main/resources/css/document/document-page.css`
- `src/main/resources/css/components/media-rail.css`
- `src/test/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentRightRailT107SourceTest.java`

**Decisión arquitectónica**: el `SplitPane` del Documento oculta su divisor con CSS acotado a `.document-split`. El rail derecho mantiene su estructura interna, pero su `resizeHandle` queda con ancho cero y transparente; el control visible para el usuario es el toggle común del panel.

**Tests corridos**:

- `mvn -q -DskipTests compile`: verde.
- `mvn -q "-Dtest=DocumentRightRailT107SourceTest,DocumentSidePanelChromeRf15SourceTest,DocumentSidebarsAndGlassPlaybarT105ASourceTest,CssModularitySourceTest" test`: verde.
- `mvn -q test`: verde.

## Paso RF16-RF20: Documento simple, Teatro/Guión y exportación de obra

**Objetivo**: separar la lectura documental limpia de la capa teatral. `Documento` queda enfocado en leer, escuchar, generar audio y ajustar capas generales; `Teatro > Guión` reutiliza el mismo documento, pero habilita herramientas teatrales como imágenes de fragmentos, personajes, mapa textual, mapa espacial, acciones y objetos.

**Archivos tocados**:

- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentWorkspaceMode.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentWorkspaceView.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/theatre/*`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/ribbon/RibbonDefinitionCatalog.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/command/AppCommandId.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/command/AppCommandRegistry.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellView.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/domain/theatre/TheatreProjectLayer.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/domain/project/DocuPodcastProject.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/json/DocuPodcastProjectJsonReader.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/json/DocuPodcastProjectJsonWriter.java`
- Tests fuente y JSON de RF16/RF20.

**Decisión arquitectónica**:

- `DocumentWorkspaceMode.READING` no muestra sidebar derecho ni el módulo `Imagen`.
- `DocumentWorkspaceMode.THEATRE_SCRIPT` agrega un `TheatreSideDock` derecho con módulos teatrales.
- El módulo de imágenes por fragmento pasa a ser `Fragmentos visuales` dentro del sidebar teatral.
- `T1`, `T2`, `T3` y siguientes son alias técnicos de fragmentos/secuencia; no representan personajes.
- Los alias dramáticos se modelan aparte con `VoiceRoleAlias`: una voz real como `Mario Alonzo` puede usarse en la obra o el documento bajo un alias/personaje como `El villano`, sin renombrar ni duplicar la voz real.
- El bloque `theatre` en `.docupodcast.json` es opcional para mantener compatibilidad con proyectos existentes.
- `Exportar obra` abre opciones teatrales iniciales y por ahora delega en el exportador de video existente; el compositor teatral completo queda como siguiente tanda.

**Comportamiento preservado**:

- Los proyectos sin bloque `theatre` cargan con capa teatral vacía.
- La lectura normal sigue funcionando sin obligar al usuario a ver herramientas teatrales.
- La exportación normal de video/audio no cambia.
- La relación voz real ↔ alias dramático queda preparada a nivel de dominio/persistencia, todavía sin UI final.

**Tests corridos**:

- `mvn -q -DskipTests compile`: verde.
- Tests enfocados de RF16-RF20, sidebars/documento, readiness y JSON teatral: verde.
- `mvn -q test`: verde.

**Pendiente siguiente**:

- Implementar compositor real de `Exportar obra`: fondo blanco/imagen, texto opcional, tipografía, sombra/borde, región de texto, mapa espacial lateral, personajes y desplazamientos.
- Diseñar UI de `Personajes`, `Mapa textual`, `Mapa espacial`, `Acciones` y `Objetos`.
- Revisar el PDF de referencia teatral `D:\Archivos generales\Documentos\Revolución Ciudadana\teatro\referencias\La Guerra de los Filósofos Guión y mapas.pdf` (38.7 MB) y documentar sus ideas visuales sin copiar contenido innecesario al repositorio; si se decide versionarlo, crear primero una política para binarios de referencia.

## Paso RF21: presets oficiales de Voz IA avanzada y mapa espacial base

**Objetivo**: reemplazar la referencia antigua de voz avanzada por un catálogo oficial embebido de voces prediseñadas, usando los audios entregados en `C:\Users\MARCOS MOREIRA\Downloads\todas voces\resultados finales`, y versionar el mapa espacial base para el futuro modo teatral.

**Archivos tocados**:

- `samples/voices/advanced-presets/`
- `samples/theatre/maps/mapa-espacial.png`
- `models/tts/xtts/speakers/voz-por-defecto.wav`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/domain/voice/OfficialAdvancedVoicePresetCatalog.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/domain/voice/VoiceLibrary.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/application/voice/VoiceReferenceSamplePathResolver.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/json/DocuPodcastProjectJsonReader.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/audio/XttsTtsCommandTemplate.java`
- Tests de biblioteca de voces, readiness XTTS, panel de audio, exportación de muestras y catálogo oficial.

**Decisión arquitectónica**:

- Las nuevas voces oficiales viven como `VoiceProfileType.PREDEFINED` con motor `XTTS` y muestras `APP_DEFAULT`/`APP_RESOURCE`, no como voces del usuario.
- `VoiceLibrary.defaults()` ya no incluye `VOC-OWN-PLACEHOLDER`; ese identificador queda solo como compatibilidad legacy para proyectos o tests antiguos y se elimina al cargar proyectos modernos.
- La voz runtime `models/tts/xtts/speakers/voz-por-defecto.wav` se genera desde el preset oficial `hombre_adulto_personaje_narrativo/neutral.wav`.
- El mapa espacial base queda en `samples/theatre/maps/mapa-espacial.png` para que el futuro modo Teatro use una referencia embebida.
- La voz real y el alias teatral son conceptos separados: una voz real como `Mario Alonzo` puede representarse en una obra o documento como alias/personaje `El villano`, sin renombrar la voz ni duplicar muestras.

**Comportamiento preservado**:

- Proyectos existentes cargan aunque no tengan bloque teatral ni catálogo oficial guardado.
- Las muestras de usuario siguen resolviéndose dentro del proyecto; las muestras oficiales se resuelven desde la raíz de la app.
- XTTS sigue recibiendo `voz-por-defecto.wav` cuando el motor necesita un speaker base portable.

**Tests corridos**:

- `mvn -q -DskipTests compile`: verde.
- Tests enfocados de catálogo oficial, biblioteca de voces, exportación de muestras y resolución de muestras: verde.
- `mvn -q test`: verde.

**Pendiente siguiente**:

- Exponer en UI la relación voz real -> alias de personaje.
- Usar el mapa espacial base en el módulo `Mapa espacial` del sidebar teatral.
- Limpiar documentación histórica que todavía menciona `Mi voz` o el MP4 antiguo, sin perder contexto de migraciones pasadas.

## Ajuste RF21-01: cambio de voz invalida chunks y limpieza de lectura

**Objetivo**: asegurar que al elegir otra voz desde la vista Documento no se reutilicen chunks generados con la voz anterior, y pulir tres detalles operativos reportados durante lectura real: puntuación XTTS, continuidad de reproducción e imágenes fuente demasiado encajonadas.

**Archivos tocados**:

- `src/main/java/com/marcosmoreiradev/docupodcaststudio/application/audio/AudioJobRepository.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/application/audio/DeletePersistedAudioJobsUseCase.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/application/AudioApplicationServices.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/application/ApplicationServicesFactory.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/audio/AudioJobFileRepository.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/audio/TtsTextPreprocessor.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/components/SourceVisualBlockView.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/AudioWorkflowCoordinator.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/PlaybackContinuationController.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/PlaybackCueDeadlineSequencer.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/PlaybackSequentialQueueDriver.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/PlaybackTimingPolicy.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceListItemView.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/domain/voice/OfficialAdvancedVoicePresetCatalog.java`
- CSS de documento, visuales fuente y biblioteca de voces.
- Tests de repositorio de jobs, preprocesamiento TTS, playback y protección fuente del cambio de voz.

**Decisión arquitectónica**:

- Cambiar la voz en `Documento` dispara `selectDocumentAudioSource(...)` y elimina los jobs/chunks persistidos del proyecto para evitar que el documento siga leyendo audio generado con la voz anterior.
- `Generar`, `Rehacer chunks` y `Renderizar desde aquí` limpian primero los chunks persistidos antes de crear una corrida nueva.
- `Seguir generando` no limpia jobs: conserva la semántica de recuperación de una generación interrumpida.
- XTTS recibe texto sin puntos de cierre de oración, preservando decimales, y la cola de playback añade una pausa mínima de 1 segundo entre cues.
- La reproducción secuencial ya no avanza por un `stop` genérico del player si el cue no llegó cerca del final; esto reduce cortes a mitad de documento.
- Las imágenes detectadas en la fuente se muestran como imagen directa, sin tarjeta informativa alrededor.
- Las voces oficiales no narrativas se etiquetan como `diálogo`; las emociones registradas se distribuyen en panel envolvente para no desbordar horizontalmente.

**Comportamiento preservado**:

- La selección de motor/voz sigue usando los settings existentes.
- Los proyectos antiguos siguen cargando; solo se eliminan chunks cuando el usuario cambia voz o pide una generación fresca.
- Las voces oficiales siguen siendo prediseñadas y no editables como voces de usuario.
- El alias teatral sigue siendo una capa separada de la voz real: una voz puede llamarse `Mario Alonzo` y usarse como `El villano`.

**Tests corridos**:

- `mvn -q -DskipTests compile`: verde.
- Tests enfocados de preprocesamiento XTTS, limpieza de jobs, pausa de playback y protección fuente de cambio de voz: verde.
- `mvn -q test`: verde.

**Pendiente siguiente**:

- Revisar con evidencia real si todavía aparece un corte de playback en documentos largos.
- Diseñar el módulo teatral derecho expandible como workspace cuando el usuario lo agrande mucho.
- Implementar el compositor real de `Exportar obra` con texto, imagen, mapa espacial y desplazamientos.

## Ajuste RF21-02: voz predeterminada del documento y voces específicas

**Objetivo**: separar explícitamente la voz predeterminada del documento de las voces asignadas a fragmentos concretos, para que una voz elegida desde `Gestionar voces` o desde el panel `Audio` aplique a todo el documento y solo sea sustituida cuando el fragmento tenga una voz propia.

**Archivos tocados**:

- `src/main/java/com/marcosmoreiradev/docupodcaststudio/application/audio/AudioGenerationUnit.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentAudioNarrationPanel.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/AudioWorkflowCoordinator.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceLibraryWorkspaceView.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceListItemView.java`
- Tests de plan de render de audio, panel de audio del documento y biblioteca de voces.

**Decisión arquitectónica**:

- `VOC-NARRATOR` queda como marcador de voz heredada/predeterminada, no como una voz fija que deba imponerse sobre el documento.
- Si un fragmento no tiene voz específica, el render usa la voz configurada como predeterminada del documento.
- Si un fragmento tiene capa `VOICE` propia, esa voz gana sobre la predeterminada del documento.
- La capa `EMOTION` sigue siendo específica del fragmento cuando se asigna junto con una voz.
- `Gestionar voces` puede seleccionar una voz para todo el documento con `Usar voz`, sin entrar al editor de muestras.
- El panel `Audio` separa acciones: usar una voz en todo el documento, asignarla solo al fragmento seleccionado y eliminar voces específicas del documento.
- Cambiar la voz global o eliminar voces específicas invalida chunks persistidos para evitar mezclar audio viejo con la voz nueva.

**Comportamiento preservado**:

- Los proyectos con segmentos antiguos que apuntan a `VOC-NARRATOR` ahora heredan la voz predeterminada del documento.
- Las asignaciones específicas por fragmento siguen funcionando como capa local.
- Las voces prediseñadas siguen siendo no editables; se pueden usar como voz de documento cuando corresponde.

**Tests corridos**:

- `mvn -q -DskipTests compile`: verde.
- Tests enfocados de plan de audio, cambio de voz del documento y biblioteca de voces: verde.
- `mvn -q test`: verde.

**Pendiente siguiente**:

- Revisar visualmente que el botón `Usar voz` quede suficientemente visible en listas largas de voces.
- Afinar el panel `Audio` para mostrar de forma compacta cuándo un fragmento hereda la voz del documento y cuándo tiene una voz específica.

## Ajuste RF21-03: voz global con regeneración automática y reproducción clara

**Objetivo**: hacer que la voz predeterminada del documento sea una decisión única y visible, sincronizada entre `Vista > Voces` y el panel `Audio`, y que cambiarla regenere los chunks sin exigir acciones manuales adicionales.

**Archivos tocados**:

- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/DocumentSelectionCoordinator.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentAudioNarrationPanel.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentWorkspaceView.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/components/FloatingReadingControlBar.java`
- `src/main/resources/css/document-reader.css`

**Decisión arquitectónica**:

- La voz predeterminada del documento vive en un único setting operativo y se puede cambiar desde `Gestionar voces` o desde `Audio`.
- Las voces específicas de fragmento siguen siendo una capa local que gana sobre la voz predeterminada.
- Cambiar la voz predeterminada borra chunks anteriores y lanza una nueva generación completa sin iniciar playback; las voces específicas existentes se respetan.
- `Reproducir desde selección` reprioriza la generación desde el fragmento elegido si ya hay un render en curso, en vez de esperar una cola previa.
- Al limpiar la selección, la acción principal vuelve a `Reproducir documento`; el botón pequeño redundante de reproducir desde inicio se retira de la playbar.

**Comportamiento preservado**:

- El formato del proyecto no cambia.
- `Asignar voz al fragmento seleccionado` y `Eliminar voces específicas del documento` mantienen su función de overrides locales.
- La barra de pausa, play, stop, velocidades, anterior y siguiente conserva su comportamiento.

**Tests corridos**:

- Pendiente en esta tanda: compilar y ejecutar pruebas enfocadas después de los ajustes de código.

**Pendiente siguiente**:

- Revisar con logs de reproducción si persiste el corte de playback a mitad de documento después de eliminar la mezcla de chunks viejos.

## Ajuste RF21-04: fragmento completo, voz general y guardado al abrir fuente

**Objetivo**: corregir tres fricciones operativas de Documento: que `Reproducir fragmento (solo este)` reproduzca todo el fragmento seleccionado, que el retorno a voz heredada sea claro como `Asignar voz general`, y que al abrir una fuente documental sin proyecto se pida guardar el `.docupodcast.json` junto a la fuente.

**Archivos tocados**:

- `src/main/java/com/marcosmoreiradev/docupodcaststudio/domain/playback/PlaybackManifest.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentWorkspaceView.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentAudioNarrationPanel.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/components/ActionButtonFactory.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/components/AppStyles.java`
- `src/main/resources/css/components/actions.css`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellView.java`
- Tests de manifest de playback, selección de documento, panel de audio y guardado al abrir fuente.

**Decisión arquitectónica**:

- `PlaybackManifest.onlySegment(...)` crea un manifest filtrado con todas las cues del segmento, para que `Reproducir fragmento (solo este)` no caiga en la primera oración/unidad del bloque.
- El panel `Audio` conserva la diferencia entre voz predeterminada del documento y voz específica del fragmento, pero el botón local ahora dice `Asignar voz general` para expresar que el fragmento vuelve a heredar la voz del documento.
- El click en blanco de la hoja solo limpia selección si el playback está detenido; durante narración, la selección activa sigue estable.
- El Shell, no el dominio, decide pedir `Guardar proyecto` después de abrir una fuente sin proyecto; el FileChooser apunta por defecto a la carpeta de la fuente documental.

**Comportamiento preservado**:

- Las voces específicas por fragmento siguen siendo overrides locales.
- `Eliminar voces específicas del documento` conserva la limpieza global de overrides.
- El formato del proyecto no cambia.

**Tests corridos**:

- `mvn -q -DskipTests compile`: verde.
- Tests enfocados de manifest de playback, reproducción de fragmento, selección de documento, panel de audio y guardado al abrir fuente: verde.
- `mvn -q test`: verde.

**Pendiente siguiente**:

- Si el playback vuelve a detenerse a mitad de documento, revisar el reporte de eventos de reproducción antes de cambiar la cola o la estrategia de avance.

## Ajuste RF21-05: Voz local simple aislada de voces avanzadas

**Objetivo**: corregir el preflight de Voz local simple cuando la voz global seleccionada es un preset o muestra de Voz IA avanzada. La carpeta `todas voces` contiene referencias avanzadas, no modelos Piper `.onnx`, así que Piper no debe interpretar esos ids como modelos locales simples.

**Archivos tocados**:

- `src/main/java/com/marcosmoreiradev/docupodcaststudio/application/runtime/PiperVoiceModelPathPolicy.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/application/runtime/RuntimeArtifactPaths.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/application/modelsetup/InspectPiperSetupReadinessUseCase.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/application/modelsetup/DownloadPiperPortableRuntimeUseCase.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/application/modelsetup/ImportPiperVoiceFolderUseCase.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/audio/PiperTtsCommandTemplate.java`
- Tests de readiness Piper, comando Piper y guardarraíl fuente de Piper.

**Decisión arquitectónica**:

- `PiperVoiceModelPathPolicy` separa la voz global de la app del modelo interno de Piper.
- Si la voz activa es un preset avanzado, una muestra humana, un archivo `.wav/.mp3/.flac`, una ruta de biblioteca de voces o un id `VOC-*`, Voz local simple vuelve al modelo embebido `models/tts/piper/voices/es_ES-default-medium.onnx`.
- Las rutas legacy/localizadas de `storage.modelsDirectory`, como `recursos locales local simple`, ya no arrastran la preparación de Piper a carpetas antiguas; se vuelve a `models/tts/piper/voices`.
- Un `.onnx` explícito sigue permitiendo un modelo Piper personalizado.

**Comportamiento preservado**:

- No cambia schema de settings ni formato de proyecto.
- Voz IA avanzada conserva sus presets y muestras.
- Voz local simple sigue usando `tools/piper/piper.exe` y `scripts/tts/piper-file-to-wav.ps1`.

**Tests corridos**:

- `mvn -q -DskipTests compile`: verde.
- `mvn -q "-Dtest=InspectPiperSetupReadinessUseCaseTest,PiperTtsCommandTemplateTest,PiperRealTtsEndToEndSourceTest" test`: verde.

**Pendiente siguiente**:

- Volver a ejecutar `Preparar dependencias recomendadas` en la app y confirmar que Voz local simple queda omitida/lista cuando ya existe el modelo embebido.

## Ajuste RF21-06: sidebar teatral expansible y playbar vertical

**Objetivo**: permitir que el sidebar teatral derecho se expanda o pliegue con más libertad sin romper el lector central; cuando el área de lectura queda estrecha, la playbar pasa a modo vertical junto al borde izquierdo del lector.

**Archivos tocados**:

- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/components/FloatingReadingControlBar.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentWorkspaceView.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/theatre/TheatreSideDock.java`
- `src/main/resources/css/components/actions.css`

**Decisión arquitectónica**:

- El sidebar teatral sigue dentro del `SplitPane` actual de Documento/Guión; elevar ambos sidebars a un contenedor raíz común queda como refactor mayor posterior.
- `FloatingReadingControlBar` conserva una superficie única, pero ahora posee layout horizontal y vertical; `DocumentWorkspaceView` solo decide el modo según el ancho disponible.
- `TheatreSideDock` relaja mínimos y permite que `Fragmentos visuales` use hijos flexibles, para que el usuario pueda ampliar el panel derecho sin topar con límites artificiales.

**Comportamiento preservado**:

- No cambian módulos teatrales, comandos, formato de proyecto, audio ni generación de chunks.
- El rail derecho sigue siendo colapsable, redimensionable y reutiliza el chrome común de sidebars.

**Tests corridos**:

- `mvn -q -DskipTests compile`: verde.
- `mvn -q "-Dtest=FloatingReadingControlSourceTest,DocumentSidebarsAndGlassPlaybarT105ASourceTest,TheatreScriptRf16SourceTest" test`: verde.

**Pendiente siguiente**:

- Abordar el refactor estructural donde sidebar izquierdo, documento central y sidebar teatral derecho sean hijos directos de un contenedor raíz común, con reglas compartidas de expansión y colapso.

## Ajuste RF21-07: playbar vertical fija en Guión y dock teatral amplio

**Objetivo**: evitar que la playbar horizontal invada el documento cuando el usuario trabaja con el sidebar teatral activo, y dar al workspace derecho un ancho real de trabajo desde el primer render.

**Archivos tocados**:

- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/components/FloatingReadingControlBar.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentWorkspaceView.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/theatre/TheatreSideDock.java`
- `src/main/resources/css/components/actions.css`
- `src/test/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/TheatrePlaybarLayoutSourceTest.java`

**Decisión arquitectónica**:

- `THEATRE_SCRIPT` fuerza la playbar vertical aunque el documento central todavía tenga ancho suficiente.
- El cambio vive en el chrome de documento y no en cada módulo teatral.
- `TheatreSideDock` sube ancho inicial y mínimo, pero conserva `setMaxWidth(Double.MAX_VALUE)` para que el usuario siga expandiéndolo.
- La etiqueta principal de la playbar usa una versión compacta de dos líneas solo en modo vertical.

**Comportamiento preservado**:

- No cambia reproducción, cola, generación de chunks, selección de fragmentos ni contenido interno de los módulos teatrales.

**Tests corridos**:

- `mvn -q -DskipTests compile`: verde.
- `mvn -q "-Dtest=TheatrePlaybarLayoutSourceTest,TheatreScriptRf16SourceTest,DocumentSidebarsAndGlassPlaybarT105ASourceTest,FloatingReadingControlSourceTest" test`: verde.
- `mvn -q test`: verde.

**Pendiente siguiente**:

- Validar visualmente con la app abierta y seguir afinando el módulo teatral activo cuando se trabaje en personajes, mapa textual y mapa espacial.
