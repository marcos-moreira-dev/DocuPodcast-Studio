# Plan maestro pendiente — DocuPodcast Studio v1

Este documento reemplaza el roadmap pendiente anterior. Su objetivo es preservar el contexto completo si se corta la conversación y fijar el orden de implementación restante.

## Principio rector

DocuPodcast Studio debe ser un lector narrado de documentos Word/DOCX para usuarios no técnicos. La pantalla principal debe permitir abrir, leer, escuchar, seleccionar y asignar capas simples sin exponer la fábrica interna. La configuración, motores, diagnósticos, modelos, FFmpeg, jobs, checksums y parámetros avanzados viven en Configuración o en módulos avanzados.

```text
Pantalla principal = zona limpia para el usuario.
Configuración/diagnóstico = bodega técnica.
Infraestructura interna = fábrica del programa.
```

## Contrato de documento importable

Todo archivo importable se trata primero como documento de trabajo, sea una obra de teatro, un texto técnico, un apunte de estudio, una narración, un guion o un documento normal. No se debe obligar al usuario a clasificar de entrada el documento como guion o como texto técnico.

```text
DOCX original = fuente importada inmutable.
ReadableDocument = documento normalizado y revisable.
NarrationScript = estructura narrable generada/derivada.
NarrativeLayerAssignments = capas de voz/audio/emoción/imagen/ambiente/nota guardadas en el proyecto.
```

Las acotaciones no se escriben dentro del Word. Se guardan como capas del proyecto DocuPodcast.

## Contrato visual operativo

La UI debe evolucionar hacia una apariencia moderna, formal y limpia. La referencia conceptual puede tomar ideas de Teams, MuseScore, Subtitle Edit, Fractal Studio y Domain Model Studio, pero sin copiar literalmente ni convertir la app en circo visual.

Reglas:

```text
no look Windows XP / JavaFX crudo;
no exceso de paneles anidados;
no panel dentro de panel porque sí;
no botones exageradamente redondeados;
no sombras o tarjetas decorativas pesadas;
no jerga técnica en pantalla principal;
componentes GUI transversales cuando haya repetición real;
CSS modular, no archivos gigantescos;
Documento debe ser el centro operativo.
```

## Tanda 49 — Auditoría de scaffolding visual + contrato FFmpeg embebido

Objetivo: revisar y documentar qué elementos visibles son operación, configuración o infraestructura antes de seguir agregando UI. Fijar FFmpeg como herramienta local embebida del producto para video.

Incluye:

```text
inventario de pantalla principal;
inventario de configuración;
inventario de workspaces avanzados;
reglas de qué botones quedan visibles;
reglas de qué se mueve a configuración;
reglas de qué se esconde como infraestructura;
contrato tools/ffmpeg/bin/ffmpeg.exe y ffprobe.exe;
manifest de FFmpeg, VERSION y LICENSE;
fallback a FFmpeg externo solo si el embebido no existe;
video 2K por defecto cuando hay storyboard/video;
```

Tests esperados:

```text
VisualScaffoldingAuditDocumentationTest
EmbeddedFfmpegContractSourceTest
Video2KContractSourceTest
MainWorkspaceDoesNotExposeTechnicalWarehouseTest
```

## Tanda 50 — Modernización visual base

Objetivo: abandonar el estilo Windows XP/JavaFX crudo y crear una base moderna para todo el producto.

Incluye:

```text
tema claro moderno;
preparación de modo oscuro futuro;
tokens CSS de color, espaciado, tipografía y estados;
barra superior más limpia;
botones formales y modernos, sin exagerar redondeos;
mejores estados vacíos;
mejor statusbar;
componentes transversales reutilizados;
limpieza de CSS legacy;
```

Tests esperados:

```text
ModernVisualThemeSourceTest
CssTokenCoverageSourceTest
NoLegacyXpLookSourceTest
NoExcessiveNestedPanelsSourceTest
NoLargeCssFileSourceTest
```

## Tanda 51 — Documento como pantalla operativa real

Objetivo: que Documento sea el lugar principal donde el usuario trabaja.

Incluye:

```text
documento tipo página Word;
texto grande y cómodo;
ancho de página controlado;
acción principal clara;
mini rail plegable no invasivo;
panel técnico colapsado;
Guion/Audio/Voz/Storyboard como avanzados, no ruta obligatoria;
estado operativo simple;
```

Tests esperados:

```text
DocumentOperationalWorkspaceSourceTest
DocumentPageRenderingSourceTest
DocumentKeepsTechnicalPanelsSecondarySourceTest
DocumentPrimaryActionVisibilityTest
```

## Tanda 52 — Flujo Word → escuchar completo

Objetivo: que un usuario no técnico pueda abrir un Word y escucharlo sin entender la fábrica interna.

Incluye:

```text
abrir/importar DOCX;
crear guion automáticamente si falta;
generar audio mock o motor disponible si falta;
reproducir cuando haya buffer suficiente;
subrayar oración/bloque activo;
scroll estable tipo MuseScore;
pausar/reanudar;
reproducir desde selección;
mensajes humanos si falta guardar o falta motor;
```

Tests esperados:

```text
ListenDocumentWorkflowTest
DocumentToScriptToAudioFlowTest
PlaybackActiveSentenceFollowTest
ListenDocumentWithoutTechnicalWorkspaceSourceTest
```

## Tanda 53 — Selección y asignación funcional desde UI

Objetivo: operar sobre oraciones/rangos reales desde el Documento.

Incluye:

```text
seleccionar oración;
asignar voz IA;
asignar voz humana pregrabada;
asignar audio externo del computador;
asignar emoción/intención;
asignar imagen/storyboard;
asignar audio ambiente;
asignar nota de producción;
quitar asignación;
reemplazar asignación;
detectar conflictos y pedir confirmación;
no modificar el Word original;
```

Tests esperados:

```text
DocumentRangeAssignmentWorkflowTest
NarrativeLayerConflictPolicyTest
ReplaceOrRemoveAssignmentUiSourceTest
AssignmentDoesNotModifyOriginalWordTest
```

## Tanda 54 — Mini rail multimedia real

Objetivo: que el rail plegable sea usable como lista compacta de capas visuales/sonoras.

Incluye:

```text
miniaturas reales;
imágenes asignadas;
imágenes sin texto asignado;
audios asignados;
voces/personajes;
estados listo/pendiente/falló;
texto asociado;
acciones rápidas: ubicar, asociar, desasignar, reproducir;
click en item => resalta texto asociado;
rail plegable/retraíble;
```

Tests esperados:

```text
MiniMediaRailInteractionTest
RailItemHighlightsDocumentRangeTest
UnassignedMediaRailItemSourceTest
CollapsibleRailBehaviorSourceTest
```

## Tanda 55 — Motor de voz usable

Objetivo: que voz/TTS sea configurable sin línea de comandos para el usuario normal.

Incluye:

```text
XTTS/Coqui como ruta potente prioritaria;
Piper como ruta liviana/semipotente;
mock o diagnóstico como fallback;
velocidad de habla;
pausas entre oraciones y párrafos;
volumen;
voz predeterminada;
controles soportados por motor;
probar voz;
preflight;
mensajes de error claros;
muestras de voz por intención: neutral, triste, feliz, enojada, intrigada;
```

Tests esperados:

```text
VoiceEngineCapabilityPolicyTest
VoiceSpeedSettingsTest
VoiceEnginePreflightSourceTest
VoiceEngineDoesNotExposeRawCommandToUserTest
```

## Tanda 56 — Asistente real de modelos

Objetivo: instalar/importar/verificar modelos sin línea de comandos.

Incluye:

```text
catálogo versionado;
checksums reales;
carpeta local de modelos;
importación manual;
descarga asistida cuando exista fuente verificable;
progreso;
diagnóstico si falla;
no depender de una URL única;
abrir carpeta de modelos;
probar motor;
```

Tests esperados:

```text
ModelInstallAssistantWorkflowTest
ModelChecksumVerificationTest
ManualModelImportTest
NoSingleHardcodedModelUrlTest
```

## Tanda 57 — Streaming/prebuffer robusto

Objetivo: documentos largos sin esperar horas.

Incluye:

```text
generar primeros 5 fragmentos;
empezar playback;
mantener 10 fragmentos por delante;
pausar si falta audio;
mostrar cargando;
reanudar automáticamente;
persistir WAVs por chunk;
no cargar todo en memoria;
configuración de buffer en Configuración;
```

Tests esperados:

```text
PlaybackBufferPolicyTest
StreamingAudioGenerationWorkflowTest
PlaybackWaitsForMissingChunkTest
LongDocumentChunkingTest
```

## Tanda 58 — Exportación de video simple 2K con FFmpeg embebido

Objetivo: exportar video cuando el usuario usa storyboard/imágenes. No es una función prescindible: es una capacidad del producto cuando se activa la capa visual.

Incluye:

```text
FFmpeg embebido en tools/ffmpeg;
ffprobe embebido;
resolución por defecto mínimo 2K;
MP4 real si FFmpeg está disponible;
fallback a paquete exportable si no se puede renderizar;
frame por oración/párrafo;
imagen asociada o fondo neutro;
duración = audio + silencio;
manifest/reporte;
validación de licencias/versiones de FFmpeg;
```

Tests esperados:

```text
EmbeddedFfmpegDiscoveryTest
VideoExportUsesBundledFfmpegTest
SimpleVideo2KExportPolicyTest
SimpleVideoMp4ExportWorkflowTest
SimpleVideoFallbackPackageTest
VideoFrameDurationFromAudioTest
VideoExportRequiresStoryboardOrMediaTest
```

## Tanda 59 — Round-trip real de usuario

Objetivo: cerrar/reabrir proyecto sin perder la experiencia real.

Incluye:

```text
Word importado;
texto normalizado;
selección de oraciones;
voces;
emociones;
audios;
imágenes;
mini rail;
storyboard;
video plan;
jobs;
playback;
guardar/cerrar/abrir;
exportar después de reabrir;
```

Tests esperados:

```text
DocuPodcastFullProjectRoundTripTest
NarrativeAssignmentsRoundTripTest
MediaRailRoundTripTest
VideoPlanRoundTripTest
```

## Tanda 60 — Refactor/cohesión interna

Objetivo: evitar que ViewModel y vistas acumulen toda la lógica nueva.

Extraer o consolidar:

```text
DocumentNarrationCoordinator
DocumentSelectionCoordinator
MediaAssignmentCoordinator
PlaybackWorkflowCoordinator
ModelSetupCoordinator
VideoExportCoordinator
SettingsCoordinator
```

Tests esperados:

```text
ShellViewModelSizeGuardTest
CoordinatorResponsibilitySourceTest
NoPresentationInfrastructureLeakTest
WorkspaceUsesSharedComponentsSourceTest
```

## Tanda 61 — Configuración avanzada terminada

Objetivo: que la bodega técnica quede ordenada y usable.

Incluye:

```text
lectura;
reproducción/buffer;
voz/TTS;
STT/Whisper;
audio;
storyboard/video;
modelos;
FFmpeg;
diagnóstico;
almacenamiento;
rendimiento;
presets simples;
```

Tests esperados:

```text
SettingsSectionsCoverageTest
SettingsDoNotPolluteMainWorkspaceTest
PlaybackBufferSettingsTest
VoiceSettingsCapabilityBindingTest
FfmpegSettingsCoverageTest
```

## Tanda 62 — Smoke real con documentos de prueba

Objetivo: validar como usuario real, no solo por source tests.

Casos mínimos:

```text
Word simple 1 página;
documento técnico varias páginas;
obra de teatro corta;
documento largo simulado;
documento con imágenes;
documento con muchas asignaciones;
exportación audio;
exportación video 2K;
```

Artefactos y tests esperados:

```text
SMOKE_MINIMO_V1.md
SMOKE_DOCUMENTO_LARGO.md
SMOKE_STORYBOARD_VIDEO_2K.md
EndToEndUserFlowTest
```

## Tanda 63 — Packaging / Release Candidate

Objetivo: generar instalable real y evidencia de release.

Incluye:

```text
app-image;
MSI;
versión única;
icono;
runtime Java;
tools/ffmpeg embebido;
manifest;
hashes;
logs;
limitaciones conocidas;
guía instalación;
guía motores;
guía FFmpeg;
smoke mínimo;
smoke extendido;
```

Tests esperados:

```text
ReleasePackagingScriptsSourceTest
InstallerManifestSourceTest
KnownLimitationsDocumentationTest
ReleaseCandidateChecklistSourceTest
EmbeddedToolsPackagingSourceTest
```

## Orden recomendado inmediato

```text
49 — Auditoría scaffolding visual + contrato FFmpeg embebido
50 — Modernización visual base
51 — Documento como pantalla operativa real
52 — Flujo Word → escuchar completo
53 — Selección y asignación funcional desde UI
54 — Mini rail multimedia real
58 — Exportación video simple 2K con FFmpeg embebido
55 — Motor de voz usable
56 — Asistente real de modelos
57 — Streaming/prebuffer robusto
59 — Round-trip real
60 — Refactor/cohesión interna
61 — Configuración avanzada terminada
62 — Smoke real
63 — Packaging / RC
```

La razón de poner 58 antes de 55/56 en algunos ciclos es que el usuario ya definió video/storyboard como parte del flujo cuando usa imágenes. Si el equipo decide priorizar motores, puede ejecutarse 55/56 antes de 58, pero no se debe eliminar 58.

## Estado tras Tanda 50

La modernización visual base queda implementada. Las próximas tandas deben enfocarse en:

1. Documento como pantalla operativa real.
2. Flujo Word → escuchar completo.
3. Selección/asignación funcional desde UI.
4. Mini rail multimedia real.
5. Motor de voz usable y asistentes reales.
6. Video simple 2K con FFmpeg embebido.

## Tanda 57 — Streaming/prebuffer robusto

- Estado: implementada en esta base.
- Contrato: 5 fragmentos iniciales, 10 de lookahead, espera visible y continuidad automática si falta audio.
- Tests: `StreamingPlaybackWindowTest`, `StreamingPlaybackRobustnessSourceTest`.

## Tanda 58 — Video simple 2K + FFmpeg embebido

- Video simple es capacidad del producto cuando el usuario usa storyboard/imágenes.
- Resolución predeterminada: 2K (2560x1440); opciones: 720p, 1080p, 2K y 4K.
- FFmpeg se busca primero como herramienta embebida en `tools/ffmpeg/bin/ffmpeg.exe` y no debe exigir PATH global.
- Durante render, la pantalla operativa entra en modo render con progreso y bloqueo temporal de lectura/edición/nuevas exportaciones.
