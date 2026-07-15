package com.marcosmoreiradev.docupodcaststudio.application.theatre;

/** Scope for theatre AI frame generation. */
public record TheatreFrameGenerationScope(Kind kind, String id) {
    public TheatreFrameGenerationScope {
        kind = kind == null ? Kind.ALL : kind;
        id = id == null ? "" : id.strip();
    }

    public static TheatreFrameGenerationScope all() {
        return new TheatreFrameGenerationScope(Kind.ALL, "");
    }

    public static TheatreFrameGenerationScope act(String actId) {
        return new TheatreFrameGenerationScope(Kind.ACT, actId);
    }

    public static TheatreFrameGenerationScope scene(String sceneId) {
        return new TheatreFrameGenerationScope(Kind.SCENE, sceneId);
    }

    public static TheatreFrameGenerationScope intervention(String interventionId) {
        return new TheatreFrameGenerationScope(Kind.INTERVENTION, interventionId);
    }

    public boolean isAll() {
        return kind == Kind.ALL || id.isBlank();
    }

    public TheatreContextExportScope asContextScope() {
        return switch (kind) {
            case ACT -> TheatreContextExportScope.act(id);
            case SCENE -> TheatreContextExportScope.scene(id);
            case INTERVENTION -> TheatreContextExportScope.intervention(id);
            case ALL -> TheatreContextExportScope.all();
        };
    }

    public enum Kind { ALL, ACT, SCENE, INTERVENTION }
}
