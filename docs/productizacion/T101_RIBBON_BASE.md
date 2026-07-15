# T101 — Ribbon base

## Objetivo

Implementar el Ribbon como herramienta por modo, con aire Word/WPS pero sin copiar su cantidad de opciones.

## Pestañas finales

```text
Inicio
Lectura
Storyboard
Vista
Exportar
```

No existen pestañas `Fragmento` ni `Medios`, porque esos ajustes viven en el Sidebar izquierdo.

## Inicio

```text
Fuente documental
  Abrir fuente documental

Escucha rápida
  Escuchar documento

Proyecto
  Guardar proyecto
```

## Lectura

```text
Voz del documento
  Voz predeterminada

Ritmo
  Velocidad global

Preparación
  Preparar audio
  Regenerar audio pendiente
```

El Ribbon no muestra estados de motor como si fueran botones; esos estados van a Configuración, StatusBar o mensajes.

## Storyboard

```text
Panel visual
  Mostrar storyboard / rail derecho

Imágenes
  Importar imágenes al proyecto

Revisión
  Ver imágenes sin asignar
```

La asignación de imagen a una oración vive en el Sidebar.

## Vista

```text
Paneles
  Mostrar / ocultar rail derecho

Lectura
  Ajustar ancho de lectura
  Restablecer tamaño de lectura

Ventana
  Pantalla completa
```

## Exportar

```text
Audio
  Exportar audio

Proyecto
  Exportar paquete del proyecto

Storyboard
  Exportar paquete de storyboard

Carpeta
  Abrir carpeta de exportaciones
```

## Iconografía

- Iconos vectoriales, no emojis.
- Icono + texto en acciones principales.
- Tooltips obligatorios.
- Aire visual con respiración tipo Word.
- Todos los botones llaman `AppCommandId`.
