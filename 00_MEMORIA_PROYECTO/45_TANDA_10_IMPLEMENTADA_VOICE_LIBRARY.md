# Memoria — Tanda 10 Voice Library

La Tanda 10 agrega la biblioteca de voces al proyecto. Ahora `DocuPodcastProject` conserva `VoiceLibrary`, el JSON principal guarda `voiceLibrary`, y el guardado materializa `voices/voice-library.json`.

El guion ya podía guardar `characterId`, `voiceProfileId` y `performanceStyleId` por segmento. Con esta tanda esos IDs dejan de ser placeholders sueltos: apuntan a una biblioteca validable.

Elementos iniciales:

- `VOC-NARRATOR`: voz prediseñada para empezar rápido.
- `VOC-OWN-PLACEHOLDER`: marcador para la voz propia pendiente de muestra.
- `CHR-NARRATOR`: personaje/rol narrador por defecto.
- `STY-NEUTRAL`: estilo básico.
- `STY-SERIOUS` y `STY-DRAMATIC`: intenciones que requieren soporte del motor.

Se agrega UI para seleccionar personaje, voz y estilo y aplicarlos al segmento seleccionado. Esto prepara teatro/guiones y también documentos académicos con narrador simple.

Queda pendiente: muestras reales, grabación de micrófono, consentimiento avanzado, estilos emocionales reales y conexión fina con worker XTTS/Piper.
