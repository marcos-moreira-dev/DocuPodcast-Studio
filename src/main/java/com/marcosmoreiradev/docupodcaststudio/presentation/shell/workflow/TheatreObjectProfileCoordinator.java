package com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow;

import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.ProjectSession;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Owns persisted theatre object profile updates for the shell. */
public final class TheatreObjectProfileCoordinator {
    public List<TheatreProjectLayer.TheatreObject> objects(Optional<ProjectSession> session) {
        return session.map(value -> value.project().theatre().objects()).orElseGet(List::of);
    }

    public SaveResult save(Optional<ProjectSession> maybeSession, String objectId, String displayName, String description) {
        if (maybeSession.isEmpty()) {
            return new SaveResult(false, "Abre o crea un proyecto antes de guardar fichas de objeto.");
        }
        String normalizedName = normalizeDisplayName(displayName);
        if (normalizedName.isBlank()) {
            return new SaveResult(false, "Escribe un nombre para el objeto.");
        }
        String normalizedId = normalizeId(objectId, normalizedName);
        String normalizedDescription = description == null ? "" : description.strip();

        ProjectSession session = maybeSession.get();
        TheatreProjectLayer theatre = session.project().theatre();
        ArrayList<TheatreProjectLayer.TheatreObject> updated = new ArrayList<>();
        boolean replaced = false;
        for (TheatreProjectLayer.TheatreObject existing : theatre.objects()) {
            if (existing.id().equals(normalizedId) || sameName(existing.displayName(), normalizedName)) {
                updated.add(new TheatreProjectLayer.TheatreObject(existing.id(), normalizedName, normalizedDescription));
                normalizedId = existing.id();
                replaced = true;
            } else {
                updated.add(existing);
            }
        }
        if (!replaced) {
            updated.add(new TheatreProjectLayer.TheatreObject(normalizedId, normalizedName, normalizedDescription));
        }

        DocuPodcastProject project = session.project().withTheatre(replaceObjects(theatre, updated));
        session.replaceProject(project, true);
        String suffix = persistProfileSidecar(session, normalizedId, normalizedName, normalizedDescription);
        return new SaveResult(true, "Ficha de objeto actualizada: " + normalizedName + suffix);
    }

    private static TheatreProjectLayer replaceObjects(
            TheatreProjectLayer theatre,
            List<TheatreProjectLayer.TheatreObject> objects) {
        return new TheatreProjectLayer(
                theatre.intervenciones(),
                theatre.characters(),
                theatre.voiceRoleAliases(),
                theatre.characterImages(),
                theatre.intervencionesVisuales(),
                theatre.intermediateFrames(),
                theatre.acts(),
                theatre.scenes(),
                theatre.positions(),
                theatre.actions(),
                theatre.textActionPlacements(),
                theatre.objectImages(),
                objects,
                theatre.audioTracks(),
                theatre.cameraReferences(),
                theatre.cameraCues(),
                theatre.stageBackdrops(),
                theatre.stageBackdropAssignments());
    }

    private static String normalizeDisplayName(String displayName) {
        return displayName == null ? "" : displayName.strip();
    }

    private static String normalizeId(String objectId, String displayName) {
        String normalized = objectId == null ? "" : objectId.strip();
        if (!normalized.isBlank()) {
            return normalized;
        }
        String ascii = Normalizer.normalize(normalizeDisplayName(displayName), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase(java.util.Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        return ascii.isBlank() ? "OBJ-SIN-NOMBRE" : "OBJ-" + ascii;
    }

    private static boolean sameName(String left, String right) {
        return Normalizer.normalize(normalizeDisplayName(left), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .equalsIgnoreCase(Normalizer.normalize(normalizeDisplayName(right), Normalizer.Form.NFD)
                        .replaceAll("\\p{M}", ""));
    }

    private static String persistProfileSidecar(
            ProjectSession session,
            String objectId,
            String displayName,
            String description) {
        return session.projectFile()
                .map(file -> writeProfileSidecar(file, objectId, displayName, description))
                .orElse(" Guarda el proyecto para crear la carpeta de objetos.");
    }

    private static String writeProfileSidecar(Path projectFile, String objectId, String displayName, String description) {
        try {
            Path projectDirectory = projectFile.toAbsolutePath().normalize().getParent();
            if (projectDirectory == null) {
                return " No se pudo resolver la carpeta del proyecto para la ficha TXT.";
            }
            Path objectDirectory = projectDirectory
                    .resolve("objetos")
                    .resolve(safeFolderName(objectId));
            Files.createDirectories(objectDirectory.resolve("imagenes"));
            Files.writeString(
                    objectDirectory.resolve("ficha-tecnica.txt"),
                    profileText(objectId, displayName, description),
                    StandardCharsets.UTF_8);
            return " Carpeta de objeto actualizada: " + projectDirectory.relativize(objectDirectory) + ".";
        } catch (IOException | RuntimeException ex) {
            return " No se pudo escribir la ficha TXT: " + safeMessage(ex) + ".";
        }
    }

    private static String profileText(String objectId, String displayName, String description) {
        String body = description == null || description.isBlank() ? "Sin ficha tecnica escrita." : description.strip();
        return "Ficha tecnica de " + normalizeDisplayName(displayName) + System.lineSeparator()
                + System.lineSeparator()
                + "Nombre: " + normalizeDisplayName(displayName) + System.lineSeparator()
                + "Id: " + objectId + System.lineSeparator()
                + System.lineSeparator()
                + body + System.lineSeparator();
    }

    private static String safeFolderName(String value) {
        String normalized = value == null ? "" : value.strip();
        if (normalized.isBlank()) {
            return "OBJ-SIN-NOMBRE";
        }
        return normalized.replaceAll("[^A-Za-z0-9._-]+", "-").replaceAll("^-+|-+$", "");
    }

    private static String safeMessage(Exception ex) {
        String message = ex == null ? "" : ex.getMessage();
        return message == null || message.isBlank() ? "error desconocido" : message;
    }

    public record SaveResult(boolean saved, String message) {
    }
}
