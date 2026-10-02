package com.marcosmoreiradev.docupodcaststudio.ink.input;

public record InkInputCapabilities(
        String providerName,
        boolean pressure,
        boolean tilt,
        boolean eraserCursor,
        boolean nativeProvider,
        String fallbackReason) {

    public InkInputCapabilities {
        providerName = providerName == null || providerName.isBlank() ? "Unknown input" : providerName;
        fallbackReason = fallbackReason == null ? "" : fallbackReason;
    }

    public static InkInputCapabilities javafxMouse() {
        return new InkInputCapabilities("JavaFX mouse", false, false, false, false, "");
    }

    public static InkInputCapabilities javafxMouse(String reason) {
        return new InkInputCapabilities("JavaFX mouse", false, false, false, false, reason);
    }

    public static InkInputCapabilities nativeStylus(String providerName,
                                                    boolean pressure,
                                                    boolean tilt,
                                                    boolean eraserCursor) {
        return new InkInputCapabilities(providerName, pressure, tilt, eraserCursor, true, "");
    }

    public static InkInputCapabilities windowsPointer() {
        return nativeStylus("Windows Pointer", true, false, true);
    }

    public static InkInputCapabilities windowsPointerWaiting(String reason) {
        return new InkInputCapabilities(
                "Windows Pointer (esperando eventos)",
                false,
                false,
                false,
                false,
                reason);
    }

    public static InkInputCapabilities lectureStudioStylus() {
        return nativeStylus("LectureStudio stylus", true, true, true);
    }

    public static InkInputCapabilities lectureStudioWaiting(String reason) {
        return new InkInputCapabilities("LectureStudio stylus (esperando eventos)",
                false, false, false, false, reason);
    }

    public static InkInputCapabilities lectureStudioUnavailable(String reason) {
        return new InkInputCapabilities("LectureStudio stylus (no disponible)",
                false, false, false, false, reason);
    }

    public static InkInputCapabilities lectureStudioFallback(String reason) {
        String detail = reason == null || reason.isBlank()
                ? "LectureStudio detectado sin puente de eventos nativos."
                : "LectureStudio detectado sin puente de eventos nativos. " + reason;
        return lectureStudioWaiting(detail);
    }
}
