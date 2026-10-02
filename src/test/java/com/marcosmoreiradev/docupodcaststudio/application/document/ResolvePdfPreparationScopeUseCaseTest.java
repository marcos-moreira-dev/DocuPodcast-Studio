package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlockType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ResolvePdfPreparationScopeUseCaseTest {
    @TempDir Path temp;
    private final InMemoryPreparedPdfDocumentRepository repository =
            new InMemoryPreparedPdfDocumentRepository();
    private final ResolvePdfPreparationScopeUseCase useCase =
            new ResolvePdfPreparationScopeUseCase(repository);

    @Test
    void resolvesCurrentSectionFromBookmarkPages() {
        DocumentOutlineProjection outline = new DocumentOutlineProjection(
                DocumentOutlineOrigin.PDF_BOOKMARKS, "Índice", "", List.of(
                entry("a", 1), entry("b", 4), entry("c", 8)), 3, 0);
        var result = useCase.resolve(new PdfPreparationScopeRequest(
                workspace(10), PdfPreparationScope.CURRENT_SECTION, 5, 0, 0, outline));

        assertEquals(List.of(4, 5, 6, 7), result.pages());
        assertTrue(result.explanation().contains("bookmarks"));
    }

    @Test
    void requestsManualRangeWhenSectionHasNoStructuralBoundary() {
        var result = useCase.resolve(new PdfPreparationScopeRequest(
                workspace(10), PdfPreparationScope.CURRENT_SECTION, 5, 0, 0, null));

        assertTrue(result.requiresManualRange());
        assertTrue(result.pages().isEmpty());
    }

    @Test
    void normalizesExplicitRangeAndWholeDocument() {
        var range = useCase.resolve(new PdfPreparationScopeRequest(
                workspace(5), PdfPreparationScope.PAGE_RANGE, 1, 4, 2, null));
        var whole = useCase.resolve(new PdfPreparationScopeRequest(
                workspace(5), PdfPreparationScope.WHOLE_DOCUMENT, 1, 0, 0, null));

        assertEquals(List.of(2, 3, 4), range.pages());
        assertEquals(List.of(1, 2, 3, 4, 5), whole.pages());
    }

    @Test
    void processIntervalResolvesExactlyTheRequestedPages() {
        var result = useCase.resolve(new PdfPreparationScopeRequest(
                workspace(10), PdfPreparationScope.PAGE_RANGE, 1, 3, 8, null,
                PdfPreparationOrigin.PROCESS_INTERVAL, true, "interval-3-8"));

        assertEquals(List.of(3, 4, 5, 6, 7, 8), result.pages());
        assertTrue(!result.requiresManualRange());
    }

    @Test
    void processIntervalRejectsReverseAndOutOfBoundsRanges() {
        var reverse = useCase.resolve(new PdfPreparationScopeRequest(
                workspace(10), PdfPreparationScope.PAGE_RANGE, 1, 8, 3, null,
                PdfPreparationOrigin.PROCESS_INTERVAL, true, "reverse"));
        var beyond = useCase.resolve(new PdfPreparationScopeRequest(
                workspace(10), PdfPreparationScope.PAGE_RANGE, 1, 3, 999, null,
                PdfPreparationOrigin.PROCESS_INTERVAL, true, "beyond"));

        assertTrue(reverse.requiresManualRange());
        assertTrue(reverse.pages().isEmpty());
        assertTrue(beyond.requiresManualRange());
        assertTrue(beyond.pages().isEmpty());
    }

    private static DocumentOutlineEntry entry(String id, int page) {
        return new DocumentOutlineEntry(id, "block-" + id, DocumentBlockType.HEADING,
                "Sección " + id, Integer.toString(page), page - 1, List.of());
    }

    private PreparedPdfWorkspaceRef workspace(int pages) {
        return PreparedPdfTestFixtures.workspace(repository, temp.resolve("p" + pages), pages);
    }
}
