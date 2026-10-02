package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegion;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfNarratability;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionType;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PreparedPdfPage;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** Conservative no-dependency baseline combining typed regions, captions and OCR labels. */
public final class DetectPdfVisualObjectsUseCase {
    public static final String DETECTOR_SIGNATURE = "typed-caption-table-cluster-v2";

    public List<PdfVisualObjectProposal> detect(PreparedPdfPage page) {
        if (page == null) return List.of();
        ArrayList<PdfVisualObjectProposal> proposals = new ArrayList<>();
        HashSet<String> consumed = new HashSet<>();
        for (PdfRegion caption : page.regions()) {
            if (!caption(caption)) continue;
            double top = Math.max(0, caption.yMin() - page.heightPoints() * 0.46);
            double left = Math.max(0, caption.xMin() - page.widthPoints() * 0.18);
            double right = Math.min(page.widthPoints(), caption.xMax() + page.widthPoints() * 0.18);
            List<PdfRegion> members = page.regions().stream()
                    .filter(region -> !region.id().equals(caption.id()))
                    .filter(region -> region.yMin() >= top && region.yMax() <= caption.yMax())
                    .filter(region -> region.xMax() >= left && region.xMin() <= right)
                    .filter(region -> visualSeed(region) || PdfRegionContentSignals.looksLikeSparseTechnicalLabel(page, region))
                    .toList();
            if (members.isEmpty()) continue;
            double xMin = Math.max(0, Math.min(caption.xMin(), members.stream().mapToDouble(PdfRegion::xMin).min().orElse(caption.xMin())) - 8);
            double yMin = Math.max(0, members.stream().mapToDouble(PdfRegion::yMin).min().orElse(top) - 8);
            double xMax = Math.min(page.widthPoints(), Math.max(caption.xMax(), members.stream().mapToDouble(PdfRegion::xMax).max().orElse(caption.xMax())) + 8);
            double yMax = Math.min(page.heightPoints(), caption.yMax() + 4);
            PdfVisualObjectProposal.Type type = captionType(caption.effectiveText());
            List<String> labels = members.stream().filter(region -> region.effectiveType() != PdfRegionType.IMAGE)
                    .map(PdfRegion::id).toList();
            proposals.add(proposal(page, type, xMin, yMin, xMax, yMax,
                    members.stream().map(PdfRegion::id).toList(), labels, caption.id(), 0.82));
            consumed.add(caption.id());
            consumed.addAll(members.stream().map(PdfRegion::id).toList());
        }
        for (PdfVisualObjectProposal table : detectAlignedTables(page, consumed)) {
            proposals.add(table);
            consumed.addAll(table.sourceRegionIds());
        }
        for (PdfRegion region : page.regions()) {
            if (consumed.contains(region.id()) || !visualSeed(region)) continue;
            PdfVisualObjectProposal.Type type = switch (region.effectiveType()) {
                case IMAGE -> PdfVisualObjectProposal.Type.FIGURE;
                case TABLE -> PdfVisualObjectProposal.Type.TABLE;
                case MATH -> PdfVisualObjectProposal.Type.FORMULA;
                default -> PdfVisualObjectProposal.Type.UNKNOWN;
            };
            proposals.add(proposal(page, type, region.xMin(), region.yMin(), region.xMax(), region.yMax(),
                    List.of(region.id()), List.of(), "", region.evidence().confidence()));
        }
        return proposals.stream().sorted(Comparator.comparingDouble(value -> value.geometry().yMinPoints())).toList();
    }

    private static boolean visualSeed(PdfRegion region) {
        return region.effectiveType() == PdfRegionType.IMAGE
                || region.effectiveType() == PdfRegionType.TABLE
                || region.effectiveType() == PdfRegionType.MATH
                || region.effectiveType() == PdfRegionType.UNKNOWN
                && region.effectiveNarratability() != PdfNarratability.NARRATABLE;
    }

    /**
     * Detects a table from aligned column headers before Qwen is involved.
     * This catches native PDF text layers that expose each column as a
     * seemingly narratable paragraph, as happens in many academic PDFs.
     */
    private static List<PdfVisualObjectProposal> detectAlignedTables(
            PreparedPdfPage page, HashSet<String> consumed) {
        List<PdfRegion> candidates = page.regions().stream()
                .filter(region -> !consumed.contains(region.id()))
                .filter(region -> !region.effectiveText().isBlank())
                .filter(region -> switch (region.effectiveType()) {
                    case HEADER, FOOTER, PAGE_NUMBER, TITLE, HEADING,
                            SUBHEADING, CAPTION, IMAGE -> false;
                    default -> true;
                })
                .sorted(Comparator.comparingDouble(PdfRegion::yMin)
                        .thenComparingDouble(PdfRegion::xMin))
                .toList();
        ArrayList<PdfVisualObjectProposal> tables = new ArrayList<>();
        HashSet<String> clustered = new HashSet<>();
        double topTolerance = Math.max(3.5, page.heightPoints() * 0.005);
        double minimumColumnGap = page.widthPoints() * 0.055;
        for (PdfRegion seed : candidates) {
            if (clustered.contains(seed.id())) continue;
            List<PdfRegion> headerBand = candidates.stream()
                    .filter(region -> !clustered.contains(region.id()))
                    .filter(region -> Math.abs(region.yMin() - seed.yMin())
                            <= topTolerance)
                    .sorted(Comparator.comparingDouble(PdfRegion::xMin))
                    .toList();
            if (distinctColumns(headerBand, minimumColumnGap) < 3) continue;
            double left = headerBand.stream().mapToDouble(PdfRegion::xMin)
                    .min().orElse(seed.xMin());
            double right = headerBand.stream().mapToDouble(PdfRegion::xMax)
                    .max().orElse(seed.xMax());
            if (right - left < page.widthPoints() * 0.42) continue;
            double top = headerBand.stream().mapToDouble(PdfRegion::yMin)
                    .min().orElse(seed.yMin());
            double bottom = headerBand.stream().mapToDouble(PdfRegion::yMax)
                    .max().orElse(seed.yMax());
            double maximumBottom = Math.min(page.heightPoints(),
                    top + page.heightPoints() * 0.34);
            double rowGap = Math.max(10.0, page.heightPoints() * 0.016);
            ArrayList<PdfRegion> members = new ArrayList<>(headerBand);
            boolean expanded;
            do {
                expanded = false;
                double currentBottom = bottom;
                for (PdfRegion region : candidates) {
                    if (clustered.contains(region.id())
                            || members.contains(region)
                            || region.yMin() < top - topTolerance
                            || region.yMin() > currentBottom + rowGap
                            || region.yMax() > maximumBottom
                            || region.xMax() < left - minimumColumnGap
                            || region.xMin() > right + minimumColumnGap) {
                        continue;
                    }
                    members.add(region);
                    bottom = Math.max(bottom, region.yMax());
                    expanded = true;
                }
            } while (expanded);
            if (members.size() < 5
                    || distinctColumns(members, minimumColumnGap) < 3) {
                continue;
            }
            List<PdfRegion> ordered = members.stream().distinct()
                    .sorted(Comparator.comparingDouble(PdfRegion::yMin)
                            .thenComparingDouble(PdfRegion::xMin)).toList();
            double xMin = Math.max(0, ordered.stream()
                    .mapToDouble(PdfRegion::xMin).min().orElse(left) - 8);
            double yMin = Math.max(0, ordered.stream()
                    .mapToDouble(PdfRegion::yMin).min().orElse(top) - 6);
            double xMax = Math.min(page.widthPoints(), ordered.stream()
                    .mapToDouble(PdfRegion::xMax).max().orElse(right) + 8);
            double yMax = Math.min(page.heightPoints(), ordered.stream()
                    .mapToDouble(PdfRegion::yMax).max().orElse(bottom) + 6);
            List<String> ids = ordered.stream().map(PdfRegion::id).toList();
            tables.add(proposal(page, PdfVisualObjectProposal.Type.TABLE,
                    xMin, yMin, xMax, yMax, ids, List.of(), "", 0.88));
            clustered.addAll(ids);
        }
        return List.copyOf(tables);
    }

    private static int distinctColumns(List<PdfRegion> regions,
                                       double minimumGap) {
        ArrayList<Double> centers = new ArrayList<>();
        regions.stream().map(region -> (region.xMin() + region.xMax()) / 2.0)
                .sorted().forEach(center -> {
                    if (centers.isEmpty()
                            || center - centers.getLast() >= minimumGap) {
                        centers.add(center);
                    }
                });
        return centers.size();
    }

    private static boolean caption(PdfRegion region) {
        if (region.effectiveType() == PdfRegionType.CAPTION) return true;
        return region.effectiveText().toLowerCase(Locale.ROOT)
                .matches("(?s)^\\s*(figura|figure|gráfica|grafica|gráfico|grafico|diagrama|diagram|chart)\\b.*");
    }

    private static PdfVisualObjectProposal.Type captionType(String text) {
        String value = text.toLowerCase(Locale.ROOT);
        if (value.contains("diagrama") || value.contains("diagram")) return PdfVisualObjectProposal.Type.DIAGRAM;
        if (value.contains("gráfic") || value.contains("grafic") || value.contains("chart")) return PdfVisualObjectProposal.Type.GRAPH;
        return PdfVisualObjectProposal.Type.FIGURE;
    }

    private static PdfVisualObjectProposal proposal(PreparedPdfPage page, PdfVisualObjectProposal.Type type,
                                                     double xMin, double yMin, double xMax, double yMax,
                                                     List<String> sources, List<String> labels,
                                                     String caption, double confidence) {
        String identity = page.pageNumber() + "|" + type + "|" + caption
                + "|sources=" + String.join("|", sources)
                + "|labels=" + String.join("|", labels);
        String id = "PVO-" + UUID.nameUUIDFromBytes(identity.getBytes(StandardCharsets.UTF_8))
                .toString().replace("-", "").substring(0, 16).toUpperCase(Locale.ROOT);
        return new PdfVisualObjectProposal(id, type,
                new PdfPageRegion(page.pageNumber(), xMin, yMin, xMax, yMax, page.widthPoints(), page.heightPoints()),
                sources, labels, caption, confidence, DETECTOR_SIGNATURE);
    }
}
