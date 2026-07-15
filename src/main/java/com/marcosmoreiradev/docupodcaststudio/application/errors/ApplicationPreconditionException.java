package com.marcosmoreiradev.docupodcaststudio.application.errors;

import com.marcosmoreiradev.docupodcaststudio.application.decisions.DecisionSeverity;

/** User action cannot continue because an application precondition is missing. */
public class ApplicationPreconditionException extends RuntimeException implements UserFacingApplicationException {
    private final String headline;
    private final String technicalDetail;

    public ApplicationPreconditionException(String headline, String message) {
        this(headline, message, "");
    }

    public ApplicationPreconditionException(String headline, String message, String technicalDetail) {
        super(message);
        this.headline = normalize(headline, "Acción no disponible");
        this.technicalDetail = technicalDetail == null ? "" : technicalDetail.strip();
    }

    @Override
    public DecisionSeverity severity() {
        return DecisionSeverity.WARNING;
    }

    @Override
    public String userHeadline() {
        return headline;
    }

    @Override
    public String userMessage() {
        return getMessage() == null || getMessage().isBlank() ? "Falta completar una condición antes de continuar." : getMessage();
    }

    @Override
    public String technicalDetail() {
        return technicalDetail;
    }

    private static String normalize(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.strip();
    }
}
