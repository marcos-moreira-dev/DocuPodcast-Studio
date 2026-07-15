package com.marcosmoreiradev.docupodcaststudio.application.project;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class ProjectContainerPathPolicyTest {
    private final ProjectContainerPathPolicy policy = new ProjectContainerPathPolicy();

    @TempDir
    Path tempDir;

    @Test
    void saveAsLooseFileCreatesContainingFolderWithProjectFileInside() {
        Path selected = tempDir.resolve("Obra.docupodcast.json");

        Path resolved = policy.resolveSaveAsTarget(selected);

        assertEquals(tempDir.resolve("Obra").resolve("Obra.docupodcast.json"), resolved);
    }

    @Test
    void saveAsNameWithoutExtensionStillCreatesContainingFolder() {
        Path selected = tempDir.resolve("Podcast de historia");

        Path resolved = policy.resolveSaveAsTarget(selected);

        assertEquals(tempDir.resolve("Podcast de historia").resolve("Podcast de historia.docupodcast.json"), resolved);
    }

    @Test
    void alreadyContainedProjectFileIsPreserved() {
        Path selected = tempDir.resolve("Demo").resolve("Demo.docupodcast.json");

        Path resolved = policy.resolveSaveAsTarget(selected);

        assertEquals(selected, resolved);
    }

    @Test
    void invalidFolderCharactersAreCleanedForContainerName() {
        assertEquals("Lucia-Acto-1", policy.sanitizeProjectName("Lucia:Acto?1"));
    }
}
