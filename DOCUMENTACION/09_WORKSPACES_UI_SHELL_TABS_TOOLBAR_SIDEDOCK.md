# 09 — Workspaces, Shell, Tabs, Toolbar y SideDock

La UI debe tomar como referencia principal a DMS.

## Shell

```text
Menú superior
Toolbar global
Toolbar contextual
Tabs de proyectos
Workspace central
Statusbar / progreso compacto
```

## Workspaces

```text
WELCOME_HOME
DOCUMENT_READER
SCRIPT_EDITOR
STORYBOARD
AUDIO_JOBS
VOICE_LIBRARY
OBSERVABILITY
SETTINGS
```

## Tabs

Cada proyecto `.docupodcast` puede abrirse como tab. Debe existir dirty state, cierre con confirmación, reordenamiento y activación.

## Toolbar global

Acciones generales:

```text
Nuevo
Abrir proyecto
Abrir Word/DOCX
Guardar
Importar guion Markdown
Biblioteca de voces
Exportar
Ayuda
```

## Toolbar contextual

Depende del workspace activo:

- Documento: analizar estructura, perfil de lectura, crear guion.
- Guion: asignar voz, estilo, personaje, dividir/unir segmentos.
- Storyboard: importar imagen, asociar imagen, preview.
- Audio: generar, pausar, cancelar, reintentar, abrir carpeta.
- Voces: nueva voz, probar voz, asignar personaje.

## SideDock

Un módulo activo a la vez. No debe comprimir excesivamente el centro. Módulos:

```text
Estructura
Segmentos
Propiedades
Personajes/Voces
Estilos
Medios
Jobs
Logs
Diagnóstico
Ayuda
```
