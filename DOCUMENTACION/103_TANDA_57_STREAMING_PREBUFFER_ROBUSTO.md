# Tanda 57 — Streaming/prebuffer robusto

La Tanda 57 fortalece el flujo de lectura larga. El programa mantiene la experiencia principal en Documento y calcula un estado de buffer comprensible: preparar 5 fragmentos iniciales, mantener 10 adelantados y pausar/cargar/continuar si se alcanza un fragmento aún no disponible.

Componentes relevantes:

```text
domain.playback.PlaybackBufferPolicy
domain.playback.StreamingPlaybackWindow
presentation.shell.DocuPodcastShellViewModel
presentation.document.DocumentWorkspaceView
```

La fábrica interna sigue generando audio por chunks, pero el usuario ve un mensaje simple de progreso y continuidad.
