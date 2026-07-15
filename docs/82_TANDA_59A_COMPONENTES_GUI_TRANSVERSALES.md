# Tanda 59A — Componentes GUI transversales

## Objetivo

Convertir los principios de Tanda 59 en una primera intervención de código: las acciones repetibles de workspaces dejan de estar hardcodeadas como botones JavaFX sueltos y empiezan a pasar por un catálogo de componentes GUI compartidos.

## Cambios productivos

Se agregan componentes en `presentation.components`:

```text
ActionButtonFactory
ActionBar
TransportControls
RailActionRow
MetricBadge
InfoBadge
DiagnosticCard
```

Se amplía `AppStyles` con estilos compartidos para acciones, transporte, rails, badges, métricas y cards de diagnóstico.

Se actualiza CSS modular:

```text
src/main/resources/css/components/actions.css
src/main/resources/css/components/cards.css
src/main/resources/css/tokens.css
```

## Superficies migradas

- Guion: creación, voz/STT y transporte usan `ActionButtonFactory`/`TransportControls`.
- Audio: generar, cancelar, reanudar y actualizar cola usan `ActionBar`.
- Storyboard: acciones de cabecera y cards usan `ActionBar`; métricas usan `MetricBadge`.
- Voces: asignación, importación y grabación usan `ActionButtonFactory`.
- Documento: perfil de lectura, rail de capas, acciones de bloque y estructura usan `ActionButtonFactory`.
- SideDock: rail y ocultar panel usan fábrica común.

## Guardarraíles

Se refuerza `GuiComponentReuseSourceTest` y se agrega `SharedActionComponentsSourceTest` para impedir regresiones en workspaces migrados.

## Fuera de alcance

Esta tanda no rediseña por completo la UX del Documento ni reduce `DocuPodcastShellViewModel`. Solo fija el contrato técnico/visual para que las siguientes tandas simplifiquen la interfaz sin dispersar controles.

## Siguiente paso

T59B debe limpiar la pantalla Documento y mover métricas/diagnóstico técnico fuera del flujo principal.
