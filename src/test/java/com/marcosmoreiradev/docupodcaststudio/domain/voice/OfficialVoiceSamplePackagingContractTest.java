package com.marcosmoreiradev.docupodcaststudio.domain.voice;

import com.marcosmoreiradev.docupodcaststudio.application.runtime.ApplicationRuntimeRoots;
import com.marcosmoreiradev.docupodcaststudio.application.voice.VoiceReferenceSamplePathResolver;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class OfficialVoiceSamplePackagingContractTest {

    @Test
    void everyAdvertisedOfficialPresetHasAllThirtyEightToneSamplesInTheInstallation() throws Exception {
        ApplicationRuntimeRoots roots = ApplicationRuntimeRoots.configured();
        VoiceReferenceSamplePathResolver resolver = new VoiceReferenceSamplePathResolver(
                roots.installationRoot(),
                roots.runtimeRoot()
        );
        Path disposableProjectRoot = roots.runtimeRoot().resolve("target").resolve("voice-contract-project");

        for (VoiceReferenceSampleSet sampleSet : OfficialAdvancedVoicePresetCatalog.sampleSets()) {
            // JOYFUL is a semantic alias of the existing HAPPY recording in the
            // current v1 catalogue; the product advertises 38 physical tones.
            assertEquals(38, sampleSet.samples().size());
            for (VoiceReferenceSample sample : sampleSet.samples()) {
                Path resolved = resolver.resolve(disposableProjectRoot, sample, "muestra oficial");
                assertTrue(Files.isRegularFile(resolved),
                        () -> "Falta la muestra oficial anunciada: " + resolved);
            }
        }
    }
}
