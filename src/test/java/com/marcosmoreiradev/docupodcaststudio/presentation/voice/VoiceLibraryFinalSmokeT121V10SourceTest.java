package com.marcosmoreiradev.docupodcaststudio.presentation.voice;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** T121-V10: final guardrails for the Voice Library product surface. */
final class VoiceLibraryFinalSmokeT121V10SourceTest {
    @Test
    void voiceLibraryKeepsFinalProductContractAndDoesNotAssignDocumentFragments() throws Exception {
        String view = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceLibraryWorkspaceView.java");
        String engineControls = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceEngineSettingsControls.java");
        String overview = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceManageOverviewPanel.java");
        String editor = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceProfileSampleEditorPanel.java");
        String source = (view + "\n" + engineControls + "\n" + overview + "\n" + editor).toLowerCase(Locale.ROOT);

        assertTrue(view.contains("Biblioteca de voces"));
        assertTrue(view.contains("Muestras de voz por emoción"));
        assertTrue(overview.contains("Prueba de voz"));
        assertTrue(editor.contains("Referencia sonora por emoción"));
        assertTrue(source.contains("voz ia avanzada"));
        assertTrue(source.contains("voz local simple"));
        assertTrue(engineControls.contains("Modo de prueba"));
        assertFalse(source.contains("asignar al segmento"));
        assertFalse(source.contains("fragmento seleccionado"));
        assertFalse(Pattern.compile("\\bpersonajes\\b").matcher(source).find());
        assertFalse(Pattern.compile("\\broles\\b").matcher(source).find());
        assertFalse(view.contains("PerformanceStyle"));
    }

    @Test
    void voiceLibraryUsesFinalComponentsCssAndTransversalActions() throws Exception {
        String workspace = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceLibraryWorkspaceView.java");
        String profile = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceProfileCard.java");
        String engine = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceEngineModeCard.java");
        String sampleRow = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceSampleRow.java");
        String generated = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceGeneratedTestPanel.java");
        String editor = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceProfileSampleEditorPanel.java");
        String css = read("src/main/resources/css/voice-library.css");

        assertTrue(workspace.contains("new VoiceProfileSampleEditorPanel"));
        assertTrue(workspace.contains("voice-engine-test-card"));
        assertTrue(workspace.contains("new VoiceSampleRow"));
        assertTrue(workspace.contains("new VoiceGeneratedTestPanel"));
        assertTrue(workspace.contains("VoiceActionStrip.of"));
        assertTrue(workspace.contains("ActionButtonFactory.primary"));
        assertTrue(workspace.contains("ActionButtonFactory.secondary"));
        assertTrue(profile.contains("voice-profile-card"));
        assertTrue(engine.contains("voice-engine-mode-card"));
        assertTrue(sampleRow.contains("voice-sample-row"));
        assertTrue(generated.contains("voice-generated-test-panel"));
        assertTrue(css.contains(".voice-profile-card"));
        assertTrue(css.contains(".voice-engine-test-card"));
        assertTrue(css.contains(".voice-engine-mode-card"));
        assertTrue(css.contains(".voice-sample-row"));
        assertTrue(css.contains(".voice-generated-test-panel"));
        assertTrue(css.contains(".voice-profile-sample-editor"));
        assertFalse(workspace.contains("setStyle("));
        assertFalse(profile.contains("setStyle("));
        assertFalse(engine.contains("setStyle("));
        assertFalse(sampleRow.contains("setStyle("));
        assertFalse(generated.contains("setStyle("));
        assertFalse(editor.contains("setStyle("));
    }

    @Test
    void normalVoiceUiDoesNotExposeTechnicalEngineNamesInStringLiterals() throws Exception {
        String visibleVoiceUi = String.join("\n",
                read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceLibraryWorkspaceView.java"),
                read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceProfileCard.java"),
                read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceEngineModeCard.java"),
                read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceSampleRow.java"),
                read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceGeneratedTestPanel.java"),
                read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceProfileSampleEditorPanel.java"));

        var matcher = Pattern.compile("\\\"(?:[^\\\"\\\\]|\\\\.)*\\\"", Pattern.DOTALL).matcher(visibleVoiceUi);
        while (matcher.find()) {
            String literal = matcher.group().toLowerCase(Locale.ROOT);
            assertFalse(literal.contains("coqui"));
            assertFalse(literal.contains("kogi"));
            assertFalse(literal.contains("xtts"));
            assertFalse(literal.contains("piper"));
        }
    }

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path));
    }
}
