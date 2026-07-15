package com.marcosmoreiradev.docupodcaststudio.presentation.shell;

import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlock;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlockType;
import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.document.SourceDocumentFormat;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegmentType;
import com.marcosmoreiradev.docupodcaststudio.domain.storyboard.StoryboardDocument;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ProjectSessionHydrationTest {
    @Test
    void hydrationDoesNotMarkOpenedProjectDirty() {
        ProjectSession session = ProjectSession.opened(DocuPodcastProject.empty("Proyecto"), Path.of("Proyecto.docupodcast.json"));
        ReadableDocument document = new ReadableDocument("Doc", SourceDocumentFormat.DOCX, Path.of("source/doc.docx"),
                List.of(DocumentBlock.of("B001", DocumentBlockType.PARAGRAPH, "Texto", "Normal")));
        NarrationScriptDocument script = NarrationScriptDocument.create("Guion", "es", "Doc", List.of(
                NarrationSegment.of("SEG-001", NarrationSegmentType.PARAGRAPH, "Intro", "Texto", List.of("B001"))));
        StoryboardDocument storyboard = StoryboardDocument.createForScript(script);

        session.hydrateImportedDocument(document);
        session.hydrateNarrationScript(script);
        session.hydrateStoryboard(storyboard);

        assertFalse(session.dirty());
        assertTrue(session.importedDocument().isPresent());
        assertTrue(session.narrationScript().isPresent());
        assertTrue(session.storyboard().isPresent());
    }
}
