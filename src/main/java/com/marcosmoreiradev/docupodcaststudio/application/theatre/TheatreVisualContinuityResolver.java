package com.marcosmoreiradev.docupodcaststudio.application.theatre;

import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer;

/** Decides whether two theatre fragments can use an interpolated frame. */
public final class TheatreVisualContinuityResolver {
    private final TheatreCameraReferenceResolver cameraResolver = new TheatreCameraReferenceResolver();

    public boolean canInterpolate(TheatreProjectLayer theatre, String fromInterventionId, String toInterventionId) {
        if (fromInterventionId == null || toInterventionId == null
                || fromInterventionId.isBlank() || toInterventionId.isBlank()
                || fromInterventionId.equals(toInterventionId)) {
            return false;
        }
        return cameraResolver.sameEffectiveCamera(theatre, fromInterventionId, toInterventionId);
    }
}
