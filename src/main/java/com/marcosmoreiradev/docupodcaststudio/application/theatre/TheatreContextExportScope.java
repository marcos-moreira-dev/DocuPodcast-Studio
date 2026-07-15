package com.marcosmoreiradev.docupodcaststudio.application.theatre;

/** Scope for theatre AI context package export and local image generation queues. */
public record TheatreContextExportScope(Kind kind, String id) {
    public TheatreContextExportScope {
        kind = kind == null ? Kind.ALL : kind;
        id = id == null ? "" : id.strip();
    }

    public static TheatreContextExportScope all() { return new TheatreContextExportScope(Kind.ALL, ""); }
    public static TheatreContextExportScope act(String actId) { return new TheatreContextExportScope(Kind.ACT, actId); }
    public static TheatreContextExportScope scene(String sceneId) { return new TheatreContextExportScope(Kind.SCENE, sceneId); }
    public static TheatreContextExportScope intervention(String interventionId) { return new TheatreContextExportScope(Kind.INTERVENTION, interventionId); }
    public boolean isAll() { return kind == Kind.ALL || id.isBlank(); }

    public enum Kind { ALL, ACT, SCENE, INTERVENTION }
}
