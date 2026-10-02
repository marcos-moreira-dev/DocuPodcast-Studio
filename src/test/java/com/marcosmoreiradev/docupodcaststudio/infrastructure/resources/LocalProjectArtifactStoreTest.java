package com.marcosmoreiradev.docupodcaststudio.infrastructure.resources;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

final class LocalProjectArtifactStoreTest {
    @TempDir Path temporaryDirectory;

    @Test
    void resolvesAndDiscardsOnlyProjectOwnedArtifacts() throws Exception {
        Path project = temporaryDirectory.resolve("project/docupodcast.json");
        Path artifact = temporaryDirectory.resolve("project/generated/frame.png");
        Files.createDirectories(artifact.getParent());
        Files.writeString(project, "{}");
        Files.writeString(artifact, "png");
        LocalProjectArtifactStore store = new LocalProjectArtifactStore();

        assertEquals(artifact, store.resolveExisting(project, "generated/frame.png"));
        assertTrue(store.isProjectOwnedRegularFile(project, artifact));

        store.discardProjectOwned(project, artifact);
        assertFalse(Files.exists(artifact));
    }

    @Test
    void rejectsTraversalAndExternalDeletion() throws Exception {
        Path project = temporaryDirectory.resolve("project/docupodcast.json");
        Path external = temporaryDirectory.resolve("outside.txt");
        Files.createDirectories(project.getParent());
        Files.writeString(project, "{}");
        Files.writeString(external, "keep");
        LocalProjectArtifactStore store = new LocalProjectArtifactStore();

        assertThrows(IOException.class, () -> store.resolveExisting(project, "../outside.txt"));
        assertThrows(IOException.class, () -> store.discardProjectOwned(project, external));
        assertTrue(Files.exists(external));
    }
}
