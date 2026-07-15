# Tanda 88 — FFmpeg real para media

Base: T87.

T88 conecta `Audio del computador` con media real: elegir audio, extraer audio de video, normalizar formatos con FFmpeg y asignar el `AUDIO_CLIP` resultante al fragmento seleccionado.

## Archivos clave

- `application/media/AudioNormalizationGateway.java`
- `application/media/AudioNormalizationProfile.java`
- `application/media/AudioNormalizationResult.java`
- `infrastructure/media/FfmpegAudioNormalizationGateway.java`
- `infrastructure/media/FfmpegVideoAudioExtractionGateway.java`
- `application/media/ImportUserMediaAssetUseCase.java`
- `presentation/document/DocumentAudioNarrationPanel.java`
- `presentation/shell/DocuPodcastShellViewModel.java`
- `infrastructure/stt/JavaSoundAudioNormalizer.java`

## Validación focal

- `ImportUserMediaAssetUseCaseTest`
- `FfmpegAudioNormalizationGatewayTest`
- `FfmpegVideoAudioExtractionGatewayTest`
- `FfmpegMediaT88SourceTest`

No se ejecutó Maven en el entorno ChatGPT por ausencia de `mvn`; se validó con revisión estática y compilación focal con `javac` cuando fue posible.
