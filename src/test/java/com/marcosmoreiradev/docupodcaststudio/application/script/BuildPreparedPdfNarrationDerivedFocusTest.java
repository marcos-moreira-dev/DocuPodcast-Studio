package com.marcosmoreiradev.docupodcaststudio.application.script;

import com.marcosmoreiradev.docupodcaststudio.application.document.PdfNarrationFocusMetadata;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfNarrationBindingMetadata;
import com.marcosmoreiradev.docupodcaststudio.application.document.PreparedPdfWorkspaceRef;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.*;
import com.marcosmoreiradev.docupodcaststudio.infrastructure.document.JsonPreparedPdfDocumentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BuildPreparedPdfNarrationDerivedFocusTest {
    @TempDir Path temp;

    @Test
    void narratesOnlyApprovedCurrentVisualDerivativeAndCarriesFocusGeometry() throws Exception {
        JsonPreparedPdfDocumentRepository repository = new JsonPreparedPdfDocumentRepository();
        String sha = "b".repeat(64);
        repository.initialize(temp, new PdfDocumentManifest(
                PdfDocumentManifest.CURRENT_SCHEMA_VERSION, "Libro", "source.pdf", sha,
                1, "v3", Instant.now(), Instant.now()));
        PdfRegion image = new PdfRegion("IMG-1", 1, 40, 100, 560, 500, 0, 0, "",
                PdfRegionType.IMAGE, PdfNarratability.NON_NARRATABLE, List.of(),
                new PdfRegionEvidence(PdfRegionOrigin.OCR_LOCAL, 0.8,
                        "x", "p", "g", "c"), PdfRegionOverride.empty(), Map.of(), 3);
        PdfDerivedTreatment approved = new PdfDerivedTreatment("DER-A",
                PdfDerivedTreatmentKind.IMAGE_DESCRIPTION, List.of(image.id()),
                "La figura presenta una curva creciente.", "qwen", "1", 0.9, Instant.now(),
                PdfDerivedTreatmentState.APPROVED, 3, "fingerprint-a", "prompt", Map.of());
        PdfDerivedTreatment stale = new PdfDerivedTreatment("DER-S",
                PdfDerivedTreatmentKind.IMAGE_DESCRIPTION, List.of(image.id()),
                "Texto obsoleto.", "qwen", "1", 0.9, Instant.now().plusSeconds(1),
                PdfDerivedTreatmentState.STALE, 2, "fingerprint-s", "prompt", Map.of());
        repository.savePage(temp, new PreparedPdfPage(PreparedPdfPage.CURRENT_SCHEMA_VERSION,
                1, 612, 792, PdfPagePreparationStatus.READY, 3, List.of(image),
                List.of(approved, stale), PdfPagePreparationMetrics.empty(),
                PdfPageAnalysisProfile.defaults(), List.of(), ""));

        var script = new BuildPreparedPdfNarrationUseCase(repository).build(
                new PreparedPdfWorkspaceRef(temp, temp.resolve("source/source.pdf"), sha),
                "Libro", "es", false, null);

        assertEquals(1, script.segments().size());
        var segment = script.segments().getFirst();
        assertEquals(approved.derivedText(), segment.narrationText());
        var focus = PdfNarrationFocusMetadata.decode(
                segment.metadata().get(PdfNarrationFocusMetadata.KEY)).orElseThrow();
        assertEquals(1, focus.pageNumber());
        assertEquals(40.0, focus.boxes().getFirst().xMin());
        assertEquals("DER-A", segment.metadata().get("pdfDerivedTreatmentId"));
        PdfNarrationBinding binding = PdfNarrationBindingMetadata.decode(
                segment.metadata().get(PdfNarrationBindingMetadata.KEY)).orElseThrow();
        assertEquals(PdfSemanticTextLayer.INTERPRETATION, binding.sourceLayer());
        assertEquals(PdfObjectNarrationPolicy.DESCRIBE_BRIEFLY,
                binding.policy());
        assertEquals("DER-A", binding.interpretationId());
    }

    @Test
    void approvedTableNarrationFocusesCitedCellsInsteadOfCoveringThePage()
            throws Exception {
        JsonPreparedPdfDocumentRepository repository =
                new JsonPreparedPdfDocumentRepository();
        String sha = "c".repeat(64);
        repository.initialize(temp, new PdfDocumentManifest(
                PdfDocumentManifest.CURRENT_SCHEMA_VERSION, "Tabla",
                "source.pdf", sha, 1, "v3", Instant.now(), Instant.now()));
        PdfRegion table = new PdfRegion("TABLE-1", 1, 20, 100, 580, 500,
                0, 0, "A | B\n1 | 2", PdfRegionType.TABLE,
                PdfNarratability.NON_NARRATABLE, List.of(),
                new PdfRegionEvidence(PdfRegionOrigin.OCR_LOCAL, 0.8,
                        "x", "p", "g", "c"),
                PdfRegionOverride.empty(), Map.of(), 2);
        PdfDerivedTreatment treatment = new PdfDerivedTreatment("DER-T",
                PdfDerivedTreatmentKind.TABLE_NARRATION, List.of(table.id()),
                "El valor máximo es dos.", "local", "2", 1.0, Instant.now(),
                PdfDerivedTreatmentState.APPROVED, 2, "fingerprint-t",
                "prompt", Map.of("focusCellIds", "CELL-X",
                "focusCell.CELL-X", "300,300,500,400"));
        repository.savePage(temp, new PreparedPdfPage(
                PreparedPdfPage.CURRENT_SCHEMA_VERSION, 1, 612, 792,
                PdfPagePreparationStatus.READY, 2, List.of(table),
                List.of(treatment), PdfPagePreparationMetrics.empty(),
                PdfPageAnalysisProfile.defaults(), List.of(), ""));

        var script = new BuildPreparedPdfNarrationUseCase(repository).build(
                new PreparedPdfWorkspaceRef(temp,
                        temp.resolve("source/source.pdf"), sha),
                "Tabla", "es", false,
                com.marcosmoreiradev.docupodcaststudio.domain.reading
                        .ReadingProfile.academicDefaults());

        var focus = PdfNarrationFocusMetadata.decode(
                script.segments().getFirst().metadata()
                        .get(PdfNarrationFocusMetadata.KEY)).orElseThrow();
        assertEquals("Celdas narradas", focus.accessibleLabel());
        assertEquals(300.0, focus.boxes().getFirst().xMin());
        assertEquals(500.0, focus.boxes().getFirst().xMax());
    }

    @Test
    void groupedGraphDerivativeIsNarratedOnceWithTheCompleteRoi()
            throws Exception {
        JsonPreparedPdfDocumentRepository repository =
                new JsonPreparedPdfDocumentRepository();
        String sha = "d".repeat(64);
        repository.initialize(temp, new PdfDocumentManifest(
                PdfDocumentManifest.CURRENT_SCHEMA_VERSION, "Gráfica",
                "source.pdf", sha, 1, "v3", Instant.now(), Instant.now()));
        PdfRegion pi = region("LABEL-PI", 120, 240, 150, 265, 2, "π");
        PdfRegion hole = region("LABEL-HOLE", 290, 300, 350, 330, 3, "hueco");
        PdfRegion caption = new PdfRegion(
                "CAPTION-1", 1, 80, 500, 540, 530, 0, 4,
                "Gráfica 2. La función se aproxima a uno.",
                PdfRegionType.CAPTION, PdfNarratability.NON_NARRATABLE,
                List.of(), evidence(), PdfRegionOverride.empty(), Map.of(), 2);
        PdfDerivedTreatment grouped = new PdfDerivedTreatment(
                "DER-GRAPH", PdfDerivedTreatmentKind.IMAGE_DESCRIPTION,
                List.of(pi.id(), hole.id(), caption.id()),
                "Gráfica de una curva que se aproxima a uno y presenta un hueco.",
                "local-vision", "1", 0.95, Instant.now(),
                PdfDerivedTreatmentState.APPROVED, 2, "fingerprint-graph",
                "prompt", Map.of(
                "technicalElementId", "DTE-GRAPH",
                "anchorRegionId", caption.id(),
                "focusBounds", "50,120,570,550"));
        PdfDerivedTreatment oldLabel = new PdfDerivedTreatment(
                "DER-LABEL", PdfDerivedTreatmentKind.IMAGE_DESCRIPTION,
                List.of(pi.id()), "Pi.", "local-vision", "1", 0.9,
                Instant.now().plusSeconds(1), PdfDerivedTreatmentState.APPROVED,
                2, "fingerprint-label", "prompt", Map.of());
        repository.savePage(temp, new PreparedPdfPage(
                PreparedPdfPage.CURRENT_SCHEMA_VERSION, 1, 612, 792,
                PdfPagePreparationStatus.READY, 2,
                List.of(pi, hole, caption), List.of(grouped, oldLabel),
                PdfPagePreparationMetrics.empty(),
                PdfPageAnalysisProfile.defaults(), List.of(), ""));

        var script = new BuildPreparedPdfNarrationUseCase(repository).build(
                new PreparedPdfWorkspaceRef(temp,
                        temp.resolve("source/source.pdf"), sha),
                "Gráfica", "es", false, null);

        assertEquals(1, script.segments().size());
        var segment = script.segments().getFirst();
        assertEquals(grouped.derivedText(), segment.narrationText());
        var focus = PdfNarrationFocusMetadata.decode(
                segment.metadata().get(PdfNarrationFocusMetadata.KEY))
                .orElseThrow();
        assertEquals(1, focus.boxes().size());
        assertEquals(50.0, focus.boxes().getFirst().xMin());
        assertEquals(570.0, focus.boxes().getFirst().xMax());
        assertEquals("DER-GRAPH",
                segment.metadata().get("pdfDerivedTreatmentId"));
    }

    @Test
    void mixedPageNarratesSafeProseAndApprovedManualDescriptionOnly()
            throws Exception {
        JsonPreparedPdfDocumentRepository repository =
                new JsonPreparedPdfDocumentRepository();
        String sha = "f".repeat(64);
        repository.initialize(temp, new PdfDocumentManifest(
                PdfDocumentManifest.CURRENT_SCHEMA_VERSION, "Mixto",
                "source.pdf", sha, 1, "v3", Instant.now(), Instant.now()));
        PdfRegion prose = typedRegion(
                "PROSE", 0, "La célula contiene material genético.",
                PdfRegionType.PARAGRAPH, PdfNarratability.NARRATABLE);
        PdfRegion uncertain = typedRegion(
                "UNCERTAIN", 1, "xqz | 1lI || ?",
                PdfRegionType.PARAGRAPH, PdfNarratability.UNCERTAIN);
        PdfRegion unknown = typedRegion(
                "UNKNOWN", 2, "Símbolos clasificados erróneamente",
                PdfRegionType.UNKNOWN, PdfNarratability.NARRATABLE);
        PdfRegion math = typedRegion(
                "MATH", 3, "x | / ? = 1",
                PdfRegionType.MATH, PdfNarratability.NARRATABLE);
        PdfRegion caption = typedRegion(
                "CAPTION", 4, "Figura 1. Curva y sus etiquetas internas.",
                PdfRegionType.CAPTION, PdfNarratability.NARRATABLE);
        PdfRegion pendingImage = typedRegion(
                "PENDING-IMAGE", 5, "",
                PdfRegionType.IMAGE, PdfNarratability.NON_NARRATABLE);
        PdfRegion manualImage = typedRegion(
                "MANUAL-IMAGE", 6, "",
                PdfRegionType.IMAGE, PdfNarratability.NON_NARRATABLE);
        Instant created = Instant.parse("2026-08-01T00:00:00Z");
        List<PdfDerivedTreatment> treatments = List.of(
                treatment("DRAFT", pendingImage, PdfDerivedTreatmentState.DRAFT,
                        "Descripción borrador.", "local-model", created),
                treatment("REJECTED", pendingImage, PdfDerivedTreatmentState.REJECTED,
                        "Descripción rechazada.", "local-model", created.plusSeconds(1)),
                treatment("STALE", pendingImage, PdfDerivedTreatmentState.STALE,
                        "Descripción obsoleta.", "local-model", created.plusSeconds(2)),
                treatment("MANUAL", manualImage, PdfDerivedTreatmentState.APPROVED,
                        "La figura muestra una doble hélice.", "manual-user",
                        created.plusSeconds(3)));
        repository.savePage(temp, new PreparedPdfPage(
                PreparedPdfPage.CURRENT_SCHEMA_VERSION,
                1, 612, 792, PdfPagePreparationStatus.READY, 1,
                List.of(prose, uncertain, unknown, math, caption,
                        pendingImage, manualImage),
                treatments, PdfPagePreparationMetrics.empty(),
                PdfPageAnalysisProfile.defaults(), List.of(), ""));

        var script = new BuildPreparedPdfNarrationUseCase(repository).build(
                new PreparedPdfWorkspaceRef(
                        temp, temp.resolve("source/source.pdf"), sha),
                "Mixto", "es", false, null);

        assertEquals(List.of(
                        "La célula contiene material genético.",
                        "Figura 1. Curva y sus etiquetas internas.",
                        "La figura muestra una doble hélice."),
                script.segments().stream()
                        .map(segment -> segment.narrationText()).toList());
        assertEquals(List.of("PROSE", "CAPTION", "MANUAL-IMAGE"),
                script.segments().stream()
                        .map(segment -> segment.sourceBlockIds().getFirst()).toList());
        PdfNarrationBinding proseBinding = PdfNarrationBindingMetadata.decode(
                script.segments().getFirst().metadata()
                        .get(PdfNarrationBindingMetadata.KEY)).orElseThrow();
        PdfNarrationBinding manualBinding = PdfNarrationBindingMetadata.decode(
                script.segments().get(2).metadata()
                        .get(PdfNarrationBindingMetadata.KEY)).orElseThrow();
        assertEquals(PdfSemanticTextLayer.LITERAL, proseBinding.sourceLayer());
        assertEquals(PdfObjectNarrationPolicy.READ_EXACT, proseBinding.policy());
        assertEquals(PdfSemanticTextLayer.INTERPRETATION,
                manualBinding.sourceLayer());
        assertEquals("DER-MANUAL", manualBinding.interpretationId());
    }

    @Test
    void humanLiteralCorrectionWinsWithoutDeletingExtractedEvidence()
            throws Exception {
        JsonPreparedPdfDocumentRepository repository =
                new JsonPreparedPdfDocumentRepository();
        String sha = "9".repeat(64);
        repository.initialize(temp, new PdfDocumentManifest(
                PdfDocumentManifest.CURRENT_SCHEMA_VERSION, "Corrección",
                "source.pdf", sha, 1, "v3", Instant.now(), Instant.now()));
        PdfRegion corrected = new PdfRegion(
                "TEXT-CORRECTED", 1, 40, 100, 560, 160, 0, 0,
                "La celula conserva el ADN.", PdfRegionType.PARAGRAPH,
                PdfNarratability.NARRATABLE, List.of(), evidence(),
                new PdfRegionOverride(
                        "La célula conserva el ADN.", null, null, null),
                Map.of(), 2);
        repository.savePage(temp, new PreparedPdfPage(
                PreparedPdfPage.CURRENT_SCHEMA_VERSION,
                1, 612, 792, PdfPagePreparationStatus.READY, 2,
                List.of(corrected), List.of(),
                PdfPagePreparationMetrics.empty(),
                PdfPageAnalysisProfile.defaults(), List.of(), ""));

        var script = new BuildPreparedPdfNarrationUseCase(repository).build(
                new PreparedPdfWorkspaceRef(
                        temp, temp.resolve("source/source.pdf"), sha),
                "Corrección", "es", false, null);

        assertEquals("La célula conserva el ADN.",
                script.segments().getFirst().narrationText());
        assertEquals("La celula conserva el ADN.", corrected.text());
        assertEquals("human-override", script.segments().getFirst().metadata()
                .get("pdfLiteralSource"));
        PdfNarrationBinding binding = PdfNarrationBindingMetadata.decode(
                script.segments().getFirst().metadata()
                        .get(PdfNarrationBindingMetadata.KEY)).orElseThrow();
        assertEquals(PdfSemanticTextLayer.LITERAL, binding.sourceLayer());
        assertTrue(binding.sourceFingerprint().startsWith("human-override|"));
    }

    @Test
    void outdatedApprovedTableNarrationCannotReenterTheScript() throws Exception {
        JsonPreparedPdfDocumentRepository repository =
                new JsonPreparedPdfDocumentRepository();
        String sha = "a".repeat(64);
        repository.initialize(temp, new PdfDocumentManifest(
                PdfDocumentManifest.CURRENT_SCHEMA_VERSION, "Tabla antigua",
                "source.pdf", sha, 1, "v3", Instant.now(), Instant.now()));
        PdfRegion table = new PdfRegion(
                "TABLE-OLD", 1, 40, 100, 560, 300, 0, 0,
                "A | B\n1 | 2", PdfRegionType.TABLE,
                PdfNarratability.NARRATABLE, List.of(), evidence(),
                PdfRegionOverride.empty(), Map.of(), 2);
        PdfDerivedTreatment outdated = new PdfDerivedTreatment(
                "DER-OLD", PdfDerivedTreatmentKind.TABLE_NARRATION,
                List.of(table.id()), "Resumen generativo desactualizado.",
                "local-model", "1", 0.9, Instant.now(),
                PdfDerivedTreatmentState.APPROVED, 1,
                "old-fingerprint", "prompt", Map.of());
        repository.savePage(temp, new PreparedPdfPage(
                PreparedPdfPage.CURRENT_SCHEMA_VERSION,
                1, 612, 792, PdfPagePreparationStatus.READY, 2,
                List.of(table), List.of(outdated),
                PdfPagePreparationMetrics.empty(),
                PdfPageAnalysisProfile.defaults(), List.of(), ""));

        var script = new BuildPreparedPdfNarrationUseCase(repository).build(
                new PreparedPdfWorkspaceRef(
                        temp, temp.resolve("source/source.pdf"), sha),
                "Tabla antigua", "es", false,
                com.marcosmoreiradev.docupodcaststudio.domain.reading
                        .ReadingProfile.academicDefaults());

        assertEquals(1, script.segments().size());
        assertFalse(script.segments().getFirst().narrationText()
                .contains("Resumen generativo desactualizado."));
        assertFalse(script.segments().getFirst().narrationText().contains("|"));
        assertTrue(script.segments().getFirst().narrationText().startsWith("Tabla con"));
    }

    @Test
    void admitsOnlyCurrentValidatedQ8DraftAuthorizedByTheSelectedPolicy()
            throws Exception {
        JsonPreparedPdfDocumentRepository repository =
                new JsonPreparedPdfDocumentRepository();
        String sha = "f".repeat(64);
        repository.initialize(temp, new PdfDocumentManifest(
                PdfDocumentManifest.CURRENT_SCHEMA_VERSION, "Q8", "source.pdf",
                sha, 1, "v3", Instant.now(), Instant.now()));
        PdfRegion image = typedRegion("Q8-IMAGE", 1, "",
                PdfRegionType.IMAGE, PdfNarratability.NON_NARRATABLE);
        Map<String, String> safeMetadata = Map.of(
                "automaticAdmission", com.marcosmoreiradev.docupodcaststudio
                        .application.document.PdfSemanticNarrationSafetyValidator
                        .AUTOMATIC_ADMISSION,
                "groundingStatus", "OK",
                "ttsSafetyValidated", "true",
                "sourceFingerprintValidated", "true",
                "semanticStrategy", "BRIEF_DESCRIPTION");
        PdfDerivedTreatment draft = new PdfDerivedTreatment(
                "DER-Q8", PdfDerivedTreatmentKind.IMAGE_DESCRIPTION,
                List.of(image.id()), "La figura compara dos curvas.",
                "qwen3-vl:4b-instruct-q8_0", "3", 0.9, Instant.now(),
                PdfDerivedTreatmentState.DRAFT, image.revision(),
                "fingerprint-q8", "prompt", safeMetadata);
        repository.savePage(temp, new PreparedPdfPage(
                PreparedPdfPage.CURRENT_SCHEMA_VERSION, 1, 612, 792,
                PdfPagePreparationStatus.READY, 1, List.of(image),
                List.of(draft), PdfPagePreparationMetrics.empty(),
                PdfPageAnalysisProfile.defaults(), List.of(), ""));

        BuildPreparedPdfNarrationUseCase useCase =
                new BuildPreparedPdfNarrationUseCase(repository);
        PreparedPdfWorkspaceRef workspace = new PreparedPdfWorkspaceRef(
                temp, temp.resolve("source/source.pdf"), sha);
        var admitted = useCase.build(workspace, "Q8", "es", false, null,
                com.marcosmoreiradev.docupodcaststudio.domain.reading
                        .SecondarySemanticReadingPolicy.IMAGES_AND_EXTRAS);
        var filtered = useCase.build(workspace, "Q8", "es", false, null,
                com.marcosmoreiradev.docupodcaststudio.domain.reading
                        .SecondarySemanticReadingPolicy.TABLES_AND_EQUATIONS);

        assertEquals(List.of("La figura compara dos curvas."),
                admitted.segments().stream()
                        .map(segment -> segment.narrationText()).toList());
        assertEquals("USER_POLICY_AUTOMATIC", admitted.segments().getFirst()
                .metadata().get("pdfTreatmentAdmission"));
        assertTrue(filtered.segments().isEmpty());
    }

    private static PdfDerivedTreatment treatment(
            String id, PdfRegion source, PdfDerivedTreatmentState state,
            String text, String model, Instant createdAt) {
        return new PdfDerivedTreatment(
                "DER-" + id, PdfDerivedTreatmentKind.IMAGE_DESCRIPTION,
                List.of(source.id()), text, model, "1", 0.9, createdAt,
                state, source.revision(), "fingerprint-" + id,
                "prompt", Map.of());
    }

    private static PdfRegion typedRegion(
            String id, int order, String text, PdfRegionType type,
            PdfNarratability narratability) {
        double y = 40 + order * 80;
        return new PdfRegion(
                id, 1, 40, y, 560, y + 50, 0, order, text,
                type, narratability, List.of(), evidence(),
                PdfRegionOverride.empty(), Map.of(), 1);
    }

    private static PdfRegion region(
            String id, double xMin, double yMin, double xMax, double yMax,
            int order, String text) {
        return new PdfRegion(id, 1, xMin, yMin, xMax, yMax, 0, order,
                text, PdfRegionType.UNKNOWN, PdfNarratability.UNCERTAIN,
                List.of(), evidence(), PdfRegionOverride.empty(), Map.of(), 2);
    }

    private static PdfRegionEvidence evidence() {
        return new PdfRegionEvidence(PdfRegionOrigin.OCR_LOCAL, 0.8,
                "test", "parser", "group", "classifier");
    }
}
