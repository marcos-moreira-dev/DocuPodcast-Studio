# Memoria — Tanda 11 Storyboard básico

La Tanda 11 introduce Storyboard vivo como workspace real. El flujo implementado es deliberadamente simple: una imagen por segmento, imagen aportada por el usuario, asociación manual y persistencia en `storyboard/storyboard.json`.

Decisiones consolidadas:

- Storyboard es visual, pero esta primera versión usa tarjetas JavaFX, no canvas común avanzado.
- El guion sigue siendo la fuente narrativa; mover o asociar imagen no cambia el orden del guion.
- La imagen vive como `ProjectAssetKind.IMAGE` en `media/images/`.
- El manifiesto del storyboard vive como `ProjectAssetKind.STORYBOARD_MANIFEST`.
- El usuario decide la correspondencia imagen-texto.

Esta tanda prepara el MVP de teatro/guion/cine básico: segmento, voz, audio y ahora imagen asociada.
