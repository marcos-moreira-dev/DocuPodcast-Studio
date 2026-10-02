package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfDocumentAnalysisSummary;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfDocumentManifest;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfNarratability;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegion;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionType;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PreparedPdfPage;

import java.text.Normalizer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Conservative cross-page layout and repeated-furniture analyzer. */
public final class PdfDocumentLayoutAnalyzer {
    private static final double EDGE_BAND = 0.12;

    public PdfLayoutAnalysisResult analyze(PdfDocumentManifest manifest, List<PreparedPdfPage> pages) {
        List<PreparedPdfPage> safePages = pages == null ? List.of() : pages;
        Set<String> headers = repeatedEdgeText(safePages, true);
        Set<String> footers = repeatedEdgeText(safePages, false);
        ArrayList<PreparedPdfPage> analyzed = new ArrayList<>();
        ArrayList<Integer> changed = new ArrayList<>();
        int maximumColumns = 0;
        for (PreparedPdfPage page : safePages) {
            PageResult result = analyzePage(page, headers, footers);
            analyzed.add(result.page());
            maximumColumns = Math.max(maximumColumns, result.columnCount());
            if (result.changed()) changed.add(page.pageNumber());
        }
        PdfDocumentAnalysisSummary previous = manifest.analysisSummary();
        PdfDocumentAnalysisSummary summary = new PdfDocumentAnalysisSummary(
                previous.revision() + 1, safePages.size(), maximumColumns,
                headers.stream().sorted().toList(), footers.stream().sorted().toList(), Instant.now());
        PdfDocumentManifest updated = new PdfDocumentManifest(
                manifest.schemaVersion(), manifest.title(), manifest.sourceFile(), manifest.sourceSha256(),
                manifest.pageCount(), manifest.preparationSignature(), summary,
                manifest.createdAt(), Instant.now(), manifest.readingStrategy(), manifest.nativeTextProvider());
        return new PdfLayoutAnalysisResult(updated, analyzed, changed);
    }

    /**
     * O(1) page publication path. Cross-page furniture analysis remains an
     * explicit document operation instead of rereading every page after each OCR.
     */
    public PdfLayoutAnalysisResult analyzeIncremental(PdfDocumentManifest manifest, PreparedPdfPage page) {
        return analyzeIncremental(manifest, page, true);
    }

    public PdfLayoutAnalysisResult analyzeIncremental(PdfDocumentManifest manifest,
                                                      PreparedPdfPage page,
                                                      boolean newlyPrepared) {
        PageResult result = analyzePage(page, Set.copyOf(manifest.analysisSummary().repeatedHeaders()),
                Set.copyOf(manifest.analysisSummary().repeatedFooters()));
        PdfDocumentAnalysisSummary previous = manifest.analysisSummary();
        PdfDocumentAnalysisSummary summary = new PdfDocumentAnalysisSummary(
                previous.revision() + 1,
                Math.min(manifest.pageCount(), Math.max(1,
                        previous.observedPages() + (newlyPrepared ? 1 : 0))),
                Math.max(previous.maximumColumnCount(), result.columnCount()),
                previous.repeatedHeaders(), previous.repeatedFooters(), Instant.now());
        PdfDocumentManifest updated = new PdfDocumentManifest(
                manifest.schemaVersion(), manifest.title(), manifest.sourceFile(), manifest.sourceSha256(),
                manifest.pageCount(), manifest.preparationSignature(), summary,
                manifest.createdAt(), Instant.now(), manifest.readingStrategy(), manifest.nativeTextProvider());
        return new PdfLayoutAnalysisResult(updated, List.of(result.page()),
                result.changed() ? List.of(page.pageNumber()) : List.of());
    }

    private static PageResult analyzePage(PreparedPdfPage page,
                                          Set<String> headers,
                                          Set<String> footers) {
        List<Double> columnStarts = page.regions().stream()
                .filter(region -> width(region) < page.widthPoints() * 0.70)
                .map(PdfRegion::xMin).sorted().toList();
        List<Double> clusters = cluster(columnStarts, page.widthPoints() * 0.12);
        ArrayList<PdfRegion> prelim = new ArrayList<>();
        for (PdfRegion region : page.regions()) {
            int column = width(region) >= page.widthPoints() * 0.70 ? 0 : nearestCluster(region.xMin(), clusters) + 1;
            Classification classification = classify(region, page, headers, footers);
            prelim.add(copy(region, column, region.readingOrder(), classification));
        }
        List<PdfRegion> ordered =
                new PdfReadingOrderResolver().resolve(page, prelim);
        ArrayList<PdfRegion> result = new ArrayList<>();
        boolean changed = false;
        for (int index = 0; index < ordered.size(); index++) {
            PdfRegion region = ordered.get(index);
            PdfRegion original = page.regions().stream().filter(value -> value.id().equals(region.id()))
                    .findFirst().orElse(region);
            boolean regionChanged = region.columnIndex() != original.columnIndex()
                    || index != original.readingOrder()
                    || region.automaticType() != original.automaticType()
                    || region.automaticNarratability() != original.automaticNarratability();
            changed |= regionChanged;
            result.add(new PdfRegion(region.id(), region.pageNumber(),
                    region.xMin(), region.yMin(), region.xMax(), region.yMax(),
                    region.columnIndex(), index, region.text(), region.automaticType(),
                    region.automaticNarratability(), region.reasons(), region.evidence(),
                    region.evidenceCandidates(), region.override(), region.attributes(),
                    regionChanged ? original.revision() + 1 : original.revision()));
        }
        PreparedPdfPage updated = new PreparedPdfPage(page.schemaVersion(), page.pageNumber(),
                page.widthPoints(), page.heightPoints(), page.status(),
                changed ? page.revision() + 1 : page.revision(),
                result, page.derivedTreatments(), page.preparationMetrics(),
                page.analysisProfile(),
                page.warnings(), page.lastAttemptError());
        return new PageResult(updated, Math.max(1, clusters.size()), changed);
    }

    private static Classification classify(PdfRegion region,
                                           PreparedPdfPage page,
                                           Set<String> headers,
                                           Set<String> footers) {
        String normalized = normalize(region.effectiveText());
        PdfRegionType type = region.automaticType();
        PdfNarratability narratability = region.automaticNarratability();
        ArrayList<String> reasons = new ArrayList<>(region.reasons());
        if (headers.contains(normalized) && region.yMin() <= page.heightPoints() * EDGE_BAND) {
            type = PdfRegionType.HEADER;
            narratability = PdfNarratability.NON_NARRATABLE;
            reasons.add("repeated-page-header");
        } else if (footers.contains(normalized) && region.yMax() >= page.heightPoints() * (1.0 - EDGE_BAND)) {
            type = PdfRegionType.FOOTER;
            narratability = PdfNarratability.NON_NARRATABLE;
            reasons.add("repeated-page-footer");
        } else if (normalized.matches("(pagina|page)?\\s*\\d{1,5}")) {
            type = PdfRegionType.PAGE_NUMBER;
            narratability = PdfNarratability.NON_NARRATABLE;
            reasons.add("page-number-pattern");
        } else if (looksLikeMath(region.text())) {
            type = PdfRegionType.MATH;
            narratability = PdfNarratability.NON_NARRATABLE;
            reasons.add("math-density");
        } else if (looksLikeCode(region.text())) {
            type = PdfRegionType.CODE;
            narratability = PdfNarratability.UNCERTAIN;
            reasons.add("code-like-layout");
        } else if (looksLikeTable(region.text())) {
            type = PdfRegionType.TABLE;
            narratability = PdfNarratability.NON_NARRATABLE;
            reasons.add("table-like-layout");
        } else if (PdfRegionContentSignals
                .looksLikeSparseTechnicalLabel(page, region)) {
            type = PdfRegionType.UNKNOWN;
            narratability = PdfNarratability.UNCERTAIN;
            reasons.add("sparse-technical-label");
        }
        return new Classification(type, narratability, reasons.stream().distinct().toList());
    }

    private static Set<String> repeatedEdgeText(List<PreparedPdfPage> pages, boolean top) {
        Map<String, Integer> occurrences = new LinkedHashMap<>();
        for (PreparedPdfPage page : pages) {
            java.util.HashSet<String> seen = new java.util.HashSet<>();
            for (PdfRegion region : page.regions()) {
                boolean inBand = top ? region.yMin() <= page.heightPoints() * EDGE_BAND
                        : region.yMax() >= page.heightPoints() * (1.0 - EDGE_BAND);
                String text = normalize(region.effectiveText());
                if (inBand && text.length() >= 2 && seen.add(text)) occurrences.merge(text, 1, Integer::sum);
            }
        }
        int threshold = Math.max(3, (int) Math.ceil(pages.size() * 0.60));
        return occurrences.entrySet().stream().filter(entry -> entry.getValue() >= threshold)
                .map(Map.Entry::getKey).collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    private static PdfRegion copy(PdfRegion region, int column, int order, Classification classification) {
        return new PdfRegion(region.id(), region.pageNumber(),
                region.xMin(), region.yMin(), region.xMax(), region.yMax(),
                column, order, region.text(), classification.type(), classification.narratability(),
                classification.reasons(), region.evidence(), region.evidenceCandidates(),
                region.override(), region.attributes(), region.revision());
    }

    private static List<Double> cluster(List<Double> starts, double tolerance) {
        ArrayList<Double> result = new ArrayList<>();
        for (double start : starts) {
            int match = -1;
            for (int i = 0; i < result.size(); i++) {
                if (Math.abs(result.get(i) - start) <= tolerance) {
                    match = i;
                    break;
                }
            }
            if (match < 0) result.add(start);
            else result.set(match, (result.get(match) + start) / 2.0);
        }
        result.sort(Double::compareTo);
        return result;
    }

    private static int nearestCluster(double x, List<Double> clusters) {
        if (clusters.isEmpty()) return 0;
        int best = 0;
        for (int i = 1; i < clusters.size(); i++) {
            if (Math.abs(clusters.get(i) - x) < Math.abs(clusters.get(best) - x)) best = i;
        }
        return best;
    }

    private static boolean looksLikeMath(String text) {
        String safe = text == null ? "" : text;
        long symbols = safe.chars().filter(ch -> "=+-*/^∑√∫≤≥≈".indexOf(ch) >= 0).count();
        return safe.length() >= 8 && symbols >= 3 && symbols / (double) safe.length() > 0.08;
    }

    private static boolean looksLikeCode(String text) {
        String safe = text == null ? "" : text;
        return safe.contains("{") && safe.contains("}")
                || safe.matches("(?s).*\\b(class|public|def|function|SELECT|FROM)\\b.*");
    }

    private static boolean looksLikeTable(String text) {
        String safe = text == null ? "" : text.strip();
        return safe.contains("\t") || safe.matches("(?s).*(\\s{3,}\\S+){2,}.*");
    }

    private static double width(PdfRegion region) {
        return region.xMax() - region.xMin();
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ").strip();
    }

    private record Classification(PdfRegionType type,
                                  PdfNarratability narratability,
                                  List<String> reasons) { }

    private record PageResult(PreparedPdfPage page, int columnCount, boolean changed) { }
}
