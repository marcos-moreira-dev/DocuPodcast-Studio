package com.marcosmoreiradev.docupodcaststudio.infrastructure.json;

import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.project.ProjectMode;
import com.marcosmoreiradev.docupodcaststudio.domain.study.DocumentMascotPosition;
import com.marcosmoreiradev.docupodcaststudio.domain.study.DocumentParagraphVisualAssignment;
import com.marcosmoreiradev.docupodcaststudio.domain.study.DocumentStudyMusicTrack;
import com.marcosmoreiradev.docupodcaststudio.domain.study.DocumentStudyClosingSlide;
import com.marcosmoreiradev.docupodcaststudio.domain.study.DocumentStudyVideoConfiguration;
import com.marcosmoreiradev.docupodcaststudio.domain.study.DocumentTableSlideConfiguration;
import com.marcosmoreiradev.docupodcaststudio.domain.study.DocumentVisualSource;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DocumentStudyVideoConfigurationJsonTest {
    @Test
    void writesAndReadsDocumentaryVideoConfiguration() throws Exception {
        DocumentParagraphVisualAssignment visual = new DocumentParagraphVisualAssignment(
                "B001", "fingerprint", "IMG-IMPORTED", "IMG-DRAWN",
                "media/images/document-study/drawings/B001.ink.json",
                DocumentVisualSource.DRAWN, "IMG-MASCOT", DocumentMascotPosition.BOTTOM_LEFT);
        DocumentStudyVideoConfiguration configuration = new DocumentStudyVideoConfiguration(
                "Instinto creativo", 8.0, List.of(visual),
                List.of(new DocumentTableSlideConfiguration("B002", 11.0)),
                List.of(new DocumentStudyMusicTrack("MUSIC-1", "AUD-1", 25.5, 0.27)),
                List.of("B003"),
                List.of(new DocumentStudyClosingSlide("DOC-CLOSING-1", "Gracias", 7.0, "IMG-END")));
        DocuPodcastProject project = DocuPodcastProject.createNew("Documental", ProjectMode.DOCUMENTARY_STUDIO)
                .withStudy(DocuPodcastProject.createNew("base").study()
                        .withDocumentaryVideoConfiguration(configuration));

        String json = new DocuPodcastProjectJsonWriter().write(project);
        DocuPodcastProject reopened = new DocuPodcastProjectJsonReader().read(json);
        DocumentStudyVideoConfiguration actual = reopened.study().documentaryVideoConfiguration();

        assertEquals("Instinto creativo", actual.videoTitle());
        assertEquals(8.0, actual.defaultTableDurationSeconds());
        assertEquals(visual, actual.paragraph("B001").orElseThrow());
        assertEquals(11.0, actual.tableDuration("B002"));
        assertEquals(1, actual.musicTracks().size());
        assertEquals(0.27, actual.musicTracks().getFirst().volume());
        assertTrue(actual.disabledBlockIds().contains("B003"));
        assertTrue(!actual.blockEnabled("B003"));
        assertEquals("Gracias", actual.closingSlide("DOC-CLOSING-1").orElseThrow().title());
        assertEquals(7.0, actual.closingSlide("DOC-CLOSING-1").orElseThrow().durationSeconds());
    }

    @Test
    void oldProjectWithoutStudyConfigurationUsesEmptyDefaults() throws Exception {
        DocuPodcastProject old = new DocuPodcastProjectJsonReader().read("""
                {
                  "formatVersion": 1,
                  "project": {
                    "id": "PRJ-OLD", "title": "Antiguo", "description": "", "language": "es",
                    "kind": "EMPTY", "mode": "DOCUMENTARY_STUDIO", "status": "DRAFT",
                    "createdAt": "2026-01-01T00:00:00Z", "updatedAt": "2026-01-01T00:00:00Z"
                  },
                  "assets": { "items": [] },
                  "view": {}
                }
                """);

        DocumentStudyVideoConfiguration configuration = old.study().documentaryVideoConfiguration();
        assertTrue(configuration.videoTitle().isBlank());
        assertEquals(6.0, configuration.defaultTableDurationSeconds());
        assertTrue(configuration.paragraphVisuals().isEmpty());
        assertTrue(configuration.musicTracks().isEmpty());
        assertTrue(configuration.disabledBlockIds().isEmpty());
        assertTrue(configuration.closingSlides().isEmpty());
    }
}
