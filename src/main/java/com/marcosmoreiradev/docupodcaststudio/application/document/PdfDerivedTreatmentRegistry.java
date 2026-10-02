package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfDerivedTreatmentKind;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Catalog of opt-in, local-only PDF interpretations. It does not install or execute models. */
public final class PdfDerivedTreatmentRegistry {
    private final Map<PdfDerivedTreatmentKind, PdfDerivedTreatmentDescriptor> descriptors;

    public PdfDerivedTreatmentRegistry(List<PdfDerivedTreatmentDescriptor> values) {
        EnumMap<PdfDerivedTreatmentKind, PdfDerivedTreatmentDescriptor> indexed =
                new EnumMap<>(PdfDerivedTreatmentKind.class);
        for (PdfDerivedTreatmentDescriptor value : values == null
                ? List.<PdfDerivedTreatmentDescriptor>of() : values) {
            if (indexed.put(value.kind(), value) != null) {
                throw new IllegalArgumentException("Duplicate PDF derived treatment: " + value.kind());
            }
        }
        descriptors = Map.copyOf(indexed);
    }

    public static PdfDerivedTreatmentRegistry localDefaults() {
        return new PdfDerivedTreatmentRegistry(List.of(
                descriptor(PdfDerivedTreatmentKind.TABLE_STRUCTURE,
                        "document-layout-analysis",
                        "Estructura revisable de tabla"),
                descriptor(PdfDerivedTreatmentKind.TABLE_NARRATION,
                        "pdf-table-narration",
                        "Lectura académica de tablas"),
                descriptor(PdfDerivedTreatmentKind.SMALL_TABLE_NARRATION, "pdf-table-narration", "Narración de tablas pequeñas"),
                descriptor(PdfDerivedTreatmentKind.MATHEMATICAL_READING, "pdf-math-reading", "Lectura matemática"),
                descriptor(PdfDerivedTreatmentKind.IMAGE_DESCRIPTION, "pdf-image-description", "Descripción de imágenes y gráficos"),
                descriptor(PdfDerivedTreatmentKind.CONTEXTUAL_CORRECTION, "pdf-context-correction", "Corrección contextual"),
                descriptor(PdfDerivedTreatmentKind.NARRATABILITY_REVIEW,
                        "pdf-narratability-review", "Revisión de galimatías con IA local"),
                descriptor(PdfDerivedTreatmentKind.LIGHTWEIGHT_LANGUAGE_MODEL, "pdf-light-language-model", "Modelo lingüístico local ligero")));
    }

    public List<PdfDerivedTreatmentDescriptor> all() {
        return descriptors.values().stream()
                .sorted(java.util.Comparator.comparing(value -> value.kind().name())).toList();
    }

    public Optional<PdfDerivedTreatmentDescriptor> find(PdfDerivedTreatmentKind kind) {
        return Optional.ofNullable(descriptors.get(kind));
    }

    private static PdfDerivedTreatmentDescriptor descriptor(PdfDerivedTreatmentKind kind,
                                                            String capabilityId,
                                                            String displayName) {
        return new PdfDerivedTreatmentDescriptor(kind, capabilityId, displayName, false, true);
    }
}
