package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfDerivedTreatment;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfDerivedTreatmentState;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfDerivedTreatmentKind;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfNarratability;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegion;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PreparedPdfPage;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Objects;

/** Publishes or removes a regenerable derivative without touching source regions. */
public final class UpdatePdfDerivedTreatmentUseCase {
    private final PreparedPdfDocumentRepository repository;

    public UpdatePdfDerivedTreatmentUseCase(PreparedPdfDocumentRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public PreparedPdfPage upsert(Path projectRoot, int pageNumber, PdfDerivedTreatment treatment)
            throws IOException {
        Objects.requireNonNull(treatment, "treatment");
        PreparedPdfPage page = page(projectRoot, pageNumber);
        ArrayList<PdfDerivedTreatment> values = new ArrayList<>(page.derivedTreatments());
        values.removeIf(existing -> existing.id().equals(treatment.id()));
        values.add(treatment);
        return save(page, projectRoot, values);
    }

    public PreparedPdfPage remove(Path projectRoot, int pageNumber, String treatmentId) throws IOException {
        PreparedPdfPage page = page(projectRoot, pageNumber);
        ArrayList<PdfDerivedTreatment> values = new ArrayList<>(page.derivedTreatments());
        values.removeIf(existing -> existing.id().equals(treatmentId));
        return save(page, projectRoot, values);
    }

    public PreparedPdfPage review(Path projectRoot, int pageNumber, String treatmentId,
                                  PdfDerivedTreatmentState state) throws IOException {
        return review(projectRoot, pageNumber, treatmentId, null, state);
    }

    /**
     * Reviews a treatment and republishes the generated value first when a
     * concurrent page refresh replaced the page snapshot that contained it.
     */
    public PreparedPdfPage reviewOrUpsert(Path projectRoot, int pageNumber,
                                          PdfDerivedTreatment treatment,
                                          PdfDerivedTreatmentState state) throws IOException {
        Objects.requireNonNull(treatment, "treatment");
        return review(projectRoot, pageNumber, treatment.id(), treatment, state);
    }

    private PreparedPdfPage review(Path projectRoot, int pageNumber, String treatmentId,
                                   PdfDerivedTreatment fallback,
                                   PdfDerivedTreatmentState state) throws IOException {
        if (state != PdfDerivedTreatmentState.APPROVED
                && state != PdfDerivedTreatmentState.REJECTED) {
            throw new IllegalArgumentException("Manual review must approve or reject a derivative");
        }
        PreparedPdfPage page = page(projectRoot, pageNumber);
        ArrayList<PdfDerivedTreatment> values = new ArrayList<>(page.derivedTreatments());
        boolean found = false;
        PdfDerivedTreatment reviewed = null;
        for (int index = 0; index < values.size(); index++) {
            if (values.get(index).id().equals(treatmentId)) {
                reviewed = values.get(index).withState(state);
                values.set(index, reviewed);
                found = true;
                break;
            }
        }
        if (!found && fallback != null && fallback.id().equals(treatmentId)) {
            reviewed = fallback.withState(state);
            values.add(reviewed);
            found = true;
        }
        if (!found) throw new IOException("No existe el tratamiento derivado " + treatmentId + ".");
        java.util.List<PdfRegion> regions = page.regions();
        if (state == PdfDerivedTreatmentState.APPROVED
                && reviewed.kind() == PdfDerivedTreatmentKind.NARRATABILITY_REVIEW) {
            regions = applyNarratabilityDecisions(page.regions(), reviewed);
        }
        return save(page, projectRoot, regions, values);
    }

    private PreparedPdfPage page(Path root, int page) throws IOException {
        return repository.loadPage(root, page)
                .orElseThrow(() -> new IOException("La página PDF aún no está preparada: " + page));
    }

    private PreparedPdfPage save(PreparedPdfPage page,
                                 Path root,
                                 java.util.List<PdfDerivedTreatment> treatments) throws IOException {
        return save(page, root, page.regions(), treatments);
    }

    private PreparedPdfPage save(PreparedPdfPage page, Path root,
                                 java.util.List<PdfRegion> regions,
                                 java.util.List<PdfDerivedTreatment> treatments) throws IOException {
        PreparedPdfPage updated = new PreparedPdfPage(page.schemaVersion(), page.pageNumber(),
                page.widthPoints(), page.heightPoints(), page.status(), page.revision() + 1,
                regions, treatments, page.preparationMetrics(),
                page.analysisProfile(),
                page.warnings(), page.lastAttemptError());
        repository.savePage(root, updated);
        return updated;
    }

    private static java.util.List<PdfRegion> applyNarratabilityDecisions(
            java.util.List<PdfRegion> regions, PdfDerivedTreatment treatment) throws IOException {
        ArrayList<PdfRegion> updated = new ArrayList<>(regions.size());
        java.util.Set<String> reviewedRegionIds =
                java.util.Set.copyOf(treatment.sourceRegionIds());
        for (PdfRegion region : regions) {
            if (!reviewedRegionIds.contains(region.id())) {
                updated.add(region);
                continue;
            }
            String value = treatment.metadata().get("decision." + region.id());
            if (value == null || value.isBlank()) {
                throw new IOException("La revisión no contiene una decisión para " + region.id() + ".");
            }
            try {
                updated.add(region.withNarratabilityOverride(PdfNarratability.valueOf(value)));
            } catch (IllegalArgumentException invalid) {
                throw new IOException("La revisión contiene una decisión inválida para "
                        + region.id() + ".", invalid);
            }
        }
        return java.util.List.copyOf(updated);
    }
}
