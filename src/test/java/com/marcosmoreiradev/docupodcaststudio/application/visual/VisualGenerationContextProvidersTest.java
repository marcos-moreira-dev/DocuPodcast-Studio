package com.marcosmoreiradev.docupodcaststudio.application.visual;

import com.marcosmoreiradev.docupodcaststudio.application.documentstudy.DocumentStudyVisualGenerationContextProvider;
import com.marcosmoreiradev.docupodcaststudio.application.documentstudy.DocumentStudyVisualGenerationInput;
import com.marcosmoreiradev.docupodcaststudio.application.narrative.NarrativeVisualGenerationContextProvider;
import com.marcosmoreiradev.docupodcaststudio.application.narrative.NarrativeVisualGenerationInput;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.TheatreImageContextAsset;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.TheatreImageGenerationUnit;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.TheatreVisualGenerationContext;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.TheatreVisualGenerationContextProvider;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.TheatreVisualGenerationInput;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class VisualGenerationContextProvidersTest {
    @TempDir
    Path tempDir;

    @Test
    void theatreMapsEverySemanticReferenceWithoutLeakingIdsIntoCoreFields() throws Exception {
        TheatreImageContextAsset identity = asset("identity", "Capitan", "id-identity");
        TheatreImageContextAsset object = asset("object", "Avion", "id-object");
        TheatreImageContextAsset environment = asset("environment", "Montana", "id-environment");
        TheatreImageContextAsset previous = asset("previous", "Anterior", "id-previous");
        TheatreImageContextAsset next = asset("next", "Siguiente", "id-next");
        TheatreImageContextAsset drawing = asset("drawing", "Boceto", "id-drawing");
        TheatreImageContextAsset camera = asset("camera", "Cerca centro nivel", "id-camera");
        TheatreImageGenerationUnit unit = new TheatreImageGenerationUnit(
                "ACT-1", "El vuelo", "SCN-1", "Hangar", "INT-1", "SEG-1",
                "CAPITAN", "Texto", "Contexto espacial", null);
        TheatreVisualGenerationContext context = new TheatreVisualGenerationContext(
                unit, List.of(identity), List.of(object), List.of(environment),
                previous, next, drawing, camera);

        VisualGenerationRequest request = new TheatreVisualGenerationContextProvider().build(
                new TheatreVisualGenerationInput(
                        context, "prompt", "negative", 1919, 1079,
                        VisualGenerationProfile.PRODUCTION_SDXL_REFERENCE,
                        cudaBinding(), output("theatre")));

        assertEquals("theatre", request.metadata().get("consumer"));
        assertEquals("INT-1", request.metadata().get("interventionId"));
        assertEquals(1920, request.width());
        assertEquals(1080, request.height());
        for (VisualConditioningRole role : VisualConditioningRole.values()) {
            if (role != VisualConditioningRole.STYLE) {
                assertTrue(request.references().has(role), "Missing role " + role);
            }
        }
    }

    @Test
    void documentaryAndNarrativeBuildRequestsThroughSameCoreContract() {
        VisualReferenceBundle references = VisualReferenceBundle.empty();
        VisualGenerationRequest documentary = new DocumentStudyVisualGenerationContextProvider().build(
                new DocumentStudyVisualGenerationInput(
                        "illustrate paragraph", "", references, 1680, 432,
                        VisualGenerationProfile.PRODUCTION_SDXL_REFERENCE,
                        cudaBinding(), output("documentary"), Map.of("blockId", "B0002")));
        VisualGenerationRequest narrative = new NarrativeVisualGenerationContextProvider().build(
                new NarrativeVisualGenerationInput(
                        "cinematic scene", "", references, 1920, 1080,
                        VisualGenerationProfile.ADVANCED_FLUX_KONTEXT,
                        cudaBinding(), output("narrative"), Map.of("sceneId", "S01")));

        assertEquals("document-study", documentary.metadata().get("consumer"));
        assertEquals("B0002", documentary.metadata().get("blockId"));
        assertEquals("narrative-video", narrative.metadata().get("consumer"));
        assertEquals("S01", narrative.metadata().get("sceneId"));
    }

    private TheatreImageContextAsset asset(String role, String label, String id) throws Exception {
        Path path = tempDir.resolve(id + ".png");
        Files.write(path, new byte[] {1, 2, 3});
        return new TheatreImageContextAsset(role, label, id,
                tempDir.relativize(path).toString(), path.toUri().toString());
    }

    private VisualOutputTarget output(String name) {
        return new VisualOutputTarget(tempDir, Path.of("generated", name), name, true);
    }

    private static VisualComputeBinding cudaBinding() {
        return new VisualComputeBinding(
                "gpu-nvidia-0", "NVIDIA GeForce GTX 1650",
                VisualComputeBackend.CUDA, 0, List.of("--cuda-device", "0"));
    }
}
