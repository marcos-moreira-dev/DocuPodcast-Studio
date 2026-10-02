package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfPageRole;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

/** Conservative deterministic merge for verifier and targeted-recovery regions. */
public final class PdfSemanticRecoveryMerger {
    public PdfSemanticPageAnalysis merge(PdfSemanticPageAnalysis base,
                                         PdfSemanticPageAnalysis additions,
                                         boolean targetedRecovery) {
        ArrayList<PdfSemanticPageAnalysis.Element> merged =
                new ArrayList<>(base.elements());
        for (PdfSemanticPageAnalysis.Element addition : additions.elements()) {
            int duplicate = duplicateIndex(merged, addition);
            if (duplicate < 0) {
                merged.add(addition);
                continue;
            }
            PdfSemanticPageAnalysis.Element existing = merged.get(duplicate);
            if (shouldReplace(existing, addition, targetedRecovery)) {
                merged.set(duplicate, preserveResponseIdentity(existing, addition));
            }
        }
        List<PdfSemanticPageAnalysis.Element> ordered = deterministicOrder(merged);
        PdfPageRole role = additions.pageRole() == PdfPageRole.UNKNOWN
                ? base.pageRole() : additions.pageRole();
        return new PdfSemanticPageAnalysis(base.reportedPageNumber(),
                base.language(), role, ordered, base.confidence(),
                base.uncertainties());
    }

    /**
     * Keeps a giant parent only as structural metadata and inserts the refined
     * leaves. Geometry/order of unrelated skeleton nodes is never recomputed
     * from worker completion order.
     */
    public PdfSemanticPageAnalysis replacePlaybackParentWithChildren(
            PdfSemanticPageAnalysis base,
            PdfSemanticPageAnalysis.Element parent,
            PdfSemanticPageAnalysis children) {
        ArrayList<PdfSemanticPageAnalysis.Element> values = new ArrayList<>();
        for (PdfSemanticPageAnalysis.Element value : base.elements()) {
            if (value != parent) values.add(value);
        }
        LinkedHashMap<String, String> parentAttributes =
                new LinkedHashMap<>(parent.attributes());
        parentAttributes.put("regionRole", "CONTAINER");
        parentAttributes.put("playbackTarget", "false");
        parentAttributes.put("refinement", "SEGMENTATION_REFINEMENT");
        String parentKey = parent.responseId().isBlank()
                ? "layout-parent-" + parent.readingOrder() : parent.responseId();
        parentAttributes.put("layoutNodeId", parentKey);
        values.add(new PdfSemanticPageAnalysis.Element(parent.responseId(),
                parent.readingOrder(), parent.type(), parent.box(), "", "",
                com.marcosmoreiradev.docupodcaststudio.domain.document.pdf
                        .PdfNarratability.NON_NARRATABLE,
                parent.confidence(), parent.uncertainties(), parentAttributes));
        for (PdfSemanticPageAnalysis.Element child : children.elements()) {
            LinkedHashMap<String, String> attributes =
                    new LinkedHashMap<>(child.attributes());
            attributes.put("regionRole", "LEAF");
            attributes.put("playbackTarget", "true");
            attributes.put("parentLayoutNodeId", parentKey);
            attributes.put("refinement", "SEGMENTATION_REFINEMENT");
            values.add(new PdfSemanticPageAnalysis.Element(child.responseId(),
                    child.readingOrder(), child.type(), child.box(),
                    child.sourceText(), child.narrationText(),
                    child.narratability(), child.confidence(),
                    child.uncertainties(), attributes));
        }
        return new PdfSemanticPageAnalysis(base.reportedPageNumber(),
                base.language(), base.pageRole(), deterministicOrder(values),
                base.confidence(), base.uncertainties());
    }

    private static int duplicateIndex(List<PdfSemanticPageAnalysis.Element> values,
                                      PdfSemanticPageAnalysis.Element candidate) {
        for (int index = 0; index < values.size(); index++) {
            PdfSemanticPageAnalysis.Element existing = values.get(index);
            boolean sameType = existing.type() == candidate.type();
            if (normalized(existing.sourceText()).equals(normalized(candidate.sourceText()))) {
                return index;
            }
            double overlap = overlap(existing.box(), candidate.box());
            if (sameType && overlap >= 0.68) return index;
            if (overlap >= 0.88 && tokenSimilarity(existing.sourceText(),
                    candidate.sourceText()) >= 0.45) return index;
        }
        return -1;
    }

    private static boolean shouldReplace(PdfSemanticPageAnalysis.Element existing,
                                         PdfSemanticPageAnalysis.Element addition,
                                         boolean targetedRecovery) {
        if (targetedRecovery && existing.type() == PdfRegionType.TABLE
                && addition.type() == PdfRegionType.TABLE) return true;
        if (existing.narrationText().isBlank()
                && !addition.narrationText().isBlank()) return true;
        return addition.sourceText().length() > existing.sourceText().length();
    }

    private static PdfSemanticPageAnalysis.Element preserveResponseIdentity(
            PdfSemanticPageAnalysis.Element existing,
            PdfSemanticPageAnalysis.Element replacement) {
        LinkedHashMap<String, String> attributes =
                new LinkedHashMap<>(replacement.attributes());
        if (!existing.responseId().isBlank()) {
            attributes.put("replacedResponseId", existing.responseId());
        }
        if (!replacement.responseId().isBlank()) {
            attributes.put("recoveryResponseId", replacement.responseId());
        }
        // The primary result owns the layout skeleton. A coverage verifier may
        // fill SOURCE/SPEECH or correct classification, but cannot silently
        // replace an already-localized bbox with a page-sized supersegment.
        attributes.put("layoutAuthority", existing.attributes()
                .getOrDefault("semanticPass", "primary"));
        attributes.put("contentAuthority", replacement.attributes()
                .getOrDefault("semanticPass", "recovery"));
        PdfSemanticPageAnalysis.NormalizedBox geometry =
                preserveExistingGeometry(existing, replacement)
                        ? existing.box() : replacement.box();
        int readingOrder = preserveExistingGeometry(existing, replacement)
                ? existing.readingOrder() : replacement.readingOrder();
        return new PdfSemanticPageAnalysis.Element(existing.responseId(),
                readingOrder, replacement.type(), geometry,
                replacement.sourceText(), replacement.narrationText(),
                replacement.narratability(), replacement.confidence(),
                replacement.uncertainties(), attributes);
    }

    private static boolean preserveExistingGeometry(
            PdfSemanticPageAnalysis.Element existing,
            PdfSemanticPageAnalysis.Element replacement) {
        String pass = replacement.attributes().getOrDefault("semanticPass", "");
        if ("verifier".equals(pass)) return true;
        double oldArea = area(existing.box());
        double newArea = area(replacement.box());
        return newArea > oldArea * 2.5 && oldArea > 0.0;
    }

    private static List<PdfSemanticPageAnalysis.Element> deterministicOrder(
            List<PdfSemanticPageAnalysis.Element> values) {
        boolean twoColumns = hasTwoColumns(values);
        ArrayList<PdfSemanticPageAnalysis.Element> sorted = new ArrayList<>(values);
        sorted.sort(twoColumns ? columnAwareComparator(values)
                : Comparator.comparingDouble((PdfSemanticPageAnalysis.Element value) ->
                        value.box().yMin()).thenComparingDouble(value -> value.box().xMin())
                .thenComparing(value -> normalized(value.sourceText())));
        ArrayList<PdfSemanticPageAnalysis.Element> ordered = new ArrayList<>();
        for (int index = 0; index < sorted.size(); index++) {
            PdfSemanticPageAnalysis.Element value = sorted.get(index);
            LinkedHashMap<String, String> attributes = new LinkedHashMap<>(value.attributes());
            attributes.put("columnIndex", Integer.toString(column(value, twoColumns)));
            ordered.add(new PdfSemanticPageAnalysis.Element(value.responseId(), index,
                    value.type(), value.box(), value.sourceText(), value.narrationText(),
                    value.narratability(), value.confidence(), value.uncertainties(),
                    attributes));
        }
        return List.copyOf(ordered);
    }

    private static Comparator<PdfSemanticPageAnalysis.Element> columnAwareComparator(
            List<PdfSemanticPageAnalysis.Element> values) {
        List<Double> separators = values.stream().filter(PdfSemanticRecoveryMerger::spanning)
                .map(value -> value.box().yMin()).sorted().toList();
        return Comparator.comparingInt((PdfSemanticPageAnalysis.Element value) ->
                        section(value, separators))
                .thenComparingInt(value -> spanning(value) ? 0 : 1)
                .thenComparingInt(value -> column(value, true))
                .thenComparingDouble(value -> value.box().yMin())
                .thenComparingDouble(value -> value.box().xMin())
                .thenComparing(value -> normalized(value.sourceText()));
    }

    private static int section(PdfSemanticPageAnalysis.Element value,
                               List<Double> separators) {
        int section = 0;
        for (double separator : separators) {
            if (separator + 2.0 < value.box().yMin()) section++;
        }
        return section;
    }

    private static int column(PdfSemanticPageAnalysis.Element value,
                              boolean twoColumns) {
        if (!twoColumns || spanning(value)) return 0;
        return (value.box().xMin() + value.box().xMax()) / 2.0 < 500.0 ? 0 : 1;
    }

    private static boolean spanning(PdfSemanticPageAnalysis.Element value) {
        return value.box().xMax() - value.box().xMin() >= 680.0;
    }

    private static boolean hasTwoColumns(List<PdfSemanticPageAnalysis.Element> values) {
        long left = values.stream().filter(value -> !spanning(value))
                .filter(value -> center(value) < 440.0).count();
        long right = values.stream().filter(value -> !spanning(value))
                .filter(value -> center(value) > 560.0).count();
        return left > 0 && right > 0;
    }

    private static double center(PdfSemanticPageAnalysis.Element value) {
        return (value.box().xMin() + value.box().xMax()) / 2.0;
    }

    static double overlap(PdfSemanticPageAnalysis.NormalizedBox a,
                          PdfSemanticPageAnalysis.NormalizedBox b) {
        double intersection = Math.max(0.0,
                Math.min(a.xMax(), b.xMax()) - Math.max(a.xMin(), b.xMin()))
                * Math.max(0.0,
                Math.min(a.yMax(), b.yMax()) - Math.max(a.yMin(), b.yMin()));
        double smaller = Math.min(area(a), area(b));
        return intersection / Math.max(1.0, smaller);
    }

    private static double area(PdfSemanticPageAnalysis.NormalizedBox box) {
        return Math.max(0.0, box.xMax() - box.xMin())
                * Math.max(0.0, box.yMax() - box.yMin());
    }

    private static double tokenSimilarity(String left, String right) {
        var a = new java.util.HashSet<>(PdfSemanticCoverageValidator.tokens(left));
        var b = new java.util.HashSet<>(PdfSemanticCoverageValidator.tokens(right));
        if (a.isEmpty() || b.isEmpty()) return 0.0;
        var intersection = new java.util.HashSet<>(a);
        intersection.retainAll(b);
        var union = new java.util.HashSet<>(a);
        union.addAll(b);
        return intersection.size() / (double) union.size();
    }

    private static String normalized(String value) {
        return PdfSemanticCoverageValidator.normalized(value)
                .toLowerCase(Locale.ROOT);
    }
}
