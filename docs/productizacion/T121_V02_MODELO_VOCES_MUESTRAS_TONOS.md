# T121-V02 — Modelo de voces, muestras, tonos teatrales y frases guía

## Objetivo

Implementar la base de dominio para que la vista Voces pueda dejar de ser una pantalla genérica y convertirse en un módulo de gestión de voces y muestras. Esta tanda no rediseña todavía toda la UI; prepara el modelo que usarán las tandas siguientes: almacenamiento, wizard, prueba generada, fallback y rediseño visual.

## Decisiones aplicadas

- La UI no debe mostrar la palabra `Coqui`; el nombre visible del motor avanzado es **Voz IA avanzada**.
- Piper queda como **Voz local simple**.
- Mock queda como **Modo de prueba**.
- Las emociones/tonos son muestras de referencia grabadas o importadas, no instrucciones escritas dentro del texto.
- Cada tono del catálogo tiene una frase guía por defecto para que la persona sepa cómo actuar la muestra.
- Neutral es la muestra principal para una voz avanzada.
- Si falta un tono, una tanda posterior usará fallback a neutral con aviso.
- El catálogo extendido incluye usos teatrales, incluyendo aburrida, heroica y seductora/insinuante no explícita.

## Clases agregadas

### `VoiceEngineTarget`

Enum visible de UX:

- `ADVANCED_AI_VOICE` → Voz IA avanzada
- `LOCAL_SIMPLE_VOICE` → Voz local simple
- `TEST_MODE` → Modo de prueba

Esto permite que la UI hable en lenguaje de producto sin mencionar motores técnicos.

### `VoiceReferenceToneCategory`

Agrupa tonos para el wizard:

- `BASIC` → Tonos recomendados
- `THEATRICAL_EXTENDED` → Catálogo teatral extendido

### `VoiceReferenceTone`

Catálogo de tonos con:

- categoría;
- etiqueta visible;
- frase guía para grabación;
- helpers `basicTones()` y `theatricalExtendedTones()`.

### `VoiceSampleOrigin`

Origen de la muestra:

- grabada en la app;
- importada desde archivo;
- prediseñada;
- cache/generada.

### `VoiceFileOwnership`

Política de propiedad de archivo:

- recurso de app protegido;
- archivo gestionado en AppData;
- asset del proyecto;
- referencia externa.

Define si el archivo es eliminable por DocuPodcast.

### `VoiceReferenceSample`

Representa una muestra concreta para una voz y un tono:

- id;
- voiceProfileId;
- tone;
- fileUri;
- origin;
- ownership;
- durationMillis;
- createdAt;
- notes.

### `VoiceReferenceSampleSet`

Agrupa muestras de una misma voz. Incluye:

- validación de voz única;
- validación de tono único;
- `forAdvancedVoice(...)`, que exige neutral;
- `sampleFor(...)`;
- `sampleForOrNeutral(...)`;
- `missingToneUsesNeutral(...)`.

## Catálogo implementado

### Básico

- Neutral
- Feliz
- Triste
- Enojada
- Seria
- Calmada
- Preocupada
- Entusiasmada
- Aburrida

### Teatral extendido

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

La etiqueta `Insinuante no explícita` se mantiene no explícita y apta para uso educativo/teatral.

## Frases guía

Cada tono tiene una frase guía de grabación. La app debe mostrar esa frase cuando el usuario presione `Grabar` para ese tono. El usuario lee la frase, puede cancelar, detener o guardar. Cancelar no reemplaza la muestra anterior; guardar asocia el audio al tono.

## Tests agregados

- `VoiceReferenceToneCatalogTest`
- `VoiceReferenceSampleSetTest`
- `VoiceToneCatalogT121V02SourceTest`

## Validación esperada

- El catálogo incluye tonos básicos y teatrales.
- Cada tono tiene etiqueta y frase guía.
- La UX visible no usa nombres técnicos de motor.
- La voz avanzada requiere muestra neutral.
- Una muestra faltante puede resolverse con neutral.
- La propiedad del archivo define si DocuPodcast puede eliminarlo.
