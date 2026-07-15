# Decisiones de producto cerradas

## Documento

- El documento fuente se trata como **solo lectura**.
- El contenido original no se modifica con instrucciones internas.
- Las asignaciones de voz, imagen, tono y audio viven como capas del proyecto.
- La selección debe operar por frase/oración cuando exista un rango concreto.
- El rail Visual derecho muestra una tarjeta por frase/fragmento, tenga o no imagen.
- Si una frase tiene imagen en el rail, el sidebar izquierdo Imagen debe mostrar la misma imagen.
- El menú contextual del rail puede copiar la imagen al fragmento anterior o posterior.

## DOCX, tablas, imágenes y fórmulas

- Las imágenes embebidas en Word se muestran como bloques visuales no narrables.
- Las tablas se muestran como bloques visuales no narrables y deben migrar a grilla sobria.
- Fórmulas/LaTeX se detectan y notifican, pero no se renderizan como editor matemático.

## Playback

- El playbar controla reproducción, no generación.
- La generación tiene controles separados en la barra de estado.
- Anterior/siguiente navegan por frase/cue real.
- El botón anterior se deshabilita en la primera frase; siguiente se deshabilita en la última.
- Reproducir desde inicio debe existir.
- La duración de chunks debe basarse en WAV real, no texto estimado.

## Motores de voz

- En UX normal no se muestran nombres técnicos de motores.
- Etiquetas visibles: Voz IA avanzada, Voz local simple, Modo de prueba.
- Voz local simple es neutral y mínima; no tiene tonos ni varias voces humanas.
- Voz IA avanzada registra voces de referencia y tonos.
- Las URLs de descarga deben ser configurables sin obligar a editar código.
- Un motor no debe mostrarse como listo hasta generar una prueba real reproducible.

## Vista Voces

- Debe ser un mini workspace con sidebar interno.
- Debe tener ComboBox de motor activo dentro de la vista.
- Las voces se muestran en lista sobria, no tabla Excel ni tarjetas comprimidas.
- Neutral es obligatorio.
- Tonos no registrados no aparecen en Documento.
- Si falta tono específico al renderizar, se usa neutral.

## Video

- Exportar video abre una ventana secundaria de producto.
- Primero se elige calidad: 4K, 2K, 1080p o 720p.
- Luego se elige ubicación del `.mp4`.
- Solo se entrega el video final, sin archivos acompañantes visibles.
- FFmpeg debe prepararse automáticamente o gestionarse como dependencia interna.
