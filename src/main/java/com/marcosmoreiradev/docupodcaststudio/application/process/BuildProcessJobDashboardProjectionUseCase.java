package com.marcosmoreiradev.docupodcaststudio.application.process;

import com.marcosmoreiradev.docupodcaststudio.domain.process.ProcessJobArtifact;
import com.marcosmoreiradev.docupodcaststudio.domain.process.ProcessJobKind;
import com.marcosmoreiradev.docupodcaststudio.domain.process.ProcessJobSnapshot;
import com.marcosmoreiradev.docupodcaststudio.domain.process.ProcessJobState;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Builds a common process projection without replacing mature audio/video job stores. */
public final class BuildProcessJobDashboardProjectionUseCase {
    public ProcessJobDashboardProjection build(List<ProcessJobSnapshot> jobs) {
        return build(jobs, null);
    }

    public ProcessJobDashboardProjection build(List<ProcessJobSnapshot> jobs, Path projectDirectory) {
        List<ProcessJobSnapshot> ordered = (jobs == null ? List.<ProcessJobSnapshot>of() : jobs).stream()
                .sorted(Comparator.comparing(ProcessJobSnapshot::updatedAt).reversed())
                .toList();
        ArrayList<ProcessJobFailureDiagnostic> diagnostics = new ArrayList<>();
        ArrayList<ProcessJobRecoveryAction> actions = new ArrayList<>();
        ArrayList<String> warnings = new ArrayList<>();
        for (ProcessJobSnapshot job : ordered) {
            if (job.state() == ProcessJobState.FAILED) {
                diagnostics.add(new ProcessJobFailureDiagnostic(
                        job.jobId(),
                        job.kind(),
                        failedMessage(job),
                        technicalDetail(job),
                        true,
                        job.recoverable(),
                        job.cancellable()));
            }
            if (job.state() == ProcessJobState.NEEDS_USER_ACTION) {
                diagnostics.add(new ProcessJobFailureDiagnostic(
                        job.jobId(),
                        job.kind(),
                        actionMessage(job),
                        technicalDetail(job),
                        true,
                        job.recoverable(),
                        false));
            }
            diagnostics.addAll(missingOutputDiagnostics(job, projectDirectory));
            if (job.cancellable()) {
                actions.add(ProcessJobRecoveryAction.cancel(job.jobId()));
            }
            if (job.recoverable()) {
                actions.add(ProcessJobRecoveryAction.retry(job.jobId()));
            }
            if (job.logs().hasAnyLog() || !job.message().isBlank()) {
                actions.add(ProcessJobRecoveryAction.inspect(job.jobId()));
            }
        }
        if (ordered.stream().anyMatch(job -> job.kind() == ProcessJobKind.VISUAL_GENERATION
                && job.state() == ProcessJobState.FAILED)) {
            warnings.add("La generacion visual fallo en al menos un proceso; conserva assets anteriores antes de reintentar.");
        }
        return new ProcessJobDashboardProjection(
                ordered,
                diagnostics,
                actions,
                ordered.size(),
                count(ordered, ProcessJobSnapshot::running),
                count(ordered, job -> job.state() == ProcessJobState.FAILED),
                count(ordered, ProcessJobSnapshot::recoverable),
                count(ordered, ProcessJobSnapshot::cancellable),
                count(ordered, job -> job.kind() == ProcessJobKind.FINAL_EXPORT || job.kind() == ProcessJobKind.VIDEO_RENDER),
                count(ordered, job -> job.kind() == ProcessJobKind.VISUAL_GENERATION),
                count(ordered, job -> job.kind() == ProcessJobKind.MANAGED_DOWNLOAD),
                warnings);
    }

    private static List<ProcessJobFailureDiagnostic> missingOutputDiagnostics(ProcessJobSnapshot job, Path projectDirectory) {
        if (projectDirectory == null || job.state() != ProcessJobState.COMPLETED) {
            return List.of();
        }
        ArrayList<ProcessJobFailureDiagnostic> diagnostics = new ArrayList<>();
        Path root = projectDirectory.toAbsolutePath().normalize();
        for (ProcessJobArtifact artifact : job.artifacts()) {
            if (!requiresOutputValidation(artifact)) {
                continue;
            }
            if (artifact.relativePath().isBlank()) {
                diagnostics.add(missingOutput(job, "La salida final no declaro archivo producido."));
                continue;
            }
            Path target = root.resolve(artifact.relativePath()).normalize();
            if (!target.startsWith(root) || !Files.isRegularFile(target)) {
                diagnostics.add(missingOutput(job, "La salida final esperada no existe: " + artifact.relativePath()));
            }
        }
        return List.copyOf(diagnostics);
    }

    private static ProcessJobFailureDiagnostic missingOutput(ProcessJobSnapshot job, String detail) {
        return new ProcessJobFailureDiagnostic(
                job.jobId(),
                job.kind(),
                "El proceso se marco como completado, pero la salida final no se pudo verificar.",
                detail,
                true,
                job.recoverable(),
                false);
    }

    private static boolean requiresOutputValidation(ProcessJobArtifact artifact) {
        String role = artifact == null ? "" : artifact.role().toLowerCase(Locale.ROOT);
        return role.contains("output")
                || role.contains("final")
                || role.contains("export")
                || role.equals("video-output")
                || role.equals("final-audio");
    }

    private static String failedMessage(ProcessJobSnapshot job) {
        return "Fallo " + job.displayName() + ". Los datos del proyecto se conservan; revisa el detalle antes de reintentar.";
    }

    private static String actionMessage(ProcessJobSnapshot job) {
        return job.displayName() + " requiere una decision del usuario antes de continuar.";
    }

    private static String technicalDetail(ProcessJobSnapshot job) {
        if (!job.message().isBlank()) {
            return job.message();
        }
        if (job.logs().hasAnyLog()) {
            return "Hay logs o diagnosticos asociados al proceso.";
        }
        return "Sin detalle tecnico adicional.";
    }

    private static int count(List<ProcessJobSnapshot> jobs, java.util.function.Predicate<ProcessJobSnapshot> predicate) {
        return (int) jobs.stream().filter(predicate).count();
    }
}
