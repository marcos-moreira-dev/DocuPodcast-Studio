
## XTTS Final ModelDir Guard HF3

Validar que `scripts/tts/xtts-file-to-wav.ps1` y `scripts/tts/Voz IA avanzada-file-to-wav.ps1` contengan `model-dir-termina-en-model-pth` y `model-normalizado`, y que `RunXttsReadinessSmokeUseCase`, `InspectXttsSetupReadinessUseCase`, `SettingsAwareVoiceTestSynthesisGateway` y `XttsTtsCommandTemplate` llamen a `OperationalSettingsMigrationPolicy.repair`.


## Validacion adicional RUNTIME-ARCH-RC1

- Ejecutar diagnostico completo.
- Verificar que scripts PowerShell nuevos sean ASCII-safe.
- Verificar que strings visibles de presentation no expongan nombres tecnicos de motores.
- Verificar que runtime use `RuntimeArtifactPaths`, `ExternalProcessRunner`, `ModelArtifactContract`, `ManagedDownloadService` y `OperationalSettingsMigrationPolicy` como contratos transversales.


## XTTS-LEGACY-SETTINGS-REPAIR-HF2 + XTTS-PYTORCH-CUDA-INSTALL-HF1

Se agrega `OperationalSettingsMigrationPolicy` para reparar settings persistentes antiguos: comandos localizados/legacy de Voz IA avanzada se convierten a `engineMode=xtts` con comando administrado, y `storage.modelsDirectory` terminado en `model.pth` se normaliza a carpeta padre. Esto evita que `%USERPROFILE%/.docupodcast-studio/operational-settings.properties` arrastre rutas absolutas viejas como `componentes locales IA avanzada-wrapper` o `recursos locales IA avanzada/model.pth`. También se agregan scripts `scripts/tts/setup-xtts-pytorch-cuda.ps1` y `scripts/39-preparar-pytorch-cuda-xtts.bat` para preparar PyTorch CUDA dentro del Python local, sin tocar Python global. Piper queda documentado como CPU principalmente.

## MODEL-ARTIFACT-CONTRACT-RF1 + MANAGED-DOWNLOAD-RF1 — contratos de artefactos y descargas

Se corrige primero el diagnóstico `20260607-115938`: `InspectXttsCudaSmokeUseCase` vuelve a referenciar explícitamente `XttsCudaSmokeReport.MANIFEST_NAME` sin abandonar `RuntimeArtifactPaths`, y `LocalTtsProcessConfiguration` normaliza argumentos legacy `-ModelDir`/`--model-dir` para evitar que comandos antiguos o localizados de Voz IA avanzada pasen `.../model.pth` como carpeta y produzcan `model.pth/model.pth`. Se agregan `ModelArtifactContract`, `ModelArtifactRequirement` y `RuntimeArtifactInspection` para describir artefactos locales de XTTS, Piper y FFmpeg. También se agregan contratos base `ManagedDownloadService`, `DownloadRequest`, `DownloadProgress`, `DownloadResult` y `DownloadResumePolicy` para futuras descargas transversales. Documento: `docs/productizacion/MODEL_ARTIFACT_MANAGED_DOWNLOAD_RF1.md`.

# VIDEO-SMOKE-RC1 + DIAGNOSTIC-TO-UI-HF1 + DEMO-READINESS-HF1 — actualización

Se agregó un smoke operativo para MP4 final (`InspectFinalVideoSmokeReadinessUseCase` y `scripts/38-smoke-video-final.bat`), se conectó la exportación del reporte diagnóstico a un message box mediante `DiagnosticUserDecisionFactory`, y el selector de ejemplos ahora muestra readiness operativo antes de crear el demo (`InspectExampleProjectReadinessUseCase`). Documento: `docs/productizacion/VIDEO_SMOKE_RC1_DIAGNOSTIC_TO_UI_DEMO_READINESS_HF1.md`.

---

# GPU-XTTS-RUNTIME-HF1 — actualización

Voz IA avanzada ya notifica con message box cuando el usuario pidió GPU (`Preferir GPU` o `Dispositivo específico`) pero el Python local no tiene CUDA confirmada por smoke. La app puede continuar por CPU, pero esa decisión defensiva ya no queda silenciosa. La evidencia incluye `deviceArgument`, GPU reportada por PyTorch, versión PyTorch/CUDA e issues del smoke. Documento: `docs/productizacion/GPU_XTTS_RUNTIME_HF1_DECISION_VISIBLE.md`.

---

# XTTS-MODEL-PATH-HF2 — actualización

Se agregó `XttsModelPathPolicy` para normalizar rutas de Voz IA avanzada desde Java antes de llegar a PowerShell/Python. Corrige rutas heredadas o seleccionadas que apunten a `model.pth`, evitando `model.pth/model.pth`. Se integró en readiness, comando XTTS, importación y descarga. Validación local en entorno ChatGPT: javac focal de clases y tests nuevos/modificados con stubs JUnit, source tests y prueba manual mínima de política. Maven completo no se ejecutó porque `mvn` no está instalado.

---

## Validación focal — XTTS-MODEL-PATH-HF1

Ejecutar `scripts\99-diagnostico-completo.bat` y probar Voz IA avanzada. El error `model.pth/model.pth` no debe reaparecer; si falta algún archivo del modelo, el mensaje debe apuntar a la carpeta real del modelo.

## FIRST-USE-ONBOARDING1 — validación

Validar que `Configuración inicial` prepare Voz local simple y que la guía integrada hable de audio final/video MP4. Ejecutar `scripts\99-diagnostico-completo.bat`. Para motores reales, usar el flag específico; la primera experiencia no debe depender de Voz IA avanzada.

## DOCUMENT-SIDEBAR-VOICE-UX1 — validación focal

## VOICE-CHUNKS-HF2 — barra de estado, dependencias XTTS y continuidad de buffer

- Fija `transformers==4.44.2` para el runtime local de Voz IA avanzada y verifica `BeamSearchScorer` antes de generar WAV.
- Acorta la barra de estado: `Renderizar desde aquí`, `Rehacer chunks`, `Seguir generando` y `Detalles`.
- Corrige estilo presionado de botones para evitar texto oscuro sobre fondo oscuro.
- Evita saltar al primer cue disponible cuando el usuario pidió reproducir desde un título/fragmento exacto.
- Reanuda automáticamente si un nuevo manifest recupera el buffer después de quedarse sin chunks.


Validar que el sidebar izquierdo de Documento muestre `Voz generada`, `Audio del computador`, `Voz` y `Tono`; que los ComboBox de tono usen nombres simples; y que la selección de voz filtre tonos por muestras registradas. Source test nuevo: `DocumentSidebarVoiceUx1SourceTest`.

# Validación focal — VOICE-LIBRARY-SYNC1

Ejecutar `scripts\99-diagnostico-completo.bat`. Validar manualmente: crear o importar una muestra Neutral en Vista Voces, confirmar que el archivo queda bajo `voice-library/samples/` de la app/runtime y no dentro de la carpeta del proyecto; volver a Documento y verificar que la voz aparece. Si la voz tiene Neutral/Feliz/Triste, el ComboBox de tonos debe mostrar solo esas emociones.

# Validación focal — VOICE-GPU-DEVICE-HF2

Ejecutar `scripts\99-diagnostico-completo.bat`. Validación manual: en Configuración > Rendimiento, el selector debe decir `Dispositivo para voz` y `Permitir GPU para voz`. En Vista Voces, Voz local simple debe mostrar el dispositivo solicitado y no declararse CPU-only. El wrapper de Voz local simple debe contener `-ComputePolicy`, `-Device` y `-GpuIndex`.

VOICE-CHUNKS-HF1 debe validarse con diagnóstico completo, prueba GPU sin CUDA y selección avanzada de fragmento → renderizar chunk desde fragmento seleccionado.

# Validación focal — VOICE-UX-POLISH1A

Ejecutar `scripts\99-diagnostico-completo.bat`. Validación manual: abrir Vista > Voces. En Inicio no debe aparecer `Ver estado de voces` ni `Resumen de operación`; la lista de voces debe mostrar tipo humano y solo tonos registrados. En Configurar motor, el microcopy bajo el selector debe verse con color legible y no debe aparecer la región `Estado y validación`. Los ComboBox de tonos deben mostrar nombres simples, no categorías ni códigos.

# Validación focal — MOTOR-GPU-SMOKE1-HF2

Ejecutar `scripts\99-diagnostico-completo.bat` y luego probar manualmente Configuración > Rendimiento / dispositivo > `Probar GPU para Voz IA avanzada`. El resultado ya no debe contener `SyntaxError: unterminated string literal`. Si informa CPU/CUDA no disponible, tratarlo como diagnóstico real del Python autocontenido, no como fallo del smoke.

# PLAYBACK-SPEED-HF9 — transición inmediata a 1.5x/1.75x

Validación focal: ejecutar `scripts\99-diagnostico-completo.bat`; luego probar documento con varias frases cortas a `1x`, `1.5x` y `1.75x`. A velocidades aceleradas, el siguiente chunk debe iniciar al terminar el WAV acelerado, sin silencio largo similar a duración `1x`. Tests nuevos: `PlaybackTimingPolicyTest` y `PlaybackSpeedHf9TimingPolicySourceTest`.

# DOC-INDEX-PLAYBACK-HF1 — índice respeta pivote de reproducción

Estado vigente: se corrige la continuidad posterior a MOTOR-GPU-SMOKE1. La app ya no expone nombres técnicos en strings de presentación para el smoke CUDA y `RunXttsCudaSmokeUseCase` respeta la frontera de capas. Además, al seleccionar un bloque desde el índice y pulsar `Escuchar`, `listenToDocument()` usa ese bloque como pivote y genera audio desde el segmento asociado antes de caer a generación global. Documento: `docs/productizacion/DOC_INDEX_PLAYBACK_HF1_GENERAR_DESDE_INDICE.md`.

## Validación focal — MOTOR-GPU-SMOKE1

Validar con `scripts\99-diagnostico-completo.bat`. Prueba manual opcional: abrir Configuración > Rendimiento / dispositivo y pulsar `Probar GPU para Voz IA avanzada`; también se puede ejecutar `scripts\37-smoke-cuda-xtts.bat`. Si CUDA no está disponible dentro de `tools\xtts-wrapper\.venv`, la app debe informar que GPU es candidata/no confirmada y mantener Voz IA avanzada en CPU.

## Validación focal — MOTOR-ADV-READY-GATE1

En entorno ChatGPT no se ejecutó Maven por falta de `mvn`. Validación realizada: `javac --release 21` del núcleo `application/domain/infrastructure`; compilación focal de tests nuevos/modificados con stubs JUnit; ejecución reflexiva de 11 métodos de test. Revalidar localmente con `scripts\99-diagnostico-completo.bat`.

# DOC-PERF/MEM1 — índices ligeros para documentos grandes

Estado: implementada sobre PLAYBACK-SELECT1/MODEL-PORT1. Corrige el diagnóstico `20260606-170202.zip` reduciendo `DocuPodcastShellViewModel` bajo el límite transitorio RF-TX1 y optimiza el lector para documentos grandes con `blockIndexById` y `sentenceSpanIndex`, evitando reescaneos completos durante navegación, selección y actualización visual. Documento: `docs/productizacion/DOC_PERF_MEM1_INDICES_LIGEROS.md`.

# DOC-INDEX-HF13 — índice navegable del documento

Estado: implementada sobre EXPORT-CLEAN1 verde. Se agrega `DocumentIndexPanel` como módulo lateral plegable `Índice` dentro del workspace Documento. Usa `TreeView<IndexEntry>` para títulos/secciones/subsecciones; cuando no hay encabezados, ofrece navegación plana por bloques narrables y visuales. El salto llama a `selectBlock(...)`, por lo que funciona también con la ventana virtualizada de documentos grandes. No se reintroduce el panel viejo `Estructura documental`. Documento: `docs/productizacion/DOC_INDEX_HF13_INDICE_NAVEGABLE.md`.

# EXPORT-CLEAN1 — exportaciones técnicas fuera del flujo común

Base actual: MOTOR-SMOKE4R-HF5 verde. Esta tanda concentra el flujo normal de exportación en salidas finales de producto: audio final y video MP4. `Exportar paquete de soporte`, `Exportar reporte de soporte` y `Validar integridad` quedan disponibles bajo `Ayuda > Soporte avanzado`, no en el menú Exportar, toolbar ni Ribbon. Documento técnico: `docs/productizacion/EXPORT_CLEAN1_EXPORTACIONES_SOPORTE_AVANZADO.md`.

# Validación FFMPEG-PREP1

Entorno ChatGPT sin Maven instalado. Validación focal ejecutada:

- `javac` de dominio + aplicación + infraestructura: OK.
- `javac` focal de `InfrastructureServices` + `ApplicationServicesFactory`: OK.
- Tests focales con stubs JUnit: `ffmpeg-prep-tests-ok=4`.
- Guardarraíl nuevo: `FfmpegPrep1DownloadPreparationSourceTest`.
- Test nuevo: `FfmpegRuntimeDownloadReportTest`.

Validación completa pendiente en Windows:

```bat
scripts\99-diagnostico-completo.bat
```

Validación manual: Configuración > Motores de voz y media > Video local > Preparar debe descargar desde la URL configurada, copiar componentes dentro del programa y dejar Video local listo o mostrar detalle verificable.

# VOZ-UX4R-3D — catálogo teatral de tonos en Vista Voces

# MOTOR-SMOKE4R-HF2 — etiqueta visible de modelo sin nombres técnicos

- Hotfix sobre HF1 tras diagnóstico `20260606-121332`: Configuración ya no muestra `Página del modelo XTTS-v2` ni `/resolve/main` como ayuda visible. La UI dice `Página oficial del modelo de voz` y conserva la normalización técnica solo por dentro. Documento: `docs/productizacion/MOTOR_SMOKE4R_HF2_PRESENTACION_URL_MODELO_VOZ.md`.


# Validación VOZ-UX4R-3D

Entorno ChatGPT sin Maven instalado. Validación focal ejecutada:

- `javac --release 21` de dominio + aplicación + `VoiceSampleWorkflowCoordinator`: `javac-main-ok`.
- Source tests productization/presentation voice/document con stubs JUnit: `source-tests-ok=452`.
- Nuevo guardarraíl: `VoiceUx4R3DTheatricalToneCatalogSourceTest`.

Validación completa pendiente en Windows:

```bat
scripts\99-diagnostico-completo.bat
```

Base verde anterior: VOZ-TTS5B. Esta tanda habilita el catálogo teatral extendido en el ComboBox de tonos de Vista Voces, mantiene filas sobrias sin dashboard y agrega el guardarraíl `VoiceUx4R3DTheatricalToneCatalogSourceTest`. No toca motores, playback ni Documento.

# Validación VOZ-TTS5B

Validación estática ejecutada en entorno sin Maven instalado. Resultado focal: compilación sin JavaFX de dominio + aplicación + infraestructura/audio + infraestructura/json con stubs JUnit.

- `AudioGenerationRequestVoiceReferenceSampleTest` — verifica resolución de muestra exacta por tono y fallback a Neutral.
- `VoiceTts5BRenderUsesReferenceSampleSourceTest` — protege que el Shell pase `voiceLibrary`, que el request resuelva muestra por `performanceStyleId`, y que el gateway pase `referenceSample` al comando TTS real.

Validación completa pendiente en Windows:

```bat
scripts\99-diagnostico-completo.bat
```

# Validación VOZ-TTS5A

Validación estática ejecutada en entorno sin Maven instalado. Resultado focal: `source-tests-ok=13`.

- `VoiceTts5ADocumentRealVoiceToneSourceTest` — Documento no usa catálogo global de tonos, exige muestra Neutral para Voz IA avanzada y documenta la tanda.
- Revisión fuente: `DocumentAudioNarrationPanel` usa `registeredTones()` y `referenceSampleSetByVoiceId(...).hasNeutral()`.

Validación completa pendiente en Windows:

```bat
scripts\99-diagnostico-completo.bat
```

# Validación VOZ-UX4R-3C-HF1

El diagnóstico local `20260606-095144.zip` reportó un fallo único de compilación en `VoiceLibraryWorkspaceView.java`: `cannot find symbol: variable summary`. El hotfix elimina la referencia residual y refuerza `VoiceUx4R3CAntiDashboardSourceTest` para impedir que vuelva `box.getChildren().addAll(summary, list);`.

Validación en entorno ChatGPT: `source-tests-ok=15`. Maven completo no se ejecutó porque `mvn` no está instalado. Validación obligatoria en Windows: `scripts\99-diagnostico-completo.bat`.

# Validación VOZ-UX4R-3C — sidebar oscuro y anti-dashboard de Voces

Validación esperada en Windows: `scripts\99-diagnostico-completo.bat`. Guardarraíl focal: `VoiceUx4R3CAntiDashboardSourceTest`. Confirmar visualmente: Vista > Voces abre con sidebar oscuro full-height, botones Inicio/Configurar motor/Gestionar voces sin descripciones incrustadas, Inicio no muestra tarjetas de métricas ni dashboard, y Configurar motor muestra filas sobrias de estado.

# Validación VOZ-UX4R-DOC1 — contrato final modular de Vista Voces

Validación esperada: `scripts\99-diagnostico-completo.bat`. Guardarraíl documental: `VoiceUx4RFinalContractDocumentationSourceTest`. Debe existir `docs/productizacion/VOZ_UX4R_CONTRATO_FINAL_MODULOS.md` y contener: tres módulos principales, regla de filas sobrias, muchas emociones, Neutral obligatoria, reemplazo de emoción por importar/grabar, eliminación con message box y archivos asociados, selector CPU/GPU real para todos los motores, y filtrado de voces/emociones reales en Documento.


## PLAYBACK-SPEED1 — controles de velocidad en playbar

Se agregan botones `1x`, `1.5x` y `1.75x` en la playbar del Documento. La velocidad afecta playback de WAV ya generados, no generación TTS. `JavaSoundSegmentAudioPlayer` aplica velocidad simple por frecuencia efectiva de salida, `PlaybackCueClock` sincroniza cursor/visual al ritmo acelerado y el buffer existente sigue esperando si la lectura rápida alcanza un chunk pendiente. Documento: `docs/productizacion/PLAYBACK_SPEED1_CONTROLES_VELOCIDAD.md`.

# Validación DOC-UX-HF10H — progreso legible, tamaño generado y rail Visual honesto

- Ejecutar `scripts\99-diagnostico-completo.bat`.
- Abrir documento grande y generar chunks.
- Confirmar que el overlay se puede ocultar.
- Confirmar que la barra de progreso es visible, más alta y verde.
- Confirmar que la ETA usa horas cuando supera 60 minutos.
- Confirmar que el texto dice audio generado en disco y no tamaño estimado.
- Confirmar que el rail Visual muestra contador de fragmentos, scroll vertical y no scroll horizontal.
- Confirmar que Configuración > Video local aclara URL vs importar carpeta.

# Validación DOC-UX-HF10G — memoria, rail virtual, overlay operable y tablas sin truncado

Validación esperada en Windows: `scripts\99-diagnostico-completo.bat`.

Puntos focales:

- `DocUxHf10GMemoryRailOverlayTablesSourceTest` protege rail virtualizado, throttle de audio, tablas sin truncado y heap local.
- `DocumentMediaRailView` debe usar `ListView<DocumentFragmentRailPresentation>` y no un `VBox` que materialice una tarjeta por frase.
- `AudioStatusUiThrottle` debe coalescer estados y entregar a JavaFX con `Platform.runLater`.
- `DocuPodcastShellViewModel` no debe usar `playbackCursor.get() != null` como razón genérica para reconstruir manifest durante generación pura.
- `SourceTableGridView` debe preferir crecimiento vertical (`wrapText`, `OverrunStyle.CLIP`, `Region.USE_PREF_SIZE`).
- `scripts\01-ejecutar-app.bat` debe permitir override con `DOCUPODCAST_APP_HEAP=-Xmx4096m` para documentos enormes.
- Prueba manual: abrir documento grande, generar chunks, ocultar/reabrir overlay y confirmar que la UI no queda congelada.

Documento: `docs/productizacion/DOC_UX_HF10G_MEMORIA_RAIL_OVERLAY_TABLAS.md`.

# Validación PLAYBACK-HF8R — cola por duración real del WAV

Validación esperada en Windows: `scripts\99-diagnostico-completo.bat`.

Puntos focales:

- `WavAudioDurationProbeTest` verifica lectura de duración desde `fmt ` + `data`.
- `AudioJobFileRepositoryDurationRepairTest` verifica que un job viejo con duración estimada se repare desde el WAV persistido.
- `PlaybackDurationQueueHf8RSourceTest` protege que el TTS real use duración medida y no `estimatedDuration(segment)`.
- `AudioJobFileRepository` repara duración de WAVs persistidos al listar/cargar jobs.
- `DocuPodcastShellViewModel` usa `PlaybackCueClock` para bloquear el siguiente chunk mientras el actual no termine.
- Prueba manual: generar varios chunks, pulsar **Escuchar documento** y confirmar que no se pisan.
- Prueba manual pendiente separada: Configuración debe descargar/preparar Voz IA avanzada y generar un WAV reproducible. Esta tanda no ejecuta descarga real en entorno ChatGPT.

Documento: `docs/productizacion/PLAYBACK_HF8R_COLA_DURACION_REAL.md`.

# Validación PLAYBACK-HF7 — reproducción secuencial por fragmentos

Validación esperada en Windows: `scripts\99-diagnostico-completo.bat`.

Puntos focales:

- `JavaSoundSegmentAudioPlayer` usa `SourceDataLine.getMicrosecondPosition()` como reloj.
- `JavaSoundSegmentAudioPlayer` usa `playbackGeneration` para aislar hilos viejos.
- `DocuPodcastShellViewModel` no inicia playback bufferizado mientras el reproductor está sonando.
- `PlaybackSequentialSessionHf7SourceTest` protege que no se vuelva a medir progreso por bytes escritos al buffer.

Documento: `docs/productizacion/PLAYBACK_HF7_REPRODUCCION_SECUENCIAL.md`.

# Validación UX-HF7 + PLAYBACK-HF6 — Diagnóstico verde y reuso de audio generado

Validación esperada en Windows: `scripts\99-diagnostico-completo.bat`.

Puntos focales:

- `BuildPlaybackManifestUseCaseTest.userAudioClipUnitsBecomeEffectivePlaybackCues` vuelve a pasar.
- `PlaybackManifestReuseHf6SourceTest` protege que Documento reconstruya el manifest antes de declarar audio faltante.
- Los source tests de Settings se alinean con botones cortos y sin puntos suspensivos.
- No hay strings visibles técnicos de motores en presentación.

Documento: `docs/productizacion/PLAYBACK_HF6_REUSO_WAVS_Y_MANIFEST_ACTIVO.md`.

# Validación UX-HF6 + DOWNLOAD-HF5 — Build verde y descarga visible

Validación esperada en Windows: `scripts\99-diagnostico-completo.bat`.

Puntos focales:

- `DocuPodcastShellViewModel` no captura variables mutables en lambdas de playback.
- `SettingsDialog` tiene resumen detallado para Voz local simple y no mezcla reportes de Voz IA/Voz local.
- La descarga de Voz IA avanzada conserva `XttsModelDownloadReport` y muestra el recurso pendiente si falla.
- El ViewModel queda bajo el límite RF2 de líneas.

Documento: `docs/productizacion/UX_HF6_DOWNLOAD_HF5_BUILD_VERDE_DESCARGA_VISIBLE.md`.

# Validación UX-HF5 + PLAYBACK-HF5

Validación esperada en Windows: `scripts\99-diagnostico-completo.bat`.

Puntos focales:

- `BuildPlaybackManifestUseCase` crea cues desde unidades TTS generadas.
- `DocuPodcastShellViewModel` refresca manifest cuando se completan nuevos fragmentos.
- `FloatingReadingControlBar` y `TransportControls` usan botones secundarios con iconos PNG y tooltips.
- `SettingsDialog` evita diálogos estrechos y agrega preparación de video local.
- `WelcomeWorkspaceView` carga logo transparente de fondo.

Source tests agregados: `PlaybackManifestRenderUnitsHf5SourceTest` y `UiPolishAndVideoLocalHf5SourceTest`.

# Validación UX-HF4 + PLAYBACK-HF4

Validación esperada en Windows: `scripts\99-diagnostico-completo.bat`.

Puntos focales:

- `WrappedSetupDialogsUxHf4SourceTest` protege diálogos legibles y setup encadenado.
- `PlaybackNextPreviousUxHf4SourceTest` protege botones de fragmento anterior/siguiente y reproductor interno por streaming.
- `DocuPodcastShellViewModel` se mantiene bajo el límite transitorio de 2650 líneas hasta RF-TX1.
- `JavaSoundSegmentAudioPlayer` usa `SourceDataLine` en vez de depender solo de `Clip`.

Documento: `docs/productizacion/UX_HF4_DIALOGOS_SETUP_Y_PLAYBACK_FRAGMENTOS.md`.

# Validación UX-HF3 — Build verde y reproductor interno WAV

Validación esperada en Windows: `scripts\99-diagnostico-completo.bat`.

Puntos focales:

- `SettingsDialog` no contiene strings multilínea sin escapar en configuración inicial.
- `DocuPodcastShellViewModel` permanece bajo el límite RF2.
- `JavaSoundSegmentAudioPlayer` reporta formato WAV y causa humana si no puede reproducir.

Documento: `docs/productizacion/UX_HF3_BUILD_Y_REPRODUCTOR_INTERNO.md`.

# Validación UX-HF2 — descarga, normalización y streaming

Validación esperada en Windows: `scripts\99-diagnostico-completo.bat`.

Puntos focales:

- `DownloadXttsOfficialModelUseCase` no bloquea la voz por recursos opcionales faltantes y reporta los recursos obligatorios que sí fallan.
- `SettingsDialog` muestra mensajes iniciales legibles con saltos de línea y detalle humano de descarga.
- `TtsTextPreprocessor.sanitizeForEngine` normaliza tildes y ñ solo para Voz local simple/Piper.
- `DocuPodcastShellViewModel` intenta reproducir fragmentos tan pronto hay WAVs suficientes y conserva errores reales del reproductor interno.

Documento: `docs/productizacion/UX-HF2_AUDIO_STREAMING_Y_DESCARGA_VOZ.md`.

# Validación UX-RIBBON3 + DOC-RAIL6

Validación esperada en Windows: `scripts\99-diagnostico-completo.bat`.

Puntos focales:

- `RibbonAndRailCleanupUxRibbon3SourceTest` protege que el Ribbon no vuelva a exponer `TOGGLE_RIGHT_RAIL` como botón transversal.
- `DocumentRightRailCleanDocRail6SourceTest` protege que el rail derecho del Documento no vuelva a mostrar una sección genérica `Imágenes`.
- Los source tests desalineados con `SettingsAwareAudioGenerationGateway` se actualizan a la arquitectura actual.
- El panel visual conserva su control interno y su estado bidireccional con el ViewModel.

Documento: `docs/productizacion/UX_RIBBON3_DOC_RAIL6_RIBBON_Y_PANEL_VISUAL_LIMPIOS.md`.

# Validación UX-SETUP3 — Motores vivos y Voz local automática

Validación esperada en Windows: `scripts\99-diagnostico-completo.bat`.

Puntos focales:

- Configuración refresca la página activa tras preparar, descargar, importar o seleccionar motor.
- Voz local simple tiene preparación automática con progreso y selección persistente.
- `SettingsAwareAudioGenerationGateway` lee la configuración guardada al iniciar cada job de audio.
- `SettingsAwareVoiceTestSynthesisGateway` lee la configuración guardada antes de sintetizar una prueba corta.
- `PiperAutomaticSetupUxSetup3SourceTest` y `SettingsAwareTtsRuntimeUxSetup3SourceTest` protegen el cierre.

Documento: `docs/productizacion/UX_SETUP3_MOTORES_VIVOS_Y_PIPER_AUTOMATICO.md`.

# Validación UX-HF1 + UX-SETUP2

Validación esperada en Windows: `scripts\99-diagnostico-completo.bat`.

Puntos focales:

- `UserFacingScaffoldCleanupUxHf1SourceTest` protege que Inicio/Configuración no expongan andamios técnicos.
- `FirstUseSetupUxSetup2SourceTest` protege `showFirstUseSetup`, `showVoiceEngines` y el flujo de preparación inicial.
- Los 2 fallos reportados por copy técnico visible quedan corregidos en `SettingsDialog`.
- `Guía rápida` ya no queda con acción nula.

Documento: `docs/productizacion/UX_HF1_SETUP2_INICIO_LIMPIO_CONFIGURACION_INICIAL.md`.

# Validación PF5C + PF6B — Motores guiados y smoke GUI asistido

Validación esperada en Windows: `scripts\99-diagnostico-completo.bat`. Para release candidate: `scripts\16-release-candidate.bat`.

Puntos focales:

- `GuidedEngineCompletionPf5CSourceTest` protege importación in-app de Voz local simple y FFmpeg.
- `GuiSmokeAssistedPf6BSourceTest` protege el checklist de smoke GUI real.
- Configuración expone `Importar voz local...`, `Importar FFmpeg...` y `Generar checklist smoke GUI`.
- El release candidate invoca `scripts\36-smoke-gui-asistido.bat` como apoyo técnico; la validación principal se completa desde la app.

Documentos: `docs/productizacion/PF5C_PREPARACION_GUIADA_COMPLETA_MOTORES.md` y `docs/productizacion/PF6B_SMOKE_GUI_ASISTIDO.md`.

# Validación PF5B + PF6A — Icono de producto y branding portable

Validación esperada en Windows: `scripts\99-diagnostico-completo.bat` y, si se desea cerrar branding de release, `scripts\14-app-image-completa.bat`, `scripts\32-preparar-app-portable-layout.bat` y `scripts\34-smoke-app-portable-runtime.bat`.

Puntos focales:

- `DocuPodcastStudioApp` carga iconos PNG desde `src/main/resources/branding/`.
- `ProductIconBrandingPf5BSourceTest` protege la carga del icono y el uso de `packaging\windows\docupodcast-icon.ico` en app-image/MSI.
- `PortableBrandingPf6ASourceTest` protege la copia/verificación del branding en portable y smoke.
- El branding se preserva para Stage, app-image, MSI y carpeta portable.

Documento: `docs/productizacion/PF5B_PF6A_ICONO_PRODUCTO_Y_BRANDING_PORTABLE.md`.

# Validación PF4B + PF5A — Smoke portable y auditoría de motores

Validación esperada en Windows: `scripts\99-diagnostico-completo.bat`.

Puntos focales corregidos/validados en esta tanda:

- Corrige `illegal escape character` en `SettingsDialog`.
- `PortableRuntimeSmokePf4BSourceTest` protege el smoke post-app-image y el launcher con `DOCUPODCAST_APP_ROOT`.
- `EngineArtifactAuditPf5ASourceTest` protege la auditoría local de motores desde Configuración y script técnico.
- `scripts\16-release-candidate.bat` ejecuta `34-smoke-app-portable-runtime.bat` y `35-auditar-artefactos-motores.bat`.
- Configuración → Motores de voz incluye `Inventario local de motores` y reporte `target\legal\ENGINE_ARTIFACTS_AUDIT.md`.

Documentos: `docs/productizacion/PF4B_SMOKE_APP_PORTABLE_RUNTIME.md` y `docs/productizacion/PF5A_AUDITORIA_ARTEFACTOS_MOTORES.md`.

# Validación PF2E + PF3B — Progreso real en Configuración y guardarraíles de Voces

Validación esperada en Windows: `scripts\99-diagnostico-completo.bat`.

Puntos focales corregidos/validados en esta tanda:

- Los 5 fallos reportados en `20260604-024335.zip` quedan alineados.
- `SettingsOperationProgressPf2ESourceTest` protege progreso vivo en Configuración.
- `PrepareXttsPortableRuntimeUseCase` emite latidos y salida del instalador interno mientras trabaja.
- `DownloadXttsOfficialModelUseCase` informa archivo actual y avance por bloques descargados.
- `ImportXttsModelFolderUseCase` informa validación, copia y verificación.
- `scripts\04-verificar-tts-config.bat` remite a Configuración como ruta normal.
- `DocuPodcastShellViewModel` vuelve a quedar bajo el límite de deuda de RF2.

Documento: `docs/productizacion/PF2E_PROGRESS_CONFIGURACION_Y_PF3B_VOCES.md`.

# Validación PF1B + PF2A — Hotfix compile y Voz IA avanzada portable

Validación esperada en Windows: `scripts\99-diagnostico-completo.bat`.

Puntos focales corregidos/validados en esta tanda:

- El error de compilación reportado por Maven queda corregido en `ScriptWorkspaceView`.
- `ScriptWorkspaceStoryboardHotfixPf1BSourceTest` protege que no vuelva `VisualesDocument/currentVisualesProperty`.
- `VoiceAiPortableRuntimePf2ASourceTest` protege el asistente local de Voz IA avanzada, instalación desde repo TTS local y verificación estricta del modelo.
- `check_xtts_runtime.py` exige `config.json`, `model.pth` y `vocab.json`.
- `scripts\04-verificar-tts-config.bat` lee configuración operacional y no anuncia mock de forma engañosa.

Documento: `docs/productizacion/PF1B_PF2A_HOTFIX_VOZ_IA_PORTABLE.md`.

# Validación PF1 — Voz generada real en Vista Voces

Validación esperada en Windows: `mvn test` y `scripts\99-diagnostico-completo.bat`. En este entorno no hay `mvn`, por lo que se realizó validación focal.

Puntos focales validados:

- `GenerateVoiceTestUseCase` compila sin ruta de WAV sintético propio.
- `VoiceTestSynthesisGateway`, `VoiceTestSynthesisRequest` y `VoiceTestSynthesisResult` compilan.
- `LocalProcessVoiceTestSynthesisGateway` compila y usa `ProcessBuilder`.
- `LocalTtsProcessConfiguration.commandForVoiceTest(...)` compila y soporta `{speakerWav}`/muestra de referencia.
- `ApplicationServicesFactory` inyecta `infrastructure.voiceTestSynthesisGateway()`.
- `VoiceGeneratedTestRealSynthesisPF1SourceTest`, `GenerateVoiceTestUseCaseTest`, `VoiceGeneratedTestT121V05SourceTest` y `VoiceLocalSimpleT121V06SourceTest` pasaron en ejecución focal con stubs JUnit.

Documento: `docs/productizacion/PF1_VOZ_GENERADA_REAL_VOCES.md`.

# Validación TV1 — Visuales/video honesto

Validación esperada en Windows: `mvn test` y `scripts\99-diagnostico-completo.bat`. En este entorno se realiza validación focal por `javac` y source tests.

Puntos focales:

- `VideoHonestyTv1SourceTest` debe pasar.
- `VisualLanguageTv1SourceTest` debe pasar.
- `SimpleVideoPackageExportResult` debe exponer estado honesto del paquete.
- `ExportWorkflowCoordinator` debe decir **Paquete de video simple preparado** y no prometer MP4 creado.
- La UX normal debe usar Visuales / Secuencia visual.

Documento: `docs/productizacion/TV1_LIMPIEZA_VISUALES_VIDEO_HONESTO.md`.

# Validación TE1 — Exportaciones alineadas a voces por tono

Validación esperada en Windows: `mvn test` y `scripts\99-diagnostico-completo.bat`. En este entorno se realizó validación focal por `javac`, source tests y smoke manual de bundle.

Puntos focales:

- `BuildVoiceReferenceSamplesExportReportUseCaseTest` debe pasar.
- `ExportVoiceSamplesTe1SourceTest` debe pasar.
- El bundle debe generar `reports/VOICE_REFERENCE_SAMPLES.md` y `reports/VOICE_REFERENCE_SAMPLES.tsv`.
- `MANIFEST_EXPORTACION.md` debe incluir el conteo de muestras de voz por tono.
- `ProjectBundleExportResult.voiceReferenceSampleCount()` debe reflejar las muestras registradas.
- La GUI normal no debe mostrar nombres técnicos de motores.

Documento: `docs/productizacion/TE1_EXPORTACIONES_ALINEADAS_VOCES_TONO.md`.

# TC1 — CommandAvailabilityPolicy

Estado: implementada sobre RF5 verde.

Se agrega `CommandAvailabilityPolicy` como fuente única de disponibilidad para comandos visibles. Menú, Ribbon y `WorkspaceCapabilityPolicy` consumen la misma política, evitando reglas duplicadas entre superficies. La tanda no cambia UX, CSS ni componentes visuales; conserva nombres amigables de motores y agrega razones humanas para futuras tooltips/status.

Documento: `docs/productizacion/TC1_COMMAND_AVAILABILITY_POLICY.md`.

# Validación RF5 — Limpieza residual STT/Whisper

Validación esperada: `scripts\99-diagnostico-completo.bat` en Windows.

Puntos focales:

- `ResidualWhisperSttCleanupRf5SourceTest` debe pasar.
- `NoWhisperSttT114Hf2SourceTest` debe seguir pasando.
- `VoiceSourceKind` no debe contener `HUMAN_RECORDING_FOR_TRANSCRIPTION`.
- `src/main/java` y `scripts` no deben contener Whisper, SpeechToText, `SPEECH_TO_TEXT`, `allowGpuForStt` ni `audio a texto`.
- Audio del computador sigue siendo clip asociado al documento, no transcripción.

Documento: `docs/productizacion/RF5_LIMPIEZA_RESIDUAL_STT_WHISPER.md`.

# RF4 — Limpieza de workspaces heredados

Estado: implementada sobre RF3 verde.

Se cierra la cuarentena real de workspaces heredados: `WorkspaceViewRegistry` rechaza factories para superficies internas, `viewFor(...)` normaliza solicitudes heredadas hacia superficies de producto y `DocuPodcastShellViewModel` deja de exponer métodos públicos de apertura de Guion interno, Procesos de audio o Secuencia visual como workspaces. El Shell sigue sin registrar handlers para comandos legacy ocultos. No se borran paquetes internos para preservar compatibilidad.

Documento: `docs/productizacion/RF4_LIMPIEZA_WORKSPACES_HEREDADOS.md`.

# RF2 — Coordinadores de presentación / selección documental

Estado: implementada sobre RF1.

Se agrega `DocumentSelectionCoordinator` para mover fuera de `DocuPodcastShellViewModel` las reglas de presentación de selección documental: previews, etiquetas de oración/fragmento/capa, ubicación fuente y copy de la acción primaria del documento. Se corrige el source test de T102 para reconocer que la persistencia del zoom de lectura ya vive en `ReadingComfortCoordinator`. No cambia la UX ni CSS, y la GUI sigue sin mostrar nombres técnicos de motores.

Documento: `docs/productizacion/RF2_COORDINADORES_PRESENTACION_DOCUMENT_SELECTION.md`.

# Validación T121-V10 — Vista Voces final

Validación esperada: `mvn test` y `scripts\99-diagnostico-completo.bat` en Windows. En este entorno se realizó validación focal por javac/source tests. Smoke manual: `docs/testeo/SMOKE_VISUAL_VOCES_FINAL.md`.

Documento: `docs/productizacion/T121_V10_TESTS_DOCUMENTACION_SMOKE_VOCES.md`.

# T121-V09 — Componentes/CSS de Voces

Estado: implementada sobre T121-V08.

La vista Voces extrae componentes específicos reutilizables (`VoiceProfileCard`, `VoiceEngineModeCard`, `VoiceToneBadge`, `VoiceSampleRow`, `VoiceGeneratedTestPanel`) y los registra en el catálogo transversal. `VoiceGeneratedTestPanel` reutiliza `SectionHeader` y `ActionBar`; se amplía `voice-library.css` sin `setStyle` y se corrige el contrato exacto de Voz local simple: no usa muestras humanas. La GUI conserva nombres amigables: Voz IA avanzada, Voz local simple y Modo de prueba.

Documento: `docs/productizacion/T121_V09_COMPONENTES_CSS_VOCES.md`.

# T121-V08 — Rediseño visual final de Voces

Estado: implementada sobre T121-V07.

La vista Voces queda reorganizada como pantalla de producto con hero, tarjetas de modo, detalle de voz seleccionada y matriz de muestras por tono. La GUI mantiene nombres amigables: Voz IA avanzada, Voz local simple y Modo de prueba.

Documento: `docs/productizacion/T121_V08_REDISHENO_VISUAL_FINAL_VOCES.md`.

# T121-V07 — Fallback de tonos faltantes en Documento

Se conecta Documento con el modelo real de tonos por muestra: el panel Audio usa `VoiceReferenceTone` en vez de estilos legacy, muestra **Tono de referencia** y resuelve el tono seleccionado con fallback neutral. Si la muestra exacta existe, se usa; si falta pero hay neutral, la asignación queda guardada con aviso de fallback neutral; si falta neutral, se bloquea la asignación avanzada. También se corrige la compilación de V06 por imports faltantes en `DocuPodcastShellViewModel`. La interfaz gráfica mantiene nombres amigables y no muestra nombres técnicos de motores.

Documento: `docs/productizacion/T121_V07_FALLBACK_TONOS_DOCUMENTO.md`.

# T121-V06 — Voz local simple mínima

Se implementa el modo mínimo de **Voz local simple** en la Vista Voces: cuando ese modo está activo, la UI oculta wizard avanzado, tonos, muestras humanas, catálogo teatral, clonación y estilos expresivos. La vista muestra una sección mínima con estado del modelo, frase editable, `Probar lectura simple` y `Reproducir última prueba`. `GenerateVoiceTestUseCase` ahora puede generar una prueba simple auditable en `voices/generated-tests/local-simple/` sin exigir muestra humana. Además, `SettingsDialog` usa etiquetas amigables para el modo de motor y se refuerza que la interfaz gráfica no muestre nombres técnicos de motores. Se extrae `VoiceSampleWorkflowCoordinator` para bajar la deuda de `DocuPodcastShellViewModel` y corregir el guardarraíl de líneas.

Documento: `docs/productizacion/T121_V06_VOZ_LOCAL_SIMPLE_MINIMA.md`.

# T121-V05 — Prueba generada con frase editable

Se agrega en Vista Voces un bloque de prueba generada con frase editable: `TextArea`, selector de tono reutilizado, botón `Generar prueba con esta voz` y `Reproducir última prueba`. La generación usa `GenerateVoiceTestUseCase`, resuelve la muestra con `ResolveVoiceToneReferenceUseCase`, cae a neutral si falta el tono solicitado y bloquea la prueba si `Voz IA avanzada` no está lista. Los artefactos se guardan en `voices/generated-tests/<voice>/` dentro del proyecto. También se actualizan tests heredados que seguían esperando nombres técnicos visibles.

Documento: `docs/productizacion/T121_V05_PRUEBA_GENERADA_FRASE_EDITABLE.md`.

# T121-V04C — Cableado visual mínimo del wizard por tono

Se conecta la Vista Voces con `VoiceRegistrationWizardPlan` y `VoiceToneRecordingPlan`: selector de tono, frase guía por tono, importación/grabación usando el tono seleccionado y conteo de muestras por tono registradas. Se elimina la frase estática anterior y se mantiene el lenguaje visible `Voz IA avanzada`, `Voz local simple` y `Modo de prueba`, sin `Coqui`/`XTTS` en Voces. También se corrige el source test de V04B que falló localmente por literal escapado.

# T121-V04B — Persistencia real de muestras por tono

Se agrega persistencia durable para muestras de voz por tono: `VoiceLibrary.referenceSampleSets`, importación con `VoiceReferenceTone`, assets por voz+tono, serialización en `.docupodcast.json` y materialización en `voices/voice-library.json`. La muestra neutral actualiza `VoiceProfile.sampleAssetId` para compatibilidad legacy; las muestras no neutrales ya no sobrescriben la neutral. Esta tanda prepara el wizard visual por tono y el fallback de Documento.

# T121-V04 — Wizard de registro de voz avanzada

Se agrega scaffolding de aplicación para planificar el wizard de registro de voces avanzadas: plan de wizard visible como “Voz IA avanzada”, frase guía por tono, plan de grabación por tono, etiquetas de cancelar/detener/guardar y garantía de que cancelar no reemplaza la muestra anterior. La UX no muestra Coqui/XTTS.


# Nota T121-V03 — Almacenamiento seguro y descarga de muestras

Se agregó infraestructura para descargar/exportar muestras de voz a una carpeta elegida por el usuario y eliminar solo muestras gestionadas por DocuPodcast. Los archivos externos originales no se borran. La UI futura debe usar DirectoryChooser para Descargar muestra.

# Nota T121-V02 — Modelo de voces, muestras y tonos teatrales

Se implementó la base de dominio para voces avanzadas: `VoiceReferenceTone`, catálogo básico y teatral extendido, frases guía por tono, `VoiceReferenceSample`, `VoiceReferenceSampleSet`, origen de muestra, propiedad de archivo y nombres UX seguros (`Voz IA avanzada`, `Voz local simple`, `Modo de prueba`). La UI no debe mostrar `Coqui` ni `XTTS`.

## T121-V01 — Deshuesamiento de la vista Voces actual

Se limpió la vista Voces para que deje de asignar fragmentos y de mostrar personajes/roles/estilos heredados. Voces queda como biblioteca de voces y muestras; Documento conserva la asignación a fragmentos. En la UX visible se usan “Voz IA avanzada”, “Voz local simple” y “Modo de prueba”, sin mostrar `Coqui` ni `XTTS`.

# Nota T120D-HF1 — Corrección CSS warnings JavaFX

Se corrigieron aliases CSS faltantes que generaban advertencias JavaFX al abrir superficies como ejemplos y configuración. La hotfix no rediseña la UI; solo estabiliza tokens para preparar el rediseño de Voces.


# Nota agregada — Frases guía por tono en Vista Voces

La planificación de Voces ahora exige que cada tono del catálogo teatral extendido tenga una frase guía por defecto. Al grabar una muestra, la app muestra la frase correspondiente al tono; el usuario la lee y luego puede cancelar, detener o guardar. Cancelar no reemplaza la muestra anterior; guardar asocia la grabación al tono.

# Nota de planificación T121 — Vista Voces UX/UI final

Se agregó documentación detallada en `docs/productizacion/voces_ux_final/`. La vista Voces debe reconstruirse como módulo de gestión de voces/muestras: no mostrar `Coqui` en UX, usar “Voz IA avanzada”, Piper mínimo como “Voz local simple”, Mock como “Modo de prueba”, catálogo teatral extendido de tonos, descarga de muestras con DirectoryChooser, prueba generada con frase editable y fallback a tono neutral si falta una muestra.

## Estado vigente — T120C

Base vigente: T120C — Aplicación del Ribbon final.

Reglas agregadas:

- El Ribbon queda aplicado por intención de usuario: Inicio, Lectura, Vista y Exportar.
- `Preparar lectura` reemplaza el lenguaje ambiguo `Preparar audio` en el catálogo de comandos y toolbar contextual.
- `Voces` se trata como biblioteca/gestión de voces; se retira la etiqueta `Voces y personajes` del contrato principal.
- `Cancelar generación` describe el job activo con más precisión que `Cancelar audio`.
- Storyboard no puede volver al Ribbon: `CREATE_STORYBOARD` y `OPEN_STORYBOARD` dejan de declarar Ribbon como superficie permitida.
- El panel visual sigue siendo parte del Documento, no una pestaña propia.

Documento: `docs/productizacion/T120C_APLICACION_RIBBON_FINAL.md`.

---

## Estado vigente — T119

Base vigente: T119 — Configuración honesta y accionable.

Reglas agregadas:

- Configuración deja de tratar los motores como lista de promesas: Coqui/XTTS, Piper y FFmpeg tienen botones reales de verificación/selección cuando corresponde.
- Las tarjetas de catálogo de motores se etiquetan explícitamente como guía informativa, no como botones falsos.
- La línea de comandos externa queda reservada para Diagnóstico avanzado; el usuario normal debe preparar Coqui/XTTS, Piper y FFmpeg desde asistentes reales.
- FFmpeg se puede verificar desde Configuración usando el probe de runtime que consulta versiones y encoders.
- La configuración sigue separada de Documento para no ensuciar la experiencia de lectura.

Documento: `docs/productizacion/T119_CONFIGURACION_HONESTA_ACCIONABLE.md`.

---

## Estado vigente — T126A

Base vigente: T126A — Fuente documental portable y refresco desde copia del proyecto.

Reglas agregadas:

- Al guardar un proyecto, el documento fuente se copia a `source/<archivo>` dentro de la carpeta del proyecto.
- Desde ese momento, `currentDocument.sourcePath()` debe apuntar a la copia interna, no al archivo externo original.
- Refrescar contenido lee la copia interna del proyecto.
- La UI avisa al usuario que el proyecto trabaja con la copia guardada e incluye “No volver a mostrar este aviso”.
- Una acción futura separada podrá llamarse “Reemplazar fuente desde archivo externo...”; no debe confundirse con Refrescar contenido.

Documento: `docs/productizacion/T126A_FUENTE_DOCUMENTAL_PORTABLE.md`.

---


## Estado vigente — T116

Base vigente: T116 — FFmpeg autocontenido real.

Reglas agregadas:

- FFmpeg y FFprobe son artefactos locales esperados en `tools/ffmpeg/bin/`.
- La validación de runtime layout final debe exigir `ffmpeg.exe` y `ffprobe.exe`, no solo carpetas.
- La app no debe depender de PATH global para media/video.
- `FfmpegRuntimeProbeUseCase` consulta versiones y encoders (`libx264`, `h264_nvenc`, `h264_qsv`, `h264_amf`) para que la UI/exportación no prometa hardware no soportado.
- Esta tanda no descarga binarios; deja el contrato listo para packaging/RC.

Documento: `docs/productizacion/T116_FFMPEG_AUTOCONTENIDO_REAL.md`.

---

## Estado vigente — T128-C01

Base vigente: T128-C01 — Documentación de limpieza de producto.

Reglas consolidadas para futuras tandas:

- DocuPodcast abre documentos Word/DOCX, PDF con texto nativo, Markdown y TXT.
- La experiencia visible habla de Documento, lectura preparada, fragmentos, voz, audio y visuales.
- No existe subir guion, importar guion ni exportar guion como categoría de usuario.
- Markdown se abre como documento fuente; no es contrato de guion.
- Whisper/STT/audio a texto fue retirado del build principal y no debe volver.
- Las superficies heredadas `SCRIPT_EDITOR`, `AUDIO_JOBS` y `STORYBOARD` no son navegación primaria.
- `PreparedReadingProjection` es la frontera vigente para hablar de lectura preparada; `NarrationScriptDocument` queda como payload interno temporal.

Documento: `docs/productizacion/T128_C01_DOCUMENTACION_LIMPIEZA_PRODUCTO.md`.

---

## Validación T124

Ejecutar en Windows:

```bat
scripts\99-diagnostico-completo.bat
```

Esperado: Maven tests verde, incluyendo `LegacyWorkspaceQuarantineT124SourceTest`. Esta tanda no elimina clases heredadas; las pone en cuarentena y protege que no vuelvan a la navegación principal.

---

## Validación T123-CAT01

Ejecutar en Windows:

```bat
scripts\99-diagnostico-completo.bat
```

Esperado: Maven compile OK, Maven tests OK, smoke automático cerebro OK, preflight arranque motores OK y Piper/FFmpeg locales OK o reporte humano de preparación.

Validación focal realizada en entorno de generación:

- `javac --release 21` de `domain`, `application`, `infrastructure` y bootstrap no JavaFX.
- Búsqueda en `src/main/java` y `src/main/resources`: no quedan `docupodcast-script-v1`, `IMPORT_SCRIPT_MARKDOWN`, import/export Markdown de narración ni `guion_narrable`.
- Tests fuente actualizados: recursos IA, importación Markdown UI, export readiness y bundle exporter.

---

## Validación T114-HF2

Ejecutar en Windows:

```bat
scripts\99-diagnostico-completo.bat
```

Esperado: Maven compile OK, Maven tests OK, smoke automático cerebro OK, preflight arranque motores OK y Piper/FFmpeg locales OK o reporte humano de preparación.

Validación focal realizada en entorno de generación:

- `javac --release 21` de `application`, `domain`, `infrastructure` y `bootstrap` no JavaFX.
- `javac --release 21` focal de tests `application`, `domain`, `infrastructure` con stubs JUnit.
- `javac --release 21` focal de tests `productization` con stubs JUnit.
- Búsqueda en `src/main/java` y `scripts`: no quedan `Whisper`, `SpeechToText`, `STT`, `stt`, `SPEECH_TO_TEXT` ni `allowGpuForStt`.

También se corrige el único fallo del diagnóstico T114-HF1: `DocumentLayerAssignmentWorkflowSourceTest` ahora espera “fragmento preparado” en vez de “segmento narrable”.

---

## Validación T114-HF1

Ejecutar en Windows:

```bat
scripts\99-diagnostico-completo.bat
```

Esperado: Maven tests verde, incluyendo `AudioClipPlaybackExportT92SourceTest`, `StoryboardVideoFromRenderPlanTi3SourceTest` y `ProductCleanNavigationT114Hf1SourceTest`.

La tanda no modifica motores ni packaging. Solo corrige source tests post-RF1, navegación legacy básica y mensajes visibles urgentes de no-guion.

---

## Validación TI2 — Audio jobs desde RenderPlan

Después de aplicar TI2 ejecutar:

```bat
cd scripts
.\99-diagnostico-completo.bat
```

La tanda agrega validación focal para:

- `AudioGenerationRequest` con `RenderUnitPlan`;
- fallback legacy sin `RenderUnitPlan`;
- gateways que consumen `request.generationUnits()`;
- mock TTS generando WAV por `RenderUnit.id`;
- omisión de audio externo y visual silencioso en el job TTS.

Pruebas nuevas:

- `AudioGenerationRequestRenderPlanTest`
- `MockAudioGenerationGatewayRenderPlanTest`
- `AudioJobsFromRenderPlanTi2SourceTest`

---

## Validación TP2

Ejecutar localmente:

```bat
cd C:\Users\MARCOS MOREIRA\Downloads\mo\scripts
.\99-diagnostico-completo.bat
```

Esperado tras TP2:

- Maven compile OK.
- Maven tests OK.
- Smoke automático cerebro OK.
- Preflight arranque motores OK.
- Piper y FFmpeg locales OK o reporte humano de preparación.

TP2 agrega preflight humano integrado: `listo`, `requiere preparación` o `error`, con acción sugerida visible en Configuración.

Notas históricas que siguen vigentes:

- Tanda vigente: TP2.
- Tanda 58C sigue como protocolo histórico de smoke exploratorio mínimo.
- Piper genera un WAV real en el smoke opt-in de motores.
- Coqui/XTTS genera un WAV real en el smoke opt-in de motores.
- FFmpeg normaliza audio y extrae audio de video en el smoke opt-in de motores.
- El smoke de motores reales no es Release Candidate final; es verificación opt-in.

## Validación T112

Ejecutar localmente:

```bat
cd C:\Users\MARCOS MOREIRA\Downloads\mo\scripts
.\99-diagnostico-completo.bat
```

Resultado esperado: Java, Maven, entorno, toolchain, configuración TTS, compile, tests, smoke cerebro, preflight y Piper/FFmpeg locales en OK.

T112 incluye hotfix focal del diagnóstico `20260602-153524`: `components/ribbon.css` conserva el marcador histórico `T111-HF2 — PNG icon polish` para alinear el source test de iconografía T111.

Smoke visual manual obligatorio antes de T113:

1. Abrir app en Inicio.
2. Abrir DOCX con imagen embebida.
3. Verificar imagen fuente visible o recuperada desde `source/*.docx`.
4. Pulsar playbar sin proyecto y confirmar aviso + diálogo de guardado.
5. Preparar audio y ocultar/restaurar overlay desde status bar.
6. Revisar sidebar Texto/Audio/Imagen, rail derecho, Voces, Guía y Configuración.
7. Guardar, cerrar y reabrir proyecto.

## Validación T108

Pendiente de validación local completa con `scripts\99-diagnostico-completo.bat`.

Cambios focales validados en entorno ChatGPT:

- Los errores de compilación de T110R-HF1 ya estaban corregidos.
- Se revisó el diagnóstico `20260602-125428`: Maven compile OK, smoke cerebro OK, fallaban 3 tests.
- Se corrigió la búsqueda de guía para `reintentar`.
- Se ajustó el mensaje DOCX de imagen sin descripción para mantener compatibilidad con tests existentes.
- El test DOCX sintético ahora incluye `word/media/image1.png` para validar `embeddedImageBase64` de forma coherente.
- Se revisó fuente de `VoiceLibraryWorkspaceView` y CSS de voces.

Ejecutar localmente:

```bat
cd C:\Users\MARCOS MOREIRA\Downloads\mo\scripts
.\99-diagnostico-completo.bat
```

## Validacion T110R

Validacion realizada en entorno ChatGPT:

```text
- Reconstrucción de T110 sobre T109.
- GuideDialog sin TextArea como visor de Markdown crudo.
- Topics visibles reescritos para DocuPodcast real, sin Whisper/STT/Audio a texto.
- Soporte inicial de imagen embebida DOCX mediante metadata base64.
- Render de imagen embebida en DocumentWorkspaceView.
- Configuración de duración de bloque visual silencioso: 5 segundos por defecto.
- Guardado guiado antes de escuchar/reproducir/generar audio.
- Playbar conectada a cancelación/pausa segura de jobs de audio.
- Tests fuente focales nuevos para T110R.
- ZIP íntegro.
```

No se ejecuto Maven completo en entorno ChatGPT por ausencia de `mvn`.

Validacion local obligatoria:

```bat
scripts\99-diagnostico-completo.bat
```

Resultado esperado: compile/tests/smoke/preflight OK.

- La inspiración Word/WPS no equivale a editor ofimático completo.
- El producto visible mantiene Coqui/XTTS, Piper y FFmpeg; no reintroducir Whisper/STT ni audio a texto.
- Bloque visual fuente no equivale a storyboard; la asignación visual sigue siendo decisión del usuario.

## Continuidad de validacion historica

- Tanda vigente: T110R.
- T108 — Workspace Voces final mínimo con criterio UX/UI sigue pendiente.
- Tanda 58C sigue como protocolo historico de smoke exploratorio minimo.
- Piper genera un WAV real en el smoke opt-in de motores.
- Coqui/XTTS genera un WAV real en el smoke opt-in de motores.
- FFmpeg normaliza audio y extrae audio de video en el smoke opt-in de motores.
- El smoke de motores reales no es Release Candidate final; es verificacion opt-in.
- La inspiración Word/WPS no equivale a editor ofimatico completo.
- El producto visible mantiene Coqui/XTTS, Piper y FFmpeg; no reintroducir Whisper/STT ni audio a texto.

## Validación T110R-HF1

El diagnóstico local `20260602-122634` falló por errores de compilación en `GuideDialog.java`. Se corrigieron los literales mal escapados reportados por Maven. Validación local requerida: ejecutar `scripts\99-diagnostico-completo.bat`.

## Validación T111 sugerida

Ejecutar:

```bat
cd C:\Users\MARCOS MOREIRA\Downloads\mo\scripts
.\99-diagnostico-completo.bat
```

Prueba manual focal:
1. Abrir `Instinto Creativo.docx`.
2. Verificar que la imagen embebida aparece en la hoja como bloque visual fuente.
3. Sin guardar proyecto, pulsar `Reproducir selección` en la playbar.
4. Confirmar que aparece aviso de guardado y luego el diálogo para crear el proyecto.


## T111-HF2 — Iconos PNG e imagen DOCX

Hotfix visual sobre T111: iconos PNG mediante componentes transversales (`AppIcon`/`IconView`) para Ribbon, sidebar izquierdo y rail derecho; refuerzo de imágenes embebidas DOCX y actualización de source tests de playbar/visual fuente.


## T111-HF3 — Corrección Inicio/IconView

Hotfix mínimo sobre T111-HF2: `WelcomeWorkspaceView` usa `IconView.sideDock(RibbonIconCatalog.iconFor(...))` en lugar de construir un `Label` con `AppIcon`, corrigiendo el fallo de compilación local `20260602-144025`.


## T111-HF4 — overlay compacto e iconografía reforzada

Se ajustó el overlay de procesos largos para poder ocultarlo y restaurarlo desde la barra de estado, se ampliaron los iconos PNG transversales y se añadió recuperación de imágenes DOCX embebidas para snapshots antiguos.
## Validacion T113

Validacion realizada en entorno ChatGPT:

```text
- DOCX demo creados con python-docx.
- Render visual de los DOCX demo con LibreOffice/headless mediante render_docx.py.
- Paginas revisadas: Cafe Luna Azul (1), El vuelo del Tornillo Dorado (2), Instinto Creativo (2).
- Recursos internos verificados: tres DOCX y tres PNG del ejemplo teatral.
- Compilacion focal de application.examples y ExampleApplicationServices con javac.
- Source test T113 agregado para menú, diálogo, catálogo y recursos.
- ZIP íntegro.
```

Validacion local obligatoria:

```bat
cd C:\Users\MARCOS MOREIRA\Downloads\mo\scripts
.\99-diagnostico-completo.bat
```

Prueba manual focal:

1. Abrir **Ejemplos > Abrir ejemplo...**.
2. Crear cada demo en una carpeta temporal.
3. Verificar que se abre Documento.
4. En el demo teatral, verificar que las tres imágenes se copiaron como assets de storyboard disponibles.

## Validacion TP1

Ejecutar localmente:

```bat
cd C:\Users\MARCOS MOREIRA\Downloads\mo\scripts
.\99-diagnostico-completo.bat
```

Esperado tras TP1:

- Maven compile OK.
- Maven tests OK.
- Smoke automatico cerebro OK.
- Preflight arranque motores OK.
- Piper y FFmpeg locales OK o reporte humano de preparación.

Validación focal en entorno ChatGPT: javac de `application.runtime`, checks fuente de integración en `InfrastructureServicesFactory` y `SettingsDialog`, ZIP íntegro. Maven completo no se ejecutó porque `mvn` no está instalado.


## TP3 Runtime layout

Validacion adicional:

```bat
scripts\29-verificar-runtime-layout.bat
```

Este script genera `target/runtime-layout/TP3_RUNTIME_LAYOUT_REPORT.md` y verifica que existan `tools/`, `models/`, `scripts/tts`, wrappers y carpetas base de motores sin usar PATH global.


## Productización TP4-TP6

Base vigente: TP4 — Licencias y manifest de terceros.
Base vigente: TP5 — Instalador/app portable real.
Base vigente: TP6 — RC instalable con smoke manual y automático.

- TP4 agrega inventario legal de FFmpeg, Piper, Coqui/XTTS, Python portable, JavaFX, dependencias Maven y ejemplos internos.
- TP5 prepara app-image/portable/MSI con runtime, tools, models, scripts y carpeta legal cuando existan.
- TP6 encadena diagnóstico, runtime layout, manifest de terceros, app-image, carpeta portable y smoke RC.

## Validación TI1

Validar localmente con:

```bat
cd scripts
.\99-diagnostico-completo.bat
```

Validaciones esperadas de TI1:

- `mvn compile` debe seguir verde.
- `mvn test` debe incluir `RenderUnitPlanTest`, `BuildRenderUnitPlanUseCaseTest` y `RenderUnitEndToEndTi1SourceTest`.
- `RenderApplicationServices` debe exponer `BuildRenderUnitPlanUseCase`.
- `docs/productizacion/ROADMAP_RESTANTE_POST_TI1_DETALLADO.md` debe enumerar todas las tandas restantes: TI2, TI3, TI4, TI5, TI6, TI7, RF1, RF2, RF3, RF4 y RF5.

Limitación honesta de TI1: todavía no cambia la generación real de audio ni el video legacy. Eso queda para TI2 y TI3.


## Validación TI4

Después de TI4, ejecutar `scripts\99-diagnostico-completo.bat`. Las validaciones focales agregadas cubren: visuales fuente no narrables, detección de tabla/fórmula, omisión de imagen/tabla/fórmula en el guion narrable y contrato visual `SourceVisualBlockView`.


## TI5 — TextAnchor fuerte y reconciliación

Se agrega `ReconcileTextAnchorsUseCase` y se fortalece `TextAnchor` con `fromDocumentSelection`, `selectedTextHash`, contexto, estados `CURRENT`, `RELOCATED`, `NEEDS_REVIEW` y `ORPHANED`. La reconciliación es local, determinista y no modifica la fuente documental.

## TI6 — FFmpeg como job persistente/cancelable

- Base: TI5 con diagnóstico Maven tests fallando por source test documental ya corregido.
- Agrega job persistente para render FFmpeg: `VideoRenderJobSnapshot`, repositorio, submit/cancel use cases y mapper a `ProcessJobSnapshot`.
- `ProcessJobKind.VIDEO_RENDER` queda marcado como persistente.
- `VideoRenderJobFileRepository` escribe `jobs/video/<jobId>/video-render-job.json`, logs y token `cancel.requested`.
- El sidebar Fragmento muestra ubicación de fuente: bloque DOCX/PDF o líneas TXT/Markdown cuando aplica.
- Se refuerza que tablas, fórmulas y LaTeX detectado no se narran como código.
- Siguiente tanda: TI7 — Playback por oración/unidad.

## Validación TI7

Después de aplicar TI7 ejecutar:

```bat
scripts\99-diagnostico-completo.bat
```

Puntos focales:

- `BuildPlaybackManifestFromRenderUnitPlanTest` debe pasar.
- `PlaybackUnitTi7SourceTest` debe pasar.
- El diagnóstico debe conservar compile, tests, smoke cerebro y preflight en verde.

## Post-TI7

Si TI7 queda verde, el siguiente recorrido es RF1 — Dividir `DocuPodcastShellViewModel`. Mantener como requisito que `ShellViewModelBrainDebtSourceTest` siga controlando el crecimiento hasta que los coordinadores queden extraídos.

## Validación RF1

- `DocuPodcastShellViewModel` delega exportaciones en `ExportWorkflowCoordinator`.
- `ExportWorkflowCoordinator` mantiene exportación Markdown, WAV, diagnóstico, bundle y storyboard/video.
- No cambia comportamiento visible ni formato de proyecto.
- Ejecutar `scripts\\99-diagnostico-completo.bat` tras aplicar la tanda.


## T122-C01 — PreparedReadingProjection

Se agrega una frontera `PreparedReadingProjection` para que el producto mantenga el flujo Documento → lectura preparada, dejando `NarrationScriptDocument` como payload interno temporal para audio, render, playback y persistencia. No se reintroduce Guion como categoría de usuario.

## T115 — Selector CPU/GPU real para Coqui/XTTS

Se agrega detección CPU/GPU para seleccionar el dispositivo de Coqui/XTTS desde Configuración. CPU siempre queda disponible; NVIDIA se mapea a `cuda:n`; AMD/Intel se detectan de forma informativa y quedan en fallback CPU para Coqui/XTTS salvo soporte posterior validado.



## T117 — Asistente real Coqui/XTTS

- Se agregó verificación local de Coqui/XTTS desde Configuración.
- El asistente revisa Python local portable, wrapper XTTS, modelo y voz neutral.
- No usa Python global ni PATH del sistema como requisito de producto.
- La acción 'Usar Coqui/XTTS' selecciona `engineMode=xtts` en pantalla para guardarlo como motor principal.


## T118 — Piper modo intermedio compatible

- Piper queda documentado y cableado como modo intermedio/liviano, no como motor principal ni como clonador de voces.
- Se agregan `InspectPiperSetupReadinessUseCase` y `SelectPiperAsEngineUseCase`.
- Configuración incorpora un bloque real de Asistente Piper con acciones `Verificar Piper` y `Usar Piper intermedio`.
- Piper exige `tools/piper/piper.exe`, `scripts/tts/piper-file-to-wav.ps1`, modelo `.onnx` y metadatos `.onnx.json`.
- La UI debe dejar explícito que Piper no habilita clonación por muestra humana, emociones ni estilos expresivos propios de Coqui/XTTS.

## Validación T119B
- `VoiceEngineCapabilityProfileTest` valida perfiles Piper, Coqui/XTTS y Mock.
- `VoiceEngineCapabilitiesT119BSourceTest` valida que el sidebar de Documento oculte capacidades imposibles para Piper y que la documentación registre la regla capability-driven.

## Validación esperada T119C/T120

- `BuildEngineArtifactManifestUseCaseTest` debe pasar.
- `EngineArtifactsManifestT119CSourceTest` debe pasar.
- `OnboardingFirstUseT120SourceTest` debe pasar.
- Inicio no debe mencionar Guion, Whisper/STT ni Storyboard como módulo principal.
- `scripts/30-generar-manifest-terceros.bat` debe generar también `ENGINE_ARTIFACTS_MANIFEST.md`.

## Validación T120B

Validación focal: tests de Welcome, Configuración como superficie, Ribbon base y `RibbonFinalContractT120BSourceTest` ejecutados con stubs JUnit. Maven completo debe ejecutarse localmente con `scripts\99-diagnostico-completo.bat`.

## PF2D/PF3A — Validación esperada

Tanda vigente: PF2D/PF3A.

Validación añadida: Configuración muestra progreso para operaciones largas de Voz IA avanzada; Biblioteca de voces puede reproducir, exportar y eliminar muestras reales por tono cuando existen. La experiencia normal ya no depende de ejecutar scripts manualmente para preparar motores.

Anclas de validación conservadas:
- T128-C01 — Documentación de limpieza de producto.
- Markdown se abre como documento fuente.
- Whisper/STT/audio a texto fue retirado del build principal.
- PreparedReadingProjection.
- No existe subir guion.
- Piper genera un WAV real.
- Coqui/XTTS genera un WAV real.
- FFmpeg normaliza audio y extrae audio de video.
- no es Release Candidate final.
- Validación T112.
- Validacion T113.
- Validación TP2.
- Base vigente: TP6.
- T121-V10.
- RF3.
- RF4 — Limpieza de workspaces heredados.
- Validación RF5.
- inspiración Word/WPS no equivale a editor ofimático completo.


## Estado PF2F/PF4A

- Base corregida desde diagnóstico `20260604-080938.zip`.
- Maven fallaba solo por 3 source tests de textos/guardarraíles de Configuración; se alinearon etiquetas y notas.
- Configuración muestra progreso con latido independiente y ruta de logs mientras prepara/descarga/importa motores.
- PF4A agrega launcher portable `run-docupodcast-studio.bat` con `DOCUPODCAST_APP_ROOT=%%~dp0`.
- Próxima tanda: PF4B — smoke post-app-image y verificación de runtime root desde launcher.


## Validación DOC-UX-HF9A

- Revisado diagnóstico `20260604-185006.zip`: el fallo único de build era `DocuPodcastShellViewModel.java:[1655,32] local variables referenced from a lambda expression must be final or effectively final`.
- Compilación focal con `javac --release 21` de `SourceDocumentRefreshCoordinator` y `SourceDocumentRefreshOutcome`: OK.
- Compilación focal de `DocUxHf9AHotfixSourceTest` con stubs JUnit: OK.
- Ejecución reflexiva de 2 métodos del guardarraíl HF9A: OK.
- Maven completo no se ejecutó en el entorno ChatGPT porque no hay `mvn`.

## Validación DOC-UX-HF9

- Compilación focal de `DocxDocumentImporter` y dependencias documentales con `javac --release 21`.
- Compilación focal de `DownloadXttsOfficialModelUseCase`, settings/compute/modelsetup con `javac --release 21`.
- Source tests agregados: `DocumentUxSyncHf9SourceTest`, `DocumentAudioEngineSyncHf9SourceTest`, `DocxVisualImportHf9SourceTest`, `XttsDownloadRobustnessHf9SourceTest`.
- Ejecución reflexiva con stubs JUnit: 10 métodos OK.
- Smoke focal DOCX: `instinto-creativo/source.docx` detecta 1 imagen embebida con base64; `caso-contable-cafe-luna/source.docx` detecta tabla con vista previa Markdown.
- Maven completo no se ejecutó en este entorno porque no hay `mvn`; validar localmente con `scripts\99-diagnostico-completo.bat`.

## Validación DOC-UX-HF9C

- Compilación focal con `javac --release 21` de dominio + `ApplyReadingProfileUseCase` / `PreviewReadingProfileUseCase`: OK.
- Compilación focal de `PreviewReadingProfileUseCaseTest` con stubs JUnit: OK.
- Ejecución reflexiva de `PreviewReadingProfileUseCaseTest`: OK.
- Compilación focal de 12 source tests de rail/visual/Whisper residual/visibilidad con stubs JUnit: OK.
- Ejecución reflexiva de 19 métodos de esos guardarraíles: OK.
- Maven completo no se ejecutó en este entorno porque no hay `mvn`; validar localmente con `scripts\99-diagnostico-completo.bat`.

## Validación DOC-UX-HF9D

- Guardarraíl agregado: `DocUxHf9DVisualRailContextMenuSourceTest`.
- Verifica que el rail use `ContextMenu` con acciones `Asignar imagen al fragmento anterior` y `Asignar imagen al fragmento posterior`.
- Verifica que las acciones queden deshabilitadas si no hay imagen o no hay vecino.
- Verifica que la copia llame a `copyFragmentImageToAdjacentFragment(...)`, seleccione el fragmento destino y actualice el panel Imagen.
- Maven completo no se ejecutó en este entorno porque no hay `mvn`; validar localmente con `scripts\99-diagnostico-completo.bat`.

## DOC-UX-HF9E — playbar, generación e imagen

- Playbar enfocado en reproducción: pausar/detener ya no cancela generación.
- Status bar expone Seguir generando / Cancelar generación.
- Rail Visual y sidebar Imagen comparten resolver por frase.
- Anterior/siguiente usan la cue exacta y se deshabilitan en extremos.
- Motor de voz no disponible muestra confirmación para abrir Configuración antes de generar.


## DOC-UX-HF9F — importación con progreso y playback estable
- DOC-UX-HF9G: apertura documental simple sin prometer preparación de fragmentos/audio y panel de preparación de audio con tamaño estimado de chunks.
- Diálogo de motor no disponible encapsulado fuera del shell.
- Apertura de fuente documental con `Task<ReadableDocument>` y diálogo de progreso para evitar bloqueo perceptible del hilo JavaFX.
- Resolución de cue por posición/unidad para evitar repetir la primera frase del mismo segmento cuando se reproduce por oraciones.
- Queda pendiente virtualización/paginación real del lector para documentos de miles de bloques.

## DOC-UX-HF9H — importación documental y URLs de descarga

- La ventana de apertura documental ahora puede ocultarse y se cierra de forma más robusta antes del render de la vista.
- Los documentos grandes usan render inicial acotado hasta implementar virtualización real.
- Las URLs de descarga de Voz IA avanzada y Voz local simple quedan editables en Configuración y persistidas en `operational-settings.properties`, con overrides por variables de entorno.
- Validación focal: settings/modelsetup compilados y source tests HF9H ejecutados.

## DOC-UX-HF9I — documentación actual y URLs de motores limpias

- Se crea `DOCUMENTACION_ACTUAL/` como fuente vigente de cierre para no mezclar decisiones actuales con documentación histórica.
- La documentación histórica se conserva por compatibilidad de guardarraíles, pero el punto de entrada actual es `DOCUMENTACION_ACTUAL/00_LEEME_PRIMERO.md`.
- Se corrigieron strings visibles de Configuración para no exponer nombres técnicos ni variables técnicas de entorno en la UX normal.
- Las URLs de descarga de Voz IA avanzada y Voz local simple siguen centralizadas y editables, pero el detalle técnico queda en settings/entorno, no en microcopy visible.
- Próxima validación: `scripts\99-diagnostico-completo.bat`.


## Validación DOC-UX-HF10E / DOC-TABLE-HF11B

1. Ejecutar `scripts\99-diagnostico-completo.bat`.
2. Abrir un documento sin audio preparado, guardar proyecto y pulsar `Generar chunks de audio`; debe preparar lectura si hace falta, abrir overlay y comenzar job.
3. Ocultar overlay; la generación debe continuar y el status bar debe permitir mostrar detalles.
4. Abrir DOCX con tablas; deben verse con `SourceTableGridView`, cabecera morada y celdas blancas.


## DOC-UX-HF10F

Tanda vigente posterior a DOC-UX-HF10E: limpia contenedores redundantes en tablas/imágenes fuente y mueve el cálculo de tamaño estimado de chunks fuera del hilo JavaFX para que el overlay de generación sea más ancho y no bloquee la interacción. Ver `DOCUMENTACION_ACTUAL/01_REGISTRO_TANDAS_RECIENTES.md`.


## PLAYBACK-SPEED2 — continuidad y tono natural

- Velocidad `1x`, `1.5x` y `1.75x` en playbar.
- `1.5x`/`1.75x` usan time-stretch PCM local para conservar mejor el tono, no sample-rate shift.
- Reproducir desde selección continúa con los siguientes chunks; `Reproducir fragmento (solo este)` queda como acción corta explícita.
- Si la lectura rápida alcanza un chunk pendiente, mantiene la espera de buffer del flujo existente.

## PLAYBACK-SPEED3 — continuidad al cambiar velocidad

Validación requerida:

1. Ejecutar `scripts\99-diagnostico-completo.bat`.
2. Generar chunks de audio.
3. Reproducir desde selección y confirmar que sigue de largo.
4. Cambiar a `1.5x` durante un chunk y confirmar que el siguiente arranca.
5. Cambiar a `1.75x` y luego `1x`; confirmar que no repite el fragmento.
6. Si falta buffer, debe esperar y continuar cuando el WAV exista.

Validación focal en entorno ChatGPT: compilación focal de reproductor/clock y source assertions de continuidad. Maven completo no ejecutado por falta de `mvn`.

## PLAYBACK-SPEED6 — controlador estructural de continuidad

Se extrajo la continuidad de playback a `PlaybackContinuationController` y se agregó callback real `SegmentAudioPlayer.setOnPlaybackFinished(Consumer<Path>)`. El avance entre chunks ya no depende solo del polling de `Timeline`: Java Sound notifica el final natural del WAV activo y el ViewModel transiciona al siguiente cue validando que el archivo terminado corresponde al cue vigente.

## PLAYBACK-SPEED7 — manifest runtime y cola reproducible

Validación requerida:

1. Ejecutar `scripts\99-diagnostico-completo.bat`.
2. Abrir un proyecto con muchos chunks WAV ya generados.
3. Pulsar `Reproducir desde selección`.
4. Confirmar que el estado muestra `Manifest JOB-...: N chunks listos` con una cantidad amplia, no 1 o 2.
5. Dejar avanzar varios fragmentos sin tocar nada.
6. Cambiar entre `1x`, `1.5x` y `1.75x` y confirmar que no se corta al final del WAV activo.

## PLAYBACK-SPEED8 — watchdog de continuidad

Se agregó un watchdog independiente del timer/callback del reproductor para evitar que la lectura se detenga tras uno o dos chunks aunque existan WAV siguientes. También se ajustó la continuación tras buffer para iniciar el `PlaybackCue` exacto.

## PLAYBACK-SPEED9 / STATUS-SCROLL1

- Continuidad de lectura: se agrega `PlaybackRuntimeQueue` para avanzar por la cola viva de cues, sin volver a resolver desde la selección del documento después de cada chunk.
- Monitor runtime: `startCueMonitoring(...)` detecta player detenido, final de cue, tiempo excedido o estancamiento de posición.
- Status bar: el mensaje principal queda dentro de un `ScrollPane` horizontal para leer estados largos.

## Validación recomendada PLAYBACK-CORE3

1. Ejecutar `scripts\\99-diagnostico-completo.bat`.
2. Generar chunks para un documento con párrafos de varias oraciones.
3. Usar `Reproducir desde selección`.
4. Verificar que avanza de `SEG-002-U001` a `SEG-002-U002` sin repetir el cue anterior.
5. Cambiar a `1.5x` y `1.75x` durante reproducción y confirmar continuidad.


## PLAYBACK-CORE4 — cola exacta guiada por final real del reproductor

- Reproduce por cola exacta de cues, pero prioriza el callback real del reproductor antes de avanzar.
- El watchdog de cola ya no corta audio si Java Sound todavía reporta reproducción activa.
- Corrige inconsistencias observadas al cambiar velocidad, pausar/reanudar y reproducir mientras se generan chunks.
- Documento técnico: `docs/productizacion/PLAYBACK_CORE4_PLAYER_CALLBACK_QUEUE.md`.

## AUDIO-RESUME-HF1

- Reanudación TTS: los segmentos no completados se reinician como pendientes para que **Seguir generando** reintente el proceso externo.
- Se elimina WAV parcial antes de cada intento de generación.

## Validación VOZ-UX4R-1

- Ejecutar `scripts\99-diagnostico-completo.bat`.
- Abrir Vista > Voces.
- Confirmar que la lista dice `Voces creadas`.
- Confirmar estados humanos: `Neutral lista`, `Tonos configurados`, `Falta neutral`, `Incompleta` o `Voz base lista`.
- Confirmar que no se muestra `Opcional` para tonos en la UI normal.
- Confirmar que no aparecen nombres técnicos de motores en Voces.

## Validación VOZ-UX4R-2A

- Ejecutar `scripts\99-diagnostico-completo.bat`.
- Abrir Vista > Voces.
- Confirmar navegación lateral por módulos: Inicio, Configurar motor, Gestionar voces.
- Confirmar que ya no se usa `SplitPane` en Vista Voces.
- Confirmar que Inicio lista voces creadas con filas sobrias.
- Confirmar que Configurar motor muestra estado/prueba sin placeholders de CPU/GPU.
- Confirmar que Gestionar voces concentra muestras por tono/emoción, importar, grabar, reproducir, exportar y eliminar muestra.
- Source guardrail nuevo: `VoiceUx4R2AModularShellSourceTest`.

## VOZ-UX4R-2B — Configurar motor real dentro de Voces

Se completa el módulo Configurar motor de la microaplicación Voces con selector de motor activo, selector de dispositivo de renderizado CPU/GPU detectado, estado honesto y prueba por textbox. Las selecciones se persisten en `OperationalSettings`, la misma configuración interna usada por Configuración. No se resuelve todavía la descarga de Voz IA avanzada/Coqui; queda para `MOTOR-SMOKE4R / COQUI-DL1`.


## Validación VOZ-UX4R-3

Ejecutar `scripts\99-diagnostico-completo.bat`. Revisar que el módulo Gestionar voces compile con `VoiceProfileAdministrationCoordinator`, `VoiceLibrary.withoutVoice(...)` y el guardarraíl `VoiceUx4R3ManageVoicesSourceTest`.

### VOZ-UX4R-3B — navegación modular sobria de Voces

La Vista Voces mantiene tres módulos internos —Inicio, Configurar motor y Gestionar voces— con botones laterales de solo nombre. El microcopy vive en el workspace derecho. Se corrige el refresco de selección que podía devolver accidentalmente la vista a Gestionar voces al intentar abrir Inicio o Configurar motor. El sidebar queda sobrio, sin degradados, con criterio administrativo de escritorio.


## MOTOR-SMOKE4R / COQUI-DL1 — implementada

Base vigente: Configuración de Voz IA avanzada incluye botón `Probar` para generar un WAV real en `runtime/tts/xtts-smoke`. El estado de motor diferencia descargado, verificado, seleccionable, WAV generado y reproducción confirmada. La descarga grande de Voz IA avanzada puede reanudarse mediante HTTP `Range` cuando existe un `.download` parcial. Documento técnico: `docs/productizacion/MOTOR_SMOKE4R_COQUI_DL1_PRUEBA_WAV.md`.

## MOTOR-PLAYCONF1

Validación esperada en Windows: ejecutar `scripts\\99-diagnostico-completo.bat`, abrir Configuración > Motores, usar Voz IA avanzada > Probar para generar WAV y luego **Reproducir prueba** para confirmar reproducción interna.

## MOTOR-SMOKE4R-HF3

La preparación de Voz IA avanzada ahora deja diagnóstico accionable de descarga en `models/tts/xtts/download-diagnostics.txt` y la UI muestra qué requisito quedó pendiente cuando la descarga no deja el motor seleccionable.


## VIDEO-EXPORT1

Validación estática en entorno ChatGPT: javac de domain+application+infra+factory, source tests focales y ZIP íntegro. Maven completo debe ejecutarse localmente con `scripts\99-diagnostico-completo.bat`.

## Validación MOTOR-SMOKE4R-HF4

1. Ejecutar `scripts\99-diagnostico-completo.bat`.
2. En Configuración > Motores de voz, usar Preparar/Descargar Voz IA avanzada.
3. Si `models\tts\xtts\download-diagnostics.txt` indica `success=true` y `Fallidos=[]`, confirmar que la UI no lo reporta como descarga fallida.
4. Si falta entorno local de ejecución, el diálogo debe decir que el modelo fue descargado correctamente y listar el componente pendiente junto con la ruta del reporte de preparación local.


## AUDIO-COMPRESS1

Exportación final de audio ampliada a WAV, MP3 y AAC. Los segmentos internos siguen siendo WAV; MP3/AAC se generan como entrega final mediante el componente local de video/audio.


## Validación MOTOR-PERF1 / MOTOR-GPU1

Ejecutar `scripts\99-diagnostico-completo.bat`. Tests focales agregados: `AssessComputeAccelerationUseCaseTest` y `MotorPerf1HonestGpuSourceTest`.

## MOTOR-SMOKE4R-HF5

- Verificar que el modelo avanzado descargado no falle por `voz-local-simple.wav`.
- Ejecutar diagnóstico completo después del hotfix.
- Guardarraíles: `InspectXttsSetupReadinessUseCaseTest`, `XttsTtsCommandTemplateTest`, `AdvancedVoiceLocalSimpleLeakHf5SourceTest`.
## Validación MOTOR-TTS-ADV-HF1

1. Ejecutar `scripts\99-diagnostico-completo.bat`.
2. Con Voz IA avanzada seleccionada y modelo descargado, abrir un proyecto corto.
3. Pulsar Generar audio.
4. Confirmar que el job deja de quedarse mudo en `0/1`: debe haber actividad de proceso o, si falla, diagnóstico con fases `DOCUPODCAST_XTTS`.
5. Si se cancela o vence el timeout, confirmar que no quedan procesos hijos Python/PowerShell vivos y que al cerrar no queda job fantasma activo.

Guardarraíl: `AdvancedTtsJobStallFixSourceTest`.



## MAVEN-DIAG-HF1

Validación focal realizada en entorno ChatGPT: `NamespaceMigrationSourceTest` compila con stubs JUnit y ya no escanea artefactos binarios descargados en `models/tts/xtts`. Maven completo no se ejecutó porque `mvn` no está instalado en el entorno.
## Validación PLAYBACK-SELECT1 / MODEL-PORT1

1. Copiar una carpeta `models/` previamente descargada a la raíz de una tanda nueva.
2. Abrir Configuración > Motores de voz y media y confirmar que Voz IA avanzada no obliga a descargar si `models/tts/xtts` está completo.
3. Abrir un documento preparado, seleccionar una sección o fragmento avanzado y usar `Reproducir desde selección`.
4. Confirmar que el job empieza por el segmento seleccionado y que el mensaje indica que el primer chunk preparado será el seleccionado.
5. Confirmar que el ícono del módulo `Índice` en el sidebar izquierdo se ve como árbol/navegación jerárquica.



### Validación VOICE-REGISTRATION-WIZARD1

Ejecutar diagnóstico completo. Tests focales nuevos: `VoiceRegistrationWizard1SourceTest`.


## THEATER-COLON-READ1

Documento puede omitir etiquetas breves antes de dos puntos para guiones teatrales, por ejemplo `Vaquero: texto` → `texto` en la generación de audio. El Word original no cambia y los chunks deben rehacerse.

## USER-DECISIONS-HF1

Se agregó el contrato transversal para decisiones defensivas visibles (`UserVisibleDecision`, `OperationResult`, `DefensiveDecisionPolicy`) y excepciones tipadas (`ApplicationPreconditionException`, `InfrastructureOperationException`, `ExternalProcessFailedException`, `EngineUnavailableException`). `UserNotification`/`ExceptionAlertPresenter` ya pueden mostrar esas decisiones como message box. El bloqueo defensivo de Voz IA avanzada usa `EngineUnavailableException` en lugar de `IllegalStateException`.



## VOICE-SAMPLES-RC1 / VOICE-WORKSPACE-HF3 / VOICE-TONE-UX-HF1

Validar manualmente: abrir Vista Voces sin proyecto guardado, crear una voz, importar o grabar Neutral, cancelar una grabación y confirmar que no reemplaza la muestra anterior. Verificar que los temporales se creen bajo `voice-library/tmp-recordings` y las muestras finales bajo `voice-library/samples`. En Documento, seleccionar esa voz y confirmar que el ComboBox de tono solo muestra emociones con muestra registrada.

## VOICE-HF3-CORRECCION + CSS-TOKENS-HF1

Validar con diagnóstico completo. Los 8 fallos previos por límite de `DocuPodcastShellViewModel` deben desaparecer: el archivo queda bajo 2700 líneas. Al abrir Documento no debe repetirse el warning JavaFX por `-dp-ink` en `.document-context-checkbox`. En el status bar, el hover de botones de proceso/generación/tamaño de lectura debe verse como morado pastel claro, no oscuro.


## Validación ESTANDARES-PENDIENTES-RC1

1. Ejecutar `scripts\99-diagnostico-completo.bat`.
2. Abrir la app con `scripts\01-ejecutar-app.bat`.
3. Abrir `Ejemplos` y confirmar que no aparece warning JavaFX por `-dp-text-secondary` en `.example-project-readiness`.
4. Confirmar que `DOCUMENTACION_ACTUAL/08_ESTANDARES_PENDIENTES_RC.md` existe y enumera estándares pendientes reales.
5. Confirmar que `DOCUMENTACION_ACTUAL/PLAN_IMPLEMENTACION_EN_PIEDRA/08_ESTANDARES_PENDIENTES_Y_TANDAS_RESTANTES.md` existe y fija el orden restante recomendado.
6. Confirmar que la próxima tanda recomendada queda documentada como `RUNTIME-PATHS-RF1`.


## Validación focal runtime/process RF1

Para esta base, además del diagnóstico completo local, validar:

- `RuntimeArtifactPathsTest`;
- `RuntimePathsRf1SourceTest`;
- `ExternalProcessRequestTest`;
- `ExternalProcessRunnerRf1SourceTest`;
- `ExternalProcessExceptionsHf1SourceTest`;
- smoke CUDA de XTTS desde Configuración si el entorno tiene Python local.

El criterio central es que los nuevos flujos de motores no creen rutas hardcodeadas ni `ProcessBuilder` dispersos; deben usar `RuntimeArtifactPaths` y `ExternalProcessRunner` o quedar marcados como legacy pendiente.

## ENGINE-READINESS-UI-HF1 + COMMAND-AUDIT-RC1 + RIBBON-CATALOG-RF1

Se agregaron estados humanos de readiness de motores (`AudioEngineReadinessUiItem`, `InspectAudioEngineReadinessUiUseCase`), auditoría de comandos visibles (`CommandAuditInspector`) y catálogo separado del Ribbon (`RibbonDefinitionCatalog`). `RibbonView` queda como renderizador y el Shell audita que todo comando visible tenga handler real. Documento/Voces pueden exponer líneas de readiness sin agregar dashboard decorativo.


### Validación SETTINGS-SPLIT-RF1 + GUI-COMPONENTS-RF1

- Verificar que `RibbonView.java` mantiene los contratos fuente históricos sin exponer `EXPORT_PROJECT_BUNDLE` ni `EXPORT_DIAGNOSTIC_REPORT` en Ribbon.
- Verificar que `SettingsDialog` delega la barra inferior en `SettingsActionBar`.
- Verificar que `OperationalStatusStrip` comunica estado operativo y no funciona como dashboard decorativo.


## Validación XTTS legacy command repair

- Verificar que `XttsTtsCommandTemplateTest.localizedLegacyAdvancedVoiceCommandIsRebuiltAsManagedPortableCommand` pase.
- Verificar que `scripts/tts/Voz IA avanzada-file-to-wav.ps1` exista y redirija a `xtts-file-to-wav.ps1`.
- Verificar que ningún comando auditado contenga `model.pth/model.pth`.
- Verificar que Voz local simple/Piper comunique uso CPU y no prometa GPU.
