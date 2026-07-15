# Notas consolidadas de conversación para continuidad del proyecto

Este documento recoge las decisiones surgidas durante las tandas de lectura e implementación de T76B a T81G y las observaciones de UX/UI hechas por el usuario. Debe leerse antes de abrir una nueva conversación o antes de continuar el rediseño visual.

## 1. Norte del producto

DocuPodcast Studio no es Word, no es un editor de video y no debe sentirse como una cabina técnica. Su identidad V1 es:

> Lector narrado local que abre un documento fuente, permite escucharlo, seleccionar fragmentos, asignar capas opcionales de audio, voz, emoción o imagen, y exportar evidencias/salidas sin modificar el documento original.

El flujo rector sigue siendo:

```text
Abrir documento → leer/escuchar → seleccionar oración → ajustar audio/imagen/emoción si hace falta → exportar
```

## 2. Documento fuente vs proyecto

Hay dos conceptos distintos:

- **Proyecto `.docupodcast`**: carpeta contenedora con manifiesto, assets, jobs, documentos materializados, audio, imágenes, video package y reportes.
- **Documento fuente**: archivo que el usuario abre como contenido principal. Puede ser DOCX, PDF con texto nativo, TXT o Markdown.

Regla: el menú **Archivo** trabaja sobre proyecto; el menú **Documento** trabaja sobre documento fuente.

## 3. Carpeta contenedora

Al crear o guardar un proyecto nuevo, la app debe crear una carpeta contenedora. No debe dejar múltiples carpetas sueltas directamente en el escritorio o en la carpeta elegida por el usuario.

Ejemplo correcto:

```text
Escritorio/MiObra/MiObra.docupodcast.json
Escritorio/MiObra/document/
Escritorio/MiObra/assets/
Escritorio/MiObra/media/
Escritorio/MiObra/jobs/
Escritorio/MiObra/reports/
```

Esto es obligatorio porque el producto manejará muchos recursos: MP3, WAV, imágenes, videos usados para extraer audio, jobs, manifests y exports.

## 4. Configuración como bodega técnica

La configuración puede ser técnica. Puede hablar de motores, modelos, CPU/GPU, FFmpeg, Whisper, Piper, XTTS, rutas, comandos, timeouts y buffers.

Pero debe cumplir estas reglas:

- Se abre únicamente desde el menú **Configuración**.
- No aparece como tarjeta en la bienvenida.
- No aparece como acceso de toolbar principal.
- No invade Documento.
- No debe obligar al usuario normal a entender comandos crudos para usar la app en modo básico.

## 5. Menu bar

El menu bar debe ser común y típico de escritorio. No debe listar módulos técnicos como menús principales.

Menús esperados:

```text
Archivo
Editar
Ver
Documento
Lectura
Herramientas
Exportar
Configuración
Ayuda
```

No deben ser menús principales:

```text
Narración
Voz
Storyboard
Audio
Reproducción
```

Esas capacidades existen, pero como herramientas avanzadas, configuración o acciones contextuales.

## 6. Toolbar

La toolbar debe tener iconos, grupos, tooltip y texto corto. No debe ser una fila de botones textuales administrativos.

Grupos esperados:

```text
Documento: Inicio / Abrir documento
Lectura: Escuchar documento
Salida: Exportar
Vista: Pantalla completa
```

No deben volver a la toolbar global como botones permanentes:

```text
Narración avanzada
Voces
Audio
Storyboard
Jobs
Diagnóstico
```

## 7. Workspace Documento

El workspace central debe servir para leer y escuchar. Debe verse como una hoja/documento cómodo, no como una matriz técnica.

No mostrar en la hoja:

```text
styleName
styleId
readingProfile
classificationSource
FirstParagraph
manifest
jobId
chunk
gateway
```

Estos datos solo pertenecen a Propiedades, Detalles técnicos, Diagnóstico o herramientas avanzadas.

## 8. Barra flotante de lectura

Dentro del workspace central, debajo de toolbar y encima de la hoja, debe existir una barra flotante de lectura global.

Función:

- Escuchar documento.
- Preparar y escuchar si falta audio.
- Reproducir desde selección si existe selección.
- Pausar.
- Reanudar.
- Detener.
- Refrescar contenido.

Regla visual: el texto largo explicativo vive en tooltip. La barra no debe comprimir botones ni meter párrafos permanentes.

## 9. Sidebar izquierdo contextual

El sidebar izquierdo queda como inspector contextual por oración/fragmento seleccionado. Debe estar disponible y organizado por módulos.

Módulos principales:

```text
Detalles
Audio / Narración
Imagen
```

### Detalles

Muestra selección, tipo de fragmento, ubicación, estado de solo lectura y datos útiles. Detalles técnicos van colapsados o en diagnóstico.

### Audio / Narración

Debe usar la nomenclatura correcta:

```text
Voz IA
Audio del computador
```

Si el origen es **Voz IA**, se muestran opciones como voz predeterminada, elegir/agregar voz, generar audio, emoción/estilo y quitar audio asignado.

Si el origen es **Audio del computador**, se muestran acciones directas:

```text
Elegir audio…
Extraer audio de video…
Reemplazar audio
Quitar audio asignado
```

La emoción/estilo solo tiene sentido con Voz IA y no debe aparecer para audio grabado/importado.

### Imagen

Debe concentrar:

```text
Elegir imagen…
Reemplazar imagen
Quitar imagen
Ir a miniatura
```

## 10. Sidebar derecho

El sidebar derecho es rail visual/navegacional. No debe ser formulario ni contener la botonera de asignación.

Debe mostrar:

- Miniatura o placeholder.
- Fragmento relacionado.
- Descripción breve o nombre de archivo.

Acción principal:

```text
clic en miniatura → ir al fragmento asociado en el documento
```

## 11. Audio del computador y nombres responsables

La UI no debe dividir en botones redundantes como “efecto de sonido”, “humano hablando”, “ambiente”, etc.

Debe haber una sola lógica de **Audio del computador**. El usuario es responsable de nombrar sus archivos con claridad.

Microcopy recomendada:

```text
Consejo: usa nombres claros para tus audios, como “Lucía hablando” o “pájaros cantando”.
```

## 12. Entrada flexible de media

El cerebro acepta o está preparado para:

```text
MP3
WAV
video para extraer solo audio: MP4, MOV, MKV, WEBM
```

La UI debe presentar esto como una acción simple, no como varias categorías técnicas.

## 13. CPU/GPU y rendimiento

La política de cómputo ya debe entenderse como parte del cerebro:

```text
AUTO
CPU_ONLY
PREFER_GPU
SPECIFIC_DEVICE
```

Y propósitos:

```text
TTS
STT
VIDEO_RENDER
```

Esto vive en Configuración/Diagnóstico, no en Documento.

## 14. Vistas secundarias

Las vistas secundarias no se eliminan necesariamente del código, pero sí se degradan visualmente:

```text
Guion narrable
Jobs de audio
Voces
Storyboard completo
```

Quedan como herramientas avanzadas, no como superficies principales.

Superficies principales:

```text
Inicio
Documento
```

## 15. Principio de componentes GUI transversales

Toda nueva superficie debe usar componentes GUI estilizados existentes o crear componentes transversales reutilizables.

No usar botoneras ad-hoc con `new Button(...)` dentro de workspaces.

Componentes relevantes:

```text
ActionButtonFactory
ToolbarActionButton
FloatingReadingControlBar
TransportControls
PrimaryActionStrip
MediaThumbnailCard
InfoBadge
SectionHeader
RailActionRow
EmptyStateView
```

## 16. Estado actual después de T81G

El cerebro V1 quedó congelado en T80. T81A-T81G avanzaron el rediseño visual sin cambiar la lógica central.

Queda pendiente una fase importante: integración real de motores IA para TTS/STT, polish visual fino, pruebas manuales con documentos reales, packaging y release candidate.

