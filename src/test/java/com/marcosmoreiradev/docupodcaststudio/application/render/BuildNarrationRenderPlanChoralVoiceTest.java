package com.marcosmoreiradev.docupodcaststudio.application.render;

import com.marcosmoreiradev.docupodcaststudio.application.theatre.TheatreChoralVoiceFingerprint;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetKind;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.render.NarrationRenderSourceKind;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegmentType;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BuildNarrationRenderPlanChoralVoiceTest {
    @Test
    void insertsGeneratedChoralMixOnlyOnceForWholeIntervention() {
        NarrationSegment segment = segment("Todos hablan. La segunda oracion sigue en la misma intervencion.");
        DocuPodcastProject base = projectWithTheatre();
        String fingerprint = TheatreChoralVoiceFingerprint.compute(base, segment, List.of("CHR-A", "CHR-B"));
        DocuPodcastProject project = withAssignment(base, fingerprint);
        NarrationScriptDocument script = NarrationScriptDocument.create("Obra", "es", "obra.docx", List.of(segment));

        var plan = new BuildNarrationRenderPlanUseCase().build(script, project);

        assertEquals(1, plan.unitCount());
        assertEquals(NarrationRenderSourceKind.AUDIO_CLIP, plan.units().getFirst().sourceKind());
        assertEquals("AUDIO-CHORAL-1", plan.units().getFirst().audioAssetId());
        assertEquals(segment.narrationText(), plan.units().getFirst().text());
    }

    @Test
    void staleGeneratedMixFallsBackToNormalSentenceTts() {
        NarrationSegment original = segment("Texto original. Segunda frase.");
        DocuPodcastProject base = projectWithTheatre();
        String fingerprint = TheatreChoralVoiceFingerprint.compute(base, original, List.of("CHR-A", "CHR-B"));
        DocuPodcastProject project = withAssignment(base, fingerprint);
        NarrationSegment changed = segment("Texto modificado. Segunda frase.");
        NarrationScriptDocument script = NarrationScriptDocument.create("Obra", "es", "obra.docx", List.of(changed));

        var plan = new BuildNarrationRenderPlanUseCase().build(script, project);

        assertEquals(2, plan.unitCount());
        assertTrue(plan.units().stream().allMatch(unit -> unit.sourceKind() == NarrationRenderSourceKind.TEXT_TO_SPEECH));
    }

    private static NarrationSegment segment(String text) {
        return new NarrationSegment("SEG-001", NarrationSegmentType.PARAGRAPH, "Todos", text,
                List.of("B0001"), "CHR-A", "VOC-A", "STY-NEUTRAL", Map.of());
    }

    private static DocuPodcastProject projectWithTheatre() {
        TheatreProjectLayer theatre = new TheatreProjectLayer(
                List.of(new TheatreProjectLayer.Intervencion("INTERVENCION-1", "B0001", 1)),
                List.of(
                        new TheatreProjectLayer.CharacterProfile("CHR-A", "A", List.of(), ""),
                        new TheatreProjectLayer.CharacterProfile("CHR-B", "B", List.of(), "")),
                List.of(
                        new TheatreProjectLayer.VoiceRoleAlias("ALIAS-A", "A", "VOC-A", "CHR-A", ""),
                        new TheatreProjectLayer.VoiceRoleAlias("ALIAS-B", "B", "VOC-B", "CHR-B", "")),
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
        return DocuPodcastProject.createNew("Obra").withTheatre(theatre)
                .withAsset(new ProjectAssetReference("AUDIO-CHORAL-1", ProjectAssetKind.AUDIO_CLIP,
                        "Mezcla", "media/audio/theatre-choral/mix.wav", "audio/wav", "Mezcla", "", ""));
    }

    private static DocuPodcastProject withAssignment(DocuPodcastProject base, String fingerprint) {
        TheatreProjectLayer.ChoralVoiceAssignment assignment = new TheatreProjectLayer.ChoralVoiceAssignment(
                "INTERVENCION-1", List.of("CHR-A", "CHR-B"), "AUDIO-CHORAL-1", fingerprint, "");
        return base.withTheatre(base.theatre().withChoralVoiceAssignments(List.of(assignment)));
    }
}
