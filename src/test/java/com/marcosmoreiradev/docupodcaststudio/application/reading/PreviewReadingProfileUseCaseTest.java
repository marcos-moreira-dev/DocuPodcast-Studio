package com.marcosmoreiradev.docupodcaststudio.application.reading;

import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlock;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlockType;
import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.document.SourceDocumentFormat;
import com.marcosmoreiradev.docupodcaststudio.domain.reading.ImageNarrationPolicy;
import com.marcosmoreiradev.docupodcaststudio.domain.reading.ReadingProfile;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PreviewReadingProfileUseCaseTest {
    @Test
    void previewsChangesWithoutMutatingOriginalDocument() {
        ReadableDocument document = new ReadableDocument("Notas", SourceDocumentFormat.DOCX, Path.of("notas.docx"), List.of(
                DocumentBlock.of("B001", DocumentBlockType.PARAGRAPH, "Introducción", "", Map.of("styleName", "Heading 1")),
                DocumentBlock.of("B002", DocumentBlockType.IMAGE_NOTICE, "Imagen sin descripción", "", Map.of())
        ));
        ReadingProfile profile = new ReadingProfile(
                ReadingProfile.academicDefaults().id(),
                "Sin imágenes",
                "Ignora imágenes",
                ReadingProfile.academicDefaults().headingRules(),
                ImageNarrationPolicy.IGNORE_IMAGES,
                ReadingProfile.academicDefaults().tablePolicy()
        );

        ReadingProfilePreview preview = new PreviewReadingProfileUseCase().preview(document, profile);

        assertEquals(1, preview.changedCount());
        assertEquals(DocumentBlockType.PARAGRAPH, document.blocks().get(0).type(), "La previsualización no debe mutar el documento original");
        assertTrue(preview.changedItems().stream().anyMatch(item -> item.proposedType() == DocumentBlockType.HEADING));
        assertEquals(DocumentBlockType.IMAGE_NOTICE, preview.items().get(1).proposedType(),
                "Las imágenes internas del Word permanecen como bloque visual no narrable, incluso si el perfil no las lee.");
    }
}
