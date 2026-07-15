package com.marcosmoreiradev.docupodcaststudio.application.process;

import com.marcosmoreiradev.docupodcaststudio.domain.process.ProcessJobKind;

/** Human-facing explanation for a process job that failed or needs attention. */
public record ProcessJobFailureDiagnostic(
        String jobId,
        ProcessJobKind kind,
        String humanMessage,
        String technicalDetail,
        boolean blocking,
        boolean canRetry,
        boolean canCancel
) {
    public ProcessJobFailureDiagnostic {
        jobId = normalize(jobId);
        kind = kind == null ? ProcessJobKind.MEDIA_PREPARATION : kind;
        humanMessage = normalize(humanMessage).isBlank()
                ? "El proceso requiere revision antes de continuar."
                : normalize(humanMessage);
        technicalDetail = normalize(technicalDetail);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }
}
