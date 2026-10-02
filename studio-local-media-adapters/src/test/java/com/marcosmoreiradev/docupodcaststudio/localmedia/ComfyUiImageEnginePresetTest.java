package com.marcosmoreiradev.docupodcaststudio.localmedia;

import com.marcosmoreiradev.docupodcaststudio.media.api.EngineConfiguration;
import com.marcosmoreiradev.docupodcaststudio.media.api.EnginePresetDescriptor;
import com.marcosmoreiradev.docupodcaststudio.media.api.ImageGenerationRequest;
import com.marcosmoreiradev.docupodcaststudio.media.api.MediaReference;
import com.marcosmoreiradev.docupodcaststudio.media.api.MediaReferenceRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ComfyUiImageEnginePresetTest {
    @TempDir Path temp;

    @Test void publishesHonestReadinessForEveryDeclaredProductPreset() throws Exception {
        EngineConfiguration configuration = resources();
        ComfyUiImageEngine engine = new ComfyUiImageEngine(configuration);
        Map<String, String> states = engine.presets().stream().collect(java.util.stream.Collectors.toMap(
                descriptor -> descriptor.id().value(),
                descriptor -> descriptor.metadata().get("resourceState")));

        assertEquals("available", states.get("draft"));
        assertEquals("missing", states.get("sd15-regional-identity"));
        assertEquals("missing", states.get("sd15-dreamshaper"));
        assertEquals("missing", states.get("sdxl-reference"));
        assertEquals("available", states.get("flux-kontext"));
        assertEquals("available", states.get("flux-high-quality"));
        assertEquals("missing", states.get("custom-comfy-workflow"));
    }

    @Test void neverSilentlyDropsTheatreConditioningReferences() throws Exception {
        Path reference = Files.write(temp.resolve("reference.png"), new byte[] {1, 2, 3});
        ComfyUiImageEngine engine = new ComfyUiImageEngine(resources());
        ImageGenerationRequest request = new ImageGenerationRequest("escena teatral", "", 512, 512,
                List.of(new MediaReference("actor", reference, MediaReferenceRole.OBJECT, 1.0, Map.of())),
                temp.resolve("out"), "conditioned", ComfyUiImageEngine.DRAFT, 42, 1);

        IOException failure = assertThrows(IOException.class, () -> engine.generate(request, null));
        assertTrue(failure.getMessage().contains("conditioned-workflow-not-installed"));
    }

    @Test void retriesOnlyOnceAfterManagedRuntimeRecovery() throws Exception {
        int[] calls = {0, 0};
        var runtime = new ComfyUiImageEngine.ManagedRuntime() {
            public void ensureReady(com.marcosmoreiradev.docupodcaststudio.media.api.ExecutionContext context) { calls[0]++; }
            public boolean recover(com.marcosmoreiradev.docupodcaststudio.media.api.ExecutionContext context) { calls[1]++; return true; }
        };
        var engine = new ComfyUiImageEngine(resources(), runtime);
        var request = new ImageGenerationRequest("tree", "", 512, 512, List.of(),
                temp.resolve("out"), "test", ComfyUiImageEngine.DRAFT, 42, 1);
        IOException failure = assertThrows(IOException.class, () -> engine.generate(request, null));
        assertEquals(1, calls[0]);
        assertEquals(1, calls[1]);
        assertEquals(1, failure.getSuppressed().length, "Preserve the first engine failure");
    }

    @Test void cancellationPreventsRuntimeStartAndRecovery() throws Exception {
        var runtime = new ComfyUiImageEngine.ManagedRuntime() {
            public void ensureReady(com.marcosmoreiradev.docupodcaststudio.media.api.ExecutionContext context) { throw new AssertionError(); }
            public boolean recover(com.marcosmoreiradev.docupodcaststudio.media.api.ExecutionContext context) { throw new AssertionError(); }
        };
        var engine = new ComfyUiImageEngine(resources(), runtime);
        var request = new ImageGenerationRequest("tree", "", 512, 512, List.of(),
                temp.resolve("out"), "test", ComfyUiImageEngine.DRAFT, 42, 1);
        var context = new com.marcosmoreiradev.docupodcaststudio.media.api.ExecutionContext("cancelled", () -> true,
                (stage, value, message) -> {},
                com.marcosmoreiradev.docupodcaststudio.media.api.ExecutionPolicy.defaults(),
                com.marcosmoreiradev.docupodcaststudio.media.api.ResourceLease.NONE);
        assertThrows(Exception.class, () -> engine.generate(request, context));
    }

    private EngineConfiguration resources() throws Exception {
        Path workflow = Files.writeString(temp.resolve("sd15.json"),
                "{\"1\":{\"class_type\":\"CheckpointLoaderSimple\",\"inputs\":{\"ckpt_name\":\"base.safetensors\"}}}");
        Path sd15 = Files.write(temp.resolve("sd15.safetensors"), new byte[] {1});
        Path flux = Files.write(temp.resolve("flux.safetensors"), new byte[] {1});
        Path clip = Files.write(temp.resolve("clip.safetensors"), new byte[] {1});
        Path t5 = Files.write(temp.resolve("t5.safetensors"), new byte[] {1});
        Path vae = Files.write(temp.resolve("vae.safetensors"), new byte[] {1});
        return new EngineConfiguration(ComfyUiImageEngine.ID, Map.of(
                "baseUrl", "http://127.0.0.1:1",
                "draftWorkflow", workflow.toString(),
                "fluxWorkflow", temp.resolve("catalog.json").toString(),
                "sd15Model", sd15.toString(),
                "dreamshaperModel", temp.resolve("dream.safetensors").toString(),
                "sdxlModel", temp.resolve("sdxl.safetensors").toString(),
                "fluxModel", flux.toString(),
                "fluxClipL", clip.toString(),
                "fluxT5", t5.toString(),
                "fluxVae", vae.toString()));
    }
}
