package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfPageNode;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfPageNodeKind;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Reuses sidecar node identities without making a bbox the permanent identity. */
public final class PdfPageMapIdentityReconciler {
    private final List<PdfPageNode> previousNodes;
    private final Set<String> claimed = new HashSet<>();

    public PdfPageMapIdentityReconciler(List<PdfPageNode> previousNodes) {
        this.previousNodes = previousNodes == null ? List.of() : List.copyOf(previousNodes);
    }

    public String resolve(PdfPageNode candidate) {
        return previousNodes.stream()
                .filter(previous -> !claimed.contains(previous.id()))
                .filter(previous -> previous.kind() == candidate.kind())
                .map(previous -> new Match(previous, score(previous, candidate)))
                .filter(match -> match.score >= threshold(candidate.kind()))
                .max(Comparator.comparingDouble(Match::score))
                .map(match -> {
                    claimed.add(match.node.id());
                    return match.node.id();
                })
                .orElse(candidate.id());
    }

    private static double score(PdfPageNode previous, PdfPageNode candidate) {
        if (!candidate.legacyRegionId().isBlank()
                && candidate.legacyRegionId().equals(previous.legacyRegionId())) {
            return 10.0;
        }
        double score = previous.geometry().intersectionOverUnion(candidate.geometry()) * 0.45;
        score += textSimilarity(previous.text().literalText(), candidate.text().literalText()) * 0.35;
        if (previous.semanticType().equals(candidate.semanticType())) score += 0.10;
        if (previous.parentId().equals(candidate.parentId())) score += 0.05;
        if (previous.order() == candidate.order()) score += 0.05;
        return score;
    }

    private static double threshold(PdfPageNodeKind kind) {
        return kind == PdfPageNodeKind.SENTENCE ? 0.58 : 0.50;
    }

    private static double textSimilarity(String first, String second) {
        String a = normalize(first);
        String b = normalize(second);
        if (a.equals(b)) return 1.0;
        if (a.isBlank() || b.isBlank()) return 0.0;
        Set<String> left = new HashSet<>(List.of(a.split("\\s+")));
        Set<String> right = new HashSet<>(List.of(b.split("\\s+")));
        Set<String> intersection = new HashSet<>(left);
        intersection.retainAll(right);
        Set<String> union = new HashSet<>(left);
        union.addAll(right);
        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip().toLowerCase(java.util.Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}]+", " ").strip();
    }

    private record Match(PdfPageNode node, double score) {
    }
}
