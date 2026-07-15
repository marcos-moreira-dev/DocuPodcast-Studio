package com.marcosmoreiradev.docupodcaststudio.presentation.theatre;

import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlock;
import com.marcosmoreiradev.docupodcaststudio.domain.document.DocumentBlockType;
import com.marcosmoreiradev.docupodcaststudio.domain.document.ReadableDocument;
import com.marcosmoreiradev.docupodcaststudio.domain.document.SourceDocumentFormat;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TheatreCharacterDetectorTest {
    @Test
    void detectsCharactersFromFirstColonOnlyAndIgnoresStructuralAliases() {
        ReadableDocument document = new ReadableDocument(
                "Obra",
                SourceDocumentFormat.TXT,
                Path.of("obra.txt"),
                List.of(
                        DocumentBlock.of("b1", DocumentBlockType.PARAGRAPH,
                                "NARRADOR: En el viejo aerodromo: dos aviadores preparan el primer vuelo.", ""),
                        DocumentBlock.of("b2", DocumentBlockType.PARAGRAPH,
                                "Escena 1: El hangar.", ""),
                        DocumentBlock.of("b3", DocumentBlockType.PARAGRAPH,
                                "Capitan Bigote: Revise el combustible.", ""),
                        DocumentBlock.of("b4", DocumentBlockType.PARAGRAPH,
                                "Capitan Bigote: Otra linea.", ""),
                        DocumentBlock.of("b5", DocumentBlockType.PARAGRAPH,
                                "INTERVENCION-1: Alias tecnico de fragmento.", "")));

        List<TheatreCharacterPresentation> characters = TheatreCharacterDetector.detect(document, null, List.of());

        assertEquals(2, characters.size());
        assertEquals("NARRADOR", characters.get(0).displayName());
        assertEquals("En el viejo aerodromo: dos aviadores preparan el primer vuelo.", characters.get(0).firstLinePreview());
        assertTrue(characters.get(0).cardPreview().contains("Ficha"));
        assertFalse(characters.get(0).cardPreview().contains("aerodromo"));
        assertEquals("Capitan Bigote", characters.get(1).displayName());
        assertEquals(2, characters.get(1).mentionCount());
        assertFalse(characters.stream().anyMatch(character -> character.displayName().startsWith("Escena")));
        assertFalse(characters.stream().anyMatch(character -> character.displayName().equals("INTERVENCION-1")));
    }

    @Test
    void mergesPersistedCharacterNotesIntoDetectedCards() {
        ReadableDocument document = new ReadableDocument(
                "Obra",
                SourceDocumentFormat.TXT,
                Path.of("obra.txt"),
                List.of(DocumentBlock.of("b1", DocumentBlockType.PARAGRAPH,
                        "Capitan Bigote: Revise el combustible.", "")));
        TheatreProjectLayer.CharacterProfile saved = new TheatreProjectLayer.CharacterProfile(
                "CHR-CAPITAN-BIGOTE",
                "Capitan Bigote",
                List.of("Bigote"),
                "Piloto veterano que confunde seguridad con volumen de voz.");

        List<TheatreCharacterPresentation> characters = TheatreCharacterDetector.detect(document, null, List.of(saved));

        assertEquals(1, characters.size());
        assertEquals("CHR-CAPITAN-BIGOTE", characters.get(0).id());
        assertTrue(characters.get(0).hasDescription());
        assertTrue(characters.get(0).cardPreview().contains("Piloto veterano"));
    }
}
