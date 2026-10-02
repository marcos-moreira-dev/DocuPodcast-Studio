package com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow;

import com.marcosmoreiradev.docupodcaststudio.application.document.PdfPreparationPriority;

/** Typed ordering and admission policy for prepared-PDF narration. */
public enum PdfNarratablePreparationMode {
    FAST_LISTEN(PdfPreparationPriority.URGENT, false, true),
    PLAY_SELECTION(PdfPreparationPriority.URGENT, false, true),
    GENERATE_PORTION(PdfPreparationPriority.HIGH, true, false),
    PROCESS_INTERVAL(PdfPreparationPriority.HIGH, true, false),
    COMPLETE_BATCH(PdfPreparationPriority.NORMAL, true, false),
    BACKGROUND_LOOK_AHEAD(PdfPreparationPriority.BACKGROUND, false, false);

    private final PdfPreparationPriority priority;
    private final boolean secondarySemanticPreparationRequired;
    private final boolean startsWhenFirstPageIsNarratable;

    PdfNarratablePreparationMode(
            PdfPreparationPriority priority,
            boolean secondarySemanticPreparationRequired,
            boolean startsWhenFirstPageIsNarratable) {
        this.priority = priority;
        this.secondarySemanticPreparationRequired = secondarySemanticPreparationRequired;
        this.startsWhenFirstPageIsNarratable = startsWhenFirstPageIsNarratable;
    }

    public PdfPreparationPriority priority() {
        return priority;
    }

    public boolean requiresSecondarySemanticPreparation() {
        return secondarySemanticPreparationRequired;
    }

    public boolean startsWhenFirstPageIsNarratable() {
        return startsWhenFirstPageIsNarratable;
    }

    public boolean background() {
        return this == BACKGROUND_LOOK_AHEAD;
    }
}
