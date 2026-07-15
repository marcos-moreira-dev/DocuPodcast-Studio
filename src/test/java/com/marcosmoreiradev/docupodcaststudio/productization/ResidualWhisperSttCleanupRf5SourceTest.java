package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ResidualWhisperSttCleanupRf5SourceTest {
    @Test
    void mainSourcesAndOperationalScriptsDoNotKeepSpeechToTextResidues() throws Exception {
        String productSourcesAndScripts = readTree(Path.of("src/main/java")) + "\n" + readTree(Path.of("scripts"));

        assertFalse(productSourcesAndScripts.contains("Whisper"));
        assertFalse(productSourcesAndScripts.contains("SpeechToText"));
        assertFalse(productSourcesAndScripts.contains("SPEECH_TO_TEXT"));
        assertFalse(productSourcesAndScripts.contains("allowGpuForStt"));
        assertFalse(productSourcesAndScripts.contains("audio a texto"));
        assertFalse(productSourcesAndScripts.contains("HUMAN_RECORDING_FOR_TRANSCRIPTION"));
        assertFalse(Files.exists(Path.of("scripts/stt")));
        assertFalse(Files.exists(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/stt")));
        assertFalse(Files.exists(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/stt")));
    }

    @Test
    void voiceSourceKindKeepsHumanAudioAsClipNotTranscriptionSource() throws Exception {
        String kind = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/domain/voice/VoiceSourceKind.java"));
        String span = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/domain/voice/PerformanceSpan.java"));

        assertTrue(kind.contains("HUMAN_RECORDING(\"Audio del computador\")"));
        assertFalse(kind.contains("TRANSCRIPTION"));
        assertFalse(kind.contains("audio a texto"));
        assertFalse(span.contains("HUMAN_RECORDING_FOR_TRANSCRIPTION"));
        assertTrue(span.contains("voiceSourceKind == VoiceSourceKind.HUMAN_RECORDING"));
    }

    @Test
    void rf5DocumentationRecordsTheBoundaryWithoutReopeningFeature() throws Exception {
        String doc = Files.readString(Path.of("docs/productizacion/RF5_LIMPIEZA_RESIDUAL_STT_WHISPER.md"));
        String readme = Files.readString(Path.of("README.md"));
        String handoff = Files.readString(Path.of("AI_HANDOFF.md"));
        String validation = Files.readString(Path.of("VALIDATION.md"));

        assertTrue(doc.contains("HUMAN_RECORDING_FOR_TRANSCRIPTION"));
        assertTrue(doc.contains("audio del computador asociado al documento"));
        assertTrue(readme.contains("RF5 — Limpieza residual STT/Whisper"));
        assertTrue(handoff.contains("Base vigente: RF5"));
        assertTrue(validation.contains("Validación RF5"));
    }

    private static String readTree(Path root) throws Exception {
        if (!Files.exists(root)) {
            return "";
        }
        List<Path> files;
        try (var stream = Files.walk(root)) {
            files = stream
                    .filter(Files::isRegularFile)
                    .filter(path -> !path.toString().contains("target"))
                    .toList();
        }
        StringBuilder builder = new StringBuilder();
        for (Path file : files) {
            try {
                builder.append(Files.readString(file)).append('\n');
            } catch (java.nio.charset.MalformedInputException ignored) {
                // Some prepared runtime helpers can be binary; source guardrails only inspect text files.
            }
        }
        return builder.toString();
    }
}
