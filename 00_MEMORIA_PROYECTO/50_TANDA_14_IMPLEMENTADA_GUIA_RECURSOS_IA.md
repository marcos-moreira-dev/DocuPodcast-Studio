# Memoria — Tanda 14 Guía integrada + recursos IA

La Tanda 14 implementa dos piezas de producto transversales: guía integrada y recursos IA exportables.

La guía es Word-first: el tema “Importar notas desde Word” queda al inicio porque DOCX es la entrada prioritaria. Markdown se mantiene como puente con IA, ejemplos e intercambio humano, pero no reemplaza a Word ni al archivo `.docupodcast.json`.

Los recursos IA se exportan desde classpath mediante catálogo/descriptores y generan índice con importabilidad, contrato y uso recomendado. Las plantillas con placeholders se marcan como no importables.

Clases clave:

- `GuideTopicId`, `GuideTopic`, `GuideCatalog`, `GetGuideTopicUseCase`, `SearchGuideTopicsUseCase`.
- `ClasspathGuideCatalog`.
- `GuideDialog`.
- `AiResourceDescriptor`, `AiResourceCatalog`, `AiResourceExporter`, `ExportAiResourcesUseCase`.
- `OfficialAiResourceCatalog`, `ClasspathAiResourceExporter`.

UI:

- `Ayuda > Guía de DocuPodcast Studio...`.
- `Ayuda > Tema: Importar notas desde Word...`.
- `Ayuda > Exportar recursos IA...`.
- Toolbar: Guía, Ayuda Word, Recursos IA.
