package com.marcosmoreiradev.docupodcaststudio.launcher;

import com.marcosmoreiradev.docupodcaststudio.localmedia.LocalMediaAdapters;
import com.marcosmoreiradev.docupodcaststudio.localmedia.LocalMediaLayout;
import com.marcosmoreiradev.docupodcaststudio.media.api.EngineDescriptor;
import com.marcosmoreiradev.docupodcaststudio.media.api.MediaEnginePlatform;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class ProductionVoiceCompositionTest {
    @TempDir
    Path installationRoot;

    @Test
    void productiveCompositionRegistersPiperXttsAndQwenExactlyOnce() {
        try (MediaEnginePlatform platform = LocalMediaAdapters.create(
                new LocalMediaLayout(installationRoot, installationRoot))) {
            List<EngineDescriptor> voices = platform.voiceEngines().descriptors();

            assertEquals(List.of("piper", "xtts", "qwen3-tts-local"),
                    voices.stream().map(descriptor -> descriptor.id().value()).toList());
            assertEquals(List.of("Voz local simple", "Voz IA avanzada",
                            "Qwen3-TTS local · 1.7B Q8"),
                    voices.stream().map(EngineDescriptor::displayName).toList());
        }
    }
}
