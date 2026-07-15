package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DocumentVoiceToneFallbackT121V07SourceTest {
    @Test
    void documentAudioPanelUsesVoiceReferenceTonesInsteadOfLegacyStyles() throws Exception {
        String panel = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentAudioNarrationPanel.java"));
        String viewModel = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java"));

        assertTrue(panel.contains("ComboBox<VoiceReferenceTone> toneSelector"));
        assertTrue(panel.contains("label(\"Tono\")"));
        assertTrue(panel.contains("documentVoiceToneStatusProperty"));
        assertTrue(panel.contains("assignVoiceToneToSelectedDocumentRange"));
        assertTrue(panel.contains("assignVoiceToSelectedDocumentRange(voice.id())"));
        assertFalse(panel.contains("ComboBox<PerformanceStyle>"));
        assertFalse(panel.contains("Emoción / estilo"));
        assertTrue(viewModel.contains("VoiceToneReferenceResolution"));
        assertTrue(viewModel.contains("se usará Neutral"));
    }

    @Test
    void documentToneAssignmentIsStoredAsProjectLayerTarget() throws Exception {
        String tone = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/domain/voice/VoiceReferenceTone.java"));
        String resolver = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/NarrativeLayerTargetResolver.java"));
        String integrity = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/project/InspectProjectIntegrityUseCase.java"));

        assertTrue(tone.contains("layerTargetId"));
        assertTrue(tone.contains("fromLayerTargetId"));
        assertTrue(resolver.contains("VoiceReferenceTone.fromLayerTargetId"));
        assertTrue(resolver.contains("Tono de referencia elegido en Documento"));
        assertTrue(integrity.contains("VoiceReferenceTone.fromLayerTargetId"));
        assertTrue(integrity.contains("estilo o tono inexistente"));
    }
}
