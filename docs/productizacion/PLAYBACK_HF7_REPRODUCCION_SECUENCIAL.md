# PLAYBACK-HF7 — reproducción secuencial por fragmentos

Esta tanda corrige el síntoma donde los fragmentos generados podían imponerse unos sobre otros durante la escucha.

## Decisiones

- El reproductor interno ya no usa los bytes escritos al buffer como reloj de reproducción.
- La posición se toma del dispositivo de audio mediante `SourceDataLine.getMicrosecondPosition()`.
- Cada reproducción tiene una generación interna (`playbackGeneration`) para impedir que un hilo viejo cierre o modifique una reproducción nueva.
- Los eventos de generación no deben iniciar otro fragmento si el reproductor ya está sonando.

## Resultado esperado

- Mientras un fragmento suena, los siguientes quedan disponibles en cola/manifest.
- Al terminar un fragmento, el cursor avanza al siguiente.
- Al pulsar siguiente/anterior, se detiene explícitamente el fragmento actual y se inicia el destino.
