package com.marcosmoreiradev.docupodcaststudio.application.modelsetup;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class InspectLocalModelFolderUseCaseTest {
    @TempDir
    Path tempDir;

    @Test
    void detectsReadyPiperFolderWithChecksumManifest() throws Exception {
        Path piper = tempDir.resolve("piper");
        Files.createDirectories(piper);
        Files.writeString(piper.resolve("es_voice.onnx"), "mock");
        Files.writeString(piper.resolve("es_voice.onnx.json"), "{}");
        Files.writeString(piper.resolve("SHA256SUMS.txt"), "mock checksum");

        ModelInspectionResult result = new InspectLocalModelFolderUseCase()
                .inspect(ModelFolderContract.piperLightweight(), piper);

        assertEquals(ModelInspectionStatus.READY, result.status());
        assertTrue(result.usable());
        assertTrue(result.checksumManifestPresent());
        assertTrue(result.discoveredFiles().contains("es_voice.onnx"));
    }

    @Test
    void reportsMissingRequirementsWithoutBreakingManualImportFlow() throws Exception {
        Path xtts = tempDir.resolve("xtts");
        Files.createDirectories(xtts);
        Files.writeString(xtts.resolve("config.json"), "{}");

        ModelInspectionResult result = new InspectLocalModelFolderUseCase()
                .inspect(ModelFolderContract.xttsHighQuality(), xtts);

        assertEquals(ModelInspectionStatus.MISSING_REQUIRED_FILES, result.status());
        assertTrue(result.needsManualFix());
        assertFalse(result.usable());
        assertTrue(result.missingRequirements().stream().anyMatch(item -> item.contains("pesos del modelo")));
        assertTrue(result.userMessage().contains("Faltan archivos"));
    }

    @Test
    void detectsReadyLocalTheatreImageFolder() throws Exception {
        Path image = tempDir.resolve("image");
        Files.createDirectories(image);
        Files.writeString(image.resolve("v1-5-pruned-emaonly-fp16.safetensors"), "mock");
        Files.writeString(image.resolve("workflow-sd15-reference.json"), "{}");
        Files.writeString(image.resolve("reference-adapter.bin"), "mock");
        Files.writeString(image.resolve("manifest.json"), "{}");
        Files.writeString(image.resolve("SHA256SUMS.txt"), "mock checksum");

        ModelInspectionResult result = new InspectLocalModelFolderUseCase()
                .inspect(ModelFolderContract.localTheatreImage(), image);

        assertEquals(ModelInspectionStatus.READY, result.status());
        assertTrue(result.usable());
        assertTrue(result.discoveredFiles().contains("workflow-sd15-reference.json"));
    }

    @Test
    void recommendedContractsDoNotIncludeSpeechToText() {
        String all = ModelFolderContract.recommended().toString().toLowerCase(java.util.Locale.ROOT);
        assertFalse(all.contains("whisper"));
        assertFalse(all.contains("stt"));
        assertTrue(all.contains("image-local-theatre"));
    }
}
