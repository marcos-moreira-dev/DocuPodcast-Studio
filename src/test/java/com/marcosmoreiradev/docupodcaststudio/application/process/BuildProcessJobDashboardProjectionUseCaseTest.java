package com.marcosmoreiradev.docupodcaststudio.application.process;

import com.marcosmoreiradev.docupodcaststudio.domain.process.ProcessJobArtifact;
import com.marcosmoreiradev.docupodcaststudio.domain.process.ProcessJobKind;
import com.marcosmoreiradev.docupodcaststudio.domain.process.ProcessJobLogReference;
import com.marcosmoreiradev.docupodcaststudio.domain.process.ProcessJobSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.process.ProcessJobStage;
import com.marcosmoreiradev.docupodcaststudio.domain.process.ProcessJobState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BuildProcessJobDashboardProjectionUseCaseTest {
    private final BuildProcessJobDashboardProjectionUseCase useCase = new BuildProcessJobDashboardProjectionUseCase();

    @TempDir
    Path tempDir;

    @Test
    void buildsHumanDiagnosticsRecoveryActionsAndCountsAcrossJobKinds() {
        ProcessJobSnapshot runningAudio = job("AUDIO-1", ProcessJobKind.TTS_AUDIO, ProcessJobState.RUNNING,
                true, true, List.of());
        ProcessJobSnapshot failedVisual = job("VISUAL-1", ProcessJobKind.VISUAL_GENERATION, ProcessJobState.FAILED,
                false, true, List.of());
        ProcessJobSnapshot download = job("DOWNLOAD-1", ProcessJobKind.MANAGED_DOWNLOAD, ProcessJobState.NEEDS_USER_ACTION,
                false, false, List.of());

        ProcessJobDashboardProjection projection = useCase.build(List.of(runningAudio, failedVisual, download));

        assertEquals(3, projection.totalJobs());
        assertEquals(1, projection.runningJobs());
        assertEquals(1, projection.failedJobs());
        assertEquals(2, projection.recoverableJobs());
        assertEquals(1, projection.cancellableJobs());
        assertEquals(1, projection.visualJobs());
        assertEquals(1, projection.downloadJobs());
        assertTrue(projection.hasBlockingDiagnostics());
        assertTrue(projection.diagnostics().stream().anyMatch(diagnostic ->
                diagnostic.jobId().equals("VISUAL-1") && diagnostic.canRetry()));
        assertTrue(projection.recoveryActions().stream().anyMatch(action ->
                action.jobId().equals("AUDIO-1") && action.actionId().equals("cancel")));
        assertTrue(projection.warnings().stream().anyMatch(warning -> warning.contains("generacion visual")));
    }

    @Test
    void completedExportWithMissingOutputBecomesBlockingDiagnostic() {
        ProcessJobSnapshot export = job("EXPORT-1", ProcessJobKind.FINAL_EXPORT, ProcessJobState.COMPLETED,
                false, true, List.of(new ProcessJobArtifact("final-output", "exports/final.mp4", "MP4 final")));

        ProcessJobDashboardProjection projection = useCase.build(List.of(export), tempDir);

        assertEquals(1, projection.exportJobs());
        assertTrue(projection.hasBlockingDiagnostics());
        assertTrue(projection.diagnostics().stream().anyMatch(diagnostic ->
                diagnostic.humanMessage().contains("salida final") && diagnostic.technicalDetail().contains("exports/final.mp4")));
    }

    @Test
    void completedExportWithExistingOutputDoesNotWarn() throws Exception {
        java.nio.file.Files.createDirectories(tempDir.resolve("exports"));
        java.nio.file.Files.writeString(tempDir.resolve("exports/final.mp4"), "not-empty");
        ProcessJobSnapshot export = job("EXPORT-1", ProcessJobKind.FINAL_EXPORT, ProcessJobState.COMPLETED,
                false, true, List.of(new ProcessJobArtifact("final-output", "exports/final.mp4", "MP4 final")));

        ProcessJobDashboardProjection projection = useCase.build(List.of(export), tempDir);

        assertFalse(projection.hasBlockingDiagnostics());
    }

    private static ProcessJobSnapshot job(String id,
                                          ProcessJobKind kind,
                                          ProcessJobState state,
                                          boolean cancellable,
                                          boolean recoverable,
                                          List<ProcessJobArtifact> artifacts) {
        return new ProcessJobSnapshot(
                id,
                kind,
                kind.displayName(),
                state,
                ProcessJobStage.RUNNING_ENGINE,
                state == ProcessJobState.COMPLETED ? 1.0 : 0.4,
                "FRG-B001",
                "Fragmento 1",
                12,
                "Detalle tecnico controlado",
                "jobs/" + id,
                cancellable,
                recoverable,
                artifacts,
                new ProcessJobLogReference("jobs/" + id + "/generation.jsonl", "jobs/" + id + "/diagnostics.jsonl", "", ""),
                Instant.parse("2026-06-21T10:00:00Z"),
                Instant.parse("2026-06-21T10:01:00Z"));
    }
}
