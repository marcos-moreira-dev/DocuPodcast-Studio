# Tanda 7D opcional — Recording, voz humana y playback clicable

## Motivo

El usuario pidió preparar la capacidad de grabar audio humano, asociarlo a texto, usarlo eventualmente para Whisper/STT e iniciar/reanudar reproducción desde cualquier línea clicada.

## Alcance recomendado

- Panel placeholder de grabación en Guion/Audio.
- Botón deshabilitado o experimental: **Grabar audio para selección**.
- Botón deshabilitado o experimental: **Transcribir audio a texto**.
- Modelo UI para elegir `AI_TTS` vs `HUMAN_RECORDING` en una selección.
- `PlaybackCursor` conectado a selección de segmento, todavía sin reproductor completo.
- Documentar claramente que Whisper/STT y grabación real no están activos.

## Por qué puede esperar

La base de dominio ya existe. Se puede avanzar a TTS real si la prioridad es escuchar audio generado. Esta tanda solo reduciría fricción de UI futura.
