package com.marcosmoreiradev.docupodcaststudio.application.errors;

import com.marcosmoreiradev.docupodcaststudio.application.decisions.DecisionSeverity;

/** Exception contract for failures that must be translated into product-facing dialogs. */
public interface UserFacingApplicationException {
    DecisionSeverity severity();

    String userHeadline();

    String userMessage();

    String technicalDetail();
}
