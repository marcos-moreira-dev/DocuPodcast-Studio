package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Guards the theatre-specific UI contracts that share storyboard and audio infrastructure. */
final class TheatreVisualVariantsAndAudioRegenerationSourceTest {
    @Test
    void mediaRailCyclesAssignedGeneratedAndDrawnVariantsWithoutReplacingMissingSlots() throws IOException {
        String variants = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/storyboard/TheatreVisualVariant.java");
        String rail = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentMediaRailView.java");
        String upsert = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/storyboard/UpsertTheatreStoryboardFrameVariantUseCase.java");
        String cleanVideo = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/video/BuildTheatreCleanVideoPlanUseCase.java");
        String primaryResolver = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/theatre/TheatrePrimaryVisualResolver.java");

        assertTrue(variants.contains("OFFICIAL"));
        assertTrue(variants.contains("GENERATED"));
        assertTrue(variants.contains("DRAWN"));
        assertTrue(rail.contains("TheatreVisualVariant.values()"));
        assertTrue(rail.contains("activateTheatreStoryboardVisualVariant"));
        assertTrue(rail.contains("Imagen IA pendiente"));
        assertTrue(rail.contains("Boceto no disponible"));
        assertTrue(upsert.contains("generatedImageAssetId"));
        assertTrue(upsert.contains("upsertGeneratedFrame"));
        assertTrue(cleanVideo.contains("TheatrePrimaryVisualResolver"));
        assertTrue(primaryResolver.contains("GENERATED_IMAGE_ASSET_ID"));
    }

    @Test
    void fragmentRailRegeneratesOnlyTheSelectedTtsInterventionAtomically() throws IOException {
        String panel = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentContextDetailsPanel.java");
        String workflow = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/TheatreInterventionAudioRegenerationWorkflow.java");
        String useCase = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/audio/RegenerateTheatreInterventionAudioUseCase.java");

        assertTrue(panel.contains("Renderizar audio de fragmento de nuevo según configuración"));
        assertTrue(workflow.contains("audio humano importado"));
        assertTrue(useCase.contains("applyGeneratedBatch"));
        assertTrue(useCase.contains("expectedUnitIds"));
        assertFalse(useCase.contains("TheatreAudioTrack"));
    }

    private static String read(String path) throws IOException {
        return Files.readString(Path.of(path));
    }
}
