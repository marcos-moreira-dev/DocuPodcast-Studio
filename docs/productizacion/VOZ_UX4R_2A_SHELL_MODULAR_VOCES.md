# VOZ-UX4R-2A — shell modular simple de Vista Voces

## Objetivo

Reemplazar la Vista Voces tipo `SplitPane` por una microaplicación administrativa sobria con tres módulos principales:

```text
Inicio
Configurar motor
Gestionar voces
```

Esta tanda no implementa todavía la selección real de CPU/GPU ni el flujo completo de crear/eliminar voces. Su objetivo es deshuesar la pantalla vieja y dejar un shell estable para las tandas siguientes.

## Contrato visual

La vista debe usar filas sobrias, listas limpias y acciones justas. No debe convertirse en un dashboard futurista ni en una app web decorativa.

Reglas:

- no usar tarjetas llamativas para datos simples;
- no usar avatares IA, porcentajes inventados ni métricas cosméticas;
- preferir filas y secciones administrativas;
- mantener nombres humanos: Voz IA avanzada, Voz local simple, Modo de prueba;
- no exponer nombres técnicos de motores en la UI normal.

## Módulo Inicio

Inicio lista las voces creadas y su estado. Debe responder:

- qué voces existen;
- cuáles están listas;
- cuáles están incompletas;
- qué emociones/tonos tiene cada voz;
- cuáles podrán aparecer en Documento.

Inicio no edita muestras. Desde aquí solo se navega a edición o creación mediante Gestionar voces.

## Módulo Configurar motor

Configurar motor concentra:

- motor activo y alcance;
- estado del motor de voz;
- estado/validación;
- prueba rápida con textbox.

La selección real de motor y de dispositivo CPU/GPU detectado queda para VOZ-UX4R-2B. La superficie de esta tanda deja preparado el lugar sin crear placeholders falsos.

## Módulo Gestionar voces

Gestionar voces concentra:

- selector de voz;
- detalle de voz seleccionada;
- registro de muestras por tono/emoción;
- importar audio;
- grabar emoción;
- detener y registrar;
- reproducir muestra;
- exportar muestra;
- eliminar muestra;
- prueba generada.

En Voz local simple, el módulo muestra la experiencia mínima y no muestra emociones, muestras humanas ni clonación.

## Reglas preservadas

- Neutral es necesaria para usar una voz avanzada en Documento.
- Muchas emociones deben estar soportadas; no limitar la arquitectura a feliz/triste/enojado.
- Una voz es una entidad única, por ejemplo `Pepito`; las emociones son muestras asociadas, no voces separadas.
- Importar o grabar una emoción reemplaza la muestra de esa emoción, no crea duplicados.
- Eliminar una voz completa debe hacerse en una tanda posterior con confirmación explícita y aviso de archivos de audio asociados.
- Documento solo debe mostrar voces con Neutral y emociones registradas para la voz seleccionada.

## Archivos principales

```text
src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceModuleId.java
src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceModuleDescriptor.java
src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceModuleNavigation.java
src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceWorkspaceShell.java
src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceLibraryWorkspaceView.java
src/main/resources/css/voice-library.css
```

## Próximas tandas

1. VOZ-UX4R-2B — Configurar motor real en Voces: selector de motor, selector CPU/GPU detectado para todos los motores, estado real y prueba con textbox.
2. VOZ-UX4R-3 — Gestionar voces real: crear, editar, eliminar, importar/grabar/reemplazar emociones, reproducir y exportar muestras.
3. VOZ-TTS5 — Documento consume voces/emociones reales con combos filtrados.
