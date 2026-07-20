package com.marcosmoreiradev.docupodcaststudio.media.api;

import java.nio.file.Path;
import java.util.Map;

/** One independently recoverable unit in a voice generation job. */
public record VoiceSynthesisUnit(
        String id,
        String text,
        String voiceId,
        String styleId,
        Path referenceAudio,
        Path outputFile,
        Map<String, String> metadata) {
    public VoiceSynthesisUnit {
        id = id == null ? "" : id.strip();
        text = text == null ? "" : text.strip();
        voiceId = voiceId == null ? "" : voiceId.strip();
        styleId = styleId == null ? "" : styleId.strip();
        if (id.isBlank()) throw new IllegalArgumentException("voice unit id is required");
        if (text.isBlank()) throw new IllegalArgumentException("voice unit text is required");
        if (outputFile == null) throw new IllegalArgumentException("voice unit output file is required");
        outputFile = outputFile.toAbsolutePath().normalize();
        referenceAudio = referenceAudio == null ? null : referenceAudio.toAbsolutePath().normalize();
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
