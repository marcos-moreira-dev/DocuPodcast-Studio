package com.marcosmoreiradev.docupodcaststudio.application.media;

import java.io.IOException;
import java.nio.file.Path;

/** Normalizes audio files through an operational adapter such as FFmpeg. */
public interface AudioNormalizationGateway {
    boolean ready();

    String readinessMessage();

    AudioNormalizationResult normalize(Path sourceAudioFile,
                                       Path targetAudioFile,
                                       AudioNormalizationProfile profile) throws IOException;
}
