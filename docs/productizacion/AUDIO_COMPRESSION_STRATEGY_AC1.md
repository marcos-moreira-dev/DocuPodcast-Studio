# AUDIO-COMPRESS1 — estrategia de compresión de audio

## Decisión

Los WAV generados por los motores locales siguen siendo la caché primaria de trabajo porque son simples, editables, reanudables y fáciles de concatenar sin pérdida. Sin embargo, el producto no debe obligar al usuario a exportar siempre audio pesado.

## Regla propuesta

1. **Proyecto interno:** conservar WAV por chunk como fuente de trabajo y recuperación.
2. **Exportación:** permitir formatos comprimidos, principalmente MP3 y AAC/M4A, usando FFmpeg interno cuando esté disponible.
3. **Chunks comprimidos:** no convertirlos por defecto todavía. Puede agregarse después como política opcional de limpieza/archivo, porque comprimir cada chunk durante generación puede consumir CPU y complicar reanudación/playback.
4. **Video:** para video final, usar AAC dentro del MP4 final.
5. **UI:** mostrar tamaño generado en disco y tamaño exportado final para que el usuario entienda la diferencia.

## Alcance futuro

- `AudioEncodingProfile`: WAV, MP3 estándar, MP3 compacto, AAC video.
- `AudioEncodingGateway`: puerto general, implementado con FFmpeg.
- `ExportPodcastAudioUseCase`: exportar WAV/MP3/AAC sin cambiar la caché interna.
- Limpieza opcional de chunks antiguos cuando exista audio final exportado.

## Riesgo

Comprimir chunks inmediatamente puede ahorrar disco, pero puede hacer más lenta la generación con motores locales y añadir fallos de FFmpeg en medio del job. Por eso debe ser una tanda separada después de estabilizar playback y FFmpeg interno.
