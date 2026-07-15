package com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow;

import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.ProjectSession;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Owns theatre act and scene list updates for the shell. */
public final class TheatreSceneCoordinator {
    private static final String DEFAULT_ACT_ID = "ACT-ACTO-1";
    private static final String DEFAULT_ACT_NAME = "Acto 1";

    public List<TheatreProjectLayer.TheatreAct> acts(Optional<ProjectSession> session) {
        return session.map(value -> displayActs(value.project().theatre())).orElseGet(List::of);
    }

    public List<TheatreProjectLayer.Scene> scenes(Optional<ProjectSession> session) {
        return session.map(value -> value.project().theatre().scenes()).orElseGet(List::of);
    }

    public SaveResult addAct(Optional<ProjectSession> maybeSession, String displayName, String description) {
        if (maybeSession.isEmpty()) {
            return new SaveResult(false, "Abre o crea un proyecto antes de agregar actos.");
        }
        String normalizedName = normalize(displayName);
        if (normalizedName.isBlank()) {
            return new SaveResult(false, "Escribe un nombre para el acto.");
        }

        ProjectSession session = maybeSession.get();
        TheatreProjectLayer theatre = session.project().theatre();
        ArrayList<TheatreProjectLayer.TheatreAct> updatedActs = new ArrayList<>(theatre.acts());
        if (updatedActs.isEmpty() && stableId("ACT", normalizedName).equals(DEFAULT_ACT_ID)) {
            updatedActs.add(new TheatreProjectLayer.TheatreAct(DEFAULT_ACT_ID, normalizedName, normalize(description)));
            DocuPodcastProject project = session.project().withTheatre(replaceActsAndScenes(theatre, updatedActs, theatre.scenes()));
            session.replaceProject(project, true);
            return new SaveResult(true, "Acto agregado: " + normalizedName + ".");
        }
        if (updatedActs.isEmpty() && !stableId("ACT", normalizedName).equals(DEFAULT_ACT_ID)) {
            updatedActs.add(defaultAct());
        }
        updatedActs.add(new TheatreProjectLayer.TheatreAct(
                uniqueActId(displayActs(theatre), normalizedName),
                normalizedName,
                normalize(description)));

        DocuPodcastProject project = session.project().withTheatre(replaceActsAndScenes(theatre, updatedActs, theatre.scenes()));
        session.replaceProject(project, true);
        return new SaveResult(true, "Acto agregado: " + normalizedName + ".");
    }

    public SaveResult updateAct(Optional<ProjectSession> maybeSession, String actId, String displayName, String description) {
        if (maybeSession.isEmpty()) {
            return new SaveResult(false, "Abre o crea un proyecto antes de editar actos.");
        }
        String normalizedId = normalize(actId);
        String normalizedName = normalize(displayName);
        if (normalizedId.isBlank() || normalizedName.isBlank()) {
            return new SaveResult(false, "Selecciona un acto y escribe un nombre.");
        }

        ProjectSession session = maybeSession.get();
        TheatreProjectLayer theatre = session.project().theatre();
        if (!containsActId(displayActs(theatre), normalizedId)) {
            return new SaveResult(false, "El acto seleccionado ya no existe.");
        }

        ArrayList<TheatreProjectLayer.TheatreAct> updatedActs = new ArrayList<>();
        boolean found = false;
        for (TheatreProjectLayer.TheatreAct act : theatre.acts()) {
            if (act.id().equals(normalizedId)) {
                updatedActs.add(new TheatreProjectLayer.TheatreAct(act.id(), normalizedName, normalize(description)));
                found = true;
            } else {
                updatedActs.add(act);
            }
        }
        if (!found && normalizedId.equals(DEFAULT_ACT_ID)) {
            updatedActs.add(0, new TheatreProjectLayer.TheatreAct(DEFAULT_ACT_ID, normalizedName, normalize(description)));
        }
        session.replaceProject(session.project().withTheatre(replaceActsAndScenes(theatre, updatedActs, theatre.scenes())), true);
        return new SaveResult(true, "Acto actualizado: " + normalizedName + ".");
    }

    public SaveResult addScene(Optional<ProjectSession> maybeSession, String actId, String displayName, String description) {
        if (maybeSession.isEmpty()) {
            return new SaveResult(false, "Abre o crea un proyecto antes de agregar escenas.");
        }
        String normalizedActId = normalize(actId);
        String normalizedName = normalize(displayName);
        if (normalizedActId.isBlank()) {
            return new SaveResult(false, "Selecciona un acto para agregar la escena.");
        }
        if (normalizedName.isBlank()) {
            return new SaveResult(false, "Escribe un nombre para la escena.");
        }

        ProjectSession session = maybeSession.get();
        TheatreProjectLayer theatre = session.project().theatre();
        ArrayList<TheatreProjectLayer.TheatreAct> updatedActs = new ArrayList<>(theatre.acts());
        if (!containsActId(displayActs(theatre), normalizedActId)) {
            return new SaveResult(false, "El acto seleccionado ya no existe.");
        }
        if (!containsActId(updatedActs, normalizedActId) && normalizedActId.equals(DEFAULT_ACT_ID)) {
            updatedActs.add(defaultAct());
        }

        ArrayList<TheatreProjectLayer.Scene> updatedScenes = new ArrayList<>(theatre.scenes());
        updatedScenes.add(new TheatreProjectLayer.Scene(
                uniqueSceneId(theatre.scenes(), normalizedName),
                normalizedName,
                normalize(description),
                normalizedActId));

        DocuPodcastProject project = session.project().withTheatre(replaceActsAndScenes(theatre, updatedActs, updatedScenes));
        session.replaceProject(project, true);
        return new SaveResult(true, "Escena agregada: " + normalizedName + ".");
    }

    public SaveResult updateScene(Optional<ProjectSession> maybeSession, String sceneId, String displayName, String description) {
        if (maybeSession.isEmpty()) {
            return new SaveResult(false, "Abre o crea un proyecto antes de editar escenas.");
        }
        String normalizedId = normalize(sceneId);
        String normalizedName = normalize(displayName);
        if (normalizedId.isBlank() || normalizedName.isBlank()) {
            return new SaveResult(false, "Selecciona una escena y escribe un nombre.");
        }
        ProjectSession session = maybeSession.get();
        TheatreProjectLayer theatre = session.project().theatre();
        ArrayList<TheatreProjectLayer.Scene> updated = new ArrayList<>();
        boolean found = false;
        for (TheatreProjectLayer.Scene scene : theatre.scenes()) {
            if (scene.id().equals(normalizedId)) {
                updated.add(new TheatreProjectLayer.Scene(
                        scene.id(), normalizedName, normalize(description), normalizedActId(scene), scene.spatialMapAssetId()));
                found = true;
            } else {
                updated.add(scene);
            }
        }
        if (!found) {
            return new SaveResult(false, "La escena seleccionada ya no existe.");
        }
        session.replaceProject(session.project().withTheatre(replaceActsAndScenes(theatre, theatre.acts(), updated)), true);
        return new SaveResult(true, "Escena actualizada: " + normalizedName + ".");
    }

    public SaveResult deleteScene(Optional<ProjectSession> maybeSession, String sceneId) {
        if (maybeSession.isEmpty()) {
            return new SaveResult(false, "Abre o crea un proyecto antes de eliminar escenas.");
        }
        String normalizedId = normalize(sceneId);
        if (normalizedId.isBlank()) {
            return new SaveResult(false, "Selecciona una escena para eliminar.");
        }
        ProjectSession session = maybeSession.get();
        TheatreProjectLayer theatre = session.project().theatre();
        List<TheatreProjectLayer.Scene> updated = theatre.scenes().stream()
                .filter(scene -> !scene.id().equals(normalizedId))
                .toList();
        if (updated.size() == theatre.scenes().size()) {
            return new SaveResult(false, "La escena seleccionada ya no existe.");
        }
        session.replaceProject(session.project().withTheatre(replaceActsAndScenes(theatre, theatre.acts(), updated)), true);
        return new SaveResult(true, "Escena eliminada.");
    }

    private static TheatreProjectLayer replaceActsAndScenes(
            TheatreProjectLayer theatre,
            List<TheatreProjectLayer.TheatreAct> acts,
            List<TheatreProjectLayer.Scene> scenes) {
        return new TheatreProjectLayer(
                theatre.intervenciones(),
                theatre.characters(),
                theatre.voiceRoleAliases(),
                theatre.characterImages().stream()
                        .filter(image -> image.sceneId().isBlank() || containsSceneId(scenes, image.sceneId()))
                        .toList(),
                theatre.intervencionesVisuales(),
                theatre.intermediateFrames(),
                acts,
                scenes,
                theatre.positions().stream().filter(position -> containsSceneId(scenes, position.sceneId())).toList(),
                theatre.actions().stream().filter(action -> containsSceneId(scenes, action.sceneId())).toList(),
                theatre.textActionPlacements().stream().filter(placement -> containsSceneId(scenes, placement.sceneId())).toList(),
                theatre.objectImages().stream()
                        .filter(image -> image.sceneId().isBlank() || containsSceneId(scenes, image.sceneId()))
                        .toList(),
                theatre.objects(),
                theatre.audioTracks(),
                theatre.cameraReferences(),
                theatre.cameraCues(),
                theatre.stageBackdrops(),
                theatre.stageBackdropAssignments());
    }

    private static List<TheatreProjectLayer.TheatreAct> displayActs(TheatreProjectLayer theatre) {
        ArrayList<TheatreProjectLayer.TheatreAct> acts = new ArrayList<>(theatre.acts());
        boolean hasLegacyScenes = theatre.scenes().stream().anyMatch(scene -> normalize(scene.actId()).isBlank());
        if ((acts.isEmpty() || hasLegacyScenes) && !containsActId(acts, DEFAULT_ACT_ID)) {
            acts.add(0, defaultAct());
        }
        return List.copyOf(acts);
    }

    private static TheatreProjectLayer.TheatreAct defaultAct() {
        return new TheatreProjectLayer.TheatreAct(DEFAULT_ACT_ID, DEFAULT_ACT_NAME, "");
    }

    private static String normalizedActId(TheatreProjectLayer.Scene scene) {
        String actId = normalize(scene.actId());
        return actId.isBlank() ? DEFAULT_ACT_ID : actId;
    }

    private static String uniqueActId(List<TheatreProjectLayer.TheatreAct> acts, String displayName) {
        String base = stableId("ACT", displayName);
        String candidate = base;
        int suffix = 2;
        while (containsActId(acts, candidate)) {
            candidate = base + "-" + suffix;
            suffix++;
        }
        return candidate;
    }

    private static String uniqueSceneId(List<TheatreProjectLayer.Scene> scenes, String displayName) {
        String base = stableId("ESC", displayName);
        String candidate = base;
        int suffix = 2;
        while (containsSceneId(scenes, candidate)) {
            candidate = base + "-" + suffix;
            suffix++;
        }
        return candidate;
    }

    private static boolean containsActId(List<TheatreProjectLayer.TheatreAct> acts, String candidate) {
        return acts.stream().anyMatch(act -> act.id().equals(candidate));
    }

    private static boolean containsSceneId(List<TheatreProjectLayer.Scene> scenes, String candidate) {
        return scenes.stream().anyMatch(scene -> scene.id().equals(candidate));
    }

    private static String stableId(String prefix, String displayName) {
        String ascii = Normalizer.normalize(displayName == null ? "" : displayName.strip(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        return ascii.isBlank() ? prefix + "-SIN-NOMBRE" : prefix + "-" + ascii;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }

    public record SaveResult(boolean saved, String message) {
    }
}
