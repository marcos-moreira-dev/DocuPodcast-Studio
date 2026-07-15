# Plan de Fases — Teatro DocuPodcast

## Estado actual (post-Fase 0)

- Renombrado Tn → Intervención en todo el código (~30 archivos)
- `FragmentAlias` → `Intervencion` (id: `"INTERVENCION-1"`, `"INTERVENCION-2"`...)
- `FragmentImage` → `IntervencionVisual`
- `TheatreTextAliasCatalog` → `IntervencionCatalogo` (métodos en español)
- `TheatreSceneAliasNumbering` → `IntervencionNumberingScene`
- `TheatreSceneTextBoundaryStore` → `IntervencionBoundaryStore`
- IDs internos: `INTERVENCION-N` (JSON key: `"intervenciones"`)
- Display: "Intervención N" via `TheatreZigzagLayout.displayLabel()`
- Tests: assert `INTERVENCION-N` en vez de `T\d+`
- 0 regresiones (14 fallos preexistentes)

---

## Fase 1 — TextActionPlacement como modelo de dominio serializable

Mover `TextActionPlacement` de record privado en `TheatreSpatialActionMapPanel.java`
a `TheatreProjectLayer.java` como componente oficial (#12), serializable en JSON.

### Archivos a modificar

| Archivo | Cambio |
|---------|--------|
| `TheatreProjectLayer.java` | + `TextActionPlacement` record, + `List<TextActionPlacement>`, actualizar `empty()` y constructores |
| `DocuPodcastProjectJsonWriter.java` | + `writeTextActionPlacements()`, llamar desde `writeTheatre()` |
| `DocuPodcastProjectJsonReader.java` | + `readTextActionPlacements()`, pasar al constructor |
| `TheatreSpatialActionMapPanel.java` | `placements` usa el record del dominio; `mostrarEditorAccion()` escribe al proyecto |
| `DocuPodcastProjectTheatreJsonTest.java` | + test round-trip con TextActionPlacement |

### `TextActionPlacement` — diseño

```java
public record TextActionPlacement(
    String intervencionId,         // "INTERVENCION-1"
    String sceneId,                // "SCN-001"
    String characterId,            // "CHR-CAPITAN-BIGOTE"
    String origin,                 // "fondo derecha", "centro"...
    String destination,            // hacia dónde apunta la flecha
    String interactionTarget,      // "TENIENTE TORNILLO", "Publico"...
    Map<String, String> characterLocations  // {characterId → location}
) {}
```

JSON shape:
```json
{
  "textActionPlacements": [
    {
      "intervencionId": "INTERVENCION-1",
      "sceneId": "SCN-001",
      "characterId": "CHR-NARRADOR",
      "origin": "centro",
      "destination": "hacia el publico",
      "interactionTarget": "Publico",
      "characterLocations": {
        "CHR-NARRADOR": "centro",
        "CHR-CAPITAN-BIGOTE": "centro izquierda",
        "CHR-TENIENTE-TORNILLO": "centro derecha"
      }
    }
  ]
}
```

---

## Fase 2 — Configurar por defecto las 24 intervenciones del demo

En `DocuPodcastShellViewModel.java`, poblar `TextActionPlacement` para
las 24 intervenciones + `IntervencionBoundaryStore` con los 3 límites de escena.

### Límites de escena

| Escena | Intervenciones |
|--------|----------------|
| SCN-001 (El hangar) | INTERVENCION-1 a INTERVENCION-7 |
| SCN-002 (En el aire) | INTERVENCION-8 a INTERVENCION-14 |
| SCN-003 (El aterrizaje) | INTERVENCION-15 a INTERVENCION-24 |

### Tabla de 24 intervenciones

| Interv | Escena | Personaje | Origen | Destino | Interacción |
|--------|--------|-----------|--------|---------|-------------|
| 1 | SCN-001 | NARRADOR | centro | público | Público |
| 2 | SCN-001 | CAPITAN BIGOTE | centro izq | frente der | TENIENTE |
| 3 | SCN-001 | TENIENTE TORNILLO | centro der | frente izq | CAPITAN |
| 4 | SCN-001 | TENIENTE TORNILLO | centro der | centro | CAPITAN |
| 5 | SCN-001 | CAPITAN BIGOTE | centro izq | centro der | TENIENTE |
| 6 | SCN-001 | TENIENTE TORNILLO | centro der | centro izq | CAPITAN |
| 7 | SCN-001 | CAPITAN BIGOTE | centro izq | público | TENIENTE |
| 8 | SCN-002 | NARRADOR | extra diegético | centro | Público |
| 9 | SCN-002 | TENIENTE TORNILLO | fondo izq | centro der | CAPITAN |
| 10 | SCN-002 | CAPITAN BIGOTE | fondo der | centro izq | TENIENTE |
| 11 | SCN-002 | TENIENTE TORNILLO | fondo izq | centro | CAPITAN |
| 12 | SCN-002 | CAPITAN BIGOTE | fondo der | centro | TENIENTE |
| 13 | SCN-002 | NARRADOR | extra diegético | centro | Público |
| 14 | SCN-002 | TENIENTE TORNILLO | fondo izq | frente der | CAPITAN |
| 15 | SCN-003 | CAPITAN BIGOTE | centro izq | centro der | TENIENTE |
| 16 | SCN-003 | TENIENTE TORNILLO | centro | centro izq | CAPITAN |
| 17 | SCN-003 | CAPITAN BIGOTE | centro izq | centro | TENIENTE |
| 18 | SCN-003 | NARRADOR | extra diegético | centro | Público |
| 19 | SCN-003 | TENIENTE TORNILLO | frente der | centro | CAPITAN |
| 20 | SCN-003 | CAPITAN BIGOTE | frente izq | frente der | TENIENTE |
| 21 | SCN-003 | NARRADOR | extra diegético | centro | Público |
| 22 | SCN-003 | TENIENTE TORNILLO | centro | centro izq | CAPITAN |
| 23 | SCN-003 | CAPITAN BIGOTE | centro | centro der | TENIENTE |
| 24 | SCN-003 | NARRADOR | centro | público | Público |

Cada una también con `characterLocations` posicionando a los 3 personajes.

---

## Fase 3 — Extender `TheatreGrammarMarkdownParser`

### Nuevo patrón
```
^>\s+(.+)$  → línea de metadatos tras cada diálogo
```
Parsear `key=value | key=value | key=value...` con:
- `origen`, `destino`, `interaccion`, `imagen[N]`

### Nuevos records

```java
public record IntervencionPlan(
    String personaje,
    String texto,
    String origen,
    String destino,
    String interaccion,
    List<String> imagenes
) {}
```

`ImportPlan` ahora incluye `Map<String, List<IntervencionPlan>>` por escena.

### Formato gramatical

```markdown
### Escena: El hangar
notas: Amanecer. Avión al centro.

NARRADOR: En el viejo aerodromo...
> origen=centro | destino=hacia el publico | interaccion=Publico | imagen=assets/fragmentos/fragmento_02_...

CAPITAN BIGOTE: Teniente, revise el combustible. La dignidad también.
> origen=centro izquierda | destino=frente derecha | interaccion=TENIENTE TORNILLO
> imagen=assets/fragmentos/fragmento_03_revision_capitan.png
> imagen=assets/fragmentos/fragmento_03b_dignidad.png
```

---

## Fase 4 — Reescribir PROYECTO_DEMO.md como gramática válida

- Sigue el formato de la Fase 3
- Contiene las 24 intervenciones distribuidas en 3 escenas
- Incluye metadatos espaciales por intervención
- Referencia imágenes como `assets/fragmentos/fragmento_XX_*.png`
- Comentarios `<!-- ... -->` para documentación extra
- Personajes con `voz=` apuntando a presets reales
- 9 objetos de utilería

---

## Fase 5 — Crear TEATRO_GRAMATICA.md (meta-especificación)

Archivo en `src/main/resources/examples/aviadores-comicos/`:
- Reglas de cada sección (obligatorio/opcional)
- Formato exacto de cada línea: `- personaje: NOMBRE | nota=... | voz=...`
- Campos aceptados por personaje, objeto, intervención
- Referencia de ubicaciones espaciales válidas
- Ejemplo mínimo vs completo

---

## Fase 6 — Conectar mapa espacial al pipeline de video

- `TextActionPlacement` + `SpatialPosition` → composición de frames
- Nueva clase `BuildSpatialVideoPlanUseCase` que genera imágenes compuestas:
  - Fondo: fragment image
  - Overlay: mapa espacial con posiciones de personajes
  - Flechas origen→destino
  - Labels de personajes en sus ubicaciones
- Conectar `TheatreWorkExportOptions.showSpatialMap` / `showCharacters` al pipeline
- Reemplazar stub `prepareSpatialVideoFrames()` por generación real
