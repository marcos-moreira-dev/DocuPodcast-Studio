# Tanda 74 — Capas narrativas reales

## Propósito

T74 cierra una deuda del cerebro de capas: las acciones visibles de Documento ya no deben crear capas productivas con identificadores placeholder como `VOICE-IA-DEFAULT`, `IMAGE-STORYBOARD-PENDIENTE` o `AUDIO-EXTERNO-PENDIENTE`.

El Documento fuente sigue siendo solo lectura. Las capas viven en el proyecto DocuPodcast, pero cuando una capa declara que apunta a voz, imagen o audio debe apuntar a un target real del proyecto o debe rechazar la asignación con un mensaje accionable.

## Decisión de producto

- Voz principal: usa una voz real de la biblioteca, empezando por `VOC-NARRATOR` cuando no hay selección explícita.
- Emoción/intención: usa un estilo real de la biblioteca, empezando por `STY-NEUTRAL`.
- Imagen: requiere una imagen real importada o seleccionada.
- Audio humano: requiere un asset de audio real.
- Audio ambiente: requiere un asset de audio real.
- Nota: puede existir sin target externo porque es una anotación interna del proyecto.

## Qué cambia

Se agrega `NarrativeLayerTargetResolver` para resolver targets concretos antes de crear `NarrativeLayerAssignment`.

`NarrativeLayerCoordinator` ahora delega resolución de target, conflicto, remoción y presentación, y devuelve `missingTarget(...)` cuando el usuario intenta asociar una capa que todavía no tiene asset real.

## Criterio de aceptación

- No se crean nuevas capas con targets placeholder.
- Una imagen no se asocia si no hay asset real.
- Una voz apunta a una voz de la biblioteca.
- La acción conserva el principio fuente solo lectura.
- Los source tests protegen que los placeholders no vuelvan al coordinador.
