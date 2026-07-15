package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tanda 2: fragments are a derived cross-media projection, not a persisted schema replacement. */
final class FragmentWorkspaceProjectionSourceTest {
    @Test
    void fragmentProjectionConnectsExistingContractsWithoutReplacingThem() throws Exception {
        String useCase = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/fragment/BuildFragmentWorkspaceProjectionUseCase.java");
        String scriptBuilder = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/script/BuildNarrationScriptUseCase.java");
        String project = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/domain/project/DocuPodcastProject.java");

        assertTrue(useCase.contains("ReadableDocument"));
        assertTrue(useCase.contains("NarrationScriptDocument"));
        assertTrue(useCase.contains("AudioJobSnapshot"));
        assertTrue(useCase.contains("StoryboardDocument"));
        assertTrue(useCase.contains("PlaybackManifest"));
        assertTrue(useCase.contains("FragmentWorkspaceProjection"));
        assertTrue(scriptBuilder.contains("\"fragmentId\", FragmentId.fromBlockId(block.id()).value()"));
        assertFalse(project.contains("DocumentFragment"));
        assertFalse(project.contains("FragmentAssetBinding"));
    }

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path));
    }
}
