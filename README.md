# VIDEO-SMOKE-RC1 + DIAGNOSTIC-TO-UI-HF1 + DEMO-READINESS-HF1 — actualización

Se agregó un smoke operativo para MP4 final (`InspectFinalVideoSmokeReadinessUseCase` y `scripts/38-smoke-video-final.bat`), se conectó la exportación del reporte diagnóstico a un message box mediante `DiagnosticUserDecisionFactory`, y el selector de ejemplos ahora muestra readiness operativo antes de crear el demo (`InspectExampleProjectReadinessUseCase`). Documento: `docs/productizacion/VIDEO_SMOKE_RC1_DIAGNOSTIC_TO_UI_DEMO_READINESS_HF1.md`.

---

# GPU-XTTS-RUNTIME-HF1 — actualización

Voz IA avanzada notifica con message box cuando la app toma una decisión defensiva en `Preferir GPU` y usa CPU porque el Python local no tiene CUDA confirmada por smoke. En `Dispositivo específico` se respeta la selección manual y el runtime informa si no la soporta. La evidencia incluye `deviceArgument`, GPU reportada por PyTorch, versión PyTorch/CUDA e issues del smoke. Documento: `docs/productizacion/GPU_XTTS_RUNTIME_HF1_DECISION_VISIBLE.md`.

---

## XTTS-MODEL-PATH-HF1 — corrección de ruta model.pth

Voz IA avanzada ya normaliza rutas de modelo cuando una configuración vieja o selección accidental apunta al archivo `model.pth` en vez de la carpeta del modelo. Esto evita el error `model.pth/model.pth` durante la prueba WAV. Documento: `docs/productizacion/XTTS_MODEL_PATH_HF1_MODEL_PTH_NORMALIZADO.md`.

## FIRST-USE-ONBOARDING1 — primera experiencia orientada a escuchar rápido

Configuración inicial ahora prioriza Voz local simple para que el usuario pueda abrir un documento y escuchar rápido. Voz IA avanzada queda como mejora posterior para voces por muestra; Video local queda para exportar MP4 final. La guía integrada se actualiza para hablar de audio final y video MP4, no de paquetes renderizables en el flujo normal. Documento: `docs/productizacion/FIRST_USE_ONBOARDING1_ESCUCHAR_RAPIDO.md`.

## DOCUMENT-SIDEBAR-VOICE-UX1 — Documento: Voz generada o Audio del computador

## VOICE-CHUNKS-HF2 — barra de estado, dependencias XTTS y continuidad de buffer

- Fija `transformers==4.44.2` para el runtime local de Voz IA avanzada y verifica `BeamSearchScorer` antes de generar WAV.
- Acorta la barra de estado: `Renderizar desde aquí`, `Rehacer chunks`, `Seguir generando` y `Detalles`.
- Corrige estilo presionado de botones para evitar texto oscuro sobre fondo oscuro.
- Evita saltar al primer cue disponible cuando el usuario pidió reproducir desde un título/fragmento exacto.
- Reanuda automáticamente si un nuevo manifest recupera el buffer después de quedarse sin chunks.


El panel Audio de Documento se simplifica para usuario final: se separa `Voz generada` de `Audio del computador`, se cambia `Tono de referencia` por `Tono`, los ComboBox muestran solo nombres simples y Documento ofrece únicamente los tonos registrados para la voz seleccionada. Las muestras de María/Neutral/Feliz/etc. siguen siendo referencias para generar texto nuevo, no clips fijos.

# Estado reciente — VOICE-LIBRARY-SYNC1

Se cierra la sincronización transversal de voces: las muestras de usuario se gestionan en `voice-library/samples/` bajo el root de la app/runtime, no dentro de cada carpeta de proyecto. Documento escucha la biblioteca activa y muestra solo los tonos realmente registrados para la voz elegida. Documento técnico: `docs/productizacion/VOICE_LIBRARY_SYNC1_APP_LIBRARY_DOCUMENTO.md`.

## Estado reciente — VOICE-GPU-DEVICE-HF2

Se corrigió el contrato del selector CPU/GPU: el dispositivo elegido aplica a los procesos de voz. Voz local simple recibe el dispositivo solicitado y puede usar GPU si su runtime lo soporta. Voz IA avanzada mantiene smoke CUDA para el modo automático, pero en dispositivo específico respeta la intención manual y deja que el runtime informe si no lo soporta.

VOICE-CHUNKS-HF1: GPU honesta, barra de estado con Renderizar desde aquí y porcentaje Documento N%.

# VOICE-UX-POLISH1A — Vista Voces limpia y operativa

Estado vigente: se limpia la Vista Voces sin meter todavía grabación. Inicio ya no muestra botón muerto `Ver estado de voces` ni `Resumen de operación`; la lista de voces es el foco operativo y muestra tipo humano + tonos registrados. Configurar motor conserva ComboBox de motor/dispositivo, corrige el microcopy ilegible y retira `Estado y validación` del flujo principal. Los ComboBox de tonos usan nombres simples mediante `VoiceToneLabelPolicy`. Documento técnico: `docs/productizacion/VOICE_UX_POLISH1A_LIMPIEZA_OPERATIVA.md`.

# MOTOR-GPU-SMOKE1-HF2 — probe CUDA robusto

Hotfix sobre `PLAYBACK-SPEED-HF9`: la prueba `Probar GPU para Voz IA avanzada` ya no ejecuta un bloque largo mediante `python -c`, porque en Windows podía romper comillas y producir `SyntaxError: unterminated string literal`. Ahora el probe se escribe a un `.py` temporal y se ejecuta con el Python autocontenido de la app. Si CUDA sigue sin aparecer, el resultado ya debe reflejar un problema real del runtime PyTorch/CUDA, no un error de sintaxis del smoke.

# PLAYBACK-SPEED-HF9 — transición inmediata a 1.5x/1.75x

Estado vigente: se corrige la continuidad de playback a velocidades aceleradas. El audio ya sonaba a `1.5x`/`1.75x`, pero la transición podía esperar un fallback conservador. Se agrega `PlaybackTimingPolicy`, se reducen márgenes dispersos y el fin natural del WAV se acepta de forma más robusta antes de caer al watchdog. Documento: `docs/productizacion/PLAYBACK_SPEED_HF9_TIMING_REAL.md`.

# DOC-INDEX-PLAYBACK-HF1 — índice respeta pivote de reproducción

Estado vigente: se corrige la continuidad posterior a MOTOR-GPU-SMOKE1. La app ya no expone nombres técnicos en strings de presentación para el smoke CUDA y `RunXttsCudaSmokeUseCase` respeta la frontera de capas. Además, al seleccionar un bloque desde el índice y pulsar `Escuchar`, `listenToDocument()` usa ese bloque como pivote y genera audio desde el segmento asociado antes de caer a generación global. Documento: `docs/productizacion/DOC_INDEX_PLAYBACK_HF1_GENERAR_DESDE_INDICE.md`.

## Estado reciente — MOTOR-GPU-SMOKE1

Segunda tanda del plan en piedra implementada. Voz IA avanzada solo puede usar GPU si el Python autocontenido confirma PyTorch + CUDA mediante smoke local; si no, la app cae a CPU de forma honesta. Se agregó `runtime/tts/xtts-smoke/xtts-cuda-smoke.json` como evidencia, `Probar GPU para Voz IA avanzada` en Configuración y el script auxiliar `scripts\37-smoke-cuda-xtts.bat`. Próxima tanda: `DOC-INDEX-PLAYBACK-HF1`.

## Estado reciente — MOTOR-ADV-READY-GATE1

La Voz IA avanzada ya no se considera usable para generar chunks de documento solo por tener runtime/modelo descargados. La generación documental avanzada exige una prueba WAV real (`generatedWavProof`) generada desde el runtime local. Detalle: `docs/productizacion/MOTOR_ADV_READY_GATE1_USABLE_REAL.md`.

# DOC-PERF/MEM1 — índices ligeros para documentos grandes

Estado: implementada sobre PLAYBACK-SELECT1/MODEL-PORT1. Corrige el diagnóstico `20260606-170202.zip` reduciendo `DocuPodcastShellViewModel` bajo el límite transitorio RF-TX1 y optimiza el lector para documentos grandes con `blockIndexById` y `sentenceSpanIndex`, evitando reescaneos completos durante navegación, selección y actualización visual. Documento: `docs/productizacion/DOC_PERF_MEM1_INDICES_LIGEROS.md`.

# DOC-INDEX-HF13 — índice navegable del documento

Estado: implementada sobre EXPORT-CLEAN1 verde. Se agrega `DocumentIndexPanel` como módulo lateral plegable `Índice` dentro del workspace Documento. Usa `TreeView<IndexEntry>` para títulos/secciones/subsecciones; cuando no hay encabezados, ofrece navegación plana por bloques narrables y visuales. El salto llama a `selectBlock(...)`, por lo que funciona también con la ventana virtualizada de documentos grandes. No se reintroduce el panel viejo `Estructura documental`. Documento: `docs/productizacion/DOC_INDEX_HF13_INDICE_NAVEGABLE.md`.

# EXPORT-CLEAN1 — exportaciones técnicas fuera del flujo común

Base actual: MOTOR-SMOKE4R-HF5 verde. Esta tanda concentra el flujo normal de exportación en salidas finales de producto: audio final y video MP4. `Exportar paquete de soporte`, `Exportar reporte de soporte` y `Validar integridad` quedan disponibles bajo `Ayuda > Soporte avanzado`, no en el menú Exportar, toolbar ni Ribbon. Documento técnico: `docs/productizacion/EXPORT_CLEAN1_EXPORTACIONES_SOPORTE_AVANZADO.md`.

# FFMPEG-PREP1 — Video local descargable y verificable

Base actual: MOTOR-SMOKE4R-HF3 verde. Esta tanda convierte **Preparar video local** en una descarga/preparación real desde la URL configurada. Descarga el ZIP, extrae componentes, copia `ffmpeg.exe` y `ffprobe.exe` a `tools/ffmpeg/bin` y verifica `readyForFinalVideo()` con `libx264`. `Importar carpeta` queda como alternativa de soporte. Documento técnico: `docs/productizacion/FFMPEG_PREP1_VIDEO_LOCAL_DESCARGA_GUIADA.md`.

# VOZ-UX4R-3D — catálogo teatral de tonos en Vista Voces

# MOTOR-SMOKE4R-HF2 — etiqueta visible de modelo sin nombres técnicos

- Hotfix sobre HF1 tras diagnóstico `20260606-121332`: Configuración ya no muestra `Página del modelo XTTS-v2` ni `/resolve/main` como ayuda visible. La UI dice `Página oficial del modelo de voz` y conserva la normalización técnica solo por dentro. Documento: `docs/productizacion/MOTOR_SMOKE4R_HF2_PRESENTACION_URL_MODELO_VOZ.md`.


Base verde anterior: VOZ-TTS5B. Esta tanda habilita el catálogo teatral extendido en el ComboBox de tonos de Vista Voces, mantiene filas sobrias sin dashboard y agrega el guardarraíl `VoiceUx4R3DTheatricalToneCatalogSourceTest`. No toca motores, playback ni Documento.

# Estado actual — VOZ-TTS5B

Base actual: VOZ-TTS5A verde + VOZ-TTS5B. Documento ya filtra voces/tonos reales y ahora la generación TTS recibe la `VoiceLibrary` del proyecto para resolver la muestra de referencia por unidad de audio. El render usa voz + tono real cuando existe muestra registrada, y hace fallback honesto a Neutral de la misma voz si el tono solicitado no tiene muestra. Documento técnico: `docs/productizacion/VOZ_TTS5B_RENDER_VOZ_TONO_REAL.md`.

# Estado actual — VOZ-TTS5A

Base actual: VOZ-UX4R-3C-HF1 verde + VOZ-TTS5A. Documento ahora filtra voces y tonos reales: la Voz IA avanzada solo ofrece voces con muestra Neutral registrada y el ComboBox de tono se llena con tonos realmente registrados para esa voz. La Voz local simple permanece sin emociones ni muestras humanas.

# VOZ-UX4R-3C-HF1 — Vista Voces sin dashboard, compilación corregida

Hotfix sobre VOZ-UX4R-3C. Corrige el fallo local de Maven reportado en `20260606-095144.zip`: `VoiceLibraryWorkspaceView.java` mantenía una referencia residual a `summary` en el módulo Inicio de Voces. La vista conserva el sidebar oscuro/full-height y el diseño anti-dashboard, pero ahora compila al eliminar esa referencia obsoleta. Documento técnico: `docs/productizacion/VOZ_UX4R_3C_HF1_COMPILE_FIX.md`.

Validación recomendada en Windows: `scripts\99-diagnostico-completo.bat`.

# VOZ-UX4R-3C — Vista Voces sin dashboard

Vista Voces conserva sus tres módulos (**Inicio**, **Configurar motor**, **Gestionar voces**), pero el módulo Inicio deja de usar métricas/tarjetas tipo dashboard. El sidebar ahora usa una paleta oscura sobria inspirada en Teams, ocupa toda la altura útil y mantiene botones simples por módulo. Se reutilizan `ActionBar` e `InfoBadge` en la superficie. Documento técnico: `docs/productizacion/VOZ_UX4R_3C_SIDEBAR_OSCURO_ANTI_DASHBOARD.md`.

# VOZ-UX4R-DOC1 — contrato final modular de Vista Voces

Se alinea la documentación para que Vista Voces avance como una microaplicación administrativa sobria, no como dashboard futurista ni panel técnico. El contrato final define solo tres módulos principales: **Inicio**, **Configurar motor** y **Gestionar voces**. Inicio lista voces y estados; Configurar motor permite elegir motor, dispositivo CPU/GPU real y probar texto; Gestionar voces permite crear/editar/eliminar voces, importar/grabar/reemplazar muestras por emoción y exportar muestras. Neutral es obligatoria; Documento solo mostrará voces con Neutral y emociones realmente registradas. Documento: `docs/productizacion/VOZ_UX4R_CONTRATO_FINAL_MODULOS.md`.


## PLAYBACK-SPEED1 — controles de velocidad en playbar

Se agregan botones `1x`, `1.5x` y `1.75x` en la playbar del Documento. La velocidad afecta playback de WAV ya generados, no generación TTS. `JavaSoundSegmentAudioPlayer` aplica velocidad simple por frecuencia efectiva de salida, `PlaybackCueClock` sincroniza cursor/visual al ritmo acelerado y el buffer existente sigue esperando si la lectura rápida alcanza un chunk pendiente. Documento: `docs/productizacion/PLAYBACK_SPEED1_CONTROLES_VELOCIDAD.md`.

# DOC-UX-HF10H — progreso legible, tamaño generado y rail Visual honesto

Se ajusta la experiencia posterior a HF10G: el overlay muestra audio generado en disco en vez de tamaño estimado, la ETA incluye horas, la barra de progreso es más gruesa y verde, el rail Visual explica su virtualización y evita scroll horizontal, y Configuración > Video local diferencia URL centralizada de importación desde carpeta. Documento: `docs/productizacion/DOC_UX_HF10H_PROGRESO_VIDEO_RAIL.md`.

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

Se corrigen los fallos de copy técnico visible y se agrega un flujo de primer uso: Inicio muestra **Configuración inicial**, Guía rápida queda funcional, Configuración puede abrir directamente en motores de voz/video y el usuario puede aceptar preparar el equipo desde la app. Se retiran de la configuración normal auditorías, smoke GUI y rutas internas; quedan en scripts/diagnóstico.

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

Estado: implementada sobre TE1 verde.

Se alinea el lenguaje visible de visuales y video: la UX normal usa **Visuales / Secuencia visual** y no `storyboard` como promesa de producto. La exportación de video simple ahora informa estado honesto del paquete: prepara plan, CSV, manifiesto y comandos, pero no crea el MP4 final por sí sola. `SimpleVideoPackageExportResult` expone `renderModeLabel`, `renderableAsMp4`, `outputFileName` y `honestStatusLabel()`.

Documento: `docs/productizacion/TV1_LIMPIEZA_VISUALES_VIDEO_HONESTO.md`.

# TE1 — Exportaciones alineadas a voces por tono

Estado: implementada sobre TC1 verde.

El paquete exportado ahora incluye reportes auditables de muestras de voz por tono: `reports/VOICE_REFERENCE_SAMPLES.md` y `reports/VOICE_REFERENCE_SAMPLES.tsv`. El manifiesto resume muestras registradas, assets, archivos faltantes y pruebas de voz generadas. `ProjectBundleExportResult` expone el conteo de muestras y las rutas de los reportes. No cambia UX ni CSS; los mensajes visibles de exportación mantienen lenguaje amigable y no nombres técnicos de motores.

Documento: `docs/productizacion/TE1_EXPORTACIONES_ALINEADAS_VOCES_TONO.md`.

# TC1 — CommandAvailabilityPolicy

Estado: implementada sobre RF5 verde.

Se agrega `CommandAvailabilityPolicy` como fuente única de disponibilidad para comandos visibles. Menú, Ribbon y `WorkspaceCapabilityPolicy` consumen la misma política, evitando reglas duplicadas entre superficies. La tanda no cambia UX, CSS ni componentes visuales; conserva nombres amigables de motores y agrega razones humanas para futuras tooltips/status.

Documento: `docs/productizacion/TC1_COMMAND_AVAILABILITY_POLICY.md`.

# RF5 — Limpieza residual STT/Whisper

Estado: implementada sobre RF4 verde.

Se cierra una limpieza residual de audio-a-texto: `VoiceSourceKind` elimina `HUMAN_RECORDING_FOR_TRANSCRIPTION`, `PerformanceSpan` queda limitado a voz IA, audio del computador o sin asignar, y el smoke real opt-in deja de imprimir mensajes de audio-a-texto/STT. Se agrega guardarraíl RF5 para impedir que `src/main/java` o `scripts` vuelvan a traer Whisper, SpeechToText, STT operativo, `allowGpuForStt`, `audio a texto` o `HUMAN_RECORDING_FOR_TRANSCRIPTION`. No cambia la UX ni CSS; audio del computador sigue existiendo como clip asociado al documento, no como transcripción.

Documento: `docs/productizacion/RF5_LIMPIEZA_RESIDUAL_STT_WHISPER.md`.

# RF4 — Limpieza de workspaces heredados

Estado: implementada sobre RF3 verde.

Se cierra la cuarentena real de workspaces heredados: `WorkspaceViewRegistry` rechaza factories para superficies internas, `viewFor(...)` normaliza solicitudes heredadas hacia superficies de producto y `DocuPodcastShellViewModel` deja de exponer métodos públicos de apertura de Guion interno, Procesos de audio o Secuencia visual como workspaces. El Shell sigue sin registrar handlers para comandos legacy ocultos. No se borran paquetes internos para preservar compatibilidad.

Documento: `docs/productizacion/RF4_LIMPIEZA_WORKSPACES_HEREDADOS.md`.

# RF2 — Coordinadores de presentación / selección documental

Estado: implementada sobre RF1.

Se agrega `DocumentSelectionCoordinator` para mover fuera de `DocuPodcastShellViewModel` las reglas de presentación de selección documental: previews, etiquetas de oración/fragmento/capa, ubicación fuente y copy de la acción primaria del documento. Se corrige el source test de T102 para reconocer que la persistencia del zoom de lectura ya vive en `ReadingComfortCoordinator`. No cambia la UX ni CSS, y la GUI sigue sin mostrar nombres técnicos de motores.

Documento: `docs/productizacion/RF2_COORDINADORES_PRESENTACION_DOCUMENT_SELECTION.md`.

# T121-V10 — Tests, documentación y smoke visual de Voces

Estado: implementada sobre T121-V09.

Se cierra la serie funcional de Vista Voces con guardarraíles finales, smoke visual manual en `docs/testeo/SMOKE_VISUAL_VOCES_FINAL.md`, documentación de producto y corrección del source test V04C para validar componentes extraídos. La interfaz gráfica normal mantiene nombres amigables: Voz IA avanzada, Voz local simple y Modo de prueba.

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

## Estado vigente — T124

Base vigente: T124 — Cuarentena legacy workspaces/tests.

DocuPodcast Studio conserva algunas clases heredadas para compatibilidad interna, pero la navegación de producto queda limitada a Inicio, Documento y Voces. `SCRIPT_EDITOR`, `AUDIO_JOBS` y `STORYBOARD` se normalizan hacia Documento y no deben registrarse como workspaces principales. Storyboard se trata como secuencia visual/panel visual del Documento, no como módulo principal.

Documento: `docs/productizacion/T124_CUARENTENA_LEGACY_WORKSPACES_TESTS.md`.

---

## Estado vigente — T123-CAT01

Base vigente: T123-CAT01 — Markdown como documento, no guion.

Markdown entra únicamente por el flujo **Abrir documento** junto con Word/DOCX, PDF con texto nativo y TXT. Se retira la vía heredada de importar/exportar Markdown como narración o guion; los recursos IA oficiales quedan como guías/plantillas/ejemplos documentales no importables como contrato especial.

Documento: `docs/productizacion/T123_CAT01_MARKDOWN_COMO_DOCUMENTO_NO_GUION.md`.

---

## Estado vigente — T114-HF2

Base vigente: T114-HF2 — Retiro total Whisper/STT.

DocuPodcast Studio no usa Whisper, STT, Speech-to-Text ni audio a texto. La hotfix retira el cableado de `application/stt`, `infrastructure/stt`, `scripts/stt`, `SpeechToTextApplicationServices`, `SttEngineSettings`, `allowGpuForStt` y `ModelFolderContract.whisperLocal()`. La configuración operativa queda centrada en lectura, TTS, FFmpeg, CPU/GPU para TTS/video, almacenamiento y diagnóstico.

Se conserva la regla de producto de T114-HF1: DocuPodcast abre documentos y prepara lectura; Guion no es categoría visible del usuario. Markdown entra como documento, no como guion.

Documento: `docs/productizacion/T114_HF2_RETIRO_TOTAL_WHISPER_STT.md`.

Nota de alcance: Whisper no pertenece al núcleo de producto. Se retira Audio a texto del flujo visible y también del build principal.

---

## Estado vigente — T114-HF1

Base vigente: T114-HF1 — Build verde + no-guion visible mínimo.

Esta hotfix conserva la arquitectura interna existente, pero recupera la intención de producto: DocuPodcast abre documentos y prepara lectura. Guion no debe aparecer como workspace visible ni como categoría de usuario. Los tests RF1 de exportación ahora validan `ExportWorkflowCoordinator`, no devuelven lógica al ViewModel. Los flujos tocados vuelven a Documento en vez de activar superficies legacy.

Documento: `docs/productizacion/T114_HF1_BUILD_VERDE_NO_GUION_VISIBLE.md`.

---

## Estado vigente — TI2 Audio jobs desde RenderPlan

La base vigente es TI2. La cola de audio ya puede generarse desde `RenderUnitPlan`: el TTS genera solamente unidades habladas que requieren síntesis y omite clips externos, visuales silenciosos y unidades omitidas.

Esto mantiene compatibilidad con jobs legacy por segmento y prepara la migración posterior de playback, storyboard/video y exportación hacia unidades de render.

Documentos principales:

- `docs/productizacion/TI2_AUDIO_JOBS_DESDE_RENDERPLAN.md`
- `docs/174_TI2_AUDIO_JOBS_DESDE_RENDERPLAN.md`
- `docs/productizacion/ROADMAP_RESTANTE_POST_TI1_DETALLADO.md`

Siguiente recomendada: TI3 — Storyboard/video desde RenderPlan.

---

## Estado tras TP2

Base vigente: TP2 — Preflight integrado a arranque/configuración.

TP2 corrige la documentación raíz que había quedado demasiado resumida tras TP1 y agrega una capa humana de estado de motores: listo, requiere preparación o error. La Configuración muestra el estado y la siguiente acción sin convertir el Documento en cabina técnica.

Puntos vigentes:

- Documento como raíz V1: abrir fuente, leer/escuchar, ajustar capas opcionales y exportar.
- Ejemplos internos existen mediante menú **Ejemplos** y ventana secundaria.
- `RuntimePathResolver` define raíz de aplicación y layout `tools/`, `models/`, `scripts/`, `examples/`.
- Preflight integrado resume Coqui/XTTS, Piper y FFmpeg con mensajes humanos y acción sugerida.
- Iconos PNG grandes usan componentes GUI transversales.
- Overlay de procesos largos es compacto, ocultable y restaurable desde status bar.
- Whisper no pertenece al núcleo de producto. Se retira Audio a texto del flujo visible; se retiró del build principal y no debe reintroducirse.

Siguiente recomendada: TP3 — Packaging tools/models/scripts.

## Guardarraíles históricos vigentes para continuidad

- Tanda 58C sigue como protocolo histórico de smoke exploratorio mínimo.
- Tanda 81D y T81E documentan el pulido frontend previo al camino de Release Candidate real.
- Tanda 85 dejó Piper como primer puente real texto → WAV sin reemplazar Coqui XTTS sigue siendo obligatorio para calidad alta.
- DocuPodcast Studio — Tanda 90 documenta el smoke opt-in de motores reales; no es Release Candidate final.
- Tanda 88C fijó el alcance de motores visibles: Coqui/XTTS, Piper y FFmpeg; No agregar más motores al producto core.
- T97 — Componentes GUI transversales congeló la política de no hardcodear controles ni volver a etiquetas truncadas.
- Base vigente: T112 fue el cierre de smoke visual UX / RC visual antes de Ejemplos.
- Base vigente: T113 fue el cierre de Menú Ejemplos + proyectos demo internos antes de productización.
- Documento preparado para lectura: la lectura preparada usa una proyección interna temporal, no un segundo producto padre obligatorio.


## Productización TP4-TP6

Base vigente: TP4 — Licencias y manifest de terceros.
Base vigente: TP5 — Instalador/app portable real.
Base vigente: TP6 — RC instalable con smoke manual y automático.

- TP4 agrega inventario legal de FFmpeg, Piper, Coqui/XTTS, Python portable, JavaFX, dependencias Maven y ejemplos internos.
- TP5 prepara app-image/portable/MSI con runtime, tools, models, scripts y carpeta legal cuando existan.
- TP6 encadena diagnóstico, runtime layout, manifest de terceros, app-image, carpeta portable y smoke RC.

## TI1 — RenderUnit end-to-end

Base vigente: TI1 — RenderUnit end-to-end.

TI1 agrega el contrato central `RenderUnit` / `RenderUnitPlan` para unir documento, guion, capas, audio, storyboard y video sin seguir forzando que todo nazca de un segmento narrable. La regla protegida queda así: audio, visual, visual silencioso y omisión son decisiones explícitas de la unidad de render.

Nuevos elementos:

- `RenderUnitKind` con `SPOKEN_ONLY`, `SPOKEN_WITH_VISUAL`, `VISUAL_SILENT` y `OMITTED`.
- `RenderUnit` como decisión final de media por unidad.
- `RenderUnitPlan` con conteos de unidades habladas, visuales, silenciosas y omitidas.
- `BuildRenderUnitPlanUseCase` como puente entre `NarrationRenderPlan` y la integración audio/video posterior.
- `RenderApplicationServices` expone `buildRenderUnitPlan`.

Documentación principal:

- `docs/productizacion/TI1_RENDERUNIT_END_TO_END.md`.
- `docs/productizacion/ROADMAP_RESTANTE_POST_TI1_DETALLADO.md`.
- `docs/173_TI1_RENDERUNIT_END_TO_END.md`.

Siguiente recomendada: TI2 — Audio jobs desde RenderPlan.


## TI3 — Storyboard/video desde RenderPlan

La base vigente incorpora TI3: el paquete storyboard/video puede construirse desde `RenderUnitPlan`, omite unidades sin visual, acepta frames hablados con imagen y frames visuales silenciosos con silencio sintético FFmpeg.


## Estado posterior TI4 — bloques visuales no narrables

La base vigente incorpora TI4: imágenes, tablas y fórmulas/matemática detectadas en la fuente se tratan como bloques visuales no narrables por defecto. Se muestran o identifican en Documento, no se narran automáticamente y no entran al storyboard/video hasta que el usuario asigne un visual.


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

## TI7 — Playback por oración/unidad

La base vigente incorpora playback unit-aware desde `RenderUnitPlan`. El manifiesto de reproducción puede localizar cues por `unitId`, resolver audio generado por TTS o audio humano/local y omitir visuales silenciosos. Las tablas, imágenes y fórmulas detectadas como bloques fuente no narrables no se leen en voz.

### Siguiente recorrido después de TI7

Con TI7 cerrado, el recorrido técnico pendiente entra en la fase RF: dividir `DocuPodcastShellViewModel`, extraer coordinadores de presentación y crear use cases de orquestación de producto.

## RF1 — Dividir `DocuPodcastShellViewModel`

Se inició el refactor de presentación extrayendo `presentation.shell.workflow.ExportWorkflowCoordinator` para que el shell delegue exportaciones sin cambiar comportamiento visible. El objetivo es reducir deuda progresivamente antes de extraer media, playback, voces y procesos largos.


## T122-C01 — PreparedReadingProjection

Se agrega una frontera `PreparedReadingProjection` para que el producto mantenga el flujo Documento → lectura preparada, dejando `NarrationScriptDocument` como payload interno temporal para audio, render, playback y persistencia. No se reintroduce Guion como categoría de usuario.

## T115 — Selector CPU/GPU real para Coqui/XTTS

Se agrega detección CPU/GPU para seleccionar el dispositivo de Coqui/XTTS desde Configuración. CPU siempre queda disponible; en automático NVIDIA se valida con smoke CUDA, y en dispositivo específico la selección manual llega al runtime para que use o rechace el backend disponible.



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

- **T119B — UX por capacidades del motor de voz:** Documento y Voces ahora usan `VoiceEngineCapabilityProfile`; Piper se muestra como narrador local intermedio sin emociones/clonación por muestra, Coqui/XTTS conserva las capacidades avanzadas cuando el motor lo soporte y Mock se declara como modo de prueba.

## T119C/T120 — Manifiestos de motores y onboarding limpio

La base incorpora el contrato `ENGINE_ARTIFACTS_MANIFEST` para inventariar FFmpeg, FFprobe, Python local, Coqui/XTTS, voz neutral y Piper con licencia y SHA-256 antes de RC final. El Inicio ahora orienta el primer uso hacia Preparar voz, Abrir documento, Escuchar y Exportar, sin Guion, Whisper/STT ni Storyboard como módulo principal.

## T120B — Contrato final del Ribbon

El Ribbon queda organizado por intención: Inicio, Lectura, Vista y Exportar. La pestaña Storyboard deja de ser superficie principal; Documento y Voces viven en Vista, y el rail visual queda como panel del Documento.

## PF2D/PF3A — Progreso in-app y Voces operativas

Base vigente: PF2D/PF3A sobre PF2B/PF2C. La preparación de Voz IA avanzada debe vivirse desde Configuración, no como requisito manual de consola. El script queda como adaptador técnico/rescate; la ruta normal es verificar, preparar, descargar/importar y usar desde botones de la app con progreso visible.

Reglas actuales de producto:
- T128-C01 — Documentación de limpieza de producto.
- Markdown se abre como documento fuente.
- Whisper/STT/audio a texto fue retirado del build principal.
- PreparedReadingProjection mantiene la lectura preparada desde el documento.
- No existe subir guion como raíz del producto.
- Documento narrable es el objeto principal: abrir fuente documental, leer, escuchar, ajustar capas opcionales y exportar.
- Whisper no pertenece al núcleo de producto.
- Se retira Audio a texto del flujo visible.

## Índice histórico de guardarraíles conservados

Este índice conserva anclas de tests de producto ya implementados para no perder contexto al productizar:
- Tanda 58C — smoke exploratorio mínimo.
- Tanda 81D — frente visual y cierre progresivo de UX.
- Tanda 88C — alcance limpio de motores: Voz IA avanzada, Voz local simple y FFmpeg.
- DocuPodcast Studio — Tanda 90 — smoke de motores reales.
- Tanda 85 — texto → WAV real en Voz local simple, sin reemplazar la voz avanzada obligatoria.
- T97 — Componentes GUI transversales.
- Base vigente: TP2.
- Base vigente: TP6.
- T121-V10 — cierre final de Biblioteca de voces.
- RF3 — orquestación de producto sin cambio visual, con PrepareListeningSessionUseCase.
- RF4 — Limpieza de workspaces heredados.
- RF5 — Limpieza residual STT/Whisper.


## Estado PF2F/PF4A

- Base corregida desde diagnóstico `20260604-080938.zip`.
- Maven fallaba solo por 3 source tests de textos/guardarraíles de Configuración; se alinearon etiquetas y notas.
- Configuración muestra progreso con latido independiente y ruta de logs mientras prepara/descarga/importa motores.
- PF4A agrega launcher portable `run-docupodcast-studio.bat` con `DOCUPODCAST_APP_ROOT=%%~dp0`.
- Próxima tanda: PF4B — smoke post-app-image y verificación de runtime root desde launcher.


## DOC-UX-HF9A — Hotfix compilación y refresco visual Word

Corrige el fallo Maven provocado por una lambda que capturaba `storyboard` reasignado en `DocuPodcastShellViewModel`. Refuerza además el refresco de visuales internos del Word: si el texto narrable no cambia pero se recuperan imágenes/tablas internas, el proyecto queda marcado para guardar esos visuales sin invalidar audio.

## DOC-UX-HF9 — Selección fina, rail visual y audio sincronizado

Base posterior a PLAYBACK-HF8R. La hoja de Documento ya diferencia selección por oración de selección por bloque completo; el playback sincroniza la oración activa cuando el cue es unitario; el rail Visual lista todos los fragmentos y reconstruye miniaturas desde capas de imagen actuales. El sidebar Audio etiqueta el origen según el motor activo: Voz IA avanzada, Voz local simple o Modo de prueba. DOCX conserva imágenes internas, tablas como vista previa no narrable y LaTeX/fórmulas como detección sin render.

## DOC-UX-HF9C — diagnóstico verde y sincronización final de imagen

Se alinea el panel Imagen izquierdo con el rail Visual derecho: ambos resuelven la imagen asociada desde la misma proyección por frase/fragmento. También se actualizan guardarraíles posteriores a HF9B para conservar el rail por frase, la selección por oración y los bloques visuales DOCX no narrables.

## DOC-UX-HF9D — Menú contextual de imagen en rail Visual

El rail derecho de Documento ahora ofrece menú contextual por tarjeta/frase: si el fragmento tiene imagen, el usuario puede copiarla al fragmento anterior o posterior. Si la tarjeta no tiene imagen, o no existe vecino, la acción queda deshabilitada. Al copiar, el fragmento destino queda seleccionado para que el sidebar izquierdo Imagen muestre la misma miniatura inmediatamente.

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


### Base reciente: DOC-UX-HF10E / DOC-TABLE-HF11B

La generación de chunks desde la barra de estado prepara la lectura si hace falta y las tablas DOCX usan un componente transversal estilizado (`SourceTableGridView`). Ver `DOCUMENTACION_ACTUAL/`.


## DOC-UX-HF10F

Tanda vigente posterior a DOC-UX-HF10E: limpia contenedores redundantes en tablas/imágenes fuente y mueve el cálculo de tamaño estimado de chunks fuera del hilo JavaFX para que el overlay de generación sea más ancho y no bloquee la interacción. Ver `DOCUMENTACION_ACTUAL/01_REGISTRO_TANDAS_RECIENTES.md`.


## PLAYBACK-SPEED2 — continuidad y tono natural

- Velocidad `1x`, `1.5x` y `1.75x` en playbar.
- `1.5x`/`1.75x` usan time-stretch PCM local para conservar mejor el tono, no sample-rate shift.
- Reproducir desde selección continúa con los siguientes chunks; `Reproducir fragmento (solo este)` queda como acción corta explícita.
- Si la lectura rápida alcanza un chunk pendiente, mantiene la espera de buffer del flujo existente.

## PLAYBACK-SPEED3 — continuidad al cambiar velocidad

Corrige la continuidad después de cambiar entre `1x`, `1.5x` y `1.75x` durante un chunk. El ViewModel ahora resincroniza el reloj con el cue activo y la posición real del reproductor, usa `playingCueSegmentId` como fuente de verdad durante el tick y evita que un cursor pausado dispare autostart de buffer repitiendo la selección. Documento: `docs/productizacion/PLAYBACK_SPEED3_CONTINUIDAD_CAMBIO_VELOCIDAD.md`.

## PLAYBACK-SPEED6 — controlador estructural de continuidad

Se extrajo la continuidad de playback a `PlaybackContinuationController` y se agregó callback real `SegmentAudioPlayer.setOnPlaybackFinished(Consumer<Path>)`. El avance entre chunks ya no depende solo del polling de `Timeline`: Java Sound notifica el final natural del WAV activo y el ViewModel transiciona al siguiente cue validando que el archivo terminado corresponde al cue vigente.

## PLAYBACK-SPEED7 — manifest runtime y cola reproducible

La continuidad de lectura ahora reconstruye el manifest en runtime antes de iniciar y después de cada chunk. `PlayableAudioJobSelector` escoge el job persistido con más audio compatible con el script actual, evitando que un manifest temprano/parcial deje la reproducción detenida después de uno o dos WAV. Ver `docs/productizacion/PLAYBACK_SPEED7_MANIFEST_RUNTIME_QUEUE.md`.

## PLAYBACK-SPEED8 — watchdog de continuidad

Se agregó un watchdog independiente del timer/callback del reproductor para evitar que la lectura se detenga tras uno o dos chunks aunque existan WAV siguientes. También se ajustó la continuación tras buffer para iniciar el `PlaybackCue` exacto.

### PLAYBACK-SPEED9

La lectura continua usa una cola runtime de chunks reproducibles y un monitor periódico del cue activo. El status bar permite desplazamiento horizontal para mensajes largos.


## PLAYBACK-CORE4 — cola exacta guiada por final real del reproductor

- Reproduce por cola exacta de cues, pero prioriza el callback real del reproductor antes de avanzar.
- El watchdog de cola ya no corta audio si Java Sound todavía reporta reproducción activa.
- Corrige inconsistencias observadas al cambiar velocidad, pausar/reanudar y reproducir mientras se generan chunks.
- Documento técnico: `docs/productizacion/PLAYBACK_CORE4_PLAYER_CALLBACK_QUEUE.md`.

# VOZ-UX4R-1 — Lista sobria de voces y contrato vivo

Se corrige el guardarraíl pendiente de playback y se retoma Vista Voces. La lista lateral ahora muestra **Voces creadas** con estados humanos (`Neutral lista`, `Tonos configurados`, `Falta neutral`, `Incompleta`) mediante `VoiceListItemView`. La UI deja de llamar “opcionales” a los tonos no neutrales: son **Tonos**. Queda documentada la secuencia VOZ-UX4R restante y la descarga/verificación de Voz IA avanzada permanece para MOTOR-SMOKE4R.

## VOZ-UX4R-2A — shell modular simple de Voces

Vista Voces deja de usar un `SplitPane` de sidebar/detalle y pasa a una microaplicación administrativa sobria con tres módulos: **Inicio**, **Configurar motor** y **Gestionar voces**. Se agregan `VoiceModuleId`, `VoiceModuleDescriptor`, `VoiceModuleNavigation` y `VoiceWorkspaceShell`. Esta tanda solo deshuesa la superficie: la selección real de motor/dispositivo queda para VOZ-UX4R-2B y la gestión completa de crear/eliminar voces queda para VOZ-UX4R-3.

## VOZ-UX4R-2B — Configurar motor real dentro de Voces

Se completa el módulo Configurar motor de la microaplicación Voces con selector de motor activo, selector de dispositivo de renderizado CPU/GPU detectado, estado honesto y prueba por textbox. Las selecciones se persisten en `OperationalSettings`, la misma configuración interna usada por Configuración. No se resuelve todavía la descarga de Voz IA avanzada/Coqui; queda para `MOTOR-SMOKE4R / COQUI-DL1`.

### VOZ-UX4R-3B — navegación modular sobria de Voces

La Vista Voces mantiene tres módulos internos —Inicio, Configurar motor y Gestionar voces— con botones laterales de solo nombre. El microcopy vive en el workspace derecho. Se corrige el refresco de selección que podía devolver accidentalmente la vista a Gestionar voces al intentar abrir Inicio o Configurar motor. El sidebar queda sobrio, sin degradados, con criterio administrativo de escritorio.


## MOTOR-SMOKE4R / COQUI-DL1 — implementada

Base vigente: Configuración de Voz IA avanzada incluye botón `Probar` para generar un WAV real en `runtime/tts/xtts-smoke`. El estado de motor diferencia descargado, verificado, seleccionable, WAV generado y reproducción confirmada. La descarga grande de Voz IA avanzada puede reanudarse mediante HTTP `Range` cuando existe un `.download` parcial. Documento técnico: `docs/productizacion/MOTOR_SMOKE4R_COQUI_DL1_PRUEBA_WAV.md`.

### MOTOR-PLAYCONF1

Se agrega confirmación de reproducción de la prueba WAV de Voz IA avanzada desde Configuración. La prueba queda completamente verificada cuando el WAV se genera y luego termina de reproducirse con el reproductor interno de la app.

## MOTOR-SMOKE4R-HF3

La preparación de Voz IA avanzada ahora deja diagnóstico accionable de descarga en `models/tts/xtts/download-diagnostics.txt` y la UI muestra qué requisito quedó pendiente cuando la descarga no deja el motor seleccionable.


### VIDEO-EXPORT1

El comando Exportar video prepara un MP4 final con selección de calidad 4K/2K/1080p/720p. Los paquetes técnicos quedan reservados para diagnóstico/soporte.


## AUDIO-COMPRESS1

Exportación final de audio ampliada a WAV, MP3 y AAC. Los segmentos internos siguen siendo WAV; MP3/AAC se generan como entrega final mediante el componente local de video/audio.


## MOTOR-PERF1 / MOTOR-GPU1

La app ahora distingue GPU detectada, GPU candidata y GPU confirmada por prueba real. No se promete aceleración GPU hasta validar el motor/componente correspondiente.

### MOTOR-SMOKE4R-HF5

Hotfix de preparación de Voz IA avanzada: la voz local simple ya no contamina la resolución del speaker avanzado. Si el motor activo es local simple, la inspección avanzada usa `voz-por-defecto.wav` y no busca `voz-local-simple.wav` dentro del modelo avanzado.
## MOTOR-TTS-ADV-HF1 — generación avanzada no queda muda en 0/1

Se corrige el lanzamiento real del proceso de Voz IA avanzada cuando el modelo ya está descargado. El wrapper Python ya no recibe `auto` como device opaco; el modo automático cae a CPU hasta tener prueba GPU real. El proceso se ejecuta con salida sin buffer, reporta fases (`cargando_modelo`, `sintetizando`, `wav_generado`) y el gateway Java destruye árbol de procesos en cancelación/timeout para evitar jobs fantasma al cerrar. Documento técnico: `docs/productizacion/MOTOR_TTS_ADVANCED_JOB_HF1_PROCESO_REAL.md`.



### Nota MAVEN-DIAG-HF1

Los diagnósticos Maven toleran modelos de voz descargados dentro de `models/tts/xtts`: los tests fuente de namespace solo auditan archivos textuales del proyecto y no artefactos binarios locales.
## PLAYBACK-SELECT1 / MODEL-PORT1 — selección como primer chunk y models portable

Estado: implementada sobre la base verde reciente. Se corrige que `Reproducir desde selección` genere desde el segmento seleccionado cuando todavía no existe WAV, evitando preparar desde `SEG-001`. También se permite reutilizar una carpeta `models/` trasplantada entre tandas si `models/tts/xtts` contiene el contrato completo del modelo, aunque la configuración persistente apunte a otra ruta. El módulo Índice ahora usa icono tipo árbol. Documento técnico: `docs/productizacion/PLAYBACK_SELECT1_MODELS_PORTABLES.md`.



### VOICE-REGISTRATION-WIZARD1

Vista Voces suma subvista **Nueva voz** para registrar muestras de referencia por emoción con grabación Java y Neutral obligatoria.


## THEATER-COLON-READ1

Documento puede omitir etiquetas breves antes de dos puntos para guiones teatrales, por ejemplo `Vaquero: texto` → `texto` en la generación de audio. El Word original no cambia y los chunks deben rehacerse.


### VOICE-SAMPLES-RC1 / VOICE-WORKSPACE-HF3 / VOICE-TONE-UX-HF1

La Vista Voces ahora opera como biblioteca de app: importar o grabar muestras ya no exige un proyecto guardado cuando el wiring productivo usa `voice-library/samples`. La grabación usa temporales en `voice-library/tmp-recordings`, añade cancelar grabación y reemplaza textos ambiguos por acciones operativas como `Crear voz`, `Grabar muestra` y `Detener y guardar`. Documento mantiene el contrato de mostrar solo tonos realmente registrados para la voz seleccionada.

### VOICE-HF3-CORRECCION + CSS-TOKENS-HF1

Se corrige el diagnóstico local de VOICE-SAMPLES/WORKSPACE/TONE reduciendo nuevamente la deuda transitoria de `DocuPodcastShellViewModel` por debajo del límite RF-TX2 y moviendo la selección de origen de voz al coordinador de audio. Además se agrega `CSS-TOKENS-HF1`: `-dp-ink` queda definido en tokens y el hover de botones del status bar usa morado pastel claro.
