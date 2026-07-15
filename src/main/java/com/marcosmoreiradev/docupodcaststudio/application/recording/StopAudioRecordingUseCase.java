package com.marcosmoreiradev.docupodcaststudio.application.recording;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

/** Stops the current human-audio capture and returns the generated WAV file. */
public final class StopAudioRecordingUseCase {
    private final AudioRecordingGateway gateway;

    public StopAudioRecordingUseCase(AudioRecordingGateway gateway) {
        this.gateway = Objects.requireNonNull(gateway, "gateway");
    }

    public Path stop() throws IOException {
        return gateway.stopRecording();
    }

    public boolean recording() {
        return gateway.recording();
    }
}
