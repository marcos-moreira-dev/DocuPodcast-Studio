package com.marcosmoreiradev.docupodcaststudio.domain.theatre.plan;

import java.util.List;

public record ActPlan(String name, String notes, List<ScenePlan> scenes) {
}
