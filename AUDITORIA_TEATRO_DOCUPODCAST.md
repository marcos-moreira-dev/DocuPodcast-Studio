# Auditoría actual de importación teatral determinística en DocuPodcast

Fecha de corte: 20 de septiembre de 2026  
Alcance: código y documentación del árbol de trabajo actual. No se implementaron cambios funcionales.

## Veredicto

**DocuPodcast todavía no puede importar de forma completamente determinística una obra teatral compleja desde `PROYECTO_TEATRO/obra.teatro.md + assets/...`, ni desde un ZIP autocontenido.**

Puede hacer, de forma determinística, partes valiosas del proceso:

- parsear una gramática Markdown teatral `theatre-v1`;
- persistir el modelo teatral existente en el proyecto nativo;
- vincular/refrescar assets desde una carpeta con manifiesto JSON, hashes y preflight transaccional;
- editar personajes, objetos, actos, escenas, voces, continuidad visual, mapas espaciales, fondos, cámaras, coros y audio en distintas superficies GUI;
- construir proyecciones de producción y contextos visuales.

No puede hoy reconstruir íntegramente, desde un único paquete externo, presencia, entradas/salidas, mirada, utilería portada, vestuario activo, herencia general y snapshot canónico por intervención. Tampoco existe exportación semántica simétrica ni adaptador ZIP teatral.

## Referencia solicitada y hallazgo

El nombre funcional `plantilla-gramatica-teatral.md` existe en `ProjectGrammarKind.THEATRE_PRODUCTION`. La plantilla que se exporta se genera dinámicamente desde:

- `src/main/java/com/marcosmoreiradev/docupodcaststudio/application/theatre/grammar/TheatreGrammarTemplate.java`

También existe una referencia histórica ampliada:

- `src/main/resources/examples/aviadores-comicos/TEATRO_GRAMATICA.md`

La auditoría no asumió que esa plantilla fuese la totalidad del producto; se contrastó con dominio, GUI, codecs, importadores y resolutores actuales.

## 1. Interfaz gráfica actual

La GUI teatral es extensa y funcionalmente más madura que la gramática:

- `TheatreCharactersPanel`: fichas, descripción, voz e imágenes de continuidad/vestuario por escena.
- `TheatreObjectsPanel`: catálogo e imágenes de objetos por escena.
- `TheatreSpatialActionMapPanel`: hablante, interlocutores, nueve ubicaciones, ausencia por intervención y copia manual de posiciones previas.
- `TheatreMultimediaLayersPane`: acceso a capas de multimedia.
- `TheatreAudioTrackPanel`: audio teatral anclado.
- `TheatreFrameSketchDialog` y storyboard: variantes oficiales, generadas y dibujadas.
- `TheatreImageGenerationWorkspaceView`: preparación y generación visual.
- coordinadores `TheatreVisualSetupCoordinator` y `TheatreChoralVoiceWorkflow`: planos, fondos y coro.

La GUI sí permite representar ausencia mediante un sentinel y copiar posiciones anteriores, pero no ofrece tipos explícitos para mirada, entrada/salida, objeto portado o estado de vestuario activo.

## 2. Gramática Markdown/importador

### Lo que reconoce el parser

`TheatreGrammarMarkdownParser` produce:

- `ImportPlan(title, acts, characters, objects, mediaLinks, interventions, voiceCatalog, toneCatalog)`;
- `ActPlan(name, notes, scenes)`;
- `ScenePlan(name, notes, textStartIndex, textEndIndex, spatialMap, stageBackdrop)`;
- `ProfilePlan(name, notes, voz, tono, imagenes)`;
- `InterventionPlan(characterName, sceneName, origin, destination, interactionTarget, images, tono, cameraCue, applyCamera, stageBackdrop, clearStageBackdrop, simultaneousVoiceNames, aiContextText, sequenceIndex, sceneIndex)`.

Admite sinónimos para mapa, fondo, plano, aplicación de cámara y voces simultáneas. También extrae los comentarios `VOZ_CATALOGO` y `TONO_CATALOGO`.

### Limitaciones del parser

- No retiene el texto pronunciado dentro de `InterventionPlan`; enlaza las intervenciones por orden con segmentos de la narración construida desde el Markdown.
- Ignora en general propiedades o líneas no reconocidas.
- Convierte enteros/booleanos inválidos a defaults en varios casos.
- Personaje, escena, tono y cámara desconocidos suelen producir advertencias, no un rechazo estricto.
- No expresa presencia completa, entradas/salidas, mirada, objetos portados, vestuario activo ni herencia.

### Hallazgo crítico del importador público

El comando real pasa por `GrammarWorkflowCoordinator`:

1. parsea el Markdown;
2. importa el propio Markdown como documento fuente;
3. ejecuta `applyTheatreGrammarPlan`, que solo aplica personajes, objetos, actos y escenas;
4. construye la narración;
5. escribe `project-semantics.json` mediante `ImportProjectGrammarMarkdownUseCase`.

El caso de uso `TheatreImportUseCase`, que sí sabe construir `TextActionPlacement`, `CameraCue`, `StageBackdropAssignment`, `ChoralVoiceAssignment`, imágenes y capas de emoción, solo aparece invocado por `TheatreExampleSetupService`.

Por tanto, el parser actual **describe más de lo que el importador público materializa**.

## 3. Modelo de datos interno

`TheatreProjectLayer` contiene 19 colecciones persistentes:

1. `intervenciones`
2. `characters`
3. `voiceRoleAliases`
4. `characterImages`
5. `intervencionesVisuales`
6. `intermediateFrames`
7. `acts`
8. `scenes`
9. `positions`
10. `actions`
11. `textActionPlacements`
12. `objectImages`
13. `objects`
14. `audioTracks`
15. `cameraReferences`
16. `cameraCues`
17. `stageBackdrops`
18. `stageBackdropAssignments`
19. `choralVoiceAssignments`

El modelo es fuerte para organización, referencias visuales y render, pero no tiene entidades tipadas para:

- entrada/salida;
- mirada/orientación corporal;
- conjunto canónico de presentes;
- colocación y estado de objetos por intervención;
- objeto portado/manipulado;
- variante de vestuario activa;
- herencia declarativa de estado.

## 4. Persistencia del proyecto

`TheatreProjectJsonCodec` lee y escribe las 19 colecciones del modelo. El archivo de proyecto nativo sí hace round-trip de los campos actuales, incluidos cámaras, fondos, coros, posiciones, acciones, objetos y audio teatral.

La persistencia no es el cuello de botella principal. El problema es que el contrato externo no puede poblar todos esos campos y que varios conceptos solicitados aún no existen en el modelo.

`project-semantics.json` es un sidecar de bindings/diagnósticos. No sustituye a `TheatreProjectLayer` y no es la fuente operativa completa de la GUI o el render teatral.

## 5. Importación y exportación

### Canal Markdown

- Entrada: archivo `.md` seleccionado por `FileChooser`.
- Salida interna: documento fuente, una parte del modelo teatral y sidecar semántico.
- Exportación: plantilla estática vacía, no serialización del proyecto.

### Canal de paquete teatral

- Entrada: carpeta seleccionada por `DirectoryChooser`.
- Requisito: `docupodcast-theatre.json`, `schemaVersion: 1`.
- Scanner: `JsonTheatrePackageScanner`.
- Seguridad: rutas relativas dentro del root, rechazo de traversal/absolutas/symlinks externos, tamaño y SHA-256.
- Commit: preflight y reconciliación transaccional.

Tipos enlazables:

- `CHARACTER_IMAGE`
- `OBJECT_IMAGE`
- `BACKDROP`
- `SPATIAL_MAP`
- `INTERVENTION_IMAGE`
- `INTERMEDIATE_FRAME`
- `HUMAN_AUDIO`

`VOICE_SAMPLE`, `VIDEO` y `OTHER` quedan solo en catálogo hasta que exista un binding de dominio explícito.

No existe exportador de `docupodcast-theatre.json`, ni empaquetador, ni lector ZIP teatral.

## 6–21. Estado de las capacidades dramáticas

### Personajes, voz y tono

Personajes y aliases se modelan correctamente. La voz se representa con `VoiceRoleAlias`, pero debe resolver contra una `VoiceLibrary` existente. El tono se representa como capa narrativa `EMOTION`, no dentro de `Intervencion`. La gramática puede declararlos; el flujo público no garantiza materializarlos.

### Interlocución y coro

`TextActionPlacement.interactionTarget` representa destinatarios y destinos especiales. La GUI admite múltiples selecciones. `ChoralVoiceAssignment` representa coros, participantes, mezcla y fingerprint. Ambos son persistentes, pero su importación desde Markdown no está conectada al flujo público completo.

### Posición, presencia y movimiento

Existen dos formas:

- `SpatialPosition` con coordenadas `x/y`;
- `TextActionPlacement` con zonas nominales y mapa de ubicaciones.

La GUI trabaja principalmente con la segunda. Ausencia es un string especial en `characterLocations`. Movimiento se aproxima con origen/destino y `TheatreAction`; no hay trayectoria ni evento de entrada/salida.

### Fondos y planos

Fondos y cámaras están bien modelados y tienen resolutores por escena/intervención. Son determinísticos cuando ya están en el proyecto. La brecha está en importarlos/exportarlos simétricamente.

### Objetos y vestuario

Los objetos tienen catálogo e imágenes por escena. No tienen estado espacial/posesión por intervención. El “vestuario” de GUI es una `CharacterImage` por escena con notas; no es una variante tipada ni seleccionable como estado activo.

### Herencia

Solo hay copia manual desde la intervención previa en la GUI. La relación de herencia no se guarda.

## 22. Rutas relativas

El paquete JSON es el subsistema más sólido en este punto. `JsonTheatrePackageScanner` normaliza rutas, exige que permanezcan dentro del root real y calcula SHA-256. Los assets materializados se almacenan como `ProjectAssetReference.relativePath`.

Las rutas mencionadas solo en Markdown no reciben el mismo tratamiento integral. `GrammarWorkflowCoordinator` informa que los links visuales “quedan como referencias en esta importación”.

## 23. Validación

Fortalezas:

- manifiesto JSON con versión;
- IDs/rutas del paquete validados;
- staging y preflight antes del commit;
- invariantes en records del dominio;
- diagnóstico de referencias de producción mediante `BuildTheatreProductionProjectionUseCase`;
- integridad de assets del proyecto.

Debilidades:

- gramática permisiva y no versionada en modo estricto;
- propiedades desconocidas silenciosas;
- advertencias donde un paquete reproducible requeriría errores;
- validación dividida entre sidecar, dominio y preflight;
- no se valida el grafo completo Markdown + manifiesto como una unidad.

## 24. Snapshot completo por intervención

No existe todavía.

Las aproximaciones actuales son:

- `TheatreProductionProjection`: preparación de producción, enlaces, disponibilidad de audio/visual y diagnósticos.
- `TheatreVisualGenerationContext`: referencias de identidad, objetos de la escena, entorno, frames adyacentes, guía de cámara y personajes inferidos.
- `TheatreSpatialParticipantResolver`: participantes y ubicaciones para vídeo/mapa.

Ninguna incluye todo el estado requerido. Además, las listas de participantes se derivan mediante reglas distintas. Falta una función pura y canónica que resuelva por intervención todos los personajes, presencia, ubicación, orientación, vestuario, objetos, portadores, voz, tono, fondo y cámara.

## Riesgos de determinismo

1. **Pérdida silenciosa:** una propiedad Markdown reconocida puede quedar solo en el sidecar.
2. **Inferencia por orden:** las intervenciones se asocian a segmentos por `sequenceIndex`.
3. **Duplicidad espacial:** zonas nominales y coordenadas sin conversión oficial.
4. **Sentinels textuales:** ausencia codificada como string.
5. **Contextos diferentes:** render, mapa y generación visual no parten de un snapshot común.
6. **Assets separados:** semántica y archivos se importan por operaciones distintas.
7. **No round-trip:** no hay exportador semántico poblado desde el proyecto.
8. **Sin ZIP:** no existe staging/extracción teatral con las mismas garantías del scanner de carpeta.

## Evidencia principal revisada

- `application/theatre/grammar/TheatreGrammarMarkdownParser.java`
- `application/theatre/grammar/TheatreGrammarTemplate.java`
- `domain/theatre/plan/*`
- `application/theatre/TheatreImportUseCase.java`
- `application/grammar/ImportProjectGrammarMarkdownUseCase.java`
- `presentation/shell/workflow/GrammarWorkflowCoordinator.java`
- `domain/theatre/TheatreProjectLayer.java`
- `infrastructure/json/TheatreProjectJsonCodec.java`
- `infrastructure/theatrepackage/JsonTheatrePackageScanner.java`
- `application/theatrepackage/ReconcileTheatreProjectUseCase.java`
- `presentation/shell/workflow/TheatrePackageRefreshCoordinator.java`
- `application/theatre/BuildTheatreProductionProjectionUseCase.java`
- `application/theatre/BuildTheatreVisualGenerationContextUseCase.java`
- `application/video/TheatreSpatialParticipantResolver.java`
- paneles bajo `presentation/theatre/`
- `docs/theatre-package-format.md`

## Verificación ejecutada

Se ejecutó una selección de 23 pruebas centradas en parser, importación semántica, persistencia JSON, scanner/reconciliación/refresco de paquete y proyección de producción. Resultado: **22 aprobadas y 1 fallida**.

La falla actual es `JsonTheatrePackageScannerTest.reportsMalformedManifestAsAControlledInputError`: el scanner sí lanza `IOException` ante un manifiesto malformado, pero el mensaje no contiene el contrato esperado `JSON inválido`. Esto no invalida la detección del archivo defectuoso, pero sí demuestra una divergencia entre la semántica de error probada y la implementación actual. Se documenta; no se corrigió por el alcance de esta auditoría.

## Entregables relacionados

- `MATRIZ_CAPACIDADES_TEATRO.md`: evaluación capacidad por capacidad.
- `BRECHAS_GRAMATICA_GUI_MODELO.md`: divergencias exactas entre superficies.
- `PROPUESTA_MINIMA_ALINEACION_TEATRO.md`: camino mínimo sin rediseñar lo que ya funciona.

## Conclusión final

La base actual es suficiente para evolucionar sin rehacer DocuPodcast. La persistencia, el scanner seguro y buena parte del dominio ya existen. La prioridad futura debe ser **alinear y hacer simétricos** los contratos actuales antes de añadir más controles: un importador atómico, campos mínimos faltantes, snapshot canónico y exportación reimportable.
