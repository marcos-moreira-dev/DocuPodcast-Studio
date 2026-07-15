# Memoria — Tanda 88 FFmpeg/media real

Se cerró la brecha detectada en LM-9/LM-18: los botones `Elegir audio…` y `Extraer audio de video…` dejaron de ser preparación de capa sin target y ahora importan/preparan media real.

Piezas nuevas: `AudioNormalizationGateway`, `AudioNormalizationProfile`, `AudioNormalizationResult` y `FfmpegAudioNormalizationGateway`.

Piezas modificadas: `ImportUserMediaAssetUseCase`, `UserMediaFormatPolicy`, `FfmpegVideoAudioExtractionGateway`, `JavaSoundAudioNormalizer`, `DocuPodcastShellViewModel`, `DocumentAudioNarrationPanel` y `DocumentLayerRailView`.

La tanda conserva el foco de producto: Documento primero, capas dentro del proyecto, documento fuente intacto.
