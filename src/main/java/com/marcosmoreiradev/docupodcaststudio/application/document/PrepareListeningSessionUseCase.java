package com.marcosmoreiradev.docupodcaststudio.application.document;

import java.util.Objects;

/**
 * Orchestrates the product-level preconditions for the single "Escuchar documento" action.
 *
 * <p>The presentation layer should ask for this decision instead of rebuilding the same chain of
 * document/projection/manifest/job/project checks. The use case deliberately returns both the
 * executable plan and the user-facing listening state so Menu/Ribbon/Workspace can stay consistent.</p>
 */
public final class PrepareListeningSessionUseCase {
    private final PrepareDocumentListeningUseCase prepareDocumentListening;

    public PrepareListeningSessionUseCase() {
        this(new PrepareDocumentListeningUseCase());
    }

    public PrepareListeningSessionUseCase(PrepareDocumentListeningUseCase prepareDocumentListening) {
        this.prepareDocumentListening = Objects.requireNonNull(prepareDocumentListening, "prepareDocumentListening");
    }

    public ListeningSessionReadiness prepare(PrepareListeningSessionRequest request) {
        Objects.requireNonNull(request, "request");
        DocumentListenPlan plan = prepareDocumentListening.prepare(
                request.document(),
                request.narrationProjection(),
                request.playbackManifest(),
                request.audioJobRunning(),
                request.projectSaved(),
                request.audioJobStatus(),
                request.bufferPolicy());
        return new ListeningSessionReadiness(plan, ListeningSessionState.from(plan));
    }
}
