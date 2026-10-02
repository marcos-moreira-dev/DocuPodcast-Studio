package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentProcessingInterval;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentProcessingIntervalUnit;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentProcessingScope;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

/** Resolves document scope once and carries its exact narration projection downstream. */
public final class ResolveDocumentProcessingSelectionUseCase {
    public ResolvedDocumentProcessingSelection execute(
            NarrationScriptDocument canonical,
            DocumentProcessingScope scope,
            DocumentProcessingInterval interval,
            String anchorSegmentId,
            String sourceDocumentId,
            String sourceSha) {
        Objects.requireNonNull(canonical, "canonical narration");
        DocumentProcessingScope safeScope = Objects.requireNonNullElse(
                scope, DocumentProcessingScope.FROM_SELECTION);
        List<NarrationSegment> selected;
        List<Integer> pages;
        int requestedStart = 0;
        int requestedEnd = 0;
        DocumentProcessingIntervalUnit unit = null;
        switch (safeScope) {
            case FULL_DOCUMENT -> {
                selected = canonical.segments();
                pages = sourcePages(selected);
            }
            case INTERVAL -> {
                if (interval == null) {
                    throw new IllegalArgumentException("INTERVAL requiere un intervalo valido");
                }
                unit = interval.unit();
                requestedStart = interval.start();
                requestedEnd = interval.end();
                if (unit == DocumentProcessingIntervalUnit.PAGE) {
                    pages = interval.inclusiveIndexes();
                    selected = canonical.segments().stream()
                            .filter(segment -> pages.contains(sourcePage(segment))).toList();
                } else if (unit == DocumentProcessingIntervalUnit.BLOCK) {
                    List<String> blocks = sourceBlocks(canonical.segments());
                    interval.validatedAgainst(blocks.size());
                    List<String> selectedBlocks = blocks.subList(
                            interval.start() - 1, interval.end());
                    selected = canonical.segments().stream()
                            .filter(segment -> segment.sourceBlockIds().stream()
                                    .anyMatch(selectedBlocks::contains))
                            .toList();
                    pages = sourcePages(selected);
                } else {
                    throw new IllegalArgumentException(
                            "Unidad de intervalo documental no soportada: " + unit);
                }
            }
            case SINGLE_FRAGMENT -> {
                int anchor = anchorIndex(canonical, anchorSegmentId);
                selected = List.of(canonical.segments().get(anchor));
                pages = sourcePages(selected);
            }
            case FROM_SELECTION -> {
                int anchor = anchorIndex(canonical, anchorSegmentId);
                selected = canonical.segments().subList(anchor, canonical.segments().size());
                pages = sourcePages(selected);
            }
            default -> throw new IllegalStateException("Alcance documental no soportado: " + safeScope);
        }
        NarrationScriptDocument projection = new NarrationScriptDocument(
                canonical.id(), canonical.title(), canonical.language(),
                canonical.sourceDocumentTitle(), selected, canonical.createdAt(),
                canonical.updatedAt(), canonical.notes());
        List<String> segmentIds = selected.stream().map(NarrationSegment::id).toList();
        String revision = revision(safeScope, requestedStart, requestedEnd, pages,
                segmentIds, sourceDocumentId, sourceSha);
        return new ResolvedDocumentProcessingSelection(safeScope, unit,
                requestedStart, requestedEnd, pages, segmentIds,
                sourceDocumentId, sourceSha, revision, projection);
    }

    private static int anchorIndex(NarrationScriptDocument script, String anchorId) {
        String requested = Objects.toString(anchorId, "").strip();
        if (requested.isBlank()) {
            throw new IllegalArgumentException("El alcance requiere un fragmento inicial");
        }
        for (int index = 0; index < script.segments().size(); index++) {
            if (script.segments().get(index).id().equals(requested)) return index;
        }
        throw new IllegalArgumentException("El fragmento inicial no pertenece al guion canonico: "
                + requested);
    }

    private static List<Integer> sourcePages(List<NarrationSegment> segments) {
        ArrayList<Integer> pages = new ArrayList<>();
        for (NarrationSegment segment : segments) {
            int page = sourcePage(segment);
            if (page > 0 && !pages.contains(page)) pages.add(page);
        }
        return List.copyOf(pages);
    }

    /** Ordered semantic Word blocks represented by the canonical narration. */
    public static List<String> sourceBlocks(List<NarrationSegment> segments) {
        LinkedHashSet<String> blocks = new LinkedHashSet<>();
        if (segments != null) {
            for (NarrationSegment segment : segments) {
                if (segment != null) {
                    segment.sourceBlockIds().stream()
                            .filter(id -> id != null && !id.isBlank())
                            .forEach(blocks::add);
                }
            }
        }
        return List.copyOf(blocks);
    }

    private static int sourcePage(NarrationSegment segment) {
        try {
            return Integer.parseInt(segment.metadata().getOrDefault("pdfSourcePage", "0"));
        } catch (NumberFormatException invalid) {
            return 0;
        }
    }

    private static String revision(DocumentProcessingScope scope, int start, int end,
                                   List<Integer> pages, List<String> segments,
                                   String sourceDocumentId, String sourceSha) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String material = scope.name() + "\n" + start + "\n" + end + "\n"
                    + pages + "\n" + segments + "\n"
                    + Objects.toString(sourceDocumentId, "") + "\n"
                    + Objects.toString(sourceSha, "");
            return "SEL-" + HexFormat.of().formatHex(digest.digest(
                    material.getBytes(StandardCharsets.UTF_8))).substring(0, 16)
                    .toUpperCase(java.util.Locale.ROOT);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}
