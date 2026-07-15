package com.marcosmoreiradev.docupodcaststudio.application.errors;

import com.marcosmoreiradev.docupodcaststudio.application.decisions.DecisionSeverity;

/** Technical infrastructure failure while reading/writing files, downloads or local resources. */
public class InfrastructureOperationException extends RuntimeException implements UserFacingApplicationException {
    private final String headline;
    private final String technicalDetail;

    public InfrastructureOperationException(String headline, String message, Throwable cause) {
        this(headline, message, "", cause);
    }

    public InfrastructureOperationException(String headline, String message, String technicalDetail, Throwable cause) {
        super(message, cause);
        this.headline = normalize(headline, "No se pudo completar la operación");
        this.technicalDetail = technicalDetail == null ? "" : technicalDetail.strip();
    }

    @Override
    public DecisionSeverity severity() {
        return DecisionSeverity.ERROR;
    }

    @Override
    public String userHeadline() {
        return headline;
    }

    @Override
    public String userMessage() {
        return getMessage() == null || getMessage().isBlank() ? "Ocurrió un problema técnico local." : getMessage();
    }

    @Override
    public String technicalDetail() {
        String detail = technicalDetail;
        Throwable cause = getCause();
        if (cause != null) {
            String causeDetail = cause.getClass().getName() + ": " + (cause.getMessage() == null ? "sin mensaje" : cause.getMessage());
            detail = detail.isBlank() ? causeDetail : detail + System.lineSeparator() + "Causa: " + causeDetail;
        }
        return detail;
    }

    private static String normalize(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.strip();
    }
}
