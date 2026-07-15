package com.marcosmoreiradev.docupodcaststudio.application.theatre;

import com.marcosmoreiradev.docupodcaststudio.application.audio.AudioEngineDescriptor;
import com.marcosmoreiradev.docupodcaststudio.application.process.ExternalProcessRequest;
import com.marcosmoreiradev.docupodcaststudio.application.process.ExternalProcessResult;
import com.marcosmoreiradev.docupodcaststudio.application.process.ExternalProcessRunner;
import com.marcosmoreiradev.docupodcaststudio.application.settings.OperationalSettings;
import com.marcosmoreiradev.docupodcaststudio.application.video.EmbeddedFfmpegLocator;
import com.marcosmoreiradev.docupodcaststudio.application.video.FfmpegRuntimeProbeUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.voice.ResolveVoiceToneReferenceUseCase;
import com.marcosmoreiradev.docupodcaststudio.application.voice.VoiceCapabilityPolicy;
import com.marcosmoreiradev.docupodcaststudio.application.voice.VoiceTestSynthesisRequest;
import com.marcosmoreiradev.docupodcaststudio.application.voice.VoiceTestSynthesisResult;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegmentType;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceEngineType;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceLibrary;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceProfile;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceProfileType;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceQualityPreset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RenderTheatreChoralVoiceUseCaseTest {
    @TempDir
    Path tempDir;

    @Test
    void synthesizesFullTextForEveryParticipantAndRegistersAtomicMix() throws Exception {
        Fixture fixture = fixture();
        ArrayList<VoiceTestSynthesisRequest> synthesisRequests = new ArrayList<>();
        var gateway = (com.marcosmoreiradev.docupodcaststudio.application.voice.VoiceTestSynthesisGateway) request -> {
            synthesisRequests.add(request);
            Files.createDirectories(request.outputFile().getParent());
            Files.write(request.outputFile(), wavBytes((byte) synthesisRequests.size()));
            return VoiceTestSynthesisResult.generated(128, "ok", "");
        };
        RecordingRunner runner = new RecordingRunner(true);
        RenderTheatreChoralVoiceUseCase useCase = useCase(fixture.applicationRoot(), gateway, runner);

        TheatreChoralVoiceRenderResult result = useCase.execute(fixture.request(), ignored -> { });

        assertEquals(2, synthesisRequests.size());
        assertTrue(synthesisRequests.stream().allMatch(request -> request.phrase().equals(fixture.segment().narrationText())));
        assertEquals(List.of("VOC-BIGOTE", "VOC-TORNILLO"), synthesisRequests.stream()
                .map(VoiceTestSynthesisRequest::voiceProfileId).toList());
        ExternalProcessRequest mix = runner.requests.stream()
                .filter(request -> request.auditLabel().equals("theatre-choral-amix"))
                .findFirst().orElseThrow();
        String filter = mix.command().get(mix.command().indexOf("-filter_complex") + 1);
        assertTrue(filter.contains("amix=inputs=2:duration=longest:dropout_transition=0:normalize=1"));
        assertTrue(filter.contains("alimiter=limit=0.95"));
        assertTrue(mix.command().containsAll(List.of("-ar", "48000", "-ac", "1", "pcm_s16le")));
        assertTrue(Files.isRegularFile(result.outputFile()));
        assertTrue(result.outputFile().startsWith(fixture.projectFile().getParent()));
        assertEquals("AUDIO-CHORAL-INTERVENCION-1", result.assignment().mixedAudioAssetId());
        assertTrue(TheatreChoralVoiceFingerprint.isGenerated(result.assignment().sourceFingerprint()));
        assertTrue(result.project().assets().byId(result.assignment().mixedAudioAssetId()).orElseThrow().isAudio());
    }

    @Test
    void synthesisFailureKeepsPreviousMixAndProjectUntouched() throws Exception {
        Fixture fixture = fixture();
        Path previous = fixture.projectFile().getParent().resolve("media/audio/theatre-choral/intervencion-1-choral.wav");
        Files.createDirectories(previous.getParent());
        byte[] oldBytes = "previous-valid-wave".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        Files.write(previous, oldBytes);
        int[] calls = {0};
        var gateway = (com.marcosmoreiradev.docupodcaststudio.application.voice.VoiceTestSynthesisGateway) request -> {
            calls[0]++;
            if (calls[0] == 2) return VoiceTestSynthesisResult.failed("fallo TTS", "diagnostico");
            Files.createDirectories(request.outputFile().getParent());
            Files.write(request.outputFile(), wavBytes((byte) 1));
            return VoiceTestSynthesisResult.generated(128, "ok", "");
        };
        RecordingRunner runner = new RecordingRunner(true);
        RenderTheatreChoralVoiceUseCase useCase = useCase(fixture.applicationRoot(), gateway, runner);

        assertThrows(IOException.class, () -> useCase.execute(fixture.request(), ignored -> { }));

        assertArrayEquals(oldBytes, Files.readAllBytes(previous));
        assertTrue(fixture.project().theatre().choralVoiceAssignments().isEmpty());
        assertFalse(runner.requests.stream().anyMatch(request -> request.auditLabel().equals("theatre-choral-amix")));
    }

    private RenderTheatreChoralVoiceUseCase useCase(Path applicationRoot,
                                                     com.marcosmoreiradev.docupodcaststudio.application.voice.VoiceTestSynthesisGateway gateway,
                                                     ExternalProcessRunner runner) {
        return new RenderTheatreChoralVoiceUseCase(
                gateway,
                new VoiceCapabilityPolicy(),
                new ResolveVoiceToneReferenceUseCase(),
                new EmbeddedFfmpegLocator(),
                new FfmpegRuntimeProbeUseCase(runner),
                runner,
                OperationalSettings::defaults,
                applicationRoot);
    }

    private Fixture fixture() throws IOException {
        Path root = tempDir.resolve("project");
        Path projectFile = root.resolve("obra.docupodcast.json");
        Files.createDirectories(root);
        Files.writeString(projectFile, "{}");
        Path applicationRoot = tempDir.resolve("app");
        Path ffmpeg = applicationRoot.resolve("tools/ffmpeg/bin/ffmpeg.exe");
        Files.createDirectories(ffmpeg.getParent());
        Files.write(ffmpeg, new byte[]{1});

        VoiceLibrary voices = VoiceLibrary.defaults()
                .withVoice(localVoice("VOC-BIGOTE", "Capitan Bigote"))
                .withVoice(localVoice("VOC-TORNILLO", "Teniente Tornillo"));
        TheatreProjectLayer theatre = new TheatreProjectLayer(
                List.of(new TheatreProjectLayer.Intervencion("INTERVENCION-1", "B0001", 1)),
                List.of(
                        new TheatreProjectLayer.CharacterProfile("CHR-BIGOTE", "CAPITAN BIGOTE", List.of("BIGOTE"), ""),
                        new TheatreProjectLayer.CharacterProfile("CHR-TORNILLO", "TENIENTE TORNILLO", List.of("TORNILLO"), "")),
                List.of(
                        new TheatreProjectLayer.VoiceRoleAlias("ALIAS-BIGOTE", "Bigote", "VOC-BIGOTE", "CHR-BIGOTE", ""),
                        new TheatreProjectLayer.VoiceRoleAlias("ALIAS-TORNILLO", "Tornillo", "VOC-TORNILLO", "CHR-TORNILLO", "")),
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
        DocuPodcastProject project = DocuPodcastProject.createNew("Obra").withVoiceLibrary(voices).withTheatre(theatre);
        NarrationSegment segment = new NarrationSegment("SEG-001", NarrationSegmentType.PARAGRAPH, "Todos",
                "Que sorpresa. Esto funciona al mismo tiempo.", List.of("B0001"), "CHR-BIGOTE",
                "VOC-BIGOTE", "STY-HAPPY", Map.of());
        NarrationScriptDocument script = NarrationScriptDocument.create("Obra", "es", "obra.docx", List.of(segment));
        TheatreChoralVoiceRenderRequest request = new TheatreChoralVoiceRenderRequest(project, projectFile, script,
                "INTERVENCION-1", List.of("CHR-BIGOTE", "CHR-TORNILLO"),
                AudioEngineDescriptor.process("Motor TTS local", true, "tts", "listo"));
        return new Fixture(project, projectFile, applicationRoot, segment, request);
    }

    private static VoiceProfile localVoice(String id, String name) {
        return new VoiceProfile(id, name, VoiceProfileType.PREDEFINED, VoiceEngineType.LOCAL_TTS_PROCESS,
                "es", "", "", VoiceQualityPreset.BALANCED, false, "", Map.of());
    }

    private static byte[] wavBytes(byte marker) {
        byte[] bytes = new byte[128];
        bytes[0] = marker;
        return bytes;
    }

    private static final class RecordingRunner implements ExternalProcessRunner {
        private final List<ExternalProcessRequest> requests = new ArrayList<>();
        private final boolean mixSucceeds;

        private RecordingRunner(boolean mixSucceeds) {
            this.mixSucceeds = mixSucceeds;
        }

        @Override
        public ExternalProcessResult run(ExternalProcessRequest request) throws IOException {
            requests.add(request);
            if (request.auditLabel().equals("theatre-choral-amix") && mixSucceeds) {
                Path output = Path.of(request.command().getLast());
                Files.createDirectories(output.getParent());
                Files.write(output, wavBytes((byte) 9));
            }
            String stdout = request.command().contains("-version")
                    ? "ffmpeg version test --enable-libx264"
                    : request.command().contains("-encoders") ? "libx264" : "";
            int exit = request.auditLabel().equals("theatre-choral-amix") && !mixSucceeds ? 1 : 0;
            return new ExternalProcessResult(exit, false, false, stdout, "", request.commandAudit(), Duration.ofMillis(2));
        }
    }

    private record Fixture(DocuPodcastProject project, Path projectFile, Path applicationRoot,
                           NarrationSegment segment, TheatreChoralVoiceRenderRequest request) {
    }
}
