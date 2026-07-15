package com.marcosmoreiradev.docupodcaststudio.application.video;

import com.marcosmoreiradev.docupodcaststudio.domain.process.ProcessJobArtifact;
import com.marcosmoreiradev.docupodcaststudio.domain.process.ProcessJobLogReference;
import com.marcosmoreiradev.docupodcaststudio.domain.process.ProcessJobStage;
import com.marcosmoreiradev.docupodcaststudio.domain.process.ProcessJobState;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

/** Persistent state for an FFmpeg video render job. */
public record VideoRenderJobSnapshot(
        String jobId,
        ProcessJobState state,
        ProcessJobStage stage,
        double progress,
        int totalFrames,
        int completedFrames,
        String currentStep,
        String message,
        String packageRelativeDirectory,
        String jobRelativeDirectory,
        String outputRelativePath,
        boolean cancellationRequested,
        boolean recoverable,
        List<ProcessJobArtifact> artifacts,
        ProcessJobLogReference logs,
        Instant createdAt,
        Instant updatedAt
) {
    public VideoRenderJobSnapshot {
        jobId = token(jobId, "jobId");
        state = state == null ? ProcessJobState.QUEUED : state;
        stage = stage == null ? ProcessJobStage.PREPARING_WORKSPACE : stage;
        progress = clamp(progress);
        totalFrames = Math.max(0, totalFrames);
        completedFrames = Math.max(0, Math.min(completedFrames, totalFrames));
        currentStep = normalize(currentStep);
        message = normalize(message);
        packageRelativeDirectory = portableOptionalPath(packageRelativeDirectory);
        jobRelativeDirectory = portableOptionalPath(jobRelativeDirectory);
        outputRelativePath = portableOptionalPath(outputRelativePath);
        artifacts = artifacts == null ? List.of() : List.copyOf(artifacts);
        logs = logs == null ? new ProcessJobLogReference("", "", "", "") : logs;
        createdAt = createdAt == null ? Instant.now() : createdAt;
        updatedAt = updatedAt == null ? createdAt : updatedAt;
    }

    public static VideoRenderJobSnapshot queued(String jobId,
                                                SimpleVideoPackageExportResult packageExport,
                                                VideoRenderCommandPlan commandPlan,
                                                String packageRelativeDirectory,
                                                String jobRelativeDirectory) {
        int frames = commandPlan == null || commandPlan.videoPlan() == null ? 0 : commandPlan.videoPlan().frameCount();
        String output = packageRelativeDirectory == null || packageRelativeDirectory.isBlank()
                ? "video-simple.mp4"
                : packageRelativeDirectory.replace('\\', '/') + "/" + commandPlanOutput(commandPlan);
        List<ProcessJobArtifact> artifacts = List.of(
                new ProcessJobArtifact("render-package", packageRelativeDirectory, "Paquete auditable de storyboard/video"),
                new ProcessJobArtifact("render-manifest", packageRelativeDirectory + "/RENDER_MANIFEST.json", "Manifest de render"),
                new ProcessJobArtifact("render-commands", packageRelativeDirectory + "/render-commands.txt", "Comandos FFmpeg auditables"),
                new ProcessJobArtifact("video-output", output, "Salida MP4 prevista")
        );
        ProcessJobLogReference logs = new ProcessJobLogReference(
                "",
                jobRelativeDirectory + "/logs/render-diagnostics.jsonl",
                jobRelativeDirectory + "/logs/stdout.log",
                jobRelativeDirectory + "/logs/stderr.log"
        );
        return new VideoRenderJobSnapshot(
                jobId,
                ProcessJobState.QUEUED,
                ProcessJobStage.PREPARING_WORKSPACE,
                0.0,
                frames,
                0,
                "Render en cola",
                "Render FFmpeg preparado como job persistente y cancelable.",
                packageRelativeDirectory,
                jobRelativeDirectory,
                output,
                false,
                true,
                artifacts,
                logs,
                Instant.now(),
                Instant.now()
        );
    }

    public VideoRenderJobSnapshot cancellationRequested(String reason) {
        return new VideoRenderJobSnapshot(
                jobId,
                ProcessJobState.CANCELLATION_REQUESTED,
                ProcessJobStage.RUNNING_ENGINE,
                progress,
                totalFrames,
                completedFrames,
                currentStep,
                normalize(reason).isBlank() ? "Cancelación solicitada por el usuario." : reason,
                packageRelativeDirectory,
                jobRelativeDirectory,
                outputRelativePath,
                true,
                true,
                artifacts,
                logs,
                createdAt,
                Instant.now()
        );
    }

    public VideoRenderJobSnapshot cancelled(String reason) {
        return new VideoRenderJobSnapshot(
                jobId,
                ProcessJobState.CANCELLED,
                ProcessJobStage.CANCELLED,
                progress,
                totalFrames,
                completedFrames,
                currentStep,
                normalize(reason).isBlank() ? "Render cancelado de forma segura." : reason,
                packageRelativeDirectory,
                jobRelativeDirectory,
                outputRelativePath,
                true,
                true,
                artifacts,
                logs,
                createdAt,
                Instant.now()
        );
    }

    public boolean cancellable() {
        return state.cancellable();
    }

    public String progressLabel() {
        return Math.round(progress * 100.0) + "%";
    }

    private static String commandPlanOutput(VideoRenderCommandPlan commandPlan) {
        return commandPlan == null ? "video-simple.mp4" : commandPlan.outputFileName();
    }

    private static String token(String value, String field) {
        String normalized = normalize(value);
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        if (normalized.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException(field + " must not contain whitespace");
        }
        return normalized;
    }

    private static String portableOptionalPath(String value) {
        String normalized = normalize(value).replace('\\', '/');
        if (normalized.isBlank()) {
            return "";
        }
        if (normalized.startsWith("/") || normalized.matches("^[A-Za-z]:/.*") || normalized.startsWith("../")
                || normalized.contains("/../") || normalized.startsWith("./") || normalized.contains("://")
                || normalized.toLowerCase(Locale.ROOT).startsWith("file:")) {
            throw new IllegalArgumentException("Path must be project-relative: " + value);
        }
        return normalized;
    }

    private static double clamp(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(1.0, value));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }
}
