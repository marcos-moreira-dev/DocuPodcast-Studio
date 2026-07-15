# T98B — Contrato de vistas y workspaces

DocuPodcast no debe ser una app de muchas vistas. El producto se centra en leer/escuchar una fuente documental y, opcionalmente, asociar voces, clips, imágenes y storyboard.

## Vistas principales

```text
Inicio
Documento
Voces
```

### Inicio

Vista funcional y propagandística. Debe tener estética moderna tipo Microsoft actual: colores sólidos, formas geométricas simples, círculos/polígonos decorativos y limpieza visual.

Función:

- abrir fuente documental;
- abrir proyecto;
- crear proyecto;
- acceder a guía de uso.

No debe mostrar métricas técnicas ni parecer dashboard de mantenimiento.

### Documento

Vista principal del producto.

Contiene:

- hoja limpia de lectura;
- playbar flotante;
- sidebar izquierdo contextual;
- rail derecho retráctil;
- status bar con `ReadingZoomControl` abajo a la derecha.

Aquí ocurre el uso principal:

- leer;
- escuchar;
- seleccionar oración;
- asignar voz IA;
- asignar audio del computador;
- asignar imagen;
- navegar storyboard/miniaturas;
- preparar audio.

### Voces

Workspace secundario real, no diálogo pequeño.

Función:

- ver voces disponibles;
- agregar nueva voz;
- subir muestra de voz;
- grabar muestra leyendo una frase larga;
- iniciar/detener grabación;
- guardar voz con nombre;
- probar voz;
- eliminar voz.

No se mezcla con Whisper. No transcribe. Solo registra/procesa voces de referencia para Coqui/XTTS.

## Vistas eliminadas como workspaces normales

### Guion

No debe existir como vista. Si el usuario quiere trabajar con un guion, abre `guion.docx` como fuente documental.

### Audio Jobs

No debe existir como vista. Debe ser overlay/panel de progreso para generación/renderizado.

### Diagnóstico

No debe existir como vista. Debe ser acción puntual desde Proyecto/Configuración con reporte breve o detalle expandible.

### Storyboard

No debe existir como vista principal. Vive en el rail derecho dentro de Documento.

### Exportación

No debe existir como workspace principal. Debe ser diálogo, flujo corto o ventana secundaria.

### Configuración

Ventana secundaria, no workspace.

## Superficies secundarias

```text
Configuración → ventana secundaria.
Guía de uso → ventana secundaria.
Exportar → diálogo/flujo corto.
Procesos largos → overlay/panel de progreso.
Integridad → diálogo/reporte.
```
