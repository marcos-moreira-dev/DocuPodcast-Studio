package com.marcosmoreiradev.docupodcaststudio.application.media;

import java.io.IOException;
import java.nio.file.Path;

/** Extracts audio from video using an operational adapter such as FFmpeg. */
public interface VideoAudioExtractionGateway {
    boolean ready();

    String readinessMessage();

    VideoAudioExtractionResult extractAudio(Path sourceVideoFile, Path targetAudioFile) throws IOException;
}
