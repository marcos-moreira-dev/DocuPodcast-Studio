# Tanda 75 — Storyboard como capa del documento

Tanda basada en T74 con hotfix de source test.

## Hotfix incluido

`DocumentLayerAssignmentWorkflowSourceTest` todavía esperaba la frase “Word original”. Se corrige el guardarraíl para el contrato actual: las capas se guardan en el proyecto y no en el documento fuente, sea DOCX, PDF, Markdown o TXT.

## Implementación

Se agrega `BuildStoryboardFromImageLayersUseCase`, que permite construir un storyboard desde capas narrativas `IMAGE` reales.

La tanda refuerza que:

- el storyboard es una capa opcional del Documento narrable;
- la imagen debe ser un asset real;
- una imagen puede reutilizarse en varios fragmentos;
- el frame dura lo que dura el texto hablado;
- el documento fuente permanece solo lectura.

## Archivos destacados

- `application/storyboard/BuildStoryboardFromImageLayersUseCase.java`
- `domain/storyboard/StoryboardDocument.java`
- `presentation/document/DocumentRailImagePresentation.java`
- `presentation/shell/DocuPodcastShellViewModel.java`
- `docs/productizacion/STORYBOARD_COMO_CAPA_DOCUMENTO_T75.md`

## Tests

- `BuildStoryboardFromImageLayersUseCaseTest`
- `StoryboardAsDocumentLayerSourceTest`
- `DocumentLayerAssignmentWorkflowSourceTest` ajustado
