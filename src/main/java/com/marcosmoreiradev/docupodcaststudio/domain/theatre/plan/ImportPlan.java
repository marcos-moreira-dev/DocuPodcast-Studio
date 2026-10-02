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
        String toneCatalog,
        String grammarVersion) {
    public ImportPlan {
        grammarVersion = grammarVersion == null || grammarVersion.isBlank() ? "theatre-v1" : grammarVersion.strip();
    }

    public ImportPlan(String title, List<ActPlan> acts, List<ProfilePlan> characters, List<ProfilePlan> objects,
                      List<String> mediaLinks, List<InterventionPlan> interventions,
                      String voiceCatalog, String toneCatalog) {
        this(title, acts, characters, objects, mediaLinks, interventions, voiceCatalog, toneCatalog, "theatre-v1");
    }
}
