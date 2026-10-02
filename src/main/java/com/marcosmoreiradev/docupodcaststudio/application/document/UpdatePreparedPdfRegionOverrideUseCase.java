package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfNarratability;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegion;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionOverride;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionType;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PreparedPdfPage;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfDerivedTreatmentState;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Objects;

/** Persists independent manual overrides without mutating the automatic PDF analysis. */
public final class UpdatePreparedPdfRegionOverrideUseCase {
    private final PreparedPdfDocumentRepository repository;

    public UpdatePreparedPdfRegionOverrideUseCase(PreparedPdfDocumentRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public PreparedPdfPage execute(PreparedPdfWorkspaceRef workspace,
                                   int pageNumber,
                                   String regionId,
                                   OverridePatch patch) throws IOException {
        Objects.requireNonNull(patch, "patch");
        Objects.requireNonNull(workspace, "workspace");
        PreparedPdfPage page = repository.loadPage(workspace.projectRoot(), pageNumber)
                .orElseThrow(() -> new IOException("La página PDF todavía no está preparada."));
        ArrayList<PdfRegion> regions = new ArrayList<>(page.regions().size());
        boolean found = false;
        for (PdfRegion region : page.regions()) {
            if (!region.id().equals(regionId)) {
                regions.add(region);
                continue;
            }
            found = true;
            PdfRegionOverride current = region.override();
            PdfRegionOverride next = new PdfRegionOverride(
                    patch.text().apply(current.text()),
                    patch.type().apply(current.type()),
                    patch.narratability().apply(current.narratability()),
                    patch.readingOrder().apply(current.readingOrder()));
            regions.add(new PdfRegion(region.id(), region.pageNumber(),
                    region.xMin(), region.yMin(), region.xMax(), region.yMax(),
                    region.columnIndex(), region.readingOrder(), region.text(),
                    region.automaticType(), region.automaticNarratability(), region.reasons(),
                    region.evidence(), region.evidenceCandidates(), next,
                    region.attributes(), region.revision() + 1));
        }
        if (!found) {
            throw new IOException("No existe la región PDF " + regionId + " en la página " + pageNumber + ".");
        }
        PreparedPdfPage updated = new PreparedPdfPage(page.schemaVersion(), page.pageNumber(),
                page.widthPoints(), page.heightPoints(), page.status(), page.revision() + 1,
                regions, page.derivedTreatments().stream()
                        .map(treatment -> treatment.sourceRegionIds().contains(regionId)
                                ? treatment.withState(PdfDerivedTreatmentState.STALE) : treatment)
                        .toList(),
                page.preparationMetrics(), page.analysisProfile(),
                page.warnings(), page.lastAttemptError());
        repository.savePage(workspace.projectRoot(), updated);
        return updated;
    }

    public record OverridePatch(
            OverrideValue<String> text,
            OverrideValue<PdfRegionType> type,
            OverrideValue<PdfNarratability> narratability,
            OverrideValue<Integer> readingOrder
    ) {
        public OverridePatch {
            text = Objects.requireNonNullElseGet(text, OverrideValue::keep);
            type = Objects.requireNonNullElseGet(type, OverrideValue::keep);
            narratability = Objects.requireNonNullElseGet(narratability, OverrideValue::keep);
            readingOrder = Objects.requireNonNullElseGet(readingOrder, OverrideValue::keep);
        }

        public static OverridePatch restoreAutomaticDecision() {
            return new OverridePatch(OverrideValue.clear(), OverrideValue.clear(),
                    OverrideValue.clear(), OverrideValue.clear());
        }
    }

    /** KEEP leaves an override untouched; SET replaces it; CLEAR restores the automatic field. */
    public record OverrideValue<T>(Action action, T value) {
        public OverrideValue {
            action = Objects.requireNonNullElse(action, Action.KEEP);
            if (action != Action.SET) value = null;
        }

        public static <T> OverrideValue<T> keep() {
            return new OverrideValue<>(Action.KEEP, null);
        }

        public static <T> OverrideValue<T> set(T value) {
            return new OverrideValue<>(Action.SET, value);
        }

        public static <T> OverrideValue<T> clear() {
            return new OverrideValue<>(Action.CLEAR, null);
        }

        T apply(T current) {
            return switch (action) {
                case KEEP -> current;
                case SET -> value;
                case CLEAR -> null;
            };
        }
    }

    public enum Action {
        KEEP,
        SET,
        CLEAR
    }
}
