package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegion;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionType;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PreparedPdfPage;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfFormulaKind;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Groups small OCR fragments into complete figures, graphs, formulas or tables. */
public final class ResolveDocumentTechnicalElementsUseCase {
    private final PreparedPdfDocumentRepository repository;
    private final SecondarySemanticComponentClassifier classifier =
            new SecondarySemanticComponentClassifier();

    public ResolveDocumentTechnicalElementsUseCase(
            PreparedPdfDocumentRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public List<DocumentTechnicalElement> resolve(
            Path projectRoot, int pageNumber, List<String> requestedRegionIds)
            throws IOException {
        PreparedPdfPage page = repository.loadPage(projectRoot, pageNumber)
                .orElseThrow(() -> new IOException(
                        "La página " + pageNumber + " aún no está preparada."));
        Set<String> requested = requestedRegionIds == null ? Set.of()
                : Set.copyOf(requestedRegionIds);
        List<PdfRegion> candidates = page.regions().stream()
                .filter(region -> requested.contains(region.id()))
                .sorted(Comparator.comparingInt(PdfRegion::effectiveReadingOrder)
                        .thenComparingDouble(PdfRegion::yMin)
                        .thenComparingDouble(PdfRegion::xMin))
                .toList();
        if (candidates.isEmpty()) return List.of();

        ArrayList<DocumentTechnicalElement> result = new ArrayList<>();
        HashSet<String> consumed = new HashSet<>();
        java.util.Map<String, PdfRegion> regionsById = page.regions().stream()
                .collect(java.util.stream.Collectors.toMap(PdfRegion::id,
                        java.util.function.Function.identity()));
        for (PdfVisualObjectProposal proposal :
                new DetectPdfVisualObjectsUseCase().detect(page)) {
            java.util.LinkedHashSet<String> memberIds =
                    new java.util.LinkedHashSet<>(proposal.sourceRegionIds());
            memberIds.addAll(proposal.internalLabelRegionIds());
            if (!proposal.captionRegionId().isBlank()) {
                memberIds.add(proposal.captionRegionId());
            }
            if (memberIds.stream().noneMatch(requested::contains)) continue;
            List<PdfRegion> members = memberIds.stream().map(regionsById::get)
                    .filter(Objects::nonNull).toList();
            if (members.isEmpty()) continue;
            PdfRegion anchor = regionsById.getOrDefault(
                    proposal.captionRegionId(), members.getFirst());
            DocumentTechnicalElement.Type type = switch (proposal.type()) {
                case TABLE -> DocumentTechnicalElement.Type.TABLE;
                case FORMULA -> DocumentTechnicalElement.Type.BLOCK_FORMULA;
                case GRAPH -> DocumentTechnicalElement.Type.GRAPH;
                case FIGURE, DIAGRAM -> DocumentTechnicalElement.Type.FIGURE;
                case UNKNOWN, DECORATION ->
                        DocumentTechnicalElement.Type.TECHNICAL_REGION;
            };
            result.add(element(page, members, anchor, type,
                    type == DocumentTechnicalElement.Type.FIGURE
                            || type == DocumentTechnicalElement.Type.GRAPH,
                    proposal.geometry()));
            consumed.addAll(memberIds);
        }
        for (PdfFormulaUnit formula :
                new DetectPdfFormulaUnitsUseCase().detect(page)) {
            List<PdfRegion> members = candidates.stream()
                    .filter(region -> !consumed.contains(region.id()))
                    .filter(region -> formula.sourceRegionIds().contains(region.id()))
                    .toList();
            if (members.isEmpty()) continue;
            result.add(element(page, members, members.getFirst(),
                    formula.kind() == PdfFormulaKind.INLINE_FORMULA
                            ? DocumentTechnicalElement.Type.INLINE_FORMULA
                            : DocumentTechnicalElement.Type.BLOCK_FORMULA,
                    false));
            members.forEach(region -> consumed.add(region.id()));
        }
        for (PdfRegion caption : page.regions()) {
            if (!isFigureCaption(caption)) continue;
            List<PdfRegion> members = candidates.stream()
                    .filter(region -> !consumed.contains(region.id()))
                    .filter(region -> belongsToCaption(page, region, caption))
                    .toList();
            if (members.isEmpty()) continue;
            ArrayList<PdfRegion> grouped = new ArrayList<>(members);
            grouped.add(caption);
            result.add(element(page, grouped, caption,
                    caption.effectiveText().toLowerCase(Locale.ROOT)
                            .contains("gráfic")
                            ? DocumentTechnicalElement.Type.GRAPH
                            : DocumentTechnicalElement.Type.FIGURE,
                    true));
            members.forEach(region -> consumed.add(region.id()));
        }

        for (PdfRegion region : candidates) {
            if (consumed.contains(region.id())) continue;
            DocumentTechnicalElement.Type type = switch (region.effectiveType()) {
                case MATH -> DocumentTechnicalElement.Type.BLOCK_FORMULA;
                case TABLE -> DocumentTechnicalElement.Type.TABLE;
                case IMAGE -> DocumentTechnicalElement.Type.FIGURE;
                default -> DocumentTechnicalElement.Type.TECHNICAL_REGION;
            };
            result.add(element(page, List.of(region), region, type, false));
        }
        return result.stream()
                .sorted(Comparator.comparingInt(
                        DocumentTechnicalElement::readingOrder))
                .toList();
    }

    /** Resolves every secondary component before the reading policy filters it. */
    public List<DocumentTechnicalElement> resolveAll(
            Path projectRoot, int pageNumber) throws IOException {
        PreparedPdfPage page = repository.loadPage(projectRoot, pageNumber)
                .orElseThrow(() -> new IOException(
                        "La página " + pageNumber + " aún no está preparada."));
        List<String> ids = page.regions().stream()
                .filter(region -> classifier.classify(region).secondary())
                .map(PdfRegion::id)
                .collect(java.util.stream.Collectors.toCollection(
                        java.util.LinkedHashSet::new)).stream().toList();
        java.util.LinkedHashSet<String> allIds =
                new java.util.LinkedHashSet<>(ids);
        for (PdfVisualObjectProposal proposal :
                new DetectPdfVisualObjectsUseCase().detect(page)) {
            if (!classifier.classifySemanticType(
                    proposal.type().name()).secondary()) continue;
            allIds.addAll(proposal.sourceRegionIds());
            allIds.addAll(proposal.internalLabelRegionIds());
            if (!proposal.captionRegionId().isBlank()) {
                allIds.add(proposal.captionRegionId());
            }
        }
        return resolve(projectRoot, pageNumber, List.copyOf(allIds));
    }

    private static boolean belongsToCaption(
            PreparedPdfPage page, PdfRegion region, PdfRegion caption) {
        if (region.id().equals(caption.id())) return true;
        double pageHeight = page.heightPoints();
        double verticalTop = Math.max(0.0, caption.yMin() - pageHeight * 0.48);
        double verticalBottom = caption.yMax() + pageHeight * 0.04;
        return region.yMin() >= verticalTop
                && region.yMax() <= verticalBottom
                && region.xMax() >= caption.xMin() - page.widthPoints() * 0.25
                && region.xMin() <= caption.xMax() + page.widthPoints() * 0.25;
    }

    private static boolean isFigureCaption(PdfRegion region) {
        if (region.effectiveType() == PdfRegionType.CAPTION) return true;
        String text = region.effectiveText().toLowerCase(Locale.ROOT);
        return text.matches("(?s)^\\s*(figura|figure|gráfica|grafica|gráfico|grafico|diagrama|chart)\\b.*");
    }

    private static DocumentTechnicalElement element(
            PreparedPdfPage page, List<PdfRegion> members, PdfRegion anchor,
            DocumentTechnicalElement.Type type, boolean expandFigure) {
        return element(page, members, anchor, type, expandFigure, null);
    }

    private static DocumentTechnicalElement element(
            PreparedPdfPage page, List<PdfRegion> members, PdfRegion anchor,
            DocumentTechnicalElement.Type type, boolean expandFigure,
            PdfPageRegion exactRoi) {
        List<PdfRegion> ordered = members.stream().distinct()
                .sorted(Comparator.comparingInt(PdfRegion::effectiveReadingOrder)
                        .thenComparingDouble(PdfRegion::yMin))
                .toList();
        double xMin = ordered.stream().mapToDouble(PdfRegion::xMin).min().orElse(0);
        double yMin = ordered.stream().mapToDouble(PdfRegion::yMin).min().orElse(0);
        double xMax = ordered.stream().mapToDouble(PdfRegion::xMax).max()
                .orElse(page.widthPoints());
        double yMax = ordered.stream().mapToDouble(PdfRegion::yMax).max()
                .orElse(page.heightPoints());
        if (expandFigure) {
            xMin = Math.max(0.0, xMin - page.widthPoints() * 0.12);
            xMax = Math.min(page.widthPoints(), xMax + page.widthPoints() * 0.12);
            yMin = Math.max(0.0, yMin - page.heightPoints() * 0.22);
            yMax = Math.min(page.heightPoints(), yMax + page.heightPoints() * 0.02);
        }
        if (exactRoi != null) {
            xMin = exactRoi.xMinPoints();
            yMin = exactRoi.yMinPoints();
            xMax = exactRoi.xMaxPoints();
            yMax = exactRoi.yMaxPoints();
        }
        List<String> ids = ordered.stream().map(PdfRegion::id).toList();
        String stableSource = page.pageNumber() + "|" + type + "|"
                + String.join("|", ids);
        String id = "DTE-" + UUID.nameUUIDFromBytes(
                        stableSource.getBytes(StandardCharsets.UTF_8))
                .toString().replace("-", "").substring(0, 16)
                .toUpperCase(Locale.ROOT);
        List<PdfRegion> contextRegions = page.regions().stream()
                .filter(region -> !ids.contains(region.id()))
                .filter(region -> region.effectiveText() != null
                        && !region.effectiveText().isBlank())
                .sorted(Comparator.comparingInt(PdfRegion::effectiveReadingOrder))
                .toList();
        List<PdfRegion> previous = contextRegions.stream()
                .filter(region -> region.effectiveReadingOrder()
                        < anchor.effectiveReadingOrder())
                .skip(Math.max(0, contextRegions.stream()
                        .filter(region -> region.effectiveReadingOrder()
                                < anchor.effectiveReadingOrder()).count() - 3))
                .toList();
        PdfRegion following = contextRegions.stream()
                .filter(region -> region.effectiveReadingOrder()
                        > anchor.effectiveReadingOrder())
                .findFirst().orElse(null);
        StringBuilder contextBuilder = new StringBuilder(
                "CONTEXTO ORIENTATIVO; NO ES EVIDENCIA VISUAL.\n");
        previous.forEach(region -> contextBuilder.append("ANTERIOR ")
                .append(region.id()).append(": ")
                .append(region.effectiveText()).append('\n'));
        if (following != null) {
            contextBuilder.append("POSTERIOR ").append(following.id()).append(": ")
                    .append(following.effectiveText());
        }
        String context = contextBuilder.toString().strip();
        return new DocumentTechnicalElement(
                id, type, page.pageNumber(), ids,
                new PdfPageRegion(page.pageNumber(), xMin, yMin, xMax, yMax,
                        page.widthPoints(), page.heightPoints()),
                isFigureCaption(anchor) ? anchor.effectiveText() : "",
                context, anchor.id(), anchor.effectiveReadingOrder());
    }
}
