package com.marcosmoreiradev.docupodcaststudio.domain.theatre.plan;

import java.util.List;

public record ActPlan(String name, String notes, List<ScenePlan> scenes, String id) {
    public ActPlan {
        name = name == null ? "" : name.strip(); notes = notes == null ? "" : notes.strip();
        scenes = scenes == null ? List.of() : List.copyOf(scenes); id = id == null ? "" : id.strip();
    }
    public ActPlan(String name, String notes, List<ScenePlan> scenes) { this(name, notes, scenes, ""); }
}
