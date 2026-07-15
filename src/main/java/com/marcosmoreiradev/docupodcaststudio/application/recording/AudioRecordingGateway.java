package com.marcosmoreiradev.docupodcaststudio.application.recording;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Port for capturing/importing human audio inside a project workspace.
 *
 * <p>Implementations may record from a microphone or wrap another local capture mechanism. They must
 * write to a project-local path and return the recorded WAV so the voice workflow can import it as a
 * portable project asset.</p>
 */
public interface AudioRecordingGateway {
    Path startRecording(Path projectDirectory, String suggestedFileName) throws IOException;

    default Path startRecording(Path projectDirectory, String suggestedFileName, String inputDeviceId) throws IOException {
        return startRecording(projectDirectory, suggestedFileName);
    }

    Path stopRecording() throws IOException;

    /** Stops the active capture and discards the temporary WAV when possible. */
    default void cancelRecording() throws IOException {
        Path recorded = stopRecording();
        java.nio.file.Files.deleteIfExists(recorded);
    }

    boolean recording();

    default List<AudioInputDevice> inputDevices() {
        return List.of(AudioInputDevice.systemDefault());
    }
}
