package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlock;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlockType;
import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.document.SourceDocumentFormat;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BuildDocumentOutlineUseCaseTest {
    private final BuildDocumentOutlineUseCase useCase = new BuildDocumentOutlineUseCase();

    @Test
    void keepsExistingHeadingStructureForBlockDocuments() {
        ReadableDocument document = document(SourceDocumentFormat.DOCX,
                block("b1", DocumentBlockType.TITLE, "Numerical Analysis"),
                block("b2", DocumentBlockType.HEADING, "Chapter 1"),
                block("b3", DocumentBlockType.SUBHEADING, "1.1 Review"),
                block("b4", DocumentBlockType.PARAGRAPH, "Body text"));

        DocumentOutlineProjection projection = useCase.build(document);

        assertEquals(DocumentOutlineOrigin.HEADINGS, projection.origin());
        assertEquals(3, projection.indexedEntryCount());
        assertEquals("b3", projection.entries().getFirst().children().getFirst()
                .children().getFirst().blockId());
    }

    @Test
    void fallsBackToFlatNavigationForPlainText() {
        ReadableDocument document = document(SourceDocumentFormat.TXT,
                block("b1", DocumentBlockType.PARAGRAPH, "First paragraph"),
                block("b2", DocumentBlockType.PARAGRAPH, "Second paragraph"));

        DocumentOutlineProjection projection = useCase.build(document);

        assertEquals(DocumentOutlineOrigin.FLAT, projection.origin());
        assertEquals(List.of("b1", "b2"),
                projection.entries().stream().map(DocumentOutlineEntry::blockId).toList());
    }

    @Test
    void rejectsPdfBlockProjection() {
        ReadableDocument legacyPdf = document(SourceDocumentFormat.PDF,
                block("b1", DocumentBlockType.PARAGRAPH, "Legacy"));

        assertThrows(IllegalArgumentException.class, () -> useCase.build(legacyPdf));
    }

    private static ReadableDocument document(SourceDocumentFormat format, DocumentBlock... blocks) {
        return new ReadableDocument("sample", format,
                Path.of("sample." + format.name().toLowerCase()), List.of(blocks));
    }

    private static DocumentBlock block(String id, DocumentBlockType type, String text) {
        return DocumentBlock.of(id, type, text, "", Map.of());
    }
}
