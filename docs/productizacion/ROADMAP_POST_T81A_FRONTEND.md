# Roadmap post T81A — Rediseño frontal guiado

## Estado después de T81A

La navegación principal queda reorientada a un patrón común de aplicaciones de escritorio. El usuario ve menús normales y una pantalla de inicio más comercial/operativa. Las superficies técnicas ya no aparecen como menús principales.

## Próximas tandas recomendadas

### T81B — Documento limpio

Objetivo: convertir el workspace Documento en una hoja de lectura sobria.

Alcance:

- quitar `styleName`, `styleId`, `readingProfile`, `classificationSource`, `FirstParagraph` y otros metadatos del lector;
- mover esos datos a Detalles técnicos o Diagnóstico;
- reducir bordes excesivos y ruido visual;
- mantener selección de oración/rango y seguimiento activo.

### T81C — Barra flotante de lectura global

Objetivo: poner debajo de la toolbar y encima de la hoja un control concreto para la lectura completa.

Estados esperados:

- `Iniciar lectura en voz alta`;
- `Preparar y escuchar`;
- `Pausar`;
- `Reanudar`;
- `Detener`.

### T81D — Inspector izquierdo contextual

Objetivo: sidebar izquierdo siempre disponible, estilo Blender, para operar el fragmento seleccionado.

Módulos propuestos:

- `Detalles`;
- `Audio / Narración`;
- `Imagen`.

En Audio/Narración:

- origen `Voz IA`;
- origen `Audio del computador`;
- acciones `Elegir audio…` y `Extraer audio de video…`;
- emoción/estilo solo cuando el origen sea `Voz IA`;
- `Quitar audio asignado` siempre disponible cuando haya audio.

### T81E — Rail derecho de miniaturas/medios

Objetivo: panel visual simple, no formulario.

Cada elemento debe mostrar:

- miniatura;
- fragmento relacionado;
- breve descripción o nombre del archivo.

Clic en tarjeta debe navegar al fragmento del documento.

### T81F — Toolbar con iconos y grupos

Objetivo: reducir el ribbon textual.

Grupos propuestos:

- Documento: abrir;
- Lectura: escuchar/pausar/detener;
- Vista: pantalla completa;
- Exportar: exportar.

### T81G — Retiro o degradación de vistas secundarias redundantes

Objetivo: sacar de la navegación principal las vistas que compiten con Documento.

Candidatas:

- Narración avanzada;
- Voces;
- Audio;
- Storyboard;
- Reproducción.

El cerebro no se elimina. Se elimina o esconde la superficie visual redundante.

## Regla transversal

Cada tanda debe usar componentes GUI transversales. No se aceptan nuevas botoneras hardcodeadas en workspaces.
