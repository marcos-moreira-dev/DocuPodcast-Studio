package com.marcosmoreiradev.docupodcaststudio.application.audio;

import com.marcosmoreiradev.docupodcaststudio.domain.audio.AudioProcessDiagnosticEvent;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/** Port for persisting and reading per-process TTS diagnostics. */
public interface AudioProcessDiagnosticsRepository {
    void append(Path projectDirectory, String jobId, AudioProcessDiagnosticEvent event) throws IOException;

    List<AudioProcessDiagnosticEvent> list(Path projectDirectory, String jobId) throws IOException;
}
