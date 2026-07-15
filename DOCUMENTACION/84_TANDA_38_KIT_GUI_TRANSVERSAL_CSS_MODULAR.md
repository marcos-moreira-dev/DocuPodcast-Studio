# Documentación — Tanda 38

La Tanda 38 introduce una capa de componentes GUI reutilizables para impedir que la UX de DocuPodcast se fragmente por workspace. El objetivo es sostener una estética limpia, moderna y consistente sin convertir cada vista en JavaFX artesanal.

Cambios principales:

- `presentation.components` como paquete transversal.
- Componentes iniciales: `PrimaryActionStrip`, `EmptyStateView`, `SectionHeader`, `SettingsPageView`.
- `DocumentWorkspaceView` y `SettingsDialog` empiezan a usar componentes compartidos.
- `docupodcast-light.css` queda como ensamblador de CSS.
- Nuevos módulos CSS de componentes.
- `compat-legacy.css` conserva reglas históricas mientras se refactorizan gradualmente.

No cambia el flujo funcional de audio, exportación, playback ni persistencia.
