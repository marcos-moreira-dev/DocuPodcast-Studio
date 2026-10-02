package com.marcosmoreiradev.docupodcaststudio.presentation.settings;

import com.marcosmoreiradev.docupodcaststudio.media.api.*;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class EngineAdministrationControllerTest {
    private static final EngineId IMAGE_ENGINE = new EngineId("fake-image");

    @Test
    void exposesRegisteredEnginesWithoutProviderSpecificPresentationBranches() {
        EngineAdministrationController controller = controller();

        var engines = controller.engines();

        assertEquals(1, engines.size());
        assertEquals(CapabilityId.IMAGE_GENERATION, engines.getFirst().descriptor().capability());
    }

    @Test
    void obtainsReadinessFromTheRegisteredEngine() {
        EngineReadiness readiness = controller().readiness(IMAGE_ENGINE);

        assertTrue(readiness.ready());
        assertEquals(IMAGE_ENGINE, readiness.engineId());
    }

    private static EngineAdministrationController controller() {
        EngineRegistry<ImageGenerationEngine> images = new EngineRegistry<>(CapabilityId.IMAGE_GENERATION);
        images.register(new FakeImageEngine());
        return new EngineAdministrationController(new MediaEnginePlatform(null, images, null, null, null));
    }

    private static final class FakeImageEngine implements ImageGenerationEngine {
        @Override public EngineDescriptor descriptor() {
            return new EngineDescriptor(IMAGE_ENGINE, CapabilityId.IMAGE_GENERATION, "Fake image", "1", "test",
                    Set.of(), false);
        }

        @Override public EngineReadiness inspectReadiness(EngineConfiguration configuration) {
            return EngineReadiness.ready(IMAGE_ENGINE, "Ready");
        }

        @Override public EngineConfigurationSchema configurationSchema() {
            return new EngineConfigurationSchema(IMAGE_ENGINE, List.of());
        }

        @Override public ImageGenerationResult generate(ImageGenerationRequest request, ExecutionContext context)
                throws IOException {
            return new ImageGenerationResult(List.of(request.outputDirectory().resolve("fake.png")), java.util.Map.of());
        }
    }
}
