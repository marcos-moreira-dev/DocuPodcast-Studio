package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfSemanticTextLayer;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;

import java.util.Optional;
import java.util.List;

/** Resolves an audio cue to its exact PDF sentence or object highlight. */
public final class ResolvePdfPlaybackHighlightUseCase {

    public Optional<PdfVisualTextHighlight> resolve(
            PdfVisualReadingProjection projection,
            NarrationSegment segment,
            String regionId,
            String unitId) {
        if (notPlaybackLeaf(segment)) return Optional.empty();
        Optional<PdfVisualTextTarget> target = resolveTarget(
                projection, segment, regionId, unitId);
        if (target.isPresent()) return target.map(PdfVisualTextTarget::highlight);
        String authoritativeRegionId = authoritativeRegionId(segment, regionId);
        return projection == null ? Optional.empty()
                : projection.highlightForRegion(authoritativeRegionId);
    }

    /** Resolves the selectable owner of the cue, not only its paint overlay. */
    public Optional<PdfVisualTextTarget> resolveTarget(
            PdfVisualReadingProjection projection,
            NarrationSegment segment,
            String regionId,
            String unitId) {
        if (projection == null) {
            return Optional.empty();
        }
        if (notPlaybackLeaf(segment)) {
            return Optional.empty();
        }
        var binding = segment == null ? Optional
                .<com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfNarrationBinding>empty()
                : PdfNarrationBindingMetadata.decode(segment.metadata().get(
                        PdfNarrationBindingMetadata.KEY));
        String authoritativeRegionId = authoritativeRegionId(segment, regionId);
        if (authoritativeRegionId.isBlank()) return Optional.empty();
        Optional<com.marcosmoreiradev.docupodcaststudio.domain.document.pdf
                .PdfNarrationFragmentBinding> fragment = segment == null ? Optional.empty()
                : PdfNarrationFragmentBindingMetadata.resolve(segment.metadata().get(
                        PdfNarrationFragmentBindingMetadata.KEY), unitId);
        if (fragment.isPresent()) {
            var value = fragment.get();
            List<PdfPageRegion> boxes = value.playbackBboxes().stream().map(box ->
                    new PdfPageRegion(value.pageNumber(), box.xMin(), box.yMin(),
                            box.xMax(), box.yMax(), box.pageWidth(), box.pageHeight())).toList();
            if (!boxes.isEmpty()) {
                PdfPageRegion union = union(boxes);
                return Optional.of(new PdfVisualTextTarget(value.unitId(), value.regionId(),
                        value.sourceTextStart(), value.sourceTextEnd(), value.pageNumber(),
                        union, boxes, List.of(value.sourceBlockId()), value.sourceText(),
                        PdfTextLayerOrigin.NATIVE_BBOX, PdfVisualTextTargetKind.SENTENCE));
            }
        }
        boolean literal = binding.map(value -> value.sourceLayer()
                == PdfSemanticTextLayer.LITERAL).orElse(false);
        int sentenceIndex = unitIndex(unitId);
        if (literal && sentenceIndex >= 0) {
            Optional<PdfVisualTextTarget> sentence = projection
                    .sentenceTargetForRegion(authoritativeRegionId, sentenceIndex);
            if (sentence.isPresent()) return sentence;
        }
        return projection.targetForRegion(authoritativeRegionId);
    }

    private static PdfPageRegion union(List<PdfPageRegion> boxes) {
        PdfPageRegion first = boxes.getFirst();
        return new PdfPageRegion(first.pageNumber(),
                boxes.stream().mapToDouble(PdfPageRegion::xMinPoints).min().orElse(first.xMinPoints()),
                boxes.stream().mapToDouble(PdfPageRegion::yMinPoints).min().orElse(first.yMinPoints()),
                boxes.stream().mapToDouble(PdfPageRegion::xMaxPoints).max().orElse(first.xMaxPoints()),
                boxes.stream().mapToDouble(PdfPageRegion::yMaxPoints).max().orElse(first.yMaxPoints()),
                first.pageWidthPoints(), first.pageHeightPoints());
    }

    public static String authoritativeRegionId(
            NarrationSegment segment, String requestedRegionId) {
        String requested = requestedRegionId == null ? "" : requestedRegionId.strip();
        if (segment == null) return requested;
        String primary = segment.metadata().getOrDefault("sourceBlockId", "").strip();
        return PdfNarrationBindingMetadata.decode(segment.metadata().get(
                        PdfNarrationBindingMetadata.KEY))
                .map(binding -> {
                    if (!primary.isBlank()
                            && binding.sourceRegionIds().contains(primary)) return primary;
                    if (!requested.isBlank()
                            && binding.sourceRegionIds().contains(requested)) return requested;
                    return binding.sourceRegionIds().stream()
                            .filter(id -> id != null && !id.isBlank())
                            .findFirst().orElse(requested);
                }).orElse(requested);
    }

    /** Primary visual owner; covered/secondary region ids never replace it. */
    public static String primaryRegionId(NarrationSegment segment) {
        return authoritativeRegionId(segment, "");
    }

    static int unitIndex(String unitId) {
        if (unitId == null) return -1;
        int marker = unitId.lastIndexOf("-U");
        if (marker < 0 || marker + 2 >= unitId.length()) return -1;
        try {
            return Math.max(-1,
                    Integer.parseInt(unitId.substring(marker + 2)) - 1);
        } catch (NumberFormatException invalid) {
            return -1;
        }
    }

    private static boolean notPlaybackLeaf(NarrationSegment segment) {
        return segment != null && (!Boolean.parseBoolean(segment.metadata()
                .getOrDefault("pdfPlaybackTarget", "true"))
                || "CONTAINER".equals(segment.metadata()
                .getOrDefault("pdfRegionRole", "LEAF")));
    }
}
