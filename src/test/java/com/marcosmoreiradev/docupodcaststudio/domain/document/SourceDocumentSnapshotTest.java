package com.marcosmoreiradev.docupodcaststudio.domain.document;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SourceDocumentSnapshotTest {
    @Test
    void detectsWhenSourceContentChangesAndMarksDerivedArtifactsForReview() {
        ReadableDocument previousDocument = document("Texto original para narrar.");
        ReadableDocument refreshedDocument = document("Texto actualizado para narrar.");

        SourceDocumentChangeReport report = SourceDocumentChangeReport.compare(
                SourceDocumentSnapshot.from(previousDocument),
                SourceDocumentSnapshot.from(refreshedDocument));

        assertTrue(report.hasContentChanges());
        assertTrue(report.audioShouldBeRegenerated());
        assertTrue(report.requiresUserReview());
        assertTrue(report.summary().contains("audio queda obsoleto"));
    }

    @Test
    void unchangedSourceKeepsDerivedArtifactsCurrent() {
        ReadableDocument previousDocument = document("Texto estable para narrar.");
        ReadableDocument refreshedDocument = document("Texto estable para narrar.");

        SourceDocumentChangeReport report = SourceDocumentChangeReport.compare(
                SourceDocumentSnapshot.from(previousDocument),
                SourceDocumentSnapshot.from(refreshedDocument));

        assertFalse(report.hasContentChanges());
        assertFalse(report.audioShouldBeRegenerated());
        assertFalse(report.requiresUserReview());
    }

    private static ReadableDocument document(String text) {
        return new ReadableDocument(
                "Documento de prueba",
                SourceDocumentFormat.DOCX,
                Path.of("documento-prueba.docx"),
                List.of(DocumentBlock.of("B001", DocumentBlockType.PARAGRAPH, text, "Normal")));
    }
}
