package com.marcosmoreiradev.docupodcaststudio.application.audio;

import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentTextRange;
import com.marcosmoreiradev.docupodcaststudio.domain.render.RenderUnit;
import com.marcosmoreiradev.docupodcaststudio.domain.render.RenderUnitKind;
import com.marcosmoreiradev.docupodcaststudio.domain.render.RenderUnitPlan;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegmentType;
import com.marcosmoreiradev.docupodcaststudio.domain.script.ScriptTextRange;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.CharacterProfile;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.PerformanceStyle;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceEngineType;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceFileOwnership;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceLibrary;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceProfile;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceProfileType;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceQualityPreset;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceReferenceSample;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceReferenceSampleSet;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceReferenceTone;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceSampleOrigin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class AudioGenerationRequestVoiceReferenceSampleTest {
    @TempDir
    Path tempDir;

    @Test
    void resolvesRegisteredToneReferenceSampleForRenderUnit() throws Exception {
        Path neutral = sampleFile("voices/samples/actor-neutral.wav");
        Path happy = sampleFile("voices/samples/actor-happy.wav");
        VoiceLibrary library = libraryWithSamples(
                sample("S-NEUTRAL", VoiceReferenceTone.NEUTRAL, relative(neutral)),
                sample("S-HAPPY", VoiceReferenceTone.HAPPY, relative(happy)));

        AudioGenerationRequest request = new AudioGenerationRequest(script(), plan("TONE-HAPPY"), tempDir, "Job", library);

        assertEquals(happy, request.referenceSamplePathFor(request.generationUnits().getFirst()).orElseThrow());
    }

    @Test
    void resolvesLegacyStyleIdToRegisteredToneReferenceSampleForRenderUnit() throws Exception {
        Path neutral = sampleFile("voices/samples/actor-neutral.wav");
        Path happy = sampleFile("voices/samples/actor-happy.wav");
        VoiceLibrary library = libraryWithSamples(
                sample("S-NEUTRAL", VoiceReferenceTone.NEUTRAL, relative(neutral)),
                sample("S-HAPPY", VoiceReferenceTone.HAPPY, relative(happy)));

        AudioGenerationRequest request = new AudioGenerationRequest(script(), plan("STY-HAPPY"), tempDir, "Job", library);

        assertEquals(happy, request.referenceSamplePathFor(request.generationUnits().getFirst()).orElseThrow());
    }

    @Test
    void fallsBackToNeutralReferenceSampleWhenRequestedToneIsMissing() throws Exception {
        Path neutral = sampleFile("voices/samples/actor-neutral.wav");
        VoiceLibrary library = libraryWithSamples(sample("S-NEUTRAL", VoiceReferenceTone.NEUTRAL, relative(neutral)));

        AudioGenerationRequest request = new AudioGenerationRequest(script(), plan("TONE-HEROIC"), tempDir, "Job", library);

        assertEquals(neutral, request.referenceSamplePathFor(request.generationUnits().getFirst()).orElseThrow());
    }

    private RenderUnitPlan plan(String toneTargetId) {
        RenderUnit unit = new RenderUnit("SEG-001-U001", "SEG-001-U001", "SEG-001", 0,
                new ScriptTextRange("SEG-001", 0, 18), new DocumentTextRange("B0001", 0, 18),
                "Fragmento", "Texto para generar", RenderUnitKind.SPOKEN_ONLY,
                "VOC-ACTOR", toneTargetId, "", "", 5.0, List.of("LAYER-VOICE", "LAYER-TONE"));
        return new RenderUnitPlan("UNITPLAN-SCRIPT-001", script().id(), List.of(unit), 5.0, Instant.EPOCH);
    }

    private static NarrationScriptDocument script() {
        return NarrationScriptDocument.create("Guion", "es", "doc", List.of(
                NarrationSegment.of("SEG-001", NarrationSegmentType.PARAGRAPH, "Intro", "Texto para generar.", List.of("B0001"))
        ));
    }

    private VoiceLibrary libraryWithSamples(VoiceReferenceSample... samples) {
        VoiceProfile voice = new VoiceProfile("VOC-ACTOR", "Actor", VoiceProfileType.OWN,
                VoiceEngineType.HUMAN_AUDIO, "es", "S-NEUTRAL", "", VoiceQualityPreset.HUMAN_REFERENCE,
                false, "Muestra propia para generación local.", Map.of());
        return new VoiceLibrary("VOICE-LIBRARY-TEST", List.of(voice), List.of(new CharacterProfile("CHR-ACTOR", "Actor", "VOC-ACTOR", "STY-NEUTRAL", "Actor de prueba", Map.of())),
                List.of(PerformanceStyle.neutral()), List.of(new VoiceReferenceSampleSet("VOC-ACTOR", List.of(samples))),
                Instant.EPOCH, "test");
    }

    private VoiceReferenceSample sample(String id, VoiceReferenceTone tone, String uri) {
        return new VoiceReferenceSample(id, "VOC-ACTOR", tone, uri, VoiceSampleOrigin.IMPORTED_FILE,
                VoiceFileOwnership.PROJECT_ASSET, 1000L, Instant.EPOCH, "test");
    }

    private Path sampleFile(String relative) throws Exception {
        Path file = tempDir.resolve(relative).normalize();
        Files.createDirectories(file.getParent());
        Files.write(file, new byte[] {1, 2, 3});
        return file;
    }

    private String relative(Path file) {
        return tempDir.relativize(file).toString().replace('\\', '/');
    }
}
