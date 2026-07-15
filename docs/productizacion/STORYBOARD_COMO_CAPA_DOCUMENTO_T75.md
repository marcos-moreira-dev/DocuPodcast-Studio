# T75 — Storyboard como capa del documento

## Decisión de producto

En V1 el storyboard no es un editor de video independiente ni un segundo objeto padre. Es una capa opcional del Documento narrable.

Flujo conceptual:

```text
Documento fuente solo lectura
→ Documento narrable
→ selección de fragmentos
→ capa IMAGE con asset real
→ storyboard proyectado desde esas capas
→ video/paquete donde cada imagen dura lo que dura el texto hablado
```

## Reglas cerradas

1. El documento fuente nunca se edita desde DocuPodcast Studio.
2. Las imágenes son assets del proyecto, no contenido incrustado en el Word/PDF/Markdown/TXT original.
3. Una capa IMAGE debe apuntar a una imagen real del catálogo de assets.
4. una imagen puede reutilizarse en varios fragmentos del documento sin duplicarse físicamente.
5. El storyboard se puede reconstruir desde capas IMAGE cuando haya narración interna preparada.
6. En exportación simple, cada frame dura lo que dura el audio del segmento hablado más el silencio configurado.
7. Si no hay imagen para un fragmento, el plan de video debe usar fallback visual o frame sin imagen, no fallar silenciosamente.

## Implementación de cerebro

Se agrega `BuildStoryboardFromImageLayersUseCase` para construir o refrescar bindings de storyboard desde capas narrativas reales de tipo `IMAGE`.

El caso de uso valida:

- que la capa sea `NarrativeLayerKind.IMAGE`;
- que el segmento exista en la narración interna;
- que el target sea asset `IMAGE` o `THUMBNAIL` real;
- que el binding declare metadata `duration=spoken-segment`;
- que la misma imagen pueda aparecer en varios bindings.

## Ajuste de mini rail

`DocumentRailImagePresentation` y `DocuPodcastShellViewModel.documentRailImagePresentations()` dejan de representar una sola asignación por imagen. Ahora pueden informar cuántos fragmentos usan la misma imagen y mostrar la primera asignación como entrada de navegación.

## Relación con video

T75 prepara el cerebro para T76. La duración final del frame todavía se calcula en `BuildSimpleVideoPlanUseCase` a partir del audio completado del segmento. La regla de negocio queda conectada: imagen asociada a texto = frame visual durante la lectura hablada de ese segmento.

## Validación esperada

- `BuildStoryboardFromImageLayersUseCaseTest`: verifica bindings desde capas IMAGE reales y reutilización de imagen.
- `StoryboardAsDocumentLayerSourceTest`: protege el contrato producto/arquitectura.
- `DocumentLayerAssignmentWorkflowSourceTest`: corregido para no hablar de Word original, sino de documento fuente.
