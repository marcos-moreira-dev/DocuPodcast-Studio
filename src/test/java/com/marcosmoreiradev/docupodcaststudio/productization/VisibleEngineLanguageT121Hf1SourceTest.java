package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** T121-HF1: normal UX uses friendly engine/visual names, while technical identifiers stay internal. */
final class VisibleEngineLanguageT121Hf1SourceTest {
    @Test
    void welcomeDocumentAndSettingsUseFriendlyVoiceEngineNames() throws Exception {
        String welcome = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/welcome/WelcomeWorkspaceView.java");
        String documentAudio = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentAudioNarrationPanel.java");
        String settings = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/settings/SettingsDialog.java");

        assertTrue(welcome.contains("Configuración inicial"));
        assertTrue(welcome.contains("Voz local simple para empezar"));
        assertFalse(welcome.contains("Coqui/XTTS"));
        assertFalse(welcome.contains("Piper"));

        assertTrue(documentAudio.contains("No muestra tonos por muestra humana"));
        assertTrue(documentAudio.contains("para tonos grabados usa Voz IA avanzada"));
        assertFalse(documentAudio.contains("Narrador Piper"));
        assertFalse(documentAudio.contains("Prepara Coqui/XTTS o Piper"));

        assertTrue(settings.contains("Voz IA avanzada"));
        assertTrue(settings.contains("Voz IA avanzada") && settings.contains("Verificar"));
        assertTrue(settings.contains("Voz local simple"));
        assertTrue(settings.contains("Voz local simple") && settings.contains("Verificar"));
        assertFalse(settings.contains("Asistente Coqui/XTTS"));
        assertFalse(settings.contains("Verificar Coqui/XTTS"));
        assertFalse(settings.contains("Asistente Piper"));
        assertFalse(settings.contains("Verificar Piper"));
    }

    @Test
    void helpAndDocumentRailUseVisualesInsteadOfStoryboardAsNormalLanguage() throws Exception {
        String rail = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentMediaRailView.java");
        String audioHelp = read("src/main/resources/help/topics/audio-generation.md");
        String visualHelp = read("src/main/resources/help/topics/storyboard-live.md");
        String exporting = read("src/main/resources/help/topics/exporting.md");
        String glossary = read("src/main/resources/help/topics/glossary.md");

        assertTrue(rail.contains("railTitle(\"Fragmentos visuales\")"));
        assertTrue(audioHelp.contains("Voz IA avanzada"));
        assertTrue(audioHelp.contains("Voz local simple"));
        assertFalse(audioHelp.contains("Coqui/XTTS"));
        assertFalse(audioHelp.contains("Piper:"));
        assertTrue(visualHelp.startsWith("# Visuales y secuencia visual"));
        assertFalse(visualHelp.contains("# Storyboard"));
        assertTrue(exporting.contains("Video MP4 final"));
        assertFalse(exporting.contains("Storyboard/video"));
        assertTrue(glossary.contains("## Asociación visual"));
        assertFalse(glossary.contains("## Storyboard Binding"));
    }

    @Test
    void engineCapabilityMessagesStayFriendly() throws Exception {
        String profile = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/voice/VoiceEngineCapabilityProfile.java");
        String policy = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/voice/VoiceCapabilityPolicy.java");

        assertTrue(profile.contains("Voz IA avanzada activa"));
        assertTrue(profile.contains("Voz local simple activa"));
        assertTrue(profile.contains("friendlyAdvancedLabel"));
        assertTrue(policy.contains("Voz IA avanzada puede usar voces importadas"));
        assertTrue(policy.contains("La Voz local simple usa modelos .onnx"));
        assertFalse(policy.contains("Coqui/XTTS puede usar voces importadas"));
        assertFalse(policy.contains("Piper usa modelos .onnx"));
    }

    private static String read(String path) throws Exception {
        Path file = Path.of(path);
        assertTrue(Files.exists(file), path + " debe existir");
        return Files.readString(file);
    }
}
