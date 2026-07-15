package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.assignment.NarrativeLayerAssignment;
import com.marcosmoreiradev.docupodcaststudio.domain.assignment.NarrativeLayerKind;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlock;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlockType;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentImportReport;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentTextRange;
import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.document.SourceDocumentFormat;
import com.marcosmoreiradev.docupodcaststudio.domain.document.TextAnchor;
import com.marcosmoreiradev.docupodcaststudio.domain.document.TextAnchorStatus;
import com.marcosmoreiradev.docupodcaststudio.domain.script.ScriptTextRange;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class TextAnchorReconciliationUseCaseTest {
    @Test
    void confirmsAnchorWhenSelectedTextStillLivesAtSameRange() {
        ReadableDocument document = document("BLK-001", "El avión despega con una brújula confundida.");
        TextAnchor anchor = TextAnchor.fromDocumentSelection("ANCH-001", document,
                new DocumentTextRange("BLK-001", 3, 8), "snap-old");
        NarrativeLayerAssignment layer = layer(anchor);

        TextAnchorReconciliationReport report = new ReconcileTextAnchorsUseCase()
                .reconcile(List.of(layer), document, "snap-new");

        assertEquals(1, report.currentCount());
        assertEquals(TextAnchorStatus.CURRENT, report.entries().get(0).reconciledAnchor().status());
    }

    @Test
    void relocatesAnchorWhenSelectedTextMovesToAnotherBlock() {
        ReadableDocument original = document("BLK-001", "El tornillo dorado salvó el vuelo.");
        TextAnchor anchor = TextAnchor.fromDocumentSelection("ANCH-002", original,
                new DocumentTextRange("BLK-001", 3, 18), "snap-old");
        NarrativeLayerAssignment layer = layer(anchor);
        ReadableDocument refreshed = new ReadableDocument("Demo", SourceDocumentFormat.DOCX, Path.of("demo.docx"), List.of(
                DocumentBlock.of("BLK-001", DocumentBlockType.PARAGRAPH, "Otra escena aparece primero.", ""),
                DocumentBlock.of("BLK-002", DocumentBlockType.PARAGRAPH, "Ahora el tornillo dorado salvó el vuelo.", "")
        ), DocumentImportReport.empty());

        TextAnchorReconciliationReport report = new ReconcileTextAnchorsUseCase()
                .reconcile(List.of(layer), refreshed, "snap-new");

        assertEquals(1, report.relocatedCount());
        assertEquals("BLK-002", report.entries().get(0).reconciledAnchor().range().blockId());
    }

    @Test
    void orphanedWhenSelectedTextDisappears() {
        ReadableDocument original = document("BLK-001", "Texto que luego desaparece.");
        TextAnchor anchor = TextAnchor.fromDocumentSelection("ANCH-003", original,
                new DocumentTextRange("BLK-001", 0, 5), "snap-old");
        NarrativeLayerAssignment layer = layer(anchor);
        ReadableDocument refreshed = document("BLK-001", "Contenido completamente distinto.");

        TextAnchorReconciliationReport report = new ReconcileTextAnchorsUseCase()
                .reconcile(List.of(layer), refreshed, "snap-new");

        assertEquals(1, report.orphanedCount());
        assertEquals(TextAnchorStatus.ORPHANED, report.entries().get(0).status());
    }

    private static NarrativeLayerAssignment layer(TextAnchor anchor) {
        return new NarrativeLayerAssignment(
                "LYR-001",
                NarrativeLayerKind.IMAGE,
                new ScriptTextRange("SEG-001", 0, 5),
                anchor.range(),
                anchor,
                "IMG-001",
                "Imagen",
                "");
    }

    private static ReadableDocument document(String blockId, String text) {
        return new ReadableDocument("Demo", SourceDocumentFormat.DOCX, Path.of("demo.docx"), List.of(
                DocumentBlock.of(blockId, DocumentBlockType.PARAGRAPH, text, "")
        ), DocumentImportReport.empty());
    }
}
