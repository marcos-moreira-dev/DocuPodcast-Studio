package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.*;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;

/** Preview-first page/document review; applying still requires explicit ids/state. */
public final class ReviewPdfTreatmentBatchUseCase {
    private final PreparedPdfDocumentRepository repository;
    private final UpdatePdfDerivedTreatmentUseCase update;

    public ReviewPdfTreatmentBatchUseCase(
            PreparedPdfDocumentRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.update = new UpdatePdfDerivedTreatmentUseCase(repository);
    }

    public PdfTreatmentReviewBatch preview(Path root, Integer pageNumber)
            throws IOException {
        ArrayList<PdfTreatmentReviewBatch.Item> items = new ArrayList<>();
        for (PreparedPdfPage page : repository.loadPages(root)) {
            if (pageNumber != null && page.pageNumber() != pageNumber) continue;
            page.derivedTreatments().stream()
                    .filter(value -> value.state() == PdfDerivedTreatmentState.DRAFT)
                    .sorted(java.util.Comparator.comparing(
                            PdfDerivedTreatment::createdAt))
                    .forEach(value -> items.add(new PdfTreatmentReviewBatch.Item(
                            value.id(), page.pageNumber(), value.kind(),
                            preview(value.derivedText()))));
        }
        return new PdfTreatmentReviewBatch(items);
    }

    public int apply(Path root, Collection<String> treatmentIds,
                     PdfDerivedTreatmentState state) throws IOException {
        if (state != PdfDerivedTreatmentState.APPROVED
                && state != PdfDerivedTreatmentState.REJECTED) {
            throw new IllegalArgumentException(
                    "La revisión por lote solo permite aprobar o rechazar.");
        }
        HashSet<String> requested = new HashSet<>(
                treatmentIds == null ? java.util.List.of() : treatmentIds);
        int changed = 0;
        for (PreparedPdfPage page : repository.loadPages(root)) {
            for (PdfDerivedTreatment treatment : page.derivedTreatments()) {
                if (!requested.remove(treatment.id())) continue;
                if (treatment.state() != PdfDerivedTreatmentState.DRAFT) continue;
                update.review(root, page.pageNumber(), treatment.id(), state);
                changed++;
            }
        }
        if (!requested.isEmpty()) {
            throw new IOException("Algunos borradores ya no existen: "
                    + String.join(", ", requested));
        }
        return changed;
    }

    private static String preview(String value) {
        String text = value == null ? "" : value.strip();
        return text.length() <= 160 ? text : text.substring(0, 157).strip() + "...";
    }
}
