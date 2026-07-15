package com.marcosmoreiradev.docupodcaststudio.application.ink;

/** Tool semantics for reusable ink strokes. */
public enum InkTool {
    DRAW,
    ERASE;

    public static InkTool fromToken(String token) {
        if (token == null || token.isBlank()) {
            return DRAW;
        }
        try {
            return InkTool.valueOf(token.strip().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return DRAW;
        }
    }
}
