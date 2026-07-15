package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** PF2D: Settings must show visible progress for long voice setup operations. */
final class AdvancedVoiceProgressPf2DSourceTest {
    @Test
    void settingsShowsProgressForLongVoiceSetupOperations() throws Exception {
        String dialog = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/SettingsDialog.java");
        String advancedVoice = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/AdvancedVoiceSettingsOperations.java");
        String operations = dialog + advancedVoice;
        String progress = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/SettingsOperationProgressCoordinator.java");
        assertTrue(operations.contains("operationProgress.show"));
        assertTrue(progress.contains("ProgressBar"));
        assertTrue(progress.contains("OperationProgress"));
        assertTrue(operations.contains("Preparando este equipo"));
        assertTrue(operations.contains("Descarga local en curso"));
        assertTrue(operations.contains("Copiando y verificando recursos"));
        assertTrue(dialog.contains("Solo se prepara cuando lo pides aquí") || dialog.contains("Configuración inicial"));
        assertTrue(progress.contains("Operación activa"));
    }

    @Test
    void presentationStringsDoNotExposeTechnicalAdvancedEngineNames() throws Exception {
        String presentation = readTree(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation"));
        assertFalse(quotedLowercaseStrings(presentation).contains("coqui"));
        assertFalse(quotedLowercaseStrings(presentation).contains("kogi"));
        assertFalse(quotedLowercaseStrings(presentation).contains("xtts"));
        assertFalse(quotedLowercaseStrings(presentation).contains("piper"));
    }

    private static String quotedLowercaseStrings(String source) {
        StringBuilder builder = new StringBuilder();
        boolean inside = false;
        boolean escaped = false;
        for (int i = 0; i < source.length(); i++) {
            char c = source.charAt(i);
            if (inside) {
                if (escaped) {
                    escaped = false;
                    continue;
                }
                if (c == '\\') {
                    escaped = true;
                    continue;
                }
                if (c == '"') {
                    inside = false;
                    builder.append('\n');
                } else {
                    builder.append(Character.toLowerCase(c));
                }
            } else if (c == '"') {
                inside = true;
            }
        }
        return builder.toString().toLowerCase(Locale.ROOT);
    }

    private static String readTree(Path root) throws Exception {
        StringBuilder builder = new StringBuilder();
        try (var stream = Files.walk(root)) {
            for (Path path : stream.filter(Files::isRegularFile).toList()) {
                builder.append(Files.readString(path)).append('\n');
            }
        }
        return builder.toString();
    }

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path));
    }
}
