package com.marcosmoreiradev.docupodcaststudio.application.recording;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/** Starts a project-local human-audio capture using the configured recording gateway. */
public final class StartAudioRecordingUseCase {
    private final AudioRecordingGateway gateway;

    public StartAudioRecordingUseCase(AudioRecordingGateway gateway) {
        this.gateway = Objects.requireNonNull(gateway, "gateway");
    }

    public Path start(Path projectFile, RecordingActionPlan plan) throws IOException {
        Objects.requireNonNull(projectFile, "projectFile");
        Objects.requireNonNull(plan, "plan");
        Path projectDirectory = projectFile.toAbsolutePath().normalize().getParent();
        if (projectDirectory == null) {
            throw new IOException("Guarda el proyecto antes de grabar audio humano");
        }
        return startInDirectory(projectDirectory, plan);
    }

    public Path startInDirectory(Path recordingDirectory, RecordingActionPlan plan) throws IOException {
        return startInDirectory(recordingDirectory, plan, AudioInputDevice.DEFAULT_ID);
    }

    public Path startInDirectory(Path recordingDirectory, RecordingActionPlan plan, String inputDeviceId) throws IOException {
        Objects.requireNonNull(recordingDirectory, "recordingDirectory");
        Objects.requireNonNull(plan, "plan");
        Path targetDirectory = recordingDirectory.toAbsolutePath().normalize();
        Files.createDirectories(targetDirectory);
        return gateway.startRecording(targetDirectory, plan.suggestedFileName(), inputDeviceId);
    }

    public List<AudioInputDevice> inputDevices() {
        return gateway.inputDevices();
    }
}
