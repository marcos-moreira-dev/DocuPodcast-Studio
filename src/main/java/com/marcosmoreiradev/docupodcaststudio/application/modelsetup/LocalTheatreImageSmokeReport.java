package com.marcosmoreiradev.docupodcaststudio.application.modelsetup;

/** Result of a local theatre image engine smoke test. */
public record LocalTheatreImageSmokeReport(boolean success, String userMessage) {
    public LocalTheatreImageSmokeReport {
        userMessage = userMessage == null ? "" : userMessage.strip();
    }
}
