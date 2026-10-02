package com.marcosmoreiradev.docupodcaststudio.application.modelsetup;

import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettings;
import com.marcosmoreiradev.docupodcaststudio.application.settings.ImageGenerationSettings;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class InspectLocalTheatreImageSetupReadinessUseCaseTest {
    @TempDir
    Path tempDir;

    @Test
    void reportsReadyWhenRuntimeAndModelPackageExist() throws Exception {
        Files.createDirectories(tempDir.resolve("tools/image"));
        Files.writeString(tempDir.resolve("tools/image/start-image-engine.bat"), "@echo off\r\n");
        Path image = tempDir.resolve("models/image");
        Files.createDirectories(image);
        writeSparse(image.resolve("v1-5-pruned-emaonly-fp16.safetensors"), 1_200_000);
        writeRealWorkflow(image);
        Files.writeString(image.resolve("model-manifest.json"), "{}");

        LocalTheatreImageSetupReadinessReport report = new InspectLocalTheatreImageSetupReadinessUseCase()
                .inspect(OperationalSettings.defaults(), tempDir);

        assertTrue(report.ready());
        assertTrue(report.userMessage().contains("basica"));
    }

    @Test
    void reportsPendingWhenRuntimeFolderExistsWithoutLauncher() throws Exception {
        Files.createDirectories(tempDir.resolve("tools/image"));
        Path image = tempDir.resolve("models/image");
        Files.createDirectories(image);
        writeSparse(image.resolve("v1-5-pruned-emaonly-fp16.safetensors"), 1_200_000);
        writeRealWorkflow(image);
        Files.writeString(image.resolve("model-manifest.json"), "{}");

        LocalTheatreImageSetupReadinessReport report = new InspectLocalTheatreImageSetupReadinessUseCase()
                .inspect(OperationalSettings.defaults(), tempDir);

        assertFalse(report.ready());
        assertFalse(report.runtimePrepared());
        assertTrue(report.missingRequirements().stream().anyMatch(item -> item.contains("Lanzador compatible")));
    }

    @Test
    void reportsPendingWithoutTriggeringPreparationOrDownload() {
        LocalTheatreImageSetupReadinessReport report = new InspectLocalTheatreImageSetupReadinessUseCase()
                .inspect(OperationalSettings.defaults(), tempDir);

        assertFalse(report.ready());
        assertTrue(report.missingRequirements().stream().anyMatch(item -> item.contains("Runtime")));
        assertTrue(report.userMessage().contains("pendiente"));
    }

    @Test
    void rejectsPlaceholderWorkflowAndAdapterAsReady() throws Exception {
        Files.createDirectories(tempDir.resolve("tools/image"));
        Files.writeString(tempDir.resolve("tools/image/start-image-engine.bat"), "@echo off\r\n");
        Path image = tempDir.resolve("models/image");
        Files.createDirectories(image.resolve("workflows"));
        Files.createDirectories(image.resolve("adapters"));
        writeSparse(image.resolve("v1-5-pruned-emaonly-fp16.safetensors"), 1_200_000);
        Files.writeString(image.resolve("workflows/workflow-sd15-reference.json"),
                "{\"purpose\":\"placeholder workflow\"}");
        Files.writeString(image.resolve("adapters/reference-adapter-placeholder.bin"), "placeholder");
        Files.writeString(image.resolve("model-manifest.json"), "{}");

        LocalTheatreImageSetupReadinessReport report = new InspectLocalTheatreImageSetupReadinessUseCase()
                .inspect(OperationalSettings.defaults(), tempDir);

        assertFalse(report.ready());
        assertEquals(ImageEngineComponentStatus.PLACEHOLDER, report.artifactInspection().workflowStatus());
        assertTrue(report.missingRequirements().stream().anyMatch(item -> item.contains("Workflow ComfyUI real")));
    }

    @Test
    void fluxPresetUsesIntegratedWorkflowButStillRequiresEveryComponentAndLicense() throws Exception {
        Files.createDirectories(tempDir.resolve("tools/image"));
        Files.writeString(tempDir.resolve("tools/image/start-image-engine.bat"), "@echo off\r\n");
        Path image = tempDir.resolve("models/image");
        Files.createDirectories(image);
        writeSparse(image.resolve("flux1-dev.safetensors"), 1_200_000);
        writeRealWorkflow(image);
        Files.writeString(image.resolve("model-manifest.json"), "{}");

        OperationalSettings settings = new OperationalSettings(null, null, null, null,
                new ImageGenerationSettings("managed-local", "http://127.0.0.1:8188", "AUTO",
                        "HIGH_QUALITY_FLUX", "flux1-dev.safetensors", "models/image/adapters", 300, false),
                null, null, null, null);
        LocalTheatreImageSetupReadinessReport report = new InspectLocalTheatreImageSetupReadinessUseCase()
                .inspect(settings, tempDir);

        assertFalse(report.ready());
        assertEquals(ImageEngineComponentStatus.READY, report.artifactInspection().workflowStatus());
        assertTrue(report.missingRequirements().stream()
                .anyMatch(item -> item.contains("vae/ae.safetensors")));
        assertTrue(report.missingRequirements().stream()
                .anyMatch(item -> item.contains("condiciones del modelo")));
    }

    private static void writeSparse(Path path, long size) throws Exception {
        Files.createDirectories(path.getParent());
        try (SeekableByteChannel channel = Files.newByteChannel(path,
                StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
            channel.position(size - 1);
            channel.write(ByteBuffer.wrap(new byte[] { 0 }));
        }
    }

    private static void writeRealWorkflow(Path image) throws Exception {
        Files.createDirectories(image.resolve("workflows"));
        Files.writeString(image.resolve("workflows/workflow-sd15-reference.json"), """
                {
                  "1": {"class_type": "CheckpointLoaderSimple"},
                  "2": {"class_type": "CLIPTextEncode"},
                  "3": {"class_type": "KSampler"},
                  "4": {"class_type": "VAEDecode"},
                  "5": {"class_type": "SaveImage"}
                }
                """);
    }
}
