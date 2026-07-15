package com.marcosmoreiradev.docupodcaststudio.application.modelsetup;

import java.util.List;

/**
 * Gate used before generating document chunks with Voz IA avanzada.
 *
 * <p>A downloaded or structurally valid XTTS folder is not enough for document generation. The
 * application must also prove that the local Python/wrapper/model/speaker path can produce a real
 * WAV. Playback confirmation is strongly recommended, but the minimum product gate for long
 * documents is a generated WAV proof from the same self-contained runtime.</p>
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
