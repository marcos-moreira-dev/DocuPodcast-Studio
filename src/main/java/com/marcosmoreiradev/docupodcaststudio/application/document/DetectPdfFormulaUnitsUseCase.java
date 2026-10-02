package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfFormulaKind;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegion;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionType;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PreparedPdfPage;

import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** Deterministically joins neighboring MATH fragments before any recognition. */
public final class DetectPdfFormulaUnitsUseCase {
    public List<PdfFormulaUnit> detect(PreparedPdfPage page) {
        if (page == null) return List.of();
        List<PdfRegion> math = page.regions().stream()
                .filter(region -> region.effectiveType() == PdfRegionType.MATH)
                .sorted(Comparator.comparingDouble(PdfRegion::yMin)
                        .thenComparingDouble(PdfRegion::xMin)).toList();
        ArrayList<PdfFormulaUnit> result = new ArrayList<>();
        HashSet<String> consumed = new HashSet<>();
        for (PdfRegion seed : math) {
            if (!consumed.add(seed.id())) continue;
            ArrayList<PdfRegion> group = new ArrayList<>();
            ArrayDeque<PdfRegion> queue = new ArrayDeque<>();
            queue.add(seed);
            while (!queue.isEmpty()) {
                PdfRegion current = queue.removeFirst();
                group.add(current);
                for (PdfRegion candidate : math) {
                    if (consumed.contains(candidate.id())) continue;
                    if (neighbors(current, candidate)) {
                        consumed.add(candidate.id());
                        queue.add(candidate);
                    }
                }
            }
            result.add(unit(page, group));
        }
        return List.copyOf(result);
    }

    private static boolean neighbors(PdfRegion left, PdfRegion right) {
        double horizontalGap = Math.max(0,
                Math.max(left.xMin(), right.xMin())
                        - Math.min(left.xMax(), right.xMax()));
        double verticalGap = Math.max(0,
                Math.max(left.yMin(), right.yMin())
                        - Math.min(left.yMax(), right.yMax()));
        double height = Math.max(8, Math.max(
                left.yMax() - left.yMin(), right.yMax() - right.yMin()));
        return verticalGap <= height * 0.8 && horizontalGap <= height * 2.5;
    }

    private static PdfFormulaUnit unit(
            PreparedPdfPage page, List<PdfRegion> fragments) {
        double xMin = fragments.stream().mapToDouble(PdfRegion::xMin).min().orElse(0);
        double yMin = fragments.stream().mapToDouble(PdfRegion::yMin).min().orElse(0);
        double xMax = fragments.stream().mapToDouble(PdfRegion::xMax).max().orElse(xMin);
        double yMax = fragments.stream().mapToDouble(PdfRegion::yMax).max().orElse(yMin);
        boolean sharesProseLine = page.regions().stream()
                .filter(region -> region.effectiveType() != PdfRegionType.MATH)
                .filter(region -> region.effectiveType() != PdfRegionType.IMAGE)
                .anyMatch(region -> overlap(region.yMin(), region.yMax(), yMin, yMax)
                        >= Math.min(region.yMax() - region.yMin(), yMax - yMin) * 0.5);
        PdfFormulaKind kind = sharesProseLine || (xMax - xMin) < page.widthPoints() * 0.45
                ? PdfFormulaKind.INLINE_FORMULA : PdfFormulaKind.BLOCK_FORMULA;
        List<String> ids = fragments.stream().map(PdfRegion::id).sorted().toList();
        String id = "PDF-FORMULA-" + UUID.nameUUIDFromBytes(
                (page.pageNumber() + "|" + String.join("|", ids))
                        .getBytes(StandardCharsets.UTF_8)).toString()
                .replace("-", "").substring(0, 16).toUpperCase(Locale.ROOT);
        return new PdfFormulaUnit(id, kind, ids,
                new PdfPageRegion(page.pageNumber(), xMin, yMin, xMax, yMax,
                        page.widthPoints(), page.heightPoints()),
                fragments.stream().mapToDouble(region -> region.evidence().confidence())
                        .average().orElse(0));
    }

    private static double overlap(double aMin, double aMax,
                                  double bMin, double bMax) {
        return Math.max(0, Math.min(aMax, bMax) - Math.max(aMin, bMin));
    }
}
