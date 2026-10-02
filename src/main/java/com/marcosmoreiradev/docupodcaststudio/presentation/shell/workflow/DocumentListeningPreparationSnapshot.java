package com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow;

import com.marcosmoreiradev.docupodcaststudio.application.document.ResolvedDocumentProcessingSelection;
import com.marcosmoreiradev.docupodcaststudio.application.reading.AdaptNarrationLanguageUseCase;

import java.util.Objects;

/** Separates canonical semantic readiness from derived listening-language readiness. */
public record DocumentListeningPreparationSnapshot(
        String correlationId,
        ResolvedDocumentProcessingSelection selection,
        boolean semanticallyPrepared,
        String listeningLanguage,
        AdaptNarrationLanguageUseCase.TranslationCacheInspection translation) {

    public DocumentListeningPreparationSnapshot {
        correlationId = Objects.requireNonNullElse(correlationId, "").strip();
        selection = Objects.requireNonNull(selection, "selection");
        listeningLanguage = Objects.requireNonNullElse(listeningLanguage, "").strip();
        translation = Objects.requireNonNull(translation, "translation");
    }

    public boolean listeningPrepared() {
        return semanticallyPrepared && translation.complete();
    }
}
