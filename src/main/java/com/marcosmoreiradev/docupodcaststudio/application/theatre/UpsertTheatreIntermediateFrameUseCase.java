package com.marcosmoreiradev.docupodcaststudio.application.theatre;

import com.marcosmoreiradev.docupodcaststudio.domain.assets.ProjectAssetReference;
import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/** Stores the inferred visual that bridges two adjacent theatre interventions. */
public final class UpsertTheatreIntermediateFrameUseCase {
    public DocuPodcastProject upsert(DocuPodcastProject project,
                                     String fromIntervencionId,
                                     String toIntervencionId,
                                     String assetId,
                                     String notes) {
        Objects.requireNonNull(project, "project");
        String from = require(fromIntervencionId, "fromIntervencionId");
        String to = require(toIntervencionId, "toIntervencionId");
        String asset = require(assetId, "assetId");
        if (from.equals(to)) {
            throw new IllegalArgumentException("El frame inferido requiere dos intervenciones distintas.");
        }
        TheatreProjectLayer theatre = project.theatre();
        Set<String> interventions = new LinkedHashSet<>();
        for (TheatreProjectLayer.Intervencion intervention : theatre.intervenciones()) {
            interventions.add(intervention.id());
        }
        if (!interventions.contains(from) || !interventions.contains(to)) {
            throw new IllegalArgumentException("El par de intervenciones del frame inferido no existe en la obra.");
        }
        ProjectAssetReference reference = project.assets().byId(asset)
                .filter(ProjectAssetReference::isImage)
                .orElseThrow(() -> new IllegalArgumentException("El frame inferido debe apuntar a un asset de imagen existente."));

        ArrayList<TheatreProjectLayer.IntermediateFrame> updated = new ArrayList<>();
        for (TheatreProjectLayer.IntermediateFrame existing : theatre.intermediateFrames()) {
            if (!existing.fromIntervencionId().equals(from) || !existing.toIntervencionId().equals(to)) {
                updated.add(existing);
            }
        }
        updated.add(new TheatreProjectLayer.IntermediateFrame(from, to, reference.id(), notes));
        return project.withTheatre(theatre.withIntermediateFrames(updated));
    }

    private static String require(String value, String field) {
        String normalized = value == null ? "" : value.strip();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return normalized;
    }
}
