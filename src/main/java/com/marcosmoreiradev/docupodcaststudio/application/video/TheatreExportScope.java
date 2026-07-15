package com.marcosmoreiradev.docupodcaststudio.application.video;

/** Scope for exporting a theatre map or final video by full work, act or scene. */
public record TheatreExportScope(Kind kind, String id) {
    public TheatreExportScope {
        kind = kind == null ? Kind.ALL : kind;
        id = id == null ? "" : id.strip();
    }

    public static TheatreExportScope all() {
        return new TheatreExportScope(Kind.ALL, "");
    }

    public static TheatreExportScope act(String actId) {
        return new TheatreExportScope(Kind.ACT, actId);
    }

    public static TheatreExportScope scene(String sceneId) {
        return new TheatreExportScope(Kind.SCENE, sceneId);
    }

    public boolean isAll() {
        return kind == Kind.ALL || id.isBlank();
    }

    public enum Kind {
        ALL,
        ACT,
        SCENE
    }
}
