# Tanda 61 — Auditoría ejecutable del cerebro y refresco de fuente

Tanda 61 toma la regla de documentos fuente solo lectura y la convierte en contrato ejecutable de cerebro para la acción futura `Refrescar contenido`.

La app debe permitir que el usuario edite el Word/PDF/Markdown/TXT fuera de DocuPodcast y luego compare cambios, sin que DocuPodcast sobrescriba el archivo original.

Componentes agregados:

```text
SourceDocumentSnapshot
SourceDocumentChangeReport
DerivedArtifactFreshness
RefreshSourceDocumentUseCase
RefreshSourceDocumentResult
```

El resultado marca audio como obsoleto si cambia el texto, y deja capas/storyboard en revisión. La UI final debe ser simple, pero la decisión debe vivir en application/domain.
