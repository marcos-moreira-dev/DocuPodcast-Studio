package com.marcosmoreiradev.docupodcaststudio.application.recording;

import java.io.IOException;
import java.util.Objects;

/** Cancels an active microphone recording without importing/replacing any voice sample. */
public final class CancelAudioRecordingUseCase {
    private final AudioRecordingGateway gateway;

    public CancelAudioRecordingUseCase(AudioRecordingGateway gateway) {
        this.gateway = Objects.requireNonNull(gateway, "gateway");
    }

    public void cancel() throws IOException {
        if (!gateway.recording()) {
            return;
        }
        gateway.cancelRecording();
    }
}
