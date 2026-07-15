# Onboarding del proyecto DocuPodcast Studio

Este documento fija el contexto técnico y de producto para continuar el desarrollo aunque se pierda el chat original.

## Objetivo del producto

DocuPodcast Studio será una aplicación JavaFX autocontenida para convertir documentos largos, especialmente Word/DOCX, en audio local y storyboards vivos.

El flujo principal es:

```text
Word/DOCX → Documento importado → Guion narrable → Audio por segmentos → Storyboard vivo / Podcast final
```

## Por qué no es un mini Word

La aplicación no compite con Microsoft Word. Word es la fuente de entrada. DocuPodcast Studio crea una representación interna para estudio, narración, voces, audio y storyboard.

## Reglas de alcance

- DOCX es entrada prioritaria.
- PDF se soportará como entrada secundaria porque puede perder orden semántico.
- Markdown DocuPodcast será puente con IA/humanos.
- `.docupodcast.json` será la persistencia editable.
- El motor de voz se encapsula; no debe invadirse la UI con detalles de Python, ONNX o binarios.
- La app puede usar Python empaquetado si eso mejora calidad de voz y reduce fricción.
- No debe haber backend/API visible para el usuario.
- Audio siempre por segmentos, no por documento entero.
- Progreso y ETA son parte central de la UX.
- Storyboard vivo usa imágenes aportadas por el usuario.

## Piezas referenciales

### Domain Model Studio/UENS

Referencia principal para:

- shell desktop;
- tabs;
- toolbar global/contextual;
- SideDock;
- workspaces estructurados y visuales;
- tema claro;
- guía integrada;
- persistencia JSON;
- recursos IA;
- tests fuente.

### Fractal Render Studio

Referencia principal para:

- trabajos largos;
- colas;
- progreso;
- cancelación cooperativa;
- generación por unidades;
- carpeta por job.

## Estado de esta tanda

Esta Tanda 1 crea:

- estructura Maven JavaFX;
- app mínima con pantalla de bienvenida;
- toolbar inicial;
- placeholder de workspaces;
- CSS claro;
- documentación viva;
- scripts base.

No implementa todavía:

- importación DOCX real;
- persistencia `.docupodcast.json`;
- motor TTS;
- audio;
- storyboard canvas;
- exportaciones reales.
