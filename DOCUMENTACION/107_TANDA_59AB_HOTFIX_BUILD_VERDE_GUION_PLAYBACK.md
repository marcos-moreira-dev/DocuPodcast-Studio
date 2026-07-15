# Tanda 59A-B — Hotfix build verde Guion/playback

Se corrige el source test `ScriptWorkspaceRecordingPlaybackSourceTest`, que fallaba tras la migración de `ScriptWorkspaceView` a `ActionButtonFactory` porque esperaba una llamada directa con punto (`viewModel.playFromSelectedSegment`) y no aceptaba la referencia de método (`viewModel::playFromSelectedSegment`).

La funcionalidad no cambió: la acción `Reproducir desde selección` sigue conectada al ViewModel.

También se registra el roadmap post 59A-B para separar el catálogo transversal de front-end de la auditoría/refactor del cerebro de la aplicación.
