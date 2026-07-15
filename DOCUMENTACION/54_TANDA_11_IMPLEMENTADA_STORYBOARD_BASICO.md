# Tanda 11 implementada — Storyboard básico

## Estado
Implementada como primer MVP funcional de Storyboard vivo.

## Qué agrega

Esta tanda introduce el primer flujo visual de DocuPodcast Studio:

```text
Guion narrable
→ Storyboard vivo
→ imagen importada por el usuario
→ imagen asociada a segmento
→ storyboard/storyboard.json
```

La aplicación no genera imágenes ni decide qué imagen corresponde al texto. El usuario importa la imagen y la asocia al segmento.

## Dominio agregado

```text
domain/storyboard/
├── StoryboardDocument
├── StoryboardBinding
├── StoryboardScene
├── StoryboardDisplayMode
└── StoryboardValidationIssue
```

Conceptos:

- `StoryboardDocument`: manifiesto editable del storyboard vivo.
- `StoryboardBinding`: relación segmento → imagen.
- `StoryboardScene`: proyección visual para la UI.
- `StoryboardDisplayMode`: modo de presentación de la imagen.

## Aplicación agregada

```text
application/storyboard/
├── BuildStoryboardFromScriptUseCase
├── BindImageToSegmentUseCase
├── ImportImageAssetUseCase
├── ValidateStoryboardUseCase
├── MaterializeStoryboardUseCase
├── ImageAssetRepository
└── StoryboardWorkspaceRepository
```

## Infraestructura agregada

```text
infrastructure/storyboard/
├── LocalImageAssetFileRepository
└── StoryboardWorkspaceFileRepository
```

La imagen importada se copia a:

```text
media/images/
```

y se registra como asset `IMAGE`.

El storyboard se materializa en:

```text
storyboard/storyboard.json
```

y se registra como asset `STORYBOARD_MANIFEST`.

## UI agregada

```text
presentation/storyboard/StoryboardWorkspaceView
```

Incluye:

- escenas/segmentos;
- imágenes importadas;
- validación;
- tarjetas de preview;
- botones para crear storyboard, importar imagen, asociar imagen y reproducir desde selección.

## Menú y toolbar

Se agregó menú `Storyboard`:

```text
Ver storyboard vivo
Crear storyboard desde guion
Importar imagen para storyboard…
Asociar última imagen al segmento
```

La toolbar ahora abre el workspace real de Storyboard y ofrece acciones básicas.

## Persistencia

Al guardar el proyecto con storyboard se crea:

```text
storyboard/storyboard.json
```

El JSON principal registra:

```text
STORYBOARD-001 → storyboard/storyboard.json
```

## Limitaciones conscientes

- No hay canvas avanzado todavía.
- No hay drag & drop de imágenes todavía.
- No hay varias imágenes por segmento todavía.
- No hay video renderizado.
- No hay playback sincronizado real todavía.
- No se recarga storyboard/storyboard.json al abrir proyecto guardado.

## Siguiente paso natural

Tanda 12 debe conectar playback real por manifest y hacer que el guion/storyboard/audio avancen juntos durante reproducción.
