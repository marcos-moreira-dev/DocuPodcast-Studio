package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ProjectContainerFolderSourceTest {
    @Test
    void saveAsUsesContainerPolicyInsteadOfLooseProjectFile() throws Exception {
        String view = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellView.java"));
        String policy = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/project/ProjectContainerPathPolicy.java"));

        assertTrue(view.contains("ProjectContainerPathPolicy"));
        assertTrue(view.contains("resolveSaveAsTarget(file.toPath())"));
        assertTrue(view.contains("carpeta contenedora"));
        assertFalse(view.contains("ensureDocuPodcastJson(file.toPath())"));

        assertTrue(policy.contains("A DocuPodcast project is intentionally a folder"));
        assertTrue(policy.contains("source/"));
        assertTrue(policy.contains("document/"));
        assertTrue(policy.contains("jobs/"));
    }

    @Test
    void roadmapRecordsContainerFolderBeforeMediaExpansion() throws Exception {
        String roadmap = Files.readString(Path.of("docs/productizacion/ROADMAP_POST_T80B_CONTENEDOR_PROYECTO.md"));
        assertTrue(roadmap.contains("T80C"));
        assertTrue(roadmap.contains("MP3/WAV/video"));
        assertTrue(roadmap.contains("carpeta contenedora"));
    }
}
