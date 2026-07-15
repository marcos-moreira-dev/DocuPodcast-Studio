package com.marcosmoreiradev.docupodcaststudio.application.recording;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class AudioRecordingGatewayDeviceSelectionTest {
    @Test
    void defaultGatewayContractExposesSystemMicrophoneAndDelegatesSelectedStart() throws Exception {
        DelegatingGateway gateway = new DelegatingGateway();

        List<AudioInputDevice> devices = gateway.inputDevices();
        Path output = gateway.startRecording(Path.of("project"), "sample.wav", "mixer:2");

        assertEquals(AudioInputDevice.DEFAULT_ID, devices.getFirst().id());
        assertEquals(Path.of("project", "sample.wav"), output);
        assertEquals("sample.wav", gateway.lastSuggestedName);
    }

    private static final class DelegatingGateway implements AudioRecordingGateway {
        private String lastSuggestedName = "";

        @Override
        public Path startRecording(Path projectDirectory, String suggestedFileName) {
            lastSuggestedName = suggestedFileName;
            return projectDirectory.resolve(suggestedFileName);
        }

        @Override
        public Path stopRecording() throws IOException {
            return Path.of("sample.wav");
        }

        @Override
        public boolean recording() {
            return false;
        }
    }
}
