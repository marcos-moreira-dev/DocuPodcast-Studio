package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfNarratability;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfPageAnalysisProfile;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfPagePreparationStatus;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfPagePreparationMetrics;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegion;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionEvidence;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionOrigin;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionOverride;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionType;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PreparedPdfPage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResolveDocumentTechnicalElementsUseCaseTest {
    @TempDir Path temp;

    @Test
    void groupsGraphLabelsAndCaptionIntoOneStableTechnicalElement()
            throws Exception {
        InMemoryPreparedPdfDocumentRepository repository =
                new InMemoryPreparedPdfDocumentRepository();
        PdfRegion prose = region("P000001-R-PROSE", 40, 80, 560, 130,
                1, "La curva se aproxima a uno.", PdfRegionType.PARAGRAPH,
                PdfNarratability.NARRATABLE);
        PdfRegion pi = region("P000001-R-PI", 120, 250, 145, 270,
                2, "π", PdfRegionType.UNKNOWN, PdfNarratability.UNCERTAIN);
        PdfRegion hole = region("P000001-R-HOLE", 300, 310, 350, 335,
                3, "hueco", PdfRegionType.UNKNOWN,
                PdfNarratability.UNCERTAIN);
        PdfRegion caption = region("P000001-R-CAPTION", 80, 500, 540, 530,
                4, "Gráfica 2. La función se aproxima a uno.",
                PdfRegionType.CAPTION, PdfNarratability.NON_NARRATABLE);
        repository.savePage(temp, new PreparedPdfPage(
                PreparedPdfPage.CURRENT_SCHEMA_VERSION, 1, 612, 792,
                PdfPagePreparationStatus.READY, 1,
                List.of(prose, pi, hole, caption), List.of(),
                PdfPagePreparationMetrics.empty(), PdfPageAnalysisProfile.defaults(),
                List.of(), ""));

        ResolveDocumentTechnicalElementsUseCase useCase =
                new ResolveDocumentTechnicalElementsUseCase(repository);
        List<DocumentTechnicalElement> first = useCase.resolve(
                temp, 1, List.of(pi.id(), hole.id()));
        List<DocumentTechnicalElement> second = useCase.resolve(
                temp, 1, List.of(pi.id(), hole.id()));

        assertEquals(1, first.size());
        DocumentTechnicalElement element = first.getFirst();
        assertEquals(DocumentTechnicalElement.Type.GRAPH, element.type());
        assertEquals(List.of(pi.id(), hole.id(), caption.id()),
                element.sourceRegionIds());
        assertEquals(caption.id(), element.anchorRegionId());
        assertEquals(first.getFirst().id(), second.getFirst().id());
        assertTrue(element.roi().xMinPoints() < pi.xMin());
        assertTrue(element.roi().yMinPoints() < pi.yMin());
        assertTrue(element.roi().yMaxPoints() >= caption.yMax());
        assertTrue(element.nearbyContext().contains("aproxima"));
    }

    private static PdfRegion region(
            String id, double xMin, double yMin, double xMax, double yMax,
            int order, String text, PdfRegionType type,
            PdfNarratability narratability) {
        return new PdfRegion(id, 1, xMin, yMin, xMax, yMax, 0, order,
                text, type, narratability, List.of(),
                new PdfRegionEvidence(PdfRegionOrigin.OCR_LOCAL, 0.8,
                        "test", "parser", "group", "classifier"),
                PdfRegionOverride.empty(), Map.of(), 1);
    }
}
