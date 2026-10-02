package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfNarratability;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegion;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionOrigin;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PreparedPdfPage;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfPageMap;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfPageNode;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfPageNodeKind;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentSentenceSplitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Builds visual highlights directly from canonical PDF V2 pages. */
public final class BuildPdfVisualReadingProjectionUseCase {
    private final PreparedPdfDocumentRepository repository;
    private final ResolvePdfPageMapUseCase pageMapResolver;
    private final SecondarySemanticComponentClassifier semanticClassifier =
            new SecondarySemanticComponentClassifier();
    private final PdfTextVisualBoundsResolver textVisualBounds =
            new PdfTextVisualBoundsResolver();

    public BuildPdfVisualReadingProjectionUseCase(PreparedPdfDocumentRepository repository) {
        this(repository, null);
    }

    public BuildPdfVisualReadingProjectionUseCase(PreparedPdfDocumentRepository repository,
                                                   ResolvePdfPageMapUseCase pageMapResolver) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.pageMapResolver = pageMapResolver;
    }

    public PdfVisualReadingProjection build(PreparedPdfWorkspaceRef workspace) {
        if (workspace == null) {
            return new PdfVisualReadingProjection(List.of(), Map.of(),
                    List.of("No hay PDF V2 activo."));
        }
        try {
            if (pageMapResolver != null) {
                return buildFromPageMaps(workspace);
            }
            List<PreparedPdfPage> pages = repository.loadPages(workspace.projectRoot());
            ArrayList<PdfTextLayer> layers = new ArrayList<>();
            Map<String, PdfVisualTextHighlight> highlights = new LinkedHashMap<>();
            ArrayList<PdfVisualTextTarget> targets = new ArrayList<>();
            for (PreparedPdfPage page : pages) {
                ArrayList<PdfTextLine> lines = new ArrayList<>();
                for (PdfRegion region : page.regions()) {
                    // Structural parents remain canonical hierarchy only. They
                    // deliberately have no playback/highlight representation.
                    if (region.container() || !region.playbackTarget()) continue;
                    PdfPageRegion box = new PdfPageRegion(page.pageNumber(), region.xMin(), region.yMin(),
                            region.xMax(), region.yMax(), page.widthPoints(), page.heightPoints());
                    PdfTextVisualBoundsResolver.Resolution visual =
                            textVisualBounds.resolve(region, box);
                    List<PdfPageRegion> playbackBoxes = visual.source()
                            == PdfTextVisualBoundsResolver.Source.OCR_WORDS
                            ? List.of(visual.tightTextBBox())
                            : PdfTextVisualBoundsResolver.textualType(region)
                            ? List.of(box) : playbackBoxes(region, box);
                    PdfTextLayerOrigin origin = visual.source()
                            == PdfTextVisualBoundsResolver.Source.OCR_WORDS
                            ? PdfTextLayerOrigin.OCR_LOCAL
                            : PdfTextVisualBoundsResolver.textualType(region)
                            ? PdfTextLayerOrigin.NATIVE_BBOX
                            : origin(region.evidence().origin());
                    lines.add(new PdfTextLine(page.pageNumber(), region.effectiveText(), box,
                            List.of(), region.evidence().confidence()));
                    highlights.put(region.id(), new PdfVisualTextHighlight(
                            page.pageNumber(), playbackBoxes, region.effectiveText(), origin));
                    boolean semantic = semanticClassifier.classify(region).secondary();
                    targets.add(new PdfVisualTextTarget(
                            region.id() + (semantic ? "-C" : "-B"), region.id(),
                            0, region.effectiveText().length(), page.pageNumber(), box,
                            playbackBoxes, List.of(region.id()), region.effectiveText(), origin,
                            semantic ? PdfVisualTextTargetKind.SEMANTIC_COMPONENT
                                    : PdfVisualTextTargetKind.BLOCK));
                    if (!semantic && region.effectiveNarratability() == PdfNarratability.NARRATABLE) {
                        targets.addAll(sentenceTargets(region, box, origin));
                    }
                }
                PdfTextLayerOrigin pageOrigin = page.regions().stream()
                        .filter(PdfRegion::playbackTarget)
                        .map(value -> origin(value.evidence().origin()))
                        .anyMatch(value -> value == PdfTextLayerOrigin.OCR_LOCAL)
                        ? PdfTextLayerOrigin.OCR_LOCAL : PdfTextLayerOrigin.NATIVE_BBOX;
                layers.add(new PdfTextLayer(page.pageNumber(), pageOrigin, lines, page.warnings()));
            }
            if (highlights.isEmpty()) {
                return new PdfVisualReadingProjection(layers, highlights, targets,
                        List.of("El PDF no tiene páginas V2 preparadas para lectura y resaltado."));
            }
            return new PdfVisualReadingProjection(layers, highlights, targets, List.of());
        } catch (IOException ex) {
            return new PdfVisualReadingProjection(List.of(), Map.of(),
                    List.of("No se pudo leer la preparación PDF V2: " + ex.getMessage()));
        }
    }

    private PdfVisualReadingProjection buildFromPageMaps(PreparedPdfWorkspaceRef workspace) throws IOException {
        ArrayList<PdfTextLayer> layers = new ArrayList<>();
        Map<String, PdfVisualTextHighlight> highlights = new LinkedHashMap<>();
        ArrayList<PdfVisualTextTarget> targets = new ArrayList<>();
        ArrayList<String> warnings = new ArrayList<>();
        for (int pageNumber : repository.listPreparedPageNumbers(workspace.projectRoot())) {
            PdfPageMapResolution resolution = pageMapResolver.resolve(workspace, pageNumber);
            PdfPageMap pageMap = resolution.pageMap();
            Map<String, PdfRegion> sourceRegions = repository.loadPage(
                            workspace.projectRoot(), pageNumber)
                    .map(page -> page.regions().stream().collect(
                            java.util.stream.Collectors.toMap(PdfRegion::id,
                                    value -> value, (left, right) -> left,
                                    LinkedHashMap::new)))
                    .map(value -> (Map<String, PdfRegion>) value)
                    .orElseGet(Map::of);
            warnings.addAll(resolution.warnings());
            ArrayList<PdfTextLine> lines = new ArrayList<>();
            java.util.Set<String> narratableRegionIds = pageMap.narrationBindings().stream()
                    .flatMap(binding -> binding.sourceRegionIds().stream()).collect(java.util.stream.Collectors.toSet());
            for (PdfPageNode node : pageMap.nodes()) {
                if (node.kind() == PdfPageNodeKind.LINE) {
                    lines.add(new PdfTextLine(pageNumber, node.text().literalText(), region(pageNumber, node.geometry()),
                            List.of(), node.evidence().confidence()));
                } else if (node.kind() == PdfPageNodeKind.BLOCK) {
                    PdfPageRegion box = region(pageNumber, node.geometry());
                    PdfRegion sourceRegion = sourceRegions.get(node.legacyRegionId());
                    PdfTextVisualBoundsResolver.Resolution visual = sourceRegion == null
                            ? new PdfTextVisualBoundsResolver.Resolution(box,
                            PdfTextVisualBoundsResolver.Source.SEMANTIC_FALLBACK, 0)
                            : textVisualBounds.resolve(sourceRegion, box);
                    PdfPageRegion visibleBox = visual.tightTextBBox();
                    PdfTextLayerOrigin origin = visual.source()
                            == PdfTextVisualBoundsResolver.Source.OCR_WORDS
                            ? PdfTextLayerOrigin.OCR_LOCAL
                            : sourceRegion != null
                            && PdfTextVisualBoundsResolver.textualType(sourceRegion)
                            ? PdfTextLayerOrigin.NATIVE_BBOX
                            : origin(node.evidence().origin());
                    highlights.put(node.legacyRegionId(), new PdfVisualTextHighlight(
                            pageNumber, visibleBox, node.text().literalText(), origin));
                    targets.add(new PdfVisualTextTarget(node.id(), node.legacyRegionId(),
                            node.text().startOffset(), node.text().endOffset(), pageNumber, box,
                            List.of(visibleBox), List.of(node.legacyRegionId()),
                            node.text().literalText(), origin, PdfVisualTextTargetKind.BLOCK));
                } else if (node.kind() == PdfPageNodeKind.VISUAL_OBJECT) {
                    PdfPageRegion box = region(pageNumber, node.geometry());
                    PdfTextLayerOrigin origin = origin(node.evidence().origin());
                    java.util.LinkedHashSet<String> sourceIds = new java.util.LinkedHashSet<>();
                    if (!node.legacyRegionId().isBlank()) sourceIds.add(node.legacyRegionId());
                    node.childIds().stream().map(pageMap::node).flatMap(java.util.Optional::stream)
                            .map(PdfPageNode::legacyRegionId).filter(value -> !value.isBlank()).forEach(sourceIds::add);
                    sourceIds.forEach(id -> highlights.put(id, new PdfVisualTextHighlight(
                            pageNumber, box, node.text().literalText(), origin)));
                    if (semanticClassifier.classifySemanticType(
                            node.semanticType()).secondary() && !sourceIds.isEmpty()) {
                        String anchorId = node.legacyRegionId().isBlank()
                                ? sourceIds.getFirst() : node.legacyRegionId();
                        targets.add(new PdfVisualTextTarget(
                                node.id(), anchorId, 0,
                                node.text().literalText().length(), pageNumber, box,
                                List.of(box), List.copyOf(sourceIds),
                                node.text().literalText(), origin,
                                PdfVisualTextTargetKind.SEMANTIC_COMPONENT));
                    }
                } else if (node.kind() == PdfPageNodeKind.SENTENCE
                        && narratableRegionIds.contains(node.legacyRegionId())) {
                    PdfPageRegion box = region(pageNumber, node.geometry());
                    List<PdfPageRegion> parts = node.geometryParts().stream()
                            .map(value -> region(pageNumber, value)).toList();
                    targets.add(new PdfVisualTextTarget(node.id(), node.legacyRegionId(),
                            node.text().startOffset(), node.text().endOffset(), pageNumber, box, parts,
                            node.text().literalText(), origin(node.evidence().origin()),
                            PdfVisualTextTargetKind.SENTENCE));
                }
            }
            if (lines.isEmpty()) {
                pageMap.nodes(PdfPageNodeKind.BLOCK).forEach(node -> lines.add(new PdfTextLine(
                        pageNumber, node.text().literalText(), region(pageNumber, node.geometry()),
                        List.of(), node.evidence().confidence())));
            }
            PdfTextLayerOrigin layerOrigin = pageMap.nodes().stream()
                    .map(node -> origin(node.evidence().origin()))
                    .anyMatch(value -> value == PdfTextLayerOrigin.OCR_LOCAL)
                    ? PdfTextLayerOrigin.OCR_LOCAL : PdfTextLayerOrigin.NATIVE_BBOX;
            layers.add(new PdfTextLayer(pageNumber, layerOrigin, lines, resolution.warnings()));
        }
        if (highlights.isEmpty()) warnings.add("El PDF no tiene PageMap textual disponible.");
        return new PdfVisualReadingProjection(layers, highlights, targets, warnings);
    }

    private static PdfPageRegion region(int pageNumber,
                                        com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfPageGeometry geometry) {
        return new PdfPageRegion(pageNumber, geometry.xMin(), geometry.yMin(), geometry.xMax(), geometry.yMax(),
                geometry.pageWidth(), geometry.pageHeight());
    }

    private static PdfTextLayerOrigin origin(String origin) {
        return origin != null && (origin.equals("OCR_LOCAL") || origin.equals("HYBRID"))
                ? PdfTextLayerOrigin.OCR_LOCAL : PdfTextLayerOrigin.NATIVE_BBOX;
    }

    private static List<PdfVisualTextTarget> sentenceTargets(PdfRegion region,
                                                             PdfPageRegion box,
                                                             PdfTextLayerOrigin origin) {
        ArrayList<PdfVisualTextTarget> result = new ArrayList<>();
        for (var span : DocumentSentenceSplitter.split(
                region.id(), region.effectiveText())) {
            int start = span.range().startOffset();
            int end = span.range().endOffset();
            SentenceGeometry geometry = sentenceGeometry(region, start, end, box);
            result.add(new PdfVisualTextTarget(
                    region.id() + "-S-" + start, region.id(), start, end,
                    region.pageNumber(), geometry.boundingBox(), geometry.lineBoxes(),
                    span.text(), origin,
                    PdfVisualTextTargetKind.SENTENCE));
        }
        return List.copyOf(result);
    }

    private static List<PdfPageRegion> playbackBoxes(PdfRegion region, PdfPageRegion fallback) {
        String value = region.attributes().getOrDefault("playbackBboxes", "");
        if (value.isBlank()) return List.of(fallback);
        boolean rasterGrounded = "OCR_RASTER_PAGE".equals(
                region.attributes().getOrDefault("playbackGeometryOrigin", ""));
        ArrayList<PdfPageRegion> result = new ArrayList<>();
        try {
            for (String encoded : value.split(";")) {
                String[] coordinates = encoded.split(",", 4);
                if (coordinates.length != 4) return List.of(fallback);
                PdfPageRegion box = new PdfPageRegion(region.pageNumber(),
                        Double.parseDouble(coordinates[0]), Double.parseDouble(coordinates[1]),
                        Double.parseDouble(coordinates[2]), Double.parseDouble(coordinates[3]),
                        fallback.pageWidthPoints(), fallback.pageHeightPoints());
                if (!rasterGrounded && (box.xMinPoints() < fallback.xMinPoints() - 1.0
                        || box.yMinPoints() < fallback.yMinPoints() - 1.0
                        || box.xMaxPoints() > fallback.xMaxPoints() + 1.0
                        || box.yMaxPoints() > fallback.yMaxPoints() + 1.0)) {
                    return List.of(fallback);
                }
                result.add(box);
            }
            return result.isEmpty() ? List.of(fallback) : List.copyOf(result);
        } catch (IllegalArgumentException malformed) {
            return List.of(fallback);
        }
    }

    private static SentenceGeometry sentenceGeometry(
            PdfRegion region, int start, int end, PdfPageRegion fallback) {
        if (!region.effectiveText().equals(region.text())) {
            return SentenceGeometry.fallback(fallback);
        }
        String boxesValue = region.attributes().getOrDefault("lineBboxes", "");
        String rangesValue = region.attributes().getOrDefault("lineCharRanges", "");
        String[] boxes = boxesValue.split(";");
        String[] ranges = rangesValue.split(";");
        if (boxes.length == 0 || boxes.length != ranges.length) {
            return SentenceGeometry.fallback(fallback);
        }
        ArrayList<PdfPageRegion> lineBoxes = new ArrayList<>();
        double xMin = Double.POSITIVE_INFINITY;
        double yMin = Double.POSITIVE_INFINITY;
        double xMax = Double.NEGATIVE_INFINITY;
        double yMax = Double.NEGATIVE_INFINITY;
        boolean matched = false;
        try {
            for (int index = 0; index < boxes.length; index++) {
                String[] range = ranges[index].split("-", 2);
                String[] coordinates = boxes[index].split(",", 4);
                if (range.length != 2 || coordinates.length != 4) {
                    return SentenceGeometry.fallback(fallback);
                }
                int lineStart = Integer.parseInt(range[0]);
                int lineEnd = Integer.parseInt(range[1]);
                if (lineEnd <= start || lineStart >= end) continue;
                PdfPageRegion lineBox = new PdfPageRegion(
                        region.pageNumber(),
                        Double.parseDouble(coordinates[0]),
                        Double.parseDouble(coordinates[1]),
                        Double.parseDouble(coordinates[2]),
                        Double.parseDouble(coordinates[3]),
                        fallback.pageWidthPoints(), fallback.pageHeightPoints());
                lineBoxes.add(lineBox);
                xMin = Math.min(xMin, lineBox.xMinPoints());
                yMin = Math.min(yMin, lineBox.yMinPoints());
                xMax = Math.max(xMax, lineBox.xMaxPoints());
                yMax = Math.max(yMax, lineBox.yMaxPoints());
                matched = true;
            }
            if (!matched) return SentenceGeometry.fallback(fallback);
            return new SentenceGeometry(
                    new PdfPageRegion(region.pageNumber(), xMin, yMin, xMax, yMax,
                            fallback.pageWidthPoints(), fallback.pageHeightPoints()),
                    lineBoxes);
        } catch (IllegalArgumentException malformedEvidence) {
            return SentenceGeometry.fallback(fallback);
        }
    }

    private record SentenceGeometry(
            PdfPageRegion boundingBox,
            List<PdfPageRegion> lineBoxes) {
        private SentenceGeometry {
            lineBoxes = lineBoxes == null || lineBoxes.isEmpty()
                    ? List.of(boundingBox) : List.copyOf(lineBoxes);
        }

        static SentenceGeometry fallback(PdfPageRegion region) {
            return new SentenceGeometry(region, List.of(region));
        }
    }

    private static PdfTextLayerOrigin origin(PdfRegionOrigin origin) {
        return origin == PdfRegionOrigin.OCR_LOCAL || origin == PdfRegionOrigin.HYBRID
                ? PdfTextLayerOrigin.OCR_LOCAL : PdfTextLayerOrigin.NATIVE_BBOX;
    }
}
