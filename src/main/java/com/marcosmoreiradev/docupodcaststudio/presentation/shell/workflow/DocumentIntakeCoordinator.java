package com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow;

import com.marcosmoreiradev.docupodcaststudio.application.WorkspaceApplicationServices;
import com.marcosmoreiradev.docupodcaststudio.application.document.ProjectDocumentSource;
import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.project.ProjectKind;
import com.marcosmoreiradev.docupodcaststudio.domain.project.ProjectStatus;
import com.marcosmoreiradev.docupodcaststudio.domain.reading.ReadingProfile;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.ProjectSession;
import com.marcosmoreiradev.docupodcaststudio.presentation.workspace.WorkspaceKind;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Handles source-document intake for the document-root workflow.
 *
 * <p>The shell remains responsible for user interaction; this coordinator owns the brain step that
 * imports the external read-only source, applies the reading profile and attaches the resulting
 * narrated document to the active project.</p>
 */
public final class DocumentIntakeCoordinator {
    private final WorkspaceApplicationServices applicationServices;

    public DocumentIntakeCoordinator(WorkspaceApplicationServices applicationServices) {
        this.applicationServices = Objects.requireNonNull(applicationServices, "applicationServices");
    }

    public ProjectDocumentSource importAndClassify(Path sourceFile, ReadingProfile profile) throws IOException {
        Objects.requireNonNull(profile, "profile");
        ProjectDocumentSource source =
                applicationServices.project().document().documentSourceImport().importSource(sourceFile);
        if (source instanceof com.marcosmoreiradev.docupodcaststudio.application.document.BlockDocumentSource block) {
            ReadableDocument classified = applicationServices.project().readingProfile()
                    .applyReadingProfile().apply(block.document(), profile);
            return new com.marcosmoreiradev.docupodcaststudio.application.document.BlockDocumentSource(classified);
        }
        return source;
    }

    public ReadableDocument importAndClassifyBlock(Path sourceFile, ReadingProfile profile) throws IOException {
        ProjectDocumentSource source = importAndClassify(sourceFile, profile);
        if (source instanceof com.marcosmoreiradev.docupodcaststudio.application.document.BlockDocumentSource block) {
            return block.document();
        }
        throw new IOException("Esta operación requiere DOCX, TXT o Markdown; PDF usa preparación V2.");
    }

    public void attachImportedDocument(ProjectSession session, ReadableDocument classified, ReadingProfile profile) {
        attachDocumentSource(session,
                new com.marcosmoreiradev.docupodcaststudio.application.document.BlockDocumentSource(classified),
                profile);
    }

    public void attachDocumentSource(ProjectSession session, ProjectDocumentSource source,
                                     ReadingProfile profile) {
        Objects.requireNonNull(session, "session");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(profile, "profile");
        DocuPodcastProject updated = session.project()
                .withMetadata(session.project().metadata()
                        .withKind(ProjectKind.DOCUMENT_ONLY)
                        .withStatus(ProjectStatus.DOCUMENT_IMPORTED))
                .withReadingProfile(profile)
                .withViewState(WorkspaceNavigationCoordinator.ACTIVE_WORKSPACE_KEY, WorkspaceKind.DOCUMENT_READER.name());
        session.replaceProject(updated, true);
        session.setDocumentSource(source);
        session.clearNarrationScript();
        session.clearStoryboard();
    }
}
