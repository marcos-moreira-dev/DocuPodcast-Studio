package com.marcosmoreiradev.docupodcaststudio.domain.theatre.plan;

public record ScenePlan(
        String name,
        String notes,
        int textStartIndex,
        int textEndIndex,
        String spatialMap,
        String stageBackdrop,
        String id) {

    public ScenePlan(String name, String notes) {
        this(name, notes, 0, 0, "", "", "");
    }

    public ScenePlan(String name, String notes, int textStartIndex, int textEndIndex, String spatialMap) {
        this(name, notes, textStartIndex, textEndIndex, spatialMap, "", "");
    }

    public ScenePlan(String name, String notes, int textStartIndex, int textEndIndex,
                     String spatialMap, String stageBackdrop) {
        this(name, notes, textStartIndex, textEndIndex, spatialMap, stageBackdrop, "");
    }

    public ScenePlan {
        name = name == null ? "" : name.strip();
        notes = notes == null ? "" : notes.strip();
        spatialMap = spatialMap == null ? "" : spatialMap.strip();
        stageBackdrop = stageBackdrop == null ? "" : stageBackdrop.strip();
        id = id == null ? "" : id.strip();
    }
}
