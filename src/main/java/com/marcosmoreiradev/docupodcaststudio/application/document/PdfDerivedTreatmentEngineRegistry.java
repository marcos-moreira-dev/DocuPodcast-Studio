package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfDerivedTreatmentKind;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Contains only executable local engines; catalog descriptors alone never imply readiness. */
public final class PdfDerivedTreatmentEngineRegistry {
    private final Map<String, PdfDerivedTreatmentEngine> engines;

    public PdfDerivedTreatmentEngineRegistry(List<PdfDerivedTreatmentEngine> values) {
        LinkedHashMap<String, PdfDerivedTreatmentEngine> indexed = new LinkedHashMap<>();
        for (PdfDerivedTreatmentEngine engine : values == null
                ? List.<PdfDerivedTreatmentEngine>of() : values) {
            if (indexed.put(engine.id(), engine) != null) {
                throw new IllegalArgumentException("Duplicate PDF treatment engine: " + engine.id());
            }
        }
        engines = Map.copyOf(indexed);
    }

    public List<PdfDerivedTreatmentEngine> all() {
        return engines.values().stream().toList();
    }

    public Optional<PdfDerivedTreatmentEngine> resolve(PdfDerivedTreatmentKind kind, String preferredId) {
        if (preferredId != null && !preferredId.isBlank()) {
            PdfDerivedTreatmentEngine engine = engines.get(preferredId.strip());
            return engine != null && engine.kind() == kind ? Optional.of(engine) : Optional.empty();
        }
        return engines.values().stream().filter(engine -> engine.kind() == kind).findFirst();
    }
}
