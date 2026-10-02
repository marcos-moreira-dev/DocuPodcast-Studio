package com.marcosmoreiradev.docupodcaststudio.application.storyboard;

/** One reusable vector drawing stored at project scope, independently of any frame. */
public record TheatreDrawingVaultItem(String id, String name, String inkStateJson) {
    public TheatreDrawingVaultItem {
        id = normalize(id);
        name = normalize(name);
        inkStateJson = inkStateJson == null ? "" : inkStateJson;
        if (id.isBlank()) throw new IllegalArgumentException("drawing vault item id is required");
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }
}
