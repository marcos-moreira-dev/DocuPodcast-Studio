# T98C — Contrato del Ribbon

El Ribbon es la superficie de herramientas por modo. No debe duplicar el MenuBar ni reemplazar el sidebar. Cambiar de pestaña del Ribbon no necesariamente cambia el workspace; normalmente cambia las herramientas disponibles sobre el Documento.

## Pestañas finales

```text
Inicio | Lectura | Storyboard | Vista | Exportar
```

No existirán pestañas `Fragmento` ni `Medios`, porque esas operaciones viven en el sidebar izquierdo cuando hay oración seleccionada.

## Inicio

Acciones de arranque rápido.

```text
Inicio
  Fuente documental
    Abrir fuente documental
  Escucha rápida
    Escuchar documento
  Proyecto
    Guardar proyecto
```

Se tolera redundancia con MenuBar solo si se llama al mismo `AppCommandId`.

Botones con icono + texto, aire visual tipo Office y tooltip obligatorio.

## Lectura

Configura la lectura global del documento, no fragmentos puntuales.

```text
Lectura
  Voz del documento
    Voz predeterminada
  Ritmo
    Velocidad de lectura
  Preparación
    Preparar audio
    Regenerar audio pendiente
```

No debe mostrar estados de motor como botones. El estado pertenece a Configuración, StatusBar o diálogo.

La playbar flotante sigue siendo la dueña de `Escuchar/Pausar/Reanudar/Detener` durante el uso diario.

## Storyboard

Acciones globales de storyboard e imágenes, no asignación fina de la oración.

```text
Storyboard
  Panel visual
    Mostrar storyboard / rail derecho
  Imágenes
    Importar imágenes al proyecto
  Revisión
    Ver imágenes sin asignar
```

La acción `Asignar imagen a esta oración` pertenece al sidebar izquierdo.

## Vista

Herramientas de visualización del workspace.

```text
Vista
  Paneles
    Mostrar / ocultar rail derecho
  Lectura
    Ajustar ancho de lectura
    Restablecer tamaño de lectura
  Ventana
    Pantalla completa
```

El control principal de tamaño de lectura no vive como botones grandes del Ribbon; vive como `ReadingZoomControl` abajo a la derecha.

## Exportar

Salidas reales del proyecto.

```text
Exportar
  Audio
    Exportar audio
  Proyecto
    Exportar paquete del proyecto
  Storyboard
    Exportar paquete de storyboard
  Carpeta
    Abrir carpeta de exportaciones
```

Mientras no exista render MP4 final, se usa `paquete de storyboard`.

## Iconografía

- No emojis.
- Iconos vectoriales, consistentes, con grosor y estilo común.
- Botones principales con icono + texto.
- Botones compactos solo con icono si tienen tooltip claro.
- El Ribbon debe respirar visualmente como Word/WPS, no parecer una barra estrecha.

## Reglas técnicas

- Todo botón llama a `AppCommandId`.
- No usar `new Button(...)` ad-hoc para comandos repetibles.
- Usar componentes transversales: `RibbonButton`, `RibbonGroup` y futuros `RibbonTab`, `RibbonView`, `RibbonIcon`.
- No incluir Whisper, Audio a texto, diagnósticos técnicos, placeholders o funciones incompletas.
