package com.marcosmoreiradev.docupodcaststudio.application.documentstudy;

import com.marcosmoreiradev.docupodcaststudio.application.document.DocumentContentItem;
import com.marcosmoreiradev.docupodcaststudio.application.document.DocumentContentKind;
import com.marcosmoreiradev.docupodcaststudio.application.document.DocumentContentProjection;
import com.marcosmoreiradev.docupodcaststudio.application.document.PdfContentAnchor;
import com.marcosmoreiradev.docupodcaststudio.application.document.DocumentContentRectangle;
import com.marcosmoreiradev.docupodcaststudio.domain.document.SourceDocumentFormat;
import com.marcosmoreiradev.docupodcaststudio.domain.study.DocumentParagraphVisualAssignment;
import com.marcosmoreiradev.docupodcaststudio.domain.study.DocumentStudyVideoConfiguration;
import com.marcosmoreiradev.docupodcaststudio.domain.study.DocumentVideoSlideConfiguration;
import com.marcosmoreiradev.docupodcaststudio.domain.study.DocumentVisualSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ReconcileDocumentStudyVideoConfigurationUseCaseTest {
    @TempDir Path temp;

    @Test
    void migratesInferenceEraKeyWithoutLosingAudiovisualEdits() {
        DocumentContentItem content = content();
        DocumentParagraphVisualAssignment visual =
                new DocumentParagraphVisualAssignment(
                        "PDF-COMP-TREATMENT-A", "old-fingerprint", "IMG-USER",
                        "", "", DocumentVisualSource.IMPORTED, "", null);
        DocumentVideoSlideConfiguration oldSlide =
                new DocumentVideoSlideConfiguration(
                        "PDF-COMP-TREATMENT-A", "old-content-fingerprint",
                        visual, 12.0, false, "AST-SOURCE-VISUAL", "visual-fp");
        DocumentStudyVideoConfiguration old = DocumentStudyVideoConfiguration
                .empty().withContent(oldSlide)
                .withBlockEnabled("PDF-COMP-TREATMENT-A", false);

        DocumentStudyVideoConfiguration migrated =
                new ReconcileDocumentStudyVideoConfigurationUseCase().execute(
                        projection(content), old);

        assertTrue(migrated.content("PDF-COMP-TREATMENT-A").isEmpty());
        DocumentVideoSlideConfiguration current = migrated
                .content("PDF-REGION-TABLE-1").orElseThrow();
        assertEquals("IMG-USER", current.visual().importedImageAssetId());
        assertEquals(12.0, current.durationSeconds());
        assertEquals("AST-SOURCE-VISUAL", current.sourceVisualAssetId());
        assertFalse(current.enabled());
        assertTrue(migrated.disabledBlockIds()
                .contains("PDF-REGION-TABLE-1"));
        assertFalse(migrated.disabledBlockIds()
                .contains("PDF-COMP-TREATMENT-A"));
        assertTrue(content.narratable(),
                "La exclusion audiovisual no modifica la narratabilidad/TTS");
    }

    @Test
    void resolverReadsOldAliasBeforeConfigurationIsPersistedAgain() {
        DocumentContentItem content = content();
        DocumentVideoSlideConfiguration oldSlide =
                new DocumentVideoSlideConfiguration(
                        "PDF-COMP-TREATMENT-A", "",
                        DocumentParagraphVisualAssignment.empty(
                                "PDF-COMP-TREATMENT-A"),
                        9.0, false, "AST-OLD", "visual-fp");
        DocumentStudyVideoConfiguration old = DocumentStudyVideoConfiguration
                .empty().withContent(oldSlide);

        List<DocumentStudyVideoContentResolver.Item> items =
                new DocumentStudyVideoContentResolver().resolve(
                        projection(content), old);

        assertEquals(1, items.size());
        assertEquals("PDF-REGION-TABLE-1",
                items.getFirst().content().contentId());
        assertEquals(9.0, items.getFirst().durationSeconds());
        assertFalse(items.getFirst().enabled());
    }

    private DocumentContentProjection projection(DocumentContentItem content) {
        return new DocumentContentProjection("PDF", SourceDocumentFormat.PDF,
                temp.resolve("source.pdf"), List.of(content));
    }

    private static DocumentContentItem content() {
        return new DocumentContentItem(
                "PDF-REGION-TABLE-1", DocumentContentKind.TABLE,
                "Tabla", "La tabla presenta resultados.", List.of("SEG-1"),
                List.of("TABLE-1"), List.of("PDF-COMP-TREATMENT-A"),
                "content-fingerprint", 2,
                new PdfContentAnchor(1, 612, 792,
                        new DocumentContentRectangle(10, 20, 300, 200),
                        List.of("TABLE-1"), 2, "visual-fingerprint"));
    }
}
