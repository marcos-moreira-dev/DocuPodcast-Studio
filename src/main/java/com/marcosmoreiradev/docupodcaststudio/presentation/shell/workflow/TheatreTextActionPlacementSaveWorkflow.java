package com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow;

import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.ProjectSession;

import java.util.ArrayList;
import java.util.Objects;

/** Persists the editable spatial/action placement for a theatre intervention. */
public final class TheatreTextActionPlacementSaveWorkflow {
    public void save(ProjectSession session, TheatreProjectLayer.TextActionPlacement placement) {
        Objects.requireNonNull(session, "session");
        Objects.requireNonNull(placement, "placement");
        TheatreProjectLayer theatre = session.project().theatre();
        ArrayList<TheatreProjectLayer.TextActionPlacement> updated = new ArrayList<>();
        boolean replaced = false;
        for (TheatreProjectLayer.TextActionPlacement existing : theatre.textActionPlacements()) {
            if (existing.sceneId().equals(placement.sceneId()) && existing.intervencionId().equals(placement.intervencionId())) {
                updated.add(placement);
                replaced = true;
            } else {
                updated.add(existing);
            }
        }
        if (!replaced) {
            updated.add(placement);
        }
        session.replaceProject(session.project().withTheatre(theatre.withTextActionPlacements(updated)), true);
    }
}
