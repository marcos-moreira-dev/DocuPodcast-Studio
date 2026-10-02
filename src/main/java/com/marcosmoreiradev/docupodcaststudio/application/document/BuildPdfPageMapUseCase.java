package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentSentenceSplitter;
import com.marcosmoreiradev.docupodcaststudio.domain.document.TextAnchor;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfEvidence;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfNarratability;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfNarrationBinding;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfObjectNarrationPolicy;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfPageGeometry;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfPageMap;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfPageNode;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfPageNodeKind;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegion;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfReviewState;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfSemanticTextLayer;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfTextStructure;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PreparedPdfPage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/** Deterministically projects V3 evidence into the reversible textual PdfPageMap V1. */
public final class BuildPdfPageMapUseCase {
    public static final String BUILDER_SIGNATURE = "pdf-page-map-semantic-components-v2";

    public PdfPageMap build(PreparedPdfPage source, PdfPageMap previous) {
        if (source == null) throw new IllegalArgumentException("source page is required");
        PdfPageGeometry pageGeometry = new PdfPageGeometry(
                0, 0, source.widthPoints(), source.heightPoints(),
                source.widthPoints(), source.heightPoints(), PdfPageGeometry.CANONICAL_SPACE);
        PdfPageMapIdentityReconciler reconciler = new PdfPageMapIdentityReconciler(
                previous == null ? List.of() : previous.nodes());
        ArrayList<PdfPageNode> nodes = new ArrayList<>();
        ArrayList<PdfNarrationBinding> bindings = new ArrayList<>();
        String pageId = "PM-P%06d".formatted(source.pageNumber());
        ArrayList<String> blockIds = new ArrayList<>();
        List<PdfVisualObjectProposal> visualObjects = new DetectPdfVisualObjectsUseCase().detect(source);
        Map<String, PdfRegion> regionsById = new HashMap<>();
        source.regions().forEach(region -> regionsById.put(region.id(), region));
        HashSet<String> ownedRegionIds = new HashSet<>();
        visualObjects.forEach(object -> {
            ownedRegionIds.addAll(object.sourceRegionIds());
            ownedRegionIds.addAll(object.internalLabelRegionIds());
            if (!object.captionRegionId().isBlank()) ownedRegionIds.add(object.captionRegionId());
        });

        for (PdfRegion region : source.regions().stream()
                .sorted(java.util.Comparator.comparingInt(PdfRegion::effectiveReadingOrder)).toList()) {
            if (region.container() || !region.playbackTarget()) continue;
            if (ownedRegionIds.contains(region.id())) continue;
            PdfPageGeometry geometry = geometry(source, region.xMin(), region.yMin(), region.xMax(), region.yMax());
            String blockCandidateId = pageId + "-B%04d".formatted(region.effectiveReadingOrder() + 1);
            PdfPageNode blockCandidate = node(blockCandidateId, region.id(), pageId,
                    PdfPageNodeKind.BLOCK, region.effectiveType().name(), region.effectiveReadingOrder(),
                    geometry, List.of(geometry), region.effectiveText(), 0, region.effectiveText().length(), -1, region);
            String blockId = reconciler.resolve(blockCandidate);
            blockIds.add(blockId);

            ArrayList<PdfPageNode> descendants = new ArrayList<>();
            ArrayList<String> childIds = new ArrayList<>();
            List<LineSpan> lines = lines(source, region, geometry);
            for (int lineIndex = 0; lineIndex < lines.size(); lineIndex++) {
                LineSpan line = lines.get(lineIndex);
                String lineId = blockId + "-L%03d".formatted(lineIndex + 1);
                PdfPageNode lineNode = node(lineId, region.id(), blockId, PdfPageNodeKind.LINE,
                        region.effectiveType().name(), lineIndex, line.geometry, List.of(line.geometry),
                        substring(region.effectiveText(), line.start, line.end), line.start, line.end, -1, region);
                descendants.add(lineNode);
                childIds.add(lineId);
            }
            for (var sentence : DocumentSentenceSplitter.split(region.id(), region.effectiveText())) {
                List<PdfPageGeometry> parts = sentenceParts(lines, sentence.range().startOffset(), sentence.range().endOffset());
                PdfPageGeometry sentenceGeometry = union(parts.isEmpty() ? List.of(geometry) : parts);
                String candidateId = blockId + "-S%03d".formatted(sentence.index() + 1);
                PdfPageNode candidate = node(candidateId, region.id(), blockId, PdfPageNodeKind.SENTENCE,
                        region.effectiveType().name(), sentence.index(), sentenceGeometry,
                        parts.isEmpty() ? List.of(geometry) : parts, sentence.text(),
                        sentence.range().startOffset(), sentence.range().endOffset(), sentence.index(), region);
                String sentenceId = reconciler.resolve(candidate);
                descendants.add(withId(candidate, sentenceId));
                childIds.add(sentenceId);
            }
            nodes.add(withChildren(withId(blockCandidate, blockId), childIds));
            nodes.addAll(descendants);
            if (region.effectiveNarratability() == PdfNarratability.NARRATABLE && !region.effectiveText().isBlank()) {
                bindings.add(new PdfNarrationBinding(source.pageNumber(), List.of(region.id()),
                        PdfSemanticTextLayer.LITERAL, PdfObjectNarrationPolicy.READ_EXACT, "",
                        region.revision(), TextAnchor.sha256(region.effectiveText())));
            }
        }
        for (PdfVisualObjectProposal proposal : visualObjects) {
            PdfPageGeometry proposedGeometry = geometry(source,
                    proposal.geometry().xMinPoints(), proposal.geometry().yMinPoints(),
                    proposal.geometry().xMaxPoints(), proposal.geometry().yMaxPoints());
            PdfPageNode previousObject = previous == null ? null : previous.node(proposal.id()).orElse(null);
            boolean manual = previousObject != null && previousObject.reviewState().humanOverride();
            PdfPageGeometry objectGeometry = manual ? previousObject.geometry() : proposedGeometry;
            String semanticType = manual ? previousObject.semanticType() : proposal.type().name();
            ArrayList<String> children = new ArrayList<>();
            ArrayList<PdfPageNode> ownedNodes = new ArrayList<>();
            HashSet<String> representedRegionIds = new HashSet<>();
            String primarySourceId = proposal.sourceRegionIds().isEmpty()
                    ? "" : proposal.sourceRegionIds().getFirst();
            if (!primarySourceId.isBlank()) representedRegionIds.add(primarySourceId);
            for (String sourceId : proposal.sourceRegionIds()) {
                PdfRegion sourceRegion = regionsById.get(sourceId);
                if (sourceRegion == null || !representedRegionIds.add(sourceId)) continue;
                String id = proposal.id() + "-SOURCE-" + sourceId;
                PdfPageGeometry sourceGeometry = geometry(source,
                        sourceRegion.xMin(), sourceRegion.yMin(),
                        sourceRegion.xMax(), sourceRegion.yMax());
                ownedNodes.add(node(id, sourceRegion.id(), proposal.id(),
                        PdfPageNodeKind.INTERNAL_LABEL, "OBJECT_SOURCE",
                        sourceRegion.effectiveReadingOrder(), sourceGeometry,
                        List.of(sourceGeometry), sourceRegion.effectiveText(), 0,
                        sourceRegion.effectiveText().length(), -1, sourceRegion));
                children.add(id);
            }
            for (String labelId : proposal.internalLabelRegionIds()) {
                PdfRegion label = regionsById.get(labelId);
                if (label == null || !representedRegionIds.add(labelId)) continue;
                String id = proposal.id() + "-LABEL-" + labelId;
                PdfPageGeometry labelGeometry = geometry(source, label.xMin(), label.yMin(), label.xMax(), label.yMax());
                ownedNodes.add(node(id, label.id(), proposal.id(), PdfPageNodeKind.INTERNAL_LABEL,
                        "INTERNAL_LABEL", label.effectiveReadingOrder(), labelGeometry, List.of(labelGeometry),
                        label.effectiveText(), 0, label.effectiveText().length(), -1, label));
                children.add(id);
            }
            if (!proposal.captionRegionId().isBlank()) {
                PdfRegion caption = regionsById.get(proposal.captionRegionId());
                if (caption != null
                        && representedRegionIds.add(caption.id())) {
                    String id = proposal.id() + "-CAPTION";
                    PdfPageGeometry captionGeometry = geometry(source, caption.xMin(), caption.yMin(), caption.xMax(), caption.yMax());
                    ownedNodes.add(node(id, caption.id(), proposal.id(), PdfPageNodeKind.CAPTION,
                            "CAPTION", caption.effectiveReadingOrder(), captionGeometry, List.of(captionGeometry),
                            caption.effectiveText(), 0, caption.effectiveText().length(), -1, caption));
                    children.add(id);
                }
            }
            String sourceText = proposal.sourceRegionIds().stream().map(regionsById::get)
                    .filter(java.util.Objects::nonNull).map(PdfRegion::effectiveText)
                    .filter(value -> !value.isBlank()).reduce("", (a, b) -> a.isBlank() ? b : a + "\n" + b);
            PdfRegion caption = regionsById.get(proposal.captionRegionId());
            if (caption != null && !caption.effectiveText().isBlank()
                    && !sourceText.contains(caption.effectiveText())) {
                sourceText = sourceText.isBlank() ? caption.effectiveText()
                        : sourceText + "\n" + caption.effectiveText();
            }
            PdfPageNode objectNode = new PdfPageNode(proposal.id(),
                    proposal.sourceRegionIds().isEmpty() ? "" : proposal.sourceRegionIds().getFirst(),
                    pageId, PdfPageNodeKind.VISUAL_OBJECT, semanticType, blockIds.size(), objectGeometry,
                    List.of(objectGeometry), new PdfTextStructure(sourceText, 0, sourceText.length(), -1),
                    new PdfEvidence("VISUAL_PROPOSAL", proposal.confidence(), proposal.detector()),
                    manual ? previousObject.reviewState() : new PdfReviewState(false, "PROPOSED"), children);
            nodes.add(objectNode);
            nodes.addAll(ownedNodes);
            blockIds.add(objectNode.id());
        }
        PdfPageNode pageNode = new PdfPageNode(pageId, "", "", PdfPageNodeKind.PAGE,
                "PAGE", 0, pageGeometry, List.of(pageGeometry),
                new PdfTextStructure("", 0, 0, -1),
                new PdfEvidence("PAGE", 1.0, BUILDER_SIGNATURE),
                new PdfReviewState(false, "UNREVIEWED"), blockIds);
        nodes.addFirst(pageNode);
        return new PdfPageMap(PdfPageMap.CURRENT_SCHEMA_VERSION, source.pageNumber(),
                source.revision(), BUILDER_SIGNATURE, pageGeometry, nodes, bindings);
    }

    private static PdfPageNode node(String id, String legacyId, String parentId, PdfPageNodeKind kind,
                                    String semanticType, int order, PdfPageGeometry geometry,
                                    List<PdfPageGeometry> parts, String text, int start, int end,
                                    int sentenceIndex, PdfRegion region) {
        return new PdfPageNode(id, legacyId, parentId, kind, semanticType, order, geometry, parts,
                new PdfTextStructure(text, start, end, sentenceIndex),
                new PdfEvidence(region.evidence().origin().name(), region.evidence().confidence(),
                        region.evidence().extractorVersion() + "|" + region.evidence().parserVersion()),
                new PdfReviewState(!region.override().emptyOverride(),
                        region.override().emptyOverride() ? "UNREVIEWED" : "HUMAN_OVERRIDE"), List.of());
    }

    private static PdfPageNode withId(PdfPageNode node, String id) {
        return new PdfPageNode(id, node.legacyRegionId(), node.parentId(), node.kind(), node.semanticType(),
                node.order(), node.geometry(), node.geometryParts(), node.text(), node.evidence(),
                node.reviewState(), node.childIds());
    }

    private static PdfPageNode withChildren(PdfPageNode node, List<String> children) {
        return new PdfPageNode(node.id(), node.legacyRegionId(), node.parentId(), node.kind(), node.semanticType(),
                node.order(), node.geometry(), node.geometryParts(), node.text(), node.evidence(),
                node.reviewState(), children);
    }

    private static List<LineSpan> lines(PreparedPdfPage page, PdfRegion region, PdfPageGeometry fallback) {
        if (!region.effectiveText().equals(region.text())) return List.of();
        String[] boxes = region.attributes().getOrDefault("lineBboxes", "").split(";");
        String[] ranges = region.attributes().getOrDefault("lineCharRanges", "").split(";");
        if (boxes.length == 0 || boxes.length != ranges.length) return List.of();
        ArrayList<LineSpan> lines = new ArrayList<>();
        try {
            for (int index = 0; index < boxes.length; index++) {
                String[] coordinates = boxes[index].split(",", 4);
                String[] range = ranges[index].split("-", 2);
                if (coordinates.length != 4 || range.length != 2) return List.of();
                lines.add(new LineSpan(Integer.parseInt(range[0]), Integer.parseInt(range[1]),
                        geometry(page, Double.parseDouble(coordinates[0]), Double.parseDouble(coordinates[1]),
                                Double.parseDouble(coordinates[2]), Double.parseDouble(coordinates[3]))));
            }
            return List.copyOf(lines);
        } catch (IllegalArgumentException malformed) {
            return List.of();
        }
    }

    private static List<PdfPageGeometry> sentenceParts(List<LineSpan> lines, int start, int end) {
        return lines.stream().filter(line -> line.end > start && line.start < end).map(LineSpan::geometry).toList();
    }

    private static PdfPageGeometry union(List<PdfPageGeometry> parts) {
        PdfPageGeometry first = parts.getFirst();
        return new PdfPageGeometry(parts.stream().mapToDouble(PdfPageGeometry::xMin).min().orElse(first.xMin()),
                parts.stream().mapToDouble(PdfPageGeometry::yMin).min().orElse(first.yMin()),
                parts.stream().mapToDouble(PdfPageGeometry::xMax).max().orElse(first.xMax()),
                parts.stream().mapToDouble(PdfPageGeometry::yMax).max().orElse(first.yMax()),
                first.pageWidth(), first.pageHeight(), first.coordinateSpace());
    }

    private static PdfPageGeometry geometry(PreparedPdfPage page, double x1, double y1, double x2, double y2) {
        return new PdfPageGeometry(x1, y1, x2, y2, page.widthPoints(), page.heightPoints(), PdfPageGeometry.CANONICAL_SPACE);
    }

    private static String substring(String text, int start, int end) {
        int safeStart = Math.max(0, Math.min(text.length(), start));
        int safeEnd = Math.max(safeStart, Math.min(text.length(), end));
        return text.substring(safeStart, safeEnd);
    }

    private record LineSpan(int start, int end, PdfPageGeometry geometry) {
    }
}
