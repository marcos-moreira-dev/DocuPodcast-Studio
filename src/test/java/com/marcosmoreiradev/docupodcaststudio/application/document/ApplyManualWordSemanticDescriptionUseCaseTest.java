package com.marcosmoreiradev.docupodcaststudio.application.document;

import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlock;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlockType;
import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.document.SourceDocumentFormat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class ApplyManualWordSemanticDescriptionUseCaseTest {
    @TempDir Path temp;

    @Test
    void storesApprovedManualDescriptionWithoutChangingSourceDocument() {
        DocumentBlock image = DocumentBlock.of("IMG-1", DocumentBlockType.IMAGE_NOTICE,
                "Figura original", "Imagen",
                Map.of("embeddedImageBase64", "AQID"));
        ReadableDocument source = new ReadableDocument("Tema", SourceDocumentFormat.DOCX,
                temp.resolve("tema.docx"), List.of(image));

        ReadableDocument updated = new ApplyManualWordSemanticDescriptionUseCase().apply(
                source, "IMG-1", "Un diagrama compara tres etapas.",
                Instant.parse("2026-08-02T12:00:00Z"));

        DocumentBlock described = updated.blockById("IMG-1").orElseThrow();
        assertEquals("Un diagrama compara tres etapas.",
                described.metadata().get("description"));
        assertEquals("APPROVED", described.metadata().get("descriptionState"));
        assertEquals("manual-user", described.metadata().get("descriptionSource"));
        assertEquals("true", described.metadata().get("manual"));
        assertNotEquals("", described.metadata().get("descriptionSourceFingerprint"));
        assertEquals(Map.of("embeddedImageBase64", "AQID"), image.metadata());
    }

    @Test
    void rejectsPlainProseAndEmptyDescriptions() {
        ReadableDocument source = new ReadableDocument("Tema", SourceDocumentFormat.DOCX,
                temp.resolve("tema.docx"), List.of(DocumentBlock.of(
                "P-1", DocumentBlockType.PARAGRAPH, "Texto", "Normal")));
        ApplyManualWordSemanticDescriptionUseCase useCase =
                new ApplyManualWordSemanticDescriptionUseCase();

        assertThrows(IllegalArgumentException.class,
                () -> useCase.apply(source, "P-1", "Descripción", Instant.EPOCH));
        assertThrows(IllegalArgumentException.class,
                () -> useCase.apply(source, "P-1", " ", Instant.EPOCH));
    }
}
