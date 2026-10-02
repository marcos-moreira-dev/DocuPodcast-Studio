package com.marcosmoreiradev.docupodcaststudio.infrastructure.json;

import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetKind;
import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.project.ProjectMode;
import com.marcosmoreiradev.docupodcaststudio.domain.study.DocumentMascotPosition;
import com.marcosmoreiradev.docupodcaststudio.domain.study.DocumentParagraphVisualAssignment;
import com.marcosmoreiradev.docupodcaststudio.domain.study.DocumentStudyMusicTrack;
import com.marcosmoreiradev.docupodcaststudio.domain.study.DocumentStudyClosingSlide;
import com.marcosmoreiradev.docupodcaststudio.domain.study.DocumentStudyVideoConfiguration;
import com.marcosmoreiradev.docupodcaststudio.domain.study.DocumentTableSlideConfiguration;
import com.marcosmoreiradev.docupodcaststudio.domain.study.DocumentVisualSource;
import com.marcosmoreiradev.docupodcaststudio.domain.study.DocumentVideoSlideConfiguration;
import com.marcosmoreiradev.docupodcaststudio.domain.study.SecondarySlideInclusionMode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DocumentStudyVideoConfigurationJsonTest {
    @Test
    void absentAppearanceFieldsKeepLegacyInlineIllustrations() throws Exception {
        String json = new DocuPodcastProjectJsonWriter().write(DocuPodcastProject.createNew("Legacy"));
        json = json.replaceAll("\\s*\"aiIllustration(?:Background|Opacity|Fit)\"\\s*:[^,}]+,", "");
        var config = new DocuPodcastProjectJsonReader().read(json).study().documentaryVideoConfiguration();
        assertEquals(com.marcosmoreiradev.docupodcaststudio.domain.study.DocumentAiIllustrationAppearance.defaults(),
                config.aiIllustrationAppearance());
    }

    @Test
    void writesAndReadsDocumentaryVideoConfiguration() throws Exception {
        DocumentParagraphVisualAssignment visual = new DocumentParagraphVisualAssignment(
                "B001", "fingerprint", "IMG-IMPORTED", "IMG-DRAWN",
                "media/images/document-study/drawings/B001.ink.json",
                DocumentVisualSource.DRAWN, "IMG-MASCOT", DocumentMascotPosition.BOTTOM_LEFT,
                30, "Una pausa necesaria", true);
        DocumentStudyVideoConfiguration configuration = new DocumentStudyVideoConfiguration(
                "Instinto creativo", 8.0, List.of(visual),
                List.of(new DocumentTableSlideConfiguration("B002", 11.0)),
                List.of(new DocumentStudyMusicTrack("MUSIC-1", "AUD-1", 25.5, 0.27)),
                List.of("B003"),
                List.of(new DocumentStudyClosingSlide("DOC-CLOSING-1", "Gracias", 7.0, "IMG-END")))
                .withAiIllustrationsEnabled(true)
                .withAiIllustrationAppearance(new com.marcosmoreiradev.docupodcaststudio.domain.study.DocumentAiIllustrationAppearance(true, 0.5, "CONTAIN"))
                .withSecondarySlideInclusionMode(
                        SecondarySlideInclusionMode.INCLUDE_ALLOWED)
                .withContent(new DocumentVideoSlideConfiguration(
                        "PDF-COMP-1", "content-fingerprint",
                        DocumentParagraphVisualAssignment.empty("PDF-COMP-1"),
                        9.0, true, "AST-PDF-SOURCE-1",
                        "visual-fingerprint"));
        ProjectAssetReference sourceVisual = new ProjectAssetReference(
                "AST-PDF-SOURCE-1", ProjectAssetKind.STUDY_SOURCE_CROP,
                "PDF-COMP-1.png",
                "media/images/document-study/pdf-roi/PDF-COMP-1.png",
                "image/png", "Visual original", "sha-visual", "");
        DocuPodcastProject project = DocuPodcastProject.createNew("Documental", ProjectMode.DOCUMENTARY_STUDIO)
                .withStudy(DocuPodcastProject.createNew("base").study()
                        .withDocumentaryVideoConfiguration(configuration))
                .withAsset(sourceVisual);

        String json = new DocuPodcastProjectJsonWriter().write(project);
        assertEquals(configuration.aiIllustrationAppearance(), new DocuPodcastProjectJsonReader().read(json)
                .study().documentaryVideoConfiguration().aiIllustrationAppearance());
        assertTrue(json.contains("\"sourceVisualAssetId\""));
        assertTrue(!json.contains("\"sourceRoiAssetId\""));
        DocuPodcastProject reopened = new DocuPodcastProjectJsonReader().read(json);
        DocumentStudyVideoConfiguration actual = reopened.study().documentaryVideoConfiguration();

        assertEquals("Instinto creativo", actual.videoTitle());
        assertTrue(actual.aiIllustrationsEnabled());
        assertEquals(8.0, actual.defaultTableDurationSeconds());
        assertEquals(8.0, actual.defaultSecondarySemanticDurationSeconds());
        assertEquals(SecondarySlideInclusionMode.INCLUDE_ALLOWED,
                actual.secondarySlideInclusionMode());
        assertEquals(visual, actual.paragraph("B001").orElseThrow());
        assertEquals("Una pausa necesaria", actual.paragraph("B001").orElseThrow().subtitle());
        assertEquals(30, actual.paragraph("B001").orElseThrow().mascotSizePercent());
        assertTrue(actual.paragraph("B001").orElseThrow().illustrationOnly());
        assertEquals(11.0, actual.tableDuration("B002"));
        assertEquals(1, actual.musicTracks().size());
        assertEquals(0.27, actual.musicTracks().getFirst().volume());
        assertTrue(actual.disabledBlockIds().contains("B003"));
        assertTrue(!actual.blockEnabled("B003"));
        assertEquals("Gracias", actual.closingSlide("DOC-CLOSING-1").orElseThrow().title());
        assertEquals(7.0, actual.closingSlide("DOC-CLOSING-1").orElseThrow().durationSeconds());
        DocumentVideoSlideConfiguration pdfSlide = actual.content("PDF-COMP-1").orElseThrow();
        assertEquals("content-fingerprint", pdfSlide.sourceFingerprint());
        assertEquals("AST-PDF-SOURCE-1", pdfSlide.sourceVisualAssetId());
        assertEquals(sourceVisual,
                reopened.assets().byId("AST-PDF-SOURCE-1").orElseThrow());
        assertEquals("visual-fingerprint", pdfSlide.sourceVisualFingerprint());
    }

    @Test
    void readsLegacyPathFieldIntoTheCanonicalProjectAssetCatalog()
            throws Exception {
        DocumentStudyVideoConfiguration legacyConfiguration =
                DocumentStudyVideoConfiguration.empty().withContent(
                        new DocumentVideoSlideConfiguration(
                                "PDF-COMP-OLD", "content-fingerprint",
                                DocumentParagraphVisualAssignment.empty("PDF-COMP-OLD"),
                                7.0, true,
                                "media/images/document-study/pdf-roi/old.png",
                                "visual-fingerprint"));
        DocuPodcastProject legacyProject = DocuPodcastProject
                .createNew("Legacy", ProjectMode.DOCUMENTARY_STUDIO)
                .withStudy(DocuPodcastProject.createNew("base").study()
                        .withDocumentaryVideoConfiguration(legacyConfiguration));
        String legacyJson = new DocuPodcastProjectJsonWriter().write(legacyProject)
                .replace("\"sourceVisualAssetId\"",
                        "\"sourceRoiAssetId\"");

        DocuPodcastProject reopened =
                new DocuPodcastProjectJsonReader().read(legacyJson);
        DocumentVideoSlideConfiguration slide = reopened.study()
                .documentaryVideoConfiguration().content("PDF-COMP-OLD")
                .orElseThrow();

        assertTrue(slide.sourceVisualAssetId().startsWith(
                "AST-DOCSRC-LEGACY-"));
        ProjectAssetReference migrated = reopened.assets()
                .byId(slide.sourceVisualAssetId()).orElseThrow();
        assertEquals(ProjectAssetKind.STUDY_SOURCE_CROP, migrated.kind());
        assertEquals("media/images/document-study/pdf-roi/old.png",
                migrated.relativePath());
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
        assertTrue(!configuration.aiIllustrationsEnabled());
        assertEquals(6.0, configuration.defaultTableDurationSeconds());
        assertEquals(6.0, configuration.defaultSecondarySemanticDurationSeconds());
        assertEquals(SecondarySlideInclusionMode.FOLLOW_READING_POLICY,
                configuration.secondarySlideInclusionMode());
        assertTrue(configuration.paragraphVisuals().isEmpty());
        assertTrue(configuration.musicTracks().isEmpty());
        assertTrue(configuration.disabledBlockIds().isEmpty());
        assertTrue(configuration.closingSlides().isEmpty());
    }

    @Test
    void readsLegacyTableDurationAsSecondarySemanticFallback() throws Exception {
        DocuPodcastProject old = new DocuPodcastProjectJsonReader().read("""
                {
                  "formatVersion": 1,
                  "project": {
                    "id": "PRJ-OLD", "title": "Antiguo", "description": "", "language": "es",
                    "kind": "EMPTY", "mode": "DOCUMENTARY_STUDIO", "status": "DRAFT",
                    "createdAt": "2026-01-01T00:00:00Z", "updatedAt": "2026-01-01T00:00:00Z"
                  },
                  "assets": { "items": [] },
                  "study": { "documentaryVideoConfiguration": { "defaultTableDurationSeconds": 13.0 } },
                  "view": {}
                }
                """);

        DocumentStudyVideoConfiguration configuration =
                old.study().documentaryVideoConfiguration();
        assertEquals(13.0,
                configuration.defaultSecondarySemanticDurationSeconds());
        assertEquals(SecondarySlideInclusionMode.FOLLOW_READING_POLICY,
                configuration.secondarySlideInclusionMode());
    }
}
