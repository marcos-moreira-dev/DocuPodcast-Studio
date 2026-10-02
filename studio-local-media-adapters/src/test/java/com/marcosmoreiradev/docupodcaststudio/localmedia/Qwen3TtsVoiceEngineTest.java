package com.marcosmoreiradev.docupodcaststudio.localmedia;

import com.marcosmoreiradev.docupodcaststudio.media.api.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import static org.junit.jupiter.api.Assertions.*;

final class Qwen3TtsVoiceEngineTest {
    @TempDir Path temporary;

    @Test void residentRequestStagingDoesNotInheritADeepOutputPath() throws Exception {
        Path staging = Qwen3TtsVoiceEngine.createStagingDirectory();
        try {
            assertTrue(Files.isDirectory(staging));
            assertTrue(staging.startsWith(Path.of(System.getProperty("java.io.tmpdir"))));
            assertTrue(staging.resolve("worker-request.txt").toString().length() < 260);
        } finally {
            Files.deleteIfExists(staging);
        }
    }

    @Test
    @EnabledIfSystemProperty(named = "docupodcast.qwen.residentSmokeRoot", matches = ".+")
    void realResidentBatchLoadsOnceAndProcessesBothUnits() throws Exception {
        Path root = Path.of(System.getProperty("docupodcast.qwen.residentSmokeRoot"));
        Path speaker = root.resolve("samples/voices/advanced-presets/hombre_20_idealista_ecuador_dialogo/neutral.wav");
        Qwen3TtsVoiceEngine engine = new Qwen3TtsVoiceEngine(configuration(
                root.resolve("tools/qwen3-tts/llama.cpp/llama-tts.exe"),
                root.resolve("models/tts/qwen3-tts/Qwen3-TTS-12Hz-1.7B-Base-Q8_0.gguf"),
                root.resolve("models/tts/qwen3-tts/mmproj-Qwen3-TTS-12Hz-1.7B-Base-Q8_0.gguf"), speaker), () -> "auto");
        AtomicInteger loads = new AtomicInteger();
        ExecutionContext context = new ExecutionContext("resident-smoke", null,
                (phase, progress, message) -> { if (phase.equals("voice-model-loading")) loads.incrementAndGet(); },
                null, null);
        var result = engine.synthesizeBatch(new VoiceSynthesisBatchRequest(List.of(
                new VoiceSynthesisUnit("one", "El capitán saluda.", "", "neutral", speaker,
                        temporary.resolve("primera.wav"), Map.of()),
                new VoiceSynthesisUnit("two", "La señora abre la puerta.", "", "neutral", speaker,
                        temporary.resolve("segunda.wav"), Map.of())), "es", Map.of()), context);
        assertEquals("resident-local-worker", result.diagnostics().get("mode"));
        assertEquals(1, loads.get());
        assertEquals(2, result.units().size());
        assertTrue(Files.size(temporary.resolve("primera.wav")) > 44);
        assertTrue(Files.size(temporary.resolve("segunda.wav")) > 44);
    }

    @Test void declaresMeasuredQ8GpuRamDemandWithoutIndependentScheduler() throws Exception {
        Qwen3TtsVoiceEngine engine = engineWithFiles();

        ComputeResourceDemand demand = engine.resourceDemand(ComputePreference.automatic());

        assertEquals(1, demand.units().get(ResourceId.CPU_HEAVY));
        assertEquals(2_570L * 1024L * 1024L, demand.modelResidency().vramBytes());
        assertTrue(demand.hostMemoryOffloadAllowed());
        assertTrue(engine.descriptor().supports(EngineFeature.REFERENCE_VOICE));
        assertTrue(engine.descriptor().supports(EngineFeature.EXPRESSIVE_STYLE));
    }

    @Test void readinessRejectsAnythingHeavierThanQ8Policy() throws Exception {
        Path executable = file("llama-tts.exe");
        Path model = file("Qwen3-TTS-12Hz-1.7B-Base-BF16.gguf");
        Path codec = file("mmproj-Qwen3-TTS-12Hz-1.7B-Base-Q8_0.gguf");
        Path speaker = file("speaker.wav");
        Qwen3TtsVoiceEngine engine = new Qwen3TtsVoiceEngine(configuration(
                executable, model, codec, speaker), () -> "auto");

        EngineReadiness readiness = engine.inspectReadiness(null);

        assertFalse(readiness.ready());
        assertTrue(readiness.summary().contains("Q8"));
    }

    private Qwen3TtsVoiceEngine engineWithFiles() throws Exception {
        return new Qwen3TtsVoiceEngine(configuration(
                file("llama-tts.exe"),
                file("Qwen3-TTS-12Hz-1.7B-Base-Q8_0.gguf"),
                file("mmproj-Qwen3-TTS-12Hz-1.7B-Base-Q8_0.gguf"),
                file("speaker.wav")), () -> "auto");
    }

    private EngineConfiguration configuration(Path executable, Path model, Path codec, Path speaker) {
        return new EngineConfiguration(Qwen3TtsVoiceEngine.ID, Map.of(
                "executable", executable.toString(),
                "model", model.toString(),
                "codec", codec.toString(),
                "defaultSpeaker", speaker.toString()));
    }

    private Path file(String name) throws Exception {
        Path target = temporary.resolve(name);
        Files.write(target, new byte[]{1});
        return target;
    }
}
