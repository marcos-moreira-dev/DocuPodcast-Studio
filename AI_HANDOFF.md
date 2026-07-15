
## XTTS Final ModelDir Guard HF3

Si aparece `FileNotFoundError ... model.pth/model.pth`, verificar que la carpeta ejecutada sea posterior a `XTTS_FINAL_MODEL_PTH_GUARD_HF3`. La version corregida repara settings en readiness/smoke/gateway/template y los scripts `xtts-file-to-wav.ps1`, `Voz IA avanzada-file-to-wav.ps1` y `synthesize_xtts.py` siempre usan la carpeta padre cuando el ModelDir termina en `model.pth`.


## RUNTIME-ARCH-RC1 + fix diagnostico 140623

Se corrige el diagnostico local `20260607-140623`: el string visible de Configuracion ya no menciona nombres tecnicos de motores en la UI normal, y `scripts/tts/setup-xtts-pytorch-cuda.ps1` queda ASCII-safe para Windows PowerShell 5.1. Se agrega documentacion `docs/productizacion/RUNTIME_ARCH_RC1_CONSOLIDACION.md` y guardarrail `RuntimeArchRc1SourceTest`. Recordatorio: Voz local simple trabaja principalmente en CPU; GPU real para Voz IA avanzada requiere Python local con aceleracion CUDA aprobada por smoke.


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

## XTTS-MODEL-PATH-HF1 — handoff

Si Voz IA avanzada falla con `FileNotFoundError ... model.pth/model.pth`, revisar `tools/xtts-wrapper/synthesize_xtts.py` y `scripts/tts/xtts-file-to-wav.ps1`: ambos normalizan `--model-dir` si termina en `model.pth`. La carpeta válida debe contener `model.pth`, `config.json` y `vocab.json`.

## FIRST-USE-ONBOARDING1 — handoff

La primera experiencia prioriza Voz local simple. `SettingsDialog.promptInitialSetup(...)` ya no dispara preparación pesada de Voz IA avanzada como primer paso; usa `DownloadPiperPortableRuntimeUseCase` y selecciona Voz local simple si queda lista. Voz IA avanzada y Video local se preparan después desde Configuración. Mantener esta regla para no bloquear la prueba inicial con descargas largas.

## DOCUMENT-SIDEBAR-VOICE-UX1 — Documento: Voz generada o Audio del computador

## VOICE-CHUNKS-HF2 — barra de estado, dependencias XTTS y continuidad de buffer

- Fija `transformers==4.44.2` para el runtime local de Voz IA avanzada y verifica `BeamSearchScorer` antes de generar WAV.
- Acorta la barra de estado: `Renderizar desde aquí`, `Rehacer chunks`, `Seguir generando` y `Detalles`.
- Corrige estilo presionado de botones para evitar texto oscuro sobre fondo oscuro.
- Evita saltar al primer cue disponible cuando el usuario pidió reproducir desde un título/fragmento exacto.
- Reanuda automáticamente si un nuevo manifest recupera el buffer después de quedarse sin chunks.


Implementada la limpieza del sidebar Audio de Documento. Mantener esta regla: Documento permite elegir Voz generada con una voz/tono registrado o Audio del computador. Los tonos visibles en ComboBox deben ser simples y filtrados por muestras reales de la voz seleccionada. No reintroducir `Tonos recomendados · ...`, `Catálogo teatral extendido · ...` ni códigos técnicos.

## Handoff reciente — VOICE-LIBRARY-SYNC1

Base: VOICE-GPU-DEVICE-HF2. Las muestras de voces de usuario pasan a biblioteca de app/runtime (`voice-library/samples`) en el wiring productivo. `ImportVoiceSampleUseCase` ya no añade esos WAV como assets del proyecto si el repositorio declara almacenamiento de aplicación. Documento ya escucha `activeVoiceLibraryProperty()` y filtra tonos por `sampleSet.registeredTones()`. Próxima tanda: DOCUMENT-SIDEBAR-VOICE-UX1.

## Handoff reciente — VOICE-GPU-DEVICE-HF2

Corrige `VOICE-CHUNKS-HF1`: no declarar Voz local simple como CPU-only. `PiperTtsCommandTemplate` y `scripts/tts/piper-file-to-wav.ps1` propagan política/dispositivo/GPU index al proceso de voz simple. La prueba CUDA sigue siendo específica de Voz IA avanzada.

VOICE-CHUNKS-HF1 corrigió mensajes CUDA/CPU, separó Rehacer chunks de Seguir generando y agregó render desde fragmento seleccionado en barra de estado.

# VOICE-UX-POLISH1A — handoff

Base verde anterior: `VOICE-PLAN-REFERENCIAS1` + `MOTOR-GPU-SMOKE1-HF2`. Se aplicó limpieza operativa de Vista Voces: fuera botón `Ver estado de voces`, fuera `Resumen de operación`, estado de motor visible, estado de prueba sin apariencia de botón, filas de voz con tipo humano y tags solo para tonos registrados. Se agregó `VoiceToneLabelPolicy` para que ComboBox de tonos muestre solo `Neutral`, `Feliz`, `Enojada`, etc. Próxima tanda funcional: `VOICE-REGISTRATION-WIZARD1`.

# MOTOR-GPU-SMOKE1-HF2 — handoff

Base verde anterior: `PLAYBACK-SPEED-HF9`. Hotfix por captura de Configuración: `ProcessXttsCudaRuntimeProbeGateway` dejaba que Windows/Python rompiera el bloque `python -c` y producía `SyntaxError: unterminated string literal`. La corrección escribe el probe CUDA en un archivo temporal `.py`, lo ejecuta con `tools/xtts-wrapper/.venv/Scripts/python.exe` y elimina el temporal. Próxima tanda funcional sigue siendo `VOICE-UX-POLISH1`.

# PLAYBACK-SPEED-HF9 — transición inmediata a 1.5x/1.75x

Handoff vigente: se agregó `PlaybackTimingPolicy` y se conectó con `PlaybackSequentialQueueDriver`, `PlaybackCueDeadlineSequencer` y `PlaybackContinuationController`. `DocuPodcastShellViewModel.matchesCompletedAudioFile(...)` conserva match absoluto y añade fallback por nombre de WAV para no ignorar callbacks naturales equivalentes. Próxima tanda: `VOICE-UX-POLISH1`.

# DOC-INDEX-PLAYBACK-HF1 — índice respeta pivote de reproducción

Estado vigente: se corrige la continuidad posterior a MOTOR-GPU-SMOKE1. La app ya no expone nombres técnicos en strings de presentación para el smoke CUDA y `RunXttsCudaSmokeUseCase` respeta la frontera de capas. Además, al seleccionar un bloque desde el índice y pulsar `Escuchar`, `listenToDocument()` usa ese bloque como pivote y genera audio desde el segmento asociado antes de caer a generación global. Documento: `docs/productizacion/DOC_INDEX_PLAYBACK_HF1_GENERAR_DESDE_INDICE.md`.

## Handoff reciente — MOTOR-GPU-SMOKE1

Segunda tanda del plan en piedra implementada. Se agregaron `RunXttsCudaSmokeUseCase`, `InspectXttsCudaSmokeUseCase`, `XttsCudaSmokeReport` y el gateway de probe Python. `SettingsAwareAudioGenerationGateway` mantiene CPU para Voz IA avanzada salvo que `gpuUsableForXtts()` esté confirmado. Configuración muestra estado CUDA y permite ejecutar el smoke. Próxima tanda: `DOC-INDEX-PLAYBACK-HF1`.

## Handoff reciente — MOTOR-ADV-READY-GATE1

Primera tanda del plan en piedra implementada. Se agregó `InspectXttsDocumentGenerationReadinessUseCase` y `XttsDocumentGenerationReadinessReport`; `InspectAiEnginesPreflightUseCase` usa `NEEDS_VERIFICATION` cuando falta prueba WAV real; `SettingsAwareAudioGenerationGateway` bloquea Voz IA avanzada para documentos hasta que exista `generatedWavProof()`. Próxima tanda: `MOTOR-GPU-SMOKE1`.

# DOC-PERF/MEM1 — índices ligeros para documentos grandes

Estado: implementada sobre PLAYBACK-SELECT1/MODEL-PORT1. Corrige el diagnóstico `20260606-170202.zip` reduciendo `DocuPodcastShellViewModel` bajo el límite transitorio RF-TX1 y optimiza el lector para documentos grandes con `blockIndexById` y `sentenceSpanIndex`, evitando reescaneos completos durante navegación, selección y actualización visual. Documento: `docs/productizacion/DOC_PERF_MEM1_INDICES_LIGEROS.md`.

# DOC-INDEX-HF13 — índice navegable del documento

Estado: implementada sobre EXPORT-CLEAN1 verde. Se agrega `DocumentIndexPanel` como módulo lateral plegable `Índice` dentro del workspace Documento. Usa `TreeView<IndexEntry>` para títulos/secciones/subsecciones; cuando no hay encabezados, ofrece navegación plana por bloques narrables y visuales. El salto llama a `selectBlock(...)`, por lo que funciona también con la ventana virtualizada de documentos grandes. No se reintroduce el panel viejo `Estructura documental`. Documento: `docs/productizacion/DOC_INDEX_HF13_INDICE_NAVEGABLE.md`.

# EXPORT-CLEAN1 — exportaciones técnicas fuera del flujo común

Base actual: MOTOR-SMOKE4R-HF5 verde. Esta tanda concentra el flujo normal de exportación en salidas finales de producto: audio final y video MP4. `Exportar paquete de soporte`, `Exportar reporte de soporte` y `Validar integridad` quedan disponibles bajo `Ayuda > Soporte avanzado`, no en el menú Exportar, toolbar ni Ribbon. Documento técnico: `docs/productizacion/EXPORT_CLEAN1_EXPORTACIONES_SOPORTE_AVANZADO.md`.

# FFMPEG-PREP1 — handoff

Tanda aplicada sobre MOTOR-SMOKE4R-HF3 verde. Cambio central: `DownloadFfmpegPortableRuntimeUseCase` permite que Configuración > Video local descargue el paquete configurado por `download.ffmpeg.runtimeZipUrl`, extraiga el ZIP en `tools/ffmpeg/downloads`, copie componentes a `tools/ffmpeg/bin` y verifique exportación final con `FfmpegRuntimeProbeUseCase.readyForFinalVideo()`. No toca Voz IA avanzada, Documento ni playback. Siguiente paso natural: `VIDEO-EXPORT1` o `MOTOR-PERF1/GPU1`.

# VOZ-UX4R-3D — catálogo teatral de tonos en Vista Voces

# MOTOR-SMOKE4R-HF2 — etiqueta visible de modelo sin nombres técnicos

- Hotfix sobre HF1 tras diagnóstico `20260606-121332`: Configuración ya no muestra `Página del modelo XTTS-v2` ni `/resolve/main` como ayuda visible. La UI dice `Página oficial del modelo de voz` y conserva la normalización técnica solo por dentro. Documento: `docs/productizacion/MOTOR_SMOKE4R_HF2_PRESENTACION_URL_MODELO_VOZ.md`.


Base verde anterior: VOZ-TTS5B. Esta tanda habilita el catálogo teatral extendido en el ComboBox de tonos de Vista Voces, mantiene filas sobrias sin dashboard y agrega el guardarraíl `VoiceUx4R3DTheatricalToneCatalogSourceTest`. No toca motores, playback ni Documento.

# VOZ-TTS5B — handoff

Tanda aplicada sobre VOZ-TTS5A verde. Cambio central: la generación de audio ya no se queda solo en `{voice}`. `AudioGenerationRequest` puede llevar `VoiceLibrary`, `referenceSamplePathFor(AudioGenerationUnit)` interpreta `performanceStyleId` como `VoiceReferenceTone.fromLayerTargetId(...)`, resuelve muestra exacta o Neutral, y `LocalTtsProcessAudioGenerationGateway` pasa esa ruta a `LocalTtsProcessConfiguration.commandFor(..., referenceSample)`. No se tocaron UI, playback ni descargas de Voz IA avanzada.

# VOZ-TTS5A — handoff

Tanda aplicada sobre VOZ-UX4R-3C-HF1 verde. No tocar playback ni motores. Cambio central: `DocumentAudioNarrationPanel` filtra Documento por verdad de biblioteca de voces. Para Voz IA avanzada, una voz solo aparece si `VoiceLibrary.referenceSampleSetByVoiceId(voice.id()).hasNeutral()` existe. Los tonos del ComboBox salen de `VoiceReferenceSampleSet.registeredTones()`, no de `VoiceReferenceTone.values()`. Siguiente lógica: `VOZ-TTS5B` para pasar voz + tono real al render TTS.

# VOZ-UX4R-3C-HF1 — hotfix compilación Vista Voces

Base actual: VOZ-UX4R-3C con hotfix de compilación. El diagnóstico local `20260606-095144.zip` falló por `cannot find symbol: variable summary` en `VoiceLibraryWorkspaceView.java`.

Corrección aplicada:

- En `homeModule(...)`, después de `voiceLibraryHero(report)` y `homeOperationalSummary(library, report)`, se agrega solo `list`.
- Se refuerza `VoiceUx4R3CAntiDashboardSourceTest` para impedir que vuelva `box.getChildren().addAll(summary, list);`.

No tocaron motores, playback, Documento ni descarga de Voz IA avanzada. El cuello de botella funcional sigue siendo Vista Voces y luego VOZ-TTS5A para filtrar voces/tonos reales en Documento.

# VOZ-UX4R-3C — Vista Voces sobria sin dashboard

La base actual elimina el mini-dashboard de Vista Voces: no debe volver `metricCard(...)`, `voice-dashboard-metrics` ni `voice-metric-card`. Inicio usa filas operativas, Configurar motor usa filas con `InfoBadge`, y el sidebar izquierdo de Voces es oscuro/full-height con estética formal tipo Teams. Mantener componentes transversales (`ActionBar`, `InfoBadge`) en nuevas superficies. Próximo paso lógico: VOZ-TTS5A para filtrar voces con Neutral y tonos realmente registrados en Documento.

# VOZ-UX4R-DOC1 — contrato final modular de Vista Voces

Mantener como norte: Vista Voces debe ser una microaplicación administrativa sobria con tres módulos —**Inicio**, **Configurar motor**, **Gestionar voces**— y filas limpias, sin tarjetas futuristas ni métricas decorativas. Una voz es una entidad única (`Pepito`) con muchas emociones/muestras asociadas; no crear voces separadas como `Pepito feliz`. Neutral es obligatoria para que Documento vea la voz. Gestionar voces debe permitir reemplazar una emoción grabando o importando de nuevo. Eliminar voz debe mostrar message box avisando que se eliminarán muestras/archivos relacionados y que pueden limpiarse asignaciones. Configurar motor debe tener selector de motor y selector CPU/GPU real para todos los motores, usando la misma configuración interna que Configuración. Documento: `docs/productizacion/VOZ_UX4R_CONTRATO_FINAL_MODULOS.md`.


## PLAYBACK-SPEED1 — controles de velocidad en playbar

Se agregan botones `1x`, `1.5x` y `1.75x` en la playbar del Documento. La velocidad afecta playback de WAV ya generados, no generación TTS. `JavaSoundSegmentAudioPlayer` aplica velocidad simple por frecuencia efectiva de salida, `PlaybackCueClock` sincroniza cursor/visual al ritmo acelerado y el buffer existente sigue esperando si la lectura rápida alcanza un chunk pendiente. Documento: `docs/productizacion/PLAYBACK_SPEED1_CONTROLES_VELOCIDAD.md`.

# DOC-UX-HF10H — progreso legible, tamaño generado y rail Visual honesto

Base vigente tras prueba real de HF10G. Mantener: rail derecho virtualizado, overlay no bloqueante, progreso legible, tamaño real generado en disco, ETA con horas y copia honesta de Video local. La descarga automática completa de FFmpeg queda para FFMPEG-PREP1; MOTOR-PERF1 debe validar GPU real para motores que la soporten.

# DOC-UX-HF10G — memoria, rail virtual, overlay operable y tablas sin truncado

Se recupera la tanda perdida sobre HF10F: el rail derecho Visual ahora usa `ListView<DocumentFragmentRailPresentation>` virtualizado, se agrega `AudioStatusUiThrottle` para no saturar JavaFX con miles de actualizaciones de chunks, el manifest de playback deja de reconstruirse por generación pura sin playback activo, las tablas fuente envuelven texto y crecen hacia abajo, y el launcher local usa `DOCUPODCAST_APP_HEAP=-Xmx2048m` por defecto. Documento: `docs/productizacion/DOC_UX_HF10G_MEMORIA_RAIL_OVERLAY_TABLAS.md`.

# PLAYBACK-HF8R — cola por duración real del WAV

Estado: implementada sobre PLAYBACK-HF7.

Se corrige la fuente real del problema de chunks que se pisan: el audio generado ya no guarda duración estimada por texto. `LocalTtsProcessAudioGenerationGateway` mide la duración real del WAV generado con `WavAudioDurationProbe`; `AudioJobFileRepository` recalcula duración al leer jobs persistidos; y `DocuPodcastShellViewModel` usa `PlaybackCueClock` para no saltar al siguiente cue antes de que se cumpla la duración del WAV activo.

La descarga de Voz IA avanzada queda como smoke manual pendiente: la tanda no cambia ese flujo, pero deja claro que debe probarse después desde Configuración.

Documento: `docs/productizacion/PLAYBACK_HF8R_COLA_DURACION_REAL.md`.

# PLAYBACK-HF7 — reproducción secuencial por fragmentos

Estado: implementada sobre UX-HF7/PLAYBACK-HF6.

Se corrige el reloj del reproductor interno para que mida el audio realmente reproducido por el dispositivo y no los bytes escritos al buffer. Además cada reproducción tiene una generación interna para que hilos viejos no interfieran con el fragmento actual.

Documento: `docs/productizacion/PLAYBACK_HF7_REPRODUCCION_SECUENCIAL.md`.

# UX-HF7 + PLAYBACK-HF6 — Diagnóstico verde y reuso de audio generado

Estado: implementada sobre UX-HF6/DOWNLOAD-HF5 tras diagnóstico `20260604-153404.zip`.

Se corrigen fallos Maven de source tests y del manifest de playback. El documento vuelve a reconstruir el manifest desde el job persistido antes de decir que un fragmento no tiene audio, y `BuildPlaybackManifestUseCase` preserva el contrato de audio externo por segmento sin romper reproducción por unidades TTS.

Documento: `docs/productizacion/PLAYBACK_HF6_REUSO_WAVS_Y_MANIFEST_ACTIVO.md`.

# UX-HF6 + DOWNLOAD-HF5 — Build verde y descarga visible de Voz IA

Estado: implementada sobre UX-HF5/PLAYBACK-HF5 tras diagnóstico `20260604-151214.zip`.

Corrige el build roto por lambdas en `DocuPodcastShellViewModel` y por mezcla de tipos en `SettingsDialog`. Además, la preparación/descarga de Voz IA avanzada conserva el reporte de descarga y muestra el recurso pendiente en lenguaje humano, en vez de perderlo como “1 componente pendiente”.

Documento: `docs/productizacion/UX_HF6_DOWNLOAD_HF5_BUILD_VERDE_DESCARGA_VISIBLE.md`.

# UX-HF5 + PLAYBACK-HF5 — integración de playback, video local y pulido visual

Estado: implementada sobre UX-HF4/PLAYBACK-HF4.

Se corrige la integración entre audio generado/exportable y reproducción interna: el manifest de playback ahora usa unidades de render TTS, no solo ids de segmento, y el shell reconstruye el manifest cuando el job gana fragmentos. También se pulen controles de la playbar con iconos PNG, se mejora la legibilidad de diálogos, se agrega logo transparente de fondo en Inicio y Video local queda con preparación/importación más concreta.

Documento: `docs/productizacion/UX-HF5_PLAYBACK_VIDEO_UI_INTEGRACION.md`.

# UX-HF4 + PLAYBACK-HF4 — diálogos legibles y navegación por fragmentos

Estado: implementada sobre UX-HF3. Corrige confirmaciones cortadas con puntos suspensivos, encadena la preparación de Voz local simple después de Voz IA avanzada cuando el usuario acepta, y agrega botones de fragmento anterior/siguiente en la barra flotante de lectura. También cambia el reproductor interno WAV a streaming con SourceDataLine para reducir fallos silenciosos al reproducir segmentos.

Documento: `docs/productizacion/UX_HF4_DIALOGOS_SETUP_Y_PLAYBACK_FRAGMENTOS.md`.

# UX-HF3 — Build verde y reproductor interno WAV

Estado: implementada sobre UX-HF2 tras fallo `SettingsDialog` con string sin cerrar.

Corrige el build, mantiene el mensaje de configuración inicial con saltos de línea válidos, oculta nombres técnicos en UI normal y refuerza `JavaSoundSegmentAudioPlayer` para abrir WAV PCM o reportar causa humana cuando el audio generado no puede sonar desde el reproductor interno.

Documento: `docs/productizacion/UX_HF3_BUILD_Y_REPRODUCTOR_INTERNO.md`.

# UX-HF2 — Descarga visible, Voz local normalizada y reproducción por fragmentos

Estado: implementada sobre UX-RIBBON3/DOC-RAIL6 tras pruebas reales de motores.

Se corrige la descarga de Voz IA avanzada para distinguir recursos obligatorios/opcionales y mostrar el detalle humano del fallo; se normaliza texto solo para Voz local simple para evitar problemas con tildes y ñ; y se ajusta el inicio de reproducción por fragmentos para que no oculte errores del reproductor interno ni espere más de lo necesario cuando ya existen WAVs por chunk.

Documento: `docs/productizacion/UX-HF2_AUDIO_STREAMING_Y_DESCARGA_VOZ.md`.

# UX-RIBBON3 + DOC-RAIL6 — Ribbon y panel visual limpios

Estado: implementada sobre UX-SETUP3, corrigiendo además los source tests desalineados reportados en `20260604-110143.zip`.

Se elimina el botón global de rail derecho del Ribbon, se deja el control del panel visual como acción propia del Documento, se quita la sección redundante `Imágenes` del rail derecho y se mejora el botón de colapsar/expandir para que no muestre símbolos duplicados.

Documento: `docs/productizacion/UX_RIBBON3_DOC_RAIL6_RIBBON_Y_PANEL_VISUAL_LIMPIOS.md`.

# UX-SETUP3 — Motores vivos y Voz local automática

Estado: implementada sobre UX-HF1/UX-SETUP2 tras logs `20260604-102314.zip`.

Se corrige el refresco de Configuración después de preparar/descargar/importar motores, se agrega preparación automática de Voz local simple y se evita que la generación de audio use una configuración vieja creada al arrancar la app. Los gateways de audio y prueba de voz ahora leen la configuración persistida al iniciar cada trabajo real.

Documento: `docs/productizacion/UX_SETUP3_MOTORES_VIVOS_Y_PIPER_AUTOMATICO.md`.

# UX-HF1 + UX-SETUP2 — Inicio limpio y configuración inicial

Estado: implementada sobre PF5C/PF6B verde.

Regla vigente: no mostrar andamios técnicos al usuario final. Inicio usa **Configuración inicial** y **Guía rápida** real. Configuración normal se concentra en operar lectura, voz, audio y video; auditorías/smoke/reportes quedan como soporte técnico fuera de la experiencia normal.

Documento: `docs/productizacion/UX_HF1_SETUP2_INICIO_LIMPIO_CONFIGURACION_INICIAL.md`.

# PF5C + PF6B — Motores guiados completos y smoke GUI asistido

Estado: implementada sobre PF5B/PF6A verde.

PF5C completa la preparación guiada desde Configuración: ahora se puede importar Voz local simple (`.onnx` + `.onnx.json`) y FFmpeg/FFprobe desde la app, además de las acciones previas de Voz IA avanzada y auditoría de artefactos. PF6B agrega el primer smoke real desde GUI como checklist asistido generado por la app, sin simular éxito ni usar placeholder.

Documentos: `docs/productizacion/PF5C_PREPARACION_GUIADA_COMPLETA_MOTORES.md` y `docs/productizacion/PF6B_SMOKE_GUI_ASISTIDO.md`.

# PF5B + PF6A — Icono de producto y branding portable

Estado: implementada sobre PF4B/PF5A verde.

Se integra la imagen generada por IA como icono real del producto: `DocuPodcastStudioApp` carga el icono PNG, `jpackage` usa `packaging\windows\docupodcast-icon.ico` para app-image/MSI, y la carpeta portable copia el branding para mantener coherencia visual. El smoke post-app-image ahora verifica también `branding\docupodcast-icon.ico` y `.png`.

Documento: `docs/productizacion/PF5B_PF6A_ICONO_PRODUCTO_Y_BRANDING_PORTABLE.md`.

# PF4B + PF5A — Smoke portable y auditoría de motores

Estado: implementada sobre PF2F/PF4A tras diagnóstico `20260604-082244.zip`.

Se corrige el fallo de compilación por escapes ilegales en `SettingsDialog` y se avanza el cierre portable: `scripts\34-smoke-app-portable-runtime.bat` valida app-image, carpeta portable, launcher y `DOCUPODCAST_APP_ROOT`. Además inicia PF5 con auditoría real de artefactos: `AuditEngineArtifactsUseCase` calcula presencia y SHA-256 de archivos locales, Configuración agrega `Inventario local de motores` y `scripts\35-auditar-artefactos-motores.bat` genera `target\legal\ENGINE_ARTIFACTS_AUDIT.md`.

Documentos: `docs/productizacion/PF4B_SMOKE_APP_PORTABLE_RUNTIME.md` y `docs/productizacion/PF5A_AUDITORIA_ARTEFACTOS_MOTORES.md`.

# PF2E + PF3B — Progreso real en Configuración y cierre de guardarraíles de Voces

Estado: implementada sobre PF2D/PF3A tras diagnóstico `20260604-024335.zip`.

Se corrigen los 5 fallos Maven reportados: etiqueta de Modo de prueba, acción `Usar Voz IA avanzada`, tarjeta informativa de motores, límite de tamaño de `DocuPodcastShellViewModel` y deuda de coordinadores. Además, las operaciones largas de Configuración ya no quedan con texto fijo: preparación, descarga e importación de Voz IA avanzada reciben progreso vivo desde los casos de uso. El diagnóstico TTS vuelve a señalar Configuración como ruta normal, dejando los scripts como rescate técnico.

Documento: `docs/productizacion/PF2E_PROGRESS_CONFIGURACION_Y_PF3B_VOCES.md`.

# PF1B + PF2A — Hotfix compile y Voz IA avanzada portable

Estado: implementada sobre PF1 tras diagnóstico `20260604-013606.zip`.

Se corrige el fallo Maven de `ScriptWorkspaceView`: ya no usa `VisualesDocument/currentVisualesProperty`, sino `StoryboardDocument/currentStoryboardProperty`. Además se inicia PF2A: el runtime de Voz IA avanzada tiene un asistente `scripts\30-preparar-voz-ia-avanzada-local.bat`, el setup acepta `-LocalTtsRepo`, el checker exige `config.json`, `model.pth` y `vocab.json`, y el diagnóstico TTS deja de decir que usará mock solo por no tener `DOCUPODCAST_TTS_COMMAND`.

Documento: `docs/productizacion/PF1B_PF2A_HOTFIX_VOZ_IA_PORTABLE.md`.

# PF1 — Voz generada real en Vista Voces

Estado: implementada sobre TV1 verde.

Se elimina el placeholder funcional de la acción **Generar prueba con esta voz**: `GenerateVoiceTestUseCase` ya no escribe un WAV artificial, sino que usa `VoiceTestSynthesisGateway`. La infraestructura agrega `LocalProcessVoiceTestSynthesisGateway`, conectado al mismo comando TTS local configurado para audio del documento. La prueba de Voz IA avanzada resuelve la muestra real por tono, puede reemplazar `-SpeakerWav`/`--speaker-wav` en el comando y guarda TXT/WAV/manifiesto auditable con `realSynthesis=true`. El Modo de prueba queda como diagnóstico y ya no simula una voz generada. También se corrige el wrapper local para exigir `model.pth`, `config.json` y `vocab.json`, sin descarga automática de modelos.

Documento: `docs/productizacion/PF1_VOZ_GENERADA_REAL_VOCES.md`.

# TV1 — Limpieza de visuales/video honesto

Base vigente: TV1.

La UX normal debe hablar de **Visuales / Secuencia visual**. `storyboard` queda permitido como nombre interno/legacy de paquetes, clases y persistencia, no como copy de usuario. La exportación de video simple es honesta: prepara un paquete renderizable/auditable con plan, CSV, manifiesto y comandos; no crea el MP4 final por sí sola. Usar `SimpleVideoPackageExportResult.honestStatusLabel()` para mensajes visibles.

Documento: `docs/productizacion/TV1_LIMPIEZA_VISUALES_VIDEO_HONESTO.md`.

# TE1 — Exportaciones alineadas a voces por tono

Base vigente: TE1.

La exportación de paquete quedó alineada con Voces final: `FileSystemProjectBundleExporter` genera `VOICE_REFERENCE_SAMPLES.md` y `VOICE_REFERENCE_SAMPLES.tsv` dentro de `reports/`, usando `BuildVoiceReferenceSamplesExportReportUseCase`. El manifiesto ahora audita muestras por tono, assets registrados, archivos faltantes y pruebas generadas. Mantener lenguaje visible amigable en exportación: muestras de voz, voz IA avanzada, voz local simple; no reintroducir nombres técnicos de motores en la GUI normal.

Documento: `docs/productizacion/TE1_EXPORTACIONES_ALINEADAS_VOCES_TONO.md`.

# TC1 — CommandAvailabilityPolicy

Estado: implementada sobre RF5 verde.

Se agrega `CommandAvailabilityPolicy` como fuente única de disponibilidad para comandos visibles. Menú, Ribbon y `WorkspaceCapabilityPolicy` consumen la misma política, evitando reglas duplicadas entre superficies. La tanda no cambia UX, CSS ni componentes visuales; conserva nombres amigables de motores y agrega razones humanas para futuras tooltips/status.

Documento: `docs/productizacion/TC1_COMMAND_AVAILABILITY_POLICY.md`.

# RF5 — Limpieza residual STT/Whisper

Base vigente: RF5.

Se elimina el residuo `HUMAN_RECORDING_FOR_TRANSCRIPTION` del dominio de voces y `PerformanceSpan` deja de reconocer fuentes de transcripción. La única grabación/audio humano vigente es audio del computador asociado a un texto o una muestra de voz, nunca audio-a-texto. Los scripts de smoke real dejan de anunciar audio-a-texto/STT. Mantener fuera del build principal y de scripts operativos cualquier retorno de Whisper, SpeechToText, `SPEECH_TO_TEXT`, `allowGpuForStt`, `scripts/stt`, `application/stt` o `infrastructure/stt`.

Documento: `docs/productizacion/RF5_LIMPIEZA_RESIDUAL_STT_WHISPER.md`.

# RF4 — Limpieza de workspaces heredados

Estado: implementada sobre RF3 verde.

Se cierra la cuarentena real de workspaces heredados: `WorkspaceViewRegistry` rechaza factories para superficies internas, `viewFor(...)` normaliza solicitudes heredadas hacia superficies de producto y `DocuPodcastShellViewModel` deja de exponer métodos públicos de apertura de Guion interno, Procesos de audio o Secuencia visual como workspaces. El Shell sigue sin registrar handlers para comandos legacy ocultos. No se borran paquetes internos para preservar compatibilidad. La base RF3 mantiene `PrepareListeningSessionUseCase` como dueño de la decisión de escuchar documento en application.

Documento: `docs/productizacion/RF4_LIMPIEZA_WORKSPACES_HEREDADOS.md`.

# RF2 — Coordinadores de presentación / selección documental

Estado: implementada sobre RF1.

Se agrega `DocumentSelectionCoordinator` para mover fuera de `DocuPodcastShellViewModel` las reglas de presentación de selección documental: previews, etiquetas de oración/fragmento/capa, ubicación fuente y copy de la acción primaria del documento. Se corrige el source test de T102 para reconocer que la persistencia del zoom de lectura ya vive en `ReadingComfortCoordinator`. No cambia la UX ni CSS, y la GUI sigue sin mostrar nombres técnicos de motores.

Documento: `docs/productizacion/RF2_COORDINADORES_PRESENTACION_DOCUMENT_SELECTION.md`.

# T121-V10 — Handoff de Vista Voces final

Última tanda implementada: T121-V10. La Vista Voces queda documentada y protegida por tests fuente como biblioteca de voces, muestras por tono y pruebas generadas. El smoke visual final está en `docs/testeo/SMOKE_VISUAL_VOCES_FINAL.md`. No reintroducir nombres técnicos de motores en la interfaz gráfica normal.

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

Nota de compatibilidad documental: la frase histórica "Documento narrable como raíz V1" se conserva como alias de lectura preparada, pero el lenguaje vigente de producto es Documento y lectura preparada.

---

## Handoff vigente — T124

Base vigente: T124 — Cuarentena legacy workspaces/tests.

DocuPodcast Studio conserva algunas clases heredadas para compatibilidad interna, pero la navegación de producto queda limitada a Inicio, Documento y Voces. `SCRIPT_EDITOR`, `AUDIO_JOBS` y `STORYBOARD` se normalizan hacia Documento y no deben registrarse como workspaces principales. Storyboard se trata como secuencia visual/panel visual del Documento, no como módulo principal.

Documento: `docs/productizacion/T124_CUARENTENA_LEGACY_WORKSPACES_TESTS.md`.

---

## Handoff vigente — T123-CAT01

La base vigente es T123-CAT01 — Markdown como documento, no guion.

Reglas que futuros agentes no deben romper:

- Markdown se abre como documento fuente normal.
- No reintroducir importación/exportación Markdown de narración.
- No reintroducir `docupodcast-script-v1` en recursos oficiales de usuario.
- No reintroducir `IMPORT_SCRIPT_MARKDOWN`.
- Los recursos IA son guías, plantillas o ejemplos documentales, no contratos importables de guion.
- El paquete auditable puede incluir `lectura_preparada.md`, no `guion_narrable.md`.

Documento de la tanda: `docs/productizacion/T123_CAT01_MARKDOWN_COMO_DOCUMENTO_NO_GUION.md`.

---

## Handoff vigente — T114-HF2

La base vigente es T114-HF2 — Retiro total Whisper/STT.

Reglas que futuros agentes no deben romper:

- No reintroducir Whisper/STT.
- No reintroducir Audio a texto.
- No reintroducir `SpeechToTextApplicationServices`.
- No reintroducir `application/stt`, `infrastructure/stt` ni `scripts/stt`.
- No reintroducir `SttEngineSettings`, `stt.*` ni `allowGpuForStt`.
- `ModelFolderContract.recommended()` debe limitarse a Coqui/XTTS y Piper.
- DocuPodcast abre documentos y prepara lectura; no existe “subir guion” como producto.

Documento de la tanda: `docs/productizacion/T114_HF2_RETIRO_TOTAL_WHISPER_STT.md`.

---

## T114-HF1 — Build verde + no-guion visible mínimo

La base vigente incorpora una hotfix conservadora: los source tests de exportación se alinean con RF1 y validan `ExportWorkflowCoordinator`; el shell deja de activar `SCRIPT_EDITOR`/`STORYBOARD` en los flujos tocados; los mensajes visibles urgentes hablan de documento, lectura preparada y fragmentos.

Regla para siguientes tandas: no reintroducir Guion como workspace ni categoría del usuario. `NarrationScriptDocument` puede sobrevivir temporalmente como compatibilidad interna, pero la UX debe hablar de Documento y lectura preparada.

Documento: `docs/productizacion/T114_HF1_BUILD_VERDE_NO_GUION_VISIBLE.md`.

---

## TI2 — Audio jobs desde RenderPlan

Base vigente: TI2 — Audio jobs desde RenderPlan.

TI2 conecta el contrato `RenderUnitPlan` de TI1 con la generación de audio. `AudioGenerationRequest` puede recibir `RenderUnitPlan`; los gateways mock y TTS real consumen `request.generationUnits()` y ya no dependen exclusivamente de `request.script().segments()`.

Reglas nuevas:

- Las unidades `SPOKEN_ONLY` y `SPOKEN_WITH_VISUAL` sin `audioAssetId` entran al TTS.
- Las unidades con `audioAssetId` externo no se regeneran por TTS.
- `VISUAL_SILENT` y `OMITTED` no entran al job de audio.
- Se conserva fallback legacy por segmento para proyectos antiguos o planes incompatibles.
- La persistencia de jobs mantiene `AudioSegmentSnapshot`; el id puede ser `SEG-001-U001` hasta que una tanda futura decida si se crea `AudioUnitSnapshot`.

Archivos clave: `AudioGenerationUnit`, `AudioGenerationRequest`, `MockAudioGenerationGateway`, `LocalTtsProcessAudioGenerationGateway`, `AudioWorkflowCoordinator`, `DocuPodcastShellViewModel`.

Siguiente recomendada: TI3 — Storyboard/video desde RenderPlan.

---

## Estado tras TP2

Base vigente: TP3 — Packaging tools/models/scripts.

TP3 corrige el fallo documental de ReleaseCandidateDocumentationSourceTest y agrega un contrato verificable para empaquetar `tools/`, `models/`, `scripts/tts` y ejemplos sin depender del PATH global.

Cambios de TP3:

- Agrega `BuildRuntimeBundleManifestUseCase`, `RuntimeBundleManifest`, `RuntimeBundleItem` y `RuntimeBundleItemKind`.
- Agrega `scripts\29-verificar-runtime-layout.bat` para validar el layout portable.
- Formaliza `tools/ffmpeg/bin`, `tools/piper`, `tools/xtts-wrapper`, `models/tts`, `scripts/tts` y ejemplos internos.
- Mantiene TP2: Configuración muestra estado humano de motores.
- Mantiene la raíz runtime de TP1 como fuente para FFmpeg, Piper y Coqui/XTTS.

Reglas vigentes:

- Usar componentes GUI transversales, no controles hardcodeados.
- Documento es centro; Voces administra voces; Configuración prepara motores; Ejemplos crea proyectos demo.
- STT/Whisper no vuelve a superficies visibles.
- Documento como raíz V1: la lectura preparada usa una proyección interna temporal.

Siguiente recomendada: TP4 — Licencias y manifest de terceros.

## Guardarraíles históricos para futuros agentes

- Handoff T97: política de componentes GUI transversales antes de rediseñar.
- T88C y `T88C_LIMPIEZA_ALCANCE_MOTORES_GUI.md`: alcance visible de motores, sin Whisper/STT en producto core.
- T89: mensajes humanos para motores y fallos, dejando detalles técnicos en Configuración/Diagnóstico.
- T90_SMOKE_REAL_MOTORES.md: smoke opt-in de motores reales, no Release Candidate final.
- T85_PIPER_TTS_REAL_END_TO_END.md: Piper genera WAV como borrador real, Coqui/XTTS sigue como objetivo de calidad alta.
- Tanda 58C: protocolo histórico de smoke exploratorio mínimo.
- Tanda 81D y T81E: pulido frontend y camino restante a Release Candidate real.
- Base vigente: T112 fue el smoke visual UX / RC visual.
- Base vigente: T113 fue Menú Ejemplos + proyectos demo internos.
- Documento como raíz V1.
- Release Candidate real sigue pendiente de productización/packaging.

- Fuentes visibles cubiertas: Word/DOCX, PDF con texto nativo, Markdown y TXT.


## Guardarrail histórico TP2

Base vigente: TP2 — Preflight integrado a arranque/configuración se mantiene como hito protegido aunque la base actual avance a TP3/TP4/TP5/TP6.


## Productización TP4-TP6

Base vigente: TP4 — Licencias y manifest de terceros.
Base vigente: TP5 — Instalador/app portable real.
Base vigente: TP6 — RC instalable con smoke manual y automático.

- TP4 agrega inventario legal de FFmpeg, Piper, Coqui/XTTS, Python portable, JavaFX, dependencias Maven y ejemplos internos.
- TP5 prepara app-image/portable/MSI con runtime, tools, models, scripts y carpeta legal cuando existan.
- TP6 encadena diagnóstico, runtime layout, manifest de terceros, app-image, carpeta portable y smoke RC.

## TI1 — RenderUnit end-to-end

Base vigente: TI1 — RenderUnit end-to-end.

TI1 agrega `RenderUnitKind`, `RenderUnit`, `RenderUnitPlan` y `BuildRenderUnitPlanUseCase`. Esta tanda es el primer puente técnico de integración profunda después de productización TP1-TP6. No reemplaza todavía audio jobs, playback ni video legacy; deja el contrato estable para que TI2/TI3/TI4 lo consuman.

Regla central protegida:

- `SPOKEN_ONLY`: audio/playback, no frame de video.
- `SPOKEN_WITH_VISUAL`: audio + imagen.
- `VISUAL_SILENT`: imagen/tabla/fórmula u otro bloque visual con visual asignado, sin narración, duración por defecto 5 segundos o la configurada en `OperationalSettings.VideoRenderSettings.silentVisualBlockSeconds`.
- `OMITTED`: no entra a video.

La documentación de continuidad pesada está en `docs/productizacion/ROADMAP_RESTANTE_POST_TI1_DETALLADO.md`. Esa hoja reemplaza al chat como fuente de continuidad para las tandas pendientes: TI2, TI3, TI4, TI5, TI6, TI7, RF1, RF2, RF3, RF4 y RF5.

No hacer después de TI1:

- No volver a crear video desde todos los segmentos por defecto.
- No narrar imágenes/tablas/LaTeX como texto por defecto.
- No asumir que imagen embebida de Word equivale a storyboard.
- No refactorizar `DocuPodcastShellViewModel` antes de cerrar al menos TI2/TI3/TI4 si el objetivo es integración de media.

Siguiente recomendada: TI2 — Audio jobs desde RenderPlan.


## Base vigente: TI4

TI4 corrige la semántica documental: `IMAGE_NOTICE`, `TABLE_NOTICE` y `MATH_NOTICE` son bloques visuales fuente no narrables por defecto. El importador DOCX detecta matemática/OMML como `MATH_NOTICE` e informa `MATH_BLOCK_DETECTED`. No se debe intentar renderizar LaTeX/OMML real en esta etapa; basta identificar la categoría y conservar el bloque visual.


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

## Continuidad TI7 — Playback por oración/unidad

Base vigente: TI7. El playback ya tiene ruta desde `RenderUnitPlan` mediante `BuildPlaybackManifestUseCase.build(RenderUnitPlan, AudioJobSnapshot, DocuPodcastProject)`. La UI puede usar `SeekPlaybackUseCase.seekUnit(...)` para saltar a una oración/unidad concreta sin abandonar compatibilidad con `PlaybackCursor.segmentId`. `VISUAL_SILENT` no entra al playback como audio. Mantener la regla: imagen / tabla / fórmula fuente no se narra por defecto ni se lee como código.

## Siguiente recorrido después de TI7

La siguiente tanda recomendada es RF1 — Dividir `DocuPodcastShellViewModel`. La deuda de crecimiento se mantuvo bajo límite sin añadir nueva lógica visible al ViewModel durante TI7. No reabrir workspaces heredados ni STT/Whisper en superficies activas.

## RF1 — Dividir `DocuPodcastShellViewModel`

Base vigente: RF1. Se extrae `ExportWorkflowCoordinator` desde `DocuPodcastShellViewModel` como primera tanda de refactor de presentación. Exportación de Markdown, WAV, diagnóstico, bundle y storyboard/video queda orquestada por el coordinador. No cambia comportamiento visible, no cambia formato JSON y no reintroduce STT/Whisper en superficies activas. Siguiente tanda: RF2 — Coordinadores de presentación por flujo.


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

## T119B — UX por capacidades del motor de voz
La UI debe ser capability-driven. `VoiceEngineCapabilityProfile` es la frontera central para que Documento/Voces/Configuración no muestren capacidades falsas. Piper no expone emociones, clonación ni voces por muestra humana; Coqui/XTTS sí puede exponer voz importada/referencia/estilos cuando el motor lo soporte; Mock no genera voz real.

## Estado T119C/T120

T119C agregó contratos de artefactos de motor (`EngineArtifactManifest`, `BuildEngineArtifactManifestUseCase`, `ValidateEngineArtifactManifestUseCase`) y endureció el script de manifiestos para generar `ENGINE_ARTIFACTS_MANIFEST.md`. T120 limpió el onboarding: Inicio muestra Preparar voz como acción real hacia Configuración, mantiene Abrir documento como flujo principal y evita conceptos legacy.

## Estado T120B

Se aplicó el contrato final del Ribbon: Inicio = arranque rápido, Lectura = preparar/generar/cancelar audio, Vista = Documento/Voces/panel visual/soporte, Exportar = salidas. Storyboard ya no es pestaña principal. Se agregó `OPEN_DOCUMENT_READER` como comando de navegación al lector.

## PF2D/PF3A — Handoff operativo

La base vigente incorpora progreso in-app para preparación/descarga/importación de Voz IA avanzada y acciones reales de muestras en Biblioteca de voces. El usuario no debe ejecutar scripts para el flujo normal; Configuración coordina la preparación. El script de preparación queda como adaptador técnico.

Anclas vigentes para próximos agentes:
- T128-C01 — Documentación de limpieza de producto.
- Markdown se abre como documento fuente.
- Whisper/STT/audio a texto fue retirado del build principal.
- PreparedReadingProjection sigue siendo el contrato de lectura preparada.
- No existe subir guion como raíz del producto.
- Tanda 58C.
- Tanda 81D.
- T81E.
- Documento narrable.
- Documento narrable como raíz V1.
- Word/PDF/TXT/Markdown.
- T88C.
- T88C_LIMPIEZA_ALCANCE_MOTORES_GUI.md.
- T89.
- Release Candidate real.
- T85_PIPER_TTS_REAL_END_TO_END.md.
- T90_SMOKE_REAL_MOTORES.md.
- Base vigente: T112.
- componentes GUI transversales.
- Base vigente: T113.
- Base vigente: TP2.
- Base vigente: TP6.
- T121-V10.
- PrepareListeningSessionUseCase.
- RF4 — Limpieza de workspaces heredados.
- Base vigente: RF5.
- Base vigente: TI1.
- Handoff T97.


## Estado PF2F/PF4A

- Base corregida desde diagnóstico `20260604-080938.zip`.
- Maven fallaba solo por 3 source tests de textos/guardarraíles de Configuración; se alinearon etiquetas y notas.
- Configuración muestra progreso con latido independiente y ruta de logs mientras prepara/descarga/importa motores.
- PF4A agrega launcher portable `run-docupodcast-studio.bat` con `DOCUPODCAST_APP_ROOT=%%~dp0`.
- Próxima tanda: PF4B — smoke post-app-image y verificación de runtime root desde launcher.


## Handoff DOC-UX-HF9A

Hotfix focal sobre HF9. El diagnóstico del usuario falló en compilación por `local variables referenced from a lambda expression must be final or effectively final` en `DocuPodcastShellViewModel`. Se creó `final StoryboardDocument storyboardForImageLayers` para la reconstrucción del rail visual desde capas. También se reforzó `SourceDocumentRefreshCoordinator` para detectar cambios de metadata en bloques visuales fuente y permitir que imágenes/tablas DOCX rehidratadas se guarden sin resetear audio si el texto no cambió.

## Handoff DOC-UX-HF9

Se corrigió el frente de Documento posterior a HF8R: selección de oración sin pintar todo el párrafo, sincronización de playback por cue unitario, rail Visual sin límite fijo y reconstruido desde capas de imagen, sidebar Audio capability-driven según motor activo y DOCX visual con imágenes/tablas/LaTeX detectado. Próximo agente debe probar localmente con `scripts\99-diagnostico-completo.bat`, reimportar `Instinto Creativo` si el proyecto demo cargado aún conserva un `document.json` antiguo sin base64, y validar descarga/generación real de Voz IA avanzada desde Configuración.

## Handoff DOC-UX-HF9C

Hotfix sobre DOC-UX-HF9B. El rail derecho ya mostraba correctamente miniaturas por frase, pero el panel izquierdo de Imagen podía seguir mostrando “Sin imagen asociada”. `selectedDocumentImageUri()` ahora consulta primero `documentFragmentRailPresentations()` y reutiliza el mismo fragmento seleccionado que pinta el rail. También se alinearon guardarraíles del diagnóstico completo: PreviewReadingProfile ya conserva imágenes como bloques visuales no narrables, los tests de rail esperan selección por `selectDocumentTextRange`, y el test anti-Whisper ignora binarios al leer el árbol como texto.

## Handoff DOC-UX-HF9D

Tanda corta sobre DOC-UX-HF9C. Se añadió menú contextual en `DocumentMediaRailView` para copiar la imagen de una tarjeta visual al fragmento anterior o posterior. Las opciones se deshabilitan cuando el fragmento no tiene imagen o no hay vecino. `DocuPodcastShellViewModel.copyFragmentImageToAdjacentFragment(...)` asigna la misma capa IMAGE al rango destino, refresca storyboard/rail, selecciona el destino con `selectDocumentTextRange(...)` y fuerza actualización del panel izquierdo de Imagen.

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


## Base reciente DOC-UX-HF10E / DOC-TABLE-HF11B

- Botón `Generar chunks de audio` en status bar prepara la proyección narrable si falta y dispara generación sin iniciar reproducción.
- El overlay de generación puede ocultarse y reabrirse desde status bar.
- `SourceTableGridView` queda como componente GUI transversal para tablas fuente; no recrear grillas dentro de workspaces.


## DOC-UX-HF10F

Tanda vigente posterior a DOC-UX-HF10E: limpia contenedores redundantes en tablas/imágenes fuente y mueve el cálculo de tamaño estimado de chunks fuera del hilo JavaFX para que el overlay de generación sea más ancho y no bloquee la interacción. Ver `DOCUMENTACION_ACTUAL/01_REGISTRO_TANDAS_RECIENTES.md`.


## PLAYBACK-SPEED2 — continuidad y tono natural

- Velocidad `1x`, `1.5x` y `1.75x` en playbar.
- `1.5x`/`1.75x` usan time-stretch PCM local para conservar mejor el tono, no sample-rate shift.
- Reproducir desde selección continúa con los siguientes chunks; `Reproducir fragmento (solo este)` queda como acción corta explícita.
- Si la lectura rápida alcanza un chunk pendiente, mantiene la espera de buffer del flujo existente.

## PLAYBACK-SPEED3 — continuidad al cambiar velocidad

Se corrigió la continuidad de playback al cambiar velocidad durante un chunk. `setPlaybackRate(...)` ya no solo actualiza la tasa del reloj: resuelve el cue activo, toma la posición local real del player, reinicia `PlaybackCueClock` desde esa posición y conserva `playingCueSegmentId`. `tickPlayback()` prioriza el unit id realmente iniciado y avanza si el reproductor terminó aunque el cursor calculado no haya llegado exactamente al borde. El autostart por buffer solo arranca desde cursor detenido; los gaps se resuelven mediante `waitingForBufferedSegmentAfter` para evitar repetir la selección.

## PLAYBACK-SPEED6 — controlador estructural de continuidad

Se extrajo la continuidad de playback a `PlaybackContinuationController` y se agregó callback real `SegmentAudioPlayer.setOnPlaybackFinished(Consumer<Path>)`. El avance entre chunks ya no depende solo del polling de `Timeline`: Java Sound notifica el final natural del WAV activo y el ViewModel transiciona al siguiente cue validando que el archivo terminado corresponde al cue vigente.

## PLAYBACK-SPEED7 — manifest runtime y cola reproducible

La continuidad de lectura dejó de depender de un manifest posiblemente obsoleto creado mientras solo había pocos chunks. `rebuildPlaybackManifestFromLatestJob()` usa `PlayableAudioJobSelector` para escoger el job persistido más compatible y las acciones de playback fuerzan manifest fresco al iniciar y al terminar cada cue. Si la reproducción vuelve a detenerse, revisar cuántos cues reporta `playbackManifestRuntimeLabel(...)` en el estado.

## PLAYBACK-SPEED8 — watchdog de continuidad

Se agregó un watchdog independiente del timer/callback del reproductor para evitar que la lectura se detenga tras uno o dos chunks aunque existan WAV siguientes. También se ajustó la continuación tras buffer para iniciar el `PlaybackCue` exacto.

## Handoff PLAYBACK-SPEED9

Si la reproducción se detiene después del primer chunk, revisar `PlaybackRuntimeQueue`, `startCueMonitoring(...)` y el texto de estado que reporta la cola. El avance continuo ya no debe depender de la selección activa del documento.

## Handoff reciente — PLAYBACK-CORE3

Se introdujo `PlaybackSequentialQueueDriver` para que la reproducción continua use una cola exacta de `PlaybackCue.unitId`. Esto evita que cues dentro del mismo `segmentId` se resuelvan de nuevo contra el cue activo anterior. La ruta crítica ahora es `startSequentialPlayback(...)` → `startCueFromSequentialQueue(...)` → `playExactCue(...)`.


## PLAYBACK-CORE4 — cola exacta guiada por final real del reproductor

- Reproduce por cola exacta de cues, pero prioriza el callback real del reproductor antes de avanzar.
- El watchdog de cola ya no corta audio si Java Sound todavía reporta reproducción activa.
- Corrige inconsistencias observadas al cambiar velocidad, pausar/reanudar y reproducir mientras se generan chunks.
- Documento técnico: `docs/productizacion/PLAYBACK_CORE4_PLAYER_CALLBACK_QUEUE.md`.

## VOZ-UX4R-1 — Continuidad para próximo chat

Base actual: AUDIO-RESUME-HF1 + corrección de guardarraíl playback + Vista Voces humanizada. No tocar la ruta de playback salvo regresión reportada; el usuario confirmó que ya funciona bien. Próximo foco recomendado: VOZ-UX4R-2, selector de motor activo dentro de Voces con persistencia real. Mantener nombres visibles: Voz IA avanzada, Voz local simple, Modo de prueba. No mostrar Coqui/XTTS/Piper en la UI normal. Neutral es necesaria; los demás son Tonos. Coqui/XTTS descarga/verificación va después como MOTOR-SMOKE4R.

## Handoff VOZ-UX4R-2A

La Vista Voces queda convertida en shell modular con tres módulos: Inicio, Configurar motor y Gestionar voces. Mantener el estilo sobrio administrativo: filas limpias, estados humanos y acciones justas, sin dashboard futurista. No volver a `SplitPane` para esta vista. Próxima tanda recomendada: VOZ-UX4R-2B para selector real de motor/dispositivo CPU/GPU dentro de Voces; después VOZ-UX4R-3 para crear/editar/eliminar voces y reemplazar muestras por emoción.

## VOZ-UX4R-2B — Configurar motor real dentro de Voces

Se completa el módulo Configurar motor de la microaplicación Voces con selector de motor activo, selector de dispositivo de renderizado CPU/GPU detectado, estado honesto y prueba por textbox. Las selecciones se persisten en `OperationalSettings`, la misma configuración interna usada por Configuración. No se resuelve todavía la descarga de Voz IA avanzada/Coqui; queda para `MOTOR-SMOKE4R / COQUI-DL1`.

### VOZ-UX4R-3B — navegación modular sobria de Voces

La Vista Voces mantiene tres módulos internos —Inicio, Configurar motor y Gestionar voces— con botones laterales de solo nombre. El microcopy vive en el workspace derecho. Se corrige el refresco de selección que podía devolver accidentalmente la vista a Gestionar voces al intentar abrir Inicio o Configurar motor. El sidebar queda sobrio, sin degradados, con criterio administrativo de escritorio.


## MOTOR-SMOKE4R / COQUI-DL1 — implementada

Base vigente: Configuración de Voz IA avanzada incluye botón `Probar` para generar un WAV real en `runtime/tts/xtts-smoke`. El estado de motor diferencia descargado, verificado, seleccionable, WAV generado y reproducción confirmada. La descarga grande de Voz IA avanzada puede reanudarse mediante HTTP `Range` cuando existe un `.download` parcial. Documento técnico: `docs/productizacion/MOTOR_SMOKE4R_COQUI_DL1_PRUEBA_WAV.md`.

## MOTOR-PLAYCONF1

Base posterior a MOTOR-SMOKE4R-HF2. Se agrega `ConfirmXttsSmokePlaybackUseCase`, acción visible **Reproducir prueba** en Configuración y manifest de smoke con `playbackConfirmed=true` solo al terminar el audio dentro del reproductor interno.

## MOTOR-SMOKE4R-HF3

La preparación de Voz IA avanzada ahora deja diagnóstico accionable de descarga en `models/tts/xtts/download-diagnostics.txt` y la UI muestra qué requisito quedó pendiente cuando la descarga no deja el motor seleccionable.

## MOTOR-SMOKE4R-HF4

El caso real de descarga de Voz IA avanzada confirmó que el modelo puede quedar completamente descargado (`download-diagnostics.txt` con `success=true`, 8 recursos descargados, sin fallidos) y aun así no seleccionarse por entorno local de ejecución pendiente. La UI ahora separa descarga correcta de preparación pendiente y muestra detalle copiable + reportes de soporte.


## AUDIO-COMPRESS1

Exportación final de audio ampliada a WAV, MP3 y AAC. Los segmentos internos siguen siendo WAV; MP3/AAC se generan como entrega final mediante el componente local de video/audio.


## MOTOR-PERF1 / MOTOR-GPU1

Base actual: `AssessComputeAccelerationUseCase` y `ComputeAccelerationAssessment` separan detección de GPU de confirmación real. La revisión post-descarga de Voz IA avanzada queda pendiente para el final.
## MOTOR-TTS-ADV-HF1 — generación avanzada no queda muda en 0/1

Se corrige el lanzamiento real del proceso de Voz IA avanzada cuando el modelo ya está descargado. El wrapper Python ya no recibe `auto` como device opaco; el modo automático cae a CPU hasta tener prueba GPU real. El proceso se ejecuta con salida sin buffer, reporta fases (`cargando_modelo`, `sintetizando`, `wav_generado`) y el gateway Java destruye árbol de procesos en cancelación/timeout para evitar jobs fantasma al cerrar. Documento técnico: `docs/productizacion/MOTOR_TTS_ADVANCED_JOB_HF1_PROCESO_REAL.md`.
## PLAYBACK-SELECT1 / MODEL-PORT1 — selección como primer chunk y models portable

Estado: implementada sobre la base verde reciente. Se corrige que `Reproducir desde selección` genere desde el segmento seleccionado cuando todavía no existe WAV, evitando preparar desde `SEG-001`. También se permite reutilizar una carpeta `models/` trasplantada entre tandas si `models/tts/xtts` contiene el contrato completo del modelo, aunque la configuración persistente apunte a otra ruta. El módulo Índice ahora usa icono tipo árbol. Documento técnico: `docs/productizacion/PLAYBACK_SELECT1_MODELS_PORTABLES.md`.



### Handoff — VOICE-REGISTRATION-WIZARD1

Se corrigieron source tests de VOICE-UX-POLISH1A y se agregó subvista Nueva voz. Siguiente: VOICE-LIBRARY-SYNC1.


## THEATER-COLON-READ1

Documento puede omitir etiquetas breves antes de dos puntos para guiones teatrales, por ejemplo `Vaquero: texto` → `texto` en la generación de audio. El Word original no cambia y los chunks deben rehacerse.

## USER-DECISIONS-HF1

Se agregó el contrato transversal para decisiones defensivas visibles (`UserVisibleDecision`, `OperationResult`, `DefensiveDecisionPolicy`) y excepciones tipadas (`ApplicationPreconditionException`, `InfrastructureOperationException`, `ExternalProcessFailedException`, `EngineUnavailableException`). `UserNotification`/`ExceptionAlertPresenter` ya pueden mostrar esas decisiones como message box. El bloqueo defensivo de Voz IA avanzada usa `EngineUnavailableException` en lugar de `IllegalStateException`.



## VOICE-SAMPLES-RC1 + VOICE-WORKSPACE-HF3 + VOICE-TONE-UX-HF1

Se cerró el flujo operativo de muestras de voz: `ImportVoiceSampleUseCase` y `VoiceSampleRepository` soportan `effectiveProjectFile` para biblioteca de app, `LocalVoiceSampleFileRepository` expone `voice-library/tmp-recordings`, `VoiceSampleWorkflowCoordinator` graba temporales fuera del proyecto, y `DocuPodcastShellViewModel` puede importar/grabar/reproducir/eliminar muestras sin sesión de proyecto cuando existe biblioteca de app. La UI de Voces cambia a `Crear voz`, `Grabar muestra`, `Detener y guardar`, `Cancelar grabación`, y agrupa acciones para evitar botoneras horizontales.

## VOICE-HF3-CORRECCION + CSS-TOKENS-HF1

El diagnóstico local `20260607-081859.zip` falló por 8 tests fuente con una misma raíz: `DocuPodcastShellViewModel` subió a 2745 líneas y superó el límite transitorio RF-TX2 de 2700. Se corrigió compactando/delegando lógica de selección de origen de audio en `AudioWorkflowCoordinator.selectDocumentAudioSource(...)`; el ViewModel queda en 2698 líneas. También se avanzó `CSS-TOKENS-HF1`: se define `-dp-ink` para eliminar warnings CSS en `.document-context-checkbox` y se agrega `-docu-status-hover-pastel` para hover claro en botones del status bar. Nuevo test: `CssTokensHf1SourceTest`.


## ESTANDARES-PENDIENTES-RC1 — continuidad documentada

Base validada por el usuario: diagnóstico completo OK, Maven compile/tests OK, smoke automático cerebro OK, Piper y FFmpeg locales OK, demo teatral estable. Se corrige warning CSS de ejemplos agregando alias `-dp-text-secondary: -docu-text-muted;` en `tokens.css`. Se documentan estándares pendientes con máximo detalle en `DOCUMENTACION_ACTUAL/08_ESTANDARES_PENDIENTES_RC.md`, `DOCUMENTACION_ACTUAL/PLAN_IMPLEMENTACION_EN_PIEDRA/08_ESTANDARES_PENDIENTES_Y_TANDAS_RESTANTES.md` y `docs/productizacion/ESTANDARES_PENDIENTES_RC1_MAPA_DE_CONTINUIDAD.md`. Próxima tanda recomendada: `RUNTIME-PATHS-RF1`, seguida por `EXTERNAL-PROCESS-RUNNER-RF1`. Reglas críticas: nada visible sin propósito operativo, ningún fallback defensivo silencioso, motores honestos, rutas centralizadas, procesos externos con runner común y excepciones tipadas.


## RUNTIME-PATHS-RF1 / EXTERNAL-PROCESS-RUNNER-RF1 / EXTERNAL-PROCESS-EXCEPTIONS-HF1

Base aplicada después de que el usuario reportó tests verdes y demo estable. Se agregó `RuntimeArtifactPaths` para centralizar rutas críticas de XTTS/Piper/FFmpeg/runtime/voice-library. Se agregaron contratos `ExternalProcessRequest`, `ExternalProcessResult`, `ExternalProcessRunner` y la implementación `infrastructure.process.DefaultExternalProcessRunner`. El smoke CUDA de XTTS acepta runner inyectado y `ApplicationServicesFactory` ahora lo cablea con `new ProcessXttsCudaRuntimeProbeGateway(new DefaultExternalProcessRunner())`. Se amplió `ExternalProcessFailedException` para permitir `ExternalProcessTimeoutException` y `ExternalProcessCancelledException`. Tests/guardarraíles nuevos: `RuntimePathsRf1SourceTest`, `RuntimeArtifactPathsTest`, `ExternalProcessRunnerRf1SourceTest`, `ExternalProcessRequestTest`, `ExternalProcessExceptionsHf1SourceTest`. Pendiente real: migrar gradualmente FFmpeg, Piper, XTTS generación real y descargas al runner/rutas comunes.

## ENGINE-READINESS-UI-HF1 + COMMAND-AUDIT-RC1 + RIBBON-CATALOG-RF1

Se agregaron estados humanos de readiness de motores (`AudioEngineReadinessUiItem`, `InspectAudioEngineReadinessUiUseCase`), auditoría de comandos visibles (`CommandAuditInspector`) y catálogo separado del Ribbon (`RibbonDefinitionCatalog`). `RibbonView` queda como renderizador y el Shell audita que todo comando visible tenga handler real. Documento/Voces pueden exponer líneas de readiness sin agregar dashboard decorativo.


### SETTINGS-SPLIT-RF1 + GUI-COMPONENTS-RF1

Base posterior a ENGINE-READINESS/COMMAND/RIBBON: se corrigieron contratos históricos de RibbonView para exportación, inicio y configuración, sin reintroducir soporte técnico en Ribbon. Se agregó `SettingsActionBar` para extraer la barra inferior de Configuración y `OperationalStatusStrip` como componente transversal sobrio para estado/siguiente paso. Tests nuevos: `SettingsSplitRf1SourceTest`, `GuiComponentsRf1SourceTest`.


### XTTS legacy command repair y claridad CPU/GPU

Tras observar nuevamente `FileNotFoundError ... model.pth/model.pth` en la prueba real de Voz IA avanzada, se reforzó la defensa: comandos antiguos/localizados como `Voz IA avanzada-file-to-wav.ps1`, `componentes locales IA avanzada-wrapper` o `recursos locales IA avanzada` se detectan como XTTS legacy y se reconstruyen con el comando administrado actual (`scripts/tts/xtts-file-to-wav.ps1`, `tools/xtts-wrapper`, `models/tts/xtts`). Se agregó script puente `scripts/tts/Voz IA avanzada-file-to-wav.ps1` y test de compatibilidad. La prueba de voz ahora consulta CPU/GPU con `applicationRoot`. Piper/Voz local simple queda explícitamente documentado como motor CPU: la GPU no lo acelera.
