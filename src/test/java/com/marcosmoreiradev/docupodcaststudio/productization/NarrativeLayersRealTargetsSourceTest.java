package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class NarrativeLayersRealTargetsSourceTest {
    @Test
    void narrativeLayersUseConcreteTargetsAndDoNotPersistPendingPlaceholders() throws Exception {
        String coordinator = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/NarrativeLayerCoordinator.java");
        String resolver = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/NarrativeLayerTargetResolver.java");
        String roadmap = read("docs/productizacion/ROADMAP_POST_T74_NARRATIVE_LAYERS_REALES.md");

        assertTrue(resolver.contains("Resolve" ) || resolver.contains("Resolves concrete project targets"));
        assertTrue(resolver.contains("VOC-NARRATOR"));
        assertTrue(resolver.contains("ProjectAssetKind.IMAGE"));
        assertTrue(resolver.contains("ProjectAssetKind.AUDIO_CLIP"));
        assertTrue(coordinator.contains("missingTarget"));
        assertFalse(coordinator.contains("VOICE-IA-DEFAULT"));
        assertFalse(coordinator.contains("IMAGE-STORYBOARD-PENDIENTE"));
        assertFalse(coordinator.contains("AUDIO-EXTERNO-PENDIENTE"));
        assertTrue(roadmap.contains("T75"));
        assertTrue(roadmap.contains("Storyboard como capa"));
    }

    private static String read(String path) throws Exception {
        Path file = Path.of(path);
        assertTrue(Files.exists(file), path + " debe existir");
        return Files.readString(file);
    }
}
