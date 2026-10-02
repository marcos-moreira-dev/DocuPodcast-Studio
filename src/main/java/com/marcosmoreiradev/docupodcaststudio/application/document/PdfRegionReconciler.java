package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfNarratability;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegion;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Preserves region identity and independent overrides across one-page reprocessing. */
public final class PdfRegionReconciler {
    public List<PdfRegion> reconcile(List<PdfRegion> previous, List<PdfRegion> candidates) {
        List<PdfRegion> oldRegions = previous == null ? List.of() : previous;
        List<PdfRegion> newRegions = candidates == null ? List.of() : candidates;
        Set<String> claimed = new HashSet<>();
        ArrayList<PdfRegion> result = new ArrayList<>(newRegions.size());
        for (PdfRegion candidate : newRegions) {
            PdfRegion match = bestMatch(candidate, oldRegions, claimed);
            if (match == null) {
                result.add(candidate);
                continue;
            }
            claimed.add(match.id());
            result.add(new PdfRegion(match.id(), candidate.pageNumber(),
                    candidate.xMin(), candidate.yMin(), candidate.xMax(), candidate.yMax(),
                    candidate.columnIndex(), candidate.readingOrder(), candidate.text(),
                    candidate.automaticType(), candidate.automaticNarratability(), candidate.reasons(),
                    candidate.evidence(), candidate.evidenceCandidates(), match.override(),
                    candidate.attributes(), match.revision() + 1L));
        }
        markSplitFollowersUncertain(result, oldRegions);
        return List.copyOf(result);
    }

    private static PdfRegion bestMatch(PdfRegion candidate, List<PdfRegion> previous, Set<String> claimed) {
        PdfRegion best = null;
        double bestScore = 0.45;
        for (PdfRegion old : previous) {
            if (claimed.contains(old.id()) || old.pageNumber() != candidate.pageNumber()) continue;
            double sameColumn = old.columnIndex() == candidate.columnIndex() ? 1.0 : 0.0;
            double sameType = old.automaticType() == candidate.automaticType() ? 1.0 : 0.0;
            double score = overlapScore(old, candidate) * 0.55
                    + textScore(old.text(), candidate.text()) * 0.30
                    + sameColumn * 0.10
                    + sameType * 0.05;
            if (score > bestScore) {
                best = old;
                bestScore = score;
            }
        }
        return best;
    }

    private static void markSplitFollowersUncertain(List<PdfRegion> result, List<PdfRegion> previous) {
        for (int index = 0; index < result.size(); index++) {
            PdfRegion region = result.get(index);
            if (previous.stream().anyMatch(old -> old.id().equals(region.id()))) continue;
            boolean overlapsOldOverride = previous.stream()
                    .filter(old -> !old.override().emptyOverride())
                    .anyMatch(old -> overlapScore(old, region) >= 0.35);
            if (!overlapsOldOverride) continue;
            ArrayList<String> reasons = new ArrayList<>(region.reasons());
            reasons.add("possible-split-from-manually-reviewed-region");
            result.set(index, new PdfRegion(region.id(), region.pageNumber(),
                    region.xMin(), region.yMin(), region.xMax(), region.yMax(),
                    region.columnIndex(), region.readingOrder(), region.text(),
                    region.automaticType(), PdfNarratability.UNCERTAIN, reasons,
                    region.evidence(), region.evidenceCandidates(), region.override(),
                    region.attributes(), region.revision()));
        }
    }

    private static double overlapScore(PdfRegion a, PdfRegion b) {
        double intersectionWidth = Math.max(0.0, Math.min(a.xMax(), b.xMax()) - Math.max(a.xMin(), b.xMin()));
        double intersectionHeight = Math.max(0.0, Math.min(a.yMax(), b.yMax()) - Math.max(a.yMin(), b.yMin()));
        double intersection = intersectionWidth * intersectionHeight;
        double union = area(a) + area(b) - intersection;
        return union <= 0.0 ? 0.0 : intersection / union;
    }

    private static double area(PdfRegion region) {
        return Math.max(0.0, region.xMax() - region.xMin()) * Math.max(0.0, region.yMax() - region.yMin());
    }

    private static double textScore(String left, String right) {
        String a = normalize(left);
        String b = normalize(right);
        if (a.isBlank() || b.isBlank()) return 0.0;
        if (a.equals(b)) return 1.0;
        Set<String> aWords = new HashSet<>(List.of(a.split("\\s+")));
        Set<String> bWords = new HashSet<>(List.of(b.split("\\s+")));
        Set<String> intersection = new HashSet<>(aWords);
        intersection.retainAll(bWords);
        Set<String> union = new HashSet<>(aWords);
        union.addAll(bWords);
        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}]+", " ")
                .strip();
    }
}
