package com.marcosmoreiradev.docupodcaststudio.presentation.ink.input;

public record InkInputSample(
        double x,
        double y,
        long nanos,
        double pressure,
        InkInputCursor cursor,
        boolean primaryButtonDown,
        boolean eraserButton,
        double rawPressure,
        String inputSource) {

    public InkInputSample(double x,
                          double y,
                          long nanos,
                          double pressure,
                          InkInputCursor cursor,
                          boolean primaryButtonDown,
                          boolean eraserButton) {
        this(x, y, nanos, pressure, cursor, primaryButtonDown, eraserButton, pressure, "");
    }

    public InkInputSample {
        x = Double.isFinite(x) ? x : 0.0;
        y = Double.isFinite(y) ? y : 0.0;
        pressure = Double.isFinite(pressure) ? Math.max(0.0, Math.min(1.0, pressure)) : 1.0;
        cursor = cursor == null ? InkInputCursor.UNKNOWN : cursor;
        rawPressure = Double.isFinite(rawPressure) ? rawPressure : pressure;
        inputSource = inputSource == null ? "" : inputSource.strip();
    }

    public boolean requestsEraser() {
        return eraserButton || cursor == InkInputCursor.ERASER;
    }
}
