# Tanda 75 — Storyboard como capa del documento

Se implementa el contrato de storyboard como capa opcional del Documento narrable. Las imágenes asociadas se guardan como assets del proyecto, no en el documento fuente.

Puntos clave:

- `BuildStoryboardFromImageLayersUseCase` proyecta capas `IMAGE` reales a bindings de storyboard.
- Una imagen puede reutilizarse en varios fragmentos.
- Cada frame dura lo que dura el texto hablado del segmento.
- Se corrige el guardarraíl que aún hablaba de “Word original”; ahora el contrato es “documento fuente”.

Siguiente foco: T76 video/render.
