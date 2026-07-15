# Tanda 28 — Storyboard visual maduro

## Objetivo

Madurar el workspace Storyboard para que deje de comportarse como una lista técnica y pase a operar como un tablero visual de escenas por segmento.

## Cambios principales

```text
StoryboardScenePresentation
StoryboardWorkspaceView con scene board visual
miniaturas reales cuando hay asset resoluble
placeholder visual cuando falta imagen
chips de imagen, audio, validación y display mode
acciones por escena: seleccionar, asociar última imagen y reproducir
storyboard.css rebaselined para tarjetas visuales
```

## Contrato de producto

Cada escena debe mostrar:

```text
segmento asociado
imagen asociada o pendiente
ruta/asset visual
caption
preview narrativo
estado de audio/cue
estado de validación
estado de selección/playback
```

La app no genera imágenes automáticamente. El usuario sigue importando y asociando imágenes.

## Validación local en este entorno

No se ejecutó Maven porque `mvn` no está instalado.

Sí se validó:

```text
javac --release 21 de domain + StoryboardScenePresentation
javac --release 21 de StoryboardScenePresentationTest con stubs JUnit
revisión estática de StoryboardWorkspaceView
revisión estática de storyboard.css
ZIP íntegro con unzip -t
```
