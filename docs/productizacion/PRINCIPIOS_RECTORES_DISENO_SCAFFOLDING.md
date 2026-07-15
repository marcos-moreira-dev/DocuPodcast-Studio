# Principios rectores de diseño y scaffolding — DocuPodcast Studio

## Propósito

Este documento fija los criterios rectores para rediseñar la interfaz sin perder la arquitectura interna ya construida. DocuPodcast debe conservar su complejidad técnica detrás de la escena, pero presentarse al usuario como un lector narrado de Word/DOCX con capas multimedia opcionales.

## Brújula de producto

DocuPodcast no debe sentirse como una cabina de jobs, manifest, gateways o pipelines. Debe sentirse como:

```text
abrir Word → leer cómodo → escuchar → ajustar capas si hace falta → exportar
```

La pantalla Documento es la experiencia principal. Guion, Audio, Storyboard, Voces, Configuración y Diagnóstico son superficies de apoyo o modo avanzado.

## Principio 1 — Documento primero

La interfaz debe responder siempre esta pregunta: ¿qué necesita el usuario para leer y escuchar su documento ahora?

Visible para usuario normal:

```text
Abrir Word/DOCX
Escuchar documento
Pausar / reanudar
Reproducir desde aquí
Texto cómodo
Resaltado de bloque/oración activa
Mini rail plegable de capas
Guardar / exportar
```

Oculto o secundario:

```text
jobs
manifest
gateway
chunks
prebuffer técnico
offsets
bloques/narrables/diagnósticos permanentes
tablas de validación técnicas
```

## Principio 2 — Menos scaffolding visible

Toda métrica o diagnóstico debe cumplir una condición: ayuda a decidir una acción inmediata. Si no ayuda, debe ir a Diagnóstico, Propiedades, Configuración o reporte.

Ejemplos que no deben dominar la pantalla principal:

```text
Bloques: 186
Narrables: 185
Estructura: 24
Ignorados: 1
Buffer: inicia con 5 fragmentos...
texto narrable / estilo Word
Manifest de playback
```

## Principio 3 — Complejidad progresiva

La app debe tener tres niveles:

| Nivel | Usuario | Superficie |
|---|---|---|
| Básico | Quiere escuchar Word | Documento + mini controles |
| Intermedio | Quiere corregir narración | Capas, Guion, mini rail |
| Avanzado | Quiere motores/export/render | Configuración, Audio, Diagnóstico, Storyboard |

Ningún usuario debe verse obligado a abrir Guion o Audio para probar el flujo mínimo.

## Principio 4 — Acciones por intención, no por tecnología

Las acciones deben nombrarse por lo que el usuario quiere lograr:

| Evitar | Preferir |
|---|---|
| Generar manifest | Preparar reproducción |
| Ejecutar job de audio | Preparar audio |
| Build script | Crear guion narrable |
| Asociar asset pendiente | Elegir imagen / Asociar imagen |
| Render package | Preparar paquete de video |

Si una acción todavía no produce un resultado final real, debe decirlo de forma honesta.

## Principio 5 — Componentes GUI transversales

Los controles repetibles deben salir de componentes compartidos, no de JavaFX ad hoc por vista.

Componentes obligatorios o esperados:

```text
PrimaryActionStrip
ActionButtonFactory
ActionBar / ActionGroup
TransportControls
RailActionRow
MetricBadge / InfoBadge
DiagnosticCard
SectionHeader
EmptyStateView
SettingsPageView
CollapsibleMediaRail
```

Regla: una vista puede componer acciones, pero no inventar estilos ni botoneras propias para patrones ya existentes.

## Principio 6 — SideDock y rail como apoyo, no como pared de opciones

El SideDock debe ser plegado por defecto cuando el usuario está leyendo. Sus módulos deben tener nombres comprensibles y ayuda operativa clara. El rail multimedia debe mostrar capas reales y assets reales; no debe hacer creer que algo quedó asociado si solo se generó un placeholder.

## Principio 7 — Contrato honesto de capacidades

Cada capacidad debe declararse como:

```text
Disponible ahora
Disponible como paquete/preparación
Pendiente de motor externo/configuración
Pendiente de implementación
```

Esto aplica especialmente a:

```text
MP4 final real
FFmpeg embebido
XTTS/Coqui
Piper
Whisper/STT
voz humana/clonación/autorización
emociones/estilos de interpretación
```

## Principio 8 — Persistencia antes de prometer flujo avanzado

No se debe promover como flujo final una acción que no sobreviva a guardar, cerrar y reabrir. Capas, voces, imágenes, storyboard, audio, manifest, jobs y settings deben tener round-trip verificable antes de considerarse producto.

## Principio 9 — Diseño claro, sobrio y de escritorio

La UI debe ser clara, luminosa, formal y legible. DocuPodcast no es dashboard web ni consola técnica. Las pantallas deben usar CSS modular, tokens compartidos y controles consistentes. No deben existir tokens CSS huérfanos ni estilos inline.

## Principio 10 — Refactor guiado por uso real

El refactor debe seguir al smoke, no sustituirlo. Primero se valida que el flujo mínimo funciona; luego se decide qué esconder, qué simplificar y qué extraer a coordinadores.

Orden rector:

```text
base verde → smoke exploratorio → criterios de diseño → componentes transversales → limpieza UX → refactor coordinadores → round-trip → configuración → smoke real → RC
```

## Reglas de decisión para próximas tandas

1. Si una pantalla requiere jerga técnica para hacer lo básico, se rediseña.
2. Si una acción visible no tiene resultado real, se renombra o se mueve a Avanzado.
3. Si una vista crea botones repetidos, se migra a componentes transversales.
4. Si navegar marca el proyecto como modificado, se separa dirty de contenido vs estado visual.
5. Si una capacidad no hace round-trip, no se vende como cerrada.
6. Si aparece una advertencia CSS en smoke, se corrige o se documenta como deuda bloqueante de polish.
