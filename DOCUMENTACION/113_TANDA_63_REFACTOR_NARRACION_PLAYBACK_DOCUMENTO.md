# Tanda 63 — Refactor narración/playback desde Documento

Se agregan `DocumentNarrationCoordinator` y `PlaybackWorkflowCoordinator` para reducir decisiones de cerebro en `DocuPodcastShellViewModel`.

La raíz sigue siendo Documento narrable. La proyección de narración existe para TTS/playback/Markdown/diagnóstico, no como objeto padre visible obligatorio.
