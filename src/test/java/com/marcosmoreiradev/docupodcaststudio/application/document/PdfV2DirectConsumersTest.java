package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.application.script.BuildPreparedPdfNarrationUseCase;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfNarratability;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfPagePreparationStatus;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegion;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionEvidence;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionOrigin;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionOverride;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionType;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PreparedPdfPage;
import com.marcosmoreiradev.docupodcaststudio.domain.reading.ReadingProfile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

final class PdfV2DirectConsumersTest {
    @TempDir Path temp;

    @Test
    void selectionSearchContextNarrationAndOverrideUseOnlyCanonicalRegions() throws Exception {
        InMemoryPreparedPdfDocumentRepository repository = new InMemoryPreparedPdfDocumentRepository();
        PreparedPdfWorkspaceRef workspace =
                PreparedPdfTestFixtures.workspace(repository, temp.resolve("workspace"), 1);
        repository.savePage(workspace.projectRoot(), new PreparedPdfPage(
                PreparedPdfPage.CURRENT_SCHEMA_VERSION,
                1, 612, 792, PdfPagePreparationStatus.READY, 1,
                List.of(
                        region("TITLE", 0, "Capítulo uno", PdfRegionType.HEADING,
                                PdfNarratability.NARRATABLE),
                        region("BODY", 1, "Texto narrable principal.", PdfRegionType.PARAGRAPH,
                                PdfNarratability.NARRATABLE),
                        region("MATH", 2, "x = a + b", PdfRegionType.MATH,
                                PdfNarratability.NON_NARRATABLE),
                        region("DOUBT", 3, "Ga1imatía dudosa", PdfRegionType.UNKNOWN,
                                PdfNarratability.UNCERTAIN)),
                List.of(), ""));
        PreparedPdfSource source = new PreparedPdfSource(workspace, "Prueba");

        PdfRegionSelectionRef selection = new PdfRegionSelectionRef(1, "BODY", 0, 5);
        DocumentSelectionSnapshot snapshot =
                new ResolveDocumentSelectionUseCase(repository).resolve(source, selection).orElseThrow();
        assertEquals("Texto", snapshot.text());
        assertEquals(1, snapshot.pageNumber());

        PreparedPdfRegionContext context =
                new BuildPreparedPdfRegionContextUseCase(repository)
                        .build(workspace, selection, 1).orElseThrow();
        assertEquals("TITLE", ((PdfRegionSelectionRef) context.before().getFirst().reference()).regionId());
        assertEquals("MATH", ((PdfRegionSelectionRef) context.after().getFirst().reference()).regionId());

        PdfTextSearchProjection search = new SearchPdfTextUseCase(repository).search(
                new PdfTextSearchRequest(workspace, "a + b", false, 10, 0));
        assertEquals("MATH", search.results().getFirst().regionId());

        var script = new BuildPreparedPdfNarrationUseCase(repository).build(
                workspace, "Prueba", "es", false, ReadingProfile.academicDefaults());
        assertEquals(3, script.segmentCount());
        assertTrue(script.segments().stream().anyMatch(
                segment -> segment.sourceBlockIds().contains("MATH")));
        assertTrue(script.segments().stream().noneMatch(
                segment -> segment.sourceBlockIds().contains("DOUBT")));

        new UpdatePreparedPdfRegionOverrideUseCase(repository).execute(
                workspace, 1, "BODY",
                new UpdatePreparedPdfRegionOverrideUseCase.OverridePatch(
                        UpdatePreparedPdfRegionOverrideUseCase.OverrideValue.keep(),
                        UpdatePreparedPdfRegionOverrideUseCase.OverrideValue.keep(),
                        UpdatePreparedPdfRegionOverrideUseCase.OverrideValue.set(
                                PdfNarratability.NON_NARRATABLE),
                        UpdatePreparedPdfRegionOverrideUseCase.OverrideValue.keep()));
        assertEquals(2, new BuildPreparedPdfNarrationUseCase(repository).build(
                workspace, "Prueba", "es", false,
                ReadingProfile.academicDefaults()).segmentCount());
    }

    @Test
    void narrationRepairsPersistedGeometricOrderAndKeepsCalloutAsAUnit()
            throws Exception {
        InMemoryPreparedPdfDocumentRepository repository =
                new InMemoryPreparedPdfDocumentRepository();
        PreparedPdfWorkspaceRef workspace =
                PreparedPdfTestFixtures.workspace(
                        repository, temp.resolve("ordered-workspace"), 1);
        repository.savePage(workspace.projectRoot(), new PreparedPdfPage(
                PreparedPdfPage.CURRENT_SCHEMA_VERSION,
                1, 600, 800, PdfPagePreparationStatus.READY, 1,
                List.of(
                        geometricRegion("BODY", 0, 40, 90, 560, 220,
                                "Explicación principal.",
                                PdfRegionType.PARAGRAPH),
                        geometricRegion("HEADER", 1, 40, 5, 250, 20,
                                "Cabecera", PdfRegionType.HEADER),
                        geometricRegion("TITLE", 2, 40, 40, 500, 65,
                                "Tema de la página", PdfRegionType.HEADING),
                        geometricRegion("TECHNICAL-LABEL", 3,
                                430, 180, 500, 195,
                                "tangente en A", PdfRegionType.PARAGRAPH),
                        geometricRegion("CALLOUT", 4, 40, 240, 560, 300,
                                "Idea clave para recordar.",
                                PdfRegionType.SIDEBAR),
                        geometricRegion("AFTER", 5, 40, 340, 560, 400,
                                "Continuación de la explicación.",
                                PdfRegionType.PARAGRAPH)),
                List.of(), ""));

        var script = new BuildPreparedPdfNarrationUseCase(repository).build(
                workspace, "Prueba", "es", false,
                ReadingProfile.academicDefaults());

        assertEquals(List.of("TITLE", "BODY", "CALLOUT", "AFTER"),
                script.segments().stream()
                        .map(segment -> segment.sourceBlockIds().getFirst())
                        .toList());
        assertTrue(script.segments().get(2).narrationText()
                .startsWith("Recuadro:"));
    }

    private static PdfRegion region(String id, int order, String text,
                                    PdfRegionType type, PdfNarratability narratability) {
        return new PdfRegion(id, 1, 40, 40 + order * 60, 520, 80 + order * 60,
                0, order, text, type, narratability, List.of("fixture"),
                new PdfRegionEvidence(PdfRegionOrigin.NATIVE_TEXT, 0.95,
                        "fixture", "fixture", "fixture", "fixture"),
                PdfRegionOverride.empty(), Map.of(), 1);
    }

    private static PdfRegion geometricRegion(
            String id, int persistedOrder, double x1, double y1,
            double x2, double y2, String text, PdfRegionType type) {
        PdfNarratability narratability = type == PdfRegionType.HEADER
                ? PdfNarratability.NON_NARRATABLE
                : PdfNarratability.NARRATABLE;
        return new PdfRegion(
                id, 1, x1, y1, x2, y2, 0, persistedOrder,
                text, type, narratability, List.of("fixture"),
                new PdfRegionEvidence(
                        PdfRegionOrigin.NATIVE_TEXT, 0.99,
                        "fixture", "fixture", "fixture", "fixture"),
                PdfRegionOverride.empty(), Map.of(), 1);
    }
}
