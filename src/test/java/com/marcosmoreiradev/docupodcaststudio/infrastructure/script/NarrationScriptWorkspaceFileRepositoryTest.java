package com.marcosmoreiradev.docupodcaststudio.infrastructure.script;

import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetKind;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegmentType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class NarrationScriptWorkspaceFileRepositoryTest {
    @TempDir
    Path tempDir;

    @Test
    void writesNarrationScriptJsonAndReturnsRelativeAsset() throws Exception {
        NarrationScriptDocument script = NarrationScriptDocument.create("Guion", "es", "Doc", List.of(
                NarrationSegment.of("SEG-001", NarrationSegmentType.PARAGRAPH, "Intro", "Texto narrable", List.of("B001"))
        ));
        Path projectFile = tempDir.resolve("guion.docupodcast.json");

        NarrationScriptWorkspaceFileRepository repository = new NarrationScriptWorkspaceFileRepository();
        var materialized = repository.materialize(script, projectFile);

        assertTrue(Files.exists(tempDir.resolve("script/narration-script.json")));
        assertEquals(ProjectAssetKind.NARRATION_SCRIPT, materialized.narrationScriptAsset().kind());
        assertEquals("script/narration-script.json", materialized.narrationScriptAsset().relativePath());
        assertTrue(Files.readString(tempDir.resolve("script/narration-script.json")).contains("SEG-001"));

        NarrationScriptDocument loaded = repository.load(projectFile).orElseThrow();
        assertEquals("Guion", loaded.title());
        assertEquals(1, loaded.segmentCount());
        assertEquals("SEG-001", loaded.segments().get(0).id());
        assertEquals("Texto narrable", loaded.segments().get(0).narrationText());
    }
}
