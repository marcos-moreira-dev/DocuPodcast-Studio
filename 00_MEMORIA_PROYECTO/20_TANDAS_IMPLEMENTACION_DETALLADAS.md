## Actualización Tanda 9

Completada la robustez inicial del gateway TTS real: reintentos configurables, diagnóstico JSONL por intento y visualización en Audio Workspace.

# Tandas de implementación detalladas

Este plan reemplaza la necesidad de reabrir toda la conversación. Cada tanda debe dejar código, documentación y tests mínimos.

## Tanda 1 — Onboarding técnico y scaffolding inicial

### Objetivo

Crear un repositorio JavaFX mínimo, con pantalla de bienvenida, tema claro, estructura por capas y documentación viva suficiente para continuar.

### Incluye

- `DocuPodcastStudioApp`.
- `ApplicationBootstrap`.
- `ApplicationRuntime`.
- `ApplicationWindowConfig`.
- `PresentationCompositionRoot` inicial.
- Shell mínimo.
- Welcome workspace.
- Toolbar inicial con “Abrir Word/DOCX”.
- Placeholders para Documento, Guion, Storyboard, Audio, Voces, Observabilidad y Configuración.
- CSS tokenizado básico.
- README, AI_HANDOFF y memoria viva.
- Maven Java 21 + Toolchain Temurin.

### Criterio de aceptación

- `mvn test` pasa localmente.
- `mvn javafx:run` abre la app.
- La pantalla de inicio muestra Word/DOCX como entrada prioritaria.
- Existe carpeta raíz `00_MEMORIA_PROYECTO` con documentación extensa.

---

## Tanda 2 — Proyecto `.docupodcast.json` mínimo

### Objetivo

Implementar la persistencia mínima del proyecto editable.

### Entregables

- `DocuPodcastProject`.
- `ProjectMetadata`.
- `ProjectKind`.
- `ProjectStatus`.
- `ProjectAssetCatalog`.
- `ProjectAssetReference`.
- `ProjectAssetKind`.
- `ProjectRepository`.
- `DocuPodcastProjectFileRepository`.
- `DocuPodcastProjectJsonReader`.
- `DocuPodcastProjectJsonWriter`.
- `DocuPodcastProjectFormat` con `formatVersion`.
- Validación de rutas relativas.

### UI

- Nuevo proyecto.
- Guardar.
- Guardar como.
- Abrir proyecto.
- Dirty state básico en título.

### Tests

- roundtrip JSON;
- rechazar rutas absolutas;
- rechazar versión futura;
- cargar proyecto mínimo;
- guardar/abrir sin perder metadata.

---

## Tanda 3 — Shell multiproyecto, tabs y workspace registry

### Objetivo

Adoptar la carcasa DMS: tabs, workspaces registrados y routing.

### Entregables

- `WorkspaceKind` completo.
- `WorkspaceRoute`.
- `WorkspaceRouteResolver`.
- `WorkspaceViewRegistry`.
- `EditorTabViewState`.
- `ScrollableEditorTabBarView`.
- `EditorTabCellView`.
- `ProjectSession`.
- `ProjectSessionCoordinator`.

### Workspaces registrados

- Welcome.
- Documento.
- Guion.
- Storyboard.
- Audio.
- Voces.
- Observabilidad.

### Tests

- activar tab;
- cerrar tab;
- dirty state;
- no mover tab home;
- workspace route correcto.

---

## Tanda 4 — Toolbar global/contextual

### Objetivo

Implementar un sistema de acciones visibles por workspace sin promesas falsas.

### Entregables

- `WorkspaceToolbarAction`.
- `WorkspaceToolbarActionId`.
- `WorkspaceToolbarSection`.
- `WorkspaceToolbarContributor`.
- `WorkspaceToolbarContributorRegistry`.
- `DefaultWorkspaceToolbarActionProvider`.
- `WorkspaceToolbarActionExecutor`.
- `WorkspaceCapabilityPresentationPolicy`.

### Acciones iniciales

- Abrir documento.
- Guardar.
- Crear guion.
- Generar audio mock.
- Abrir voces.
- Importar imagen placeholder.

### Tests

- ninguna acción visible sin handler;
- no mostrar MP3 si no existe exporter;
- no mostrar emoción si motor no soporta estilos;
- Word/DOCX visible globalmente.

---

## Tanda 5 — SideDock y workspaces estructurados

### Objetivo

Implementar SideDock modular y carcasa de workspaces estructurados.

### Entregables

- `WorkspaceSideDock`.
- `SideDockModule`.
- `SideDockModuleId`.
- `SideDockContext`.
- `SideDockModuleRegistry`.
- `SideDockStatePolicy`.
- `StructuredWorkspaceView`.
- `WorkspaceHeaderView`.
- `WorkspacePanelSupport`.

### Módulos iniciales

- Documento: Estructura, Propiedades, Diagnóstico, Ayuda.
- Guion: Segmentos, Propiedades, Personajes/Voces, Validación, Ayuda.
- Audio: Jobs, Logs, Motor, Ayuda.

### Tests

- un único módulo activo;
- conservar módulo si sigue disponible;
- no doble scroll;
- mínimo visual para listas.

---

## Tanda 6 — Importación Word/DOCX

### Objetivo

Hacer real el flujo principal del producto: abrir notas Word.

### Entregables

- `DocxDocumentImporter`.
- `ReadableDocument`.
- `DocumentBlock`.
- `DocumentBlockType`.
- `DocumentImageNotice`.
- `DocumentTableNotice`.
- `ReadingProfile`.
- `HeadingDetectionRules`.
- `ImportDocumentUseCase`.
- `AnalyzeDocumentStructureUseCase`.

### UI

- Abrir DOCX.
- Mostrar documento con scroll.
- Panel estructura.
- Diagnóstico de importación.

### Tests

- párrafos;
- títulos por estilos;
- subtítulos;
- listas simples;
- tablas simples;
- imágenes y alt text si existe;
- no modificar DOCX original.

---

## Tanda 7 — Perfil de lectura

### Objetivo

Permitir configurar qué se considera título, subtítulo, imagen narrable o bloque ignorado.

### Entregables

- `ReadingProfileDialog`.
- reglas por estilo Word;
- reglas por tamaño/negrita/numeración;
- política de imágenes;
- política de tablas;
- preview de clasificación.

### Tests

- clasificación por estilos;
- fallback por formato;
- imagen sin descripción;
- cambio de perfil recalcula estructura sin romper documento.

---

## Tanda 8 — Guion narrable

### Objetivo

Crear el artefacto central: `NarrationScriptDocument`.

### Entregables

- `NarrationScriptDocument`.
- `NarrationSegment`.
- `ScriptSection`.
- `ScriptSelection`.
- `BuildNarrationScriptUseCase`.
- `ScriptWorkspaceView`.
- `ScriptSegmentsPanel`.
- `ScriptPropertiesPanel`.
- `ScriptValidationPanel`.

### Funciones

- Crear guion desde documento.
- Editar segmento.
- Dividir/unir segmento.
- Ignorar segmento.
- Validar segmentos vacíos o muy largos.

### Tests

- IDs estables;
- segmentos desde párrafos;
- título genera frase de transición;
- guion no usa canvas;
- selección por segmento.

---

## Tanda 9 — Audio mock y progreso real de UI

### Objetivo

Construir la UI y arquitectura de jobs sin depender todavía del TTS real.

### Entregables

- `AudioJob`.
- `AudioJobState`.
- `AudioJobStatusDto`.
- `AudioGenerationPlan`.
- `AudioGenerationGateway`.
- `MockAudioGenerationGateway`.
- `AudioJobQueueView`.
- `AudioJobsPanel`.
- Progress bar + ETA.
- Logs por job.

### Tests

- progreso por segmento;
- ETA después de algunos segmentos;
- cancelación cooperativa;
- mock genera WAV falso o archivo dummy;
- UI no se bloquea.

---

## Tanda 10 — Persistencia de jobs y reanudación

### Objetivo

Guardar estado de generación y permitir reanudar.

### Entregables

- `AudioJobFileRepository`.
- `segments-status.json`.
- `audio-manifest.json`.
- `generation-log.jsonl`.
- `RetryFailedSegmentsUseCase`.
- `ResumeAudioJobUseCase`.

### Tests

- cancelar conserva segmentos completados;
- reabrir proyecto restaura job;
- reintentar fallidos;
- no regenerar segmentos completados.

---

## Tanda 11 — Motor TTS real

### Objetivo

Conectar un motor de voz real detrás de `AudioGenerationGateway`.

### Opciones

- Python empaquetado con XTTS si da mejor calidad.
- Piper como alternativa rápida.
- ONNX Runtime Java si resulta viable.

### Entregables

- `VoiceProfile`.
- `VoiceEngineCapability`.
- `TtsEngineGateway`.
- generación WAV por segmento;
- manejo de errores del motor;
- diagnóstico de motor.

### Tests

- contrato gateway con fake;
- real opcional marcado como integration/manual;
- no UI directa al motor.

---

## Tanda 12 — Voces/personajes/estilos

### Objetivo

Permitir asignar voces a segmentos/personajes y estilos como intención.

### Entregables

- `VoiceProfile`.
- `VoiceProfileType`.
- `CharacterProfile`.
- `PerformanceStyle`.
- `VoiceLibraryWorkspace`.
- `AssignVoiceToSegmentUseCase`.
- `AssignStyleToSegmentUseCase`.

### Tests

- voz prediseñada;
- voz propia;
- voz autorizada;
- estilo no soportado se deshabilita o advierte;
- notas de consentimiento.

---

## Tanda 13 — Storyboard vivo MVP

### Objetivo

Una imagen por segmento + preview visual.

### Entregables

- `MediaAsset`.
- `StoryboardDocument`.
- `StoryboardBinding`.
- `StoryboardWorkspaceView`.
- `StoryboardCanvasAdapter`.
- `StoryboardRenderKit`.
- `BindImageToSegmentUseCase`.
- `MediaLibraryPanel`.

### Tests

- binding requiere segmento existente;
- binding requiere imagen existente;
- canvas no crea conectores;
- layout visual no cambia orden narrativo.

---

## Tanda 14 — Playback sincronizado

### Objetivo

Reproducir audio resaltando segmento e imagen.

### Entregables

- `PlaybackManifest`.
- `PlaybackCue`.
- `PlaybackCursor`.
- `PlaybackControlBar`.
- `ActiveCueHighlighter`.
- sincronización Script/Storyboard.

### Tests

- cue conecta segmento/audio/imagen;
- reproducción cambia segmento activo;
- storyboard resalta escena;
- guion resalta segmento.

---

## Tanda 15 — Exportaciones

### Objetivo

Exportar artefactos reales sin promesas falsas.

### Entregables

- exportar guion Markdown;
- exportar WAV final;
- exportar MP3 si encoder disponible;
- exportar proyecto bundle;
- exportar reporte diagnóstico;
- exportar storyboard preview PNG.

### Tests

- no ofrecer MP3 sin encoder;
- no exportar podcast completo si faltan segmentos;
- bundle incluye DOCX fuente;
- manifest de exportación.

---

## Tanda 16 — Guía integrada y recursos IA

### Objetivo

Documentar el uso dentro de la app y permitir exportar plantillas/ejemplos IA.

### Entregables

- `GuideDialog`.
- temas Markdown de ayuda.
- `ExportAiResourcesUseCase`.
- `docupodcast-script-v1`.
- ejemplos académico/teatro/storyboard.

### Tests

- guía carga temas;
- Word aparece antes que Markdown;
- ejemplos importables pasan parser;
- plantillas con placeholders no son importables.

---

## Tanda 17 — Empaquetado Windows

### Objetivo

Generar app-image/MSI con JavaFX y runtime adecuado.

### Entregables

- scripts de jpackage;
- app icon;
- verificación de toolchain;
- documentación de release.

### Tests/manual

- app abre desde app-image;
- MSI instala;
- ruta de storage válida;
- logs se crean correctamente.


## Actualización posterior a Tanda 2

Se implementó la base `.docupodcast.json` + assets relativos. Antes del importador DOCX se agrega una tanda intermedia:

```text
Tanda 2.5 — Integración UI de proyecto/session
```

Razón: la capa interna de persistencia ya existe, pero el usuario todavía no puede crear, abrir, guardar ni cerrar proyectos desde la interfaz. Esa sesión es necesaria antes de cargar Word/DOCX en un proyecto real.

Después de Tanda 2.5 continúa:

```text
Tanda 3 — Importador Word/DOCX real
```

## Tanda 3 parcial — DOCX operativo

Adelantada parcialmente: existe importer DOCX mínimo, workspace documental y materialización `source/` + `document/document.json` al guardar. Falta estructura lateral, diagnóstico y perfil de lectura.


## Tanda 3/4 — DOCX diagnóstico y Document Workspace

Se implementó el cierre de la primera versión del importador DOCX y se avanzó el workspace Documento con SideDock mínimo: Estructura, Propiedades y Diagnóstico. El JSON documental materializado ahora incluye resumen, metadatos e issues de importación.


## Tanda 3 cerrada / Tanda 4 avanzada

Tanda 3 queda cerrada en primera versión: DOCX importer con diagnóstico. Tanda 4 queda avanzada: Document Workspace con SideDock inicial, pendiente de extracción de paneles y filtros.


## Actualización — Tanda 3 cerrada y Tanda 4 avanzada

- Tanda 3: importador DOCX mejorado con orden de cuerpo, listas, tablas, imágenes, metadata y diagnósticos.
- Tanda 4: workspace Documento avanzado con estructura lateral, diagnóstico, métricas y selección de bloques.
- Validación parcial: compilación `javac --release 21` de domain/application/infrastructure sin JavaFX.
- Validación completa pendiente en entorno local con Maven Toolchain Temurin 21.


## Actualización Tanda 4 / avance Tanda 5

- Tanda 4 implementada: Document Workspace modular con SideDock reutilizable, filtros y acciones manuales de bloque.
- Tanda 5 avanzada: dominio Reading Profile, perfil académico Word-first y aplicación de reglas sobre bloques.
- Sigue pendiente cerrar Tanda 5 con editor/persistencia de perfiles y puente formal hacia guion narrable.

## Actualización posterior — Tanda 5B implementada

Reading Profile queda cerrado en primera versión: editor visible, persistencia en `.docupodcast.json`, previsualización no destructiva y aplicación al documento importado. La siguiente tanda recomendada es **Tanda 6 — Guion narrable**, donde se construirá `NarrationScriptDocument` desde los bloques ya clasificados.

Tandas pendientes actualizadas:

1. Tanda 6 — Guion narrable.
2. Tanda 7 — Audio job mock con progreso/ETA.
3. Tanda 8 — Gateway TTS real.
4. Tanda 9 — Reanudación de jobs.
5. Tanda 10 — Voice Library.
6. Tanda 11 — Storyboard básico.
7. Tanda 12 — Playback sincronizado.
8. Tanda 13 — Exportaciones.
9. Tanda 14 — Guía integrada + recursos IA.
10. Tanda 15 — Packaging / release candidate.

## Estado tras Tanda 6

Tanda 6 implementada: Guion narrable.

Ahora existe:

```text
ReadableDocument → NarrationScriptDocument
```

Pendientes principales:

```text
Tanda 7: Audio job mock con progreso/ETA.
Tanda 8: Gateway TTS real.
Tanda 9: Reanudación de jobs.
Tanda 10: Voice Library.
Tanda 11: Storyboard básico.
Tanda 12: Playback sincronizado.
Tanda 13: Exportaciones.
Tanda 14: Guía integrada + recursos IA.
Tanda 15: Packaging / release candidate.
```

## Estado tras Tanda 7

La Tanda 7 queda implementada como audio mock. La siguiente tanda recomendada es `Tanda 8 — Gateway TTS real`, con posibilidad de insertar una `Tanda 7B — Persistencia temprana de jobs` si se decide robustecer reanudación antes del TTS real.

---

## Actualización Tanda 7B — Persistencia temprana de jobs de audio

Se implementó persistencia temprana de jobs de audio sobre el mock:

- `AudioJobSnapshot` y `AudioSegmentSnapshot`.
- `AudioJobRepository` + `AudioJobFileRepository`.
- Escritura de `jobs/JOB-*/job.json`.
- Escritura de `jobs/JOB-*/segments-status.json`.
- El `MockAudioGenerationGateway` persiste estado durante la generación.
- El workspace Audio muestra historial persistido.
- Al reabrir proyecto, la UI puede recuperar el último estado persistido sin tratarlo como job activo fantasma.

Queda agregada como recomendación la **Tanda 7C opcional — Recuperación operativa/reintento de jobs persistidos**, antes de conectar TTS real si se desea máxima robustez.

Tandas pendientes actualizadas:

```text
Tanda 7C opcional — Recuperación operativa de jobs persistidos
Tanda 8           — Gateway TTS real
Tanda 9           — Reanudación avanzada de jobs
Tanda 10          — Voice Library
Tanda 11          — Storyboard básico
Tanda 12          — Playback sincronizado
Tanda 13          — Exportaciones
Tanda 14          — Guía integrada + recursos IA
Tanda 15          — Packaging / release candidate
```



## Actualización tras Tanda 7C

La recuperación operativa de jobs persistidos ya está implementada. Se agregó una posible Tanda 7D opcional para UI de grabación/voz humana/playback clicable.

Queda pendiente explícito:

- seleccionar rango de texto y elegir voz IA o humana;
- asociar esa selección a imagen;
- grabar/importar audio humano para texto;
- usar audio humano como fuente para Whisper/STT;
- pausar y reanudar reproducción desde cualquier línea/segmento clicado.

## Actualización tras Tanda 7D

Se completó la tanda opcional de placeholders para selección de texto, voz IA/TTS, voz humana, Whisper/STT futuro y playback clicable.

Pendiente actualizado:

```text
Tanda 8           — Gateway TTS real
Tanda 9           — Reanudación avanzada de jobs con motor real
Tanda 10          — Voice Library
Tanda 11          — Storyboard básico
Tanda 12          — Playback sincronizado real
Tanda 13          — Exportaciones
Tanda 14          — Guía integrada + recursos IA
Tanda 15          — Packaging / release candidate
```


## Actualización de mapa tras Tanda 8

La Tanda 8 queda implementada como gateway TTS real por proceso local. No incluye modelos ni binarios pesados.

Tandas restantes:

1. Tanda 9 — Reanudación avanzada de jobs con motor real y diagnóstico.
2. Tanda 10 — Voice Library.
3. Tanda 11 — Storyboard básico.
4. Tanda 12 — Playback sincronizado real.
5. Tanda 13 — Exportaciones.
6. Tanda 14 — Guía integrada + recursos IA.
7. Tanda 15 — Packaging / release candidate.


## Actualización tras Tanda 10

Voice Library implementada. El guion puede asignar personaje, voz y estilo por segmento usando una biblioteca persistida en el proyecto.

Queda:

- Tanda 10B opcional — muestras de voz.
- Tanda 11 — Storyboard básico.
- Tanda 12 — Playback sincronizado real.
- Tanda 13 — Exportaciones.
- Tanda 14 — Guía integrada + recursos IA.
- Tanda 15 — Packaging / RC.


## Tanda 10B — Implementada

Se implementó importación de muestras de voz para `VOC-OWN-PLACEHOLDER`, con `VOICE_SAMPLE`, rutas relativas y checksum. Pendientes: grabación real, normalización, selección de perfil destino y uso directo por TTS/Whisper.


### Tanda 11 — Storyboard básico — IMPLEMENTADA

Se implementó el manifiesto de storyboard, importación de imágenes, asociación imagen-segmento y workspace Storyboard. El canvas avanzado queda para una mejora posterior; el MVP usa tarjetas visuales.


## Actualización Tanda 12

El playback ya no es solo placeholder: se construye desde datos persistidos y puede reproducir por segmento usando `SegmentAudioPlayer`. Queda pendiente mejorar UI de reproductor, alineación fina y exportaciones.

## Actualización tras Tanda 13

Exportaciones iniciales implementadas:

- Guion Markdown.
- Podcast WAV.
- Reporte diagnóstico Markdown.
- Paquete completo de proyecto.

Queda pendiente:

```text
Tanda 14 — Guía integrada + recursos IA
Tanda 15 — Packaging / release candidate
```


## Actualización tras Tanda 14

La guía integrada y recursos IA quedaron implementados. Word/DOCX permanece como entrada prioritaria. Markdown queda como puente IA/humano.

Pendiente: Tanda 15 — Packaging / release candidate.

## Hotfix scripts Maven root-safe

Se corrigieron los scripts para que funcionen tanto desde la raíz del repositorio como desde la carpeta `scripts\`. Todos los scripts que ejecutan Maven ahora hacen `pushd` a la raíz y usan `call mvn`, evitando el error `MissingProjectException` cuando se ejecutan desde `scripts\`.

También se añadió `ScriptsRootSafeSourceTest` como guardarraíl.



## Hotfix tests Windows v2

Estabilización previa a Tanda 15: tests locales reportaron cinco fallos y se corrigieron sin introducir nueva funcionalidad.

## Cierre Tanda 15

Tanda 15 completada: packaging/release candidate. La siguiente tanda no obligatoria pero recomendada es Tanda 16 — polish visual/UX.


## Tanda 16 — Polish visual / UX

- Controles visibles de ventana.
- Ventana centrada y ajustada a pantalla.
- Toolbar contextual por workspace.
- Statusbar con retroalimentación más clara.
- Pantalla de inicio con azul suave y tarjetas refinadas.
