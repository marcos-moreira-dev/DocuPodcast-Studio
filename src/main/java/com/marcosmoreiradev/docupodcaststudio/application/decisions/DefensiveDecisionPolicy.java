package com.marcosmoreiradev.docupodcaststudio.application.decisions;

/** Central policy for defensive choices that must not remain silent. */
public final class DefensiveDecisionPolicy {
    private DefensiveDecisionPolicy() {
    }

    /**
     * A fallback must be shown when DocuPodcast changes an explicit user intention:
     * GPU to CPU, advanced voice to another engine, requested tone to Neutral, or a
     * complete export to a partial/omitted result.
     */
    public static UserVisibleDecision fallbackChangedUserIntent(String headline,
                                                                String message,
                                                                String technicalDetail) {
        return UserVisibleDecision.defensiveFallback(headline, message, technicalDetail);
    }
}
