# Matriz de capacidades teatrales de DocuPodcast

Fecha de corte: 20 de septiembre de 2026. Estado auditado: árbol de trabajo actual, incluidas las mejoras locales todavía no consolidadas.

## Convenciones

- **Sí**: existe un contrato explícito y operativo.
- **Parcial**: existe una aproximación, representación indirecta o flujo incompleto.
- **No**: no se encontró un contrato de producto que represente la capacidad.
- **Det.**: determinística a partir de datos declarados, sin inferencia por IA ni heurística textual.
- En **Importa** se evalúa el flujo público actual, no la mera existencia de un `UseCase` aislado.
- En **Exporta** se evalúa la posibilidad de reconstruir el mismo concepto en un contrato reimportable; exportar vídeo o una plantilla vacía no cuenta como round-trip.

| # | Capacidad | GUI | Gramática | Modelo interno | Persistente | Importa | Exporta | Det. | Propiedad, clase o campo exacto | Brecha actual |
|---:|---|:---:|:---:|:---:|:---:|:---:|:---:|:---:|---|---|
| 1 | Interfaz de edición teatral | Sí | N/A | Sí | Sí | N/A | N/A | Parcial | `TheatreCharactersPanel`, `TheatreObjectsPanel`, `TheatreSpatialActionMapPanel`, `TheatreMultimediaLayersPane`, `TheatreAudioTrackPanel` | La GUI es más rica que la gramática y no dispone de un importador único que alimente todos sus controles. |
| 2 | Gramática Markdown teatral | Sí | Sí | Sí | Parcial | Parcial | No | Parcial | `ProjectGrammarKind.THEATRE_PRODUCTION`, `TheatreGrammarMarkdownParser`, `ImportPlan` | El parser es tolerante y omite líneas/propiedades desconocidas; la exportación solo produce una plantilla, no el estado actual. |
| 3 | Modelo teatral interno | Sí | Parcial | Sí | Sí | Parcial | No | Parcial | `TheatreProjectLayer` y sus 19 colecciones | El modelo cubre producción visual, pero no todos los estados dramáticos solicitados. |
| 4 | Persistencia de proyecto | Sí | N/A | Sí | Sí | Sí | Sí* | Sí | `TheatreProjectJsonCodec`, sección JSON `theatre` | El round-trip del archivo de proyecto es completo para los campos existentes; no equivale a exportar un paquete teatral externo. |
| 5 | Importación/exportación teatral completa | Parcial | Parcial | Parcial | Parcial | Parcial | No | No | `GrammarWorkflowCoordinator`, `TheatrePackageRefreshCoordinator`, `JsonTheatrePackageScanner` | Hay dos canales separados: semántica Markdown y assets JSON. No hay import/export autocontenido único ni ZIP. |
| 6 | Personajes | Sí | Sí | Sí | Sí | Parcial | No | Parcial | `ProfilePlan`; `TheatreProjectLayer.CharacterProfile(id, displayName, aliases, notes)` | El flujo GUI de gramática crea/actualiza fichas, pero no conserva de forma tipada todas las variantes y relaciones. |
| 7 | Asignación de voces | Sí | Sí (`voz`) | Sí | Sí | Parcial | No | Parcial | `ProfilePlan.voz`; `VoiceRoleAlias.voiceProfileId`, `characterId`; `VoiceLibrary` | `TheatreImportUseCase` puede resolver una voz existente, pero el flujo público de gramática no lo invoca; una muestra `VOICE_SAMPLE` de paquete queda solo en catálogo. |
| 8 | Tonos/emociones | Parcial | Sí (`tono`) | Sí, fuera del núcleo teatral | Sí | Parcial | No | Parcial | `InterventionPlan.tono`; `NarrativeLayerAssignment` con `NarrativeLayerKind.EMOTION`; `VoiceReferenceTone` | El tono no es campo de `Intervencion`; el importador público lo conserva en sidecar, no garantiza su materialización como capa. Valores desconocidos generan advertencia o fallback. |
| 9 | Interlocución: quién habla a quién | Sí | Sí | Sí | Sí | Parcial | No | Parcial | `TextActionPlacement.characterId`, `interactionTarget`; `InterventionPlan.characterName`, `interactionTarget` | La GUI permite varios destinatarios y destinos especiales; la gramática no expresa ubicaciones individuales ni valida todas las referencias como error bloqueante. |
| 10 | Posición espacial de personajes | Sí | Parcial | Sí | Sí | Parcial | No | Parcial | `SpatialPosition(x,y)`; `TextActionPlacement.origin`, `destination`, `characterLocations`; `STAGE_LOCATIONS` | La GUI maneja nueve zonas nominales; la gramática solo declara origen/destino del hablante. El `TheatreImportUseCase` crea `SpatialPosition(0.5,0.5)` y guarda el origen en `notes`, por lo que no existe equivalencia geométrica. |
| 11 | Entradas y salidas | Parcial | No | Parcial | Sí | No | No | Parcial | Sentinel `"No presente"` dentro de `TextActionPlacement.characterLocations` | No hay evento `ENTER/EXIT`; la presencia se materializa por intervención y solo desde GUI. |
| 12 | Desplazamientos | Sí | Parcial | Sí | Sí | Parcial | No | Parcial | `TheatreAction(fromAlias,toAlias,characterId,description,showArrow)`; `TextActionPlacement.origin/destination` | No hay trayectoria, duración ni transición tipada. En importación, la acción derivada puede usar el mismo alias como origen y destino. |
| 13 | Dirección de mirada/orientación del personaje | No | No | No | No | No | No | No | No existe campo; `CameraReference.orientation` describe la cámara | La orientación de cámara no debe confundirse con mirada o pose del personaje. |
| 14 | Personajes presentes por escena/intervención | Sí | No | Parcial | Sí | No | No | Parcial | `TextActionPlacement.characterLocations`, sentinel `No presente`; `TheatreSpatialParticipantResolver` | No hay conjunto tipado de presentes. Se infiere desde ubicaciones, hablante e interlocutores y puede incluir personajes ausentes en contextos visuales. |
| 15 | Fondos/escenarios | Sí | Sí | Sí | Sí | Parcial | No | Sí dentro del proyecto | `Scene.spatialMapAssetId`; `StageBackdrop`; `StageBackdropAssignment(scope,scopeId,backdropId)`; `fondo_escenario`, `fondo`, `quitar_fondo` | El paquete de assets sí enlaza fondos; el flujo público Markdown no materializa sus asignaciones. No hay exportador de manifiesto. |
| 16 | Planos/encuadres | Sí | Sí | Sí | Sí | Parcial | No | Sí dentro del proyecto | `CameraReference(distance,orientation,height,defaultCamera)`; `CameraCue(intervencionId,cameraId)`; `plano`, `aplicar_plano` | Catálogo incorporado y resolutores existen, pero el importador GUI de Markdown no crea los cues. |
| 17 | Objetos y utilería | Sí | Sí | Sí | Sí | Parcial | No | Parcial | `TheatreObject`; `ObjectImage(objectId,sceneId,view,assetId,notes)`; `ProfilePlan` de objeto | Se modela catálogo e imagen por escena, no ubicación/estado del objeto por intervención. |
| 18 | Objetos portados/manipulados | No | No | No | No | No | No | No | No existe binding personaje–objeto–intervención | `interactionTarget` solo apunta a personajes/destinos; no representa posesión ni manipulación de utilería. |
| 19 | Vestuario/variantes visuales | Sí, parcial | Parcial | Parcial | Sí | Parcial | No | Parcial | `CharacterImage(characterId,sceneId,view,assetId,notes)` | La GUI lo llama “Vestuario ... por escena”, pero el modelo solo almacena una referencia visual con `view` y notas; no hay `costumeStateId`, variante activa ni herencia. |
| 20 | Herencia de estado entre intervenciones | Sí, manual | No | No como regla | Solo resultado copiado | No | No | No | Checkbox “Preservar las posiciones...” copia el `TextActionPlacement` anterior | No existe política declarativa, resolución de deltas ni estado base. El usuario duplica valores y pierde la semántica de herencia. |
| 21 | Coros/múltiples voces | Sí | Sí (`voces`) | Sí | Sí | Parcial | No | Parcial | `ChoralVoiceAssignment(intervencionId,participantCharacterIds,mixedAudioAssetId,sourceFingerprint,notes)` | `TheatreImportUseCase` lo construye si resuelve al menos dos personajes, pero el flujo público Markdown no lo materializa. |
| 22 | Rutas relativas a archivos | Sí, paquete | Sí, textual | Sí vía assets | Sí | Parcial | No | Sí en paquete | `ProjectAssetReference.relativePath`; `TheatrePackageEntry.relativePath`; referencias `imagen`, `mapa_espacial`, `fondo` | El scanner JSON protege el root y calcula SHA-256. En Markdown las rutas quedan referencias y no se copian/resuelven de forma integral. |
| 23 | Validación de faltantes/propiedades inválidas | Sí | Parcial | Sí, constructores | Sí, diagnósticos | Parcial | N/A | Parcial | `GrammarDiagnostic`; `JsonTheatrePackageScanner`; `PreflightTheatreRefreshUseCase`; invariantes de records | El paquete bloquea rutas/manifest inválidos, pero hoy falla la prueba del mensaje controlado para JSON malformado. La gramática emite muchas advertencias, ignora claves desconocidas y no valida assets ni todo el grafo. |
| 24 | Snapshot completo por intervención | Parcial | No | Parcial | Parcial | No | No | No | `TheatreVisualGenerationContext`; `TheatreProductionProjection`; `TheatreSpatialParticipantResolver` | Son proyecciones de render/diagnóstico, no un snapshot canónico. Faltan mirada, utilería portada, vestuario activo, entradas/salidas tipadas y resolución general de herencia. |
| 25 | Carpeta autocontenida `obra.teatro.md + assets/` | Parcial | Parcial | Parcial | Parcial | No | No | No | Markdown `theatre-v1` + manifiesto independiente `docupodcast-theatre.json` | La forma objetivo no es aceptada como una operación atómica ni como única fuente de verdad. |
| 26 | ZIP autocontenido | No | N/A | N/A | No | No | No | No | Ningún adaptador teatral ZIP; el selector usa `DirectoryChooser` | El soporte ZIP hallado pertenece a DOCX/runtimes, no a paquetes teatrales. |

\* El archivo `.docupodcast.json` exporta/persiste el proyecto nativo completo para los campos existentes mediante guardado normal. No existe un exportador de `docupodcast-theatre.json` ni de `obra.teatro.md` poblado desde el proyecto.

## Resultado de la matriz

DocuPodcast **no puede todavía** importar de forma completamente determinística una obra teatral compleja desde la forma objetivo ni desde ZIP. Sí posee tres bases aprovechables:

1. persistencia sólida del modelo teatral que ya existe;
2. parser Markdown con actos, escenas, fichas e indicaciones visuales;
3. sincronización transaccional y segura de assets desde una carpeta manifestada.

La brecha es de alineación y completitud contractual, no de ausencia total de infraestructura.
