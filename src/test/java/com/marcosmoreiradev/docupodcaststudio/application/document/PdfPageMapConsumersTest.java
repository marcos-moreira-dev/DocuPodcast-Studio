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
import com.marcosmoreiradev.docupodcaststudio.infrastructure.document.JsonPdfPageMapRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PdfPageMapConsumersTest {
    @TempDir Path temp;

    @Test
    void narrationAndHighlightConsumeTheSamePageMapBinding() throws Exception {
        InMemoryPreparedPdfDocumentRepository v3 = new InMemoryPreparedPdfDocumentRepository();
        PreparedPdfWorkspaceRef workspace = PreparedPdfTestFixtures.workspace(v3, temp, 1);
        PdfRegion region = new PdfRegion("R1", 1, 72, 100, 420, 142, 0, 0,
                "Primera oración. Segunda oración.", PdfRegionType.PARAGRAPH,
                PdfNarratability.NARRATABLE, List.of(),
                new PdfRegionEvidence(PdfRegionOrigin.NATIVE_TEXT, 0.99,
                        "e", "p", "g", "c"), PdfRegionOverride.empty(),
                Map.of("lineBboxes", "72,100,420,112;72,130,420,142",
                        "lineCharRanges", "0-18;18-36",
                        PdfTextVisualBoundsResolver.OCR_WORD_BBOXES_ATTRIBUTE,
                        "80,103,150,112;154,103,225,112;80,132,158,141"), 1);
        v3.savePage(temp, new PreparedPdfPage(PreparedPdfPage.CURRENT_SCHEMA_VERSION,
                1, 612, 792, PdfPagePreparationStatus.READY, 1,
                List.of(region), List.of(), ""));
        ResolvePdfPageMapUseCase resolver = new ResolvePdfPageMapUseCase(
                v3, new JsonPdfPageMapRepository(), new BuildPdfPageMapUseCase(), true);

        PdfVisualReadingProjection projection = new BuildPdfVisualReadingProjectionUseCase(v3, resolver)
                .build(workspace);
        var script = new BuildPreparedPdfNarrationUseCase(v3, resolver).build(
                workspace, "PDF", "es", false, ReadingProfile.academicDefaults());

        assertEquals(2, projection.targets().stream()
                .filter(target -> target.kind() == PdfVisualTextTargetKind.SENTENCE).count());
        assertEquals(1, script.segmentCount());
        assertEquals("pdf-page-map-v1", script.segments().getFirst().metadata().get("generationSource"));
        assertTrue(script.segments().getFirst().metadata().containsKey(PdfNarrationBindingMetadata.KEY));
        assertTrue(projection.sentenceTargetForRegion("R1", 1).isPresent());
        PdfVisualTextTarget block = projection.targets().stream()
                .filter(target -> target.kind() == PdfVisualTextTargetKind.BLOCK)
                .findFirst().orElseThrow();
        assertEquals(420.0, block.region().xMaxPoints(), 0.001,
                "La geometria semantica canonica sigue siendo la autoridad de hit testing");
        assertEquals(225.0, block.highlight().visibleRegion().xMaxPoints(), 0.001,
                "PageMap debe propagar la union tight de WORD boxes a presentacion");
        assertEquals(PdfTextLayerOrigin.OCR_LOCAL, block.origin());
    }
}
