package com.marcosmoreiradev.docupodcaststudio.application.voice;

import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceLibrary;

import java.io.IOException;
import java.nio.file.Path;

/** Persists the voice library snapshot inside the project workspace. */
public interface VoiceLibraryWorkspaceRepository {
    MaterializedVoiceLibrary materialize(VoiceLibrary voiceLibrary, Path projectFile) throws IOException;
}
