package com.marcosmoreiradev.docupodcaststudio.ink.input;

/** Live, provider-neutral diagnostic state for every ink editor shell. */
public record InkInputStatus(
        String providerName,
        String activeSource,
        InkInputCursor cursor,
        double rawPressure,
        double pressure,
        boolean pressureVariable,
        boolean nativeActive,
        String fallbackReason) {

    public InkInputStatus {
        providerName = providerName == null || providerName.isBlank() ? "Unknown input" : providerName;
        activeSource = activeSource == null ? "" : activeSource.strip();
        cursor = cursor == null ? InkInputCursor.UNKNOWN : cursor;
        pressure = Double.isFinite(pressure) ? Math.max(0.0, Math.min(1.0, pressure)) : 1.0;
        fallbackReason = fallbackReason == null ? "" : fallbackReason;
    }

    public static InkInputStatus idle(InkInputCapabilities capabilities) {
        InkInputCapabilities safe = capabilities == null ? InkInputCapabilities.javafxMouse() : capabilities;
        return new InkInputStatus(safe.providerName(), "", InkInputCursor.UNKNOWN, Double.NaN, 1.0,
                false, safe.nativeProvider(), safe.fallbackReason());
    }
}
