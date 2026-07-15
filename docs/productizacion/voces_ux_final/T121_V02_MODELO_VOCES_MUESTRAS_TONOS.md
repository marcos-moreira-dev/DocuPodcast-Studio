# T121-V02 — Modelo de dominio para voces, muestras y tonos teatrales

## Objetivo

Crear el modelo de dominio que permita manejar voces de referencia, muestras por tono y catálogo teatral extendido.

## Entidades propuestas

### `VoiceProfile`

Representa una voz registrada.

Campos:

- `id`
- `displayName`
- `engineTarget`: ADVANCED_AI_VOICE, LOCAL_SIMPLE_VOICE, MOCK
- `protectedVoice`: true/false
- `createdAt`
- `updatedAt`
- `samples`
- `metadata`

### `VoiceReferenceSample`

Representa una muestra de audio de una voz en un tono específico.

Campos:

- `id`
- `voiceProfileId`
- `tone`
- `fileUri`
- `origin`
- `ownership`
- `durationMillis`
- `createdAt`
- `notes`

### `VoiceReferenceTone`

Enum/catálogo de tonos.

Debe incluir catálogo básico y teatral extendido.

### `VoiceSampleOrigin`

- RECORDED_IN_APP
- IMPORTED_FILE
- APP_DEFAULT
- GENERATED_CACHE

### `VoiceFileOwnership`

- APP_RESOURCE
- USER_APPDATA
- PROJECT_ASSET
- EXTERNAL_REFERENCE

Regla: solo se eliminan archivos gestionados por DocuPodcast.

## Catálogo de tonos

### Básico

- NEUTRAL
- HAPPY
- SAD
- ANGRY
- SERIOUS
- CALM
- WORRIED
- ENTHUSIASTIC
- BORED

### Teatral extendido

- JOYFUL
- EUPHORIC
- MELANCHOLIC
- NERVOUS
- AFRAID
- SURPRISED
- DOUBTFUL
- TIRED
- PLEADING
- AUTHORITATIVE
- IRONIC
- SARCASTIC
- MYSTERIOUS
- SOLEMN
- HEROIC
- DRAMATIC
- TENSE
- RUSHED
- CONFUSED
- TENDER
- COLD
- MOCKING
- DISTRUSTFUL
- REPENTANT
- VULNERABLE
- HOPEFUL
- RESIGNED
- THREATENING
- DEFIANT
- SEDUCTIVE_NON_EXPLICIT

## Etiquetas visibles en español

- Neutral
- Feliz
- Triste
- Enojada
- Seria
- Calmada
- Preocupada
- Entusiasmada
- Aburrida
- Alegre
- Eufórica
- Melancólica
- Nerviosa
- Asustada
- Sorprendida
- Dudosa
- Cansada
- Suplicante
- Autoritaria
- Irónica
- Sarcástica
- Misteriosa
- Solemne
- Heroica
- Dramática
- Tensa
- Apurada
- Confundida
- Tierna
- Fría
- Burlona
- Desconfiada
- Arrepentida
- Vulnerable
- Esperanzada
- Resignada
- Amenazante
- Desafiante
- Insinuante no explícita

## Reglas

- Neutral es la muestra principal.
- Una voz avanzada debe tener neutral para considerarse usable.
- Los tonos extra son opcionales.
- Si falta tono, fallback a neutral.
- La voz prediseñada es protegida.
- Las voces importadas son eliminables.

## Tests recomendados

- `VoiceReferenceToneCatalogTest`
- `VoiceProfileRequiresNeutralForAdvancedVoiceTest`
- `VoiceProfileFallbackTonePolicyTest`
- `ProtectedNeutralVoicePolicyTest`

## Criterios de aceptación

- El catálogo teatral existe en dominio/aplicación.
- Neutral es obligatorio para voz avanzada.
- Los tonos faltantes tienen fallback claro.
- Las etiquetas visibles no usan jerga técnica.


## Campo requerido por tono: `suggestedRecordingPrompt`

Cada tono debe exponer una frase guía por defecto:

```text
VoiceReferenceTone.suggestedRecordingPrompt()
```

Esto permite que el wizard muestre una frase adecuada para grabar cada tono. No se debe depender de frases generadas dinámicamente ni de prompts improvisados en runtime para V1.

Reglas:

- La frase neutral es obligatoria.
- Cada tono del catálogo básico y extendido debe tener frase guía.
- Las frases deben ser aptas para uso educativo.
- La frase de tonos como `Insinuante no explícita` debe ser teatral y no explícita.
- La UI puede mostrar la frase completa en la tarjeta de grabación.
