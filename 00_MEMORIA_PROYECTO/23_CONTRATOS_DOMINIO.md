# Contratos de dominio iniciales

## DocuPodcastProject

Agregado raíz del proyecto editable.

Debe contener metadata, referencias a documento, guion, voces, storyboard, audio, assets y vista.

## ReadableDocument

Representación importada del Word/PDF/Markdown/TXT.

No es el archivo original.

## NarrationScriptDocument

Guion narrable editable. Artefacto central.

## NarrationSegment

Unidad narrable con texto y metadatos.

## PerformanceSpan

Rango de texto con voz/estilo/personaje específico.

## VoiceProfile

Perfil de voz prediseñada, propia, autorizada o importada.

## CharacterProfile

Personaje narrativo que puede usar una voz.

## StoryboardBinding

Relación entre segmento/rango e imagen.

## AudioJob

Proceso persistente de generación.

## AudioManifest

Relación entre segmentos y clips de audio.

## PlaybackManifest

Relación entre tiempo, segmento, audio e imagen.
