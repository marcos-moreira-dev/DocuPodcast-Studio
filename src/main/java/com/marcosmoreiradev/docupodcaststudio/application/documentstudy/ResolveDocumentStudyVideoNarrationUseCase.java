package com.marcosmoreiradev.docupodcaststudio.application.documentstudy;

import com.marcosmoreiradev.docupodcaststudio.application.document.DocumentContentProjection;
import com.marcosmoreiradev.docupodcaststudio.domain.reading.SecondarySemanticComponentKind;
import com.marcosmoreiradev.docupodcaststudio.domain.reading.SecondarySemanticReadingPolicy;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;
import com.marcosmoreiradev.docupodcaststudio.domain.study.DocumentStudyVideoConfiguration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Resolves the spoken subset of the configured documentary-video region. */
public final class ResolveDocumentStudyVideoNarrationUseCase {
    private final DocumentStudyVideoContentResolver contentResolver;

    public ResolveDocumentStudyVideoNarrationUseCase() {
        this(new DocumentStudyVideoContentResolver());
    }

    ResolveDocumentStudyVideoNarrationUseCase(DocumentStudyVideoContentResolver contentResolver) {
        this.contentResolver = Objects.requireNonNull(contentResolver, "contentResolver");
    }

    public NarrationScriptDocument execute(
            DocumentContentProjection projection,
            DocumentStudyVideoConfiguration configuration,
            SecondarySemanticReadingPolicy readingPolicy,
            NarrationScriptDocument scopedNarration) {
        Objects.requireNonNull(projection, "projection");
        Objects.requireNonNull(scopedNarration, "scopedNarration");
        if (scopedNarration.empty()) return scopedNarration;

        SecondarySemanticReadingPolicy policy = Objects.requireNonNullElse(
                readingPolicy, SecondarySemanticReadingPolicy.INTERPRET_ALL_BRIEF);
        List<DocumentStudyVideoContentResolver.Item> content = contentResolver.resolve(
                projection, configuration, policy);
        Map<String, List<NarrationSegment>> bySource = segmentsBySource(scopedNarration);
        LinkedHashSet<String> admittedIds = new LinkedHashSet<>();
        for (DocumentStudyVideoContentResolver.Item item : content) {
            if (!item.enabled() || !spokenContentAllowed(item, policy)) continue;
            for (NarrationSegment segment : segmentsForItem(item, scopedNarration, bySource)) {
                admittedIds.add(segment.id());
            }
        }
        List<NarrationSegment> admitted = scopedNarration.segments().stream()
                .filter(segment -> admittedIds.contains(segment.id()))
                .toList();
        return new NarrationScriptDocument(scopedNarration.id(), scopedNarration.title(),
                scopedNarration.language(), scopedNarration.sourceDocumentTitle(), admitted,
                scopedNarration.createdAt(), scopedNarration.updatedAt(), scopedNarration.notes());
    }

    private static boolean spokenContentAllowed(
            DocumentStudyVideoContentResolver.Item item,
            SecondarySemanticReadingPolicy policy) {
        if (!item.content().secondarySemanticComponent()) return true;
        SecondarySemanticComponentKind kind = switch (item.content().kind()) {
            case TABLE -> SecondarySemanticComponentKind.TABLE;
            case EQUATION -> SecondarySemanticComponentKind.EQUATION;
            case IMAGE -> SecondarySemanticComponentKind.IMAGE;
            case EXTRA -> SecondarySemanticComponentKind.EXTRA;
            default -> SecondarySemanticComponentKind.NONE;
        };
        // A visual without admitted narration remains a silent slide and must not
        // become an acoustic requirement.
        return policy.includes(kind) && item.content().narratable();
    }

    private static List<NarrationSegment> segmentsForItem(
            DocumentStudyVideoContentResolver.Item item,
            NarrationScriptDocument script,
            Map<String, List<NarrationSegment>> bySource) {
        List<NarrationSegment> direct = item.content().narrationSegmentIds().stream()
                .map(script::segmentById)
                .flatMap(java.util.Optional::stream)
                .filter(NarrationSegment::narratable)
                .toList();
        if (!direct.isEmpty()) return direct;
        LinkedHashSet<NarrationSegment> fallback = new LinkedHashSet<>();
        for (String sourceId : item.content().sourceIds()) {
            fallback.addAll(bySource.getOrDefault(sourceId, List.of()));
        }
        if (fallback.isEmpty()) {
            fallback.addAll(bySource.getOrDefault(item.content().contentId(), List.of()));
        }
        return fallback.stream().filter(NarrationSegment::narratable).toList();
    }

    private static Map<String, List<NarrationSegment>> segmentsBySource(
            NarrationScriptDocument script) {
        LinkedHashMap<String, ArrayList<NarrationSegment>> mutable = new LinkedHashMap<>();
        for (NarrationSegment segment : script.segments()) {
            if (!segment.narratable()) continue;
            for (String sourceId : segment.sourceBlockIds()) {
                mutable.computeIfAbsent(sourceId, ignored -> new ArrayList<>()).add(segment);
            }
        }
        LinkedHashMap<String, List<NarrationSegment>> result = new LinkedHashMap<>();
        mutable.forEach((sourceId, segments) -> result.put(sourceId, List.copyOf(segments)));
        return result;
    }
}
