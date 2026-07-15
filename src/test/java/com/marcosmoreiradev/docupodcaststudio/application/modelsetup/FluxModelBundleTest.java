package com.marcosmoreiradev.docupodcaststudio.application.modelsetup;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class FluxModelBundleTest {
    @TempDir Path tempDir;

    @Test
    void resolvesBf16T5AndAllRequiredComponents() throws Exception {
        Path base = tempDir.resolve("models/image");
        Files.createDirectories(base.resolve("vae"));
        Files.createDirectories(base.resolve("text_encoders"));
        Files.writeString(base.resolve("flux1-dev.safetensors"), "model");
        Files.writeString(base.resolve("vae/ae.safetensors"), "vae");
        Files.writeString(base.resolve("text_encoders/clip_l.safetensors"), "clip");
        Files.writeString(base.resolve("text_encoders/t5xxl_bf16.safetensors"), "t5");

        FluxModelBundle bundle = FluxModelBundle.inspect(tempDir);

        assertTrue(bundle.ready());
        assertEquals("t5xxl_bf16.safetensors", bundle.t5Name());
    }

    @Test
    void reportsComponentsSeparately() {
        FluxModelBundle bundle = FluxModelBundle.inspect(tempDir);

        assertFalse(bundle.ready());
        assertTrue(bundle.missingComponents().contains("flux1-dev.safetensors"));
        assertTrue(bundle.missingComponents().stream().anyMatch(value -> value.contains("T5XXL")));
    }
}
