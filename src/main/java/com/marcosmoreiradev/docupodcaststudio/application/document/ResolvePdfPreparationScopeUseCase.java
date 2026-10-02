package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlockType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.io.IOException;
import java.util.Objects;

/** Resolves page, range and section scopes without initiating extraction. */
public final class ResolvePdfPreparationScopeUseCase {
    private final PreparedPdfDocumentRepository repository;

    public ResolvePdfPreparationScopeUseCase(PreparedPdfDocumentRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public PdfPreparationScopeResolution resolve(PdfPreparationScopeRequest request) {
        int pageCount = pageCount(request.workspace());
        if (pageCount <= 0) {
            return new PdfPreparationScopeResolution(request.scope(), List.of(), false,
                    "El PDF no informa páginas.");
        }
        int current = Math.min(pageCount, request.currentPage());
        return switch (request.scope()) {
            case CURRENT_PAGE -> resolution(request.scope(), range(current, current), "Página actual.");
            case PAGE_RANGE -> explicitRange(request, pageCount);
            case WHOLE_DOCUMENT -> resolution(request.scope(), range(1, pageCount), "Documento completo.");
            case CURRENT_SECTION -> section(request, current, pageCount, false);
            case FROM_CURRENT_TO_SECTION_END -> section(request, current, pageCount, true);
        };
    }

    com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfDocumentManifest preferences(
            PreparedPdfWorkspaceRef workspace) throws IOException {
        return new PdfReadingPreferencesUseCase(repository).read(workspace);
    }

    private int pageCount(PreparedPdfWorkspaceRef workspace) {
        try {
            return repository.loadManifest(workspace.projectRoot())
                    .filter(manifest -> manifest.sourceSha256().equalsIgnoreCase(workspace.sourceSha256()))
                    .map(manifest -> manifest.pageCount())
                    .orElse(0);
        } catch (IOException ex) {
            return 0;
        }
    }

    private static PdfPreparationScopeResolution explicitRange(PdfPreparationScopeRequest request, int pageCount) {
        if (request.origin() == PdfPreparationOrigin.PROCESS_INTERVAL) {
            return strictExplicitRange(request, pageCount);
        }
        if (request.rangeStart() <= 0 || request.rangeEnd() <= 0) {
            return new PdfPreparationScopeResolution(request.scope(), List.of(), true,
                    "Selecciona la primera y la última página.");
        }
        int start = Math.min(pageCount, Math.min(request.rangeStart(), request.rangeEnd()));
        int end = Math.min(pageCount, Math.max(request.rangeStart(), request.rangeEnd()));
        return resolution(request.scope(), range(start, end), "Rango de páginas " + start + "-" + end + ".");
    }

    private static PdfPreparationScopeResolution strictExplicitRange(
            PdfPreparationScopeRequest request, int pageCount) {
        if (request.rangeStart() < 1) {
            return invalidRange(request, "La página inicial debe ser al menos 1.");
        }
        if (request.rangeEnd() < request.rangeStart()) {
            return invalidRange(request,
                    "La página final no puede ser anterior a la inicial.");
        }
        if (request.rangeEnd() > pageCount) {
            return invalidRange(request,
                    "La página final supera las " + pageCount + " páginas del PDF.");
        }
        return resolution(request.scope(),
                range(request.rangeStart(), request.rangeEnd()),
                "Intervalo de páginas " + request.rangeStart() + "-"
                        + request.rangeEnd() + ".");
    }

    private static PdfPreparationScopeResolution invalidRange(
            PdfPreparationScopeRequest request, String explanation) {
        return new PdfPreparationScopeResolution(request.scope(), List.of(), true,
                explanation);
    }

    private static PdfPreparationScopeResolution section(PdfPreparationScopeRequest request,
                                                         int current,
                                                         int pageCount,
                                                         boolean fromCurrent) {
        List<Integer> headings = headingPages(request.outline());
        int start = headings.stream().filter(page -> page <= current).max(Integer::compareTo).orElse(0);
        int endExclusive = headings.stream().filter(page -> page > current).min(Integer::compareTo)
                .orElse(pageCount + 1);
        if (start <= 0) {
            return new PdfPreparationScopeResolution(request.scope(), List.of(), true,
                    "No hay bookmark ni heading preparado que delimite la sección; selecciona un rango.");
        }
        int effectiveStart = fromCurrent ? current : start;
        int end = Math.max(effectiveStart, endExclusive - 1);
        String source = request.outline() != null
                && request.outline().origin() == DocumentOutlineOrigin.PDF_BOOKMARKS
                ? "bookmarks" : "headings preparados";
        return resolution(request.scope(), range(effectiveStart, end),
                "Sección resuelta mediante " + source + ".");
    }

    private static List<Integer> headingPages(DocumentOutlineProjection outline) {
        if (outline == null || (outline.origin() != DocumentOutlineOrigin.PDF_BOOKMARKS
                && outline.origin() != DocumentOutlineOrigin.HEADINGS
                && outline.origin() != DocumentOutlineOrigin.CONTENTS)) return List.of();
        ArrayList<Integer> pages = new ArrayList<>();
        collect(outline.entries(), pages);
        return pages.stream().filter(page -> page > 0).distinct().sorted().toList();
    }

    private static void collect(List<DocumentOutlineEntry> entries, List<Integer> pages) {
        for (DocumentOutlineEntry entry : entries == null ? List.<DocumentOutlineEntry>of() : entries) {
            if (entry.kind() == DocumentBlockType.TITLE || entry.kind() == DocumentBlockType.HEADING
                    || entry.kind() == DocumentBlockType.SUBHEADING
                    || entry.kind() == DocumentBlockType.EMPTY) {
                positive(entry.sourcePage()).ifPresent(pages::add);
            }
            collect(entry.children(), pages);
        }
    }

    private static java.util.Optional<Integer> positive(String value) {
        try {
            int parsed = Integer.parseInt(value == null ? "" : value.strip());
            return parsed > 0 ? java.util.Optional.of(parsed) : java.util.Optional.empty();
        } catch (NumberFormatException ex) {
            return java.util.Optional.empty();
        }
    }

    private static List<Integer> range(int start, int end) {
        return java.util.stream.IntStream.rangeClosed(start, end).boxed().toList();
    }

    private static PdfPreparationScopeResolution resolution(PdfPreparationScope scope,
                                                            List<Integer> pages,
                                                            String explanation) {
        return new PdfPreparationScopeResolution(scope, pages, false, explanation);
    }
}
