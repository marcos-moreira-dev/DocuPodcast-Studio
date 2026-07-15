package com.marcosmoreiradev.docupodcaststudio.presentation.shell.workflow;

import com.marcosmoreiradev.docupodcaststudio.domain.project.DocuPodcastProject;
import com.marcosmoreiradev.docupodcaststudio.domain.theatre.TheatreProjectLayer;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceLibrary;
import com.marcosmoreiradev.docupodcaststudio.domain.voice.VoiceProfile;
import com.marcosmoreiradev.docupodcaststudio.presentation.shell.ProjectSession;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Owns persisted theatre character profile updates for the shell. */
public final class TheatreCharacterProfileCoordinator {
    public List<TheatreProjectLayer.CharacterProfile> profiles(Optional<ProjectSession> session) {
        return session.map(value -> value.project().theatre().characters()).orElseGet(List::of);
    }

    public SaveResult save(Optional<ProjectSession> maybeSession, String characterId, String displayName, String description) {
        if (maybeSession.isEmpty()) {
            return new SaveResult(false, "Abre o crea un proyecto antes de guardar fichas de personaje.");
        }
        String normalizedName = normalizeDisplayName(displayName);
        String normalizedId = normalizeId(characterId, normalizedName);
        String normalizedDescription = description == null ? "" : description.strip();

        ProjectSession session = maybeSession.get();
        TheatreProjectLayer theatre = session.project().theatre();
        ArrayList<TheatreProjectLayer.CharacterProfile> updated = new ArrayList<>();
        boolean replaced = false;
        for (TheatreProjectLayer.CharacterProfile existing : theatre.characters()) {
            if (existing.id().equals(normalizedId) || sameName(existing.displayName(), normalizedName)) {
                updated.add(new TheatreProjectLayer.CharacterProfile(existing.id(), normalizedName, existing.aliases(), normalizedDescription));
                normalizedId = existing.id();
                replaced = true;
            } else {
                updated.add(existing);
            }
        }
        if (!replaced) {
            updated.add(new TheatreProjectLayer.CharacterProfile(normalizedId, normalizedName, List.of(), normalizedDescription));
        }

        DocuPodcastProject project = session.project().withTheatre(replaceCharacters(theatre, updated));
        session.replaceProject(project, true);
        String suffix = persistProfileSidecar(session, normalizedId, normalizedName, normalizedDescription);
        return new SaveResult(true, "Ficha de personaje actualizada: " + normalizedName + suffix);
    }

    private static TheatreProjectLayer replaceCharacters(
            TheatreProjectLayer theatre,
            List<TheatreProjectLayer.CharacterProfile> characters) {
        return new TheatreProjectLayer(
                theatre.intervenciones(),
                characters,
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
                theatre.objects(),
                theatre.audioTracks(),
                theatre.cameraReferences(),
                theatre.cameraCues(),
                theatre.stageBackdrops(),
                theatre.stageBackdropAssignments());
    }

    private static String normalizeDisplayName(String displayName) {
        String normalized = displayName == null ? "" : displayName.strip();
        return normalized.isBlank() ? "Personaje sin nombre" : normalized;
    }

    private static String normalizeId(String characterId, String displayName) {
        String normalized = characterId == null ? "" : characterId.strip();
        if (!normalized.isBlank()) {
            return normalized;
        }
        String ascii = Normalizer.normalize(normalizeDisplayName(displayName), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase(java.util.Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        return ascii.isBlank() ? "CHR-SIN-NOMBRE" : "CHR-" + ascii;
    }

    private static boolean sameName(String left, String right) {
        return Normalizer.normalize(normalizeDisplayName(left), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .equalsIgnoreCase(Normalizer.normalize(normalizeDisplayName(right), Normalizer.Form.NFD)
                        .replaceAll("\\p{M}", ""));
    }

    private static String persistProfileSidecar(
            ProjectSession session,
            String characterId,
            String displayName,
            String description) {
        return session.projectFile()
                .map(file -> writeProfileSidecar(file, characterId, displayName, description))
                .orElse(" Guarda el proyecto para crear la carpeta de personajes.");
    }

    private static String writeProfileSidecar(Path projectFile, String characterId, String displayName, String description) {
        try {
            Path projectDirectory = projectFile.toAbsolutePath().normalize().getParent();
            if (projectDirectory == null) {
                return " No se pudo resolver la carpeta del proyecto para la ficha TXT.";
            }
            Path characterDirectory = projectDirectory
                    .resolve("personajes")
                    .resolve(safeFolderName(characterId));
            Files.createDirectories(characterDirectory.resolve("imagenes"));
            Files.writeString(
                    characterDirectory.resolve("ficha-tecnica.txt"),
                    profileText(characterId, displayName, description),
                    StandardCharsets.UTF_8);
            return " Carpeta de personaje actualizada: " + projectDirectory.relativize(characterDirectory) + ".";
        } catch (IOException | RuntimeException ex) {
            return " No se pudo escribir la ficha TXT: " + safeMessage(ex) + ".";
        }
    }

    private static String profileText(String characterId, String displayName, String description) {
        String body = description == null || description.isBlank() ? "Sin ficha tecnica escrita." : description.strip();
        return "Ficha tecnica de " + normalizeDisplayName(displayName) + System.lineSeparator()
                + System.lineSeparator()
                + "Nombre: " + normalizeDisplayName(displayName) + System.lineSeparator()
                + "Id: " + characterId + System.lineSeparator()
                + System.lineSeparator()
                + body + System.lineSeparator();
    }

    private static String safeFolderName(String value) {
        String normalized = value == null ? "" : value.strip();
        if (normalized.isBlank()) {
            return "CHR-SIN-NOMBRE";
        }
        return normalized.replaceAll("[^A-Za-z0-9._-]+", "-").replaceAll("^-+|-+$", "");
    }

    private static String safeMessage(Exception ex) {
        String message = ex == null ? "" : ex.getMessage();
        return message == null || message.isBlank() ? "error desconocido" : message;
    }

    public VoiceAssignmentResult assignVoice(ProjectSession session, String characterId, String displayName, String voiceProfileId, VoiceLibrary voiceLibrary) {
        String normalizedCharacterId = characterId == null ? "" : characterId.strip();
        String normalizedVoiceId = voiceProfileId == null ? "" : voiceProfileId.strip();
        if (normalizedCharacterId.isBlank() || normalizedVoiceId.isBlank()) {
            return new VoiceAssignmentResult(false, "Selecciona personaje y voz antes de asignar.", null);
        }
        VoiceProfile voice = voiceLibrary.voiceById(normalizedVoiceId).orElse(null);
        if (voice == null) {
            return new VoiceAssignmentResult(false, "La voz seleccionada ya no existe en la biblioteca.", null);
        }
        TheatreProjectLayer theatre = session.project().theatre();
        java.util.ArrayList<TheatreProjectLayer.VoiceRoleAlias> updated = new java.util.ArrayList<>();
        for (TheatreProjectLayer.VoiceRoleAlias alias : theatre.voiceRoleAliases()) {
            if (!alias.characterId().equals(normalizedCharacterId)) updated.add(alias);
        }
        String name = displayName == null || displayName.isBlank() ? normalizedCharacterId : displayName.strip();
        updated.add(new TheatreProjectLayer.VoiceRoleAlias("VOICE-ROLE-" + normalizedCharacterId, name, normalizedVoiceId, normalizedCharacterId, "Voz asignada desde fichas de personajes."));
        session.replaceProject(session.project().withTheatre(replaceVoiceRoleAliases(theatre, updated)), true);
        return new VoiceAssignmentResult(true, "Voz asignada a " + name + ": " + voice.displayName() + ".", voice);
    }

    public static TheatreProjectLayer replaceVoiceRoleAliases(TheatreProjectLayer theatre, java.util.List<TheatreProjectLayer.VoiceRoleAlias> voiceRoleAliases) {
        return new TheatreProjectLayer(theatre.intervenciones(), theatre.characters(), voiceRoleAliases,
                theatre.characterImages(), theatre.intervencionesVisuales(), theatre.intermediateFrames(),
                theatre.acts(), theatre.scenes(), theatre.positions(), theatre.actions(), theatre.textActionPlacements(),
                theatre.objectImages(), theatre.objects(), theatre.audioTracks(), theatre.cameraReferences(),
                theatre.cameraCues(), theatre.stageBackdrops(), theatre.stageBackdropAssignments());
    }

    public record SaveResult(boolean saved, String message) {}
    public record VoiceAssignmentResult(boolean assigned, String message, VoiceProfile voice) {}
}
