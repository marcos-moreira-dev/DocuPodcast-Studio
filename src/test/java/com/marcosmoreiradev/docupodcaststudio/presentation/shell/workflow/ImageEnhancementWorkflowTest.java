package com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow;

import com.marcosmoreiradev.docupodcaststudio.application.image.ImageAspectStrategy;
import com.marcosmoreiradev.docupodcaststudio.application.image.ImageEnhancementOutputProfile;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class ImageEnhancementWorkflowTest {
    @Test
    void mapsLegacyEnhancementToCompositionPreservingRefinement() {
        var request = ImageEnhancementWorkflow.request(Path.of("source.png"), Path.of("generated"), "candidate",
                ImageAspectStrategy.OUTPAINT_TO_TARGET);

        assertTrue(request.prompt().contains("preserve identity"));
        assertTrue(request.options().get("sourceKind").contains("legacy"));
    }
}
