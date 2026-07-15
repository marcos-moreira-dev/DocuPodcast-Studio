# Tanda 96 — Limpieza de placeholders y acciones visibles falsas

T96 elimina accesos visibles que solo disparaban placeholders. Es una tanda de higiene antes del rediseño GUI: menú, toolbar y superficies operativas deben mostrar solo acciones reales o claramente soportadas.

Archivos clave:

- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellView.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/toolbar/MainToolbarView.java`
- `src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/toolbar/WorkspaceToolbarActionProvider.java`
- `src/test/java/com/marcosmoreiradev/docupodcaststudio/presentation/ux/VisibleActionCleanupT96SourceTest.java`
- `docs/productizacion/T96_LIMPIEZA_PLACEHOLDERS_ACCIONES_VISIBLES.md`

Validación local recomendada:

```bat
scripts\02-ejecutar-tests.bat
```
