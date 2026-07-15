# T98A — Contrato del MenuBar

El MenuBar organiza acciones estructurales, transversales o poco frecuentes. No es la superficie principal de operación diaria.

## MenuBar final propuesto

```text
Archivo
  Nuevo proyecto…
  Abrir proyecto…
  Abrir recientes
  Salir

Proyecto
  Guardar proyecto
  Guardar proyecto como…
  Cerrar proyecto
  Abrir carpeta del proyecto
  Revisar integridad del proyecto

Fuente documental
  Abrir fuente documental…
  Refrescar desde fuente documental
  Abrir ubicación de la fuente documental

Ver
  Mostrar / ocultar rail derecho
  Ajustar ancho de lectura
  Pantalla completa

Lectura
  Escuchar documento
  Pausar
  Reanudar
  Detener
  Reproducir oración seleccionada

Exportar
  Exportar audio…
  Exportar paquete del proyecto…
  Exportar paquete de storyboard…
  Abrir carpeta de exportaciones

Configuración
  [clic directo → abre la ventana de Configuración]

Ayuda
  Guía de uso
  Acerca de DocuPodcast Studio
```

## Decisiones clave

### Archivo vs Proyecto

Se mantiene separación por ahora:

- `Archivo`: entrada/salida general de la app.
- `Proyecto`: operaciones sobre el proyecto `.docupodcast` actual.

### Fuente documental

Se usa `Fuente documental`, no `Documento`, para comunicar que es el archivo de entrada de la aplicación.

Objeto conceptual:

```text
Fuente documental = DOCX/PDF/TXT/Markdown abierto para leer/escuchar.
Proyecto = contenedor .docupodcast con capas, audios, imágenes, jobs y exportaciones.
```

No existirá `Reemplazar fuente documental…`. Para cambiar de fuente se usa `Abrir fuente documental…`, y la app debe guiar el cierre/guardado/creación de proyecto según corresponda.

### Ver y tamaño de lectura

El tamaño de lectura no se resuelve principalmente con múltiples ítems de menú. Debe tener un componente transversal visual en la esquina inferior derecha, junto a la barra de estado:

```text
[-] ━━━━━●━━━━━ 115% [+]
```

Nombre recomendado: `ReadingZoomControl`.

Semántica: tamaño de lectura/tamaño de letra que adapta la hoja al workspace, no zoom de lienzo.

### Lectura

`Reproducir selección` se reemplaza por `Reproducir oración seleccionada`, porque la interacción principal es por oración clicada, no por arrastre libre de texto.

### Exportar

Mientras no exista MP4 final generado desde la app, la salida visual correcta es:

```text
Exportar paquete de storyboard…
```

No `Exportar video final`.

### Configuración

`Configuración` debe abrir directamente la ventana secundaria. No debe aparecer como `Configuración > Configuración…`.

### Ayuda

Queda mínima:

```text
Ayuda
  Guía de uso
  Acerca de DocuPodcast Studio
```

No se duplican `Primeros pasos`, `Ver documentación del proyecto` ni guías redundantes.

## Prohibiciones

No deben aparecer en el MenuBar normal:

- Whisper;
- Audio a texto;
- Importar narración Markdown;
- Exportar diagnóstico como acción protagonista;
- Modo lectura limpia si no existe;
- Buscar/Ir a fragmento si no existe;
- placeholders visibles.
