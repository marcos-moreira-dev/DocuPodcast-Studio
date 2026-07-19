package com.marcosmoreiradev.docupodcaststudio.application.visual;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class VisualModelPackageServiceTest {
    @TempDir
    Path tempDir;

    private final VisualModelPackageService service = new VisualModelPackageService();

    @Test
    void inspectsKontextComponentsAndRequiresExplicitLicense() throws Exception {
        create("flux1-kontext-dev.safetensors");
        create("vae/ae.safetensors");
        create("text_encoders/clip_l.safetensors");
        create("text_encoders/t5xxl_fp8_e4m3fn.safetensors");
        create("workflows/workflow-flux-kontext-reference-api.json");

        VisualModelPackageInspection blocked = service.inspect(
                tempDir, VisualGenerationProfile.ADVANCED_FLUX_KONTEXT, false);
        VisualModelPackageInspection ready = service.inspect(
                tempDir, VisualGenerationProfile.ADVANCED_FLUX_KONTEXT, true);

        assertFalse(blocked.ready());
        assertTrue(blocked.missingComponents().stream().anyMatch(value -> value.contains("licencia")));
        assertTrue(ready.ready());
    }

    @Test
    void sdxlProductionRequiresFaceIdLoraAndInsightFace() throws Exception {
        create("sd_xl_base_1.0.safetensors");
        create("sd_xl_refiner_1.0.safetensors");
        create("clip_vision/CLIP-ViT-H-14-laion2B-s32B-b79K.safetensors");
        create("ipadapter/ip-adapter-plus_sdxl_vit-h.safetensors");
        create("ipadapter/ip-adapter-faceid-plusv2_sdxl.bin");
        create("controlnet/diffusers_xl_canny_full.safetensors");
        create("workflows/workflow-sdxl-ipadapter-reference-api.json");

        VisualModelPackageInspection incomplete = service.inspect(
                tempDir, VisualGenerationProfile.PRODUCTION_SDXL_REFERENCE, true);
        create("loras/ip-adapter-faceid-plusv2_sdxl_lora.safetensors");
        create("insightface/models/antelopev2/glintr100.onnx");
        VisualModelPackageInspection complete = service.inspect(
                tempDir, VisualGenerationProfile.PRODUCTION_SDXL_REFERENCE, true);

        assertFalse(incomplete.ready());
        assertTrue(incomplete.missingComponents().stream().anyMatch(value -> value.startsWith("faceid-lora")));
        assertTrue(incomplete.missingComponents().stream().anyMatch(value -> value.startsWith("insightface")));
        assertTrue(complete.ready());
    }

    @Test
    void detectsDuplicatesByHashAndReusesVerifiedContent() throws Exception {
        Path firstRoot = tempDir.resolve("first");
        Path secondRoot = tempDir.resolve("second");
        Files.createDirectories(firstRoot);
        Files.createDirectories(secondRoot);
        Path first = firstRoot.resolve("model.safetensors");
        Path second = secondRoot.resolve("copy.safetensors");
        Files.writeString(first, "same-model");
        Files.writeString(second, "same-model");

        Map<String, List<Path>> duplicates = service.findDuplicatesByHash(List.of(firstRoot, secondRoot));
        Path reused = tempDir.resolve("store/reused.safetensors");
        service.reuseByHash(first, reused);

        assertEquals(1, duplicates.size());
        assertEquals(2, duplicates.values().iterator().next().size());
        assertEquals(service.sha256(first), service.sha256(reused));
    }

    private void create(String relative) throws Exception {
        Path file = tempDir.resolve(relative);
        Files.createDirectories(file.getParent());
        Files.writeString(file, "component");
    }
}
