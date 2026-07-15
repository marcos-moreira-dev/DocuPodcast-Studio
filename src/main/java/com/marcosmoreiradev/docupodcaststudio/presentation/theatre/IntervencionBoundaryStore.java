package com.marcosmoreiradev.docupodcaststudio.presentation.theatre;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;

import java.util.LinkedHashMap;
import java.util.Map;

/** Shared intervention boundaries for theatre scene modules. */
public final class IntervencionBoundaryStore {
    private final Map<String, SceneBoundary> boundaries = new LinkedHashMap<>();
    private final IntegerProperty revision = new SimpleIntegerProperty(0);

    public ReadOnlyIntegerProperty revisionProperty() {
        return revision;
    }

    public SceneBoundary limite(String sceneId) {
        return boundaries.getOrDefault(normalize(sceneId), SceneBoundary.empty());
    }

    public Map<String, SceneBoundary> snapshot() {
        return Map.copyOf(boundaries);
    }

    public void replaceAll(Map<String, SceneBoundary> nextBoundaries) {
        boundaries.clear();
        if (nextBoundaries != null) {
            for (Map.Entry<String, SceneBoundary> entry : nextBoundaries.entrySet()) {
                String sceneId = normalize(entry.getKey());
                SceneBoundary boundary = entry.getValue();
                if (!sceneId.isBlank() && boundary != null) {
                    boundaries.put(sceneId, new SceneBoundary(
                            normalize(boundary.startId()),
                            normalize(boundary.endId())));
                }
            }
        }
        revision.set(revision.get() + 1);
    }

    public void clear() {
        if (boundaries.isEmpty()) {
            return;
        }
        boundaries.clear();
        revision.set(revision.get() + 1);
    }

    public void setInicio(String sceneId, String startId) {
        update(sceneId, normalize(startId), limite(sceneId).endId());
    }

    public void setFin(String sceneId, String endId) {
        update(sceneId, limite(sceneId).startId(), normalize(endId));
    }

    private void update(String sceneId, String startId, String endId) {
        String id = normalize(sceneId);
        if (id.isBlank()) {
            return;
        }
        boundaries.put(id, new SceneBoundary(startId, endId));
        revision.set(revision.get() + 1);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }

    public record SceneBoundary(String startId, String endId) {
        static SceneBoundary empty() {
            return new SceneBoundary("", "");
        }
    }
}
