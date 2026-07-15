# GPU-XTTS-RUNTIME-HF1 — decisión visible cuando Voz IA avanzada cae a CPU

## Objetivo

Cerrar la brecha entre GPU detectada, GPU seleccionada y GPU realmente usable por el Python autocontenido de Voz IA avanzada.

Si el usuario selecciona `Preferir GPU` pero la prueba CUDA local no está aprobada, DocuPodcast puede continuar por CPU como decisión defensiva, pero ya no debe hacerlo en silencio. Si el usuario selecciona `Dispositivo específico`, DocuPodcast respeta la intención manual y deja que el runtime informe si no soporta ese dispositivo.

## Cambios

- Nuevo `InspectXttsGpuFallbackDecisionUseCase` en `application.compute`.
- Nuevo `DocumentAudioDefensiveDecisionGuard` en `presentation.shell.workflow`.
- `DocuPodcastShellView` muestra `UserVisibleDecision` con `ExceptionAlertPresenter.showDialogDecisions(...)` antes de continuar con acciones de audio del Documento.
- El aviso se muestra una vez por sesión/estado para no convertir cada clic en ruido.
- El detalle del aviso incluye evidencia útil: dispositivo solicitado, `deviceArgument`, GPU reportada por PyTorch, versión de PyTorch, versión CUDA y problemas del smoke.

## Contrato de producto

- Si el usuario pidió CPU, no hay aviso.
- Si el usuario dejó automático y no eligió GPU explícitamente, no hay aviso molesto.
- Si el usuario pidió `Preferir GPU` para Voz IA avanzada y CUDA no está confirmada, se muestra un message box porque la app cambia la intención a CPU.
- Si el usuario pidió `Dispositivo específico`, no se muestra fallback defensivo: la operación intenta ese dispositivo y puede fallar con diagnóstico del runtime.
- La operación automática puede continuar por CPU; esto no es una excepción fatal, es una decisión defensiva visible.

## Validación focal

- `GpuXttsRuntimeHf1SourceTest` valida que el fallback GPU→CPU requiera diálogo y que el detalle incluya evidencia CUDA/PyTorch.
- `DocuPodcastShellViewModel` no crece; la decisión se resuelve fuera del ViewModel.
