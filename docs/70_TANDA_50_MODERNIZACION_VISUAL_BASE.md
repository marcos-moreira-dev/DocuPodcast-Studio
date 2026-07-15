# Tanda 50 — Modernización visual base

## Objetivo

La Tanda 50 abandona el aspecto de aplicación JavaFX/Windows XP y fija una base visual moderna, formal y limpia para DocuPodcast Studio. La pantalla principal debe sentirse como un lector narrado de documentos para usuarios no técnicos; la infraestructura pesada queda en configuración, asistentes y módulos avanzados.

## Decisiones de producto

- El look debe acercarse a aplicaciones modernas de escritorio como Teams/MuseScore/Subtitle Edit: formal, claro, sobrio y usable.
- No se busca una interfaz publicitaria ni un “circo” de colores.
- Los botones pueden tener suavidad visual, pero no deben parecer exageradamente redondeados.
- Los paneles no deben anidarse porque sí. Un panel solo existe si agrupa información o una tarea clara.
- La pantalla Documento debe leerse como página cómoda, no como lista técnica de tarjetas.
- CSS debe seguir modular: tokens, componentes y workspaces.

## Cambios realizados

- `tokens.css` ahora define una paleta moderna con acento tipo Teams (`#5B5FC7`), superficies claras, chrome oscuro sobrio y sombras suaves.
- `shell.css` elimina el estilo gradiente clásico y crea una barra superior más moderna.
- `toolbar.css` elimina gradientes tipo XP y usa botones planos, formales y con hover discreto.
- `statusbar.css` pasa a una barra inferior discreta.
- `welcome.css` actualiza la bienvenida para verse más como producto moderno y menos como prototipo técnico.
- `document-reader.css` refuerza la lectura cómoda: página blanca, texto de 18px, interlineado 7px, bloques menos técnicos, selección y seguimiento visual más sobrios.
- `components/actions.css`, `cards.css`, `media-rail.css` y `settings-shell.css` modernizan componentes reutilizables.

## Tests agregados

- `ModernVisualThemeSourceTest`
- `CssTokenCoverageSourceTest`
- `NoExcessiveNestedPanelsSourceTest`
- `NoLegacyXpLookSourceTest`

## No cambia

- No se toca audio.
- No se toca playback.
- No se toca persistencia.
- No se toca exportación.
- No se toca selección de rangos.
- No se toca video.

Esta tanda es una base visual. Las siguientes tandas deben convertir la pantalla Documento en experiencia operativa real y después reforzar el flujo Word → escuchar.
