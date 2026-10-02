package com.marcosmoreiradev.docupodcaststudio.infrastructure.json;

import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.project.ProjectVisualProcessingSettings;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VisualProcessingProjectJsonTest {
    @Test
    void roundTripsProjectOverridesInVersionSix() throws Exception {
        DocuPodcastProject source = DocuPodcastProject.createNew("540p")
                .withVisualProcessing(new ProjectVisualProcessingSettings(
                        "P540", true, "UHD_4K", "RealESRGAN_x4plus.pth",
                        true, "conservative", "comfyui-controlnet-tile"));

        String json = new DocuPodcastProjectJsonWriter().write(source);
        DocuPodcastProject restored = new DocuPodcastProjectJsonReader().read(json);

        assertTrue(json.contains("\"formatVersion\": 6"));
        assertTrue(json.contains("\"visualProcessing\""));
        assertTrue(json.contains("\"refineAfterUpscale\": true"));
        assertTrue(json.contains("\"refinementPreset\": \"conservative\""));
        assertEquals(source.visualProcessing(), restored.visualProcessing());
    }

    @Test
    void oldProjectsInheritGlobalVisualDefaults() throws Exception {
        String json = new DocuPodcastProjectJsonWriter().write(DocuPodcastProject.createNew("legacy"))
                .replace("\"formatVersion\": 6", "\"formatVersion\": 5");
        DocuPodcastProject restored = new DocuPodcastProjectJsonReader().read(json);

        assertFalse(restored.visualProcessing().hasOverrides());
        assertNull(restored.visualProcessing().upscaleEnabled());
        assertNull(restored.visualProcessing().refineAfterUpscale());
    }
}
