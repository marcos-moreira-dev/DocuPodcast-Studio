# T121-V04B — Persistencia real de muestras por tono

## Objetivo

Hacer durable el contrato de Voces avanzadas: una voz puede tener una muestra neutral y múltiples muestras por tono, guardadas dentro del proyecto y recuperables al reabrirlo.

Esta tanda no rediseña la UI todavía. Prepara la base necesaria para el wizard visual por tono, la prueba generada con frase editable y el fallback de tonos faltantes en Documento.

## Cambios implementados

- `VoiceLibrary` agrega `referenceSampleSets` como parte del agregado de voces.
- `VoiceReferenceSampleSet` puede reemplazar una muestra por tono sin duplicar tonos.
- `VoiceSampleImportRequest` acepta `VoiceReferenceTone` y conserva constructor legacy neutral.
- `ImportVoiceSampleUseCase` importa muestras con asset id por voz + tono.
- La muestra neutral actualiza `VoiceProfile.sampleAssetId` por compatibilidad legacy.
- Las muestras no neutrales se guardan en `referenceSampleSets` sin sobrescribir la muestra neutral legacy.
- `.docupodcast.json` serializa y lee `voiceLibrary.referenceSampleSets`.
- `voices/voice-library.json` materializa también los sets de muestras por tono.
- El reader tolera proyectos legacy sin `referenceSampleSets`.

## Contrato persistente

```text
VoiceLibrary
 ├─ voices
 ├─ characters legacy/compatibilidad
 ├─ styles legacy/compatibilidad
 └─ referenceSampleSets
      └─ voiceProfileId
          ├─ NEUTRAL → VoiceReferenceSample / asset VOICE_SAMPLE
          ├─ HAPPY   → VoiceReferenceSample / asset VOICE_SAMPLE
          └─ ...
```

## Compatibilidad

- Los proyectos anteriores sin `referenceSampleSets` abren con lista vacía.
- `VoiceProfile.sampleAssetId` se mantiene para compatibilidad con flujos heredados.
- El flujo nuevo debe consultar `referenceSampleSetByVoiceId(...)` para obtener muestras por tono.

## Tests agregados/actualizados

- `VoiceLibraryTest`
- `ImportVoiceSampleUseCaseTest`
- `DocuPodcastProjectVoiceLibraryJsonTest`
- `VoiceLibraryWorkspaceFileRepositoryTest`
- `VoiceToneSamplesPersistenceT121V04BSourceTest`

## Validación en entorno ChatGPT

- `javac --release 21` de `domain`, `application` e `infrastructure`.
- `javac --release 21` focal de tests nuevos/modificados con stubs JUnit.
- Smoke Java manual: importar muestra neutral + feliz, serializar `.docupodcast.json`, reabrir y verificar ambos tonos.

Maven completo no se ejecutó porque `mvn` no está instalado en este entorno.
