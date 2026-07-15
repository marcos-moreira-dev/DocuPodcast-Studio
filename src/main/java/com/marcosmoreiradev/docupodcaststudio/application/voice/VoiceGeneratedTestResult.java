package com.marcosmoreiradev.docupodcaststudio.application.voice;

import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceReferenceTone;

import java.nio.file.Path;
import java.util.Optional;

/** Result of generating or blocking a voice test. */
public record VoiceGeneratedTestResult(
        boolean generated,
        boolean requiresConfiguration,
        boolean fallbackToNeutral,
        VoiceReferenceTone requestedTone,
        VoiceReferenceTone resolvedTone,
        Optional<Path> audioFile,
        Optional<Path> manifestFile,
        String phrase,
        String referenceSampleAssetId,
        String userMessage
) {
    public VoiceGeneratedTestResult {
        requestedTone = requestedTone == null ? VoiceReferenceTone.NEUTRAL : requestedTone;
        resolvedTone = resolvedTone == null ? VoiceReferenceTone.NEUTRAL : resolvedTone;
        audioFile = audioFile == null ? Optional.empty() : audioFile;
        manifestFile = manifestFile == null ? Optional.empty() : manifestFile;
        phrase = phrase == null ? "" : phrase.strip();
        referenceSampleAssetId = referenceSampleAssetId == null ? "" : referenceSampleAssetId.strip();
        userMessage = userMessage == null ? "" : userMessage.strip();
    }

    public static VoiceGeneratedTestResult blocked(VoiceReferenceTone requestedTone, String message, boolean requiresConfiguration) {
        return new VoiceGeneratedTestResult(false, requiresConfiguration, false, requestedTone, VoiceReferenceTone.NEUTRAL,
                Optional.empty(), Optional.empty(), "", "", message);
    }
}
