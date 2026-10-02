package com.marcosmoreiradev.docupodcaststudio.application.render;

import com.marcosmoreiradev.docupodcaststudio.application.reading.PreparedReadingProjection;
import com.marcosmoreiradev.docupodcaststudio.application.theatre.TheatreChoralVoiceFingerprint;
import com.marcosmoreiradev.docupodcaststudio.application.voice.EffectiveVoiceAssignmentResolver;
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
import java.util.LinkedHashSet;
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
    private final EffectiveVoiceAssignmentResolver voiceResolver;
    private final NarrationSynthesisGranularityPolicy granularityPolicy;

    public BuildNarrationRenderPlanUseCase() {
        this(new EffectiveVoiceAssignmentResolver(), new NarrationSynthesisGranularityPolicy());
    }

    public BuildNarrationRenderPlanUseCase(EffectiveVoiceAssignmentResolver voiceResolver) {
        this(voiceResolver, new NarrationSynthesisGranularityPolicy());
    }

    BuildNarrationRenderPlanUseCase(EffectiveVoiceAssignmentResolver voiceResolver,
                                    NarrationSynthesisGranularityPolicy granularityPolicy) {
        this.voiceResolver = Objects.requireNonNull(voiceResolver, "voiceResolver");
        this.granularityPolicy = Objects.requireNonNull(granularityPolicy, "granularityPolicy");
    }

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
            boolean wholeSemanticSegment = granularityPolicy.resolve(project)
                    == NarrationSynthesisGranularityPolicy.Granularity.SEMANTIC_SEGMENT;
            boolean choralAudio = choralAudioFor(segment, theatre, project).isPresent();
            List<DocumentSentenceSpan> spans = wholeSemanticSegment || choralAudio
                    ? wholeSegmentSpan(segment)
                    : DocumentSentenceSplitter.split(segment.id(), segment.narrationText());
            if (spans.isEmpty()) {
                spans = wholeSegmentSpan(segment);
            }
            for (DocumentSentenceSpan span : spans) {
                units.add(unitFor(segment, span, layers, roleVoices, theatre, project));
            }
        }
        return new NarrationRenderPlan("RENDER-" + script.id(), script.id(), units, Instant.now());
    }

    private static List<DocumentSentenceSpan> wholeSegmentSpan(NarrationSegment segment) {
        return List.of(new DocumentSentenceSpan(
                segment.id() + ":S001",
                0,
                new DocumentTextRange(segment.id(), 0, segment.narrationText().length()),
                segment.narrationText()));
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
        String theatreVoice = defaultStageDirectionVoice(segment, project, theatreVoices)
                .orElseGet(() -> theatreVoiceFor(segment.characterId(), theatreVoices).orElse(""));
        String voiceProfileId = voiceResolver.resolve(
                voice.map(NarrativeLayerAssignment::targetId),
                Optional.ofNullable(theatreVoice).filter(value -> !value.isBlank()),
                effectiveDocumentDefaultVoice(segment, project),
                EffectiveVoiceAssignmentResolver.DEFAULT_NARRATOR_VOICE_ID).voiceProfileId();
        String performanceStyleId = emotion.map(NarrativeLayerAssignment::targetId)
                .orElseGet(() -> effectiveDocumentDefaultTone(segment, project));
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

    /**
     * Gives stage directions a stable voice that is not used by a character.
     * A manually assigned non-narrator alias remains authoritative. Older
     * projects commonly persisted VOC-NARRATOR for CHR-ACOTACION; that value is
     * treated as the legacy placeholder and upgraded at render time.
     */
    private static Optional<String> defaultStageDirectionVoice(
            NarrationSegment segment,
            DocuPodcastProject project,
            List<TheatreProjectLayer.VoiceRoleAlias> theatreVoices) {
        if (segment == null || !"true".equalsIgnoreCase(
                segment.metadata().getOrDefault("theatreStageDirection", "false"))) {
            return Optional.empty();
        }
        Optional<String> assigned = theatreVoiceFor(segment.characterId(), theatreVoices)
                .filter(voice -> !EffectiveVoiceAssignmentResolver.DEFAULT_NARRATOR_VOICE_ID
                        .equalsIgnoreCase(voice));
        if (assigned.isPresent() || project == null) {
            return assigned;
        }
        LinkedHashSet<String> usedByCharacters = new LinkedHashSet<>();
        theatreVoices.stream()
                .filter(alias -> !"CHR-ACOTACION".equalsIgnoreCase(alias.characterId()))
                .map(TheatreProjectLayer.VoiceRoleAlias::voiceProfileId)
                .filter(voice -> voice != null && !voice.isBlank())
                .forEach(usedByCharacters::add);
        List<String> preferred = List.of(
                "VOC-PRESET-MUJER-ADULTA-CALIDA-NARRATIVA",
                "VOC-PRESET-HOMBRE-ADULTO-PERSONAJE-NARRATIVO");
        return java.util.stream.Stream.concat(
                        preferred.stream(),
                        project.voiceLibrary().voices().stream().map(voice -> voice.id()))
                .filter(voice -> !EffectiveVoiceAssignmentResolver.DEFAULT_NARRATOR_VOICE_ID
                        .equalsIgnoreCase(voice))
                .filter(voice -> !usedByCharacters.contains(voice))
                .filter(voice -> project.voiceLibrary().voiceById(voice).isPresent())
                .filter(voice -> project.voiceLibrary().referenceSampleSetByVoiceId(voice).isPresent())
                .findFirst();
    }

    private static String effectiveDocumentDefaultVoice(
            NarrationSegment segment,
            DocuPodcastProject project) {
        String segmentVoice = segment == null || segment.voiceProfileId() == null
                ? "" : segment.voiceProfileId().strip();
        if (project == null
                || (!segmentVoice.isBlank()
                && !EffectiveVoiceAssignmentResolver.DEFAULT_NARRATOR_VOICE_ID
                .equalsIgnoreCase(segmentVoice))) {
            return segmentVoice;
        }
        String projectVoice = project.documentDefaultVoiceProfileId();
        return projectVoice.isBlank() ? segmentVoice : projectVoice;
    }

    private static String effectiveDocumentDefaultTone(
            NarrationSegment segment,
            DocuPodcastProject project) {
        String segmentTone = segment == null || segment.performanceStyleId() == null
                ? "" : segment.performanceStyleId().strip();
        if (project == null || (!segmentTone.isBlank()
                && !"STY-NEUTRAL".equalsIgnoreCase(segmentTone))) {
            return segmentTone;
        }
        String projectTone = project.documentDefaultVoiceToneId();
        return projectTone.isBlank() ? segmentTone : projectTone;
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
