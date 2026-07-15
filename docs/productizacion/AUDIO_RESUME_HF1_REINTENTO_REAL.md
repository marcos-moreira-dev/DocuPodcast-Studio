# AUDIO-RESUME-HF1 — Seguir generando reintenta realmente

## Problema

Después de un fallo temprano del primer chunk, el botón **Seguir generando** podía fallar en menos de un segundo. La causa técnica era que la reanudación conservaba los intentos agotados del segmento fallido. Al volver a entrar al job, el bucle de intentos podía no ejecutar de nuevo el proceso externo.

## Corrección

- Los segmentos ya completados se conservan.
- Los segmentos no completados —pendientes, fallidos, cancelados o en generación— se reconstruyen como `PENDING` al reanudar.
- Cada segmento no completado vuelve a intentar desde `attempt = 1`.
- Antes de ejecutar el comando TTS se elimina un WAV parcial/stale del mismo segmento con `Files.deleteIfExists(outputFile)`.

## Resultado esperado

Si falla `SEG-001`, al pulsar **Seguir generando** la app debe ejecutar otra vez el proceso TTS de `SEG-001`; no debe marcar `Fallido` inmediatamente por intentos heredados.
