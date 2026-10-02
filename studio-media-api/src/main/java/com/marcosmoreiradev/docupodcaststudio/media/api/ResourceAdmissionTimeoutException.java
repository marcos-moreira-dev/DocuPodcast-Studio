package com.marcosmoreiradev.docupodcaststudio.media.api;

/** A compute request exhausted its operation deadline before resources were admitted. */
public final class ResourceAdmissionTimeoutException extends InterruptedException {
    private final String admissionId;
    private final String operationId;
    private final String fitFailureReason;

    public ResourceAdmissionTimeoutException(String admissionId,
                                             String operationId,
                                             String fitFailureReason) {
        super("RESOURCE_ADMISSION_TIMEOUT: operation=" + operationId
                + " admission=" + admissionId + " reason=" + fitFailureReason);
        this.admissionId = admissionId == null ? "" : admissionId;
        this.operationId = operationId == null ? "" : operationId;
        this.fitFailureReason = fitFailureReason == null ? "UNKNOWN"
                : fitFailureReason;
    }

    public String admissionId() { return admissionId; }
    public String operationId() { return operationId; }
    public String fitFailureReason() { return fitFailureReason; }
}
