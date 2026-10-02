package com.marcosmoreiradev.docupodcaststudio.localmedia;

import com.marcosmoreiradev.docupodcaststudio.media.api.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Opt-in physical smoke; never downloads and only consumes explicitly supplied local assets. */
final class Qwen3TtsRealSmokeTest {
    @Test
    @EnabledIfSystemProperty(named = "docupodcast.qwenTts.real", matches = "true")
    void generatesCanonicalEmotionalQ8Units() throws Exception {
        Path executable = required("docupodcast.qwenTts.executable");
        Path model = required("docupodcast.qwenTts.model");
        Path codec = required("docupodcast.qwenTts.codec");
        Path voiceRoot = required("docupodcast.qwenTts.voiceRoot");
        Path output = Path.of(System.getProperty("docupodcast.qwenTts.output",
                "target/qwen3-tts-real-smoke")).toAbsolutePath().normalize();
        Files.createDirectories(output);
        Qwen3TtsVoiceEngine engine = new Qwen3TtsVoiceEngine(
                new EngineConfiguration(Qwen3TtsVoiceEngine.ID, Map.of(
                        "executable", executable.toString(), "model", model.toString(),
                        "codec", codec.toString(),
                        "defaultSpeaker", voiceRoot.resolve("neutral.wav").toString())), () -> "auto");
        List<VoiceSynthesisUnit> units = List.of(
                new VoiceSynthesisUnit("happy", "¡Hoy comienza algo maravilloso!", "maria", "HAPPY",
                        voiceRoot.resolve("feliz.wav"), output.resolve("happy.wav"), Map.of()),
                new VoiceSynthesisUnit("angry", "¡Basta! Necesito que me escuches ahora.", "maria", "ANGRY",
                        voiceRoot.resolve("enojada.wav"), output.resolve("angry.wav"), Map.of()));
        ExecutionContext context = new ExecutionContext("qwen3-tts-real-smoke", CancellationToken.NONE,
                ProgressSink.NONE, new ExecutionPolicy(Duration.ofMinutes(4), 1), ResourceLease.NONE);

        VoiceSynthesisBatchResult result = engine.synthesizeBatch(
                new VoiceSynthesisBatchRequest(units, "es", Map.of()), context);

        assertEquals(2, result.units().size());
        assertEquals("Q8_0", result.units().getFirst().diagnostics().get("quantization"));
        for (VoiceSynthesisUnit unit : units) {
            assertTrue(Files.size(unit.outputFile()) > 44);
            try (AudioInputStream wav = AudioSystem.getAudioInputStream(unit.outputFile().toFile())) {
                assertEquals(24_000.0f, wav.getFormat().getSampleRate());
                assertEquals(16, wav.getFormat().getSampleSizeInBits());
                assertEquals(1, wav.getFormat().getChannels());
            }
        }
    }

    private static Path required(String key) {
        String value = System.getProperty(key, "").strip();
        if (value.isBlank()) throw new IllegalArgumentException("Missing system property " + key);
        Path path = Path.of(value).toAbsolutePath().normalize();
        if (!Files.exists(path)) throw new IllegalArgumentException("Missing local asset " + path);
        return path;
    }
}
