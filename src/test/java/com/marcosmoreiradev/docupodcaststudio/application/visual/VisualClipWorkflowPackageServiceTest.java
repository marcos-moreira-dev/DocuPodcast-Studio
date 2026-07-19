package com.marcosmoreiradev.docupodcaststudio.application.visual;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class VisualClipWorkflowPackageServiceTest {
    @TempDir
    Path tempDir;

    private final VisualClipWorkflowPackageService service = new VisualClipWorkflowPackageService();

    @Test
    void loadsExplicitCustomApiWorkflowWithoutDownloadingAnything() throws Exception {
        Path workflow = tempDir.resolve("video-api.json");
        Files.writeString(workflow, """
                {
                  "1": {
                    "class_type": "LoadImage",
                    "inputs": {"image": "{{START_IMAGE}}"}
                  }
                }
                """);

        VisualClipWorkflowPackageService.WorkflowPackage installed = service.require(
                VisualClipGenerationProfile.CUSTOM_COMFY_VIDEO,
                workflow.toString());

        assertEquals(workflow.toAbsolutePath().normalize(), installed.workflowPath());
        assertEquals("custom-comfy-video", installed.modelId());
        assertTrue(installed.template().contains("{{START_IMAGE}}"));
    }

    @Test
    void missingPackageFailsPreflightWithoutAutomaticDownload() {
        IOException failure = assertThrows(IOException.class, () -> service.require(
                VisualClipGenerationProfile.CUSTOM_COMFY_VIDEO,
                tempDir.resolve("missing.json").toString()));

        assertTrue(failure.getMessage().contains("No se descargara automaticamente"));
        assertTrue(failure.getMessage().contains("missing.json"));
    }
}
