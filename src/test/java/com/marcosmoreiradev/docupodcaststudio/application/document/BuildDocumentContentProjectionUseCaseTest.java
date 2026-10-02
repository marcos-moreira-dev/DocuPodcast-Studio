package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlock;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlockType;
import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.document.SourceDocumentFormat;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfNarratability;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfNarrationBinding;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfObjectNarrationPolicy;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegion;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionEvidence;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionOrigin;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionOverride;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfRegionType;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfSemanticTextLayer;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PreparedPdfPage;
import com.marcosmoreiradev.docupodcaststudio.domain.document.pdf.PdfPagePreparationStatus;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationScriptDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegment;
import com.marcosmoreiradev.docupodcaststudio.domain.script.NarrationSegmentType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BuildDocumentContentProjectionUseCaseTest {
    @TempDir Path temp;

    @Test
    void wordPreservesBlockOrderAndStableIds() {
        ReadableDocument document = new ReadableDocument("Tema", SourceDocumentFormat.DOCX,
                temp.resolve("tema.docx"), List.of(
                DocumentBlock.of("B-1", DocumentBlockType.TITLE, "Tema", "Title"),
                DocumentBlock.of("B-2", DocumentBlockType.PARAGRAPH, "Texto", "Normal")));
        NarrationScriptDocument script = NarrationScriptDocument.create("Tema", "es", "Tema", List.of(
                NarrationSegment.of("SEG-1", NarrationSegmentType.TITLE, "Tema", "Tema", List.of("B-1")),
                NarrationSegment.of("SEG-2", NarrationSegmentType.PARAGRAPH, "Texto", "Texto", List.of("B-2"))));
        BuildDocumentContentProjectionUseCase useCase = new BuildDocumentContentProjectionUseCase(
                new OpenPreparedPdfWorkspaceUseCase(new InMemoryPreparedPdfDocumentRepository()));

        DocumentContentProjection result = useCase.build(new BlockDocumentSource(document), script);

        assertEquals(List.of("B-1", "B-2"), result.items().stream()
                .map(DocumentContentItem::contentId).toList());
        assertEquals(DocumentContentKind.COVER, result.items().getFirst().kind());
        assertEquals(DocumentPresentationMode.TEXT_RENDER,
                result.items().getFirst().presentationMode());
        assertInstanceOf(WordContentAnchor.class, result.items().getFirst().anchor());
    }

    @Test
    void wordProjectionKeepsVisualMetadataAndTreatsEquationsAsProse() {
        ReadableDocument document = new ReadableDocument("Tema", SourceDocumentFormat.DOCX,
                temp.resolve("tema.docx"), List.of(
                DocumentBlock.of("IMG-1", DocumentBlockType.IMAGE_NOTICE, "Imagen", "",
                        Map.of("embeddedImageBase64", "AQID", "embeddedImageMimeType", "image/png")),
                DocumentBlock.of("MATH-1", DocumentBlockType.MATH_NOTICE, "x mas uno", "",
                        Map.of("sourceMathText", "x mas uno"))));
        NarrationScriptDocument script = NarrationScriptDocument.create("Tema", "es", "Tema", List.of(
                NarrationSegment.of("SEG-MATH", NarrationSegmentType.PARAGRAPH,
                        "Formula", "x mas uno", List.of("MATH-1"))));

        DocumentContentProjection result = new BuildDocumentContentProjectionUseCase(
                new OpenPreparedPdfWorkspaceUseCase(new InMemoryPreparedPdfDocumentRepository()))
                .build(new BlockDocumentSource(document), script);

        assertEquals("AQID", result.items().getFirst().wordAnchor().orElseThrow()
                .metadata().get("embeddedImageBase64"));
        assertTrue(result.items().getFirst().narrationText().isBlank());
        assertTrue(!result.items().getFirst().narratable());
        assertEquals(DocumentContentKind.PROSE, result.items().getLast().kind());
    }

    @Test
    void pdfGroupsInternalRegionsUnderOneAdmittedOwnerAndUnionsItsRoi() throws Exception {
        InMemoryPreparedPdfDocumentRepository repository = new InMemoryPreparedPdfDocumentRepository();
        PreparedPdfWorkspaceRef workspace = new PreparedPdfWorkspaceRef(
                temp, temp.resolve("source.pdf"), "a".repeat(64));
        repository.savePage(temp, new PreparedPdfPage(PreparedPdfPage.CURRENT_SCHEMA_VERSION,
                2, 612, 792, PdfPagePreparationStatus.READY, 7,
                List.of(region("CELL-1", 72, 120, 260, 170),
                        region("CELL-2", 260, 120, 520, 210)), List.of(), List.of(), ""));
        PdfNarrationBinding binding = new PdfNarrationBinding(2,
                List.of("CELL-1", "CELL-2"), PdfSemanticTextLayer.INTERPRETATION,
                PdfObjectNarrationPolicy.SUMMARIZE, "TABLE-QWEN-1", 7, "source-fp");
        Map<String, String> metadata = Map.of(
                "sourceBlockType", "TABLE",
                PdfNarrationBindingMetadata.KEY, PdfNarrationBindingMetadata.encode(binding),
                "pdfDerivedTreatmentId", "TABLE-QWEN-1");
        NarrationScriptDocument script = NarrationScriptDocument.create("PDF", "es", "PDF", List.of(
                new NarrationSegment("PDFSEG-1", NarrationSegmentType.TABLE_NOTICE,
                        "Tabla", "La tabla compara dos valores.", List.of("CELL-1"),
                        "CHR-NARRATOR", "VOC-NARRATOR", "STY-NEUTRAL", metadata)));
        BuildDocumentContentProjectionUseCase useCase = new BuildDocumentContentProjectionUseCase(
                new OpenPreparedPdfWorkspaceUseCase(repository));

        DocumentContentProjection result = useCase.build(
                new PreparedPdfSource(workspace, "PDF"), script);

        assertEquals(1, result.items().size());
        DocumentContentItem table = result.items().getFirst();
        assertEquals(DocumentContentKind.TABLE, table.kind());
        assertEquals(DocumentPresentationMode.SOURCE_CAPTURE, table.presentationMode());
        assertTrue(table.contentId().startsWith("PDF-CONTENT-"));
        assertTrue(table.legacyContentIds().contains("PDF-COMP-TABLE-QWEN-1"));
        assertEquals(table, result.itemById("PDF-COMP-TABLE-QWEN-1").orElseThrow());
        assertEquals(List.of("CELL-1", "CELL-2"), table.sourceIds());
        PdfContentAnchor anchor = table.pdfAnchor().orElseThrow();
        assertEquals(72.0, anchor.roi().xMin());
        assertEquals(520.0, anchor.roi().xMax());
        assertEquals(210.0, anchor.roi().yMax());
        assertTrue(table.narratable());
    }

    @Test
    void pdfEnumeratesAVisualOwnerWithoutInventingNarration() throws Exception {
        InMemoryPreparedPdfDocumentRepository repository =
                new InMemoryPreparedPdfDocumentRepository();
        PreparedPdfWorkspaceRef workspace = new PreparedPdfWorkspaceRef(
                temp, temp.resolve("source.pdf"), "b".repeat(64));
        repository.savePage(temp, new PreparedPdfPage(PreparedPdfPage.CURRENT_SCHEMA_VERSION,
                3, 612, 792, PdfPagePreparationStatus.READY, 4,
                List.of(region(3, "IMAGE-1", 90, 180, 500, 520)),
                List.of(), List.of(), ""));

        DocumentContentProjection result = new BuildDocumentContentProjectionUseCase(
                new OpenPreparedPdfWorkspaceUseCase(repository)).build(
                new PreparedPdfSource(workspace, "PDF"), null);

        assertEquals(1, result.items().size());
        DocumentContentItem visual = result.items().getFirst();
        assertEquals(DocumentContentKind.TABLE, visual.kind());
        assertTrue(visual.narrationText().isBlank());
        assertTrue(!visual.narratable());
        assertEquals(List.of("IMAGE-1"), visual.sourceIds());
        assertEquals(3, visual.pdfAnchor().orElseThrow().pageNumber());
    }

    @Test
    void pdfContentIdentityDoesNotChangeWhenItsInterpretationIsRegenerated()
            throws Exception {
        InMemoryPreparedPdfDocumentRepository repository =
                new InMemoryPreparedPdfDocumentRepository();
        PreparedPdfWorkspaceRef workspace = new PreparedPdfWorkspaceRef(
                temp, temp.resolve("source.pdf"), "c".repeat(64));
        repository.savePage(temp, new PreparedPdfPage(
                PreparedPdfPage.CURRENT_SCHEMA_VERSION, 1, 612, 792,
                PdfPagePreparationStatus.READY, 1,
                List.of(region(1, "TABLE-1", 70, 100, 530, 400)),
                List.of(), List.of(), ""));
        BuildDocumentContentProjectionUseCase useCase =
                new BuildDocumentContentProjectionUseCase(
                        new OpenPreparedPdfWorkspaceUseCase(repository));

        DocumentContentProjection first = useCase.build(
                new PreparedPdfSource(workspace, "PDF"),
                scriptForTreatment("TREATMENT-A", "Primera interpretacion."));
        DocumentContentProjection regenerated = useCase.build(
                new PreparedPdfSource(workspace, "PDF"),
                scriptForTreatment("TREATMENT-B", "Interpretacion regenerada."));

        assertEquals(first.items().getFirst().contentId(),
                regenerated.items().getFirst().contentId());
        assertTrue(first.items().getFirst().legacyContentIds()
                .contains("PDF-COMP-TREATMENT-A"));
        assertTrue(regenerated.items().getFirst().legacyContentIds()
                .contains("PDF-COMP-TREATMENT-B"));
    }

    private static NarrationScriptDocument scriptForTreatment(
            String treatmentId, String text) {
        PdfNarrationBinding binding = new PdfNarrationBinding(1,
                List.of("TABLE-1"), PdfSemanticTextLayer.INTERPRETATION,
                PdfObjectNarrationPolicy.SUMMARIZE, treatmentId, 1, "source-fp");
        return NarrationScriptDocument.create("PDF", "es", "PDF", List.of(
                new NarrationSegment("SEG-" + treatmentId,
                        NarrationSegmentType.TABLE_NOTICE, "Tabla", text,
                        List.of("TABLE-1"), "CHR-NARRATOR", "VOC-NARRATOR",
                        "STY-NEUTRAL", Map.of(
                        "sourceBlockType", "TABLE",
                        PdfNarrationBindingMetadata.KEY,
                        PdfNarrationBindingMetadata.encode(binding),
                        "pdfDerivedTreatmentId", treatmentId))));
    }

    private static PdfRegion region(String id, double x1, double y1, double x2, double y2) {
        return region(2, id, x1, y1, x2, y2);
    }

    private static PdfRegion region(int pageNumber, String id,
                                    double x1, double y1, double x2, double y2) {
        PdfRegionEvidence evidence = new PdfRegionEvidence(PdfRegionOrigin.NATIVE_TEXT,
                0.99, "extractor", "parser", "group", "classifier");
        return new PdfRegion(id, pageNumber, x1, y1, x2, y2, 0, 0, "celda",
                PdfRegionType.TABLE, PdfNarratability.UNCERTAIN, List.of(), evidence,
                PdfRegionOverride.empty(), Map.of(), 7);
    }
}
