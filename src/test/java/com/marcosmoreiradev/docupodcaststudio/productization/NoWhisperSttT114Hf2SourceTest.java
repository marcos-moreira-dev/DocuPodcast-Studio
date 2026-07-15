package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class NoWhisperSttT114Hf2SourceTest {
    @Test
    void productMainSourcesDoNotKeepWhisperOrSpeechToText() throws Exception {
        List<Path> files;
        try (var stream = Files.walk(Path.of("src/main/java"))) {
            files = stream
                    .filter(path -> path.toString().endsWith(".java"))
                    .toList();
        }
        String main = new StringBuilder()
                .append(readIfExists(Path.of("src/main/java/module-info.java")))
                .append("\n")
                .append(readAll(files))
                .toString();

        assertFalse(main.contains("Whisper"));
        assertFalse(main.contains("SpeechToText"));
        assertFalse(main.contains("SPEECH_TO_TEXT"));
        assertFalse(main.contains("allowGpuForStt"));
        assertFalse(Files.exists(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/stt")));
        assertFalse(Files.exists(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/stt")));
        assertFalse(Files.exists(Path.of("scripts/stt")));
    }

    @Test
    void modelSetupRecommendsOnlyVoiceAndMediaEngines() throws Exception {
        String contract = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/modelsetup/ModelFolderContract.java"));
        assertTrue(contract.contains("xttsHighQuality"));
        assertTrue(contract.contains("piperLightweight"));
        assertFalse(contract.contains("whisperLocal"));
        assertFalse(contract.contains("models/stt"));
    }

    private static String readAll(List<Path> files) throws Exception {
        StringBuilder builder = new StringBuilder();
        for (Path file : files) {
            builder.append(Files.readString(file)).append("\n");
        }
        return builder.toString();
    }

    private static String readIfExists(Path path) throws Exception {
        return Files.exists(path) ? Files.readString(path) : "";
    }
}
