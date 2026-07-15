
## PRESENTATION-ORCHESTRATION-RF21-05 — Voz local simple aislada de voces avanzadas

Estado: implementada como corrección operativa de dependencias embebidas. `PiperVoiceModelPathPolicy` impide que Voz local simple use ids o muestras de Voz IA avanzada como si fueran modelos `.onnx`; cuando la voz global es avanzada, Piper vuelve al modelo embebido `models/tts/piper/voices/es_ES-default-medium.onnx`. También se ignoran rutas legacy/localizadas como `recursos locales local simple` para que la preparación no busque modelos en carpetas antiguas. Validación enfocada: compile y tests de readiness/comando Piper verdes.

## PRESENTATION-ORCHESTRATION-RF15-01 — divisores invisibles en Documento

Estado: implementada como ajuste visual sobre RF15. El `SplitPane` de Documento oculta su divisor y el rail derecho elimina la manija visual de redimensionado para que los sidebars se sientan limpios y no aparezca la barra rígida entre paneles y lectura. Validación: `mvn -q test` verde.

## PRESENTATION-ORCHESTRATION-RF15 — carcasas homogéneas de sidebars de Documento

Estado: implementada como pulido de scaffolding visual en `Documento`. `WorkspaceSideDock` y `CollapsibleMediaRail` usan `DocumentSidePanelChrome` y `SidePanelToggleButton`, eliminando los controles textuales `"X"` y `"<"` de los sidebars visibles. Se conservan contenidos internos y títulos `Fragmento`/`Visual`; el rail izquierdo suma un acento azul grisáceo sobrio. Validación: `mvn -q test` verde.

## PRESENTATION-ORCHESTRATION-RF14-08 — Inicio de Voces balanceado

Estado: implementada como pulido visual de `Inicio` en `Vista > Voces`. `Inicio` usa un maestro-detalle balanceado para que `Voces creadas` y `Detalle de voz seleccionada` tengan anchura comparable. La lista de voces puede crecer con scroll propio y `Tonos registrados` separa tono, origen y archivo en filas legibles para evitar puntos suspensivos por compresión. Validación: `mvn -q test` verde.

## PRESENTATION-ORCHESTRATION-RF14-07 — emociones listas y bloqueo de prediseñadas

Estado: implementada como ajuste operativo de `Vista > Voces`. El editor común muestra una microtarjeta de `Emociones listas`, `Escuchar` cambia a `Reproduciendo muestra...` mientras dura la reproducción estimada, el botón principal queda como `Guardar / actualizar voz` y `Gestionar voz seleccionada` se deshabilita para voz simple y voces prediseñadas. La microtarjeta se extrajo a `VoiceReadyToneMicroCard` para mantener `VoiceLibraryWorkspaceView` bajo el límite vigente. Validación: `mvn -q test` verde.

## PRESENTATION-ORCHESTRATION-RF14-06 — editor único para voz avanzada

Estado: implementada como simplificación operativa de `Vista > Voces`. `Nueva voz` y `Gestionar voz seleccionada` usan el mismo `VoiceProfileSampleEditorPanel`: nombre de voz, emoción de referencia, frase grande para interpretar, estado de muestra y acciones de importar/grabar/detener/cancelar/escuchar/repetir/eliminar muestra. La voz simple no entra al editor porque no usa muestras ni emociones. Se elimina del editor el progreso tipo wizard, el catálogo teatral visible y el detalle duplicado. Validación: `mvn -q test` verde.

## PRESENTATION-ORCHESTRATION-RF14 — wizard claro para registrar voces

Estado: implementada como pulido UX de `Vista > Voces`. `Gestionar voces > Nueva voz` conserva el subflujo interno, pero ahora refuerza los pasos `Identidad`, `Neutral`, `Tonos opcionales` y `Guardar`; la frase a interpretar usa una clase grande `voice-interpretation-phrase`; las acciones de grabación/reproducción usan símbolo + texto con tooltip accesible; y `VoiceActionStrip` reemplaza filas rígidas para evitar botones con puntos suspensivos. No cambia schema, motores, rutas ni síntesis. Documento vigente: `DOCUMENTACION_ACTUAL/09_BITACORA_REFACTOR_PRESENTACION.md`.

## PRESENTATION-ORCHESTRATION-RF13 — Vista Voces por maestro-detalle

Estado: implementada como reordenamiento UX operativo de `Vista > Voces`. El sidebar conserva `Inicio`, `Configurar motor` y `Gestionar voces`; los workspaces internos pasan a selección izquierda + detalle/trabajo derecho, con `VoiceWorkspaceLayout` como helper de presentación. `Nueva voz` sigue dentro de `Gestionar voces`, pero ahora se presenta como subflujo guiado con pasos `Identidad`, `Neutral` y `Tonos opcionales`. No cambia schema, motores, rutas ni síntesis. Documento vigente: `DOCUMENTACION_ACTUAL/09_BITACORA_REFACTOR_PRESENTACION.md`.

## PRESENTATION-ORCHESTRATION-RF12 — coordinadores de Voces para muestras y motor

Estado: implementada como primera extracción de la microaplicación `Voces`. `VoiceLibraryWorkspaceView` conserva navegación, selección, render de módulos y estado visual, pero las acciones operativas de muestras pasan a `VoiceSampleActions` y el bloque de motor/dispositivo pasa a `VoiceEngineSettingsControls`. No cambia UX, schema ni contratos de voz; los guardarraíles fuente ahora inspeccionan la superficie compuesta `VoiceLibraryWorkspaceView` + coordinadores. `VoiceLibraryWorkspaceView` baja a cerca de 1000 líneas y queda pendiente extraer catálogo/tonos o gestión de voz. Documento vigente: `DOCUMENTACION_ACTUAL/09_BITACORA_REFACTOR_PRESENTACION.md`.

## PRESENTATION-ORCHESTRATION-RF11 — coordinador de configuración inicial

Estado: implementada como extracción por dominio. `SettingsDialog` conserva el punto de entrada `showFirstUseSetup`, el disparo del prompt inicial y métodos delegadores históricos, pero la inspección de readiness, confirmación, descarga de Voz local simple, selección del motor y mensajes de primer uso pasan a `InitialSetupSettingsOperations`. No cambia UX, schema ni casos de uso; `SettingsDialog` baja a menos de 900 líneas. Con esto Configuración queda razonablemente estabilizada por coordinadores de dominio; la siguiente tanda recomendada pasa a `VoiceLibraryWorkspaceView`. Documento vigente: `DOCUMENTACION_ACTUAL/09_BITACORA_REFACTOR_PRESENTACION.md`.

## PRESENTATION-ORCHESTRATION-RF10 — coordinador de Video local y FFmpeg

Estado: implementada como extracción por dominio. `SettingsDialog` conserva la tarjeta visual de Video local, la URL configurable y los botones `Verificar`, `Preparar` e `Importar carpeta`, pero la verificación de FFmpeg/FFprobe, descarga, importación, persistencia de ruta embebida y progreso pasan a `VideoLocalSettingsOperations`. No cambia UX, schema ni casos de uso; `SettingsDialog` baja a menos de 1000 líneas y queda como siguiente deuda directa la configuración inicial. Además se registra como feature futura del Ribbon `Exportar` el modo `Video por texto del documento`, con fondo blanco por defecto, imagen de fondo opcional, tipografía, color, borde/sombra, región de texto y uso opcional de la imagen ya asignada al fragmento desde el sidebar derecho. Documento vigente: `DOCUMENTACION_ACTUAL/09_BITACORA_REFACTOR_PRESENTACION.md`.

## PRESENTATION-ORCHESTRATION-RF9 — coordinador de Voz local simple y Piper

Estado: implementada como extracción por dominio. `SettingsDialog` conserva la tarjeta visual de Voz local simple, los botones y los métodos delegadores históricos, pero la verificación, preparación, importación, selección y resumen de requisitos de Piper pasan a `PiperSettingsOperations`. No cambia UX, schema ni casos de uso; `SettingsDialog` baja a poco más de 1000 líneas y quedan como siguientes extracciones `VideoLocalSettingsOperations` y configuración inicial. Documento vigente: `DOCUMENTACION_ACTUAL/09_BITACORA_REFACTOR_PRESENTACION.md`.

## PRESENTATION-ORCHESTRATION-RF8 — coordinador de Voz IA avanzada y CUDA

Estado: implementada como extracción por dominio. `SettingsDialog` conserva la composición visual de Configuración y los botones de la tarjeta de Voz IA avanzada, pero la orquestación larga de CUDA, preparación, descarga, importación, smoke WAV, reproducción de prueba y selección del motor pasa a `AdvancedVoiceSettingsOperations`. No cambia UX, schema ni casos de uso; se reduce `SettingsDialog` a menos de 1300 líneas y queda preparado el mismo patrón para Piper, FFmpeg y configuración inicial. Documento vigente: `DOCUMENTACION_ACTUAL/09_BITACORA_REFACTOR_PRESENTACION.md`.

## PRESENTATION-ORCHESTRATION-RF7 — workers de Configuración al runner común

Estado: implementada como extracción mecánica y acotada. `SettingsDialog` deja de crear workers manuales con `Thread worker = new Thread` y delega todas sus operaciones largas de Configuración a `FxBackgroundTaskRunner`: CUDA/XTTS, descarga/importación de Voz IA avanzada, prueba y reproducción de smoke, Piper, FFmpeg embebido e instalación inicial de Voz local simple. No cambia UX, schema ni lógica de preparación; queda pendiente separar esos bloques en coordinadores por dominio para reducir tamaño y acoplamiento de `SettingsDialog`. Documento vigente: `DOCUMENTACION_ACTUAL/09_BITACORA_REFACTOR_PRESENTACION.md`.

## PRESENTATION-ORCHESTRATION-RF6 — orquestadores restantes y legacy documental

Estado: implementada como auditoría vigente y extracción técnica acotada. Se agrega `DOCUMENTACION_ACTUAL/10_AUDITORIA_ORQUESTADORES_Y_LEGACY.md` con el orden recomendado para reducir `DocuPodcastShellViewModel`, `SettingsDialog`, `VoiceLibraryWorkspaceView`, `LocalTtsProcessAudioGenerationGateway` y `DocuPodcastShellView`. La limpieza documental se limita a autoridad y señalización: `DOCUMENTACION_ACTUAL/` gobierna; `docs/`, `DOCUMENTACION/`, `DOCUMENTACION_ESTRATEGICA/` y `00_MEMORIA_PROYECTO/` siguen como archivo histórico. `EmbeddedDependencySetupAssistant` empieza a usar `FxBackgroundTaskRunner` para sus workers principales; `SettingsDialog` queda como siguiente migración por grupos.

## PRESENTATION-ORCHESTRATION-RF5 — Exportar y workers de presentación

Estado: implementada como refactor incremental del flujo `Exportar video`. `DocuPodcastShellView` conserva el comando, el `FileChooser` y la verificación de dependencias, pero delega propiedades de salida a `VideoExportOptionsDialog` y progreso/cancelación a `VideoExportProgressCoordinator`. Se agrega `FxBackgroundTaskRunner` para centralizar el arranque de `Task` en hilos daemon y se usa también en la importación documental del shell. El combo de FPS sigue visible al exportar, ahora como componente propio de `presentation.video`. Documento vigente: `DOCUMENTACION_ACTUAL/09_BITACORA_REFACTOR_PRESENTACION.md`.

## PRESENTATION-ORCHESTRATION-RF4 — Configuración por intención operativa

Estado: implementada como reorganización funcional de Configuración sin cambiar schema ni comportamiento de motores. El sidebar queda en siete secciones: Lectura, Reproducción, Motores y dependencias, Rendimiento, Video final, Almacenamiento y Soporte y diagnóstico. Se elimina la sección independiente Audio y se fusiona Voz y lectura dentro de motores. `SettingsFormModel` concentra el mapeo controles/settings para mantener `SettingsDialog` bajo el límite vigente. `SettingsSupportActions` conecta acciones reales del Shell para validar proyecto, revisar exportación, exportar reportes y abrir carpetas operativas. Documento vigente: `DOCUMENTACION_ACTUAL/09_BITACORA_REFACTOR_PRESENTACION.md`.

## PRESENTATION-ORCHESTRATION-RF3 — progreso de Configuración extraído

Estado: implementada como segunda extracción de presentación por caso de uso. `SettingsOperationProgressCoordinator` concentra el diálogo de progreso, heartbeat, cierre seguro y estados `finish/fail` usados por preparación de CUDA, Voz IA avanzada, Voz local simple y Video local. `SettingsDialog` baja a menos de 1800 líneas y conserva la orquestación específica de Configuración: confirmaciones, llamadas a use cases, mensajes finales y refresco de página activa. Documento vigente: `DOCUMENTACION_ACTUAL/09_BITACORA_REFACTOR_PRESENTACION.md`.

## PRESENTATION-ORCHESTRATION-RF2 — transporte de playback y contratos de vista

Estado: implementada como refactor incremental sin cambio visible de UX. Se crea `PlaybackTransportCoordinator` para concentrar JavaSound, continuidad de cue, cola secuencial/runtime, deadline y diagnóstico de playback. `DocuPodcastShellViewModel` baja de 2700 a menos de 2600 líneas y conserva selección, navegación, propiedades JavaFX y mensajes. Se fija en `DOCUMENTACION_ACTUAL/09_BITACORA_REFACTOR_PRESENTACION.md` el contrato operativo de Inicio, Documento, Voces, Configuración y Exportar, dejando workspaces heredados como internos/ocultos.

## RF-TX1-CONSOLIDACION-DOCUMENTAL — refactor consciente y autoridad vigente

Estado: implementada como primera tanda acotada de refactor consciente posterior a la centralización de runtime/procesos. `DOCUMENTACION_ACTUAL/` queda reforzada como única fuente de verdad operativa; `docs/`, `DOCUMENTACION/`, `DOCUMENTACION_ESTRATEGICA/` y `00_MEMORIA_PROYECTO/` quedan como archivo histórico/contexto, sin gobernar decisiones nuevas. Se documenta que PDF ya es aceptado como fuente, pero su soporte profundo queda para una futura línea de PDF avanzado. En Configuración se extrae la humanización de mensajes técnicos a `SettingsTechnicalMessageHumanizer`, usada por `SettingsDialog` y `EmbeddedDependencySetupAssistant`, sin cambiar comportamiento visible ni schema de settings.

## RUNTIME-PROCESOS-INTEGRACION1 — runner común y rutas embebidas operativas

Estado: implementada como cierre operativo de la tanda `RuntimeArtifactPaths` + `ExternalProcessRunner`. `DefaultExternalProcessRunner` queda como único dueño de `ProcessBuilder`; las ejecuciones de XTTS/Piper por chunks, prueba de voz, smoke CUDA, preparación CUDA/XTTS, FFmpeg de exportación/compresión/importación y discovery PowerShell pasan por el runner común con observador, timeout y cancelación. Los preflights operativos de arranque, motores, Piper y FFmpeg resuelven rutas desde `RuntimeArtifactPaths`. Se agregan tests unitarios del runner y guardarraíl fuente para bloquear `new ProcessBuilder` fuera del adaptador.

## XTTS FINAL MODELDIR GUARD HF4

Se refuerza la correccion de `model.pth/model.pth` en scripts PowerShell y wrapper Python. El script legacy `Voz IA avanzada-file-to-wav.ps1` ahora normaliza comillas/slash final, colapsa `model.pth`, y si la carpeta legacy esta incompleta usa `models/tts/xtts` cuando esta disponible.


## XTTS Final ModelDir Guard HF3

Se refuerza la reparacion definitiva del caso `model.pth/model.pth`: readiness, smoke WAV, gateway de prueba de voz y command template reparan settings antes de ejecutar. Ademas, ambos scripts PowerShell y el wrapper Python colapsan cualquier `ModelDir` terminado en `model.pth` a la carpeta padre y emiten marcas diagnosticas `model-normalizado`. Documento: `docs/productizacion/XTTS_FINAL_MODEL_PTH_GUARD_HF3.md`.


## RUNTIME-ARCH-RC1 — Consolidacion runtime/documental y guardarrailes

Base: correccion de settings XTTS legacy y script CUDA local. Se corrigen fallos de diagnostico relacionados con strings visibles de motores tecnicos en Configuracion y con script PowerShell no ASCII-safe. Se agrega documentacion `docs/productizacion/RUNTIME_ARCH_RC1_CONSOLIDACION.md` y guardarrail fuente para dejar como contrato RC que `RuntimeArtifactPaths`, `ExternalProcessRunner`, `ModelArtifactContract`, `ManagedDownloadService` y `OperationalSettingsMigrationPolicy` son las piezas transversales vigentes de runtime.


## XTTS-LEGACY-SETTINGS-REPAIR-HF2 + XTTS-PYTORCH-CUDA-INSTALL-HF1

Se agrega `OperationalSettingsMigrationPolicy` para reparar settings persistentes antiguos: comandos localizados/legacy de Voz IA avanzada se convierten a `engineMode=xtts` con comando administrado, y `storage.modelsDirectory` terminado en `model.pth` se normaliza a carpeta padre. Esto evita que `%USERPROFILE%/.docupodcast-studio/operational-settings.properties` arrastre rutas absolutas viejas como `componentes locales IA avanzada-wrapper` o `recursos locales IA avanzada/model.pth`. También se agregan scripts `scripts/tts/setup-xtts-pytorch-cuda.ps1` y `scripts/39-preparar-pytorch-cuda-xtts.bat` para preparar PyTorch CUDA dentro del Python local, sin tocar Python global. Piper queda documentado como CPU principalmente.

## MODEL-ARTIFACT-CONTRACT-RF1 + MANAGED-DOWNLOAD-RF1 — contratos de artefactos y descargas

Se corrige primero el diagnóstico `20260607-115938`: `InspectXttsCudaSmokeUseCase` vuelve a referenciar explícitamente `XttsCudaSmokeReport.MANIFEST_NAME` sin abandonar `RuntimeArtifactPaths`, y `LocalTtsProcessConfiguration` normaliza argumentos legacy `-ModelDir`/`--model-dir` para evitar que comandos antiguos o localizados de Voz IA avanzada pasen `.../model.pth` como carpeta y produzcan `model.pth/model.pth`. Se agregan `ModelArtifactContract`, `ModelArtifactRequirement` y `RuntimeArtifactInspection` para describir artefactos locales de XTTS, Piper y FFmpeg. También se agregan contratos base `ManagedDownloadService`, `DownloadRequest`, `DownloadProgress`, `DownloadResult` y `DownloadResumePolicy` para futuras descargas transversales. Documento: `docs/productizacion/MODEL_ARTIFACT_MANAGED_DOWNLOAD_RF1.md`.

## XTTS-MODEL-PATH-HF1 — normalización de model.pth

Hotfix de Voz IA avanzada: el wrapper Python y el puente PowerShell normalizan `--model-dir` cuando llega una ruta que termina en `model.pth`, evitando `model.pth/model.pth` durante prueba WAV. Documento: `docs/productizacion/XTTS_MODEL_PATH_HF1_MODEL_PTH_NORMALIZADO.md`.

## FIRST-USE-ONBOARDING1 — primera experiencia orientada a escuchar rápido

Se actualiza Inicio, Configuración inicial y guía integrada para que el primer uso prepare Voz local simple y permita escuchar documentos sin esperar Voz IA avanzada. Voz IA avanzada queda como mejora posterior y Video local como requisito de MP4 final. Documento: `docs/productizacion/FIRST_USE_ONBOARDING1_ESCUCHAR_RAPIDO.md`.

## DOCUMENT-SIDEBAR-VOICE-UX1 — Documento separa Voz generada y Audio del computador


# VOICE-CHUNKS-HF2 — barra de estado, dependencia XTTS y reanudación de buffer

Estado: hotfix funcional tras prueba visual. Se fija `transformers==4.44.2` para evitar el error `BeamSearchScorer` del runtime local de Voz IA avanzada, se acortan botones de la barra de estado, se corrige el estado presionado y se evita saltar del título seleccionado al primer cue disponible. Documento: `docs/productizacion/VOICE_CHUNKS_GPU_STATUS_HF2_STATUSBAR_RUNTIME_BUFFER.md`.


El módulo Audio del sidebar izquierdo ahora distingue de forma explícita **Voz generada** de **Audio del computador**. El campo de voz queda como `Voz`; el campo de emoción queda como `Tono`; los ComboBox usan `VoiceToneLabelPolicy.comboLabel(...)` y solo muestran nombres simples de tonos realmente registrados para la voz elegida. Se mantiene la regla de que las muestras de voz son referencias para generar texto nuevo, no clips fijos. Documento técnico: `docs/productizacion/DOCUMENT_SIDEBAR_VOICE_UX1_VOZ_IA_AUDIO_LOCAL.md`.

## VOICE-LIBRARY-SYNC1 — Biblioteca de voces de la app y Documento sincronizado

Las muestras de voz registradas por el usuario quedan en la biblioteca de voces de la app/runtime (`voice-library/samples/`) y no como archivos dentro de cada proyecto. La biblioteca activa se propaga a Documento: al registrar Neutral/Feliz/Triste para una voz, el sidebar de Documento refresca sus ComboBox y muestra solo las emociones con muestra real para esa voz. Documento técnico: `docs/productizacion/VOICE_LIBRARY_SYNC1_APP_LIBRARY_DOCUMENTO.md`.

## VOICE-GPU-DEVICE-HF2 — selector CPU/GPU compartido para motores de voz

- Corrige el contrato visual de `VOICE-CHUNKS-HF1`: Voz local simple no queda declarada CPU-only.
- El selector ahora se presenta como dispositivo para voz y se propaga también al wrapper de Voz local simple.
- `PiperTtsCommandTemplate` añade `-ComputePolicy`, `-Device` y `-GpuIndex`; `piper-file-to-wav.ps1` exporta hints de entorno para runtimes capaces de usar GPU.
- Voz IA avanzada conserva su compuerta CUDA específica: solo usa NVIDIA cuando el Python local la confirma.

## VOICE-CHUNKS-HF1 — GPU honesta, chunk seleccionado y avance del documento

- Corrige la interpretación de CUDA no disponible como error visual.
- Añade Renderizar desde aquí en la barra de estado.
- Separa Rehacer chunks de Seguir generando.
- Añade Documento N% y sincronización aproximada del índice lateral.


# VOICE-UX-POLISH1A — limpieza operativa de Vista Voces

Estado: implementada sobre `VOICE-PLAN-REFERENCIAS1` verde. Se limpia la Vista Voces sin introducir todavía el wizard: se quita `Ver estado de voces`, se elimina `Resumen de operación`, se corrige el estado de motor ilegible, `Sin prueba generada` queda como texto de estado, las filas de voz muestran tipo humano y solo tonos realmente registrados, y los ComboBox de tonos usan nombres simples mediante `VoiceToneLabelPolicy`. Documento: `docs/productizacion/VOICE_UX_POLISH1A_LIMPIEZA_OPERATIVA.md`.

# VOICE-PLAN-REFERENCIAS1 — registro de Vista Voces, wizard y muestras de referencia

Estado: documentación operativa agregada sobre `MOTOR-GPU-SMOKE1-HF2` / `PLAYBACK-SPEED-HF9` verde. Se actualiza el plan en piedra para dividir el bloque de voces en `VOICE-UX-POLISH1A`, `VOICE-REGISTRATION-WIZARD1`, `VOICE-LIBRARY-SYNC1` y `DOCUMENT-SIDEBAR-VOICE-UX1`. Queda fijado que Vista Voces administra voces y muestras de referencia para Coqui/XTTS; Documento asigna esas voces/tonos a fragmentos o permite usar audio local. Las muestras se graban con Java/API de audio, se guardan en la biblioteca de voces de la app/runtime y no dentro de cada proyecto. Documento: `DOCUMENTACION_ACTUAL/PLAN_IMPLEMENTACION_EN_PIEDRA/07_VOCES_VISTA_Y_WIZARD_REFERENCIAS.md`.

# MOTOR-GPU-SMOKE1-HF2 — probe CUDA robusto en Windows

Estado: hotfix aplicado sobre `PLAYBACK-SPEED-HF9` verde tras captura de Configuración donde la prueba GPU fallaba con `SyntaxError: unterminated string literal`. El smoke CUDA ya no usa `python -c` con un bloque largo; `ProcessXttsCudaRuntimeProbeGateway` escribe un `.py` temporal UTF-8, ejecuta el Python autocontenido con ese archivo y limpia el temporal al finalizar. Esto convierte el resultado en diagnóstico real de PyTorch/CUDA, no en falso negativo por comillas de Windows. Documento: `docs/productizacion/MOTOR_GPU_SMOKE1_HF2_PROBE_SCRIPT_WINDOWS.md`.

# PLAYBACK-SPEED-HF9 — transición inmediata a 1.5x/1.75x

Estado: implementada sobre `DOC-INDEX-PLAYBACK-HF1` verde. Corrige la sensación de silencio artificial entre chunks cuando el audio se reproduce a `1.5x` o `1.75x`. Se agrega `PlaybackTimingPolicy` para centralizar watchdog/deadline/polling/gracia, la cola secuencial y el deadline usan `duration / playbackRate` con margen breve, y `matchesCompletedAudioFile(...)` acepta de forma robusta el fin natural del WAV antes de caer a fallback. Documento: `docs/productizacion/PLAYBACK_SPEED_HF9_TIMING_REAL.md`.

# DOC-INDEX-PLAYBACK-HF1 — índice genera/reproduce desde el fragmento seleccionado

Estado: implementada sobre `MOTOR-GPU-SMOKE1` corregida. Corrige el caso donde el índice navegaba visualmente al bloque correcto, pero `Escuchar` podía construir proyección y luego caer a generación global desde el inicio si todavía no había manifest/audio. `listenToDocument()` ahora resuelve `selectedDocumentBlockId` después de preparar la proyección y llama a `submitAudioGenerationFromSegment(..., true)` antes de usar `submitAudioGeneration()` global. También se corrige `MOTOR-GPU-SMOKE1` para no importar infraestructura desde aplicación y para no exponer nombres técnicos en strings de presentación. Documento: `docs/productizacion/DOC_INDEX_PLAYBACK_HF1_GENERAR_DESDE_INDICE.md`.

# MOTOR-GPU-SMOKE1 — GPU real dentro del Python autocontenido

Estado: implementada sobre `MOTOR-ADV-READY-GATE1` verde. Se agrega smoke CUDA local para Voz IA avanzada con `RunXttsCudaSmokeUseCase`, `InspectXttsCudaSmokeUseCase` y manifiesto `runtime/tts/xtts-smoke/xtts-cuda-smoke.json`. `SettingsAwareAudioGenerationGateway` solo permite `cuda:N` cuando el Python autocontenido confirma PyTorch + CUDA; si no, Voz IA avanzada cae a CPU de forma explícita. Configuración > Rendimiento agrega `Probar GPU para Voz IA avanzada`. Documento: `docs/productizacion/MOTOR_GPU_SMOKE1_CUDA_RUNTIME_LOCAL.md`.

# MOTOR-ADV-READY-GATE1 — Voz IA avanzada descargada vs usable para documentos

Estado: implementada sobre `PLAN_IMPLEMENTACION_EN_PIEDRA`. Cierra la primera tanda funcional del plan maestro: Voz IA avanzada ya no queda usable para generar chunks de documento solo por tener runtime/modelo configurados. Se agrega `InspectXttsDocumentGenerationReadinessUseCase` + `XttsDocumentGenerationReadinessReport`, el preflight usa `NEEDS_VERIFICATION` cuando falta prueba WAV real, y `SettingsAwareAudioGenerationGateway` bloquea generación documental avanzada hasta que exista `generatedWavProof()`. Documento: `docs/productizacion/MOTOR_ADV_READY_GATE1_USABLE_REAL.md`.

# MOTOR-TTS-ADV-HF2 — runtime real de Voz IA avanzada sin contaminación de modo

Estado: implementada sobre VIDEO-EXPORT1-HF verde. El ZIP de `jobs` mostró que el proceso sí llegaba al wrapper Python y a `cargando_modelo`, pero el job se presentaba como Voz local simple y enviaba `cuda:0` aunque el Python local caía a CPU. Se corrige la resolución de comandos gestionados para ignorar plantillas absolutas viejas, se etiqueta el motor por comando efectivo, se fuerza CPU para Voz IA avanzada hasta tener smoke CUDA real y se publican fases `tts_process_phase` para que la UI no parezca congelada durante carga de modelo/síntesis. Documento: `docs/productizacion/MOTOR_TTS_ADV_HF2_RUNTIME_REAL.md`.

# DOC-PERF/MEM1 — índices ligeros para documentos grandes

Estado: implementada sobre PLAYBACK-SELECT1/MODEL-PORT1. Corrige el diagnóstico `20260606-170202.zip` reduciendo `DocuPodcastShellViewModel` bajo el límite transitorio RF-TX1 y optimiza el lector para documentos grandes con `blockIndexById` y `sentenceSpanIndex`, evitando reescaneos completos durante navegación, selección y actualización visual. Documento: `docs/productizacion/DOC_PERF_MEM1_INDICES_LIGEROS.md`.

# MAVEN-DIAG-HF1 — tests fuente no leen artefactos binarios de motores

Estado: hotfix aplicado tras diagnóstico `20260606-161721.zip`. Corrige el fallo de Maven tests en `NamespaceMigrationSourceTest` causado por intentar leer como texto artefactos binarios descargados en `models/tts/xtts/`, especialmente `model.pth`. El test ahora audita solo archivos textuales del proyecto y excluye carpetas locales/generadas como `models`, `tools`, `jobs`, `runtime`, `exports`, `target` y `.git`. Documento: `docs/productizacion/MAVEN_DIAG_HF1_BINARY_ASSETS_SOURCE_TEST.md`.

# DOC-INDEX-HF13 — índice navegable del documento

Estado: implementada sobre EXPORT-CLEAN1 verde. Se agrega `DocumentIndexPanel` como módulo lateral plegable `Índice` dentro del workspace Documento. Usa `TreeView<IndexEntry>` para títulos/secciones/subsecciones; cuando no hay encabezados, ofrece navegación plana por bloques narrables y visuales. El salto llama a `selectBlock(...)`, por lo que funciona también con la ventana virtualizada de documentos grandes. No se reintroduce el panel viejo `Estructura documental`. Documento: `docs/productizacion/DOC_INDEX_HF13_INDICE_NAVEGABLE.md`.

# EXPORT-CLEAN1 — exportaciones técnicas fuera del flujo común

Estado: implementada sobre MOTOR-SMOKE4R-HF5 verde. El menú `Exportar`, Ribbon y toolbar quedan centrados en salidas finales de producto: audio final, video MP4, estado y carpeta de exportaciones. `EXPORT_PROJECT_BUNDLE` y `EXPORT_DIAGNOSTIC_REPORT` se conservan, pero se mueven a `Ayuda > Soporte avanzado` con etiquetas de soporte. Guardarraíl: `ExportClean1SupportSurfaceSourceTest`. Documento: `docs/productizacion/EXPORT_CLEAN1_EXPORTACIONES_SOPORTE_AVANZADO.md`.

# MOTOR-SMOKE4R-HF2 — etiqueta visible de modelo sin nombres técnicos

Estado: hotfix aplicado sobre MOTOR-SMOKE4R-HF1 tras diagnóstico `20260606-121332`. Corrige los fallos de `VoiceLocalSimpleT121V06SourceTest` y `AdvancedVoiceProgressPf2DSourceTest`: `SettingsDialog` ya no expone `XTTS` ni `/resolve/main` en cadenas visibles de presentación. La UI usa `Página oficial del modelo de voz`; la normalización técnica queda en aplicación/descargador. Documento: `docs/productizacion/MOTOR_SMOKE4R_HF2_PRESENTACION_URL_MODELO_VOZ.md`.

# MOTOR-SMOKE4R-HF1 — URL real de Coqui/XTTS y normalización de descarga

Estado: hotfix aplicado sobre MOTOR-SMOKE4R / COQUI-DL1. Corrige la fuente de descarga de Voz IA avanzada para no mostrar ni conservar `/resolve/main` como página navegable. La configuración usa la página oficial del modelo `https://huggingface.co/coqui/XTTS-v2` y el descargador construye internamente URLs por archivo. Si el usuario pega `https://github.com/coqui-ai/TTS`, la app lo reconoce como fuente de código Coqui TTS y usa el repositorio oficial del modelo para artefactos XTTS-v2, evitando construir recursos inexistentes. Documento: `docs/productizacion/MOTOR_SMOKE4R_HF1_URL_COQUI_XTTS.md`.

# MOTOR-SMOKE4R / COQUI-DL1 — prueba WAV real de Voz IA avanzada

Estado: implementada sobre VOZ-UX4R-3D verde. Configuración > Voz IA avanzada agrega botón `Probar` para ejecutar una prueba corta con el gateway real de síntesis y generar `runtime/tts/xtts-smoke/xtts-readiness-smoke.wav`. Se agregan `RunXttsReadinessSmokeUseCase`, `InspectXttsSmokeTestUseCase` y `XttsSmokeTestReport`. El preflight ya distingue descargado/seleccionable de prueba WAV generada y reproducción confirmada. `DownloadXttsOfficialModelUseCase` soporta reanudación parcial con HTTP `Range` para descargas grandes. Documento: `docs/productizacion/MOTOR_SMOKE4R_COQUI_DL1_PRUEBA_WAV.md`.

# VOZ-UX4R-3D — catálogo teatral de tonos en Vista Voces

Estado: implementada sobre VOZ-TTS5B verde. La Vista Voces ahora carga Neutral, tonos recomendados y el catálogo teatral extendido en el ComboBox de tono/emoción. Se agregó una sección sobria `Catálogo teatral de tonos` con filas e `InfoBadge`, sin dashboard, que explica cuántos tonos existen y cuántas muestras tiene registrada la voz seleccionada. Documento sigue siendo honesto: solo muestra emociones con muestra registrada. Guardarraíl: `VoiceUx4R3DTheatricalToneCatalogSourceTest`. Documento: `docs/productizacion/VOZ_UX4R_3D_CATALOGO_TEATRAL_TONOS.md`.

# VOZ-TTS5B — render TTS usa voz y tono real

Estado: implementada sobre VOZ-TTS5A verde. Documento ya filtraba voces y tonos reales; esta tanda conecta esa verdad con la generación: `AudioGenerationRequest` lleva la `VoiceLibrary`, cada `AudioGenerationUnit` resuelve su muestra por `voiceProfileId` + `performanceStyleId`, y `LocalTtsProcessAudioGenerationGateway` pasa la muestra al comando TTS mediante `commandFor(..., referenceSample)`. Si falta el tono solicitado, se usa Neutral de la misma voz. No se tocaron UI, playback ni descargas de Voz IA avanzada. Documento: `docs/productizacion/VOZ_TTS5B_RENDER_VOZ_TONO_REAL.md`.

## VOZ-TTS5A — Documento filtra voces y tonos reales

**Motivo:** Documento mostraba el catálogo completo de tonos aunque una voz no tuviera esas muestras registradas. Eso podía prometer emociones falsas en Voz IA avanzada.

**Cambios clave:**

- `DocumentAudioNarrationPanel` ya no usa `VoiceReferenceTone.values()` para llenar el ComboBox de Documento.
- En Voz IA avanzada, Documento solo muestra voces con muestra Neutral registrada en `VoiceReferenceSampleSet`.
- Al elegir una voz, los tonos salen de `registeredTones()` para esa voz, con Neutral primero.
- Si no hay voces avanzadas con Neutral, el panel muestra aviso humano y dirige a la biblioteca de voces.
- Voz local simple conserva UI sin emociones ni muestras humanas.
- Guardarraíl: `VoiceTts5ADocumentRealVoiceToneSourceTest`.

Documento: `docs/productizacion/VOZ_TTS5A_DOCUMENTO_VOCES_TONOS_REALES.md`.

## VOZ-UX4R-3C-HF1 — hotfix compilación Vista Voces

Hotfix aplicado tras diagnóstico local `20260606-095144.zip`. Corrige el fallo Maven en `VoiceLibraryWorkspaceView.java` causado por la referencia residual `box.getChildren().addAll(summary, list);` después de reemplazar el dashboard por filas sobrias. La vista ahora agrega `list` directamente y el guardarraíl `VoiceUx4R3CAntiDashboardSourceTest` bloquea la regresión. No toca motores, playback ni Documento. Documento: `docs/productizacion/VOZ_UX4R_3C_HF1_COMPILE_FIX.md`.

## VOZ-UX4R-3C — sidebar oscuro y Vista Voces sin dashboard

**Motivo:** la navegación modular de Voces ya existía, pero Inicio conservaba métricas/tarjetas tipo dashboard y el sidebar seguía claro. La vista debía acercarse a una superficie administrativa sobria, formal y moderna, inspirada en la paleta tipo Teams.

**Cambios clave:**

- `VoiceLibraryWorkspaceView` elimina `metricCard(...)`, `voice-dashboard-metrics` y `voice-metric-card`.
- Inicio usa filas operativas (`homeOperationalSummary` / `homeStatusRow`) en vez de tarjetas de métricas.
- Configurar motor presenta modos en filas (`engineModeRow`) con `InfoBadge`, no como tarjetas redundantes.
- Las acciones de Voces reutilizan `ActionBar` donde aplica.
- `VoiceModuleNavigation` queda oscuro y full-height con `setMaxHeight(Double.MAX_VALUE)` y spacer expansible.
- Guardarraíl: `VoiceUx4R3CAntiDashboardSourceTest`.

Documento: `docs/productizacion/VOZ_UX4R_3C_SIDEBAR_OSCURO_ANTI_DASHBOARD.md`.

## DOC-UX-HF10H — progreso legible, tamaño generado y rail Visual honesto

## PLAYBACK-SPEED3 — continuidad al cambiar velocidad

**Motivo:** tras `PLAYBACK-SPEED2`, la velocidad con tono natural funcionaba durante el chunk activo, pero podía romper continuidad al terminar el fragmento o repetir una unidad en fronteras/casos de buffer.

**Cambios clave:**

- `setPlaybackRate(...)` resincroniza el reloj desde el cue activo y la posición local real del reproductor.
- `tickPlayback()` prioriza `playingCueSegmentId` para saber qué unidad está sonando realmente.
- Si el reproductor terminó y el reloj ya no bloquea el cue, se permite avanzar al siguiente aunque la posición calculada no haya llegado exactamente al final.
- El autostart por buffer ya no usa cualquier cursor pausado como permiso para arrancar desde la selección; los gaps se recuperan con `waitingForBufferedSegmentAfter`.

**Resultado esperado:** cambiar entre `1x`, `1.5x` y `1.75x` no debe cortar la reproducción continua ni repetir el mismo fragmento.

- Overlay de audio: barra más visible, verde y con ETA en horas.
- Tamaño: se muestra audio generado en disco, no estimación fluctuante.
- Rail Visual: contador de fragmentos y virtualización explicada; se evita scroll horizontal.
- Configuración Video local: URL centralizada e importación desde carpeta quedan diferenciadas.

# Registro de tandas recientes — HF8/HF9/HF10G

## DOC-UX-HF10G — memoria, rail virtual, overlay operable y tablas sin truncado

**Motivo:** la tanda HF10F todavía permitía que documentos grandes saturaran JavaFX: el rail Visual podía materializar miles de tarjetas, el overlay recibía demasiadas actualizaciones y las tablas podían cortar texto.

**Cambios clave:**

- `DocumentMediaRailView` usa `ListView<DocumentFragmentRailPresentation>` virtualizado.
- Se agrega `AudioStatusUiThrottle` para coalescer estados de progreso antes de `Platform.runLater`.
- `DocuPodcastShellViewModel` no reconstruye manifest por cada chunk en generación pura sin playback activo.
- `SourceTableGridView` elimina truncado visual: wrap, alto variable, `OverrunStyle.CLIP` y sin resumen de filas restantes.
- `DocxDocumentImporter` conserva texto completo de celdas y todas las filas/columnas en el markdown interno de tabla.
- `scripts\01-ejecutar-app.bat` usa `DOCUPODCAST_APP_HEAP=-Xmx2048m` por defecto y permite override a `-Xmx4096m`.

**Resultado esperado:** abrir/generar audio en documentos grandes deja de provocar OOM por el rail derecho, el overlay se puede ocultar y las tablas crecen verticalmente sin puntos suspensivos innecesarios.

Documento: `docs/productizacion/DOC_UX_HF10G_MEMORIA_RAIL_OVERLAY_TABLAS.md`.


Este registro documenta las tandas implementadas durante la fase de estabilización final alrededor de playback, Documento, Visual, motores y documentación.

## PLAYBACK-HF8R — cola por duración real del WAV

**Motivo:** el audio por chunks podía saltar al siguiente fragmento antes de que terminara el WAV real porque el manifest usaba duración estimada por texto.

**Cambios clave:**

- Se agregó medición de duración real de WAV.
- La generación TTS guarda duración medida, no duración estimada.
- Los jobs persistidos pueden reparar duraciones antiguas desde WAV existente.
- El shell usa reloj de cue para no avanzar antes de tiempo.

**Resultado esperado:** el fragmento 2 espera a que termine el fragmento 1 antes de reproducirse.

## DOC-UX-HF9 — selección, rail, audio y DOCX visual

**Motivo:** la selección visual marcaba bloques/párrafos completos y el rail no reflejaba correctamente imágenes por frase.

**Cambios clave:**

- Selección por oración/rango de texto.
- Rail Visual reconstruido desde documento, guion y capas.
- Panel de Audio sincronizado con motor activo.
- Importación DOCX reforzada para imágenes internas, tablas y fórmulas detectadas.

## DOC-UX-HF9A — hotfix de compilación y refresco visual Word

**Motivo:** error de compilación por variable no efectivamente final en lambda y necesidad de refrescar visuales internos sin invalidar audio.

**Cambios clave:**

- Hotfix de lambda en ViewModel.
- Refresco de visuales internos del Word aunque el texto narrable no cambie.
- Mensaje humano para guardar proyecto tras actualizar visuales.

## DOC-UX-HF9B — rail por frase, reproducción de selección y visuales Word

**Motivo:** el rail tenía pocas tarjetas frente a la cantidad real de frases; reproducir selección desde frases intermedias podía no funcionar.

**Cambios clave:**

- Una tarjeta por frase/fragmento en el rail.
- Resolver dedicado para reproducir desde la frase seleccionada.
- Corrección para que imágenes Word no se degraden a ignoradas.
- Mejor sincronía entre frase activa, rail y documento.

## DOC-UX-HF9C — diagnóstico verde y sidebar Imagen sincronizado

**Motivo:** el rail derecho mostraba miniaturas correctas, pero el sidebar izquierdo podía seguir diciendo “Sin imagen asociada”.

**Cambios clave:**

- El panel izquierdo de Imagen resuelve la misma capa visual que el rail derecho.
- Ajustes a pruebas fuente y reading profile.
- Conservación de imágenes Word como bloques visuales no narrables.

## DOC-UX-HF9D — menú contextual de imagen en rail Visual

**Motivo:** el usuario necesita reutilizar una imagen en el fragmento anterior o posterior sin repetir FileChooser manual.

**Cambios clave:**

- Clic derecho sobre tarjeta del rail Visual.
- Opciones: asignar imagen al fragmento anterior/posterior.
- Opciones deshabilitadas si no hay imagen o no hay vecino.
- Tras copiar, se selecciona el destino y el sidebar izquierdo se actualiza.

## DOC-UX-HF9E — playbar, generación e imagen sincronizada

**Motivo:** el playbar estaba demasiado acoplado a generación; anterior/siguiente podían repetir la frase; faltaba mensaje humano si motor no estaba disponible.

**Cambios clave:**

- Pausar/detener playback ya no cancela generación.
- Controles de generación pasan a status bar.
- Diálogo humano de motor no disponible con opción de abrir Configuración.
- Botón de reproducir desde inicio.
- Anterior/siguiente avanzan por cue real y se deshabilitan en límites.
- Botón morado del playbar con borde blanco para contraste.

## DOC-UX-HF9F — importación con progreso y playback más estable

**Motivo:** abrir documentos grandes podía bloquear la UI; algunas frases se repetían ocasionalmente al reproducir.

**Cambios clave:**

- Diálogo de importación documental con Task/hilo de fondo.
- Separación de importar documento y adjuntar/renderizar documento.
- Corrección de resolución de cue dentro de segmentos con varias unidades.

## DOC-UX-HF9G — importación simple y tamaño estimado de audio

**Motivo:** el diálogo decía “preparando fragmentos” al abrir documento, aunque abrir fuente no debe preparar audio.

**Cambios clave:**

- Mensaje de apertura: solo leer documento para mostrarlo.
- Overlay de generación muestra tamaño estimado de chunks cuando hay WAVs generados.
- Diálogo intenta cerrarse antes de adjuntar el documento pesado.

## DOC-UX-HF9H — diálogo de importación y URLs de descarga editables

**Motivo:** la ventana secundaria de apertura podía quedarse congelada; las URLs de descarga de motores debían centralizarse y ser editables.

**Cambios clave:**

- Botón Ocultar en diálogo de importación.
- Límite inicial de render para documentos grandes.
- Campos de URL editables para Voz IA avanzada y Voz local simple.
- Persistencia en `operational-settings.properties`.
- Overrides por variables de entorno y system properties.

## DOC-UX-HF9I — documentación actual y limpieza de guardarraíles de motores

**Motivo:** los tests detectaron nombres técnicos en strings visibles y la documentación vigente estaba mezclada con histórico.

**Cambios clave:**

- Strings visibles ya no muestran nombres técnicos ni variables técnicas de entorno.
- Los downloaders conservan URLs oficiales como constantes internas para trazabilidad y guardarraíles.
- Se crea `DOCUMENTACION_ACTUAL/` como fuente vigente de cierre.
- La documentación histórica se conserva por compatibilidad, pero queda marcada como histórica.

## DOC-PERF-HF10 — lector con ventana virtual para documentos grandes

**Motivo:** documentos de miles de bloques podían hacer lenta la vista Documento porque JavaFX intentaba mantener demasiados nodos en pantalla. Aunque la importación ya ocurría en segundo plano, el render inicial seguía siendo pesado.

**Cambios clave:**

- Se agregó `DocumentRenderWindow` como proyección inmutable de una ventana de bloques.
- `DocumentWorkspaceView` ya no intenta pintar todos los bloques de documentos grandes.
- La vista muestra una ventana acotada alrededor del punto actual.
- Se agregan controles visibles: bloques anteriores, siguientes bloques, inicio y final del documento.
- Si el playback, rail o selección apuntan a un bloque fuera de la ventana actual, la vista cambia de ventana y desplaza el bloque a la zona de lectura.
- La fuente documental completa sigue viva en el modelo; solo se limita la cantidad de nodos JavaFX renderizados.

**Resultado esperado:** abrir documentos grandes deja de convertir el lector en una superficie pesada de miles de nodos; el scroll trabaja sobre una ventana cercana al punto activo.

## DOC-PERF-HF10B — autoventana, status de chunks y URL de video local

**Motivo:** tras DOC-PERF-HF10 el lector ya abría documentos grandes con una ventana virtual, pero el usuario todavía debía avanzar manualmente con botones; además el playbar podía interferir con el scrollbar y la generación de audio seguía demasiado asociada al flujo de reproducción.

**Cambios clave:**

- La ventana virtual avanza automáticamente cuando el usuario llega al borde inferior o superior del ScrollPane.
- El playbar reduce su ancho máximo y deja más margen derecho para no tapar el área del scrollbar.
- La barra de estado muestra una acción independiente **Generar chunks de audio**, separada del botón de reproducción.
- La recuperación de buffer usa el ID de unidad/cue, no solo el segmento, para evitar repetir una oración al continuar después de esperar nuevos chunks.
- Se centraliza la URL de descarga de video local/FFmpeg igual que las URLs de Voz IA avanzada y Voz local simple.
- `operational-settings.properties` guarda `download.ffmpeg.runtimeZipUrl` y permite override por variable de entorno o system property.

**Resultado esperado:** documentos grandes se recorren con una experiencia más cercana a un lector paginado/virtual, la barra flotante no bloquea el scrollbar, la generación de audio puede iniciarse sin reproducir inmediatamente y la configuración de video local queda preparada para automatizar FFmpeg en una tanda posterior.

## DOC-PERF-HF10C — corrección de guardarraíles, scroll y documentación operativa

**Motivo:** tras DOC-PERF-HF10B el diagnóstico completo detectó tres regresiones de guardarraíl: el CSS de Documento superó el presupuesto humano, el playbar perdió el contrato literal histórico de margen superior y el test de operación primaria esperaba que la barra flotante siguiera anclada con el mismo margen. Además, el usuario aclaró que toda tanda nueva debe quedar documentada en `DOCUMENTACION_ACTUAL/` porque la conversación puede truncarse.

**Cambios clave:**

- Se separó el CSS de documentos grandes a `css/document/large-document.css` para mantener `document-page.css` bajo el presupuesto modular.
- `docupodcast-light.css` importa explícitamente `document/large-document.css`.
- El playbar conserva el margen histórico `new Insets(6, 24, 0, 24)` exigido por los guardarraíles, pero limita su ancho máximo a 820 px y usa `setPickOnBounds(false)` para reducir interferencia con el scrollbar.
- Se documenta que **Generar chunks de audio** usa el flujo de generación de audio ya existente, enviado al gateway de audio en segundo plano mediante `AudioWorkflowCoordinator`/`AudioGenerationGateway`; la UI no debe ejecutar TTS pesado en el hilo JavaFX.
- Se deja como decisión futura `DOC-INDEX` para navegación por índice/árbol de documento y salto por bloque/chunk, especialmente útil en Word/Markdown con encabezados.

**Resultado esperado:** el diagnóstico vuelve a quedar verde, la barra flotante mantiene el contrato visual anterior sin bloquear tanto el scrollbar, y el repositorio conserva memoria escrita de la tanda.


## DOC-UX-HF10D — generación de chunks con overlay reabrible

**Motivo:** el botón **Generar chunks de audio** de la barra de estado podía parecer inerte o quedar visualmente desconectado del panel flotante de generación. El usuario necesita iniciar generación sin reproducir, ver el panel semitransparente de progreso, ocultarlo sin detener el job y reabrirlo después como detalle de generación.

**Cambios clave:**

- El botón de barra de estado abre/expande el panel de generación antes de disparar la acción.
- Cuando hay generación activa y el panel está oculto, el botón visible pasa a **Mostrar detalles de generación**.
- El overlay usa el texto **Generando chunks de audio** y aclara que ocultarlo no cancela el proceso.
- Se conserva la separación: playback controla reproducción; status bar/overlay controla generación.

**Resultado esperado:** el usuario puede dejar generando chunks en segundo plano, ocultar el panel y volver a mostrar detalles sin detener el proceso.

## DOC-TABLE-HF11 — tablas DOCX como grilla visual sobria

**Motivo:** las tablas DOCX se detectaban, pero se mostraban como texto/Markdown bruto, poco legible para el usuario.

**Cambios clave:**

- `SourceVisualBlockView` agrega `tableGrid(...)` para renderizar tablas fuente como `GridPane` de solo lectura.
- `DocumentWorkspaceView` usa la grilla para bloques `TABLE_NOTICE`.
- `source-visual.css` agrega estilos `ui-source-table-grid`, `ui-source-table-cell`, `ui-source-table-header-cell` y `ui-source-table-more`.
- La tabla sigue siendo bloque visual no narrable por defecto; no se promete edición tipo Excel.

**Resultado esperado:** una tabla de Word se ve como cuadricula sobria dentro del documento, no como cadena Markdown comprimida.

## VIEW-HOME-HF12 — volver a Inicio desde la pestaña Vista

**Motivo:** el usuario necesita volver a la pantalla inicial/bienvenida sin cerrar el proyecto ni perder el documento.

**Cambios clave:**

- El comando **Inicio** queda permitido en Ribbon.
- La pestaña **Vista** incluye **Inicio** junto a Documento y Voces.
- El comando reutiliza `SHOW_WELCOME` y conserva el proyecto abierto.

**Resultado esperado:** desde Vista se puede volver a la pantalla inicial de forma natural, como vista principal más del producto.


## DOC-TABLE-HF11B / DOC-UX-HF10E — generación de chunks desde status bar y tabla transversal

**Motivo:** el botón `Generar chunks de audio` aparecía en la barra de estado pero podía no iniciar nada cuando el documento aún no tenía proyección narrable interna. Además, las tablas DOCX ya eran legibles, pero necesitaban consolidarse como componente visual transversal y no como una grilla improvisada.

**Cambios:**
- El botón `Generar chunks de audio` ahora prepara la proyección narrable si hace falta y luego inicia la generación sin comenzar reproducción.
- Si el overlay se oculta, la generación sigue y la barra de estado permite restaurar los detalles.
- Se agrega `SourceTableGridView` como componente transversal de tablas fuente.
- La cabecera de tabla usa color morado sólido; las celdas quedan blancas, sobrias y legibles.
- Se mantiene la tabla como bloque visual no narrable.

**Validación esperada:** diagnóstico completo verde y prueba manual de botón de chunks + render de tablas.

## DOC-UX-HF10F — visuales fuente limpios y overlay de generación no bloqueante

**Motivo:** después de `DOC-UX-HF10E`, las tablas ya se renderizaban como grilla, pero visualmente seguían dentro de demasiados contenedores y con texto redundante. Además, el panel flotante de generación de chunks podía sentirse congelado: el texto quedaba comprimido, el botón **Ocultar** no siempre respondía y la UI podía entrar en estado “No responde” durante jobs grandes.

**Cambios clave:**

- `SourceTableGridView` se consolida como componente transversal de tabla fuente de ancho flexible.
- Las tablas dejan de usar contenedores visuales redundantes y se renderizan como grilla directa dentro del bloque del documento.
- La cabecera de tabla conserva color morado sólido; las celdas quedan blancas, legibles y con padding de producto.
- Las imágenes fuente conservan contexto mínimo, pero se elimina el host visual interno innecesario para reducir paneles anidados.
- `LongProcessOverlayView` amplía el ancho del panel de generación para que ETA, contador y tamaño estimado sean legibles.
- El cálculo de tamaño estimado de chunks deja de ejecutarse en el hilo JavaFX: ahora se hace mediante un executor daemon (`docupodcast-audio-size-probe`) y vuelve a la UI con `Platform.runLater`.
- `DocuPodcastShellViewModel.acceptAudioStatus(...)` deja de reconstruir el manifest de playback en cada actualización cuando la acción es solo generar chunks en segundo plano. Solo reconstruye cuando hay reproducción, buffer pendiente o finalización.
- Se mantiene la regla: generar chunks es paralelo a la UI; JavaFX solo observa progreso y nunca debe ejecutar TTS pesado ni escanear jobs grandes en su hilo principal.

**Resultado esperado:** el botón **Ocultar** del overlay responde mejor, el documento se puede seguir usando mientras se generan chunks, el texto del overlay queda legible y las tablas/imágenes fuente se ven menos encerradas en paneles innecesarios.

## PLAYBACK-SPEED1 — controles de velocidad en playbar

Se agregan botones `1x`, `1.5x` y `1.75x` en la playbar del Documento. La velocidad afecta playback de WAV ya generados, no generación TTS. `JavaSoundSegmentAudioPlayer` aplica velocidad simple por frecuencia efectiva de salida, `PlaybackCueClock` sincroniza cursor/visual al ritmo acelerado y el buffer existente sigue esperando si la lectura rápida alcanza un chunk pendiente. Documento: `docs/productizacion/PLAYBACK_SPEED1_CONTROLES_VELOCIDAD.md`.


## PLAYBACK-SPEED2 — continuidad y tono natural

**Motivo:** `PLAYBACK-SPEED1` agregó botones `1x`, `1.5x` y `1.75x`, pero la primera aceleración cambiaba el tono por frecuencia efectiva y la continuidad podía quedar frágil si se cambiaba velocidad durante un fragmento.

**Cambios clave:**

- `JavaSoundSegmentAudioPlayer` usa `PcmTimeStretchProcessor.speedUpPreservePitch(...)` para `1.5x` y `1.75x`.
- Se abandona la aceleración por multiplicación del sample rate de salida.
- La playbar conserva `setMaxWidth(820)` para no interceptar scrollbars.
- Al pasar al siguiente cue, el ViewModel valida si el siguiente WAV arrancó; si falta buffer y hay job activo, espera en vez de quedar silencioso.
- La acción principal aclara `Reproducir desde selección`; el botón corto queda como `Reproducir fragmento (solo este)`.

**Resultado esperado:** la lectura acelerada mantiene tono más natural y sigue de largo entre chunks, incluso después de cambiar velocidad durante un fragmento.

## PLAYBACK-SPEED6 — controlador estructural de continuidad

Se extrajo la continuidad de playback a `PlaybackContinuationController` y se agregó callback real `SegmentAudioPlayer.setOnPlaybackFinished(Consumer<Path>)`. El avance entre chunks ya no depende solo del polling de `Timeline`: Java Sound notifica el final natural del WAV activo y el ViewModel transiciona al siguiente cue validando que el archivo terminado corresponde al cue vigente.

## PLAYBACK-SPEED7 — manifest runtime y cola reproducible

Se corrige la hipótesis principal tras PLAYBACK-SPEED6: la reproducción podía usar un manifest temprano/parcial aunque los WAV existieran. Se agrega `PlayableAudioJobSelector`, se reconstruye manifest en runtime antes de iniciar y después de cada cue, y se muestra un resumen humano del manifest activo para diagnosticar si la cola real tiene muchos chunks o solo pocos.


## PLAYBACK-CORE1 — diagnóstico runtime de reproducción

Se agrega diagnóstico visible de manifest/cue/WAV/player/cola en el status bar desplazable para aislar por qué la continuidad se detiene tras el primer fragmento.

## PLAYBACK-CORE3 — cola secuencial exacta de cues

- Se agrega `PlaybackSequentialQueueDriver` para que la continuidad de lectura no dependa de volver a resolver desde la selección ni del cue activo anterior.
- La cola reproduce el cue exacto por `unitId` y programa el siguiente por duración real y velocidad activa.
- El `Timeline` queda como apoyo de sincronización visual; la cola secuencial es la dueña del avance continuo.
- Se corrige el caso de cues múltiples dentro del mismo `segmentId`, evitando que `SEG-002-U002` pueda caer otra vez en `SEG-002-U001`.


## PLAYBACK-CORE4 — cola exacta guiada por final real del reproductor

- Reproduce por cola exacta de cues, pero prioriza el callback real del reproductor antes de avanzar.
- El watchdog de cola ya no corta audio si Java Sound todavía reporta reproducción activa.
- Corrige inconsistencias observadas al cambiar velocidad, pausar/reanudar y reproducir mientras se generan chunks.
- Documento técnico: `docs/productizacion/PLAYBACK_CORE4_PLAYER_CALLBACK_QUEUE.md`.

## VOZ-UX4R-1 — Lista sobria de voces y contrato vivo

- Corrige el guardarraíl pendiente de `PlaybackCore3SequentialQueueSourceTest` tras PLAYBACK-CORE5.
- Inicia la línea fina de Vista Voces sin tocar aún descarga Coqui/XTTS.
- Agrega `VoiceListItemView` para que la lista de voces no parezca tabla/CRUD.
- La UI habla de `Voces creadas` y muestra estados humanos: `Voz base lista`, `Neutral lista`, `Tonos configurados`, `Falta neutral`, `Incompleta`.
- Se elimina el lenguaje visible `Opcional` para tonos; ahora se habla de `Tono`.
- Documenta el mapa restante de VOZ-UX4R y mantiene MOTOR-SMOKE4R para descarga/verificación de Voz IA avanzada.

## MOTOR-PLAYCONF1 — confirmación de reproducción interna de Voz IA avanzada

Se completa el ciclo de prueba de Voz IA avanzada agregando reproducción/confirmación del WAV de preparación desde Configuración. `ConfirmXttsSmokePlaybackUseCase` reproduce el archivo generado mediante el reproductor interno y marca `playbackConfirmed=true` solo cuando recibe final natural del audio. La app usa un reproductor Java Sound separado para no interferir con la cola de lectura del Documento. Documento técnico: `docs/productizacion/MOTOR_PLAYCONF1_CONFIRMACION_REPRODUCCION.md`.

## MOTOR-SMOKE4R-HF3 — observabilidad de descarga de Voz IA avanzada

Se mejora el diagnóstico de preparación/descarga de Voz IA avanzada. Cuando la operación falla o queda incompleta, el diálogo ya no termina solo con un mensaje genérico: combina el resultado de descarga con los requisitos locales pendientes y muestra una ruta de reporte técnico (`download-diagnostics.txt`) para soporte. El reporte registra fuente configurada, URL técnica por recurso, HTTP status, tamaño, recursos faltantes e inspección final. Documento técnico: `docs/productizacion/MOTOR_SMOKE4R_HF3_OBSERVABILIDAD_DESCARGA.md`.

## FFMPEG-PREP1 — Video local descargable y verificable

Se convierte **Preparar video local** en una acción real: descarga el paquete configurado por `download.ffmpeg.runtimeZipUrl`, extrae el ZIP en `tools/ffmpeg/downloads`, localiza `ffmpeg.exe` y `ffprobe.exe`, copia ambos a `tools/ffmpeg/bin` y ejecuta `FfmpegRuntimeProbeUseCase.readyForFinalVideo()` para confirmar que sirve para exportación final con fallback CPU `libx264`. **Importar carpeta** queda como alternativa de soporte/manual. Documento técnico: `docs/productizacion/FFMPEG_PREP1_VIDEO_LOCAL_DESCARGA_GUIADA.md`.


## VIDEO-EXPORT1 — Exportación MP4 final

- El comando visible pasa a **Exportar video**.
- La UI solicita calidad 4K / 2K / 1080p / 720p y luego archivo `.mp4`.
- Se agrega `ExportFinalVideoUseCase`, `FinalVideoExportRequest` y `FinalVideoExportResult`.
- La revisión post-descarga de Voz IA avanzada queda pospuesta hasta que el usuario entregue diagnóstico/reporte.

## MOTOR-SMOKE4R-HF4 — descarga completa y runtime pendiente

Se corrige la lectura del caso real donde `download-diagnostics.txt` confirma descarga exitosa del modelo de Voz IA avanzada, pero la configuración inicial todavía queda pendiente por entorno local de ejecución o prueba. La UI ahora separa “modelo descargado correctamente” de “falta preparar entorno local”, muestra rutas de reporte y usa un área copiable en el diálogo de progreso para enviar el detalle completo. Documento técnico: `docs/productizacion/MOTOR_SMOKE4R_HF4_DESCARGA_COMPLETA_RUNTIME_PENDIENTE.md`.

## AUDIO-COMPRESS1 — Exportación final MP3/AAC

- Se agrega exportación final de audio en WAV, MP3 y AAC.
- Los chunks y jobs internos siguen siendo WAV.
- MP3/AAC se comprimen con el componente local de video/audio y generan reporte final.
- La revisión de Voz IA avanzada post-descarga queda pospuesta hasta que el usuario la solicite.

## MOTOR-SMOKE4R-HF5 — voz local simple no contamina Voz IA avanzada

Corrige la inspección de Voz IA avanzada cuando la configuración tiene seleccionada Voz local simple. El modelo descargado podía estar completo, pero la inspección buscaba erróneamente `voz-local-simple.wav` como speaker avanzado. Ahora la inspección y la plantilla de comando avanzada usan `voz-por-defecto.wav` cuando el motor activo no es avanzado o cuando el identificador pertenece a Voz local simple.
## MOTOR-TTS-ADV-HF1 — generación avanzada no queda muda en 0/1

Se corrige el lanzamiento real del proceso de Voz IA avanzada cuando el modelo ya está descargado. El wrapper Python ya no recibe `auto` como device opaco; el modo automático cae a CPU hasta tener prueba GPU real. El proceso se ejecuta con salida sin buffer, reporta fases (`cargando_modelo`, `sintetizando`, `wav_generado`) y el gateway Java destruye árbol de procesos en cancelación/timeout para evitar jobs fantasma al cerrar. Documento técnico: `docs/productizacion/MOTOR_TTS_ADVANCED_JOB_HF1_PROCESO_REAL.md`.
## PLAYBACK-SELECT1 / MODEL-PORT1 — selección como primer chunk y models portable

Estado: implementada sobre la base verde reciente. Se corrige que `Reproducir desde selección` genere desde el segmento seleccionado cuando todavía no existe WAV, evitando preparar desde `SEG-001`. También se permite reutilizar una carpeta `models/` trasplantada entre tandas si `models/tts/xtts` contiene el contrato completo del modelo, aunque la configuración persistente apunte a otra ruta. El módulo Índice ahora usa icono tipo árbol. Documento técnico: `docs/productizacion/PLAYBACK_SELECT1_MODELS_PORTABLES.md`.


## VIDEO-EXPORT1-HF — progreso/cancelación del MP4 final

La exportación visible de video final ahora se ejecuta en segundo plano desde `DocuPodcastShellView`, muestra `VideoRenderProgressView` y transporta progreso/cancelación hasta `ExportFinalVideoUseCase`. El use case reporta preparación, render por frame, salida útil de FFmpeg, unión final y verificación del MP4. Al cancelar, destruye `ffmpeg.exe` y procesos hijos para evitar renders colgados. Documento técnico: `docs/productizacion/VIDEO_EXPORT1_HF_PROGRESO_CANCELACION.md`.


## VOICE-REGISTRATION-WIZARD1 — Nueva voz con muestras de referencia

Se agrega subvista **Nueva voz** dentro de Vista Voces: nombre de voz, selector simple de emoción, frase guía, grabación Java, detener/asignar, reproducir/eliminar muestra y compuerta de Neutral obligatoria. Se corrigen source tests heredados de VOICE-UX-POLISH1A que esperaban microcopy anterior.


## THEATER-COLON-READ1

Documento puede omitir etiquetas breves antes de dos puntos para guiones teatrales, por ejemplo `Vaquero: texto` → `texto` en la generación de audio. El Word original no cambia y los chunks deben rehacerse.

## VOICE-HF3-CORRECCION + CSS-TOKENS-HF1

Se corrige el diagnóstico local posterior a VOICE-SAMPLES-RC1/WORKSPACE-HF3/TONE-UX-HF1: el ViewModel vuelve a quedar bajo el límite transitorio RF-TX2 y la selección de origen de voz queda delegada al coordinador de audio. Se agrega además hotfix visual CSS: `-dp-ink` queda definido y los botones del status bar usan hover morado pastel claro.

## ESTANDARES-PENDIENTES-RC1 — documentación exhaustiva de continuidad

Base reportada por el usuario: diagnóstico completo OK, Maven compile/tests OK, smoke automático cerebro OK, preflight de motores OK, Piper y FFmpeg locales OK, demo teatral estable. No se avanzó una tanda funcional nueva; se priorizó documentar dentro del repositorio los estándares pendientes de cierre y corregir el warning CSS observado al abrir ejemplos.

Cambios:

- Se agrega `DOCUMENTACION_ACTUAL/08_ESTANDARES_PENDIENTES_RC.md` con el mapa detallado de estándares pendientes: superficies operativas, decisiones defensivas visibles, excepciones tipadas, rutas runtime, procesos externos, contratos de motores, descargas, readiness, comandos, Settings, UI, persistencia y RC.
- Se agrega `DOCUMENTACION_ACTUAL/PLAN_IMPLEMENTACION_EN_PIEDRA/08_ESTANDARES_PENDIENTES_Y_TANDAS_RESTANTES.md` como mapa de continuidad si se pierde el contexto de conversación.
- Se agrega `docs/productizacion/ESTANDARES_PENDIENTES_RC1_MAPA_DE_CONTINUIDAD.md` como documento de productización.
- Se actualizan `DOCUMENTACION_ACTUAL/00_LEEME_PRIMERO.md`, `DOCUMENTACION_ACTUAL/03_ROADMAP_PENDIENTE_CIERRE.md`, `DOCUMENTACION_ACTUAL/PLAN_IMPLEMENTACION_EN_PIEDRA/00_INDICE.md`, `AI_HANDOFF.md` y `VALIDATION.md` para apuntar a la nueva autoridad documental.
- Se corrige el warning JavaFX `Could not resolve '-dp-text-secondary'` agregando el alias `-dp-text-secondary: -docu-text-muted;` en `tokens.css`.
- Se agrega `StandardsPendingRc1SourceTest` y se amplía `CssWarningsT120DHf1SourceTest` para proteger la continuidad documental y el token CSS.

Próxima tanda recomendada: `RUNTIME-PATHS-RF1`, seguida de `EXTERNAL-PROCESS-RUNNER-RF1`.

## RUNTIME-PATHS-RF1 + EXTERNAL-PROCESS-RUNNER-RF1 + EXTERNAL-PROCESS-EXCEPTIONS-HF1

Se agregan estándares transversales iniciales de runtime/procesos: `RuntimeArtifactPaths`, `ExternalProcessRequest`, `ExternalProcessResult`, `ExternalProcessRunner`, `DefaultExternalProcessRunner`, `ExternalProcessTimeoutException` y `ExternalProcessCancelledException`. El smoke CUDA de XTTS ya puede usar runner común por inyección y `ApplicationServicesFactory` lo cablea con `DefaultExternalProcessRunner`. Se agrega documentación en `docs/productizacion/RUNTIME_PATHS_EXTERNAL_PROCESS_RF1.md`. Próxima prioridad recomendada: `MODEL-ARTIFACT-CONTRACT-RF1` o `MANAGED-DOWNLOAD-RF1` si se desea seguir limpiando motores; `ENGINE-READINESS-UI-HF1` si se prioriza claridad visible.

## ENGINE-READINESS-UI-HF1 + COMMAND-AUDIT-RC1 + RIBBON-CATALOG-RF1

Se agregaron estados humanos de readiness de motores (`AudioEngineReadinessUiItem`, `InspectAudioEngineReadinessUiUseCase`), auditoría de comandos visibles (`CommandAuditInspector`) y catálogo separado del Ribbon (`RibbonDefinitionCatalog`). `RibbonView` queda como renderizador y el Shell audita que todo comando visible tenga handler real. Documento/Voces pueden exponer líneas de readiness sin agregar dashboard decorativo.


## SETTINGS-SPLIT-RF1 + GUI-COMPONENTS-RF1

Se corrigieron los source tests de Ribbon tras la extracción a `RibbonDefinitionCatalog` y se avanzó una división inicial de `SettingsDialog`: la barra inferior vive ahora en `SettingsActionBar`. También se agregó `OperationalStatusStrip` como componente GUI transversal para comunicar estados operativos sin crear dashboards decorativos.

## XTTS-LEGACY-COMMAND-REPAIR-HF1 + PIPER-GPU-CLARITY-HF1

Se corrigió un caso real donde settings antiguos/localizados seguían ejecutando `scripts/tts/Voz IA avanzada-file-to-wav.ps1` y enviaban `.../model.pth` como carpeta de modelo, provocando `model.pth/model.pth` en Coqui/XTTS. `XttsTtsCommandTemplate` detecta comandos legacy/localizados y reconstruye el comando administrado desde el `applicationRoot` actual; además se agregó un script puente compatible y se reforzó la normalización Java de `-ModelDir`. También se aclaró en el catálogo de motores que Voz local simple/Piper trabaja en CPU y que seleccionar GPU no lo acelera.

## RF14-02 — Configurar motor con prueba rápida clara

`Vista > Voces > Configurar motor` queda concentrada en selección de motor/dispositivo y prueba rápida del motor seleccionado. Se retira del render el bloque `Motor activo y alcance`, se reformulan estados de prueba (`Generando voz...`, `Voz de prueba lista`, `Reproduciendo voz de prueba`) y los botones secundarios blancos pasan a un gris suave desde el CSS transversal de acciones. Validación: `mvn -q test` verde.

## RF14-03 — Prueba de motor sin congelar JavaFX

La generación de voz de prueba ahora prepara un snapshot en el ViewModel y ejecuta la síntesis en un `Task` de fondo; el resultado vuelve al hilo de UI para actualizar estado y proyecto. El panel de prueba rápida vuelve a blanco; solo los botones secundarios conservan el gris suave transversal. Validación: `mvn -q test` verde.

## RF14-04 — Gestionar voces con resumen operativo

`Vista > Voces > Gestionar voces` queda dividido en dos niveles: resumen rápido y edición completa. La columna `Voz registrada` se conserva, el panel derecho muestra solo estado puntual, combo de emociones registradas cuando aplica, prueba de voz y acciones `Nueva voz` / `Gestionar voz seleccionada`. El editor de muestras, catálogo y tonos queda detrás del subflujo explícito. Validación: `mvn -q test` verde.

## RF14-05 — Gestionar voces como selección y prueba contextual

`Vista > Voces > Gestionar voces` deja de repetir el detalle de `Inicio` y deja el mantenimiento completo para el subflujo `Gestionar voz seleccionada`. La columna izquierda se nombra `Voces registradas`; el resumen muestra prueba contextual, selector de emoción solo para voces avanzadas con muestras registradas, acciones `Nueva voz` / `Gestionar voz seleccionada`, y sincroniza el motor al seleccionar voz simple o avanzada. Validación: `mvn -q test` verde.
## RF16-RF20 — Documento simple, Teatro/Guión y exportación de obra

Se separa la lectura limpia de la capa teatral. `Documento` queda sin sidebar derecho ni módulo `Imagen`; `Teatro > Guión` reutiliza el documento con un sidebar teatral para `Fragmentos visuales`, `Personajes`, `Mapa textual`, `Mapa espacial`, `Acciones` y `Objetos`. Se agrega `Exportar obra` con opciones iniciales de imagen, texto, tipografía, efecto y mapa. El bloque opcional `theatre` del proyecto persiste alias de fragmento `T1/T2/T3`, personajes, imágenes, escenas, posiciones, acciones y alias dramáticos vinculados a voces reales; por ejemplo, la voz real `Mario Alonzo` puede aparecer en la obra como `El villano` sin renombrar la voz. Validación: tests enfocados de RF16-RF20 y `mvn -q test` completos en verde.

## RF21 — Voces avanzadas prediseñadas oficiales y mapa espacial base

Se reemplaza la antigua referencia personal de Voz IA avanzada por 15 presets oficiales embebidos en `samples/voices/advanced-presets/`, se actualiza `models/tts/xtts/speakers/voz-por-defecto.wav` desde el preset oficial `hombre_adulto_personaje_narrativo`, y se incorpora `samples/theatre/maps/mapa-espacial.png` como base para Teatro. `VOC-OWN-PLACEHOLDER` deja de ser biblioteca por defecto y queda solo como compatibilidad legacy. También se documenta que la voz real y el alias teatral son capas separadas: una voz puede llamarse `Mario Alonzo` y usarse en una obra como `El villano`. Validación: `mvn -q test` completo en verde.

## RF21-01 — Cambio de voz invalida chunks y pulidos de lectura

Cambiar la voz desde la vista Documento ahora elimina los jobs/chunks persistidos del proyecto para impedir que se reproduzca audio generado con la voz anterior. `Generar`, `Rehacer chunks` y `Renderizar desde aquí` limpian audio persistido antes de crear una corrida nueva; `Seguir generando` conserva la recuperación de jobs interrumpidos. XTTS recibe texto sin puntos de cierre de oración y la cola de playback mantiene una pausa mínima entre cues. También se muestran imágenes fuente sin contenedor, se etiquetan voces oficiales no narrativas como `diálogo` y las emociones registradas se distribuyen en panel envolvente. Validación: tests enfocados y `mvn -q test` completos en verde.

## RF21-02 — Voz predeterminada del documento y voces específicas

La voz elegida desde `Gestionar voces` o desde el panel `Audio` puede aplicarse como voz predeterminada de todo el documento. Los fragmentos solo usan otra voz si tienen una asignación específica; si no, heredan la voz del documento. `VOC-NARRATOR` queda como marcador heredado y resuelve contra la voz predeterminada configurada. El panel `Audio` separa `Usar voz en todo el documento`, `Asignar voz al fragmento seleccionado` y `Eliminar voces específicas del documento`. Cambiar la voz global o limpiar voces específicas invalida chunks persistidos para evitar mezclar audio generado con voces anteriores. Validación: compile, tests enfocados y `mvn -q test` completos en verde.

## RF21-03 — Voz global sincronizada, regeneración automática y playbar clara

La voz predeterminada del documento queda sincronizada entre `Vista > Voces` y el panel `Audio`. Al cambiarla, DocuPodcast elimina chunks anteriores y lanza una nueva generación completa sin iniciar reproducción; las voces específicas por fragmento se conservan como overrides. `Reproducir desde selección` reprioriza la generación desde el fragmento elegido si ya hay un render activo. Sin selección, la acción principal vuelve a `Reproducir documento`, se retira el botón pequeño redundante de reproducir desde inicio y el panel `Audio` queda con una sola nota contextual más compacta. Validación pendiente en esta tanda.

## RF21-04 — Fragmento completo, voz general y guardado de fuente

`Reproducir fragmento (solo este)` ahora usa un manifest filtrado con todas las cues del segmento seleccionado, no solo la primera oración/unidad. En `Audio`, el retorno de un fragmento a la voz heredada se expresa como `Asignar voz general` con estilo naranja, manteniendo `Eliminar voces específicas del documento` como limpieza global. El click en blanco solo deselecciona si la reproducción está detenida. Al abrir una fuente documental sin proyecto guardado, DocuPodcast pide guardar el proyecto y propone la carpeta de la fuente como destino. Validación: compile, tests enfocados y `mvn -q test` completos en verde.
## PRESENTATION-ORCHESTRATION-RF21-06 — sidebar teatral expansible y playbar vertical

Estado: implementada como ajuste responsive del modo `Teatro > Guión`. El sidebar teatral derecho reduce sus mínimos internos para poder expandirse o plegarse con más libertad, y la playbar flotante cambia a modo vertical cuando el lector central queda estrecho. No se cambian módulos teatrales, comandos ni formato de proyecto. Validación enfocada: compile y tests de sidebars/playbar/teatro verdes. Queda pendiente el refactor mayor para que ambos sidebars sean hermanos directos en un contenedor raíz común.

## PRESENTATION-ORCHESTRATION-RF21-07 — playbar vertical fija en Guión y dock teatral amplio

`Teatro > Guión` fuerza la playbar vertical desde el primer render para no invadir el documento cuando el sidebar derecho está activo. El dock teatral inicia más ancho, tiene mínimos más realistas y sigue siendo expandible manualmente. El botón principal de la playbar usa texto compacto en vertical para evitar compresión visual.
