package com.marcosmoreiradev.docupodcaststudio.application.render;

import com.marcosmoreiradev.docupodcaststudio.application.reading.PreparedReadingProjection;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.TheatreChoralVoiceFingerprint;
import com.marcosmoreiradev.docupodcaststudio.domain.assignment.NarrativeLayerAssignment;
import com.marcosmoreiradev.docupodcaststudio.domain.assignment.NarrativeLayerKind;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentSentenceSpan;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentSentenceSplitter;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentTextRange;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.render.NarrationRenderPlan;
import com.marcosmoreiradev.docupodcaststudio.domain.render.NarrationRenderSourceKind;
import com.marcosmoreiradev.docupodcaststudio.domain.render.NarrationRenderUnit;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;
import com.marcosmoreiradev.docupodcaststudio.domain.script.ScriptTextRange;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Builds a unit-level render plan from the prepared-reading payload and project layers.
 *
 * <p>The segment-based {@code NarrationScriptDocument} input is retained for compatibility, while
 * product code can enter through {@link PreparedReadingProjection}. The user-facing contract is
 * document fragments/oraciones with effective voice, audio clip and visual resources.</p>
 */
public final class BuildNarrationRenderPlanUseCase {
    public NarrationRenderPlan build(PreparedReadingProjection projection, DocuPodcastProject project) {
        Objects.requireNonNull(projection, "projection");
        return build(projection.narrationScript(), project);
    }

    public NarrationRenderPlan build(NarrationScriptDocument script, DocuPodcastProject project) {
        Objects.requireNonNull(project, "project");
        return build(script, project.narrativeLayerAssignments(), project.theatre(), project);
    }

    public NarrationRenderPlan build(NarrationScriptDocument script, List<NarrativeLayerAssignment> assignments) {
        return build(script, assignments, List.of());
    }

    public NarrationRenderPlan build(NarrationScriptDocument script,
                                     List<NarrativeLayerAssignment> assignments,
                                     List<TheatreProjectLayer.VoiceRoleAlias> theatreVoices) {
        Objects.requireNonNull(script, "script");
        List<NarrativeLayerAssignment> layers = assignments == null ? List.of() : List.copyOf(assignments);
        List<TheatreProjectLayer.VoiceRoleAlias> roleVoices = theatreVoices == null ? List.of() : List.copyOf(theatreVoices);
        return build(script, layers, TheatreProjectLayer.empty().withChoralVoiceAssignments(List.of()), roleVoices, null);
    }

    private NarrationRenderPlan build(NarrationScriptDocument script,
                                      List<NarrativeLayerAssignment> assignments,
                                      TheatreProjectLayer theatre,
                                      DocuPodcastProject project) {
        List<TheatreProjectLayer.VoiceRoleAlias> roleVoices =
                theatre == null ? List.of() : List.copyOf(theatre.voiceRoleAliases());
        return build(script, assignments, theatre, roleVoices, project);
    }

    private NarrationRenderPlan build(NarrationScriptDocument script,
                                      List<NarrativeLayerAssignment> layers,
                                      TheatreProjectLayer theatre,
                                      List<TheatreProjectLayer.VoiceRoleAlias> roleVoices,
                                      DocuPodcastProject project) {
        ArrayList<NarrationRenderUnit> units = new ArrayList<>();
        for (NarrationSegment segment : script.segments()) {
            if (!segment.narratable()) {
                continue;
            }
            boolean choralAudio = choralAudioFor(segment, theatre, project).isPresent();
            List<DocumentSentenceSpan> spans = choralAudio
                    ? List.of(new DocumentSentenceSpan(segment.id() + ":S001", 0,
                    new DocumentTextRange(segment.id(), 0, segment.narrationText().length()), segment.narrationText()))
                    : DocumentSentenceSplitter.split(segment.id(), segment.narrationText());
            if (spans.isEmpty()) {
                spans = List.of(new DocumentSentenceSpan(segment.id() + ":S001", 0,
                        new com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentTextRange(segment.id(), 0, segment.narrationText().length()),
                        segment.narrationText()));
            }
            for (DocumentSentenceSpan span : spans) {
                units.add(unitFor(segment, span, layers, roleVoices, theatre, project));
            }
        }
        return new NarrationRenderPlan("RENDER-" + script.id(), script.id(), units, Instant.now());
    }

    private NarrationRenderUnit unitFor(NarrationSegment segment,
                                        DocumentSentenceSpan span,
                                        List<NarrativeLayerAssignment> layers,
                                        List<TheatreProjectLayer.VoiceRoleAlias> theatreVoices,
                                        TheatreProjectLayer theatre,
                                        DocuPodcastProject project) {
        ScriptTextRange range = new ScriptTextRange(segment.id(), span.range().startOffset(), span.range().endOffset());
        List<NarrativeLayerAssignment> matching = layers.stream()
                .filter(layer -> overlaps(layer.textRange(), range))
                .toList();
        Optional<NarrativeLayerAssignment> humanAudio = first(matching, NarrativeLayerKind.HUMAN_AUDIO);
        Optional<NarrativeLayerAssignment> voice = first(matching, NarrativeLayerKind.VOICE);
        Optional<NarrativeLayerAssignment> emotion = first(matching, NarrativeLayerKind.EMOTION);
        Optional<NarrativeLayerAssignment> image = first(matching, NarrativeLayerKind.IMAGE);
        Optional<String> choralAudio = choralAudioFor(segment, theatre, project);

        NarrationRenderSourceKind sourceKind = choralAudio.isPresent() || humanAudio.isPresent()
                ? NarrationRenderSourceKind.AUDIO_CLIP
                : NarrationRenderSourceKind.TEXT_TO_SPEECH;
        String voiceProfileId = voice.map(NarrativeLayerAssignment::targetId)
                .or(() -> theatreVoiceFor(segment.characterId(), theatreVoices))
                .orElse(segment.voiceProfileId());
        String performanceStyleId = emotion.map(NarrativeLayerAssignment::targetId).orElse(segment.performanceStyleId());
        String audioAssetId = choralAudio.or(() -> humanAudio.map(NarrativeLayerAssignment::targetId)).orElse("");
        String imageAssetId = image.map(NarrativeLayerAssignment::targetId).orElse("");
        DocumentTextRange documentRange = matching.stream()
                .map(NarrativeLayerAssignment::documentRange)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
        List<String> appliedLayerIds = matching.stream().map(NarrativeLayerAssignment::id).toList();
        String unitId = segment.id() + "-U" + String.format("%03d", span.index() + 1);
        return new NarrationRenderUnit(
                unitId,
                segment.id(),
                span.index(),
                range,
                documentRange,
                span.text(),
                sourceKind,
                voiceProfileId,
                performanceStyleId,
                audioAssetId,
                imageAssetId,
                appliedLayerIds
        );
    }

    private static Optional<String> choralAudioFor(NarrationSegment segment,
                                                   TheatreProjectLayer theatre,
                                                   DocuPodcastProject project) {
        if (segment == null || theatre == null || project == null) {
            return Optional.empty();
        }
        Optional<String> interventionId = interventionIdForSegment(theatre, segment);
        if (interventionId.isEmpty()) {
            return Optional.empty();
        }
        return theatre.choralVoiceAssignments().stream()
                .filter(assignment -> assignment.intervencionId().equals(interventionId.get()))
                .filter(assignment -> TheatreChoralVoiceFingerprint.isCurrent(assignment, project, segment))
                .map(TheatreProjectLayer.ChoralVoiceAssignment::mixedAudioAssetId)
                .filter(assetId -> assetId != null && !assetId.isBlank())
                .filter(assetId -> project.assets().byId(assetId).filter(asset -> asset.isAudio()).isPresent())
                .findFirst();
    }

    private static Optional<String> interventionIdForSegment(TheatreProjectLayer theatre, NarrationSegment segment) {
        if (theatre == null || segment == null) {
            return Optional.empty();
        }
        return segment.sourceBlockIds().stream()
                .flatMap(blockId -> theatre.intervenciones().stream()
                        .filter(intervention -> intervention.blockId().equals(blockId))
                        .map(TheatreProjectLayer.Intervencion::id))
                .findFirst();
    }

    private static Optional<String> theatreVoiceFor(String characterId, List<TheatreProjectLayer.VoiceRoleAlias> theatreVoices) {
        String target = characterId == null ? "" : characterId.strip();
        if (target.isBlank()) {
            return Optional.empty();
        }
        return theatreVoices.stream()
                .filter(alias -> alias.characterId().equals(target))
                .map(TheatreProjectLayer.VoiceRoleAlias::voiceProfileId)
                .filter(voice -> !voice.isBlank())
                .findFirst();
    }

    private static Optional<NarrativeLayerAssignment> first(List<NarrativeLayerAssignment> assignments, NarrativeLayerKind kind) {
        return assignments.stream().filter(layer -> layer.kind() == kind).findFirst();
    }

    private static boolean overlaps(ScriptTextRange candidate, ScriptTextRange target) {
        if (candidate == null || target == null || !candidate.segmentId().equals(target.segmentId())) {
            return false;
        }
        return candidate.startOffset() < target.endOffset() && target.startOffset() < candidate.endOffset();
    }
}
