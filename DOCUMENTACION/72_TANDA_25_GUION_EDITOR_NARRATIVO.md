# Tanda 25 — Guion como editor narrativo real

## Objetivo

Convertir el workspace **Guion** de una lista funcional de segmentos a un editor narrativo con lectura de producto: cada segmento muestra su estado de voz, audio, storyboard, validación y playback.

## Cambios principales

- Se agrega `ScriptSegmentPresentation` como proyección testeable del estado de un segmento.
- `ScriptWorkspaceView` incorpora header editorial, tarjetas de preparación, chips de estado y panel lateral de validación.
- Cada tarjeta de segmento muestra:
  - personaje, voz y estilo con nombres legibles;
  - estado de voz usable;
  - audio generado o pendiente según `PlaybackManifest`;
  - imagen de storyboard asociada o pendiente;
  - errores/advertencias de validación por segmento;
  - estado de playback activo/pausado.
- El empty state ahora guía el flujo Word/DOCX → Documento → Guion → Voces/Audio/Storyboard.
- `script-workspace.css` añade estilos para readiness cards, status chips, pausado/reproduciendo y validación lateral.

## Alcance

Esta tanda no implementa edición inline ni drag-and-drop. Es una maduración visual y de contrato del editor de guion para que el usuario vea claramente qué falta antes de generar audio/storyboard.

## Tests

- `ScriptSegmentPresentationTest`
- `ScriptWorkspaceSourceTest` actualizado

## Validación local recomendada

```bat
scripts\02-ejecutar-tests.bat
scripts\01-ejecutar-app.bat
```
