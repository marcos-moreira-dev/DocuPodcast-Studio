package com.marcosmoreiradev.docupodcaststudio.application.theatrepackage;

import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer;
import com.marcosmoreiradev.docupodcaststudio.domain.theatrepackage.TheatrePackageAssetKind;
import com.marcosmoreiradev.docupodcaststudio.domain.theatrepackage.TheatrePackageDelta;
import com.marcosmoreiradev.docupodcaststudio.domain.theatrepackage.TheatrePackageDeltaStatus;
import com.marcosmoreiradev.docupodcaststudio.domain.theatrepackage.TheatrePackageEntry;
import com.marcosmoreiradev.docupodcaststudio.domain.theatrepackage.TheatreRefreshPlan;

import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/** Pure validation of a theatre refresh plan plus filesystem capacity checks. */
public final class PreflightTheatreRefreshUseCase {
    public TheatreRefreshPreflightReport inspect(DocuPodcastProject project, Path projectFile,
                                                 Path sourceRoot, TheatreRefreshPlan plan) {
        Objects.requireNonNull(project, "project");
        Objects.requireNonNull(plan, "plan");
        ArrayList<TheatreRefreshDiagnostic> diagnostics = new ArrayList<>();
        if (projectFile == null || projectFile.toAbsolutePath().normalize().getParent() == null) {
            diagnostics.add(error("DTP001", "", "Guarda el proyecto antes de vincular o refrescar la obra."));
        }
        if (sourceRoot == null || !Files.isDirectory(sourceRoot)) {
            diagnostics.add(error("DTP002", "", "La carpeta de obra no existe o no es legible."));
        }

        TheatreProjectLayer theatre = project.theatre();
        Set<String> characters = new HashSet<>();
        theatre.characters().forEach(item -> characters.add(item.id()));
        Set<String> objects = new HashSet<>();
        theatre.objects().forEach(item -> objects.add(item.id()));
        Set<String> scenes = new HashSet<>();
        theatre.scenes().forEach(item -> scenes.add(item.id()));
        Set<String> interventions = new HashSet<>();
        theatre.intervenciones().forEach(item -> interventions.add(item.id()));

        long stagedBytes = 0;
        for (TheatrePackageDelta delta : plan.deltas()) {
            if (delta.status() == TheatrePackageDeltaStatus.CONFLICT) {
                diagnostics.add(error("DTP010", delta.logicalId(), delta.reason()));
                continue;
            }
            if (delta.status() == TheatrePackageDeltaStatus.MISSING_SOURCE_RETAINED) {
                diagnostics.add(warning("DTP020", delta.logicalId(),
                        "El archivo falta en el origen; DocuPodcast conservará la copia del proyecto."));
                continue;
            }
            if (delta.status() == TheatrePackageDeltaStatus.UNCHANGED) continue;
            TheatrePackageEntry entry = delta.after();
            stagedBytes += entry.size();
            validateBinding(entry, characters, objects, scenes, interventions, diagnostics);
            if (entry.kind() == TheatrePackageAssetKind.HUMAN_AUDIO) {
                theatre.intervenciones().stream()
                        .filter(item -> item.id().equals(entry.metadata("interventionId")))
                        .findFirst()
                        .ifPresent(intervention -> {
                            boolean replacesPrimary = project.narrativeLayerAssignments().stream()
                                    .anyMatch(layer -> layer.primaryNarrationLayer()
                                            && layer.textRange().segmentId().equals(intervention.blockId()));
                            if (replacesPrimary) diagnostics.add(warning("DTP121", entry.logicalId(),
                                    "El audio humano reemplazará la voz o audio principal de esa intervención."));
                        });
            }
        }

        if (projectFile != null && projectFile.toAbsolutePath().normalize().getParent() != null) {
            try {
                Path root = projectFile.toAbsolutePath().normalize().getParent();
                Files.createDirectories(root);
                FileStore store = Files.getFileStore(root);
                long required = Math.max(stagedBytes * 2L, stagedBytes + 8L * 1024L * 1024L);
                if (store.getUsableSpace() < required) {
                    diagnostics.add(error("DTP030", "", "No hay espacio suficiente para staging y rollback."));
                }
            } catch (Exception ex) {
                diagnostics.add(warning("DTP031", "", "No se pudo comprobar el espacio libre: " + ex.getMessage()));
            }
        }
        return new TheatreRefreshPreflightReport(plan, diagnostics);
    }

    private static void validateBinding(TheatrePackageEntry entry, Set<String> characters, Set<String> objects,
                                        Set<String> scenes, Set<String> interventions,
                                        ArrayList<TheatreRefreshDiagnostic> diagnostics) {
        switch (entry.kind()) {
            case CHARACTER_IMAGE -> requireResolved(entry, "characterId", characters, "personaje", "DTP101", diagnostics);
            case OBJECT_IMAGE -> requireResolved(entry, "objectId", objects, "objeto", "DTP102", diagnostics);
            case SPATIAL_MAP -> requireResolved(entry, "sceneId", scenes, "escena", "DTP103", diagnostics);
            case INTERVENTION_IMAGE, HUMAN_AUDIO -> requireResolved(entry, "interventionId", interventions,
                    "intervención", "DTP104", diagnostics);
            case INTERMEDIATE_FRAME -> {
                requireResolved(entry, "fromInterventionId", interventions, "intervención inicial", "DTP105", diagnostics);
                requireResolved(entry, "toInterventionId", interventions, "intervención final", "DTP106", diagnostics);
            }
            case OTHER -> diagnostics.add(warning("DTP120", entry.logicalId(),
                    "El asset se conservará en catálogo, pero no tiene binding teatral automático."));
            default -> { }
        }
    }

    private static void requireResolved(TheatrePackageEntry entry, String metadataKey, Set<String> candidates,
                                        String concept, String code,
                                        ArrayList<TheatreRefreshDiagnostic> diagnostics) {
        String id = entry.metadata(metadataKey);
        if (id.isBlank() || !candidates.contains(id)) {
            diagnostics.add(error(code, entry.logicalId(),
                    "No se pudo resolver " + concept + " '" + id + "' para " + entry.relativePath() + "."));
        }
    }

    private static TheatreRefreshDiagnostic error(String code, String id, String message) {
        return new TheatreRefreshDiagnostic(code, TheatreRefreshDiagnosticSeverity.ERROR, id, message);
    }
    private static TheatreRefreshDiagnostic warning(String code, String id, String message) {
        return new TheatreRefreshDiagnostic(code, TheatreRefreshDiagnosticSeverity.WARNING, id, message);
    }
}
