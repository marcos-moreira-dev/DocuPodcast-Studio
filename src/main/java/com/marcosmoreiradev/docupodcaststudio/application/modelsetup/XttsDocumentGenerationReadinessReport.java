package com.marcosmoreiradev.docupodcaststudio.application.modelsetup;

import java.util.List;

/**
 * Readiness used before generating document chunks with Voz IA avanzada.
 *
 * <p>The local Python, wrapper, model and speaker are the mandatory gate. A generated WAV and its
 * playback confirmation are recommended evidence, not prerequisites for the first real job.</p>
 */
public record XttsDocumentGenerationReadinessReport(
        XttsSetupReadinessReport setup,
        XttsSmokeTestReport smoke,
        boolean canGenerateDocumentAudio,
        boolean playbackConfirmed,
        List<String> blockingReasons,
        List<String> warnings,
        String userMessage,
        String recommendedAction
) {
    public XttsDocumentGenerationReadinessReport {
        blockingReasons = List.copyOf(blockingReasons == null ? List.of() : blockingReasons);
        warnings = List.copyOf(warnings == null ? List.of() : warnings);
        userMessage = userMessage == null ? "" : userMessage.strip();
        recommendedAction = recommendedAction == null ? "" : recommendedAction.strip();
    }

    public boolean needsUserPlaybackConfirmation() {
        return canGenerateDocumentAudio && !playbackConfirmed;
    }
}
