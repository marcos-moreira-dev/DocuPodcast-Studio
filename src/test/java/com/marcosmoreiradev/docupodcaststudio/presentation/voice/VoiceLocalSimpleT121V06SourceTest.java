package com.marcosmoreiradev.docupodcaststudio.presentation.voice;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class VoiceLocalSimpleT121V06SourceTest {
    @Test
    void voiceLibraryHasMinimalLocalSimpleModeWithoutAdvancedControls() throws Exception {
        String workspace = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceLibraryWorkspaceView.java"));
        String overview = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceManageOverviewPanel.java"));
        String useCase = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/voice/GenerateVoiceTestUseCase.java"));

        assertTrue(overview.contains("La voz simple no usa muestras ni emociones."));
        assertTrue(overview.contains("Genera un WAV corto con el motor de voz simple."));
        assertTrue(workspace.contains("Voz simple lista para lectura neutral"));
        assertTrue(workspace.contains("No usa muestras humanas"));
        assertTrue(workspace.contains("No hay pasos de preparación para esta voz desde Inicio"));
        assertTrue(useCase.contains("engine.piperMode()"));
        assertTrue(useCase.contains("Prueba simple generada con Voz local simple"));
    }

    @Test
    void settingsEngineModeUsesFriendlyLabelsInsteadOfRawTechnicalValues() throws Exception {
        String settings = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/SettingsFormModel.java"));
        String modes = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/settings/TtsEngineModes.java"));

        assertTrue(settings.contains("engineModeCombo"));
        assertTrue(settings.contains("TtsEngineModes.safeLabel"));
        assertTrue(modes.contains("Voz local simple"));
        assertTrue(modes.contains("Voz IA avanzada"));
        assertTrue(modes.contains("Modo de prueba"));
    }

    @Test
    void presentationQuotedStringsDoNotExposeTechnicalEngineNames() throws Exception {
        Pattern quotedString = Pattern.compile("\\\"(?:[^\\\"\\\\]|\\\\.)*\\\"", Pattern.DOTALL);
        try (Stream<Path> files = Files.walk(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation"))) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                Matcher matcher = quotedString.matcher(Files.readString(file));
                while (matcher.find()) {
                    String literal = matcher.group().toLowerCase(java.util.Locale.ROOT);
                    assertFalse(literal.contains("piper"), file + " expone nombre técnico en string visible: " + matcher.group());
                    assertFalse(literal.contains("coqui"), file + " expone nombre técnico en string visible: " + matcher.group());
                    assertFalse(literal.contains("kogi"), file + " expone nombre técnico en string visible: " + matcher.group());
                    assertFalse(literal.contains("xtts"), file + " expone nombre técnico en string visible: " + matcher.group());
                }
            }
        }
    }
}
