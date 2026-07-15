package com.marcosmoreiradev.docupodcaststudio.application.export;

import com.marcosmoreiradev.docupodcaststudio.domain.project.ProjectMode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ProjectExportTargetCatalogTest {
    private final ProjectExportTargetCatalog catalog = new ProjectExportTargetCatalog();

    @Test
    void creativeTargetsFollowProjectMode() {
        assertEquals(List.of(
                ExportableArtifactKind.PODCAST_WAV,
                ExportableArtifactKind.DOCUMENT_TEXT_AUDIO_VIDEO),
                catalog.creativeTargets(ProjectMode.DOCUMENTARY_STUDIO));

        assertEquals(List.of(
                ExportableArtifactKind.PODCAST_WAV,
                ExportableArtifactKind.FINAL_VIDEO_MP4),
                catalog.creativeTargets(ProjectMode.NARRATIVE_VIDEO));

        assertEquals(List.of(
                ExportableArtifactKind.PODCAST_WAV,
                ExportableArtifactKind.THEATRE_WORK_VIDEO,
                ExportableArtifactKind.THEATRE_SPATIAL_MAP_VIDEO,
                ExportableArtifactKind.THEATRE_PORTION_VIDEO),
                catalog.creativeTargets(ProjectMode.THEATRE_PRODUCTION));
    }

    @Test
    void supportTargetsRemainAuditableButNotCreative() {
        assertTrue(catalog.supportTargets().contains(ExportableArtifactKind.PROJECT_BUNDLE));
        assertTrue(catalog.supportTargets().contains(ExportableArtifactKind.DIAGNOSTIC_REPORT));
        assertTrue(catalog.supportTargets().contains(ExportableArtifactKind.STORYBOARD_SUMMARY));
        assertTrue(catalog.supportTargets().contains(ExportableArtifactKind.SIMPLE_VIDEO_PACKAGE));
        assertFalse(catalog.isCreativeTarget(ProjectMode.NARRATIVE_VIDEO, ExportableArtifactKind.SIMPLE_VIDEO_PACKAGE));
    }
}
