package com.marcosmoreiradev.docupodcaststudio.application.reading;

import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlock;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlockType;
import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.document.SourceDocumentFormat;
import com.marcosmoreiradev.docupodcaststudio.domain.reading.ReadingProfile;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApplyReadingProfileUseCaseTest {
    @Test
    void appliesWordStyleRulesAndPreservesManualOverrides() {
        ReadableDocument document = new ReadableDocument("Notas", SourceDocumentFormat.DOCX, Path.of("notas.docx"), List.of(
                DocumentBlock.of("B001", DocumentBlockType.PARAGRAPH, "Introducción", "Heading1", Map.of("styleName", "Heading 1")),
                DocumentBlock.of("B002", DocumentBlockType.PARAGRAPH, "Objetivos específicos", "", Map.of("bold", "true")),
                DocumentBlock.of("B003", DocumentBlockType.PARAGRAPH, "No leer", "", Map.of("manualOverride", "true"))
                        .withType(DocumentBlockType.IGNORED, "test")
        ));

        ReadableDocument updated = new ApplyReadingProfileUseCase().apply(document, ReadingProfile.academicDefaults());

        assertEquals(DocumentBlockType.HEADING, updated.blocks().get(0).type());
        assertEquals(DocumentBlockType.SUBHEADING, updated.blocks().get(1).type());
        assertEquals(DocumentBlockType.IGNORED, updated.blocks().get(2).type());
    }
}
