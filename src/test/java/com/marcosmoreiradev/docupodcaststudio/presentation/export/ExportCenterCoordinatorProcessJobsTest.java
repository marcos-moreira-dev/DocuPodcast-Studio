package com.marcosmoreiradev.docupodcaststudio.presentation.export;

import com.marcosmoreiradev.docupodcaststudio.application.export.DocuPodcastExportFormat;
import com.marcosmoreiradev.docupodcaststudio.application.export.ExportReadinessItem;
import com.marcosmoreiradev.docupodcaststudio.application.export.ExportReadinessReport;
import com.marcosmoreiradev.docupodcaststudio.application.export.ExportableArtifactKind;
import com.marcosmoreiradev.docupodcaststudio.domain.process.ProcessJobKind;
import com.marcosmoreiradev.docupodcaststudio.domain.process.ProcessJobLogReference;
import com.marcosmoreiradev.docupodcaststudio.domain.process.ProcessJobSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.process.ProcessJobStage;
import com.marcosmoreiradev.docupodcaststudio.domain.process.ProcessJobState;
import com.marcosmoreiradev.docupodcaststudio.domain.project.ProjectMode;
import com.marcosmoreiradev.docupodcaststudio.presentation.command.AppCommandId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class ExportCenterCoordinatorProcessJobsTest {
    @Test
    void narrativeVideoTargetShowsRelatedFailedRenderJob() {
        ProcessJobSnapshot failedVideo = new ProcessJobSnapshot(
                "VIDEO-1",
                ProcessJobKind.VIDEO_RENDER,
                "Render narrativo",
                ProcessJobState.FAILED,
                ProcessJobStage.FAILED,
                0.5,
                "FRG-B001",
                "Fragmento 1",
                0,
                "FFmpeg devolvio error.",
                "jobs/video/VIDEO-1",
                false,
                true,
                List.of(),
                new ProcessJobLogReference("", "jobs/video/VIDEO-1/logs/render-diagnostics.jsonl", "", ""),
                Instant.parse("2026-06-21T10:00:00Z"),
                Instant.parse("2026-06-21T10:01:00Z"));
        ExportReadinessReport readiness = ExportReadinessReport.from("Video", List.of(
                ExportReadinessItem.exportable(
                        ExportableArtifactKind.PODCAST_WAV,
                        DocuPodcastExportFormat.WAV,
                        "audio-final.wav",
                        List.of("Listo."),
                        List.of()),
                ExportReadinessItem.exportable(
                        ExportableArtifactKind.FINAL_VIDEO_MP4,
                        DocuPodcastExportFormat.MP4,
                        "video-narrativo-final.mp4",
                        List.of("Listo."),
                        List.of())));
        ExportCenterState state = new ExportCenterState(ProjectMode.NARRATIVE_VIDEO,
                true, readiness, List.of(failedVideo));

        ExportTargetPresentation target = new ExportCenterCoordinator().targets(state).stream()
                .filter(candidate -> candidate.commandId() == AppCommandId.EXPORT_SIMPLE_VIDEO_PACKAGE)
                .findFirst()
                .orElseThrow();

        assertTrue(target.relatedProcessSummary().contains("fallido"));
        assertTrue(target.relatedProcessSummary().contains("Ver estado"));
    }
}
