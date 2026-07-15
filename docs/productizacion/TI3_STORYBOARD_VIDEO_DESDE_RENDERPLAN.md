# TI3 — Storyboard/video desde RenderPlan

## Objetivo

TI3 conecta el paquete de storyboard/video con el contrato transversal creado en TI1 y usado por audio en TI2: `RenderUnitPlan`.

Antes de esta tanda, el plan de video simple seguía naciendo de `NarrationScriptDocument` y de los segmentos narrables. Eso era suficiente para la primera exportación, pero no respetaba la regla final del producto: el video no debe nacer de todos los segmentos, sino de las unidades con visual asignado.

## Regla cerrada

- `SPOKEN_ONLY`: se escucha, pero no entra al video.
- `SPOKEN_WITH_VISUAL`: entra al video como imagen + audio.
- `VISUAL_SILENT`: entra al video como imagen + silencio sintético.
- `OMITTED`: no entra al video.

Una imagen, tabla o fórmula detectada en el documento fuente no aparece automáticamente en el video. Solo aparece si el usuario le asigna un visual o si una tanda futura crea una política explícita de autoasignación desactivada por defecto.

## Cambios implementados

- `BuildSimpleVideoPlanUseCase` tiene entrada nueva desde `RenderUnitPlan`.
- `SimpleVideoFrame` distingue frames visuales silenciosos.
- `SimpleVideoPlan.framesMissingAudio()` ya no trata un frame silencioso como error.
- `BuildVideoRenderCommandPlanUseCase` genera silencio FFmpeg con `anullsrc` para `VISUAL_SILENT`.
- `ExportSimpleVideoPackageUseCase` tiene overload para exportar desde `RenderUnitPlan`.
- `DocuPodcastShellViewModel.exportSimpleVideoPackage(...)` intenta primero `RenderUnitPlan` y conserva fallback legacy por compatibilidad.

## Contrato de render silencioso

Un frame `VISUAL_SILENT` se representa como:

```text
imagen asignada + audio sintético silencioso + duración configurada
```

La duración inicial proviene de Configuración y por defecto es 5 segundos.

## Límites deliberados

TI3 no implementa todavía la selección/asignación directa a bloques no narrables del documento. Eso queda para TI4. TI3 consume esos casos cuando ya existen como `RenderUnit` visual silencioso.

## Validación esperada

- Maven compile verde.
- Maven tests verdes.
- El plan de video desde RenderUnit omite `SPOKEN_ONLY`.
- El plan incluye `SPOKEN_WITH_VISUAL`.
- El plan incluye `VISUAL_SILENT` sin exigir audio real.
- Los comandos FFmpeg incluyen `anullsrc` para frames silenciosos.
