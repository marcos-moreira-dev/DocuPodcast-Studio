# Brechas entre gramática, GUI, modelo e importador teatral

Fecha de corte: 20 de septiembre de 2026.

## Resumen ejecutivo

La GUI, la gramática y el modelo interno no son hoy tres vistas equivalentes del mismo contrato. La GUI permite editar más estado espacial y visual que el Markdown; el parser reconoce más propiedades que las que el flujo público materializa; y el modelo persiste más campos que los que pueden importarse o exportarse mediante la gramática.

## Flujo real actual

```text
Markdown teatral
  -> TheatreGrammarMarkdownParser
  -> ImportPlan
  -> GrammarWorkflowCoordinator.applyTheatreGrammarPlan
       -> fichas de personaje/objeto + actos + escenas
  -> project-semantics.json
       -> conserva metadatos de intervenciones

Carpeta teatral
  -> docupodcast-theatre.json + assets/
  -> JsonTheatrePackageScanner
  -> preflight/refresh transaccional
  -> ReconcileTheatreProjectUseCase
       -> imágenes, fondos, mapas, audio humano y frames puente

Proyecto nativo
  -> TheatreProjectLayer
  -> TheatreProjectJsonCodec
  -> .docupodcast.json
```

El flujo que sí construye muchas entidades a partir de `ImportPlan`, `TheatreImportUseCase`, solo se usa actualmente desde `TheatreExampleSetupService`. No aparece conectado al comando público de importar gramática.

## Divergencias críticas

### 1. El parser reconoce propiedades que el importador público no aplica

`TheatreGrammarMarkdownParser` produce `InterventionPlan` con origen, destino, interlocutor, tono, plano, fondo, coro y contexto IA. `ImportProjectGrammarMarkdownUseCase` copia esos valores a `project-semantics.json`. Sin embargo, `GrammarWorkflowCoordinator.applyTheatreGrammarPlan` solo crea o actualiza perfiles, actos y escenas.

Consecuencia: un usuario puede importar un Markdown válido, recibir un informe que enumera intervenciones, cámaras o fondos, y aun así no obtener `TextActionPlacement`, `CameraCue`, `StageBackdropAssignment`, `ChoralVoiceAssignment` ni capas de emoción equivalentes en `TheatreProjectLayer`.

### 2. La exportación de gramática no es un round-trip

`EXPORT_THEATRE_GRAMMAR_TEMPLATE` llama a `BuildGrammarTemplateUseCase` y escribe el texto fijo de `TheatreGrammarTemplate.markdown()`. No serializa el proyecto abierto.

Consecuencia: no se puede exportar una obra editada en GUI, reimportarla y obtener el mismo estado.

### 3. Markdown y paquete de assets son contratos independientes

La gramática referencia rutas en campos como `imagen`, `mapa_espacial` y `fondo`. El paquete vinculado exige `docupodcast-theatre.json` y aplica assets mediante `ReconcileTheatreProjectUseCase`. No hay coordinador que lea `obra.teatro.md`, valide sus rutas contra el manifiesto y confirme ambos cambios en una sola transacción.

Consecuencia: la forma objetivo `obra.teatro.md + assets/...` no es todavía un paquete importable atómico.

### 4. La posición tiene dos representaciones no equivalentes

- GUI: zonas nominales en `TextActionPlacement.origin`, `destination` y `characterLocations`.
- Modelo espacial heredado: `SpatialPosition.x/y`.
- Gramática: solo origen/destino del hablante.
- `TheatreImportUseCase`: crea `SpatialPosition` en `(0.5, 0.5)` y deja el origen nominal en `notes`.

Consecuencia: no existe una conversión canónica entre los nueve cuadrantes y las coordenadas; un mismo archivo no garantiza la misma ubicación en todos los consumidores.

### 5. Presencia no es una entidad del dominio

La GUI añade `"No presente"` o `"No presente en esta intervencion"` como valor dentro de `characterLocations`. `TheatreSpatialParticipantResolver` interpreta ese sentinel. La gramática no lo expresa y `BuildTheatreVisualGenerationContextUseCase` recorre las claves de `characterLocations` sin un contrato de presencia independiente.

Consecuencia: diferentes proyecciones pueden formar conjuntos de participantes distintos. Tampoco existen eventos tipados de entrada/salida.

### 6. Vestuario se ofrece en GUI, pero no existe como estado tipado

`TheatreCharactersPanel` denomina la sección “Vestuario de personaje por escena”. Lo persistido es `CharacterImage(characterId, sceneId, view, assetId, notes)`.

Consecuencia: no se distingue de forma determinística entre pose, ángulo, vestuario, maquillaje o variante identitaria, ni se selecciona una variante activa por intervención.

### 7. Objetos están catalogados, pero no escenificados

`TheatreObject` y `ObjectImage` describen utilería y sus imágenes por escena. No existe una colocación de objeto, vínculo de portador, mano, acción de tomar/soltar ni estado por intervención.

Consecuencia: el contexto visual incluye objetos disponibles en la escena, no necesariamente los presentes o manipulados en ese instante.

### 8. La herencia es una acción de GUI, no una regla reproducible

El checkbox “Preservar las posiciones de los personajes del texto anterior” copia valores del `TextActionPlacement` anterior al formulario actual. No persiste que el estado fuese heredado ni qué campos eran deltas.

Consecuencia: el resultado guardado es estable, pero no se puede reconstruir la intención de herencia ni recalcularla si cambia una intervención previa.

### 9. Voz y tono cruzan agregados diferentes

- Voz de personaje: `VoiceRoleAlias` enlaza a un `VoiceProfile` existente.
- Tono: capa `NarrativeLayerAssignment` de clase `EMOTION` y muestras de `VoiceLibrary`.
- Gramática: `voz` y `tono` son strings.
- Paquete: `VOICE_SAMPLE` se escanea pero `ReconcileTheatreProjectUseCase` lo deja “catalog-only”.

Consecuencia: un paquete no puede garantizar por sí solo que la voz o la muestra tonal mencionada exista y quede vinculada.

### 10. Cámara y orientación son conceptos distintos

`CameraReference.orientation` representa orientación de cámara. No existe orientación corporal ni dirección de mirada del personaje.

Consecuencia: usar ese campo para mirada sería una corrupción semántica y no debe considerarse una capacidad existente.

## Divergencias de validación

| Caso | Gramática Markdown | Paquete JSON/assets | Proyecto nativo |
|---|---|---|---|
| Clave desconocida | Generalmente se ignora | Metadata escalar se conserva; tipo desconocido falla | Campo JSON estructural inválido falla |
| Personaje/escena desconocidos | Advertencia | Algunas referencias fallan al reconciliar; otras producen bindings sin entidad | `BuildTheatreProductionProjectionUseCase` emite diagnóstico |
| Tono/plano desconocido | Advertencia | N/A | Puede haber fallback o referencia no resuelta |
| Archivo faltante | No se valida integralmente | Error/bloqueo en preflight | Integridad del proyecto lo detecta según el tipo de asset |
| Ruta absoluta o escape del root | Sin contrato único | Rechazada por `JsonTheatrePackageScanner.resolveInside` | Los assets gestionados usan rutas relativas |
| ZIP | No | No | No |
| Propiedades contradictorias | Advertencia para fondo + quitar fondo | Depende del binding | Algunas invariantes se validan en constructores |

La prueba focalizada `JsonTheatrePackageScannerTest.reportsMalformedManifestAsAControlledInputError` falla actualmente porque el texto de la excepción no conserva el diagnóstico esperado `JSON inválido`. La entrada se rechaza, pero el contrato de diagnóstico no coincide.

## Capacidades solo presentes en GUI

- presencia/ausencia por personaje e intervención;
- copia manual de posiciones desde la intervención anterior;
- edición conjunta de ubicaciones de participantes;
- gestión visual de vestuario/continuidad por escena;
- edición y dibujo de storyboard;
- varias operaciones de multimedia y audio no descritas por la gramática.

## Capacidades solo presentes o más completas en el modelo

- audio teatral de fondo con anclaje y recorte (`TheatreAudioTrack`);
- frames intermedios;
- referencias de cámara detalladas;
- asignaciones de fondo con scopes explícitos;
- huella y audio mezclado de coros;
- coordenadas `x/y` de `SpatialPosition`;
- catálogo de assets y persistencia de todas estas colecciones.

## Capacidades presentes en gramática pero no materializadas por el flujo público

- `origen`, `destino`, `interaccion`;
- `tono` por intervención;
- `plano` y `aplicar_plano`;
- `fondo` y `quitar_fondo`;
- `voces` simultáneas;
- imágenes de intervención;
- `contexto_ia`.

## Conclusión

La divergencia central es que **parsear, describir en sidecar y materializar el dominio son operaciones diferentes**. Hasta que una sola transacción valide y construya el grafo teatral completo, la importación no puede declararse determinística aunque cada subsistema aislado sí tenga partes deterministas.
