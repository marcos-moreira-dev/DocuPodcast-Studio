# T99C — Deshuesadero visual mínimo

## Objetivo

Quitar elementos visualmente malos o heredados antes de construir la nueva GUI.

## Alcance

- Quitar etiquetas truncadas `Det/Aud/Img`.
- Quitar emojis como iconografía final.
- Quitar textos de relleno permanentes.
- Quitar etiquetas visibles `Párrafo`, `Título`, `Subtítulo` dentro de la hoja.
- Quitar placeholders restantes visibles.
- Preparar CSS base para Ribbon, Sidebar, Rail, Playbar y StatusBar.

## Criterios

- La hoja debe leerse como documento.
- Los indicadores deben ser mínimos: puntos, iconos sutiles, color discreto o tooltip.
- Ningún botón visible debe parecer prototipo.

## Criterio de aceptación

- No hay labels truncados en navegación lateral.
- No hay emojis como iconos finales en nuevas superficies.
- No hay textos técnicos permanentes en el workspace Documento.

## Estado implementado en T99C

T99C quedó implementada como limpieza visual mínima sobre la base verde T99B-HF4. La hoja del Documento ya no muestra textos permanentes de onboarding ni etiquetas visibles de tipo de bloque; el SideDock reemplaza `Det/Aud/Img` por rótulos legibles; la toolbar deja de usar emojis de prototipo y pasa a marcadores textuales sobrios. Los detalles quedan documentados en `T99C_DESHUESADERO_VISUAL_MINIMO_IMPLEMENTADO.md`.
