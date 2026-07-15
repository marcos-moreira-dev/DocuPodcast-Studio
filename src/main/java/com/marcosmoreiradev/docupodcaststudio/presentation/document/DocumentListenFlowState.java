package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import com.marcosmoreiradev.docupodcaststudio.application.document.DocumentListenPlan;
import com.marcosmoreiradev.docupodcaststudio.application.document.ListeningSessionState;

/**
 * Presentation compatibility wrapper for the document listening flow labels.
 *
 * <p>The product decision now lives in application as {@link ListeningSessionState}. This wrapper
 * remains for existing views/tests that still reference the old presentation type.</p>
 */
public record DocumentListenFlowState(String title, String detail, String styleClass) {
    public static DocumentListenFlowState noDocument() {
        return from(ListeningSessionState.noDocument());
    }

    public static DocumentListenFlowState notNarratable() {
        return from(ListeningSessionState.notNarratable());
    }

    public static DocumentListenFlowState scriptPending() {
        return from(ListeningSessionState.projectionPending());
    }

    public static DocumentListenFlowState saveRequired() {
        return from(ListeningSessionState.saveRequired());
    }

    public static DocumentListenFlowState audioPending() {
        return from(ListeningSessionState.audioPending());
    }

    public static DocumentListenFlowState buffering() {
        return from(ListeningSessionState.buffering());
    }

    public static DocumentListenFlowState playable() {
        return from(ListeningSessionState.playable());
    }

    public static DocumentListenFlowState of(
            boolean hasDocument,
            boolean hasNarratableText,
            boolean hasScript,
            boolean hasPlaybackManifest,
            boolean projectSaved,
            boolean audioRunning) {
        DocumentListenPlan plan;
        if (!hasDocument) {
            plan = DocumentListenPlan.noDocument();
        } else if (!hasNarratableText) {
            plan = DocumentListenPlan.noNarratableText();
        } else if (!hasScript) {
            plan = DocumentListenPlan.buildNarrationProjection();
        } else if (hasPlaybackManifest) {
            plan = DocumentListenPlan.playExistingAudio();
        } else if (audioRunning) {
            plan = DocumentListenPlan.waitForAudioBuffer("");
        } else if (!projectSaved) {
            plan = DocumentListenPlan.saveProjectRequired();
        } else {
            plan = DocumentListenPlan.generateAudio();
        }
        return from(ListeningSessionState.from(plan));
    }

    private static DocumentListenFlowState from(ListeningSessionState state) {
        return new DocumentListenFlowState(state.title(), state.detail(), state.styleClass());
    }
}
