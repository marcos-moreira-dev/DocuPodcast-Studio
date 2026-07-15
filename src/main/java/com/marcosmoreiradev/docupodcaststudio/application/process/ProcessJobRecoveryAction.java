package com.marcosmoreiradev.docupodcaststudio.application.process;

/** Action the UI may expose for a process job without embedding domain logic in the view. */
public record ProcessJobRecoveryAction(
        String actionId,
        String jobId,
        String label,
        String description,
        boolean enabled
) {
    public ProcessJobRecoveryAction {
        actionId = normalize(actionId).isBlank() ? "inspect" : normalize(actionId);
        jobId = normalize(jobId);
        label = normalize(label).isBlank() ? "Revisar" : normalize(label);
        description = normalize(description);
    }

    public static ProcessJobRecoveryAction cancel(String jobId) {
        return new ProcessJobRecoveryAction("cancel", jobId, "Cancelar",
                "Solicitar cancelacion segura del proceso en curso.", true);
    }

    public static ProcessJobRecoveryAction retry(String jobId) {
        return new ProcessJobRecoveryAction("retry", jobId, "Reintentar",
                "Reintentar el proceso conservando los datos y outputs validos.", true);
    }

    public static ProcessJobRecoveryAction inspect(String jobId) {
        return new ProcessJobRecoveryAction("inspect", jobId, "Ver detalle",
                "Abrir diagnostico humano y detalle tecnico controlado.", true);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }
}
