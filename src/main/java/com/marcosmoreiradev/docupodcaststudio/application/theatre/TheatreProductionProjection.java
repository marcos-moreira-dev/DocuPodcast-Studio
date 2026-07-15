package com.marcosmoreiradev.docupodcaststudio.application.theatre;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Derived production view for the theatre mode. */
public record TheatreProductionProjection(
        String title,
        List<TheatreProductionScene> scenes,
        List<TheatreProductionIntervention> interventions,
        TheatreProductionReadiness readiness,
        List<String> diagnostics
) {
    public TheatreProductionProjection {
        title = title == null || title.isBlank() ? "Produccion teatral" : title.strip();
        scenes = scenes == null ? List.of() : List.copyOf(scenes);
        interventions = interventions == null ? List.of() : List.copyOf(interventions);
        readiness = readiness == null ? emptyReadiness() : readiness;
        diagnostics = diagnostics == null ? List.of() : List.copyOf(diagnostics);
    }

    public Optional<TheatreProductionIntervention> interventionById(String interventionId) {
        String target = normalize(interventionId);
        if (target.isBlank()) {
            return Optional.empty();
        }
        return interventions.stream()
                .filter(intervention -> intervention.interventionId().equals(target))
                .findFirst();
    }

    public Optional<TheatreProductionIntervention> interventionForBlock(String blockId) {
        String target = normalize(blockId);
        if (target.isBlank()) {
            return Optional.empty();
        }
        return interventions.stream()
                .filter(intervention -> intervention.blockId().equals(target))
                .min(Comparator.comparingInt(TheatreProductionIntervention::sequenceIndex));
    }

    public Optional<TheatreProductionScene> sceneById(String sceneId) {
        String target = normalize(sceneId);
        if (target.isBlank()) {
            return Optional.empty();
        }
        return scenes.stream().filter(scene -> scene.sceneId().equals(target)).findFirst();
    }

    private static TheatreProductionReadiness emptyReadiness() {
        return new TheatreProductionReadiness(
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                List.of("Importa o prepara una obra teatral con intervenciones."),
                List.of("Importa o prepara una obra teatral con mapa textual o espacial."),
                List.of("Importa o prepara actos o escenas teatrales."),
                List.of());
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }
}
