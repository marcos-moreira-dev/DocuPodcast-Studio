package com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow;

import com.marcosmoreiradev.docupodcaststudio.application.ApplicationServices;
import com.marcosmoreiradev.docupodcaststudio.application.document.RefreshSourceDocumentResult;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlock;
import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.reading.ReadingProfile;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.ProjectSession;

import java.io.IOException;
import java.util.Objects;

/**
 * Coordinates the brain workflow behind the visible "Refrescar contenido" action.
 *
 * <p>The external Word/PDF/Markdown/TXT source remains read-only. This coordinator re-imports it,
 * applies the current reading profile to the refreshed in-project document and reports which
 * derived artifacts need review without deleting audio, layers or storyboard data silently.</p>
 */
public final class SourceDocumentRefreshCoordinator {
    private final ApplicationServices applicationServices;

    public SourceDocumentRefreshCoordinator(ApplicationServices applicationServices) {
        this.applicationServices = Objects.requireNonNull(applicationServices, "applicationServices");
    }

    public SourceDocumentRefreshOutcome refresh(ProjectSession session,
                                                ReadableDocument currentDocument,
                                                ReadingProfile activeProfile) throws IOException {
        Objects.requireNonNull(session, "session");
        Objects.requireNonNull(currentDocument, "currentDocument");
        Objects.requireNonNull(activeProfile, "activeProfile");

        RefreshSourceDocumentResult result = applicationServices.document().refreshSourceDocument().refresh(currentDocument);
        if (!result.refreshedDocumentAvailable()) {
            return new SourceDocumentRefreshOutcome(null, result.report(), false);
        }

        ReadableDocument classified = applicationServices.readingProfile()
                .applyReadingProfile()
                .apply(result.refreshedDocument(), activeProfile);

        boolean visualMetadataChanged = sourceVisualMetadataChanged(currentDocument, classified);
        if (result.report().hasContentChanges() || visualMetadataChanged) {
            session.setImportedDocument(classified);
            return new SourceDocumentRefreshOutcome(classified, result.report(), true);
        }

        session.hydrateImportedDocument(classified);
        return new SourceDocumentRefreshOutcome(classified, result.report(), false);
    }

    private static boolean sourceVisualMetadataChanged(ReadableDocument previous, ReadableDocument refreshed) {
        if (previous.blocks().size() != refreshed.blocks().size()) {
            return false;
        }
        for (int index = 0; index < previous.blocks().size(); index++) {
            DocumentBlock before = previous.blocks().get(index);
            DocumentBlock after = refreshed.blocks().get(index);
            if (!before.id().equals(after.id()) || before.type() != after.type()) {
                return false;
            }
            if ((before.sourceVisual() || after.sourceVisual()) && !before.metadata().equals(after.metadata())) {
                return true;
            }
        }
        return false;
    }
}
