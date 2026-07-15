package com.marcosmoreiradev.docupodcaststudio.application.services;

import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioDurationProbe;
import com.marcosmoreiradev.docupodcaststudio.application.media.ImportUserMediaAssetUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.media.UserMediaFormatPolicy;

/** User media use cases: MP3/WAV import and video-to-audio extraction. */
public record MediaApplicationServices(
        ImportUserMediaAssetUseCase importUserMediaAsset,
        UserMediaFormatPolicy userMediaFormatPolicy,
        AudioDurationProbe audioDurationProbe
) {
    public MediaApplicationServices(ImportUserMediaAssetUseCase importUserMediaAsset,
                                    UserMediaFormatPolicy userMediaFormatPolicy) {
        this(importUserMediaAsset, userMediaFormatPolicy,
                path -> { throw new java.io.IOException("No hay sonda de duracion de audio configurada."); });
    }
}
