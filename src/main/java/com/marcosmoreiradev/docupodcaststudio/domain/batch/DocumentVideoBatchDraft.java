package com.marcosmoreiradev.docupodcaststudio.domain.batch;

/** Editable Express setup saved before a production queue is created. */
public record DocumentVideoBatchDraft(
        String title,
        String sourceRoot,
        String destinationRoot,
        DocumentVideoBatchProfile profile
) {
    public DocumentVideoBatchDraft {
        title = clean(title);
        sourceRoot = clean(sourceRoot);
        destinationRoot = clean(destinationRoot);
        profile = profile == null ? DocumentVideoBatchProfile.defaults() : profile;
    }

    private static String clean(String value) {
        return value == null ? "" : value.strip();
    }
}
