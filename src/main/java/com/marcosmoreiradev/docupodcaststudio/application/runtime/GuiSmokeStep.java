package com.marcosmoreiradev.docupodcaststudio.application.runtime;

/** One user-visible step in the assisted GUI smoke checklist. */
public record GuiSmokeStep(
        String area,
        String expectedAction,
        String status,
        String detail
) {
    public GuiSmokeStep {
        area = normalize(area);
        expectedAction = normalize(expectedAction);
        status = normalize(status).isBlank() ? "PENDING" : normalize(status);
        detail = normalize(detail);
    }

    public boolean readyOrManual() {
        return "READY".equals(status) || "MANUAL".equals(status);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }
}
