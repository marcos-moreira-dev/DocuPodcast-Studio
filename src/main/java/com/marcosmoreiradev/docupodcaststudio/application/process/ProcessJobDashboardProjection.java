package com.marcosmoreiradev.docupodcaststudio.application.process;

import com.marcosmoreiradev.docupodcaststudio.domain.process.ProcessJobKind;
import com.marcosmoreiradev.docupodcaststudio.domain.process.ProcessJobSnapshot;

import java.util.List;

/** Read model for status bar, process overlay, export center and support diagnostics. */
public record ProcessJobDashboardProjection(
        List<ProcessJobSnapshot> jobs,
        List<ProcessJobFailureDiagnostic> diagnostics,
        List<ProcessJobRecoveryAction> recoveryActions,
        int totalJobs,
        int runningJobs,
        int failedJobs,
        int recoverableJobs,
        int cancellableJobs,
        int exportJobs,
        int visualJobs,
        int downloadJobs,
        List<String> warnings
) {
    public ProcessJobDashboardProjection {
        jobs = jobs == null ? List.of() : List.copyOf(jobs);
        diagnostics = diagnostics == null ? List.of() : List.copyOf(diagnostics);
        recoveryActions = recoveryActions == null ? List.of() : List.copyOf(recoveryActions);
        totalJobs = Math.max(0, totalJobs);
        runningJobs = Math.max(0, runningJobs);
        failedJobs = Math.max(0, failedJobs);
        recoverableJobs = Math.max(0, recoverableJobs);
        cancellableJobs = Math.max(0, cancellableJobs);
        exportJobs = Math.max(0, exportJobs);
        visualJobs = Math.max(0, visualJobs);
        downloadJobs = Math.max(0, downloadJobs);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }

    public boolean hasBlockingDiagnostics() {
        return diagnostics.stream().anyMatch(ProcessJobFailureDiagnostic::blocking);
    }

    public List<ProcessJobSnapshot> relatedTo(ProcessJobKind kind) {
        if (kind == null) {
            return List.of();
        }
        return jobs.stream().filter(job -> job.kind() == kind).toList();
    }

    public String summary() {
        if (totalJobs == 0) {
            return "Sin procesos registrados.";
        }
        return totalJobs + " proceso(s); " + runningJobs + " en curso; "
                + failedJobs + " fallido(s); " + recoverableJobs + " recuperable(s).";
    }
}
