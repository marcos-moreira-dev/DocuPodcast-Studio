package com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow;

import com.marcosmoreiradev.docupodcaststudio.application.ApplicationServices;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlock;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlockType;
import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.document.SourceDocumentFormat;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.project.ProjectKind;
import com.marcosmoreiradev.docupodcaststudio.domain.project.ProjectStatus;
import com.marcosmoreiradev.docupodcaststudio.domain.reading.ReadingProfile;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegmentType;
import com.marcosmoreiradev.docupodcaststudio.domain.storyboard.StoryboardDocument;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.ProjectSession;
import com.marcosmoreiradev.docupodcaststudio.presentation.workspace.WorkspaceKind;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DocumentIntakeCoordinatorTest {
    @Test
    void attachingPrimarySourcePreservesProjectTitleAndClearsDerivedArtifacts() {
        DocumentIntakeCoordinator coordinator = new DocumentIntakeCoordinator(applicationServices());
        ProjectSession session = ProjectSession.newUnsaved(DocuPodcastProject.createNew("Proyecto editorial"));
        NarrationScriptDocument script = script();
        session.setNarrationScript(script);
        session.setStoryboard(StoryboardDocument.createForScript(script));

        coordinator.attachImportedDocument(session, document(), ReadingProfile.academicDefaults());

        assertEquals("Proyecto editorial", session.project().metadata().title());
        assertEquals(ProjectKind.DOCUMENT_ONLY, session.project().metadata().kind());
        assertEquals(ProjectStatus.DOCUMENT_IMPORTED, session.project().metadata().status());
        assertEquals(WorkspaceKind.DOCUMENT_READER.name(), session.project().viewState().get("activeWorkspace"));
        assertTrue(session.importedDocument().isPresent());
        assertTrue(session.narrationScript().isEmpty());
        assertTrue(session.storyboard().isEmpty());
        assertTrue(session.dirty());
    }

    private static ReadableDocument document() {
        return new ReadableDocument("Fuente externa", SourceDocumentFormat.TXT, Path.of("fuente.txt"),
                List.of(DocumentBlock.of("B1", DocumentBlockType.PARAGRAPH, "Texto narrable.", "")));
    }

    private static NarrationScriptDocument script() {
        return NarrationScriptDocument.create("Lectura anterior", "es", "anterior.txt",
                List.of(NarrationSegment.of("SEG-001", NarrationSegmentType.PARAGRAPH, "Anterior",
                        "Texto anterior.", List.of("B0"))));
    }

    private static ApplicationServices applicationServices() {
        return new ApplicationServices(null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null);
    }
}
