package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfDerivedTreatment;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/** Lists derivatives attached to one canonical region, newest first. */
public final class ListPdfDerivedTreatmentsUseCase {
    private final PreparedPdfDocumentRepository repository;

    public ListPdfDerivedTreatmentsUseCase(PreparedPdfDocumentRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public List<PdfDerivedTreatment> forRegion(Path projectRoot, int pageNumber, String regionId)
            throws IOException {
        if (projectRoot == null || regionId == null || regionId.isBlank()) return List.of();
        return repository.loadPage(projectRoot, pageNumber).stream()
                .flatMap(page -> page.derivedTreatments().stream())
                .filter(value -> value.sourceRegionIds().contains(regionId))
                .sorted(java.util.Comparator.comparing(PdfDerivedTreatment::createdAt).reversed())
                .toList();
    }
}
