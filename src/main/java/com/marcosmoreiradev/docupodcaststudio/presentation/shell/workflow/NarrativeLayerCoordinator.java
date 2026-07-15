package com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow;

import com.marcosmoreiradev.docupodcaststudio.application.voice.VoiceToneReferenceResolution;
import com.marcosmoreiradev.docupodcaststudio.domain.assignment.NarrativeLayerAssignment;
import com.marcosmoreiradev.docupodcaststudio.domain.assignment.NarrativeLayerAssignmentPolicy;
import com.marcosmoreiradev.docupodcaststudio.domain.assignment.NarrativeLayerKind;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentTextRange;
import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.document.SourceDocumentSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.document.TextAnchor;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;
import com.marcosmoreiradev.docupodcaststudio.domain.script.ScriptTextRange;
import com.marcosmoreiradev.docupodcaststudio.domain.storyboard.StoryboardDocument;
import com.marcosmoreiradev.docupodcaststudio.presentation.document.DocumentLayerAssignmentPresentation;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.ProjectSession;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Coordinates the narrative-layer brain behind the narrated document.
 *
 * <p>Voice, human audio, emotion, image, ambient audio and production notes are
 * project-side layers anchored to the narrated document and its internal narration
 * projection. This coordinator keeps conflict detection, real target resolution,
 * range mapping and removal rules out of the shell view-model; the shell remains only
 * responsible for visible state and user feedback.</p>
 */
public final class NarrativeLayerCoordinator {
    private final NarrativeLayerTargetResolver targetResolver = new NarrativeLayerTargetResolver();

    public AssignmentOutcome assign(ProjectSession session,
                                    NarrativeLayerKind kind,
                                    ScriptTextRange scriptRange,
                                    DocumentTextRange documentRange,
                                    String documentBlockId,
                                    String selectedDocumentTextPreview) {
        return assign(session, kind, scriptRange, documentRange, documentBlockId, selectedDocumentTextPreview, "");
    }

    public AssignmentOutcome assign(ProjectSession session,
                                    NarrativeLayerKind kind,
                                    ScriptTextRange scriptRange,
                                    DocumentTextRange documentRange,
                                    String documentBlockId,
                                    String selectedDocumentTextPreview,
                                    String preferredTargetId) {
        return assign(session, kind, scriptRange, documentRange, null, documentBlockId, selectedDocumentTextPreview, preferredTargetId);
    }

    public AssignmentOutcome assign(ProjectSession session,
                                    NarrativeLayerKind kind,
                                    ScriptTextRange scriptRange,
                                    DocumentTextRange documentRange,
                                    ReadableDocument document,
                                    String documentBlockId,
                                    String selectedDocumentTextPreview,
                                    String preferredTargetId) {
        Objects.requireNonNull(session, "session");
        Objects.requireNonNull(scriptRange, "scriptRange");
        NarrativeLayerKind normalizedKind = kind == null ? NarrativeLayerKind.NOTE : kind;
        NarrativeLayerTargetResolver.TargetResolution target = targetResolver.resolve(session.project(), normalizedKind, preferredTargetId);
        if (!target.resolved()) {
            return AssignmentOutcome.missingTarget(target.message());
        }
        String assignmentId = assignmentId(normalizedKind, scriptRange);
        TextAnchor anchor = buildRichAnchor(assignmentId, document, documentRange);
        NarrativeLayerAssignment assignment = new NarrativeLayerAssignment(
                assignmentId,
                normalizedKind,
                scriptRange,
                documentRange,
                anchor,
                target.targetId(),
                target.displayName(),
                target.notes());
        List<NarrativeLayerAssignment> conflicts = NarrativeLayerAssignmentPolicy.conflicts(
                session.project().narrativeLayerAssignments(), assignment);
        if (!conflicts.isEmpty()) {
            return AssignmentOutcome.conflict(NarrativeLayerAssignmentPolicy.conflictMessage(conflicts.get(0), assignment)
                    + " Usa Quitar capa principal y vuelve a asignar, o divide el rango.");
        }
        session.replaceProject(session.project().withNarrativeLayerAssignment(assignment), true);
        return AssignmentOutcome.assigned(assignment, successMessage(normalizedKind, assignment, documentRange, documentBlockId, selectedDocumentTextPreview));
    }

    public RemovalOutcome removePrimaryAssignment(ProjectSession session, ScriptTextRange selectedRange) {
        Objects.requireNonNull(session, "session");
        Objects.requireNonNull(selectedRange, "selectedRange");
        Optional<NarrativeLayerAssignment> removable = session.project().narrativeLayerAssignments().stream()
                .filter(NarrativeLayerAssignment::primaryNarrationLayer)
                .filter(assignment -> assignment.textRange().segmentId().equals(selectedRange.segmentId()))
                .filter(assignment -> assignment.textRange().startOffset() < selectedRange.endOffset()
                        && selectedRange.startOffset() < assignment.textRange().endOffset())
                .findFirst();
        if (removable.isEmpty()) {
            return RemovalOutcome.notFound("El rango seleccionado no tiene voz o audio principal para quitar.");
        }
        session.replaceProject(session.project().withoutNarrativeLayerAssignment(removable.get().id()), true);
        return RemovalOutcome.removed(removable.get(), "Capa principal quitada: " + removable.get().displayName()
                + ". Ahora puedes asignar otra voz o audio.");
    }


    public RemovalOutcome removeFirstAssignmentOfKind(ProjectSession session, ScriptTextRange selectedRange, NarrativeLayerKind kind) {
        Objects.requireNonNull(session, "session");
        Objects.requireNonNull(selectedRange, "selectedRange");
        NarrativeLayerKind normalizedKind = kind == null ? NarrativeLayerKind.NOTE : kind;
        Optional<NarrativeLayerAssignment> removable = session.project().narrativeLayerAssignments().stream()
                .filter(assignment -> assignment.kind() == normalizedKind)
                .filter(assignment -> assignment.textRange().segmentId().equals(selectedRange.segmentId()))
                .filter(assignment -> assignment.textRange().startOffset() < selectedRange.endOffset()
                        && selectedRange.startOffset() < assignment.textRange().endOffset())
                .findFirst();
        if (removable.isEmpty()) {
            return RemovalOutcome.notFound("El rango seleccionado no tiene "
                    + normalizedKind.displayName().toLowerCase(java.util.Locale.ROOT) + " para quitar.");
        }
        session.replaceProject(session.project().withoutNarrativeLayerAssignment(removable.get().id()), true);
        return RemovalOutcome.removed(removable.get(), "Capa quitada: " + removable.get().kind().displayName()
                + " → " + removable.get().displayName() + ".");
    }

    public Optional<NarrativeLayerAssignment> findAssignment(ProjectSession session, String assignmentId) {
        Objects.requireNonNull(session, "session");
        String normalized = assignmentId == null ? "" : assignmentId.strip();
        if (normalized.isBlank()) {
            return Optional.empty();
        }
        return session.project().narrativeLayerAssignments().stream()
                .filter(assignment -> assignment.id().equals(normalized))
                .findFirst();
    }

    public List<DocumentLayerAssignmentPresentation> presentations(ProjectSession session) {
        Objects.requireNonNull(session, "session");
        return session.project().narrativeLayerAssignments().stream()
                .map(DocumentLayerAssignmentPresentation::from)
                .toList();
    }

    public Optional<ScriptTextRange> scriptRangeForLayer(Optional<NarrationSegment> linked,
                                                         DocumentTextRange documentRange,
                                                         String selectedDocumentTextPreview) {
        if (linked == null || linked.isEmpty()) {
            return Optional.empty();
        }
        NarrationSegment segment = linked.get();
        if (documentRange == null) {
            return Optional.of(new ScriptTextRange(segment.id(), 0, segment.narrationText().length()));
        }
        int start = Math.min(documentRange.startOffset(), segment.narrationText().length());
        int end = Math.min(Math.max(documentRange.endOffset(), start), segment.narrationText().length());
        String preview = selectedDocumentTextPreview == null ? "" : selectedDocumentTextPreview.strip();
        if (!preview.isBlank()) {
            int index = segment.narrationText().indexOf(preview);
            if (index >= 0) {
                start = index;
                end = index + preview.length();
            }
        }
        return Optional.of(new ScriptTextRange(segment.id(), start, end));
    }

    private static String successMessage(NarrativeLayerKind kind,
                                         NarrativeLayerAssignment assignment,
                                         DocumentTextRange documentRange,
                                         String documentBlockId,
                                         String selectedDocumentTextPreview) {
        String anchor = documentRange == null
                ? (documentBlockId == null || documentBlockId.isBlank() ? assignment.textRange().segmentId() : documentBlockId)
                : documentRange.displayLabel() + " · “" + (selectedDocumentTextPreview == null ? "" : selectedDocumentTextPreview) + "”";
        return "Capa asignada: " + kind.displayName() + " → " + assignment.displayName()
                + " sobre " + anchor + ". Se guardó en el proyecto, no en el documento fuente.";
    }


    private static TextAnchor buildRichAnchor(String assignmentId, ReadableDocument document, DocumentTextRange documentRange) {
        if (document == null || documentRange == null) {
            return null;
        }
        try {
            return TextAnchor.fromDocumentSelection("ANCH-" + safeAssetToken(assignmentId), document, documentRange,
                    SourceDocumentSnapshot.from(document).contentHash());
        } catch (IllegalArgumentException ex) {
            return TextAnchor.legacy(assignmentId, documentRange);
        }
    }

    private static String assignmentId(NarrativeLayerKind kind, ScriptTextRange range) {
        return "NLA-" + kind.name() + "-" + safeAssetToken(range.segmentId()) + "-" + range.startOffset() + "-" + range.endOffset();
    }

    private static String safeAssetToken(String value) {
        String normalized = value == null ? "SEG" : value.strip().toUpperCase(java.util.Locale.ROOT);
        normalized = normalized.replaceAll("[^A-Z0-9._-]", "-").replaceAll("-+", "-");
        return normalized.isBlank() ? "SEG" : normalized;
    }

    public static String toneStatus(VoiceToneReferenceResolution resolution) {
        if (resolution == null) return "Sin tono resuelto.";
        if (!resolution.available()) return resolution.userMessage();
        if (resolution.fallbackToNeutral()) return "No hay muestra " + resolution.requestedTone().displayName() + "; se usará Neutral.";
        return "Tono asignado: " + resolution.resolvedTone().displayName() + ".";
    }

    public static boolean projectImageAssetInUse(DocuPodcastProject project, StoryboardDocument storyboard, String assetId) {
        boolean usedByDocumentLayer = project.narrativeLayerAssignments().stream().anyMatch(layer -> assetId.equals(layer.targetId()));
        boolean usedByStoryboard = storyboard != null && !storyboard.bindingsForImage(assetId).isEmpty();
        boolean usedByTheatreCharacters = project.theatre().characterImages().stream().anyMatch(image -> assetId.equals(image.assetId()));
        boolean usedByTheatreFragments = project.theatre().intervencionesVisuales().stream().anyMatch(image -> assetId.equals(image.assetId()));
        boolean usedByTheatreObjects = project.theatre().objectImages().stream().anyMatch(image -> assetId.equals(image.assetId()));
        return usedByDocumentLayer || usedByStoryboard || usedByTheatreCharacters || usedByTheatreFragments || usedByTheatreObjects;
    }

    public Optional<NarrativeLayerAssignment> findAssignmentOfKind(ProjectSession session, ScriptTextRange selectedRange, NarrativeLayerKind kind) {
        return session.project().narrativeLayerAssignments().stream()
                .filter(assignment -> assignment.kind() == kind)
                .filter(assignment -> assignment.textRange().segmentId().equals(selectedRange.segmentId()))
                .filter(assignment -> assignment.textRange().startOffset() < selectedRange.endOffset()
                        && selectedRange.startOffset() < assignment.textRange().endOffset())
                .findFirst();
    }

    public static List<NarrativeLayerAssignment> filterAddEmotions(
            DocuPodcastProject project, List<NarrativeLayerAssignment> emotions) {
        if (emotions == null || emotions.isEmpty()) return project.narrativeLayerAssignments();
        List<String> segmentIds = emotions.stream()
                .map(a -> a.textRange().segmentId())
                .filter(java.util.Objects::nonNull)
                .toList();
        List<NarrativeLayerAssignment> base = project.narrativeLayerAssignments().stream()
                .filter(a -> a.kind() != NarrativeLayerKind.EMOTION || !segmentIds.contains(a.textRange().segmentId()))
                .toList();
        List<NarrativeLayerAssignment> merged = new java.util.ArrayList<>(base);
        merged.addAll(emotions);
        return List.copyOf(merged);
    }

    public static List<NarrativeLayerAssignment> filterAddTheatreManifestLayers(
            DocuPodcastProject project,
            List<NarrativeLayerAssignment> emotions,
            List<NarrativeLayerAssignment> images) {
        List<NarrativeLayerAssignment> merged = new java.util.ArrayList<>(filterAddEmotions(project, emotions));
        if (images == null || images.isEmpty()) {
            return List.copyOf(merged);
        }
        java.util.Set<String> imageSegmentIds = images.stream()
                .map(NarrativeLayerAssignment::textRange)
                .filter(Objects::nonNull)
                .map(ScriptTextRange::segmentId)
                .collect(java.util.stream.Collectors.toSet());
        merged = new java.util.ArrayList<>(merged.stream()
                .filter(layer -> layer.kind() != NarrativeLayerKind.IMAGE
                        || !imageSegmentIds.contains(layer.textRange().segmentId()))
                .toList());
        merged.addAll(images);
        return List.copyOf(merged);
    }

    public int removeAllSpecificVoices(ProjectSession session) {
        DocuPodcastProject project = session.project();
        List<NarrativeLayerAssignment> kept = project.narrativeLayerAssignments().stream()
                .filter(layer -> layer.kind() != NarrativeLayerKind.VOICE && layer.kind() != NarrativeLayerKind.EMOTION)
                .toList();
        int removed = project.narrativeLayerAssignments().size() - kept.size();
        if (removed > 0) {
            session.replaceProject(project.withNarrativeLayerAssignments(kept), true);
        }
        return removed;
    }

    public record AssignmentOutcome(boolean assigned, NarrativeLayerAssignment assignment, String message) {
        public static AssignmentOutcome assigned(NarrativeLayerAssignment assignment, String message) {
            return new AssignmentOutcome(true, Objects.requireNonNull(assignment, "assignment"), message == null ? "" : message);
        }

        public static AssignmentOutcome conflict(String message) {
            return new AssignmentOutcome(false, null, message == null ? "" : message);
        }

        public static AssignmentOutcome missingTarget(String message) {
            return new AssignmentOutcome(false, null, message == null ? "" : message);
        }
    }

    public record RemovalOutcome(boolean removed, NarrativeLayerAssignment assignment, String message) {
        public static RemovalOutcome removed(NarrativeLayerAssignment assignment, String message) {
            return new RemovalOutcome(true, Objects.requireNonNull(assignment, "assignment"), message == null ? "" : message);
        }

        public static RemovalOutcome notFound(String message) {
            return new RemovalOutcome(false, null, message == null ? "" : message);
        }
    }
}
