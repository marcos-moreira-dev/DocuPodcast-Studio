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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuildDocumentOutlineUseCaseTest {
    private final BuildDocumentOutlineUseCase useCase = new BuildDocumentOutlineUseCase();

    @Test
    void keepsExistingHeadingStructureWhenPresent() {
        ReadableDocument document = document(SourceDocumentFormat.DOCX,
                block("b1", DocumentBlockType.TITLE, "Numerical Analysis", 1),
                block("b2", DocumentBlockType.HEADING, "Chapter 1 Mathematical Preliminaries", 2),
                block("b3", DocumentBlockType.SUBHEADING, "1.1 Review of Calculus", 3),
                block("b4", DocumentBlockType.PARAGRAPH, "Body text", 3));

        DocumentOutlineProjection projection = useCase.build(document);

        assertEquals(DocumentOutlineOrigin.HEADINGS, projection.origin());
        assertEquals(3, projection.indexedEntryCount());
        assertEquals("b1", projection.entries().get(0).blockId());
        assertEquals("b2", projection.entries().get(0).children().get(0).blockId());
        assertEquals("b3", projection.entries().get(0).children().get(0).children().get(0).blockId());
    }

    @Test
    void buildsPdfOutlineFromContentsEntriesAndMapsToMatchingBlocks() {
        ReadableDocument document = document(SourceDocumentFormat.PDF,
                block("c0", DocumentBlockType.PARAGRAPH, "Contents", 2),
                block("c1", DocumentBlockType.PARAGRAPH, "1.1 Review of Calculus 2", 2),
                block("c2", DocumentBlockType.PARAGRAPH, "1.2 Round-off Errors and Computer Arithmetic 17", 2),
                block("c3", DocumentBlockType.PARAGRAPH, "1.3 Algorithms and Convergence 25", 2),
                block("c4", DocumentBlockType.PARAGRAPH, "2.1 Solutions of Equations in One Variable 48", 2),
                block("b1", DocumentBlockType.PARAGRAPH, "1.1 Review of Calculus", 4),
                block("b2", DocumentBlockType.PARAGRAPH, "1.2 Round-off Errors and Computer Arithmetic", 19),
                block("b3", DocumentBlockType.PARAGRAPH, "1.3 Algorithms and Convergence", 27),
                block("b4", DocumentBlockType.PARAGRAPH, "2.1 Solutions of Equations in One Variable", 50));

        DocumentOutlineProjection projection = useCase.build(document);

        assertEquals(DocumentOutlineOrigin.CONTENTS, projection.origin());
        assertEquals(4, projection.indexedEntryCount());
        assertEquals("b1", projection.entries().get(0).blockId());
        assertEquals("b2", projection.entries().get(1).blockId());
        assertTrue(projection.entries().get(0).label().contains("p. 4"));
    }

    @Test
    void parsesContentsEntriesSplitAcrossLines() {
        ReadableDocument document = document(SourceDocumentFormat.PDF,
                block("c0", DocumentBlockType.PARAGRAPH, "Table of Contents", 1),
                block("c1", DocumentBlockType.PARAGRAPH, "1.1 Review of Calculus\n2", 1),
                block("c2", DocumentBlockType.PARAGRAPH, "1.2 Round-off Errors and Computer Arithmetic\n17", 1),
                block("c3", DocumentBlockType.PARAGRAPH, "1.3 Algorithms and Convergence\n25", 1),
                block("c4", DocumentBlockType.PARAGRAPH, "2.1 Solutions of Equations in One Variable\n48", 1),
                block("b1", DocumentBlockType.PARAGRAPH, "1.1 Review of Calculus", 3),
                block("b2", DocumentBlockType.PARAGRAPH, "1.2 Round-off Errors and Computer Arithmetic", 18),
                block("b3", DocumentBlockType.PARAGRAPH, "1.3 Algorithms and Convergence", 26),
                block("b4", DocumentBlockType.PARAGRAPH, "2.1 Solutions of Equations in One Variable", 49));

        DocumentOutlineProjection projection = useCase.build(document);

        assertEquals(DocumentOutlineOrigin.CONTENTS, projection.origin());
        assertEquals(4, projection.indexedEntryCount());
        assertEquals("b4", projection.entries().get(3).blockId());
    }

    @Test
    void infersTechnicalSectionsWhenNoContentsOrHeadingsExist() {
        ReadableDocument document = document(SourceDocumentFormat.PDF,
                block("b1", DocumentBlockType.PARAGRAPH, "Chapter 1 Mathematical Preliminaries", 1),
                block("b2", DocumentBlockType.PARAGRAPH, "1.1 Review of Calculus", 2),
                block("b3", DocumentBlockType.PARAGRAPH, "1.1.1 Limits and Continuity", 3),
                block("b4", DocumentBlockType.PARAGRAPH, "This is a long body paragraph that should not be treated as a heading.", 3));

        DocumentOutlineProjection projection = useCase.build(document);

        assertEquals(DocumentOutlineOrigin.INFERRED_SECTIONS, projection.origin());
        assertEquals(3, projection.indexedEntryCount());
        assertEquals("b2", projection.entries().get(0).children().get(0).blockId());
        assertEquals("b3", projection.entries().get(0).children().get(0).children().get(0).blockId());
    }

    @Test
    void fallsBackToFlatNavigationWhenConfidenceIsLow() {
        ReadableDocument document = document(SourceDocumentFormat.TXT,
                block("b1", DocumentBlockType.PARAGRAPH, "First paragraph", 1),
                block("b2", DocumentBlockType.PARAGRAPH, "Second paragraph", 1));

        DocumentOutlineProjection projection = useCase.build(document);

        assertEquals(DocumentOutlineOrigin.FLAT, projection.origin());
        assertEquals(2, projection.indexedEntryCount());
        assertFalse(projection.entries().isEmpty());
        assertEquals("b1", projection.entries().get(0).blockId());
    }

    @Test
    void pdfWithoutReliableStructureFallsBackToPageAnchorsBeforeFlatBlocks() {
        ReadableDocument document = document(SourceDocumentFormat.PDF,
                block("b1", DocumentBlockType.PARAGRAPH, "y\n3\n2\n2\n3", 2),
                block("b2", DocumentBlockType.PARAGRAPH, "3 x\n>=2 2 x e3 x", 3),
                block("b3", DocumentBlockType.PARAGRAPH, "noisy exercise fragment", 4));

        DocumentOutlineProjection projection = useCase.build(document);

        assertEquals(DocumentOutlineOrigin.PDF_PAGES, projection.origin());
        assertEquals(4, projection.indexedEntryCount());
        assertEquals("b1", projection.entries().get(0).blockId());
        assertEquals("Pagina 1", projection.entries().get(0).label());
    }

    @Test
    void pdfPageFallbackUsesSourcePageCountBeyondExtractedPages() {
        ReadableDocument document = document(SourceDocumentFormat.PDF,
                block("b1", DocumentBlockType.PARAGRAPH, "front matter", 1, 800),
                block("b2", DocumentBlockType.PARAGRAPH, "first extracted exercise", 2, 800));

        DocumentOutlineProjection projection = useCase.build(document);

        assertEquals(DocumentOutlineOrigin.PDF_PAGES, projection.origin());
        assertEquals(800, projection.indexedEntryCount());
        assertEquals("Pagina 800", projection.entries().get(799).label());
        assertEquals("b2", projection.entries().get(799).blockId());
    }

    @Test
    void largePdfRejectsWeakBibliographicInferredSectionsBeforePageFallback() {
        ReadableDocument document = document(SourceDocumentFormat.PDF,
                block("b1", DocumentBlockType.PARAGRAPH, "front matter", 1, 800),
                block("b2", DocumentBlockType.PARAGRAPH, "Bibliography Science No. an Hulzen Springer erlag", 780, 800),
                block("b3", DocumentBlockType.PARAGRAPH, "References on these techniques include Briggs and Lambert", 781, 800),
                block("b4", DocumentBlockType.PARAGRAPH, "Bibliography [Lam] Lambert The initial value problem", 782, 800),
                block("b5", DocumentBlockType.PARAGRAPH, "Bibliography [Ste] A note on numerical analysis", 783, 800));

        DocumentOutlineProjection projection = useCase.build(document);

        assertEquals(DocumentOutlineOrigin.PDF_PAGES, projection.origin());
        assertEquals(800, projection.indexedEntryCount());
        assertEquals("Pagina 1", projection.entries().getFirst().label());
    }

    @Test
    void usesPdfBookmarkHintsWhenTheyMapToPages() {
        BuildDocumentOutlineUseCase withBookmarks = new BuildDocumentOutlineUseCase(document -> List.of(
                new DocumentOutlineHint("Chapter 1", 1, "2", DocumentOutlineOrigin.PDF_BOOKMARKS),
                new DocumentOutlineHint("1.1 Review of Calculus", 2, "3", DocumentOutlineOrigin.PDF_BOOKMARKS)
        ));
        ReadableDocument document = document(SourceDocumentFormat.PDF,
                block("b1", DocumentBlockType.PARAGRAPH, "Title page", 1),
                block("b2", DocumentBlockType.PARAGRAPH, "Chapter 1 Mathematical Preliminaries", 2),
                block("b3", DocumentBlockType.PARAGRAPH, "1.1 Review of Calculus", 3));

        DocumentOutlineProjection projection = withBookmarks.build(document);

        assertEquals(DocumentOutlineOrigin.PDF_BOOKMARKS, projection.origin());
        assertEquals("b2", projection.entries().get(0).blockId());
        assertEquals("b3", projection.entries().get(0).children().get(0).blockId());
    }

    private static ReadableDocument document(SourceDocumentFormat format, DocumentBlock... blocks) {
        return new ReadableDocument("sample", format, Path.of("sample." + format.name().toLowerCase()), List.of(blocks));
    }

    private static DocumentBlock block(String id, DocumentBlockType type, String text, int sourcePage) {
        return DocumentBlock.of(id, type, text, "", Map.of("sourcePage", Integer.toString(sourcePage)));
    }

    private static DocumentBlock block(String id, DocumentBlockType type, String text, int sourcePage, int sourcePageCount) {
        return DocumentBlock.of(id, type, text, "", Map.of(
                "sourcePage", Integer.toString(sourcePage),
                "sourcePageCount", Integer.toString(sourcePageCount)));
    }
}
