# Auditoría de scaffolding visual y operativo

Este documento fija cómo desmontar la interfaz excesivamente técnica sin borrar la infraestructura útil.

## Problema observado

La base técnica de DocuPodcast Studio creció correctamente, pero la interfaz visible puede sentirse como cabina técnica. El usuario final esperado no es un ingeniero de audio ni un desarrollador: es una persona con conocimientos básicos de computadora/ofimática que quiere abrir un Word, escucharlo y opcionalmente asociar voces, audios o imágenes.

## Clasificación obligatoria

Cada elemento visible debe clasificarse como:

```text
Operación principal
Configuración
Avanzado
Infraestructura interna
```

## Operación principal

Debe permanecer en la pantalla Documento:

```text
Abrir Word/DOCX
Escuchar documento
Pausar/Reanudar
Reproducir desde aquí
Seleccionar oración/rango
Asignar voz/audio/emoción/imagen desde acción simple
Mini rail plegable
Estado humano: listo, preparando, reproduciendo, falta audio
```

## Configuración

Debe vivir fuera de la pantalla principal:

```text
motores TTS/STT
modelos
FFmpeg
buffer/prebuffer
rendimiento
carpetas
checksums
diagnóstico
parámetros avanzados
```

## Workspaces avanzados

Pueden existir, pero no deben ser ruta obligatoria para escuchar un documento:

```text
Guion
Audio jobs
Voces
Storyboard técnico
Diagnóstico
Exportaciones avanzadas
```

## Infraestructura interna

No debe aparecer como lenguaje principal para el usuario:

```text
manifest
job id
payload
gateway
checksum
command template
ffmpeg args
whisper args
chunks internos
sourceBlockIds
ScriptTextRange
```

## Reglas visuales

```text
sin paneles anidados innecesarios;
sin card dentro de card porque sí;
sin botones exageradamente redondeados;
sin decoración pesada;
si un contenedor no mejora comprensión, eliminarlo;
si un botón no hace acción clara, moverlo/ocultarlo;
si una vista requiere explicación técnica para operar, rediseñarla;
```

## Componentes GUI transversales

Se deben reutilizar componentes cuando haya repetición real:

```text
PrimaryActionStrip
EmptyStateView
SectionHeader
SettingsPageView
CollapsibleMediaRail
DocumentPageContainer
DocumentBlockView
StatusChip
```

No se deben crear wrappers por crear wrappers.

## Tests obligatorios futuros

```text
VisualScaffoldingAuditDocumentationTest
MainWorkspaceDoesNotExposeTechnicalWarehouseTest
NoExcessiveNestedPanelsSourceTest
NoRoadmapButtonSourceTest
WorkspaceUsesSharedComponentsSourceTest
```
