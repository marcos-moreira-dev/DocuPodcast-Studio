package com.marcosmoreiradev.docupodcaststudio.presentation.document;

import com.marcosmoreiradev.docupodcaststudio.application.voice.VoiceEngineCapabilityProfile;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.OfficialAdvancedVoicePresetCatalog;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceLibrary;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceProfile;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DocumentAudioNarrationPanelTest {
    @Test
    void xttsDocumentAcceptsDefaultNarratorAndPredesignedAdvancedNeutral() {
        VoiceLibrary library = VoiceLibrary.defaults();
        VoiceEngineCapabilityProfile xtts = VoiceEngineCapabilityProfile.coquiXtts(true, "Voz IA avanzada");
        VoiceProfile narrator = library.voiceById("VOC-NARRATOR").orElseThrow();
        VoiceProfile advancedPreset = library.voiceById(OfficialAdvancedVoicePresetCatalog.PRIMARY_PRESET_ID).orElseThrow();

        assertTrue(DocumentAudioNarrationPanel.usesDefaultEngineVoice(narrator));
        assertTrue(DocumentAudioNarrationPanel.usableInDocumentForEngine(library, narrator, xtts));
        assertTrue(DocumentAudioNarrationPanel.usableInDocumentForEngine(library, advancedPreset, xtts));
    }

    @Test
    void documentPanelUsesSharedVoiceServicesInsteadOfLocalPolicy() throws Exception {
        String panel = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentAudioNarrationPanel.java"));

        assertFalse(panel.contains("new VoiceCapabilityPolicy()"));
        assertTrue(panel.contains("buildVoiceAssignmentOptions()"));
        assertTrue(panel.contains("voiceCapabilityPolicy()"));
        assertTrue(panel.contains("VoiceAssignmentOption::selectable"));
    }
}
