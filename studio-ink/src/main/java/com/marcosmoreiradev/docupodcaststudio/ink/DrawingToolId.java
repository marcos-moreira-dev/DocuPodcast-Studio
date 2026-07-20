package com.marcosmoreiradev.docupodcaststudio.ink;

import java.util.Locale;

public record DrawingToolId(String value) {
    public static final DrawingToolId PEN = new DrawingToolId("pen");
    public static final DrawingToolId ERASER = new DrawingToolId("eraser");
    public static final DrawingToolId IMAGE = new DrawingToolId("image");
    public static final DrawingToolId CROP = new DrawingToolId("crop");
    public static final DrawingToolId PAN = new DrawingToolId("pan");

    public DrawingToolId {
        value = value == null ? "" : value.strip().toLowerCase(Locale.ROOT);
        if (value.isBlank()) throw new IllegalArgumentException("drawing tool id is required");
    }
}
