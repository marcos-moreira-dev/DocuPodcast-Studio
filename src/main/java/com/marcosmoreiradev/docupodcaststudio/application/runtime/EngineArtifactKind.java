package com.marcosmoreiradev.docupodcaststudio.application.runtime;

/** Type of binary/model/script artifact tracked before a release build. */
public enum EngineArtifactKind {
    EXECUTABLE,
    MODEL_FILE,
    MODEL_CONFIG,
    VOICE_SAMPLE,
    SCRIPT,
    RUNTIME,
    DIRECTORY,
    LICENSE_NOTICE
}
