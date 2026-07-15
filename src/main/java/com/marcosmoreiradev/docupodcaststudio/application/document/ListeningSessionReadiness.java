package com.marcosmoreiradev.docupodcaststudio.application.document;

import java.util.Objects;

/** Decision returned by the end-to-end document listening orchestration use case. */
public record ListeningSessionReadiness(DocumentListenPlan plan, ListeningSessionState state) {
    public ListeningSessionReadiness {
        Objects.requireNonNull(plan, "plan");
        Objects.requireNonNull(state, "state");
    }
}
