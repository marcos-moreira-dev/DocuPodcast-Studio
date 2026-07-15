# TI2 — Audio jobs desde RenderPlan

## Estado

Implementada sobre TI1 verde. TI2 mueve la cola de generación TTS hacia el contrato `RenderUnitPlan`, sin romper la ruta legacy por segmentos.

## Objetivo

La generación de audio ya no debe depender exclusivamente de `NarrationScriptDocument` por segmento. Desde esta tanda, un job de audio puede recibir un `RenderUnitPlan` y generar solamente las unidades que realmente requieren voz sintetizada.

Esto prepara el camino para:

- voz por oración/unidad;
- emoción/estilo por rango;
- audio humano/local como sustituto de TTS;
- omisión de bloques visuales silenciosos;
- playback y exportación por unidad.

## Contrato implementado

### 1. `AudioGenerationRequest` acepta `RenderUnitPlan`

Se mantiene compatibilidad:

```java
new AudioGenerationRequest(script, projectDirectory, jobName)
```

Y se agrega la ruta nueva:

```java
new AudioGenerationRequest(script, renderUnitPlan, projectDirectory, jobName)
```

La petición expone:

```java
usesRenderUnitPlan()
generationUnits()
generationUnitCount()
```

### 2. Nueva unidad efectiva de audio

Se agrega:

```java
application.audio.AudioGenerationUnit
```

Representa lo que el motor TTS debe generar. Puede venir de:

- un `NarrationSegment` legacy;
- un `RenderUnit` de TI1 que requiera TTS.

### 3. Qué entra y qué no entra al job TTS

Entra:

```text
SPOKEN_ONLY sin audioAssetId
SPOKEN_WITH_VISUAL sin audioAssetId
```

No entra:

```text
SPOKEN_ONLY con audioAssetId externo
SPOKEN_WITH_VISUAL con audioAssetId externo
VISUAL_SILENT
OMITTED
```

### 4. Gateways actualizados

Actualizados:

```text
MockAudioGenerationGateway
LocalTtsProcessAudioGenerationGateway
```

Ambos consumen:

```java
request.generationUnits()
```

en vez de iterar directamente sobre `request.script().segments()`.

### 5. UI/coordinador conectado

`AudioWorkflowCoordinator` tiene overload:

```java
request(script, renderUnitPlan, projectDirectory, jobName)
```

`DocuPodcastShellViewModel` construye:

```text
NarrationRenderPlan
→ RenderUnitPlan
→ AudioGenerationRequest
```

antes de enviar o reanudar un job.

Si por compatibilidad un proyecto viejo no puede construir unidades, se conserva fallback a la ruta legacy por segmento.

## Decisión importante

TI2 no cambia todavía el formato de persistencia de jobs. `AudioSegmentSnapshot.segmentId` puede guardar un id de unidad, por ejemplo:

```text
SEG-001-U001
```

La migración de nombre a `AudioUnitSnapshot` queda para una tanda posterior si resulta necesaria.

## Limitaciones conocidas

- El manifest de audio conserva estructura histórica.
- La exportación/playback todavía debe seguir migrando a unidad en TI7.
- Video/storyboard desde RenderPlan queda para TI3.
- Bloques visuales no narrables completos quedan para TI4.

## Tests agregados

```text
AudioGenerationRequestRenderPlanTest
MockAudioGenerationGatewayRenderPlanTest
AudioJobsFromRenderPlanTi2SourceTest
```

## Siguiente tanda

TI3 — Storyboard/video desde RenderPlan.
