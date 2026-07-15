package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class VoiceToneCatalogT121V02SourceTest {
    @Test
    void voiceToneCatalogIncludesTheatricalPromptsAndSafeUxNames() throws Exception {
        String toneSource = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/domain/voice/VoiceReferenceTone.java"));
        String engineTargetSource = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/domain/voice/VoiceEngineTarget.java"));

        assertTrue(toneSource.contains("BORED"));
        assertTrue(toneSource.contains("HEROIC"));
        assertTrue(toneSource.contains("SEDUCTIVE_NON_EXPLICIT"));
        assertTrue(toneSource.contains("suggestedRecordingPrompt"));
        assertTrue(toneSource.contains("Hoy leeré este texto con claridad"));
        assertTrue(engineTargetSource.contains("Voz IA avanzada"));
        assertTrue(engineTargetSource.contains("Voz local simple"));
        assertFalse(engineTargetSource.contains("Coqui"));
        assertFalse(engineTargetSource.contains("XTTS"));
    }

    @Test
    void voiceSampleModelProtectsManagedOwnershipAndNeutralFallback() throws Exception {
        String sampleSource = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/domain/voice/VoiceReferenceSample.java"));
        String sampleSetSource = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/domain/voice/VoiceReferenceSampleSet.java"));
        String ownershipSource = Files.readString(Path.of("src/main/java/com/marcosmoreiradev/docupodcaststudio/domain/voice/VoiceFileOwnership.java"));

        assertTrue(sampleSource.contains("VoiceFileOwnership"));
        assertTrue(sampleSetSource.contains("forAdvancedVoice"));
        assertTrue(sampleSetSource.contains("sampleForOrNeutral"));
        assertTrue(sampleSetSource.contains("missingToneUsesNeutral"));
        assertTrue(ownershipSource.contains("APP_RESOURCE"));
        assertTrue(ownershipSource.contains("EXTERNAL_REFERENCE"));
        assertTrue(ownershipSource.contains("managedDeletable"));
    }
}
