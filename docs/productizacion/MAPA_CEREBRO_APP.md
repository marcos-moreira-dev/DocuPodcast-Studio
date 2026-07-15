# Mapa del cerebro de DocuPodcast Studio

## Propósito

DocuPodcast Studio no usa un backend web, pero sí tiene un equivalente funcional al backend dentro de la app desktop. Ese cerebro gobierna dominio, casos de uso, coordinadores, servicios, persistencia, jobs, audio, playback, video, configuración y validaciones.

Este documento define cómo debe entenderse y auditarse ese cerebro antes de continuar con un rediseño visual profundo.

## Principio rector

```text
La cara puede evolucionar; el cerebro debe ser estable, auditable y recuperable.
```

La UI puede cambiar de forma, pero el flujo interno debe tener responsabilidades claras:

```text
Word/DOCX/PDF/Markdown/TXT fuente solo lectura → Documento narrable del proyecto → capas/proyecciones internas → audio/jobs → playback → storyboard/video opcional → exportación → persistencia
```

## Capas actuales

| Capa | Rol | Regla |
|---|---|---|
| `domain` | Modelos, invariantes y políticas puras. | No debe conocer JavaFX, infraestructura ni presentación. |
| `application` | Casos de uso y orquestación de reglas. | No debe depender de vistas ni JavaFX. |
| `infrastructure` | Repositorios, importadores, gateways y archivos. | Debe implementar puertos sin filtrar detalles al dominio. |
| `presentation` | Vistas, ViewModels, coordinadores de UI y feedback. | No debe absorber reglas de negocio ni persistencia directa. |
| `bootstrap` | Composición de servicios y runtime. | Debe cablear, no decidir producto. |

## Subsistemas del cerebro

### Proyecto y sesión

Responsables actuales: `ProjectWorkflowCoordinator`, `ProjectSession`, `ProjectSessionCoordinator`, repositorios JSON/workspace.

Debe garantizar:

- crear proyecto;
- abrir proyecto;
- hidratar Documento narrable/proyecciones/storyboard/voces/jobs;
- guardar sin pérdida;
- cerrar con confirmación;
- no marcar dirty por navegación simple.

Riesgo actual: el estado visual puede ensuciar el proyecto si se persiste como cambio de contenido.

### Documento

Responsables actuales: importador DOCX, `ReadableDocument`, perfiles de lectura, paneles de Documento y selección.

Debe garantizar:

- Word/DOCX como fuente principal;
- documento original no modificado;
- bloques narrables y diagnósticos claros;
- selección por bloque/oración/rango;
- lenguaje de usuario en la pantalla principal.

Riesgo actual: demasiado diagnóstico técnico visible en Documento.


### Documentos fuente solo lectura

Responsables actuales/futuros: importadores/lectores de Word/DOCX, PDF, Markdown/MD y TXT, `ReadableDocument`, perfiles de lectura y persistencia de artefactos del proyecto.

Debe garantizar:

- abrir documentos fuente en modo solo lectura;
- no sobrescribir Word/PDF/Markdown/TXT original;
- derivar proyecciones de narración, capas, audio, storyboard, transcripciones y exportaciones como artefactos del proyecto;
- distinguir Markdown abierto como documento fuente de Markdown importado como proyección `docupodcast-script-v1`;
- mantener una frontera clara entre fuente inmutable y artefacto editable del proyecto.

Riesgo actual/futuro: que una pantalla inspirada en Word/WPS comunique edición de fuente cuando el alcance V1 es lectura y producción por capas.

### Documento narrable como raíz

Responsables actuales/futuros: `ReadableDocument`, perfiles de lectura, selección documental, playback resaltado, capas narrativas y materialización del documento dentro del proyecto.

Debe garantizar:

- actuar como objeto padre visible de la experiencia V1;
- abrir Word/DOCX, PDF, Markdown/MD y TXT como fuente solo lectura;
- permitir leer, escuchar, seleccionar y asignar capas sin exigir al usuario entrar a Guion;
- mantener diagnóstico técnico fuera del flujo normal;
- conservar trazabilidad hacia la fuente inmutable.

Riesgo actual: algunas pantallas/documentos todavía comunican Guion como paso central cuando debería ser proyección interna/avanzada.

### Proyección interna de narración / guion compatible

Responsables actuales: casos de uso de construcción/importación/validación, repositorio de guion, workspace avanzado de Guion.

Debe garantizar:

- segmentar el Documento narrable para TTS/audio;
- conservar relación con bloques/rangos fuente;
- permitir edición avanzada cuando el usuario lo solicite;
- sostener compatibilidad con `docupodcast-script-v1`;
- no ser obligatorio para escuchar un documento simple.

Riesgo actual: offsets entre documento y proyección de narración pueden desplazarse por prefijos narrativos.

### Capas narrativas

Responsables actuales: `NarrativeLayerAssignment`, política de solapamientos, rail de capas, JSON de proyecto.

Debe garantizar:

- asignar voz/audio/emoción/imagen/ambiente/nota sin tocar Word;
- proteger capas primarias incompatibles;
- validar targets reales cuando la capacidad ya exista;
- persistir y reabrir asignaciones.

Riesgo actual: algunos targets siguen siendo placeholders.

### Voz, TTS y STT

Responsables actuales: biblioteca de voces, configuración de motor, gateways TTS/STT, importación/grabación de muestras.

Debe garantizar:

- distinguir mock, motor real, Piper/XTTS/Coqui y Whisper;
- no prometer emoción/voz si el motor no lo soporta;
- permitir diagnóstico y prueba guiada;
- guardar configuración de usuario.

Riesgo actual: configuración aún es más informativa que operativa.

### Audio, jobs y playback

Responsables actuales: audio jobs, manifest, buffer policy, playback cursor, reproductor de segmentos.

Debe garantizar:

- generar audio por segmentos;
- reanudar o recuperar jobs;
- iniciar playback con buffer;
- pausar si falta fragmento y continuar de forma entendible;
- separar observabilidad técnica de experiencia normal.

Riesgo actual: la orquestación está demasiado concentrada en `DocuPodcastShellViewModel`.

### Storyboard, imagen y video

Responsables actuales: storyboard document, repositorios de imagen/storyboard, video plan, FFmpeg locator, export package.

Debe garantizar:

- asociar imágenes reales a segmentos/rangos;
- permitir reutilizar una imagen en varios segmentos;
- distinguir paquete renderizable de MP4 real;
- bloquear o informar durante render.

Riesgo actual: video todavía es paquete/contrato, no MP4 final real.

### Configuración y diagnóstico

Responsables actuales: settings views, catálogos de motor/modelo, diagnósticos de proceso.

Debe garantizar:

- configurar rutas y modelos sin consola;
- probar motor/voz/STT;
- guardar preferencias;
- presentar diagnóstico entendible.

Riesgo actual: falta persistencia operativa de settings.

## Deuda prioritaria detectada

| Deuda | Impacto | Prioridad |
|---|---|---|
| `DocuPodcastShellViewModel` concentra demasiada lógica. | Dificulta rediseño, pruebas y cambios seguros. | Alta |
| Falta mapa formal de coordinadores. | Los flujos cruzados quedan acoplados. | Alta |
| Settings no están plenamente operativos/persistidos. | El usuario dependería de variables externas. | Alta |
| Targets de capas pueden ser placeholders. | Se puede guardar una promesa no real. | Alta |
| Video no debe prometer MP4 final mientras sea paquete. | Riesgo de contrato falso. | Alta |
| Metadatos/checksums requieren validación de round-trip. | Riesgo de proyecto portable incompleto. | Media-alta |

## Coordinadores propuestos

| Coordinador | Responsabilidad |
|---|---|
| `DocumentNarrationCoordinator` | Documento narrable → proyección interna → audio inicial → escucha. |
| `DocumentSelectionCoordinator` | Bloque/oración/rango y vínculo con proyección interna cuando exista. |
| `NarrativeLayerCoordinator` | Asignaciones, conflictos, reemplazos y targets. |
| `PlaybackWorkflowCoordinator` | Manifest, cursor, buffer y controles de reproducción. |
| `AudioWorkflowCoordinator` | Jobs, generación, cancelación, reanudación y diagnósticos. |
| `StoryboardWorkflowCoordinator` | Imágenes, bindings, rail y sincronización visual. |
| `VideoExportCoordinator` | Video plan, paquete, FFmpeg, render y progreso. |
| `VoiceWorkflowCoordinator` | Voces, muestras, grabación, consentimiento y asignación. |
| `WorkspaceNavigationCoordinator` | Navegación sin ensuciar el contenido. |
| `SettingsWorkflowCoordinator` | Configuración persistente y pruebas de motor/modelo. |

## Criterio de auditoría

Una funcionalidad pertenece al cerebro si cumple alguna condición:

- modifica proyecto, Documento narrable, proyecciones internas, capas, assets, jobs o configuración;
- decide si una acción está permitida;
- coordina procesos largos;
- persiste o rehidrata estado;
- valida invariantes;
- transforma documentos, audio, video o transcripciones.

Una funcionalidad pertenece a la cara si solo presenta estado o captura intención de usuario.


## Ajuste T60 — Documento narrable raíz

T60 corrige la lectura conceptual del cerebro: el flujo no debe entenderse como “Word y luego Guion” para el usuario normal. Debe entenderse como:

```text
Abrir documento → Documento narrable → leer/escuchar → capas opcionales → storyboard/video opcional → exportar
```

Las clases `NarrationScript*` pueden conservarse como infraestructura y compatibilidad, pero quedan clasificadas como proyección interna/avanzada de narración.

## Ajuste T60B — Refresco de documento fuente

### Refresco de documento fuente

Responsabilidad futura inmediata: permitir que el usuario pulse **Refrescar contenido** cuando el archivo Word/PDF/Markdown/TXT haya sido modificado fuera de DocuPodcast.

El cerebro debe modelar esta capacidad sin romper el contrato de solo lectura:

- `SourceDocumentSnapshot`: huella del archivo importado, fecha, tamaño, hash y estructura narrable derivada.
- `SourceDocumentChangeDetector`: compara snapshot anterior contra archivo actual.
- `SourceDocumentChangeReport`: resume bloques nuevos, modificados, eliminados, movidos o dudosos.
- `RefreshSourceDocumentUseCase`: reimporta solo por decisión del usuario y produce impacto sobre Documento narrable.
- `DerivedArtifactStalenessPolicy`: marca audio, capas, storyboard y transcripciones como vigentes, obsoletos o en revisión.
- `SourceDocumentRefreshCoordinator`: conecta botón/acción de UI con casos de uso y persistencia.

Regla: refrescar contenido lee y compara; no edita ni sobrescribe el documento fuente.
