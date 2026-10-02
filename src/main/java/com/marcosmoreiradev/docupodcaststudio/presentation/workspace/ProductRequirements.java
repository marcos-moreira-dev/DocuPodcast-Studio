package com.marcosmoreiradev.docupodcaststudio.presentation.workspace;

/** Official transversal requirements used to provision infrastructure and GUI composition. */
public final class ProductRequirements {
    public static final ProductRequirementId DOCUMENT_PAGED = id("content.document-paged");
    public static final ProductRequirementId CONTEXT_RAILS = id("layout.context-rails");
    public static final ProductRequirementId PRODUCTION_BOARD = id("layout.production-board");
    public static final ProductRequirementId INK_EDITING = id("editing.ink");
    public static final ProductRequirementId MEDIA_CANDIDATE_REVIEW = id("media.candidate-review");
    public static final ProductRequirementId PERSISTENT_JOBS = id("jobs.persistent");
    public static final ProductRequirementId RECOVERABLE_JOBS = id("jobs.recoverable");
    public static final ProductRequirementId LONG_RUNNING_OPERATIONS = id("operations.long-running");

    private ProductRequirements() { }

    private static ProductRequirementId id(String value) {
        return new ProductRequirementId(value);
    }
}
