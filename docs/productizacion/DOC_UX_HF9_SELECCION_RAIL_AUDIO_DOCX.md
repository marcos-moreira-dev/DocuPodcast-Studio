# DOC-UX-HF9 — Contrato de selección fina, rail visual y audio capability-driven

## Contrato de selección

Cuando el usuario selecciona una oración, el estado de selección es el rango (`DocumentTextRange`), no el bloque completo. El bloque puede seguir siendo el contenedor de scroll, pero no debe aparecer como la selección principal.

Cuando el playback avanza por unidades `SEG-xxx-Uyyy`, la hoja debe seleccionar la oración correspondiente si el bloque de origen conserva sus spans.

## Contrato del rail Visual

El rail derecho debe:

- listar todos los fragmentos disponibles en scroll;
- reflejar las capas de imagen actuales del proyecto;
- mostrar miniatura real si el asset de imagen existe;
- seleccionar el mismo fragmento/rango que el workspace Documento al hacer clic.

El rail no debe esconder elementos tras un límite fijo de ocho tarjetas.

## Contrato del sidebar Audio

El origen principal del sidebar Audio se deriva de `VoiceEngineCapabilityProfile`:

- motor avanzado → `Voz IA avanzada`;
- Piper/intermedio → `Voz local simple`;
- mock → `Modo de prueba`.

Piper no expone controles de emociones, tonos expresivos ni voces por muestra humana.

## Contrato DOCX visual

El importador DOCX debe conservar:

- imágenes embebidas como bloques `IMAGE_NOTICE` con base64 cuando sea posible;
- tablas como bloques `TABLE_NOTICE` con metadata de filas/columnas/vista previa;
- fórmulas/LaTeX como `MATH_NOTICE` identificado sin render matemático.

Estos bloques no son narrables por defecto.
