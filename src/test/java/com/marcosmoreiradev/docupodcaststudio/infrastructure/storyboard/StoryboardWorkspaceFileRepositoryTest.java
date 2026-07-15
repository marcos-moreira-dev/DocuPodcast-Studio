package com.marcosmoreiradev.docupodcaststudio.infrastructure.storyboard;

import com.marcosmoreiradev.docupodcaststudio.domain.storyboard.StoryboardBinding;
import com.marcosmoreiradev.docupodcaststudio.domain.storyboard.StoryboardDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class StoryboardWorkspaceFileRepositoryTest {
    @TempDir
    Path temp;

    @Test
    void writesStoryboardManifestAndReturnsRelativeAsset() throws Exception {
        Path projectFile = temp.resolve("MiProyecto.docupodcast.json");
        StoryboardDocument storyboard = new StoryboardDocument(
                "STORYBOARD-001",
                "Storyboard",
                "SCRIPT-001",
                List.of(StoryboardBinding.of("STB-001", "SEG-001", "IMG-001", "Caption")),
                Map.of(),
                Instant.now(),
                Instant.now(),
                ""
        );

        StoryboardWorkspaceFileRepository repository = new StoryboardWorkspaceFileRepository();
        var result = repository.materialize(projectFile, storyboard);

        assertTrue(Files.exists(temp.resolve("storyboard/storyboard.json")));
        assertEquals("storyboard/storyboard.json", result.asset().relativePath());
        assertTrue(Files.readString(result.absolutePath()).contains("SEG-001"));

        StoryboardDocument loaded = repository.load(projectFile).orElseThrow();
        assertEquals("Storyboard", loaded.title());
        assertEquals(1, loaded.bindingCount());
        assertEquals("IMG-001", loaded.bindings().get(0).imageAssetId());
    }
}
