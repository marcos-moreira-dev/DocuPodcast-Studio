package com.marcosmoreiradev.docupodcaststudio.presentation.voice;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class VoiceComponentsCssT121V09SourceTest {
    @Test
    void voiceWorkspaceUsesDedicatedComponentsInsteadOfAdHocRowsAndCards() throws Exception {
        String workspace = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceLibraryWorkspaceView.java"));
        String profileCard = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceProfileCard.java"));
        String engineCard = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceEngineModeCard.java"));

        assertTrue(workspace.contains("new VoiceSampleRow"));
        assertTrue(workspace.contains("new VoiceGeneratedTestPanel"));
        assertTrue(workspace.contains("new VoiceProfileSampleEditorPanel"));
        assertTrue(workspace.contains("VoiceActionStrip.of"));
        assertTrue((profileCard + engineCard).contains("new InfoBadge"));
        assertTrue(workspace.contains("voice-engine-test-card"));
        assertFalse(workspace.contains("HBox actions = new HBox(8)"));
        assertFalse(workspace.contains("setStyle("));
    }

    @Test
    void voiceComponentsExistAndReuseTransversalBuildingBlocks() throws Exception {
        String profileCard = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceProfileCard.java"));
        String engineCard = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceEngineModeCard.java"));
        String toneBadge = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceToneBadge.java"));
        String sampleRow = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceSampleRow.java"));
        String generatedPanel = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceGeneratedTestPanel.java"));
        String editorPanel = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceProfileSampleEditorPanel.java"));

        assertTrue(profileCard.contains("InfoBadge"));
        assertTrue(engineCard.contains("InfoBadge"));
        assertTrue(toneBadge.contains("AppStyles.UI_INFO_BADGE"));
        assertTrue(sampleRow.contains("VoiceToneBadge"));
        assertTrue(sampleRow.contains("voice-tone-sample-title"));
        assertTrue(sampleRow.contains("Region.USE_PREF_SIZE"));
        assertTrue(generatedPanel.contains("SectionHeader"));
        assertTrue(generatedPanel.contains("VoiceActionStrip"));
        assertTrue(editorPanel.contains("VoiceActionStrip"));
    }

    @Test
    void voiceCssAndComponentCatalogKnowTheNewVoiceComponents() throws Exception {
        String css = Files.readString(Path.of("src/main/resources/css/voice-library.css"));
        String catalog = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/components/GuiComponentCatalog.java"));

        assertTrue(css.contains(".voice-engine-test-card"));
        assertTrue(css.contains(".voice-engine-mode-card"));
        assertTrue(css.contains(".voice-generated-test-panel"));
        assertTrue(css.contains(".voice-profile-sample-editor"));
        assertTrue(css.contains(".voice-tone-badge-optional"));
        assertTrue(css.contains(".voice-tone-sample-detail"));
        assertTrue(css.contains(".voice-tone-sample-title"));
        assertTrue(css.contains(".voice-profile-card"));
        assertTrue(css.contains(".voice-sample-row"));
        assertTrue(catalog.contains("VoiceProfileCard"));
        assertTrue(catalog.contains("VoiceEngineModeCard"));
        assertTrue(catalog.contains("VoiceToneBadge"));
        assertTrue(catalog.contains("VoiceSampleRow"));
        assertTrue(catalog.contains("VoiceGeneratedTestPanel"));
        assertTrue(catalog.contains("VoiceProfileSampleEditorPanel"));
    }

    @Test
    void voicePresentationStillUsesFriendlyEngineLabelsOnly() throws Exception {
        String workspace = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceLibraryWorkspaceView.java"));
        String engineCard = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceEngineModeCard.java"));
        String engineControls = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/voice/VoiceEngineSettingsControls.java"));
        String visibleSource = workspace + "\n" + engineCard + "\n" + engineControls;

        assertTrue(visibleSource.contains("Voz IA avanzada"));
        assertTrue(visibleSource.contains("Voz local simple"));
        assertTrue(visibleSource.contains("Modo de prueba"));
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("\\\"(?:[^\\\"\\\\]|\\\\.)*\\\"", java.util.regex.Pattern.DOTALL)
                .matcher(visibleSource);
        while (matcher.find()) {
            String literal = matcher.group().toLowerCase(java.util.Locale.ROOT);
            assertFalse(literal.contains("coqui"));
            assertFalse(literal.contains("kogi"));
            assertFalse(literal.contains("xtts"));
            assertFalse(literal.contains("piper"));
        }
    }
}
