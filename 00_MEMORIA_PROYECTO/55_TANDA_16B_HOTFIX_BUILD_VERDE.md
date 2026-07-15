# Tanda 16B — Hotfix build verde y rebaseline mínimo

## Objetivo

Corregir la desalineación detectada tras Tanda 16 sin revertir la decisión de toolbar contextual.

## Problema

`AiResourcesUiSourceTest` todavía exigía que `MainToolbarView` contuviera `Recursos IA`, pero la Tanda 16 movió la acción productiva a:

```text
Ayuda → Exportar recursos IA…
```

La función no estaba rota; el test estaba atrasado respecto a la UX vigente.

## Cambios aplicados

```text
AiResourcesUiSourceTest valida DocuPodcastShellView y el menú Ayuda.
AiResourcesUiSourceTest deja de exigir botón global en MainToolbarView.
README.md queda rebaselined a Tanda 16B.
AI_HANDOFF.md queda rebaselined a Tanda 16B.
VALIDATION.md queda rebaselined a Tanda 16B.
```

## Decisión de producto

No reintroducir `Recursos IA` en la toolbar. La toolbar contextual debe seguir enfocada en el workspace activo.

## Validación

No se ejecutó Maven en el entorno de generación porque `mvn` no está instalado. La validación completa debe ejecutarse localmente con:

```bat
scripts\02-ejecutar-tests.bat
```

## Riesgos que quedan fuera

```text
rehidratación completa de proyecto
fuga domain → application en AudioJobSnapshot
guardarraíles de arquitectura
importador Markdown real docupodcast-script-v1
madurez visual de Guion/Audio/Storyboard
release candidate auditable
```
