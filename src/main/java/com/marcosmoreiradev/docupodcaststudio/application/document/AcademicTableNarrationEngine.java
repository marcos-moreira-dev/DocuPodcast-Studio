package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfDerivedTreatmentKind;

/** Current table workflow; the historical small-table engine remains readable. */
public final class AcademicTableNarrationEngine
        extends RuleBasedSmallTableNarrationEngine {
    public static final String ID = "pdf-academic-table-narration-local";

    public AcademicTableNarrationEngine() {
        super(PdfDerivedTreatmentKind.TABLE_NARRATION);
    }

    @Override public String id() {
        return ID;
    }
}
