package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class VoiceTransversalTanda6SourceTest {
    @Test
    void sharedVoiceContractIsRegisteredAndUsedByTheatreAndDocumentPanels() throws Exception {
        String services = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/services/VoiceApplicationServices.java");
        String factory = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/bootstrap/ApplicationServicesFactory.java");
        String theatrePanel = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/theatre/TheatreCharactersPanel.java");
        String documentPanel = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/document/DocumentAudioNarrationPanel.java");

        assertTrue(services.contains("BuildVoiceAssignmentOptionsUseCase buildVoiceAssignmentOptions"));
        assertTrue(factory.contains("new BuildVoiceAssignmentOptionsUseCase(voiceCapabilityPolicy)"));
        assertTrue(theatrePanel.contains("VoiceAssignmentOption"));
        assertTrue(theatrePanel.contains("buildVoiceAssignmentOptions()"));
        assertTrue(theatrePanel.contains("VoiceRoleAlias"));
        assertFalse(theatrePanel.contains("VoiceProfile::usableForTts"));
        assertTrue(documentPanel.contains("VoiceAssignmentOption"));
        assertTrue(documentPanel.contains("buildVoiceAssignmentOptions()"));
        assertFalse(documentPanel.contains("new VoiceCapabilityPolicy()"));
    }

    @Test
    void theatreKeepsOnlyVoiceAliasesWhileProjectionResolvesThemForAudio() throws Exception {
        String projection = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/audio/BuildAudioVoiceProductionProjectionUseCase.java");
        String theatreLayer = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/domain/theatre/TheatreProjectLayer.java");

        assertTrue(theatreLayer.contains("VoiceRoleAlias"));
        assertFalse(theatreLayer.contains("VoiceCapabilityPolicy"));
        assertFalse(theatreLayer.contains("VoiceEngineType"));
        assertTrue(projection.contains("theatreVoiceId"));
        assertTrue(projection.contains("VoiceRoleAlias::voiceProfileId"));
    }

    @Test
    void tandaSixContractDocumentExists() throws Exception {
        String doc = read("DOCUMENTACION_ACTUAL/TANDA_06_VOCES_TRANSVERSALES/VOICE_TRANSVERSAL_CONTRACT.md");

        assertTrue(doc.contains("VoiceLibrary"));
        assertTrue(doc.contains("VoiceAssignmentOption"));
        assertTrue(doc.contains("VoiceRoleAlias"));
    }

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path));
    }
}
