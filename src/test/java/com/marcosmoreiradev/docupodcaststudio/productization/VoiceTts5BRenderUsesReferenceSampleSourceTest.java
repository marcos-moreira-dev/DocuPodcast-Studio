package com.marcosmoreiradev.docupodcaststudio.productization;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** VOZ-TTS5B: generated document audio must use the selected voice/tone reference sample when available. */
final class VoiceTts5BRenderUsesReferenceSampleSourceTest {
    @Test
    void audioRequestCarriesVoiceLibraryAndResolvesToneReferenceSample() throws Exception {
        String request = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/audio/AudioGenerationRequest.java");
        String resolver = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/application/voice/VoiceReferenceSamplePathResolver.java");

        assertTrue(request.contains("VoiceLibrary voiceLibrary"));
        assertTrue(request.contains("referenceSamplePathFor(AudioGenerationUnit unit)"));
        assertTrue(request.contains("VoiceReferenceTone.fromLayerTargetId(unit.performanceStyleId())"));
        assertTrue(request.contains("set.sampleFor(requestedTone).or(set::neutralSample)"));
        assertTrue(request.contains("VoiceReferenceSamplePathResolver.fromCurrentApplicationRoot()"));
        assertTrue(resolver.contains("La \" + label + \" apunta fuera de la carpeta del proyecto"));
        assertTrue(resolver.contains("La \" + label + \" embebida apunta fuera de la carpeta de la app"));
    }

    @Test
    void shellPassesProjectVoiceLibraryToAudioGenerationRequest() throws Exception {
        String shell = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/DocuPodcastShellViewModel.java");
        String coordinator = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/presentation/shell/workflow/AudioWorkflowCoordinator.java");

        assertTrue(shell.contains("session.project().voiceLibrary()"));
        assertTrue(coordinator.contains("VoiceLibrary voiceLibrary"));
        assertTrue(coordinator.contains("new AudioGenerationRequest(script, renderUnitPlan, projectDirectory, jobName, \"es\", configuredVoiceProfileId(), voiceLibrary)"));
    }

    @Test
    void localTtsGatewayPassesResolvedReferenceSampleToProcessCommand() throws Exception {
        String gateway = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/audio/LocalTtsProcessAudioGenerationGateway.java");
        String config = read("src/main/java/com/marcosmoreiradev/docupodcaststudio/infrastructure/audio/LocalTtsProcessConfiguration.java");

        assertTrue(gateway.contains("request.referenceSamplePathFor(segment).orElse(null)"));
        assertTrue(gateway.contains("tts_reference_sample"));
        assertTrue(gateway.contains("request.language(), segment.effectiveVoiceProfileId(request.voiceProfileId()), referenceSample"));
        assertTrue(config.contains("public List<String> commandFor(String segmentId, Path textFile, Path outputFile, String requestLanguage,"));
        assertTrue(config.contains(".replace(\"{speakerWav}\", speakerWav)"));
        assertTrue(config.contains("overrideSpeakerArgument(tokens, speaker)"));
    }

    @Test
    void documentationRegistersVoiceTts5B() throws Exception {
        String current = read("DOCUMENTACION_ACTUAL/01_REGISTRO_TANDAS_RECIENTES.md");
        String validation = read("VALIDATION.md");

        assertTrue(current.contains("VOZ-TTS5B"));
        assertTrue(validation.contains("VoiceTts5BRenderUsesReferenceSampleSourceTest"));
        assertFalse(current.contains("TTS5B pendiente de implementar"));
    }

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path));
    }
}
