# Tanda 10 implementada — Voice Library

## Propósito estratégico

Separar voz, personaje y estilo como conceptos de dominio antes de avanzar a storyboard y playback.

## Resultado

DocuPodcast Studio ya puede modelar:

- voces prediseñadas;
- voz propia pendiente;
- voces autorizadas/importadas;
- personajes;
- estilos de interpretación;
- asignación de voz/personaje/estilo por segmento.

## Regla de producto

La app no promete actuación emocional perfecta. Guarda una intención (`PerformanceStyle`) y delega el resultado al motor TTS real configurado.

## Regla ética

Las voces propias o autorizadas deben contar con muestras y permiso. Las voces autorizadas/importadas con muestra requieren nota de consentimiento.

## Persistencia

- `.docupodcast.json`: sección `voiceLibrary`.
- `voices/voice-library.json`: snapshot exportable/depurable.
- assets: `VOICE_LIBRARY`.

## Guardarraíles

Se agregan tests para dominio, asignación, persistencia y UI fuente.
