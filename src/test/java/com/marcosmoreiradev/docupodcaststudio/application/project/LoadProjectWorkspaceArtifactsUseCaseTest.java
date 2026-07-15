package com.marcosmoreiradev.docupodcaststudio.application.project;

import com.marcosmoreiradev.docupodcaststudio.application.document.ImportedDocumentWorkspaceRepository;
import com.marcosmoreiradev.docupodcaststudio.application.document.MaterializedImportedDocument;
import com.marcosmoreiradev.docupodcaststudio.application.script.MaterializedNarrationScript;
import com.marcosmoreiradev.docupodcaststudio.application.script.NarrationScriptWorkspaceRepository;
import com.marcosmoreiradev.docupodcaststudio.application.storyboard.MaterializedStoryboard;
import com.marcosmoreiradev.docupodcaststudio.application.storyboard.StoryboardWorkspaceRepository;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlock;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlockType;
import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.document.SourceDocumentFormat;
import com.marcosmoreiradev.docupodcaststudio.domain.reading.ReadingProfile;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegmentType;
import com.marcosmoreiradev.docupodcaststudio.domain.storyboard.StoryboardDocument;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class LoadProjectWorkspaceArtifactsUseCaseTest {
    @Test
    void loadsAllMaterializedWorkspaceArtifacts() throws Exception {
        ReadableDocument document = new ReadableDocument("Doc", SourceDocumentFormat.DOCX, Path.of("source/doc.docx"),
                List.of(DocumentBlock.of("B001", DocumentBlockType.PARAGRAPH, "Texto", "Normal")));
        NarrationScriptDocument script = NarrationScriptDocument.create("Guion", "es", "Doc", List.of(
                NarrationSegment.of("SEG-001", NarrationSegmentType.PARAGRAPH, "Intro", "Texto", List.of("B001"))));
        StoryboardDocument storyboard = StoryboardDocument.createForScript(script);

        LoadProjectWorkspaceArtifactsUseCase useCase = new LoadProjectWorkspaceArtifactsUseCase(
                new DocumentRepo(document), new ScriptRepo(script), new StoryboardRepo(storyboard));

        ProjectWorkspaceHydration hydration = useCase.load(Path.of("Proyecto.docupodcast.json"));

        assertEquals(3, hydration.loadedArtifactCount());
        assertTrue(hydration.importedDocument().isPresent());
        assertTrue(hydration.narrationScript().isPresent());
        assertTrue(hydration.storyboard().isPresent());
        assertTrue(hydration.statusLabel().contains("lectura preparada 1 segmentos"));
    }

    private record DocumentRepo(ReadableDocument document) implements ImportedDocumentWorkspaceRepository {
        @Override public MaterializedImportedDocument materialize(ReadableDocument document, ReadingProfile activeReadingProfile, Path projectFile) { throw new UnsupportedOperationException(); }
        @Override public Optional<ReadableDocument> load(Path projectFile) { return Optional.of(document); }
    }

    private record ScriptRepo(NarrationScriptDocument script) implements NarrationScriptWorkspaceRepository {
        @Override public MaterializedNarrationScript materialize(NarrationScriptDocument script, Path projectFile) { throw new UnsupportedOperationException(); }
        @Override public Optional<NarrationScriptDocument> load(Path projectFile) { return Optional.of(script); }
    }

    private record StoryboardRepo(StoryboardDocument storyboard) implements StoryboardWorkspaceRepository {
        @Override public MaterializedStoryboard materialize(Path projectFile, StoryboardDocument storyboard) throws IOException { throw new UnsupportedOperationException(); }
        @Override public Optional<StoryboardDocument> load(Path projectFile) { return Optional.of(storyboard); }
    }
}
