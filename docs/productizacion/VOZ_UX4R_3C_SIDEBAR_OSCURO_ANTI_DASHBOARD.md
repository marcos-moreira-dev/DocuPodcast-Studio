# VOZ-UX4R-3C — sidebar oscuro y Vista Voces sin dashboard

## Contexto

La vista Voces ya estaba organizada como microaplicación de tres módulos, pero el módulo Inicio conservaba un mini-dashboard con métricas/tarjetas y el sidebar todavía se veía como panel claro. Eso chocaba con el criterio de producto: una superficie administrativa sobria, formal y moderna, inspirada en la paleta tipo Microsoft Teams sin copiar un dashboard decorativo.

## Cambios implementados

- `VoiceLibraryWorkspaceView` reemplaza las métricas de Inicio por un resumen operativo en filas sobrias.
- Se eliminan `metricCard(...)`, `voice-dashboard-metrics` y `voice-metric-card` de la superficie de Voces.
- El resumen de motor deja de usar tarjetas de modo en el módulo Configurar motor y pasa a filas de estado con `InfoBadge`.
- Las acciones principales de Voces pasan a usar el componente transversal `ActionBar` donde aplica.
- `VoiceModuleNavigation` ahora se declara full-height mediante `setMaxHeight(Double.MAX_VALUE)` y un spacer con `VBox.setVgrow`.
- `voice-library.css` actualiza el sidebar a una paleta oscura sobria: fondo `#252A44`, selección `#383F66` y acento `#8B8CFF`, sin gradientes.

## Alcance no tocado

- No se cambia playback.
- No se cambia generación de audio.
- No se cambia descarga de Voz IA avanzada ni Voz local simple.
- No se cambia Documento ni asignación de voces/emociones.
- No se agrega lógica al `DocuPodcastShellViewModel`.

## Guardarraíl

Se agrega `VoiceUx4R3CAntiDashboardSourceTest`, que protege:

- Inicio de Voces sin `metricCard`, `voice-dashboard-metrics` ni `voice-metric-card`.
- Uso de filas sobrias `homeOperationalSummary`, `homeStatusRow` y `engineModeRow`.
- Uso de componentes transversales `ActionBar` e `InfoBadge`.
- Sidebar oscuro/full-height y sin gradientes.

## Siguiente paso recomendado

`VOZ-TTS5A`: Documento debe mostrar solo voces con Neutral registrada y, al elegir una voz, solo emociones/tonos realmente registrados para esa voz.
