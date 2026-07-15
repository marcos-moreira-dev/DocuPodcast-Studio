package com.marcosmoreiradev.docupodcaststudio.presentation.document.ink.input;

public record InkInputSample(
        double x,
        double y,
        long nanos,
        double pressure,
        InkInputCursor cursor,
        boolean primaryButtonDown,
        boolean eraserButton) {

    public InkInputSample {
        pressure = Double.isFinite(pressure) ? Math.max(0.0, Math.min(1.0, pressure)) : 1.0;
        cursor = cursor == null ? InkInputCursor.UNKNOWN : cursor;
    }

    public boolean requestsEraser() {
        return eraserButton || cursor == InkInputCursor.ERASER;
    }
}
