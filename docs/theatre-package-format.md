# Carpeta de obra teatral DocuPodcast (schema 1)

Para importar el guion y autoconfigurar su estructura, consulte el [contrato de gramática teatral](theatre-grammar-format.md). Este documento describe la vinculación y el refresco de recursos.

Una carpeta vinculable contiene en su raíz `docupodcast-theatre.json`. DocuPodcast trata la carpeta como entrada de solo lectura, copia los assets al proyecto con nombres versionados por hash y guarda su estado de sincronización junto al proyecto.

## Manifiesto mínimo

```json
{
  "schemaVersion": 1,
  "packageId": "como-sera-la-patria",
  "packageVersion": "1",
  "assets": []
}
```

`packageId` es la identidad estable de la obra y no debe cambiar entre refrescos. `packageVersion` es informativo. Un cambio de `packageId` bloquea el refresco para evitar mezclar dos obras.

## Descubrimiento por carpetas

Los archivos multimedia compatibles dentro de `assets/` se descubren sin enumerarlos en el JSON:

```text
assets/
├── personajes/<personaje>/<vista>.png
├── objetos/<objeto>/<vista>.png
├── fondos/<fondo>.png
├── mapas/<escena>.png
├── intervenciones/<intervencion>.png
├── puentes/<intervencion-inicial>__<intervencion-final>.png
├── audio/<escena>/<intervencion>.wav
├── voces/<voz>.wav
└── videos/<video>.mp4
```

Se aceptan nombres equivalentes en inglés (`characters`, `objects`, `backdrops`, `maps`, `interventions`, `bridges`, `voices`, `video`). Las intervenciones `i_00842`, `I-842` e `INTERVENCION-842` se normalizan a `INTERVENCION-842`.

Las extensiones descubiertas son PNG, JPG/JPEG, WEBP, WAV, MP3, FLAC, M4A, OGG y MP4. Los archivos de otro tipo se ignoran.

## Assets explícitos

Cuando los IDs del proyecto no coincidan con los nombres de carpeta o haga falta indicar un binding, declare el asset:

```json
{
  "schemaVersion": 1,
  "packageId": "como-sera-la-patria",
  "packageVersion": "1",
  "assets": [
    {
      "logicalId": "character:concha:view:frontal",
      "kind": "CHARACTER_IMAGE",
      "path": "assets/personajes/concha/frontal.png",
      "characterId": "PERSONAJE-CONCHA",
      "view": "frontal",
      "displayName": "Concha frontal"
    },
    {
      "logicalId": "scene:c08:intervention:i-00842:audio",
      "kind": "HUMAN_AUDIO",
      "path": "assets/audio/c08/i_00842.wav",
      "sceneId": "ESCENA-C08",
      "interventionId": "INTERVENCION-842",
      "displayName": "Ya no pedimos permiso"
    }
  ]
}
```

Tipos soportados: `CHARACTER_IMAGE`, `OBJECT_IMAGE`, `BACKDROP`, `SPATIAL_MAP`, `INTERVENTION_IMAGE`, `INTERMEDIATE_FRAME`, `HUMAN_AUDIO`, `VOICE_SAMPLE`, `VIDEO` y `OTHER`.

Metadata de binding:

- `CHARACTER_IMAGE`: `characterId`, opcionalmente `sceneId`, `view`.
- `OBJECT_IMAGE`: `objectId`, opcionalmente `sceneId`, `view`.
- `BACKDROP`: `backdropId`; opcionalmente `scope` y `scopeId` para asignarlo.
- `SPATIAL_MAP`: `sceneId`.
- `INTERVENTION_IMAGE` y `HUMAN_AUDIO`: `interventionId`.
- `INTERMEDIATE_FRAME`: `fromInterventionId`, `toInterventionId`.

## Semántica de refresco

- Nuevo o modificado: se muestra en preflight y se materializa al confirmar.
- Idéntico: no se vuelve a copiar ni se reescribe el proyecto.
- Renombrado inequívoco: se reconoce por tipo y SHA-256.
- Ausente: la copia materializada se conserva y se informa; nunca se borra automáticamente.
- Ambiguo, referencia no resuelta o manifiesto incompatible: bloquea el commit.
- Cancelación o fallo: se restaura el proyecto y no quedan referencias parciales.

La gramática Markdown histórica continúa siendo compatible. La carpeta vinculada agrega un flujo incremental de assets; no reemplaza esa importación.

## Carpeta oficial v2: guion y configuración

El flujo oficial v2 sí importa el guion: construye su propio documento de parlamentos y su propia lectura, junto con la capa teatral. No utiliza el guion previamente abierto para decidir el texto del paquete. Se aplica desde **Teatro → Refrescar obra**, sobre un proyecto guardado; después se guardan los cambios importados.

```json
{
  "schemaVersion": 2,
  "grammarVersion": "theatre-v2",
  "grammar": "obra.teatro.md",
  "packageId": "obra-ejemplo",
  "packageVersion": "1.0.0",
  "assets": []
}
```

`grammar` es una ruta relativa dentro de la carpeta. El Markdown debe declarar `grammarVersion: theatre-v2`. Las rutas declaradas en la gramática deben corresponder a recursos del manifiesto. El manifiesto v2 enumera los recursos: no depende del descubrimiento automático de carpetas de v1.

Cada intervención tiene un bloque documental y un segmento de lectura distintos, enlazados por identidad. El documento contiene únicamente el texto hablado. Actos, escenas, personajes, objetos, voces, ubicaciones, fondos, cámara y contexto permanecen en sus estructuras de configuración. La carpeta incluye los archivos necesarios; no necesita un modelo de IA para configurarse.

La importación conserva una copia de la gramática y del manifiesto junto con los recursos. Importar el mismo contenido reutiliza la copia si sigue íntegra; un cambio de gramática genera otra ubicación aunque las imágenes no hayan cambiado. No se sobrescriben copias alteradas localmente. `config/voces.csv`, si está presente junto a la gramática, sigue el contrato descrito en [gramática teatral](theatre-grammar-format.md).

### Un recurso con varios usos

Un archivo se enumera una vez. Sus usos adicionales se expresan en `bindings`:

```json
{
  "path": "assets/audio/ambiente.wav",
  "logicalId": "audio-ambiente",
  "kind": "HUMAN_AUDIO",
  "interventionId": "INTERVENCION-1",
  "textStart": "0",
  "textEnd": "18",
  "bindings": [
    {
      "kind": "HUMAN_AUDIO",
      "trackId": "TRACK-AMBIENTE",
      "interventionId": "INTERVENCION-2",
      "sourceStartSeconds": "1",
      "sourceEndSeconds": "8",
      "sourceDurationSeconds": "10",
      "endMode": "FILE_END",
      "volume": "0.4",
      "gentleFade": "true"
    }
  ]
}
```

`textStart`/`textEnd` son offsets del parlamento, no tiempos de audio. Si se omiten, un `HUMAN_AUDIO` reemplaza el parlamento completo. Con `trackId`, la declaración configura una pista adicional con su ancla, recorte, volumen y fundido; no reemplaza el diálogo.

Cada binding contiene su propio tipo y destino. No puede cambiar `path`, `logicalId`, `sha256` ni `size`; todos los usos comparten el archivo validado. El exportador conserva los usos y copia el archivo una sola vez.

### Muestras de voz

`VOICE_SAMPLE` requiere `voiceProfileId` de una voz existente y admite `sampleId`, `tone` (por defecto `NEUTRAL`), `durationMillis`, `notes` y `referenceTranscript`. El archivo se copia al proyecto y se registra en el conjunto de muestras de esa voz. No se descarga ni se inventa un perfil ausente. La exportación transporta las muestras gestionadas como `PROJECT_ASSET`; las voces prediseñadas siguen siendo recursos de la aplicación.

### Límites y protección del texto

La exportación exige lectura preparada y una escena asociada a cada intervención; informa de los datos que faltan en lugar de inventar parlamentos o omitirlos silenciosamente. Los proyectos teatrales antiguos se actualizan mediante importación completa de gramática/carpeta; no hay una migración automática que interprete párrafos mezclados con instrucciones técnicas.

“Refrescar fuente documental” no puede reconvertir una proyección teatral a Markdown genérico: remite al flujo de Teatro. `VIDEO` y `OTHER` siguen siendo recursos de catálogo, sin asignación implícita a reproducción. La configuración/importación de motores o perfiles de voz nuevos y la equivalencia completa de todos los editores de otros modos no forman parte de este contrato.
# Vista de personajes y escenografía

El manifiesto V2 admite `"presentationMode": "scenery"`. Otros valores válidos son `fragments`, `characters` y `none`; al omitirlo se conserva el valor predeterminado `fragments`. Esta preferencia se guarda en el proyecto y se conserva al exportar y reimportar la carpeta.

`scenery` usa `fondo_escenario` y los cambios de `fondo` de la intervención. `mapa_espacial` continúa siendo la referencia técnica y no sustituye un fondo ausente. Los personajes se obtienen del hablante, miembros explícitos de coro y `interaccion`; no se infieren del parlamento. Las imágenes PNG conservan su transparencia sobre el escenario. Un personaje sin archivo asignado muestra un marcador negro con su nombre.
