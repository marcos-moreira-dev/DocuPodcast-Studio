package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegion;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionOrigin;

import java.io.IOException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Searches all prepared V2 regions without starting OCR. */
public final class SearchPdfTextUseCase {
    private final PreparedPdfDocumentRepository repository;

    public SearchPdfTextUseCase(PreparedPdfDocumentRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public PdfTextSearchProjection search(PdfTextSearchRequest request) {
        PreparedPdfWorkspaceRef workspace = request == null ? null : request.workspace();
        String query = request == null ? "" : request.query();
        if (workspace == null || query.isBlank()) {
            return new PdfTextSearchProjection(query, List.of(), 0, List.of());
        }
        try {
            PreparedPdfRegionIndex index = repository.loadRegionIndex(workspace.projectRoot());
            String needle = normalize(query);
            ArrayList<PdfTextSearchResult> results = new ArrayList<>();
            int ordinal = 0;
            for (PreparedPdfRegionIndex.Entry entry : index.regions()) {
                PdfRegion region = entry.region();
                if (!normalize(region.effectiveText()).contains(needle)) continue;
                results.add(new PdfTextSearchResult("pdf-search-" + (++ordinal),
                        entry.pageNumber(), region.id(), snippet(region.effectiveText(), query),
                        new PdfPageRegion(entry.pageNumber(), region.xMin(), region.yMin(),
                                region.xMax(), region.yMax(),
                                entry.pageWidthPoints(), entry.pageHeightPoints()),
                        origin(region.evidence().origin()),
                        region.evidence().confidence()));
                if (results.size() >= request.maxResults()) {
                    return projection(query, results, preparedPageCount(index), request.includeOcr());
                }
            }
            return projection(query, results, preparedPageCount(index), request.includeOcr());
        } catch (IOException ex) {
            return new PdfTextSearchProjection(query, List.of(), 0,
                    List.of("No se pudo leer la preparación PDF V2: " + ex.getMessage()));
        }
    }

    private static int preparedPageCount(PreparedPdfRegionIndex index) {
        return (int) index.regions().stream().map(PreparedPdfRegionIndex.Entry::pageNumber)
                .distinct().count();
    }

    private static PdfTextSearchProjection projection(String query,
                                                       List<PdfTextSearchResult> results,
                                                       int pages,
                                                       boolean expandRequested) {
        List<String> warnings = expandRequested
                ? List.of("La búsqueda consultó las páginas preparadas. "
                + "«Ampliar búsqueda» debe solicitar un alcance PDF.")
                : List.of();
        return new PdfTextSearchProjection(query, results.stream()
                .sorted(Comparator.comparingInt(PdfTextSearchResult::pageNumber)).toList(),
                pages, warnings);
    }

    private static PdfTextLayerOrigin origin(PdfRegionOrigin origin) {
        return origin == PdfRegionOrigin.OCR_LOCAL || origin == PdfRegionOrigin.HYBRID
                ? PdfTextLayerOrigin.OCR_LOCAL : PdfTextLayerOrigin.NATIVE_BBOX;
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ").strip();
    }

    private static String snippet(String text, String query) {
        String safe = text == null ? "" : text.replaceAll("\\s+", " ").strip();
        String normalized = normalize(safe);
        String needle = normalize(query);
        int index = normalized.indexOf(needle);
        if (index < 0) return safe.length() > 140 ? safe.substring(0, 140).strip() + "..." : safe;
        int start = Math.max(0, index - 48);
        int end = Math.min(safe.length(), index + needle.length() + 72);
        return (start > 0 ? "... " : "") + safe.substring(start, end).strip()
                + (end < safe.length() ? " ..." : "");
    }
}
