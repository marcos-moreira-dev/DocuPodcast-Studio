package com.marcosmoreiradev.docupodcaststudio.localmedia;

import com.marcosmoreiradev.docupodcaststudio.media.api.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Opt-in exhaustive physical sample set for one registered human voice. */
final class Qwen3TtsAllEmotionsRealSmokeTest {
    private static final String COMPARISON_TEXT =
            "María abrió la puerta, miró a los demás y dijo: ha llegado el momento de comenzar.";

    @Test
    @EnabledIfSystemProperty(named = "docupodcast.qwenTts.allEmotions", matches = "true")
    void generatesEveryRegisteredToneForOneHuman() throws Exception {
        Path executable = requiredFile("docupodcast.qwenTts.executable");
        Path model = requiredFile("docupodcast.qwenTts.model");
        Path codec = requiredFile("docupodcast.qwenTts.codec");
        Path voiceRoot = requiredDirectory("docupodcast.qwenTts.voiceRoot");
        Path output = Path.of(System.getProperty("docupodcast.qwenTts.output",
                "target/qwen3-tts-all-emotions")).toAbsolutePath().normalize();
        Files.createDirectories(output);
        List<Path> references;
        try (var files = Files.list(voiceRoot)) {
            references = files.filter(Files::isRegularFile)
                    .filter(file -> file.getFileName().toString().toLowerCase(java.util.Locale.ROOT).endsWith(".wav"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .toList();
        }
        assertFalse(references.isEmpty());
        Qwen3TtsVoiceEngine engine = new Qwen3TtsVoiceEngine(
                new EngineConfiguration(Qwen3TtsVoiceEngine.ID, Map.of(
                        "executable", executable.toString(), "model", model.toString(),
                        "codec", codec.toString(), "defaultSpeaker",
                        voiceRoot.resolve("neutral.wav").toString())), () -> "auto");
        List<VoiceSynthesisUnit> units = references.stream().map(reference -> {
            String filename = reference.getFileName().toString();
            String tone = filename.substring(0, filename.length() - 4);
            return new VoiceSynthesisUnit(tone, COMPARISON_TEXT, voiceRoot.getFileName().toString(),
                    tone, reference, output.resolve(tone + ".wav"), Map.of("registeredTone", tone));
        }).toList();
        ExecutionContext context = new ExecutionContext("qwen3-tts-all-emotions", CancellationToken.NONE,
                (stage, amount, message) -> System.out.println("QWEN_TTS_PROGRESS " + stage + " "
                        + Math.round(amount * 100) + "% " + message),
                new ExecutionPolicy(Duration.ofMinutes(4), 1), ResourceLease.NONE);

        VoiceSynthesisBatchResult result = engine.synthesizeBatch(
                new VoiceSynthesisBatchRequest(units, "es", Map.of()), context);

        assertEquals(references.size(), result.units().size());
        for (VoiceSynthesisUnit unit : units) {
            assertTrue(Files.size(unit.outputFile()) > 44, unit.id());
            try (AudioInputStream wav = AudioSystem.getAudioInputStream(unit.outputFile().toFile())) {
                assertEquals(24_000.0f, wav.getFormat().getSampleRate(), unit.id());
                assertEquals(16, wav.getFormat().getSampleSizeInBits(), unit.id());
                assertEquals(1, wav.getFormat().getChannels(), unit.id());
            }
        }
    }

    private static Path requiredFile(String key) {
        Path path = required(key);
        if (!Files.isRegularFile(path)) throw new IllegalArgumentException("Missing local file " + path);
        return path;
    }

    private static Path requiredDirectory(String key) {
        Path path = required(key);
        if (!Files.isDirectory(path)) throw new IllegalArgumentException("Missing local directory " + path);
        return path;
    }

    private static Path required(String key) {
        String value = System.getProperty(key, "").strip();
        if (value.isBlank()) throw new IllegalArgumentException("Missing system property " + key);
        return Path.of(value).toAbsolutePath().normalize();
    }
}
