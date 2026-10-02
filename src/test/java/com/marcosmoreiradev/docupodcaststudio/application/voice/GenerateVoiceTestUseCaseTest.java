package com.marcosmoreiradev.docupodcaststudio.application.voice;

import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioEngineDescriptor;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceEngineType;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceFileOwnership;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceLibrary;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceProfile;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceProfileType;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceQualityPreset;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceReferenceSample;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceReferenceTone;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceSampleOrigin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class GenerateVoiceTestUseCaseTest {
    private static final String TEST_ADVANCED_VOICE_ID = "VOC-TEST-ADVANCED";

    @TempDir
    Path tempDir;

    @Test
    void generatesCacheArtifactThroughRealSynthesisPortAndFallsBackToNeutralTone() throws Exception {
        Path projectFile = tempDir.resolve("Obra/Obra.docupodcast.json");
        Files.createDirectories(projectFile.getParent().resolve("voices/samples"));
        Files.writeString(projectFile.getParent().resolve("voices/samples/S-NEUTRAL.wav"), "sample", StandardCharsets.UTF_8);
        VoiceLibrary library = testLibrary()
                .withReferenceSample(sample("S-NEUTRAL", VoiceReferenceTone.NEUTRAL));
        VoiceGeneratedTestRequest request = new VoiceGeneratedTestRequest(
                TEST_ADVANCED_VOICE_ID,
                VoiceReferenceTone.HEROIC,
                "Frase editable de prueba.",
                advancedReadyEngine());

        VoiceGeneratedTestResult result = realUseCase().generate(projectFile, library, request);

        assertTrue(result.generated());
        assertTrue(result.fallbackToNeutral());
        assertTrue(result.audioFile().filter(Files::exists).isPresent());
        assertTrue(result.manifestFile().filter(Files::exists).isPresent());
        String manifest = Files.readString(result.manifestFile().orElseThrow());
        assertTrue(manifest.contains("Frase editable de prueba"));
        assertTrue(manifest.contains("\"realSynthesis\": true"));
    }

    @Test
    void blocksWhenAdvancedVoiceEngineIsNotReady() throws Exception {
        Path projectFile = tempDir.resolve("Obra/Obra.docupodcast.json");
        Files.createDirectories(projectFile.getParent());
        VoiceLibrary library = testLibrary()
                .withReferenceSample(sample("S-NEUTRAL", VoiceReferenceTone.NEUTRAL));
        VoiceGeneratedTestRequest request = new VoiceGeneratedTestRequest(
                TEST_ADVANCED_VOICE_ID,
                VoiceReferenceTone.NEUTRAL,
                "Frase editable.",
                AudioEngineDescriptor.mock());

        VoiceGeneratedTestResult result = realUseCase().generate(projectFile, library, request);

        assertFalse(result.generated());
        assertTrue(result.requiresConfiguration());
        assertTrue(result.userMessage().contains("Modo de prueba"));
    }

    @Test
    void blocksWhenRegisteredReferenceSampleFileIsMissing() throws Exception {
        Path projectFile = tempDir.resolve("Obra/Obra.docupodcast.json");
        Files.createDirectories(projectFile.getParent());
        VoiceLibrary library = testLibrary()
                .withReferenceSample(sample("S-NEUTRAL", VoiceReferenceTone.NEUTRAL));
        VoiceGeneratedTestRequest request = new VoiceGeneratedTestRequest(
                TEST_ADVANCED_VOICE_ID,
                VoiceReferenceTone.NEUTRAL,
                "Frase editable.",
                advancedReadyEngine());

        try {
            realUseCase().generate(projectFile, library, request);
        } catch (java.io.IOException ex) {
            assertTrue(ex.getMessage().contains("La muestra de voz registrada no existe"));
            return;
        }
        throw new AssertionError("Expected missing sample to fail loudly");
    }

    @Test
    void piperCannotSilentlyPresentItsPackagedVoiceAsTheSelectedCharacter() throws Exception {
        Path projectFile = tempDir.resolve("Obra/Obra.docupodcast.json");
        Files.createDirectories(projectFile.getParent().resolve("voices/samples"));
        Files.writeString(projectFile.getParent().resolve("voices/samples/S-NEUTRAL.wav"),
                "sample", StandardCharsets.UTF_8);
        VoiceLibrary library = testLibrary()
                .withReferenceSample(sample("S-NEUTRAL", VoiceReferenceTone.NEUTRAL));
        AudioEngineDescriptor piper = new AudioEngineDescriptor("piper", "Voz local simple",
                "piper", true, true, "", "Piper listo",
                java.util.Set.of(com.marcosmoreiradev.docupodcaststudio.media.api.EngineFeature.PACKAGED_VOICE),
                false);

        VoiceGeneratedTestResult result = realUseCase().generate(projectFile, library,
                new VoiceGeneratedTestRequest(TEST_ADVANCED_VOICE_ID, VoiceReferenceTone.HAPPY,
                        "Frase editable.", piper));

        assertFalse(result.generated());
        assertTrue(result.userMessage().contains("Coqui XTTS o Qwen3-TTS"));
    }

    @Test
    void transportsResolvedToneAsProviderNeutralPerformanceStyle() throws Exception {
        Path projectFile = tempDir.resolve("Obra/Obra.docupodcast.json");
        Files.createDirectories(projectFile.getParent().resolve("voices/samples"));
        Files.writeString(projectFile.getParent().resolve("voices/samples/S-ANGRY.wav"),
                "sample", StandardCharsets.UTF_8);
        VoiceLibrary library = testLibrary()
                .withReferenceSample(sample("S-ANGRY", VoiceReferenceTone.ANGRY));
        AtomicReference<VoiceTestSynthesisRequest> submitted = new AtomicReference<>();
        VoiceTestSynthesisGateway gateway = request -> {
            submitted.set(request);
            Files.createDirectories(request.outputFile().getParent());
            Files.write(request.outputFile(), fakeWavBytes());
            return VoiceTestSynthesisResult.generated(Files.size(request.outputFile()), "ok", "ok");
        };

        VoiceGeneratedTestResult result = new GenerateVoiceTestUseCase(
                new VoiceCapabilityPolicy(), new ResolveVoiceToneReferenceUseCase(), gateway)
                .generate(projectFile, library, new VoiceGeneratedTestRequest(
                        TEST_ADVANCED_VOICE_ID, VoiceReferenceTone.ANGRY,
                        "¡Escúchame ahora!", advancedReadyEngine()));

        assertTrue(result.generated());
        assertTrue("ANGRY".equals(submitted.get().performanceStyleId()));
    }

    private static GenerateVoiceTestUseCase realUseCase() {
        VoiceTestSynthesisGateway fakeGateway = request -> {
            Files.createDirectories(request.outputFile().getParent());
            Files.write(request.outputFile(), fakeWavBytes());
            return VoiceTestSynthesisResult.generated(Files.size(request.outputFile()), "Prueba real generada por fake de test.", "ok");
        };
        return new GenerateVoiceTestUseCase(new VoiceCapabilityPolicy(), new ResolveVoiceToneReferenceUseCase(), fakeGateway);
    }

    private static byte[] fakeWavBytes() {
        byte[] bytes = new byte[64];
        bytes[0] = 'R';
        bytes[1] = 'I';
        bytes[2] = 'F';
        bytes[3] = 'F';
        bytes[8] = 'W';
        bytes[9] = 'A';
        bytes[10] = 'V';
        bytes[11] = 'E';
        return bytes;
    }

    private static AudioEngineDescriptor advancedReadyEngine() {
        return new AudioEngineDescriptor("local-process", "Voz IA avanzada", "xtts", true, true,
                "xtts-wrapper", "Runtime avanzado listo",
                java.util.Set.of(
                        com.marcosmoreiradev.docupodcaststudio.media.api.EngineFeature.REFERENCE_VOICE,
                        com.marcosmoreiradev.docupodcaststudio.media.api.EngineFeature.EXPRESSIVE_STYLE),
                false);
    }

    private static VoiceLibrary testLibrary() {
        return VoiceLibrary.defaults().withVoice(new VoiceProfile(
                TEST_ADVANCED_VOICE_ID,
                "Voz avanzada de prueba",
                VoiceProfileType.OWN,
                VoiceEngineType.XTTS,
                "es",
                "S-NEUTRAL",
                "",
                VoiceQualityPreset.HUMAN_REFERENCE,
                true,
                "Voz de prueba registrada por el usuario.",
                Map.of("userManaged", "true")
        ));
    }

    private static VoiceReferenceSample sample(String id, VoiceReferenceTone tone) {
        return new VoiceReferenceSample(id, TEST_ADVANCED_VOICE_ID, tone,
                "voices/samples/" + id + ".wav", VoiceSampleOrigin.IMPORTED_FILE,
                VoiceFileOwnership.PROJECT_ASSET, 1200L, Instant.now(), "test");
    }
}
