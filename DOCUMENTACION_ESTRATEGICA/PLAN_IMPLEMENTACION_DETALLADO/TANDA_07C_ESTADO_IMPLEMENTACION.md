# Tanda 7C — Estado de implementación

## Estado

Implementada.

## Alcance cerrado

- Reanudar jobs persistidos con mock gateway.
- Saltar segmentos completados.
- Generar pendientes/fallidos/cancelados.
- Reconstruir manifest/final mock.
- Mostrar detalle persistido en workspace Audio.
- Añadir botón para continuar último reanudable.
- Añadir infraestructura preliminar para selección de texto, voz IA vs voz humana, grabación, STT/Whisper y playback clicable.

## Fuera de alcance

- TTS real.
- Grabación real desde micrófono.
- Whisper real.
- Playback real con audio timeline.
- Selector visual de rangos.

## Próxima recomendación

Antes de TTS real conviene hacer una Tanda 7D opcional de **Playback/recording contracts UI placeholders**, si se quiere que el workspace ya muestre botones inactivos/planificados para grabar, insertar transcripción y saltar a línea. Si se prefiere avanzar a voz real, pasar a Tanda 8.
