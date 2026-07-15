package com.marcosmoreiradev.docupodcaststudio.domain.theatre.plan;

import java.util.List;

public record ImportPlan(
        String title,
        List<ActPlan> acts,
        List<ProfilePlan> characters,
        List<ProfilePlan> objects,
        List<String> mediaLinks,
        List<InterventionPlan> interventions,
        String voiceCatalog,
        String toneCatalog) {
}
