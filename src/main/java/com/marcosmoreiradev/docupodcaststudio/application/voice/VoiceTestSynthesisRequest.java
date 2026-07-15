package com.marcosmoreiradev.docupodcaststudio.application.voice;

import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioEngineDescriptor;

import java.nio.file.Path;

/** Input for a short, blocking voice-test synthesis. */
public record VoiceTestSynthesisRequest(
        String segmentId,
        String phrase,
        String language,
        String voiceProfileId,
        Path referenceSampleFile,
        Path textFile,
        Path outputFile,
        Path workingDirectory,
        AudioEngineDescriptor engineDescriptor
) {
    public VoiceTestSynthesisRequest {
        segmentId = normalize(segmentId).isBlank() ? "voice-test" : normalize(segmentId);
        phrase = normalize(phrase);
        language = normalize(language).isBlank() ? "es" : normalize(language);
        voiceProfileId = normalize(voiceProfileId).isBlank() ? "VOC-NARRATOR" : normalize(voiceProfileId);
        engineDescriptor = engineDescriptor == null ? AudioEngineDescriptor.mock() : engineDescriptor;
    }

    public boolean hasReferenceSample() {
        return referenceSampleFile != null;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }
}
