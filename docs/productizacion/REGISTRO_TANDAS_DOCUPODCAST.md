# VOZ-TTS5A — Documento con voces/tonos reales

Documento ya no muestra el catálogo global de tonos: filtra voces avanzadas por muestra Neutral y tonos por `registeredTones()` de la voz seleccionada. Documento técnico: `docs/productizacion/VOZ_TTS5A_DOCUMENTO_VOCES_TONOS_REALES.md`.

## VOZ-UX4R-3C-HF1 — hotfix compilación Vista Voces

Hotfix aplicado tras diagnóstico local `20260606-095144.zip`. Corrige el fallo Maven en `VoiceLibraryWorkspaceView.java` causado por la referencia residual `box.getChildren().addAll(summary, list);` después de reemplazar el dashboard por filas sobrias. La vista ahora agrega `list` directamente y el guardarraíl `VoiceUx4R3CAntiDashboardSourceTest` bloquea la regresión. No toca motores, playback ni Documento. Documento: `docs/productizacion/VOZ_UX4R_3C_HF1_COMPILE_FIX.md`.

## VOZ-UX4R-3C — sidebar oscuro y Vista Voces sin dashboard

- Base: VOZ-UX4R-3B.
- El módulo Inicio elimina métricas/tarjetas de dashboard (`metricCard`, `voice-dashboard-metrics`, `voice-metric-card`).
- Se reemplazan métricas por filas operativas con `InfoBadge`.
- Configurar motor reemplaza tarjetas redundantes de modo por filas sobrias con estado activo/disponible.
- El sidebar de Voces pasa a paleta oscura formal tipo Teams y se vuelve full-height con spacer expansible.
- Se reutiliza `ActionBar` en filas de acciones de Voces.
- Guardarraíl: `VoiceUx4R3CAntiDashboardSourceTest`.
- Documento técnico: `docs/productizacion/VOZ_UX4R_3C_SIDEBAR_OSCURO_ANTI_DASHBOARD.md`.
- Próxima recomendada: VOZ-TTS5A — Documento filtra voces con Neutral y tonos registrados.

# T121-V10 — Tests, documentación y smoke visual de Voces

- Estado: implementada sobre T121-V09.
- Alcance: cierra la Vista Voces con source tests finales, smoke visual manual, documentación raíz y guardarraíles de lenguaje visible.
- Corrección: `VoiceLibraryWizardWiringT121V04CSourceTest` valida `voice-profile-card` en `VoiceProfileCard` tras la extracción de componentes de V09.
- Regla UX: la interfaz gráfica normal usa `Voz IA avanzada`, `Voz local simple` y `Modo de prueba`; no nombres técnicos de motores.
- Docs: `docs/productizacion/T121_V10_TESTS_DOCUMENTACION_SMOKE_VOCES.md`, `docs/testeo/SMOKE_VISUAL_VOCES_FINAL.md`.
- Próxima tanda: RF1 — Dividir `DocuPodcastShellViewModel`.

## T121-V08 — Rediseño visual final de Voces

- Base: T121-V07 con diagnóstico local rojo por tests fuente heredados de `PerformanceStyle` y de la extracción a `VoiceSampleWorkflowCoordinator`.
- Corrige source tests para validar `VoiceReferenceTone`, `VoiceSampleWorkflowCoordinator` y los textos amigables vigentes.
- `VoiceLibraryWorkspaceView` agrega hero de producto, tarjetas de modo activo, matriz de muestras por tono y lenguaje de registro de muestras por tono.
- CSS: `voice-library-hero`, `voice-engine-card`, `voice-sample-grid`, `voice-sample-row`, `voice-tone-badge`.
- Tests: `VoiceLibraryFinalRedesignT121V08SourceTest`.
- Documento: `docs/productizacion/T121_V08_REDISHENO_VISUAL_FINAL_VOCES.md`.
- Próxima tanda: T121-V09 — Componentes/CSS de Voces.


## T121-V07 — Fallback de tonos faltantes en Documento

- Base: T121-V06 con diagnóstico local rojo por imports faltantes en `DocuPodcastShellViewModel`.
- Corrige compilación agregando imports de `RecordingActionPlan` y `RecordingPurpose`.
- Documento usa `VoiceReferenceTone` como selector de **Tono de referencia** en lugar de `PerformanceStyle` heredado.
- `assignVoiceToneToSelectedDocumentRange(...)` resuelve tono exacto o fallback neutral con `ResolveVoiceToneReferenceUseCase`.
- `VoiceReferenceTone.layerTargetId()` permite persistir el tono como target estable de capa documental.
- `NarrativeLayerTargetResolver` e integridad aceptan estilos legacy o tonos nuevos para `NarrativeLayerKind.EMOTION`.
- Tests: `VoiceReferenceToneLayerTargetTest`, `DocumentVoiceToneFallbackT121V07SourceTest` y actualización de `DocumentSidebarContextualT106SourceTest`.
- Documento: `docs/productizacion/T121_V07_FALLBACK_TONOS_DOCUMENTO.md`.

## T121-V06 — Voz local simple mínima

- Estado: implementada sobre T121-V05.
- Alcance: Vista Voces detecta modo **Voz local simple** y muestra superficie mínima sin wizard avanzado, tonos, muestras humanas, clonación ni estilos expresivos.
- UI: sección `Voz local simple`, frase editable, `Probar lectura simple`, `Reproducir última prueba`; gestión de modelo se mantiene en Configuración.
- Backend: `GenerateVoiceTestUseCase` soporta prueba simple auditable en `voices/generated-tests/local-simple/` sin muestra humana.
- Arquitectura: se agrega `VoiceSampleWorkflowCoordinator` para reducir deuda en `DocuPodcastShellViewModel`.
- Regla UX: la interfaz gráfica no debe mostrar nombres técnicos de motores; usa `Voz IA avanzada`, `Voz local simple` y `Modo de prueba`.
- Docs: `docs/productizacion/T121_V06_VOZ_LOCAL_SIMPLE_MINIMA.md`.
- Próxima tanda: T121-V07 — Fallback de tonos faltantes en Documento.

## T121-V05 — Prueba generada con frase editable

- Estado: implementada sobre T121-HF1.
- Alcance: Vista Voces incorpora frase editable, generación de prueba, reproducción de última prueba, resolución de tono exacto/fallback neutral y bloqueo honesto cuando Voz IA avanzada no está lista.
- Backend: `ResolveVoiceToneReferenceUseCase`, `GenerateVoiceTestUseCase`, `VoiceGeneratedTestRequest`, `VoiceGeneratedTestResult`, `VoiceToneReferenceResolution`.
- UI: `VoiceLibraryWorkspaceView` usa `TextArea`, `generatedTestActionLabel`, `Reproducir última prueba` y clases CSS `voice-generated-test-*`.
- Corrección: se actualizan tests antiguos que todavía esperaban nombres técnicos visibles y la aserción de mensaje `lista`.
- Docs: `docs/productizacion/T121_V05_PRUEBA_GENERADA_FRASE_EDITABLE.md`.
- Próxima tanda: T121-V07 — Fallback de tonos faltantes en Documento.

## T121-HF1 — Limpieza de lenguaje visible de motores y visuales

- Estado: implementada sobre T121-V04C.
- Alcance: Inicio, Documento, Configuración, catálogos de motores, mensajes de capacidades, ayuda integrada, rail visual y exportación visible usan `Voz IA avanzada`, `Voz local simple`, `Modo de prueba` y `Visuales`/`Secuencia visual`.
- Regla: `Coqui/XTTS`, `Piper` y `Storyboard` pueden permanecer como identificadores técnicos internos o históricos, pero no como lenguaje principal de UX normal.
- Docs: `docs/productizacion/T121_HF1_LENGUAJE_VISIBLE_MOTORES_VISUALES.md`.
- Próxima tanda: T121-V05 — Prueba generada con frase editable.

## T121-V04 — Wizard de registro de voz avanzada

Se implementa scaffolding de aplicación para el wizard de voces avanzadas: `BuildVoiceRegistrationWizardPlanUseCase`, `VoiceRegistrationWizardPlan`, `VoiceToneRecordingPrompt`, `BuildVoiceToneRecordingPlanUseCase` y `VoiceToneRecordingPlan`. Cada tono usa su frase guía del catálogo teatral; el usuario ve “Voz IA avanzada” y no Coqui/XTTS; cancelar una grabación no reemplaza la muestra anterior.

## T121-V03 — Almacenamiento seguro y descarga de muestras

Implementa backend para resolver, descargar y eliminar muestras de voz gestionadas por DocuPodcast. La descarga copia a carpeta elegida; la eliminación no borra archivos externos originales.

## T121-V02 — Modelo de voces, muestras, tonos teatrales y frases guía

Se agrega la base de dominio para la futura Vista Voces: catálogo de tonos básico y teatral extendido, frases guía por tono, muestras de referencia, propiedad segura de archivos, origen de muestras y perfil de motor visible sin mencionar Coqui/XTTS en UX. Neutral queda como muestra obligatoria para voz avanzada; tonos faltantes podrán caer a neutral con aviso en tandas posteriores.

## T121-V01 — Deshuesamiento de la vista Voces actual

Se limpió la vista Voces para que deje de asignar fragmentos y de mostrar personajes/roles/estilos heredados. Voces queda como biblioteca de voces y muestras; Documento conserva la asignación a fragmentos. En la UX visible se usan “Voz IA avanzada”, “Voz local simple” y “Modo de prueba”, sin mostrar `Coqui` ni `XTTS`.

## T120D-HF1 — Corrección CSS warnings JavaFX

Hotfix visual previa al rediseño de Voces. Agrega aliases CSS en `tokens.css` para tokens usados por ejemplos/configuración (`-dp-text`, `-dp-primary`, `-dp-muted`, `-dp-surface`, `-dp-border`, `-docu-chip-background`) y añade `CssWarningsT120DHf1SourceTest` para evitar regresiones.

## Ajuste planificación Voces — frases guía por tono

Cada tono del catálogo teatral extendido tendrá una frase guía por defecto. El wizard de grabación mostrará la frase correspondiente al tono antes de grabar, para que el usuario actúe la muestra con intención adecuada.

## Planificación T121 — Vista Voces UX/UI final

Se agrega planificación detallada para reconstruir la vista Voces con enfoque en Voz IA avanzada, Piper mínimo, Mock como prueba, catálogo teatral extendido, wizard de grabación/importación de muestras, descarga de muestras, prueba generada con frase editable, fallback a tono neutral y rediseño visual sobrio. Documentación en `docs/productizacion/voces_ux_final/`.

# Registro T120C — Aplicación del Ribbon final

- Estado: implementada sobre T120B verde validada localmente.
- Alcance: aplica etiquetas finales del Ribbon, retira Storyboard de superficies Ribbon, ajusta Preparar lectura/Cancelar generación y trata Voces como biblioteca/gestión, no personajes.
- Reglas: Ribbon por intención de usuario; Documento y Voces son vistas principales desde Vista; panel visual pertenece al Documento; no reintroducir Guion, Whisper/STT ni Storyboard como módulo.
- Docs: `docs/productizacion/T120C_APLICACION_RIBBON_FINAL.md`.
- Próxima tanda: T121 — Auditoría UX/UI Voces contigo.

---

# Registro TI2 — Audio jobs desde RenderPlan

- Estado: implementada sobre TI1 verde.
- Alcance: `AudioGenerationRequest` acepta `RenderUnitPlan`; `AudioGenerationUnit` representa la unidad efectiva de TTS; mock y TTS real consumen `request.generationUnits()`; `DocuPodcastShellViewModel` construye `NarrationRenderPlan → RenderUnitPlan → AudioGenerationRequest` antes de enviar/reanudar audio.
- Reglas: visual silencioso no genera audio; audio externo no se regenera por TTS; modo intermedio legacy por segmento se conserva.
- Docs: `docs/productizacion/TI2_AUDIO_JOBS_DESDE_RENDERPLAN.md`, `docs/174_TI2_AUDIO_JOBS_DESDE_RENDERPLAN.md`.
- Próxima tanda: TI3 — Storyboard/video desde RenderPlan.

---


T104 consolida Documento como hoja de lectura tipo escritorio y corrige el Ribbon tras prueba visual: mas respirable, mas ancho y con altura suficiente para iconografia futura. No cambia el cerebro ni implementa playbar final; prepara T105.

Documentos clave:

```text
docs/productizacion/T104_DOCUMENTO_LIMPIO_LECTOR_WORD.md
docs/155_TANDA_104_DOCUMENTO_LIMPIO_LECTOR_WORD.md
```

Siguiente paso recomendado: T105 - Playbar flotante final.

# Registro T103 - Inicio propagandistico moderno

- Fecha: 2026-06-02.
- Base: T102 verde validada localmente.
- Alcance: rediseña Inicio como portada moderna de producto con hero, tarjetas de flujo, alcance V1 y recientes como empty state.
- Reglas: sin Configuracion, sin Whisper/STT, sin Jobs/manifests, sin botones hardcodeados y sin capacidades nuevas.
- Docs: `docs/productizacion/T103_INICIO_PROPAGANDISTICO_MODERNO.md`, `docs/153_TANDA_103_INICIO_PROPAGANDISTICO_MODERNO.md`.
- Siguiente: T104 - Documento limpio tipo lector Word.

# Registro T102 - StatusBar + ReadingZoomControl

- Fecha: 2026-06-02.
- Base: T101 verde validada localmente.
- Alcance: agrega control inferior derecho de tamano de lectura en StatusBar.
- Superficie: StatusBar inferior; no Ribbon/MenuBar/Playbar.
- Reglas: cambia fuente/reflujo de lectura, no zoom de canvas; persiste `OperationalSettings.readingDocument.baseFontSize`; usa clases CSS `document-reader-size-*` sin estilos inline.
- Docs: `docs/productizacion/T102_STATUSBAR_READING_ZOOM.md`, `docs/152_TANDA_102_STATUSBAR_READING_ZOOM.md`.
- Siguiente: T103 - Inicio propagandistico moderno.

# Registro T101 - Ribbon base real

- Fecha: 2026-06-02.
- Base: T100 verde validada localmente.
- Alcance: reemplaza la toolbar superior transicional por `RibbonView` con pestanas y grupos de comandos.
- Pestanas: Inicio, Lectura, Storyboard, Vista y Exportar.
- Reglas: comandos por `AppCommandId`; sin workspaces heredados; sin Whisper/STT visible; playbar sigue siendo dueno futuro de la escucha.
- Docs: `docs/productizacion/T101_RIBBON_BASE_REAL.md`, `docs/151_TANDA_101_RIBBON_BASE_REAL.md`.
- Siguiente: T102 - StatusBar + ReadingZoomControl.

## T100 — MenuBar final

- Estado: implementada.
- Alcance: cierra el MenuBar como superficie final antes del Ribbon; agrega comandos reales para ubicacion de fuente documental, carpeta de exportaciones y mostrar/ocultar rail derecho.
- Documentos:
  - `docs/productizacion/T100_MENUBAR_FINAL.md`
  - `docs/150_TANDA_100_MENUBAR_FINAL.md`
- Proxima tanda: T101 — Ribbon base real.

## T99B — Deshuesadero de acciones duplicadas

- Estado: implementada.
- Alcance: conecta Shell/MenuBar/Toolbar/Inicio al catálogo único de comandos. `DocuPodcastShellView` registra `AppCommandDispatcher`; el MenuBar usa `commandItem(AppCommandId...)`; `MainToolbarView` despacha `action.commandId()` en vez de duplicar handlers por capability.
- Documentos:
  - `docs/productizacion/T99B_DESHUESADERO_ACCIONES_DUPLICADAS.md`
  - `docs/145_TANDA_99B_DESHUESADERO_ACCIONES_DUPLICADAS.md`
- Próxima tanda: T99C — Deshuesadero visual mínimo.

## T99A — Deshuesadero GUI y roadmap de implementación completo

- Tipo: implementación GUI preparatoria + documentación de continuidad.
- Alcance: Inicio, Documento y Voces quedan como workspaces reales. Guion, Audio Jobs y Storyboard dejan de presentarse como vistas normales; rutas antiguas vuelven a Documento. Se documentan todas las tandas planificadas T99B–T112.
- Documentos:
  - `docs/productizacion/T99A_DESHUESADERO_VISTAS_NAVEGACION.md`
  - `docs/productizacion/T99_GUI_ROADMAP_IMPLEMENTACION_DETALLADO.md`
  - `docs/144_TANDA_99A_DESHUESADERO_GUI_Y_ROADMAP.md`
- Próxima tanda: T99B — Deshuesadero de acciones duplicadas.

## T98 — Contrato GUI completo documentado

- Tipo: documentación de producto/frontend.
- Alcance: consolida MenuBar, Ribbon, vistas/workspaces, sidebar izquierdo, rail derecho, playbar flotante, configuración/guía/diálogos y roadmap de implementación GUI.
- Documentos:
  - `docs/productizacion/T98_CONTRATO_GUI_GLOBAL.md`
  - `docs/productizacion/T98A_CONTRATO_MENUBAR.md`
  - `docs/productizacion/T98B_CONTRATO_VISTAS_WORKSPACES.md`
  - `docs/productizacion/T98C_CONTRATO_RIBBON.md`
  - `docs/productizacion/T98D_CONTRATO_SIDEBAR_RAIL.md`
  - `docs/productizacion/T98E_CONTRATO_PLAYBAR_FLOTANTE.md`
  - `docs/productizacion/T98F_CONTRATO_CONFIGURACION_GUIA_DIALOGOS.md`
  - `docs/productizacion/T98G_ROADMAP_IMPLEMENTACION_GUI.md`
  - `docs/143_TANDA_98_CONTRATO_GUI_COMPLETO.md`
- No implementa GUI final; fija contrato para próximas tandas.


## T97B — Hotfix tests y principio de automatización UI

- Corrige tests locales post T97: formato JSON esperado en v2 y limpieza de etiqueta visible `Audio a texto` en T90G.
- Documenta el principio de automatizar pasos previos obvios con confirmación humana antes del contrato GUI T98.
- Documento: `docs/productizacion/T97B_HOTFIX_TESTS_Y_PRINCIPIO_AUTOMATIZACION_UI.md`.

# Registro — Tanda 90

T90 agrega smoke real opt-in de motores locales: Coqui/XTTS, Piper y FFmpeg. La suite normal no depende de modelos locales; `scripts\19-smoke-motores-reales.bat` activa `RealEnginesSmokeScenarioTest` y genera evidencia en `target/docupodcast-real-engines-smoke/`. No es RC final y no reintroduce audio a texto.

---

# Registro — Tanda 81G

T81G corrige el guardarraíl de T81F para `ToolbarActionButton` y degrada las vistas secundarias. Inicio y Documento quedan como superficies primarias. Guion, Audio, Voces y Storyboard completo continúan implementados como herramientas avanzadas. La restauración de un proyecto con vista avanzada activa vuelve a Documento.

---

# Registro de tandas — DocuPodcast Studio

Este registro resume la evolución acumulada. Los detalles completos viven en los archivos `docs/*TANDA*`, `DOCUMENTACION/*`, `00_MEMORIA_PROYECTO/*` y `DOCUMENTACION_ESTRATEGICA/*`.

## Base inicial y primeras tandas

| Tanda | Enfoque |
|---|---|
| 1 | Scaffolding inicial de app JavaFX local inspirado en DMS/Fractal. |
| 2 | Estructura base de proyecto, servicios y primeras pantallas. |
| 3–4 | Importación DOCX y workspace Documento inicial. |
| 5–7D | Guion narrable, flujo Word a guion, persistencia inicial y UX base. |
| 8–14 | Maduración de workspaces, recursos, persistencia, audio/storyboard inicial y validaciones. |
| 15–16C | Scripts, packaging inicial, hotfixes Windows, toolbar, ventana y rutas con paréntesis. |
| 17–24 | Guardarraíles de arquitectura, rehidratación, assets, capacidades, diálogos, recursos IA e importador Markdown. |
| 25–29 | Editor de guion, cola de audio, TTS local robusto, storyboard visual y playback sincronizado. |
| 30–34 | Biblioteca de voces madura, grabación, STT/Whisper, bundle auditable y podcast WAV final real. |

## Tandas recientes de alineación producto/UX

| Tanda | Estado |
|---|---|
| 34B | Hotfix de compilación por accessor incorrecto. |
| 34C | Build verde de Tanda 34 con tests fuente alineados. |
| 35A | Documento narrado como brújula de producto. |
| 35A-B | Hotfix verde por guardarraíl de Markdown opcional. |
| 35B | Documento como operación principal con `Escuchar documento`. |
| 36 | Lectura cómoda y configuración técnica separada. |
| 37 | Seguimiento visual del bloque/oración activa tipo MuseScore. |
| 38 | Kit GUI transversal y CSS modular. |
| 38B | Migración a `com.marcosmoreiradev` y configuración de voz/muestras. |
| 39 | Reproducción por chunks/prebuffer tipo YouTube. |
| 40 | Botón inteligente `Escuchar documento` / `Reproducir desde aquí`. |
| 41 | Selección de capas y mini rail plegable. |
| 42 | Storyboard simple integrado con miniaturas en rail. |
| 43 | Configuración guiada de motores TTS/STT/Whisper. |
| 44 | Asistente de instalación/importación/verificación de modelos. |
| 45 | Selección exacta por oración y rangos parciales. |
| 46 | Persistencia/round-trip de asignaciones en `narrativeLayers`. |
| 47 | Exportación de video simple como paquete/plan auditable. |
| 47B | Hotfix verde y criterio de documento unificado. |
| 48 | Documentación maestra de continuidad, UX y pendientes. |

## Estado conceptual tras Tanda 48

La arquitectura debe seguir protegiendo estas ideas:

```text
Documento importado = documento de trabajo, sea técnico o teatral.
Capas de producción = asignaciones del proyecto, no texto insertado en Word.
Pantalla principal = leer/escuchar/asignar de forma simple.
Configuración = área de ajustes técnicos.
Componentes GUI = transversales y moderados.
CSS = modular y sin archivos gigantes.
```

## Tanda 49 — Plan maestro FFmpeg/scaffolding

Tipo: documental / planificación protegida.

Se agregan documentos de continuidad para cerrar el producto v1 sin perder contexto:

```text
docs/productizacion/PLAN_MAESTRO_TANDAS_49_63.md
docs/productizacion/CONTRATO_FFMPEG_EMBEBIDO_VIDEO_2K.md
docs/productizacion/AUDITORIA_SCAFFOLDING_VISUAL_OPERATIVO.md
docs/productizacion/MATRIZ_TESTS_TANDAS_RESTANTES.md
```

Decisiones registradas:

```text
video simple es capacidad del producto cuando hay storyboard/imágenes;
resolución por defecto mínimo 2K;
FFmpeg debe preferirse embebido/local en tools/ffmpeg;
la pantalla principal no debe exponer la área de ajustes técnicos;
antes de modernizar se debe auditar scaffolding visual;
toda tanda futura debe traer tests.
```

## Tanda 50 — Modernización visual base

La Tanda 50 fija una base visual moderna: tokens tipo Teams formal, toolbar plana, shell sobrio, documento como página cómoda y componentes transversales modernizados. No agrega funcionalidades nuevas; prepara la experiencia para que DocuPodcast deje de parecer JavaFX crudo/Windows XP y avance hacia un producto usable por usuarios no técnicos. Agrega guardarraíles `ModernVisualThemeSourceTest`, `CssTokenCoverageSourceTest`, `NoExcessiveNestedPanelsSourceTest` y `NoLegacyXpLookSourceTest`.

## Tanda 51

| 51 | Documento como pantalla operativa real: SideDock técnico plegado por defecto, lectura primero y corrección de tests tras CSS modular. |


## Tanda 52 — Flujo Word → escuchar completo

La Tanda 52 agrega estado visible del flujo Word → escuchar dentro de Documento. El usuario ve si falta abrir Word, preparar guion, guardar proyecto, generar audio o reproducir. La pantalla mantiene Guion/Audio/Jobs como fábrica interna y habla en lenguaje de usuario.


## Tanda 55 — Motor de voz usable

- Estado: implementada en esta entrega.
- Aporta: `VoiceEngineUsabilityPolicy`, `VoiceEngineOption`, `VoiceEngineControl`, `VoiceSynthesisSettings`, tests y configuración de voz más clara.
- No aporta todavía: motor XTTS/Piper real conectado ni descarga de modelos.


## Tanda 56 — Asistente real de modelos

Agrega contratos de carpeta local, verificación de archivos mínimos y reconocimiento de checksum para XTTS/Coqui, Piper y Whisper. Mantiene importación manual obligatoria y evita depender de URLs fijas. Tests: `InspectLocalModelFolderUseCaseTest`, `RealModelAssistantSourceTest`.

## Tanda 57 — Streaming/prebuffer robusto

- Estado: implementada en esta base.
- Contrato: 5 fragmentos iniciales, 10 de lookahead, espera visible y continuidad automática si falta audio.
- Tests: `StreamingPlaybackWindowTest`, `StreamingPlaybackRobustnessSourceTest`.

## Tanda 58 — Video simple 2K + FFmpeg embebido

- Video simple es capacidad del producto cuando el usuario usa storyboard/imágenes.
- Resolución predeterminada: 2K (2560x1440); opciones: 720p, 1080p, 2K y 4K.
- FFmpeg se busca primero como herramienta embebida en `tools/ffmpeg/bin/ffmpeg.exe` y no debe exigir PATH global.
- Durante render, la pantalla operativa entra en modo render con progreso y bloqueo temporal de lectura/edición/nuevas exportaciones.

## Tanda 58B — Hotfix build verde buffer/video

- Estado: hotfix posterior a Tanda 58.
- Corrige: mensaje de espera de buffer duplicado entre shell y política.
- Corrige: contrato de video simple para no prometer MP4 final real cuando todavía se exporta paquete renderizable/auditable.
- Tests alineados: `StreamingPlaybackBufferSourceTest`, `SimpleVideoExportSourceTest`.
- Siguiente paso recomendado: smoke exploratorio mínimo antes de rediseñar scaffolding o refactorizar.


## Tanda 58C — Smoke exploratorio mínimo

- Estado: documental/operativa previa a rediseño y refactor.
- Base: T58B validada localmente con todos los tests pasando.
- Aporta: protocolo de smoke manual, checklist, reporte, script de orientación y DOCX de muestra.
- Objetivo: probar abrir Word, escuchar, guardar, cerrar y reabrir antes de cambiar scaffolding.
- No cambia código productivo ni promete RC.
- Siguiente paso: decidir entre corregir funcionalidad, rediseñar scaffolding o pasar a auditoría GUI según resultado del smoke.


## Tanda 59 — Principios rectores de rediseño y scaffolding

- Fija la brújula de rediseño previa a T59A/T59B/T60.
- Declara Documento primero, menos scaffolding visible, complejidad progresiva, acciones por intención y componentes GUI transversales.
- Agrega `PRINCIPIOS_RECTORES_DISENO_SCAFFOLDING.md` y `ROADMAP_POST_T59_REDISSENO_GUIADO.md`.
- Agrega guardarraíles fuente de principios y aliases CSS.
- Incorpora aliases de compatibilidad para tokens CSS detectados como huérfanos durante smoke exploratorio.

## Tanda 59A — Componentes GUI transversales

- Estado: implementada como primera ejecución de componentes transversales.
- Aporta: `ActionButtonFactory`, `ActionBar`, `TransportControls`, `RailActionRow`, `MetricBadge`, `InfoBadge` y `DiagnosticCard`.
- Riesgo detectado: al aplicar componentes sobre vistas concretas, un source test de Guion quedó acoplado a sintaxis antigua.

## Tanda 59A-B — Hotfix build verde Guion/playback

- Estado: hotfix de T59A.
- Corrige: `ScriptWorkspaceRecordingPlaybackSourceTest` acepta la conexión válida `viewModel::playFromSelectedSegment` posterior a la migración a `ActionButtonFactory`.
- No cambia comportamiento productivo.
- Base local confirmada verde por el usuario.

## Tanda 59B — Catálogo transversal y mapa del cerebro

- Estado: documental/arquitectura protegida.
- Fija que el catálogo de componentes transversales no equivale a “botones bonitos”, sino a contrato semántico, API, estilo común y gobernanza.
- Fija el mapa del cerebro de la app: dominio, casos de uso, coordinadores, servicios, repositorios, jobs, audio, playback, video, settings, capability policies y validation policies.
- Aporta `CATALOGO_COMPONENTES_TRANSVERSALES.md`, `MAPA_CEREBRO_APP.md`, `AUDITORIA_CEREBRO_PRIORITARIA.md` y `ROADMAP_POST_T59B_CEREBRO_COMPONENTES.md`.
- Próximo paso recomendado: T60 — auditoría ejecutable del cerebro antes de refactor y antes de rediseño visual aplicado.


## Tanda 59C — Contrato documentos fuente solo lectura

- Estado: documental/arquitectura protegida.
- Fija que Word/DOCX, PDF, Markdown/MD y TXT se abren como documentos fuente en modo solo lectura para V1.
- Declara que Documento narrable, proyecciones de narración, capas, audio, storyboard, transcripciones y metadatos viven como artefactos del proyecto DocuPodcast.
- Archiva una referencia visual Word/WPS-like para rediseño futuro, sin convertir DocuPodcast en procesador de texto completo.
- Próximo paso recomendado: T60 — auditoría ejecutable del cerebro bajo la regla de fuente inmutable.


## Tanda 60 — Auditoría cerebro y Documento narrable raíz

- Fija Documento narrable como objeto padre V1.
- Recontrata el guion como proyección interna/avanzada.
- Mantiene storyboard/video como capa opcional asociada a texto hablado.

## Tanda 60B — Hotfix verde + Refrescar contenido

- Corrige source tests documentales.
- Agrega contrato de refresco de fuente solo lectura.
- Declara que audio/capas/storyboard deben marcarse como vigentes, obsoletos o en revisión.

## Tanda 61 — Auditoría ejecutable del cerebro y refresco de fuente

- Agrega snapshots, reportes de cambio y caso de uso `RefreshSourceDocumentUseCase`.
- La fuente externa sigue siendo solo lectura.
- Prepara la acción real `Refrescar contenido`.

## Tanda 62 — Refactor prioritario de coordinadores del cerebro

- Agrega `DocumentIntakeCoordinator`, `SourceDocumentRefreshCoordinator` y `WorkspaceNavigationCoordinator`.
- Expone `Refrescar contenido` como acción secundaria en Documento usando componente transversal.
- La navegación de workspace deja de marcar contenido como sucio por sí sola.
- Siguiente foco: narración/audio/playback desde Documento.


## Tanda 62B — Hotfix build verde coordinadores

Se alinea `WorkspaceNavigationCoordinator` con el source test que exige declarar explícitamente que navegar no marca dirty.

## Tanda 63 — Refactor narración/playback desde Documento

Se agregan `DocumentNarrationCoordinator` y `PlaybackWorkflowCoordinator`; el ViewModel delega creación de proyección interna de narración y reglas de buffer/playback.

## Tanda 70 — DocumentSource unificado y PDF con texto nativo

- Unifica el intake documental V1 para DOCX, PDF con texto nativo, Markdown/MD y TXT.
- Agrega `PdfDocumentImporter` y rechaza PDF escaneado/imagen con aviso de producto, sin intentar OCR.
- Mantiene todos los documentos fuente como solo lectura y conserva los artefactos en el proyecto DocuPodcast.

## T77 — Integridad y reparación del proyecto

Agrega `InspectProjectIntegrityUseCase`, `ProjectIntegrityReport`, `ProjectIntegrityIssue`, `ProjectIntegrityStatus` y `ProjectIntegritySeverity`. El cerebro diferencia `OK`, `CON_ADVERTENCIAS` y `REQUIERE_REPARACION` para carpeta, assets, checksums, materializados, capas, storyboard y audio jobs. Tests: `ProjectIntegrityReportTest`, `InspectProjectIntegrityUseCaseTest`, `ProjectIntegrityRepairSourceTest`.

## Tanda 80 — Congelación del cerebro V1

- Estado: implementada sobre T80C verde.
- Aporta: `BrainV1CapabilityMatrix`, `BrainV1Capability`, `BrainV1CapabilityArea` y `BrainV1CapabilityStatus`.
- Congela capacidades V1: documento narrable, escucha, playback, capas reales, media flexible, storyboard/video simple, integridad, export readiness, configuración operativa, CPU/GPU y smoke automático.
- Congela diferidos V2: OCR, editor Word completo, editor de video avanzado y nube/colaboración.
- Regla post T80: no seguir agregando cerebro antes del RC salvo hotfix; pasar a Lectura 15, T81 y T82.


## Tanda 81A — Navegación y superficies frontend

- Reorganiza menu bar común: Archivo, Editar, Ver, Documento, Lectura, Herramientas, Exportar, Configuración, Ayuda.
- Documento se refiere al documento fuente, no al proyecto.
- Agrega Archivo → Abrir carpeta del proyecto y Ver → Pantalla completa.
- Inicia la pantalla de bienvenida como superficie de producto.

## Tanda 81B — Documento limpio y Configuración solo desde menú

- Corrige T81A: Configuración ya no aparece como tarjeta de bienvenida ni como acceso fuera del menu bar.
- `SettingsDialog` elimina lenguaje metafórico visible y usa “Ajustes operativos persistentes”.
- `DocumentWorkspaceView` deja de imprimir metadatos técnicos debajo de cada bloque.
- `DocumentPropertiesPanel` conserva metadatos para diagnóstico.
- Próximo paso: T81C — barra flotante de lectura global.

## Tanda 81C — Barra flotante de lectura global

- Corrige T81B: `VoiceSampleImportUiSourceTest` deja de exigir que la importación de muestra de voz reabra el menú principal `Voz` o navegación técnica. La capacidad sigue disponible en el cerebro y en superficies avanzadas.
- Agrega `FloatingReadingControlBar` como componente GUI transversal para lectura global.
- `DocumentWorkspaceView` instala la barra con `documentSurface.setTop(floatingReadingControl())`.
- La barra compone `PrimaryActionStrip`, `TransportControls` y `ActionButtonFactory.secondary`, evitando botoneras hardcodeadas en el workspace.
- La hoja central sigue limpia: lectura global arriba, contenido en el centro, acciones por fragmento pendientes para T81D.
- Próximo paso: T81D — Sidebar izquierdo contextual con Detalles, Audio/Narración e Imagen.

## Tanda 81D — Sidebar izquierdo contextual

- Agrega el inspector contextual de Documento con módulos `Detalles`, `Audio / Narración` e `Imagen`.
- `Audio / Narración` usa origen `Voz IA` o `Audio del computador`.
- `Audio del computador` ofrece acciones directas `Elegir audio…` y `Extraer audio de video…`.
- Emoción/estilo solo se muestra con `Voz IA`.
- `DocumentMediaRailView` deja de incluir acciones de asignación y queda como rail visual/navegacional.
- Agrega remoción por tipo de capa para imagen y emoción.
- Próximo paso: T81E — sidebar derecho de miniaturas / medios.

## Tanda 81D-HF1 — Hotfix build verde sidebar contextual

Corrige seis fallos de source tests/documentación detectados localmente tras T81D. No cambia el cerebro V1 ni revierte el inspector contextual. Siguiente: T81E sidebar derecho de miniaturas / medios.

## Tanda 81E — Sidebar derecho de miniaturas / medios

- Convierte `DocumentMediaRailView` en rail visual/navegacional: medios asignados, mini storyboard e imágenes disponibles.
- Agrega `MediaThumbnailCard` como componente transversal para tarjeta con miniatura, fragmento relacionado y descripción breve.
- El rail derecho no contiene acciones de asignación; esas viven en el inspector izquierdo contextual.
- Compacta la barra flotante: el hint de la acción principal pasa a tooltip.
- Próximo paso: T81F — Toolbar con iconos y grupos.

## Tanda 81E-HF1 — Hotfix build verde rail derecho

- Corrige cuatro fallos reportados localmente tras T81E.
- Mantiene el rail derecho como navegación visual de miniaturas/medios.
- Preserva el inspector izquierdo como lugar de acciones frecuentes por fragmento.
- Alinea source tests históricos con los literales `inspector izquierdo`, `Capas activas`, `Medios asignados`, `assignedMediaCard` y `layerCard`.
- Próximo paso: T81F — Toolbar con iconos y grupos.

## Tanda 81F — Toolbar con iconos y grupos

- Agrega `ToolbarActionButton` como componente transversal para acciones con icono, texto corto y tooltip.
- La toolbar global se reorganiza en grupos `Documento`, `Lectura`, `Salida` y `Vista`.
- Retira de la fila global botones permanentes de vistas técnicas: `Narración avanzada`, `Voces`, `Audio`, `Storyboard`.
- Mantiene el cerebro y las capacidades avanzadas; solo cambia la superficie visible.
- Próximo paso: T81G — retiro o degradación de vistas secundarias redundantes.


## Tanda 82 — Handoff de continuidad, referencias visuales y plan IA real

- Conserva capturas usadas durante la planificación en `docs/referencias/capturas-chat/`.
- Agrega índice visual con explicaciones de DocuPodcast, Xournal++, WPS Office, Blender y retroalimentación T81.
- Consolida notas de conversación en `NOTAS_CONVERSACION_T81_T82.md`.
- Documenta el plan exacto para integrar TTS/STT real en `PLAN_EXACTO_IA_TTS_STT_REAL.md`.
- Agrega handoff para futuras sesiones y roadmap post T82 hacia IA real y RC.
- Declara explícitamente que T82 no es RC final: falta cerrar motores reales, FFmpeg operativo, smoke con modelos reales y packaging.


## Tanda 83 — Plan cerrado de motores IA reales

Tanda documental/contractual. Cierra la ruta oficial de motores: Piper, XTTS/Coqui wrapper, whisper.cpp y FFmpeg. No agrega nuevos motores ni cambia UI productiva. Su objetivo es evitar dispersión antes de implementar preflight real y la primera demo con voz/transcripción reales.

## Tanda 84 — Preflight operativo de motores IA con Coqui obligatorio

- Agrega `application.engines` con `InspectAiEnginesPreflightUseCase`, `AiEnginePreflightReport` y estados de readiness.
- Coqui XTTS queda como motor obligatorio de calidad alta para el producto objetivo.
- Piper queda como motor intermedio/liviano para demo rápida y modo intermedio, sin reemplazar Coqui.
- whisper.cpp queda como STT local oficial.
- FFmpeg queda como runtime de media para extraer audio desde video y preparar video simple.
- Se documenta que los modelos pesados no se incrustan dentro de `.docupodcast`; se verifican en `models/` y `tools/`.
- Próximo paso: T85 — Piper real end-to-end o T86 — wrapper Coqui real, según prioridad operativa.

## Tanda 85 — Piper TTS real end-to-end

- Agrega `PiperTtsCommandTemplate`.
- Agrega `scripts/tts/piper-file-to-wav.ps1`.
- Permite que `engineMode=piper` derive un comando real si existe `tools/piper/piper.exe` y una voz `.onnx` en `models/tts/piper/voices/`.
- Corrige el test del botón inteligente de lectura para el contrato toolbar/barra flotante.
- Mantiene Coqui XTTS como motor obligatorio de calidad alta para la siguiente tanda.

## T86 — Coqui XTTS wrapper y voz por defecto

- Agrega `XttsTtsCommandTemplate` para derivar comando cuando `engineMode=xtts` o `coqui`.
- Agrega wrapper PowerShell `scripts/tts/xtts-file-to-wav.ps1`.
- Agrega wrapper Python `tools/xtts-wrapper/synthesize_xtts.py`.
- Conserva la fuente `samples/voices/default/source/voz-por-defecto.mp4`.
- Agrega muestra normalizada `models/tts/xtts/speakers/voz-por-defecto.wav`.
- Corrige handoff para `AiVoiceTranscriptionRoadmapSourceTest` con `T83` y `Release Candidate real`.


## Tanda 87 — whisper.cpp STT real

- Corrige el preflight completo de motores para incluir la muestra `voz-por-defecto.wav` requerida por Coqui/XTTS.
- Agrega `scripts/stt/whisper-file-to-text.ps1`.
- `WhisperCppSpeechToTextGateway` soporta scripts `.cmd`/`.bat` en Windows mediante `cmd.exe /c`.
- Agrega test end-to-end con `whisper.cpp` falso para validar transcript, logs y rutas dentro del proyecto.
- Documenta que el usuario no planea usar modelos/voces como mercancía y que cualquier redistribución futura requiere revisión de licencia.
- Próximo paso: T88 — FFmpeg real para extracción y normalización de media.

## Tanda 88C — limpieza de alcance visible de motores y GUI

Corrige el alcance antes de T89: Whisper/STT deja de pertenecer al producto visible DocuPodcast. La UI, Configuración, toolbar y catálogos guiados se limitan a Coqui/XTTS, Piper y FFmpeg. `Audio del computador` queda definido como clip genérico elegido por el usuario. Se agregan guardarraíles para evitar botones/promesas no implementadas.


## T90B — Onboarding Python local para Coqui/XTTS

- Agrega preparación repo-local de Python para Coqui/XTTS.
- Agrega scripts `20-preparar-python-portable-coqui.bat`, `21-probar-coqui-xtts.bat` y `22-verificar-coqui-xtts-local.bat`.
- Mantiene Java como núcleo de la app y Python como wrapper aislado para síntesis WAV.
- No reintroduce Whisper/STT como producto visible.

## T90C–T90E — Python embebido obligatorio, onboarding local y preflight de arranque

- T90C: elimina modo intermedio a Python global para Coqui/XTTS.
- T90D: consolida onboarding Coqui/XTTS repo-local.
- T90E: agrega preflight de arranque de motores.
- Documentos: `docs/productizacion/T90C_PYTHON_EMBEBIDO_OBLIGATORIO.md`, `T90D_ONBOARDING_COQUI_XTTS_LOCAL.md`, `T90E_PREFLIGHT_ARRANQUE_MOTORES.md`.

## T91 — Render narrativo por unidades

Se agrega `NarrationRenderPlan`, `NarrationRenderUnit` y `BuildNarrationRenderPlanUseCase` para representar oraciones/rangos como unidades efectivas de narración con voz, estilo, audio clip e imagen. `PlaybackCue` incorpora `unitId` y `PlaybackManifest` permite múltiples cues por segmento si las unidades son distintas. T91 prepara T92, pero no reemplaza todavía jobs TTS por segmento.


## T93 — TextAnchor mínimo + migración controlada

T93 sube `.docupodcast.json` a `formatVersion = 2` y agrega `TextAnchor` para capas narrativas. El reader sigue aceptando v1 y migra rangos documentales legacy a anclas `LOW/NEEDS_REVIEW`. El writer conserva `sourceBlockId/sourceStartOffset/sourceEndOffset` y agrega objeto `textAnchor` para preparar reconciliación granular futura. No modifica el documento fuente.

- **T95 — Catálogo único de comandos:** agrega `presentation.command`, `AppCommandId`, registro oficial, dispatcher base y mapeo desde `WorkspaceCapability` para preparar menú/ribbon/sidebar sin duplicar lógica.


## T96 — Limpieza de placeholders y acciones visibles falsas

T96 limpia menú y toolbar antes de pisar fuerte la GUI. Se retiran acciones visibles que solo llamaban a `showPlaceholder(...)` y se oculta `Perfil de lectura` del toolbar contextual hasta tener flujo real. La regla queda: si aparece, funciona; si no funciona, se oculta.

## T97 — Componentes GUI transversales

T97 congela el inventario de componentes GUI antes del rediseño fuerte. Agrega `GuiComponentCatalog`, contratos de superficie/estado y componentes base `RibbonButton`, `RibbonGroup`, `SidebarIconTab` y `RailToggleButton`. Documenta que las futuras tandas GUI deben reutilizar componentes compartidos, evitar botones hardcodeados y no construir sobre placeholders o decisiones alucinadas.
## T106A — Sidebar contextual ajuste fino

- Base: T106.
- Corrige tests fuente/documentacion historica reportados por diagnostico local.
- Ajusta Sidebar izquierdo: Fragmento sin botones redundantes, Audio con combos, Imagen con preview.
- Hace que los scrolls de sidebars ocupen toda la altura disponible.
- Siguiente: T107 — Rail derecho retractil/redimensionable.


## T106B — Sidebar imagen/audio refinado

- Base: T106A.
- Corrige logs y alinea guardarraíles de deuda del ViewModel.
- Agrega `Reproducir fragmento` para reproducción limitada al fragmento seleccionado.
- Guía con mensaje modal cuando se intenta importar imagen sin guardar proyecto.
- Refresca preview y rail visual mediante `documentMediaRevisionProperty`.
- Quitar imagen elimina capa y elimina asset/archivo si no se comparte con otros fragmentos.
- Amplía estilos base de voz IA: Cálido, Feliz, Alegre, Triste y Calmado además de Neutro/Serio/Dramático.
- Deuda explícita: extraer `DocumentMediaWorkflowCoordinator` antes de seguir acumulando lógica en `DocuPodcastShellViewModel`.

## T107 — Rail derecho retráctil/redimensionable

- Base: T106B.
- Objetivo: cerrar rail derecho plegable, redimensionable y estrictamente visual.
- Cambios: `CollapsibleMediaRail` redimensionable; acción para borrar imágenes del proyecto; acceso a biblioteca de voces desde Audio; guardado guiado antes de importar imágenes.
- Documentación: `docs/productizacion/T107_RAIL_DERECHO_RETRACTIL_REDIMENSIONABLE.md` y `docs/161_TANDA_107_RAIL_DERECHO_RETRACTIL_REDIMENSIONABLE.md`.

## T109 — Overlay de procesos largos

- Base: T107.
- Objetivo: reemplazar Audio Jobs como destino visible por un overlay de progreso y ajustar rail derecho segun feedback.
- Cambios: rail sin `Asignadas`; clic en tarjetas visuales actualiza tambien sidebar izquierdo; `LongProcessOverlayView` se monta sobre el workspace; flujos de audio dejan de navegar a `AUDIO_JOBS`.
- Documentación: `docs/productizacion/T109_OVERLAY_PROCESOS_LARGOS_IMPLEMENTADO.md` y `docs/162_TANDA_109_OVERLAY_PROCESOS_LARGOS.md`.
- Siguiente recomendada: T108 — Workspace Voces final.


## T110R — Reconstrucción de T110 sobre T109

- Base: T109 física, porque la T110 implementada en ventana anterior no quedó disponible como ZIP.
- Cambios: guía renderizada sin Markdown crudo, topics visibles sin STT/Whisper, soporte inicial de imágenes embebidas DOCX, configuración `silentVisualBlockSeconds` = 5s, guardado guiado antes de escuchar/generar/reproducir, playbar conectada a jobs activos y documentación de bloque visual fuente ≠ storyboard.
- Documentación: `docs/productizacion/T110R_RECONSTRUCCION_CONFIGURACION_GUIA_DIALOGOS.md` y `docs/163_TANDA_110R_RECONSTRUCCION_CONFIGURACION_GUIA_DIALOGOS.md`.
- Siguiente recomendada: T108 — Workspace Voces final mínimo con criterio UX/UI.

## T110R-HF1 — Hotfix de compilación GuideDialog

- Base: T110R.
- Motivo: diagnóstico local `20260602-122634` fallaba en `mvn compile` por literales Java mal escapados en `GuideDialog.java`.
- Cambios: se corrige regex de separación de líneas, salto de línea en bloque de código y regex de listas numeradas.
- Alcance: hotfix mínimo de compilación; no cambia contratos funcionales de T110R.
- Documentación: `docs/productizacion/T110R_HF1_GUIDE_DIALOG_ESCAPES.md` y `docs/164_TANDA_110R_HF1_GUIDE_DIALOG_ESCAPES.md`.
- Siguiente: reejecutar `scripts\\99-diagnostico-completo.bat`.

## T108 — Workspace Voces final mínimo con criterio UX/UI

- Base: T110R-HF1.
- Incluye hotfix focal de 3 fallos Maven reportados en `20260602-125428`.
- Reorganiza `VoiceLibraryWorkspaceView` como biblioteca usable, con lista de voces, detalle seleccionado, muestra/importación/grabación y frase sugerida.
- Mantiene honestidad: muestra propia/autorizada no implica clonación real ni síntesis si el motor no lo soporta.
- Siguiente recomendada: T111 — Iconografía/CSS final.

## T111 — Iconografía/CSS final

- Base: T108 verde.
- Cambios: introduce catálogo semántico de iconografía, `SourceVisualBlockView`, CSS modular de visuales fuente y guardado guiado desde playbar antes de preparar/reproducir audio.
- Siguiente: smoke visual y correcciones de iconografía/imagen según pruebas locales.

## T111-HF2/HF3 — Iconos PNG y corrección de compilación

- Base: T111.
- Cambios: se migra la iconografía a PNG bajo `src/main/resources/icons/ui/`, se agrega `AppIcon`/`IconView`, se conectan ribbon/sidebar/rail y se corrige `WelcomeWorkspaceView` para usar `IconView` en vez de `Label` con `AppIcon`.

## T111-HF4 — Overlay compacto, iconos grandes e imagen DOCX persistida

- Base: T111-HF3.
- Cambios: overlay de procesos largos compacto con botón `Ocultar`, restauración desde status bar mediante `Mostrar preparación`, iconos PNG más grandes en ribbon/sidebar/rail y recuperación de imágenes embebidas desde `source/*.docx` cuando un snapshot anterior no tenía `embeddedImageBase64`.
- Mantiene regla transversal: acciones con `ActionButtonFactory`, iconos con `AppIcon`/`IconView`, visuales fuente con `SourceVisualBlockView`.
- Siguiente recomendada: T112 — Smoke visual UX / RC visual.

## T112 — Smoke visual UX / RC visual

- Base: T111-HF4.
- Incluye hotfix focal del diagnóstico `20260602-153524`: se conserva el marcador histórico `T111-HF2 — PNG icon polish` en `components/ribbon.css` para mantener verde `IconographyCssAndPlaybarT111SourceTest` mientras la iconografía PNG grande de T111-HF4 sigue vigente.
- Objetivo: cerrar el recorrido visual de usuario antes de pasar a ejemplos internos y productización.
- Checklist: Inicio, Documento, imágenes DOCX, tablas, playbar, guardado guiado, overlay ocultable/restaurable, sidebar, rail, Voces, Guía, Configuración, exportación y reapertura.
- Documentación: `docs/productizacion/T112_SMOKE_VISUAL_UX_RC.md`, `docs/productizacion/T112_SMOKE_VISUAL_REPORTE_MANUAL.md` y `docs/168_TANDA_112_SMOKE_VISUAL_UX_RC.md`.
- Siguiente recomendada: T113 — Menú Ejemplos + proyectos demo internos.

## TP1 — RuntimePathResolver / layout de instalación

- Corrige `GuiComponentSurface.WELCOME` faltante tras T113.
- Agrega `application.runtime` con `RuntimePathResolver` y `ApplicationRuntimeLayout`.
- Centraliza layout `tools/`, `models/`, `scripts/`, `examples/`, FFmpeg, Piper y Coqui/XTTS.
- `InfrastructureServicesFactory` y `SettingsDialog` usan raíz runtime compartida.
- Próxima recomendada: TP2 — Preflight integrado a arranque/configuración.

## TP2 — Preflight integrado a arranque/configuración

- Base: TP1.
- Corrige continuidad documental rota por raíz demasiado resumida tras TP1.
- Agrega estado humano de motores: listo, requiere preparación o error.
- Agrega `BuildHumanEnginePreflightSummaryUseCase`, `HumanEnginePreflightSummary` y `HumanEnginePreflightState`.
- `SettingsDialog` muestra estado humano y siguiente acción en Configuración > Motores.
- Mantiene alcance visible: Coqui/XTTS, Piper y FFmpeg; Whisper/STT no vuelve al producto core visible.
- Documentación: `docs/productizacion/TP2_PREFLIGHT_INTEGRADO_ARRANQUE_CONFIGURACION.md`.
- Próxima recomendada: TP3 — Packaging tools/models/scripts.

## T113 — Menú Ejemplos + proyectos demo internos

- Base: T112 verde.
- Cambios: agrega menú `Ejemplos`, acción `Probar ejemplo` en Inicio, `ExampleProjectDialog`, catálogo de ejemplos y flujo guiado para crear proyecto demo con carpeta contenedora.
- Demos: `Instinto Creativo`, caso contable `Café Luna Azul` y guion teatral cómico `El vuelo del Tornillo Dorado` con assets visuales copiados al proyecto.
- Regla: los assets del demo se copian como recursos disponibles, no se asignan automáticamente al storyboard.
- Documentación: `docs/productizacion/T113_EJEMPLOS_PROYECTOS_DEMO.md`.
- Siguiente recomendada: TP1 — RuntimePathResolver / layout de instalación.

## TP3 — Packaging tools/models/scripts

- Base: TP2.
- Corrige fallo documental de `ReleaseCandidateDocumentationSourceTest`: `AI_HANDOFF.md` vuelve a mencionar Tanda 81D, T81E, Documento narrable y Word/DOCX.
- Agrega contrato de packaging runtime con `BuildRuntimeBundleManifestUseCase`, `RuntimeBundleManifest`, `RuntimeBundleItem` y `RuntimeBundleItemKind`.
- Formaliza estructura `tools/`, `models/`, `scripts/tts`, `tools/ffmpeg/bin`, `tools/piper`, `tools/xtts-wrapper`, `models/tts/xtts`, `models/tts/piper/voices` y ejemplos internos.
- Agrega `scripts/29-verificar-runtime-layout.bat` y reporte `target/runtime-layout/TP3_RUNTIME_LAYOUT_REPORT.md`.
- No redistribuye binarios ni modelos de terceros; TP4 queda dedicada a licencias y manifest de terceros.
- Documentación: `docs/productizacion/TP3_PACKAGING_TOOLS_MODELS_SCRIPTS.md` y `docs/169_TP3_PACKAGING_TOOLS_MODELS_SCRIPTS.md`.
- Próxima recomendada: TP4 — Licencias y manifest de terceros.

## TP4 — Licencias y manifest de terceros

TP4 agrega inventario legal/auditable de componentes de terceros: FFmpeg, Piper, Coqui/XTTS, Python portable, JavaFX, dependencias Maven, scripts y ejemplos internos. Introduce `BuildThirdPartyLicenseManifestUseCase` y `scripts/30-generar-manifest-terceros.bat`.

## TP5 — Instalador/app portable real

TP5 mejora app-image/MSI y agrega carpeta portable. `scripts/14-app-image-completa.bat` y `scripts/15-msi-completo.bat` copian runtime/legal cuando existen; `scripts/32-preparar-app-portable-layout.bat` genera `dist/portable/DocuPodcastStudio`.

## TP6 — RC instalable con smoke manual y automático

TP6 encadena revalidación, runtime layout, manifest de terceros, app-image, carpeta portable y smoke RC. Agrega `ReleaseCandidateGate` y `scripts/33-smoke-rc-instalable.bat`.

## TI1 — RenderUnit end-to-end

- Base: TP6 verde validada localmente por el usuario.
- Objetivo: crear el contrato central de unidades de render para integrar documento, guion, capas, audio, storyboard y video.
- Agrega `RenderUnitKind`, `RenderUnit`, `RenderUnitPlan` y `BuildRenderUnitPlanUseCase`.
- `RenderApplicationServices` ahora expone `buildRenderUnitPlan`.
- Clasificación de unidades:
  - `SPOKEN_ONLY`: unidad hablada sin visual, se omite del video.
  - `SPOKEN_WITH_VISUAL`: unidad hablada con visual, candidata a imagen + audio.
  - `VISUAL_SILENT`: unidad visual no narrable, candidata a imagen + silencio.
  - `OMITTED`: unidad omitida.
- Documentación exhaustiva de lo pendiente: `docs/productizacion/ROADMAP_RESTANTE_POST_TI1_DETALLADO.md`.
- Tests agregados: `RenderUnitPlanTest`, `BuildRenderUnitPlanUseCaseTest`, `RenderUnitEndToEndTi1SourceTest`.
- Siguiente recomendada: TI2 — Audio jobs desde RenderPlan.

## TI4 — Bloques visuales no narrables

- Agrega `MATH_NOTICE` para fórmulas/matemática detectadas en DOCX.
- Declara imagen, tabla y fórmula como bloques visuales fuente no narrables por defecto.
- Refuerza `SourceVisualBlockView` para imagen, tabla y fórmula.
- Mantiene LaTeX/OMML como identificación, no render completo.
- Siguiente tanda: TI5 — TextAnchor fuerte y reconciliación.

## TI6 — FFmpeg como job persistente/cancelable

- Base: TI5 con diagnóstico Maven tests fallando por source test documental ya corregido.
- Agrega job persistente para render FFmpeg: `VideoRenderJobSnapshot`, repositorio, submit/cancel use cases y mapper a `ProcessJobSnapshot`.
- `ProcessJobKind.VIDEO_RENDER` queda marcado como persistente.
- `VideoRenderJobFileRepository` escribe `jobs/video/<jobId>/video-render-job.json`, logs y token `cancel.requested`.
- El sidebar Fragmento muestra ubicación de fuente: bloque DOCX/PDF o líneas TXT/Markdown cuando aplica.
- Se refuerza que tablas, fórmulas y LaTeX detectado no se narran como código.
- Siguiente tanda: TI7 — Playback por oración/unidad.

## TI7 — Playback por oración/unidad

- Agrega construcción de `PlaybackManifest` desde `RenderUnitPlan`.
- Resuelve audio generado por `RenderUnit.id` en `AudioJobSnapshot`.
- Resuelve audio humano/local desde assets del proyecto.
- Agrega `SeekPlaybackUseCase.seekUnit(...)`.
- Mantiene `VISUAL_SILENT` fuera del playback de audio.

## RF1 — Dividir `DocuPodcastShellViewModel`

Se inicia el refactor de presentación extrayendo `ExportWorkflowCoordinator` para sacar la orquestación de exportaciones del ViewModel principal. La tanda no cambia UX ni formato persistente; prepara RF2.

## T114-HF1 — Build verde + no-guion visible mínimo

- Base: RF1 con diagnóstico local rojo por 2 source tests desalineados.
- Corrige `AudioClipPlaybackExportT92SourceTest` y `StoryboardVideoFromRenderPlanTi3SourceTest` para validar `ExportWorkflowCoordinator` en vez de exigir orquestación dentro de `DocuPodcastShellViewModel`.
- El shell deja de activar directamente `SCRIPT_EDITOR` y `STORYBOARD` en los flujos tocados; las rutas vuelven a `DOCUMENT_READER`.
- Se cambian mensajes visibles urgentes de “guion” a “lectura preparada/documento”.
- Se agrega `ProductCleanNavigationT114Hf1SourceTest` como guardarraíl inicial de producto limpio.
- Documento: `docs/productizacion/T114_HF1_BUILD_VERDE_NO_GUION_VISIBLE.md`.
- Próxima recomendada: T114-HF2 — Retiro total Whisper/STT.


## T114-HF2 — Retiro total Whisper/STT

- Base: T114-HF1.
- Corrige el único fallo reportado por el diagnóstico `20260602-224724`: `DocumentLayerAssignmentWorkflowSourceTest` ya no exige el texto viejo “segmento narrable”; valida “fragmento preparado”.
- Retira Whisper/STT del producto principal: se eliminan `application/stt`, `infrastructure/stt`, `scripts/stt`, `SpeechToTextApplicationServices` y el cableado en bootstrap.
- `OperationalSettings` deja de tener `SttEngineSettings`.
- `ComputeSettings` deja de tener `allowGpuForStt`.
- `PropertiesOperationalSettingsRepository` deja de persistir `stt.*`.
- `ModelFolderContract.recommended()` queda en Coqui/XTTS y Piper.
- `RecordingPurpose`, `AudioRecordingReference`, `RecordingActionPlan` y `AudioNormalizationProfile` dejan de tener ruta de Speech-to-Text.
- Agrega `NoWhisperSttT114Hf2SourceTest`.
- Documentación: `docs/productizacion/T114_HF2_RETIRO_TOTAL_WHISPER_STT.md`.
- Próxima recomendada: T123-CAT01 — Markdown como documento, no guion.


## T123-CAT01 — Markdown como documento, no guion

- Base: T114-HF2 verde validada localmente por el usuario.
- Objetivo: cerrar la vía heredada de Markdown como guion/importación especial.
- Markdown queda exclusivamente como documento fuente en `Abrir documento`, junto con DOCX, PDF nativo y TXT.
- Se retiran `handleImportScriptMarkdown`, `handleExportScriptMarkdown`, los métodos equivalentes en ViewModel, el cableado de servicios y la capacidad `IMPORT_SCRIPT_MARKDOWN`.
- Se retiran del build principal los use cases/parser heredados de `docupodcast-script-v1`.
- Los recursos IA oficiales pasan a ser guías/plantillas/ejemplos documentales no importables como contrato especial.
- La guía integrada deja de registrar Guion e IA/Markdown como topics de importación especial.
- El bundle auditable usa `lectura_preparada.md` en vez de `guion_narrable.md`.
- Documentación: `docs/productizacion/T123_CAT01_MARKDOWN_COMO_DOCUMENTO_NO_GUION.md`.
- Próxima recomendada: T124 — Cuarentena legacy workspaces/tests.

## T124 — Cuarentena legacy workspaces/tests

- Base: T123-CAT01 con diagnóstico rojo por 2 tests históricos.
- Corrige `GuideCatalogTest` por retiro de topics heredados de Guion/Markdown IA.
- Corrige `LoadProjectWorkspaceArtifactsUseCaseTest` para lenguaje de lectura preparada.
- Refuerza `WorkspaceSurfacePolicy.isLegacyInternalSurface(...)`.
- `SCRIPT_EDITOR`, `AUDIO_JOBS` y `STORYBOARD` quedan como superficies internas heredadas.
- `WorkspaceNavigationCoordinator` normaliza superficies legacy hacia `DOCUMENT_READER`.
- `DocuPodcastShellViewModel.showPlaceholder(...)` informa el workspace resuelto, no el legacy solicitado.
- Comandos heredados de Storyboard se reetiquetan hacia secuencia visual/visuales.
- Configuración cambia “Storyboard / video” por “Visuales / video”.
- Agrega `LegacyWorkspaceQuarantineT124SourceTest`.
- Documentación: `docs/productizacion/T124_CUARENTENA_LEGACY_WORKSPACES_TESTS.md`.

## T122-C01 — Encapsular Script como PreparedReadingProjection

- Base: T124 verde validada localmente por el usuario.
- Agrega `PreparedReadingProjection` como frontera de producto limpio.
- Agrega `BuildPreparedReadingProjectionUseCase`.
- `ScriptApplicationServices` expone `buildPreparedReadingProjection()`.
- `DocumentNarrationCoordinator` usa lectura preparada en vez de construir directamente el payload histórico.
- `AudioGenerationRequest` y `BuildNarrationRenderPlanUseCase` aceptan la frontera de lectura preparada.
- `NarrationScriptDocument` y `BuildNarrationScriptUseCase` quedan documentados como compatibilidad interna.
- Mensajes pasan a “Lectura preparada: X fragmentos...”.
- Agrega tests de dominio/productización para PreparedReadingProjection.
- Documentación: `docs/productizacion/T122_C01_PREPARED_READING_PROJECTION.md`.

## T128-C01 — Documentación de limpieza de producto

- Base: T122-C01.
- README, AI_HANDOFF y VALIDATION incorporan la regla vigente: Documento, no Guion; sin Whisper/STT; Markdown como documento; legacy controlado.
- Agrega `ProductDocumentationCleanT128C01SourceTest`.
- Documentación: `docs/productizacion/T128_C01_DOCUMENTACION_LIMPIEZA_PRODUCTO.md`.

## T115 — Selector CPU/GPU real para Coqui/XTTS

- Base: T122-C01 + T128-C01 con diagnóstico local rojo por 1 source test documental heredado.
- Corrige `BrainNarrationPlaybackCoordinatorSourceTest` para validar “prepared-reading brain” y compatibilidad interna en vez de frases viejas de narración avanzada.
- Agrega `ComputeDeviceDiscoveryGateway` como puerto de detección local CPU/GPU.
- Agrega `EnvironmentComputeDeviceDiscoveryGateway` como modo intermedio seguro.
- Agrega `WindowsComputeDeviceDiscoveryGateway` para consultar `Win32_VideoController` y clasificar GPUs NVIDIA/AMD/Intel.
- Agrega `XttsComputeDeviceMapper` para traducir selección de dispositivo a argumento efectivo de Coqui/XTTS: CPU, auto o `cuda:n` para NVIDIA.
- Configuración reemplaza el campo manual “Dispositivo específico” por selector “Dispositivo para Coqui/XTTS”.
- `LocalTtsProcessConfiguration` conserva `{computeDevice}` como ID crudo y usa `{device}` como argumento efectivo para XTTS.
- `ApplicationServicesFactory` usa el gateway Windows para detección real en runtime.
- Agrega tests `XttsComputeDeviceMapperTest`, `WindowsComputeDeviceDiscoveryGatewayTest` y `ComputeDeviceSelectorT115SourceTest`.
- Documentación: `docs/productizacion/T115_SELECTOR_CPU_GPU_COQUI_XTTS.md`.
- Próxima recomendada: T116 — FFmpeg autocontenido real.

## T116 — FFmpeg autocontenido real

- Base: T115 con diagnóstico local rojo por `ComputeDeviceBrainContractSourceTest`.
- Corrige el source test para validar que las señales `CUDA_VISIBLE_DEVICES` viven en `EnvironmentComputeDeviceDiscoveryGateway` y que Windows usa `Win32_VideoController`.
- Endurece el contrato FFmpeg local: `ffmpeg.exe` y `ffprobe.exe` deben estar en `tools/ffmpeg/bin/` para producto final.
- `BuildRuntimeBundleManifestUseCase` marca FFprobe como obligatorio para portable final.
- `scripts/29-verificar-runtime-layout.bat` valida los binarios reales de FFmpeg/FFprobe.
- `scripts/tts/preflight-piper-ffmpeg.ps1` reporta versiones y encoders.
- Agrega `FfmpegRuntimeProbeUseCase` y `FfmpegRuntimeReport` para detectar `libx264`, `h264_nvenc`, `h264_qsv` y `h264_amf`.
- Agrega tests `FfmpegRuntimeProbeUseCaseTest` y `FfmpegAutocontainedT116SourceTest`.
- Documentación: `docs/productizacion/T116_FFMPEG_AUTOCONTENIDO_REAL.md`.
- Próxima recomendada: T117 — Asistente real Coqui/XTTS.

## T126A — Fuente documental portable y refresco desde copia del proyecto

- Base: T116 con diagnóstico local rojo por `RuntimeBundleManifestUseCaseTest`.
- Corrige el texto de `BuildRuntimeBundleManifestUseCase` para no usar la frase `PATH global`, manteniendo la regla de no depender de rutas globales del sistema.
- `ReadableDocumentWorkspaceRepository.materialize(...)` ahora rebasa el documento activo hacia `source/<archivo>` dentro del proyecto.
- `MaterializedImportedDocument` expone `projectSourceDocument`.
- `MaterializeImportedDocumentUseCase` expone `materializeWithResult(...)`.
- `ProjectWorkflowCoordinator.saveProject(...)` actualiza la sesión con la copia canónica del proyecto.
- `DocuPodcastShellViewModel.saveCurrentProjectAs(...)` actualiza `currentDocument` desde la sesión al guardar.
- `DocuPodcastShellView` muestra aviso al usuario con checkbox “No volver a mostrar este aviso”.
- Agrega `ProjectSourcePortabilityT126ASourceTest`.
- Documentación: `docs/productizacion/T126A_FUENTE_DOCUMENTAL_PORTABLE.md`.

## T118 — Piper modo intermedio compatible

- Piper queda como modo intermedio/liviano de compatibilidad, separado del motor principal Coqui/XTTS.
- Se agregaron verificación y selección operativa de Piper desde Configuración.
- El contrato exige `.onnx` + `.onnx.json` y wrapper local sin depender del PATH.
- La UX no debe prometer emociones, clonación por muestra humana ni personajes expresivos en modo Piper.
## T119 — Configuración honesta y accionable

Categoría: CAT-02 — Motores reales.

Resumen:

- Corrige el fallo local de `ProjectSourcePortabilityT126ASourceTest` tras extraer el aviso de fuente portable a un diálogo de producto.
- Refuerza Configuración como superficie honesta: las acciones reales son botones y las tarjetas de catálogo se presentan como guía informativa.
- Agrega verificación accionable de FFmpeg desde Configuración usando `FfmpegRuntimeProbeUseCase`.
- Mantiene Coqui/XTTS y Piper como asistentes con botones reales de verificar/usar.
- Mueve la línea de comandos externa al ámbito de Diagnóstico avanzado para no confundir al usuario normal.
- Agrega `HonestActionableSettingsT119SourceTest`.

Documento: `docs/productizacion/T119_CONFIGURACION_HONESTA_ACCIONABLE.md`.


## T119B — UX por capacidades del motor de voz

La UI pasa a depender de capacidades reales del motor activo: Piper queda como narrador intermedio sin emociones/clonación por muestra; Coqui/XTTS habilita capacidades avanzadas cuando correspondan; Mock se declara modo de prueba.

## T119C — Manifiestos, licencias y checksums de motores

Se agrega contrato formal de artefactos de motor: FFmpeg/FFprobe, Python local, wrapper Coqui/XTTS, modelo XTTS, voz neutral y Piper. Cada artefacto queda con ruta esperada, licencia, origen y SHA-256 pendiente hasta fijar el binario/modelo final. El script de terceros genera también `ENGINE_ARTIFACTS_MANIFEST.md`.

## T120 — Limpieza de onboarding / primer uso

Inicio queda orientado a flujo real de usuario: Preparar voz, Abrir documento, Escuchar y Exportar. Preparar voz abre Configuración mediante `OPEN_SETTINGS`; no es texto falso. El onboarding elimina señales de Guion, Whisper/STT y Storyboard como módulo principal.

## T120B — Contrato final del Ribbon y navegación principal

- Inicio concentra abrir fuente, escuchar documento/selección y proyecto.
- Lectura concentra preparar lectura, generar audio y cancelar generación.
- Vista incluye Documento/Lector y Voces como vistas principales, además de panel visual, pantalla completa, Configuración y Guía.
- Storyboard deja de ser pestaña principal; el rail visual pertenece al Documento.
- Se agrega `OPEN_DOCUMENT_READER` y test guardarraíl `RibbonFinalContractT120BSourceTest`.


## T121-V09 — Componentes/CSS de Voces

- Base: T121-V08 con diagnóstico local rojo por `VoiceLocalSimpleT121V06SourceTest`.
- Corrige el literal visible de Voz local simple para mantener el contrato `no usa muestras humanas`.
- Extrae componentes específicos de Voces: `VoiceProfileCard`, `VoiceEngineModeCard`, `VoiceToneBadge`, `VoiceSampleRow`, `VoiceGeneratedTestPanel`.
- `VoiceLibraryWorkspaceView` usa los componentes para tarjetas, modos, filas de muestras y prueba generada.
- Registra los componentes nuevos en `GuiComponentCatalog`.
- Amplía `voice-library.css` con clases de componentes sin usar `setStyle`.
- Mantiene nombres amigables en GUI: Voz IA avanzada, Voz local simple y Modo de prueba.
- Tests: `VoiceComponentsCssT121V09SourceTest` y guardarraíl V06 corregido.
- Documento: `docs/productizacion/T121_V09_COMPONENTES_CSS_VOCES.md`.
- Próxima recomendada: T121-V10 — Tests, documentación y smoke visual de Voces.

## T121-V04C — Cableado visual mínimo del wizard por tono

- Base: T121-V04B con diagnóstico local rojo por un source test de literal escapado.
- Corrige `VoiceToneSamplesPersistenceT121V04BSourceTest` para validar `referenceSampleSets` sin depender de comillas no escapadas en código Java.
- `VoiceLibraryWorkspaceView` consume `VoiceRegistrationWizardPlan` y `VoiceToneRecordingPlan`.
- La Vista Voces muestra selector de tono, frase guía del tono, contrato de cancelar/detener/guardar y acciones de importar/grabar usando `ActionButtonFactory`.
- `DocuPodcastShellViewModel` conserva el tono activo de grabación y registra la muestra importada/grabada con `VoiceSampleImportRequest.forOwnVoiceTone(...)`.
- Tests: `VoiceLibraryWizardWiringT121V04CSourceTest`.
- Documento: `docs/productizacion/T121_V04C_CABLEADO_VISUAL_WIZARD_TONO.md`.
- Próxima recomendada: T121-HF1 — Limpieza de lenguaje visible de motores/visuales o T121-V05 si se prioriza prueba generada.

## T121-V04B — Persistencia real de muestras por tono

- Base: T121-V04.
- `VoiceLibrary` incorpora `referenceSampleSets` para guardar muestras por voz y tono.
- `VoiceReferenceSampleSet` permite reemplazar una muestra por tono sin duplicados.
- `VoiceSampleImportRequest` acepta `VoiceReferenceTone` y mantiene compatibilidad neutral para llamadas legacy.
- `ImportVoiceSampleUseCase` crea assets `VOICE-SAMPLE-<VOICE>-<TONE>` y preserva `VoiceProfile.sampleAssetId` como muestra neutral legacy.
- `.docupodcast.json` y `voices/voice-library.json` serializan `referenceSampleSets`.
- El reader tolera proyectos anteriores sin `referenceSampleSets`.
- Tests: dominio, importación, roundtrip JSON, materialización workspace y source test de producto.
- Documento: `docs/productizacion/T121_V04B_PERSISTENCIA_MUESTRAS_TONO.md`.
- Próxima recomendada: T121-V04C — Cableado visual mínimo del wizard por tono.

## RF1 — Continuidad: ReadingComfortCoordinator

- Base: T121-V10 verde confirmada por el usuario.
- Se extrae `ReadingComfortCoordinator` para mover fuera de `DocuPodcastShellViewModel` la carga, clamp, zoom y persistencia del tamaño de lectura.
- `DocuPodcastShellViewModel` queda por debajo de 2500 líneas y conserva solo propiedades JavaFX/handlers visibles de lectura.
- No cambia comportamiento visible; StatusBar y lector mantienen rango 14–28 px y 18 px = 100%.
- Tests: `PresentationRefactorRf1SourceTest` y `ShellViewModelBrainDebtSourceTest`.
- Documento: `docs/productizacion/RF1_CONTINUIDAD_READING_COMFORT_COORDINATOR.md`.
- Próxima recomendada: RF2 — Coordinadores de presentación.


## RF2 — Coordinadores de presentación / selección documental

- Base: RF1 con diagnóstico local rojo por `StatusBarReadingZoomT102SourceTest`.
- Corrige el test de T102 para validar `ReadingComfortCoordinator` como dueño de persistencia de zoom.
- Agrega `DocumentSelectionCoordinator` para centralizar previews, etiquetas, ubicación fuente y copy de acción primaria del Documento.
- `DocuPodcastShellViewModel` queda por debajo de 2450 líneas y mantiene solo propiedades JavaFX/handlers.
- No altera CSS ni componentes visuales transversales.
- Documento: `docs/productizacion/RF2_COORDINADORES_PRESENTACION_DOCUMENT_SELECTION.md`.
- Próxima recomendada: RF3 — Use cases de orquestación de producto.

## RF3 — Use cases de orquestación de producto

- Base: RF2 con diagnóstico local rojo por `LongProcessOverlayT109SourceTest`.
- Corrige el test de T109 para validar `previewForRange` en `DocumentSelectionCoordinator`, no en `DocuPodcastShellViewModel`.
- Agrega `PrepareListeningSessionUseCase`, `PrepareListeningSessionRequest`, `ListeningSessionReadiness` y `ListeningSessionState` en application.
- `DocumentApplicationServices` expone `prepareListeningSession` y `ApplicationServicesFactory` lo cablea.
- `DocumentNarrationCoordinator` delega el plan de escucha y el estado del recorrido a application.
- `DocuPodcastShellViewModel.refreshDocumentListenFlow()` deja de reconstruir booleanos de precondiciones y consume `documentNarration.listeningSession(...)`.
- No cambia UX, CSS ni componentes visuales.
- Documento: `docs/productizacion/RF3_USE_CASES_ORQUESTACION_PRODUCTO.md`.
- Próxima recomendada: RF4 — Limpieza de workspaces heredados.


## RF4 — Limpieza de workspaces heredados

- Base: RF3 verde confirmada por el usuario.
- `WorkspaceViewRegistry` rechaza factories para superficies internas heredadas y normaliza solicitudes heredadas con `WorkspaceSurfacePolicy.restoreStartupWorkspace(...)`.
- `DocuPodcastShellViewModel` deja de exponer `showScriptWorkspace()`, `showStoryboardWorkspace()` y `showAudioWorkspace()`.
- `DocuPodcastShellView` no registra handlers para comandos ocultos `OPEN_STORYBOARD` ni `OPEN_AUDIO_JOBS`.
- Los comandos legacy de apertura quedan solo como ocultos en el catálogo para compatibilidad, no como UX.
- Tests: `LegacyWorkspaceCleanupRf4SourceTest`.
- Documento: `docs/productizacion/RF4_LIMPIEZA_WORKSPACES_HEREDADOS.md`.
- Próxima recomendada: RF5 — Limpieza residual STT/Whisper.


## RF5 — Limpieza residual STT/Whisper

- Base: RF4 verde confirmada por el usuario.
- Elimina `HUMAN_RECORDING_FOR_TRANSCRIPTION` de `VoiceSourceKind`.
- `PerformanceSpan` ya no reconoce fuentes de transcripción; solo voz IA, audio del computador o sin asignar.
- Scripts/smoke dejan de imprimir audio-a-texto/STT como alcance operativo.
- Agrega `ResidualWhisperSttCleanupRf5SourceTest` como guardarraíl.
- Documento: `docs/productizacion/RF5_LIMPIEZA_RESIDUAL_STT_WHISPER.md`.
- Próxima recomendada: TC1 — `CommandAvailabilityPolicy`.


## TC1 — CommandAvailabilityPolicy

- Base: RF5 verde confirmada por el usuario.
- Agrega `CommandAvailabilityPolicy` como fuente única de disponibilidad para comandos visibles.
- `DocuPodcastShellView.commandItem(...)` y `RibbonView` delegan en la política central.
- `WorkspaceCapabilityPolicy` queda como adaptador legacy vía `WorkspaceCapabilityCommandMapper`.
- Se agregan razones humanas con `unavailableReason(...)`.
- No cambia UX, CSS ni componentes visuales; la GUI normal mantiene nombres amigables de motores.
- Tests: `CommandAvailabilityPolicyTc1SourceTest` y actualización de guardarraíles de Documento/Toolbar.
- Documento: `docs/productizacion/TC1_COMMAND_AVAILABILITY_POLICY.md`.
- Próxima recomendada: TE1 — Exportaciones alineadas a voces por tono.


## TE1 — Exportaciones alineadas a voces por tono

- Base: TC1 verde confirmada por el usuario.
- Agrega `BuildVoiceReferenceSamplesExportReportUseCase` y `VoiceReferenceSamplesExportReport`.
- El bundle exportado genera `reports/VOICE_REFERENCE_SAMPLES.md` y `reports/VOICE_REFERENCE_SAMPLES.tsv`.
- El manifiesto de exportación audita muestras por tono, assets registrados, archivos faltantes y pruebas de voz generadas.
- `ProjectBundleExportResult` expone `voiceReferenceSamplesReportFile`, `voiceReferenceSamplesIndexFile` y `voiceReferenceSampleCount`.
- `ExportWorkflowCoordinator` informa el conteo de muestras de voz sin nombres técnicos de motores.
- Tests: `BuildVoiceReferenceSamplesExportReportUseCaseTest`, `ExportVoiceSamplesTe1SourceTest`.
- Documento: `docs/productizacion/TE1_EXPORTACIONES_ALINEADAS_VOCES_TONO.md`.
- Próxima recomendada: TV1 — Limpieza de visuales/video honesto.


## TV1 — Limpieza de visuales/video honesto

- Base: TE1 verde confirmada por el usuario.
- `SimpleVideoPackageExportResult` expone estado honesto del paquete: `renderModeLabel`, `renderableAsMp4`, `outputFileName` y `honestStatusLabel()`.
- `ExportSimpleVideoPackageUseCase` y `ExportWorkflowCoordinator` dejan explícito que la exportación prepara un paquete renderizable/auditable y no crea el MP4 final por sí sola.
- La UX normal habla de Visuales / Secuencia visual; `storyboard` queda como nombre interno/legacy.
- Se limpian textos visibles en Documento, ayuda, componentes y superficies heredadas.
- Tests: `VideoHonestyTv1SourceTest`, `VisualLanguageTv1SourceTest` y actualización de tests de video/visual/documentación.
- Documento: `docs/productizacion/TV1_LIMPIEZA_VISUALES_VIDEO_HONESTO.md`.
- Próxima recomendada: TD1 — Cuarentena documental histórica.

## PF2B — Configuración in-app de Voz IA avanzada

- Base: PF1B + PF2A.
- El script de preparación deja de ser la experiencia normal: queda como adaptador técnico invocado desde Configuración bajo demanda.
- `SettingsDialog` ofrece acciones reales: verificar, preparar automáticamente, importar modelo y usar solo si está listo.
- Agrega `PrepareXttsPortableRuntimeUseCase`, `XttsRuntimePreparationReport`, `ImportXttsModelFolderUseCase` y `XttsModelImportReport`.
- `SettingsApplicationServices` y `ApplicationServicesFactory` cablean los nuevos casos de uso.
- `ModelFolderContract.xttsHighQuality()` exige `config.json`, `model.pth` y `vocab.json`, alineado al wrapper/checker.
- Tests: `AdvancedVoiceInAppSetupPf2BSourceTest`, `ImportXttsModelFolderUseCaseTest`.
- Documento: `docs/productizacion/PF2B_CONFIGURACION_IN_APP_VOZ_IA.md`.
- Próxima recomendada: PF2C — catálogo/descarga real de artefactos de motor con manifiesto, checksums y licencias.

## PF2C — Descarga in-app del modelo oficial de Voz IA avanzada

- Base: PF2B.
- Agrega `DownloadXttsOfficialModelUseCase` y `XttsModelDownloadReport`.
- `SettingsDialog` suma el botón `Descargar modelo oficial` con confirmación explícita y ejecución en hilo de fondo.
- La descarga queda bajo `models/tts/xtts` y no se ejecuta al iniciar.
- `ModelFolderContract.xttsHighQuality()` y `check_xtts_runtime.py` exigen `config.json`, `model.pth`, `vocab.json`, `speakers_xtts.pth`, `dvae.pth` y `mel_stats.pth`.
- `module-info.java` agrega `requires java.net.http`.
- Documento: `docs/productizacion/PF2C_DESCARGA_MODELO_OFICIAL_VOZ_IA.md`.
- Próxima recomendada: PF3 — smoke real desde GUI y ejecutable portable.

## PF2D/PF3A — progreso in-app y voces operativas

- PF2D — Configuración con progreso visible para Voz IA avanzada.
- PF3A — Biblioteca de voces con acciones reales sobre muestras.
- Documentos: `PF2D_CONFIGURACION_PROGRESO_VOZ_IA.md`, `PF3A_VOCES_ACCIONES_REALES_MUESTRAS.md`.

## PF2E/PF3B — progreso vivo, logs alineados y voces más operativas

- Base: PF2D/PF3A con diagnóstico `20260604-024335.zip`.
- Corrige fallos Maven reportados: etiqueta de modo de prueba, acción `Usar Voz IA avanzada`, tarjeta de motores, deuda de `DocuPodcastShellViewModel` y límite RF2.
- `PrepareXttsPortableRuntimeUseCase` transmite salida del instalador y latidos periódicos para evitar diálogos congelados.
- `DownloadXttsOfficialModelUseCase` informa archivo actual y avance por bloques descargados.
- `ImportXttsModelFolderUseCase` reporta validación, copia y verificación.
- `SettingsDialog` muestra última actualización, estado vivo y cierre solo tras completado/error.
- `scripts/04-verificar-tts-config.bat` remite a Configuración como ruta normal y deja el script como rescate técnico.
- `VoiceSampleWorkflowCoordinator` absorbe reglas de muestras para reducir deuda del shell.
- Vista Voces suma accesos directos a Configuración de motores y estado de voces desde el hero.
- Tests: `SettingsOperationProgressPf2ESourceTest`, `VoiceLibraryOperationalUxPf3BSourceTest` y actualización de guardarraíles existentes.
- Documento: `docs/productizacion/PF2E_PROGRESS_CONFIGURACION_Y_PF3B_VOCES.md`.
- Próxima recomendada: PF4 — ejecutable portable/app-image con runtime y verificación post-instalación.


## PF2F/PF4A — hotfix configuración honesta y launcher portable

- Base: PF2E/PF3B con diagnóstico `20260604-080938.zip`.
- Corrige fallos Maven por textos legacy de Configuración: `Usar Voz IA avanzada`, nota exacta de línea de comandos avanzada y tarjetas informativas de motores.
- `SettingsDialog.OperationProgress` agrega latido UI independiente: tiempo transcurrido, última señal y ruta de logs técnicos mientras instala/descarga.
- PF4A agrega launcher `run-docupodcast-studio.bat` al app-image y carpeta portable.
- El launcher fija `DOCUPODCAST_APP_ROOT=%%~dp0` para doble clic limpio sin depender de la carpeta desde donde se abrió PowerShell.
- `APP_IMAGE_MANIFEST.txt` deja de describir la app como mock por defecto y registra la configuración in-app de motores.
- Tests: `PortableAppImagePf4ASourceTest` y actualización de guardarraíles T117/T119.
- Documento: `docs/productizacion/PF4A_EJECUTABLE_PORTABLE_LAUNCHER.md`.
- Próxima recomendada: PF4B — smoke post-app-image y verificación de runtime root desde launcher.

## PF4B/PF5A — smoke portable y auditoría de artefactos de motores

- Base: PF2F/PF4A con diagnóstico `20260604-082244.zip`.
- Hotfix: corrige escapes ilegales en `SettingsDialog` para rutas `target\\docupodcast-engine-setup\\logs` que rompían `mvn compile`.
- PF4B agrega `scripts/34-smoke-app-portable-runtime.bat` para validar app-image, carpeta portable, launcher, `DOCUPODCAST_APP_ROOT`, runtime root y manifiesto portable.
- `scripts/16-release-candidate.bat` incorpora el smoke PF4B en el flujo RC.
- PF5A agrega `AuditEngineArtifactsUseCase`, `EngineArtifactAuditReport` y `EngineArtifactFileStatus` para auditar archivos locales, rutas esperadas y SHA-256 de motores.
- Configuración → Motores de voz agrega la tarjeta `Inventario local de motores` con acción real `Auditar artefactos locales` y reporte `target/legal/ENGINE_ARTIFACTS_AUDIT.md`.
- Nuevo script técnico `scripts/35-auditar-artefactos-motores.bat` para diagnóstico equivalente sin GUI.
- Tests: `PortableRuntimeSmokePf4BSourceTest` y `EngineArtifactAuditPf5ASourceTest`.
- Documentos: `PF4B_SMOKE_APP_PORTABLE_RUNTIME.md` y `PF5A_AUDITORIA_ARTEFACTOS_MOTORES.md`.
- Próxima recomendada: PF5B — preparación guiada por bloques de motores/FFmpeg/modelos con manifiesto legal accionable desde Configuración.

## VOZ-UX4R-DOC1 — contrato final modular de Vista Voces

- Base: VOZ-UX4R-1.
- Tanda documental: no cambia comportamiento productivo, alinea la documentación antes de implementar la vista delicada de Voces.
- Define Vista Voces como microaplicación administrativa sobria, con tres módulos principales: Inicio, Configurar motor y Gestionar voces.
- Cierra reglas de producto: filas sobrias, muchas emociones, Neutral obligatoria, una voz como entidad única con muestras por emoción, reemplazo de emoción por importación/grabación, eliminación con message box y borrado de archivos asociados.
- Configurar motor dentro de Voces debe usar la misma configuración interna que Configuración y debe incluir selector de dispositivo CPU/GPU real para todos los motores.
- Documento debe listar solo voces con Neutral y emociones realmente registradas para la voz seleccionada.
- Nuevo guardarraíl: `VoiceUx4RFinalContractDocumentationSourceTest`.
- Documento rector: `docs/productizacion/VOZ_UX4R_CONTRATO_FINAL_MODULOS.md`.
- Próxima recomendada: VOZ-UX4R-2A — shell modular simple de Voces.

## VOZ-UX4R-2A — shell modular simple de Voces

- Vista Voces se reorganiza como microaplicación administrativa con módulos Inicio, Configurar motor y Gestionar voces.
- Se agregan `VoiceModuleId`, `VoiceModuleDescriptor`, `VoiceModuleNavigation` y `VoiceWorkspaceShell`.
- Se elimina el layout `SplitPane` de la vista de voces.
- Gestionar voces concentra selección de voz, detalle, muestras por tono/emoción y pruebas.
- Documento técnico: `docs/productizacion/VOZ_UX4R_2A_SHELL_MODULAR_VOCES.md`.

## VOZ-UX4R-2B — Configurar motor real dentro de Voces

Se completa el módulo Configurar motor de la microaplicación Voces con selector de motor activo, selector de dispositivo de renderizado CPU/GPU detectado, estado honesto y prueba por textbox. Las selecciones se persisten en `OperationalSettings`, la misma configuración interna usada por Configuración. No se resuelve todavía la descarga de Voz IA avanzada/Coqui; queda para `MOTOR-SMOKE4R / COQUI-DL1`.


- **VOZ-UX4R-3**: Gestionar voces real — crear/guardar voz, eliminar con aviso de audios, reemplazar emociones importando/grabando, exportar muestras. Siguiente: VOZ-TTS5 para que Documento consuma voces con Neutral y emociones registradas.

## VOZ-UX4R-3B — navegación modular sobria de Voces

Corrige la navegación interna de Vista Voces: los módulos Inicio, Configurar motor y Gestionar voces cambian correctamente sin que el refresco de selección devuelva la vista a Gestionar voces. El sidebar ahora muestra solo el nombre de cada módulo, con estilo administrativo sobrio tipo escritorio/Teams, sin degradados ni descripciones dentro del botón.

## EXAMPLE-WORKFLOW-RF1 + DEMO-FALLBACK-NOTICES-HF1 + DEMO-ASSET-BINDINGS-TEST1

Se extrae la creación operativa de ejemplos a `ExampleProjectCreationWorkflow`, se muestra una decisión visible si el demo teatral queda con visuales sin asociar y se agregan guardarraíles para proteger los 24 bindings visuales del demo.
