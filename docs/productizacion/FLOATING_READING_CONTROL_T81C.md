# Contrato de producto — Barra flotante de lectura global T81C

## Decisión de UX

DocuPodcast Studio debe sentirse como un lector narrado de documentos. La persona usuaria no debe tener que ir a una vista de audio, narración avanzada o jobs para iniciar la lectura. El control de lectura debe estar en el workspace Documento y debe quedar visualmente cerca de la hoja.

La barra flotante de T81C cumple ese rol: se ubica debajo de las superficies superiores de navegación y encima del documento renderizado.

## Principio rector

```text
La lectura global del documento vive sobre la hoja.
Las acciones por oración viven en el sidebar contextual.
La configuración técnica vive en Configuración.
```

Este principio evita que el workspace central se convierta en cabina técnica.

## Composición visual

La superficie queda conceptualmente así:

```text
Menu bar
Toolbar general temporal
Barra flotante de lectura global
Documento renderizado
```

La barra no debe sustituir al futuro inspector contextual. Su alcance es global: iniciar, pausar, reanudar o detener lectura del documento/proyecto.

## Componentes permitidos

La barra debe componerse con componentes transversales estilizados:

- `PrimaryActionStrip`
- `TransportControls`
- `ActionButtonFactory`
- `AppStyles`

No se debe crear una botonera manual con estilos locales dispersos en `DocumentWorkspaceView`.

## Lenguaje de usuario

Permitido:

- Escuchar documento
- Reproducir selección
- Reproducir desde aquí
- Pausar
- Reanudar
- Detener
- Refrescar contenido

No permitido en esta superficie:

- manifest
- audio job
- chunk
- gateway
- render queue
- engine
- TTS command
- STT command
- GPU/NVENC/CUDA

Si algo técnico falla o falta, el mensaje debe resumirse de forma humana y remitir a Configuración o Diagnóstico cuando corresponda.

## Interacción esperada

### Sin documento

La acción primaria conserva texto tipo `Escuchar documento`, pero el hint indica que primero se debe abrir un DOCX/PDF/TXT/Markdown. La barra no debe inventar un flujo sin documento.

### Con documento sin audio listo

La acción primaria inicia el flujo inteligente existente: preparar narración si hace falta, generar audio si corresponde o reproducir si ya existe manifest.

### Con selección

La acción primaria cambia a `Reproducir selección` o `Reproducir desde aquí`, según el rango o bloque seleccionado.

### Durante reproducción

Los controles de transporte permiten pausar, reanudar y detener sin cambiar de workspace.

## Relación con Refrescar contenido

`Refrescar contenido` se mantiene como acción secundaria porque el documento fuente es solo lectura y puede cambiar fuera de DocuPodcast. La acción no pertenece a configuración ni a diagnóstico; pertenece al workspace Documento.

## Roadmap inmediato

Después de T81C:

1. T81D — Sidebar izquierdo contextual.
2. T81E — Sidebar derecho de miniaturas/medios.
3. T81F — Toolbar con iconos y grupos.
4. T81G — Retiro o degradación de vistas secundarias redundantes.
5. T82 — Release Candidate.
