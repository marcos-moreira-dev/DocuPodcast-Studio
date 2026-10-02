package com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow;

/**
 * User-facing document audio intents shared by every shell entry point.
 *
 * <p>Listening is latency-sensitive: it audits and fills acoustic coverage from
 * the narration that is already available, but never blocks on document-wide
 * semantic image interpretation. Explicit processing actions remain responsible
 * for preparing authorized secondary content before producing its audio.</p>
 */
public enum DocumentAudioAction {
    FAST_LISTEN(false, false),
    PLAY_SELECTION(false, false),
    PROCESS_FRAGMENT(true, true),
    PROCESS_INTERVAL(true, true),
    PROCESS_COMPLETE(true, false),
    GENERATE_ALL(true, true),
    GENERATE_SELECTION(true, true);

    private final boolean wordSemanticPreparationRequired;
    private final boolean forceGeneration;

    DocumentAudioAction(boolean wordSemanticPreparationRequired, boolean forceGeneration) {
        this.wordSemanticPreparationRequired = wordSemanticPreparationRequired;
        this.forceGeneration = forceGeneration;
    }

    public boolean requiresWordSemanticPreparation() {
        return wordSemanticPreparationRequired;
    }

    public boolean forceGeneration() {
        return forceGeneration;
    }
}
