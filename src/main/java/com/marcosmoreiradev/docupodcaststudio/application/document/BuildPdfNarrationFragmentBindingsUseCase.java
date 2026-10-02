package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentSentenceSpan;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentSentenceSplitter;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfNarrationFragmentBinding;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfPageGeometry;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegion;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfSemanticTextLayer;

import java.util.ArrayList;
import java.util.List;

/** Builds fragment grounding once, while SOURCE and its geometry are still available. */
public final class BuildPdfNarrationFragmentBindingsUseCase {

    public List<PdfNarrationFragmentBinding> build(String segmentId, PdfRegion region,
                                                    String narrationText,
                                                    PdfSemanticTextLayer sourceLayer,
                                                    double pageWidth, double pageHeight) {
        String source = region.effectiveText();
        PdfPageGeometry regionBox = geometry(region.xMin(), region.yMin(), region.xMax(),
                region.yMax(), pageWidth, pageHeight);
        ArrayList<PdfNarrationFragmentBinding> result = new ArrayList<>();
        result.add(binding(segmentId, segmentId, region, 0, source.length(),
                List.of(), List.of(), List.of(regionBox), source,
                PdfNarrationFragmentBinding.GeometryAuthority.REGION_FALLBACK));

        List<DocumentSentenceSpan> narrationSpans = DocumentSentenceSplitter.split(
                segmentId, narrationText);
        List<DocumentSentenceSpan> sourceSpans = sourceLayer == PdfSemanticTextLayer.LITERAL
                ? DocumentSentenceSplitter.split(region.id(), source) : List.of();
        boolean stableSentenceMap = !sourceSpans.isEmpty()
                && sourceSpans.size() == narrationSpans.size();
        List<LineEvidence> lines = lineEvidence(region, pageWidth, pageHeight);
        List<PdfPageGeometry> explicitPlayback = playbackBboxes(region, pageWidth, pageHeight);

        for (int index = 0; index < narrationSpans.size(); index++) {
            String unitId = segmentId + "-U%03d".formatted(index + 1);
            if (!stableSentenceMap) {
                result.add(binding(unitId, segmentId, region, 0, source.length(),
                        List.of(), List.of(), List.of(regionBox), source,
                        PdfNarrationFragmentBinding.GeometryAuthority.REGION_FALLBACK));
                continue;
            }
            DocumentSentenceSpan span = sourceSpans.get(index);
            List<Integer> lineIndices = new ArrayList<>();
            List<PdfPageGeometry> boxes = new ArrayList<>();
            for (int lineIndex = 0; lineIndex < lines.size(); lineIndex++) {
                LineEvidence line = lines.get(lineIndex);
                if (line.end() <= span.range().startOffset()
                        || line.start() >= span.range().endOffset()) continue;
                lineIndices.add(lineIndex);
                boxes.add(line.box());
            }
            PdfNarrationFragmentBinding.GeometryAuthority authority;
            if (!boxes.isEmpty()) {
                authority = PdfNarrationFragmentBinding.GeometryAuthority.LINE_CHAR_RANGES;
            } else if (sourceSpans.size() == 1 && !explicitPlayback.isEmpty()) {
                boxes = explicitPlayback;
                authority = PdfNarrationFragmentBinding.GeometryAuthority.EXPLICIT_FOCUS;
            } else {
                boxes = List.of(regionBox);
                authority = PdfNarrationFragmentBinding.GeometryAuthority.REGION_FALLBACK;
            }
            result.add(binding(unitId, segmentId, region,
                    span.range().startOffset(), span.range().endOffset(), lineIndices,
                    List.of(), boxes, span.text(), authority));
        }
        return List.copyOf(result);
    }

    private static PdfNarrationFragmentBinding binding(
            String unitId, String segmentId, PdfRegion region, int start, int end,
            List<Integer> lineIndices, List<Integer> wordIndices,
            List<PdfPageGeometry> boxes, String sourceText,
            PdfNarrationFragmentBinding.GeometryAuthority authority) {
        return new PdfNarrationFragmentBinding(unitId, segmentId, region.id(), region.id(),
                region.pageNumber(), start, end, lineIndices, wordIndices, boxes,
                sourceText, authority);
    }

    private static List<LineEvidence> lineEvidence(PdfRegion region,
                                                   double pageWidth, double pageHeight) {
        if (!region.effectiveText().equals(region.text())) return List.of();
        String boxesValue = region.attributes().getOrDefault("lineBboxes", "");
        String rangesValue = region.attributes().getOrDefault("lineCharRanges", "");
        if (boxesValue.isBlank() || rangesValue.isBlank()) return List.of();
        String[] boxes = boxesValue.split(";");
        String[] ranges = rangesValue.split(";");
        if (boxes.length != ranges.length) return List.of();
        ArrayList<LineEvidence> result = new ArrayList<>();
        try {
            for (int index = 0; index < boxes.length; index++) {
                String[] c = boxes[index].split(",", 4);
                String[] r = ranges[index].split("-", 2);
                if (c.length != 4 || r.length != 2) return List.of();
                result.add(new LineEvidence(Integer.parseInt(r[0]), Integer.parseInt(r[1]),
                        geometry(Double.parseDouble(c[0]), Double.parseDouble(c[1]),
                                Double.parseDouble(c[2]), Double.parseDouble(c[3]),
                                pageWidth, pageHeight)));
            }
            return List.copyOf(result);
        } catch (RuntimeException malformed) {
            return List.of();
        }
    }

    private static List<PdfPageGeometry> playbackBboxes(PdfRegion region,
                                                         double pageWidth, double pageHeight) {
        String value = region.attributes().getOrDefault("playbackBboxes", "");
        if (value.isBlank()) return List.of();
        ArrayList<PdfPageGeometry> result = new ArrayList<>();
        try {
            for (String raw : value.split(";")) {
                String[] c = raw.split(",", 4);
                if (c.length != 4) return List.of();
                result.add(geometry(Double.parseDouble(c[0]), Double.parseDouble(c[1]),
                        Double.parseDouble(c[2]), Double.parseDouble(c[3]),
                        pageWidth, pageHeight));
            }
            return List.copyOf(result);
        } catch (RuntimeException malformed) {
            return List.of();
        }
    }

    private static PdfPageGeometry geometry(double x1, double y1, double x2, double y2,
                                             double pageWidth, double pageHeight) {
        return new PdfPageGeometry(x1, y1, x2, y2, pageWidth, pageHeight,
                PdfPageGeometry.CANONICAL_SPACE);
    }

    private record LineEvidence(int start, int end, PdfPageGeometry box) { }
}
